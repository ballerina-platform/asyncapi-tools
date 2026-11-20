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
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.ballerina.asyncapi.core.SpecWarning;
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.core.model.channel.AsyncApiChannel;
import io.ballerina.asyncapi.core.model.component.AsyncApiComponent;
import io.ballerina.asyncapi.core.model.component.AsyncApiSchema;
import io.ballerina.asyncapi.core.model.info.AsyncApiInfo;
import io.ballerina.asyncapi.core.model.message.AsyncApiMessage;
import io.ballerina.asyncapi.core.model.operation.AsyncApiOperation;
import io.ballerina.asyncapi.core.model.server.AsyncApiServer;
import io.ballerina.asyncapi.generator.ws.client.utils.IncomingMessages;
import org.semver4j.Semver;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.ballerina.asyncapi.generator.ws.client.extractor.DispatcherKeyExtractor.X_DISPATCHER_KEY;
import static io.ballerina.asyncapi.generator.ws.client.extractor.DispatcherStreamIdExtractor.X_DISPATCHER_STREAM_ID;
import static io.ballerina.asyncapi.generator.ws.client.generator.ClientGenerator.X_RESPONSE;

/**
 * A view of a spec whose requests and their replies name the same payload field as their {@code correlationId},
 * such as {@code $message.payload#/id}: that field becomes the {@code x-dispatcherStreamId}, so each reply goes to
 * the call that sent its request, even when several calls of one kind wait at once.
 *
 * <p>The field is used only when every request that waits for a reply requires it as a string and every reply
 * declares it, so the client can give each request an id of its own when the caller leaves it empty.
 */
public final class CorrelationIdSpec implements AsyncApiSpec {

    // That the stream id comes from the spec's correlationIds, so the client fills in an id the caller leaves empty
    public static final String X_NEW_STREAM_IDS = "x-ballerina-newStreamIds";
    private static final Pattern PAYLOAD_FIELD = Pattern.compile("^\\$message\\.payload#/([^/]+)$");
    private static final String STRING = "string";
    private static final int MAX_DEPTH = 32;

    private final AsyncApiSpec spec;
    private final Map<String, JsonNode> extensions;

    private CorrelationIdSpec(AsyncApiSpec spec, Map<String, JsonNode> extensions) {
        this.spec = spec;
        this.extensions = extensions;
    }

    /**
     * Returns {@code spec} with the field its correlationIds name as the stream id, when one field pairs every
     * request with its replies; otherwise unchanged.
     *
     * @param spec     the spec, with its dispatcher key worked out
     * @param warnings where to report the field used
     * @return the spec, or a view of it
     */
    public static AsyncApiSpec of(AsyncApiSpec spec, Consumer<SpecWarning> warnings) {
        Map<String, JsonNode> specExtensions = spec == null ? Map.of() : spec.getAsyncApiExtensions().orElse(Map.of());
        // Replies are paired by id only on a key every frame is read by, and only when the spec doesn't name one
        if (spec == null || !specExtensions.containsKey(X_DISPATCHER_KEY)
                || specExtensions.containsKey(X_DISPATCHER_STREAM_ID)) {
            return spec;
        }
        AsyncApiComponent components = spec.getAsyncApiComponents().orElse(null);
        Map<String, AsyncApiMessage> componentMessages = components != null && components.messages() != null
                ? components.messages() : Map.of();
        Map<String, AsyncApiSchema> schemas = components != null && components.schemas() != null
                ? components.schemas() : Map.of();
        List<AsyncApiMessage> requests = new ArrayList<>();
        List<AsyncApiMessage> replies = new ArrayList<>();
        for (AsyncApiOperation op : spec.getAsyncApiOperations().orElse(Map.of()).values()) {
            if (op.action() != AsyncApiOperation.Action.SEND || op.messages() == null) {
                continue;
            }
            for (AsyncApiMessage message : op.messages().values()) {
                JsonNode xResponse = message.extensions() != null ? message.extensions().get(X_RESPONSE) : null;
                if (xResponse == null) {
                    continue;
                }
                requests.add(message);
                for (String reply : IncomingMessages.replyNames(xResponse)) {
                    if (componentMessages.get(reply) != null) {
                        replies.add(componentMessages.get(reply));
                    }
                }
            }
        }
        String field = sharedField(requests, replies);
        if (field == null || !requests.stream().allMatch(m -> declaresString(m, field, true, schemas))
                || !replies.stream().allMatch(m -> declaresString(m, field, false, schemas))) {
            return spec;
        }
        warnings.accept(new SpecWarning("", String.format("the spec has no x-dispatcherStreamId, so the client "
                + "pairs each reply with its request on '%s', the correlationId of its messages, and gives a "
                + "request an id of its own when '%s' is empty", field, field),
                String.format("Add 'x-dispatcherStreamId: %s' to the spec to make it explicit.", field)));
        Map<String, JsonNode> paired = new LinkedHashMap<>(specExtensions);
        paired.put(X_DISPATCHER_STREAM_ID, TextNode.valueOf(field));
        paired.put(X_NEW_STREAM_IDS, BooleanNode.TRUE);
        return new CorrelationIdSpec(spec, paired);
    }

    /**
     * Returns whether the client gives a request an id of its own when the caller leaves the stream id empty.
     *
     * @param spec the spec
     * @return whether the stream id comes from the spec's correlationIds
     */
    public static boolean newStreamIds(AsyncApiSpec spec) {
        return spec.getAsyncApiExtensions().map(ext -> ext.get(X_NEW_STREAM_IDS)).map(JsonNode::asBoolean)
                .orElse(false);
    }

    // The one top-level payload field every request and reply names as its correlationId, if they all name one
    private static String sharedField(List<AsyncApiMessage> requests, List<AsyncApiMessage> replies) {
        if (requests.isEmpty() || replies.isEmpty()) {
            return null;
        }
        Set<String> fields = new LinkedHashSet<>();
        List<AsyncApiMessage> all = new ArrayList<>(requests);
        all.addAll(replies);
        for (AsyncApiMessage message : all) {
            String location = message.correlationId() != null ? message.correlationId().location() : null;
            Matcher matcher = location != null ? PAYLOAD_FIELD.matcher(location) : null;
            if (matcher == null || !matcher.matches()) {
                return null;
            }
            fields.add(matcher.group(1).replace("~1", "/").replace("~0", "~"));
        }
        return fields.size() == 1 ? fields.iterator().next() : null;
    }

    // Whether the message's payload declares the field as a string, and requires it when it must
    private static boolean declaresString(AsyncApiMessage message, String field, boolean required,
                                         Map<String, AsyncApiSchema> schemas) {
        if (!(message.payload() instanceof AsyncApiSchema payload)) {
            return false;
        }
        Map<String, AsyncApiSchema> declared = new LinkedHashMap<>();
        Set<String> requires = new LinkedHashSet<>();
        collect(payload, schemas, declared, requires, 0);
        AsyncApiSchema schema = resolve(declared.get(field), schemas);
        return schema != null && STRING.equals(schema.type()) && (!required || requires.contains(field));
    }

    private static void collect(AsyncApiSchema schema, Map<String, AsyncApiSchema> schemas,
                                Map<String, AsyncApiSchema> declared, Set<String> required, int depth) {
        AsyncApiSchema resolved = resolve(schema, schemas);
        if (resolved == null || depth > MAX_DEPTH) {
            return;
        }
        if (resolved.allOf() != null) {
            for (AsyncApiSchema part : resolved.allOf()) {
                collect(part, schemas, declared, required, depth + 1);
            }
        }
        if (resolved.properties() != null) {
            resolved.properties().forEach(declared::putIfAbsent);
        }
        if (resolved.required() != null) {
            required.addAll(resolved.required());
        }
    }

    private static AsyncApiSchema resolve(AsyncApiSchema schema, Map<String, AsyncApiSchema> schemas) {
        return schema != null && schema.name() != null && schemas.containsKey(schema.name())
                ? schemas.get(schema.name()) : schema;
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
        return spec.getAsyncApiOperations();
    }

    @Override
    public Optional<AsyncApiComponent> getAsyncApiComponents() {
        return spec.getAsyncApiComponents();
    }

    @Override
    public Optional<Map<String, JsonNode>> getAsyncApiExtensions() {
        return Optional.of(extensions);
    }
}
