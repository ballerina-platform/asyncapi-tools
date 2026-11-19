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
package io.ballerina.asyncapi.generator.ws.client.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.core.model.channel.AsyncApiChannel;
import io.ballerina.asyncapi.core.model.component.AsyncApiComponent;
import io.ballerina.asyncapi.core.model.component.AsyncApiSchema;
import io.ballerina.asyncapi.core.model.message.AsyncApiMessage;
import io.ballerina.asyncapi.core.model.operation.AsyncApiOperation;
import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.ws.client.model.ChannelSpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.ballerina.asyncapi.generator.ws.client.generator.ClientGenerator.X_RESPONSE;

/**
 * The messages a client receives on each connection of a spec: those of its receive operations and the replies
 * ({@code x-response}) its sent messages wait for. Only these have to be told apart, so only they need the
 * dispatcher key, and a connection that receives one kind of message needs none.
 */
public final class IncomingMessages {

    private static final String ONE_OF = "oneOf";
    private static final String REF = "$ref";

    /**
     * A channel and the operations on it.
     *
     * @param channelName the channel's key in the spec
     * @param operations  the operations on the channel, by operation id
     */
    public record Connection(String channelName, Map<String, AsyncApiOperation> operations) { }

    private IncomingMessages() {
    }

    /**
     * Returns each channel of {@code spec} with its operations. A spec with one channel has all its operations on
     * it, as its client is generated from the whole spec.
     *
     * @param spec the spec
     * @return the connections, in spec order
     */
    public static List<Connection> connections(AsyncApiSpec spec) {
        Map<String, AsyncApiChannel> channels = spec.getAsyncApiChannels().orElse(Map.of());
        Map<String, AsyncApiOperation> operations = spec.getAsyncApiOperations().orElse(Map.of());
        if (channels.size() <= 1) {
            String name = channels.isEmpty() ? "" : channels.keySet().iterator().next();
            return List.of(new Connection(name, operations));
        }
        List<Connection> result = new ArrayList<>();
        for (Map.Entry<String, AsyncApiChannel> channel : channels.entrySet()) {
            result.add(new Connection(channel.getKey(),
                    ChannelSpec.operationsOn(spec, channel.getKey(), channel.getValue())));
        }
        return result;
    }

    /**
     * Returns the payloads received through {@code operations}, each once, leaving out close frames and messages
     * without a JSON Schema payload. A named payload is its component schema.
     *
     * @param spec       the spec the operations are in
     * @param operations the operations on one connection
     * @return the received payloads
     */
    public static List<AsyncApiSchema> payloads(AsyncApiSpec spec, Map<String, AsyncApiOperation> operations) {
        AsyncApiComponent components = spec.getAsyncApiComponents().orElse(null);
        Map<String, AsyncApiSchema> schemas = components != null && components.schemas() != null
                ? components.schemas() : Map.of();
        Map<String, AsyncApiMessage> messages = components != null && components.messages() != null
                ? components.messages() : Map.of();
        // By identity, as two messages with the same shape are still two messages
        Set<AsyncApiSchema> received = Collections.newSetFromMap(new IdentityHashMap<>());
        List<AsyncApiSchema> result = new ArrayList<>();
        for (AsyncApiOperation op : operations.values()) {
            if (op.messages() == null) {
                continue;
            }
            for (AsyncApiMessage message : op.messages().values()) {
                if (op.action() == AsyncApiOperation.Action.RECEIVE) {
                    add(message, schemas, received, result);
                } else if (message.extensions() != null && message.extensions().get(X_RESPONSE) != null) {
                    for (String reply : replyNames(message.extensions().get(X_RESPONSE))) {
                        add(messages.get(reply), schemas, received, result);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Returns whether some connection of {@code spec} receives more than one kind of message, so its client needs
     * a key to tell them apart.
     *
     * @param spec the spec
     * @return whether a dispatcher key is needed
     */
    public static boolean needsKey(AsyncApiSpec spec) {
        for (Connection connection : connections(spec)) {
            if (payloads(spec, connection.operations()).size() > 1) {
                return true;
            }
        }
        return false;
    }

    private static void add(AsyncApiMessage message, Map<String, AsyncApiSchema> schemas,
                            Set<AsyncApiSchema> received, List<AsyncApiSchema> result) {
        if (message == null || !(message.payload() instanceof AsyncApiSchema payload)) {
            return;
        }
        AsyncApiSchema schema = payload.name() != null && schemas.containsKey(payload.name())
                ? schemas.get(payload.name()) : payload;
        if (!CodegenUtils.isCloseFrameSchema(schema) && received.add(schema)) {
            result.add(schema);
        }
    }

    // The component messages an x-response points at, directly or through oneOf
    private static List<String> replyNames(JsonNode xResponse) {
        List<JsonNode> entries = new ArrayList<>();
        if (xResponse.get(ONE_OF) instanceof ArrayNode oneOf) {
            oneOf.forEach(entries::add);
        } else {
            entries.add(xResponse);
        }
        List<String> names = new ArrayList<>();
        for (JsonNode entry : entries) {
            JsonNode ref = entry.get(REF);
            if (ref == null) {
                continue;
            }
            try {
                names.add(CodegenUtils.extractReferenceType(ref.asText()));
            } catch (GeneratorException e) {
                // A reference outside the spec is reported where the reply's type is worked out
            }
        }
        return names;
    }
}
