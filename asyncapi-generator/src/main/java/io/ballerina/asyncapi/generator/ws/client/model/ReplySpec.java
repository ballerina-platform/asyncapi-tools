/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package io.ballerina.asyncapi.generator.ws.client.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.ballerina.asyncapi.core.SpecWarning;
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.core.model.channel.AsyncApiChannel;
import io.ballerina.asyncapi.core.model.component.AsyncApiComponent;
import io.ballerina.asyncapi.core.model.info.AsyncApiInfo;
import io.ballerina.asyncapi.core.model.message.AsyncApiMessage;
import io.ballerina.asyncapi.core.model.operation.AsyncApiOperation;
import io.ballerina.asyncapi.core.model.operation.AsyncApiOperationReply;
import io.ballerina.asyncapi.core.model.server.AsyncApiServer;
import org.semver4j.Semver;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static io.ballerina.asyncapi.generator.ws.client.generator.ClientGenerator.X_RESPONSE;
import static io.ballerina.asyncapi.generator.ws.client.generator.ClientGenerator.X_RESPONSE_TYPE;

/**
 * A view of a 3.0 spec whose operation replies are read as {@code x-response}: each message an operation sends
 * gets the operation's reply messages as its reply, so its function returns them. A message's own
 * {@code x-response} wins.
 */
public final class ReplySpec implements AsyncApiSpec {

    private static final String MESSAGES_REF_PREFIX = "#/components/messages/";
    private static final String SIMPLE_RPC = "simple-rpc";

    private final AsyncApiSpec spec;
    private final Map<String, AsyncApiOperation> operations;
    private final AsyncApiComponent components;

    private ReplySpec(AsyncApiSpec spec, Map<String, AsyncApiOperation> operations, AsyncApiComponent components) {
        this.spec = spec;
        this.operations = operations;
        this.components = components;
    }

    /**
     * Returns {@code spec} with the replies of its send operations read as {@code x-response}, warning about a
     * reply the client can't wait for.
     *
     * @param spec     the spec, read from the client's side
     * @param warnings where to report a reply left out
     * @return the spec, or a view of it
     */
    public static AsyncApiSpec of(AsyncApiSpec spec, Consumer<SpecWarning> warnings) {
        Map<String, AsyncApiOperation> original = spec == null ? null : spec.getAsyncApiOperations().orElse(null);
        if (original == null || original.values().stream().noneMatch(op -> op != null && op.reply() != null)) {
            return spec;
        }
        AsyncApiComponent component = spec.getAsyncApiComponents().orElse(null);
        Map<String, AsyncApiMessage> messages = new LinkedHashMap<>();
        if (component != null && component.messages() != null) {
            messages.putAll(component.messages());
        }
        Map<String, AsyncApiOperation> result = new LinkedHashMap<>();
        for (Map.Entry<String, AsyncApiOperation> entry : original.entrySet()) {
            AsyncApiOperation op = entry.getValue();
            Map<String, AsyncApiMessage> replies = op == null ? null : replies(entry.getKey(), op, original, warnings);
            if (replies == null) {
                result.put(entry.getKey(), op);
                continue;
            }
            JsonNode xResponse = xResponse(replies, messages);
            Map<String, AsyncApiMessage> sent = new LinkedHashMap<>();
            for (Map.Entry<String, AsyncApiMessage> message : op.messages().entrySet()) {
                AsyncApiMessage request = message.getValue();
                if (request == null || request.extensions() != null && request.extensions().containsKey(X_RESPONSE)) {
                    sent.put(message.getKey(), request);
                    continue;
                }
                AsyncApiMessage withReply = withExtensions(request, xResponse);
                sent.put(message.getKey(), withReply);
                // The component the operation lists gets the reply too, as functions are named after it
                messages.replaceAll((name, m) -> request.equals(m) ? withReply : m);
            }
            result.put(entry.getKey(), new AsyncApiOperation(op.action(), op.channelId(), op.channel(), op.title(),
                    op.summary(), op.description(), sent, op.security(), op.reply(), op.tags(), op.externalDocs(),
                    op.bindings(), op.traits(), op.extensions()));
        }
        return new ReplySpec(spec, result, withMessages(component, messages));
    }

    /**
     * Returns whether a send operation of {@code spec} has a reply, which {@link #of} reads.
     *
     * @param spec the spec, read from the client's side
     * @return whether the spec has a send operation with a reply
     */
    public static boolean hasReplies(AsyncApiSpec spec) {
        return spec != null && spec.getAsyncApiOperations().orElse(Map.of()).values().stream()
                .anyMatch(op -> op != null && op.action() == AsyncApiOperation.Action.SEND && op.reply() != null);
    }

    /**
     * Returns the warning for replies left out because the client couldn't be generated with them.
     *
     * @param reason why generating with the replies failed
     * @return the warning
     */
    public static SpecWarning leftOut(String reason) {
        String firstLine = reason == null ? "" : reason.lines().findFirst().orElse("").strip();
        if (firstLine.endsWith(":")) {
            firstLine = firstLine.substring(0, firstLine.length() - 1);
        }
        return new SpecWarning("/operations", "the replies of the spec's operations can't be routed, so the "
                + "functions that send their requests don't wait for them: " + firstLine,
                "Give each reply a field with its own fixed value and name it in x-dispatcherKey. See "
                        + "https://github.com/ballerina-platform/asyncapi-tools/blob/main/docs/"
                        + "websocket-dispatcher-key.md");
    }

    // The replies a send operation's messages get, or null when it has none the client can wait for
    private static Map<String, AsyncApiMessage> replies(String id, AsyncApiOperation op,
                                                        Map<String, AsyncApiOperation> operations,
                                                        Consumer<SpecWarning> warnings) {
        AsyncApiOperationReply reply = op.reply();
        if (reply == null || op.messages() == null || op.messages().isEmpty()) {
            return null;
        }
        String location = "/operations/" + pointerToken(id) + "/reply";
        if (op.action() == AsyncApiOperation.Action.RECEIVE) {
            warnReceivedRequest(location, op, reply, operations, warnings);
            return null;
        }
        if (op.action() != AsyncApiOperation.Action.SEND) {
            return null;
        }
        // Each channel is a connection of its own, so a reply on another one can't be paired with the request
        if (reply.channel() != null && op.channel() != null && !reply.channel().equals(op.channel())) {
            warnings.accept(new SpecWarning(location, String.format("operation '%s' gets its reply on another "
                    + "channel, which the client connects to separately, so its function doesn't wait for the "
                    + "reply", id), "Receive the reply with the client of that channel."));
            return null;
        }
        Map<String, AsyncApiMessage> replies = reply.messages();
        if (replies == null || replies.isEmpty()) {
            // Without messages, a reply can be any message of its channel other than the ones sent
            AsyncApiChannel channel = reply.channel() != null ? reply.channel() : op.channel();
            replies = new LinkedHashMap<>();
            if (channel != null && channel.messages() != null) {
                for (Map.Entry<String, AsyncApiMessage> message : channel.messages().entrySet()) {
                    if (message.getValue() != null && !op.messages().containsValue(message.getValue())) {
                        replies.put(message.getKey(), message.getValue());
                    }
                }
            }
        }
        if (replies.isEmpty()) {
            warnings.accept(new SpecWarning(location, String.format("operation '%s' expects a reply but doesn't "
                    + "say which message it is, so its function doesn't wait for one", id),
                    "List the reply's message under the operation's reply 'messages'."));
            return null;
        }
        return replies;
    }

    // A message the server sends expecting the client to answer: the client receives it, but doesn't answer itself
    private static void warnReceivedRequest(String location, AsyncApiOperation op, AsyncApiOperationReply reply,
                                            Map<String, AsyncApiOperation> operations,
                                            Consumer<SpecWarning> warnings) {
        List<String> answers = reply.messages() == null ? List.of() : new ArrayList<>(reply.messages().keySet());
        if (answers.isEmpty()) {
            return;
        }
        boolean sendable = operations.values().stream().anyMatch(other -> other != null
                && other.action() == AsyncApiOperation.Action.SEND && other.messages() != null
                && answers.stream().anyMatch(other.messages()::containsKey));
        warnings.accept(new SpecWarning(location, String.format("the server expects '%s' in reply to '%s', and the "
                        + "client doesn't reply on its own", String.join("' or '", answers),
                String.join("' or '", op.messages().keySet())),
                sendable ? "Send the reply with its function after the client receives the message."
                        : "Add a send operation for the reply to get a function that sends it."));
    }

    // The x-response that names the replies, adding a reply that isn't a component message to the components
    private static JsonNode xResponse(Map<String, AsyncApiMessage> replies, Map<String, AsyncApiMessage> messages) {
        List<String> names = new ArrayList<>();
        for (Map.Entry<String, AsyncApiMessage> reply : replies.entrySet()) {
            String name = messages.entrySet().stream().filter(m -> reply.getValue().equals(m.getValue()))
                    .map(Map.Entry::getKey).findFirst().orElse(null);
            if (name == null) {
                name = reply.getKey();
                for (int n = 2; messages.containsKey(name); n++) {
                    name = reply.getKey() + n;
                }
                messages.put(name, reply.getValue());
            }
            if (!names.contains(name)) {
                names.add(name);
            }
        }
        if (names.size() == 1) {
            return ref(names.get(0));
        }
        ObjectNode oneOf = JsonNodeFactory.instance.objectNode();
        ArrayNode refs = oneOf.putArray("oneOf");
        names.forEach(name -> refs.add(ref(name)));
        return oneOf;
    }

    private static ObjectNode ref(String name) {
        return JsonNodeFactory.instance.objectNode().put("$ref", MESSAGES_REF_PREFIX + pointerToken(name));
    }

    private static AsyncApiMessage withExtensions(AsyncApiMessage message, JsonNode xResponse) {
        Map<String, JsonNode> extensions = message.extensions() == null ? new LinkedHashMap<>()
                : new LinkedHashMap<>(message.extensions());
        extensions.put(X_RESPONSE, xResponse);
        extensions.putIfAbsent(X_RESPONSE_TYPE, TextNode.valueOf(SIMPLE_RPC));
        return new AsyncApiMessage(message.headers(), message.payload(), message.correlationId(),
                message.contentType(), message.name(), message.title(), message.summary(), message.description(),
                message.tags(), message.externalDocs(), message.bindings(), message.examples(), message.traits(),
                extensions);
    }

    private static AsyncApiComponent withMessages(AsyncApiComponent c, Map<String, AsyncApiMessage> messages) {
        if (c == null) {
            return new AsyncApiComponent(null, null, null, null, null, messages, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null);
        }
        return new AsyncApiComponent(c.schemas(), c.servers(), c.serverVariables(), c.channels(), c.operations(),
                messages, c.securitySchemes(), c.parameters(), c.correlationIds(), c.operationTraits(),
                c.messageTraits(), c.replies(), c.replyAddresses(), c.externalDocs(), c.tags(), c.serverBindings(),
                c.channelBindings(), c.operationBindings(), c.messageBindings(), c.extensions());
    }

    private static String pointerToken(String key) {
        return key.replace("~", "~0").replace("/", "~1");
    }

    @Override
    public Semver getAsyncApiVersion() {
        return spec.getAsyncApiVersion();
    }

    @Override
    public Optional<URI> getAsyncApiId() {
        return spec.getAsyncApiId();
    }

    @Override
    public AsyncApiInfo getAsyncApiInfo() {
        return spec.getAsyncApiInfo();
    }

    @Override
    public Optional<Map<String, AsyncApiServer>> getAsyncApiServers() {
        return spec.getAsyncApiServers();
    }

    @Override
    public Optional<String> getAsyncApiContentType() {
        return spec.getAsyncApiContentType();
    }

    @Override
    public Optional<Map<String, AsyncApiChannel>> getAsyncApiChannels() {
        return spec.getAsyncApiChannels();
    }

    @Override
    public Optional<Map<String, AsyncApiOperation>> getAsyncApiOperations() {
        return Optional.of(operations);
    }

    @Override
    public Optional<AsyncApiComponent> getAsyncApiComponents() {
        return Optional.ofNullable(components);
    }

    @Override
    public Optional<Map<String, JsonNode>> getAsyncApiExtensions() {
        return spec.getAsyncApiExtensions();
    }
}
