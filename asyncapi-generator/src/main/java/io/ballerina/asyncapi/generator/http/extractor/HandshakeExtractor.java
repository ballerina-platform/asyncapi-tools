/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com)
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
package io.ballerina.asyncapi.generator.http.extractor;

import com.fasterxml.jackson.databind.JsonNode;
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.core.model.component.AsyncApiSchema;
import io.ballerina.asyncapi.core.model.message.AsyncApiCorrelationId;
import io.ballerina.asyncapi.core.model.message.AsyncApiMessage;
import io.ballerina.asyncapi.core.model.operation.AsyncApiOperation;
import io.ballerina.asyncapi.core.model.operation.HttpOperationBindings;
import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.http.model.HandshakeConfig;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Finds the endpoint verification handshake in a definition. A handshake is a {@code receive} operation with a
 * {@code reply}, the AsyncAPI request/reply pattern: the listener answers it itself, so it is not an event.
 *
 * <ul>
 *     <li>A {@code POST} (the default): the {@code const} values in the request message's payload schema tell the
 *     handshake apart from events, for example {@code type: {const: url_verification}}, and the request message's
 *     {@code correlationId} points at the challenge to echo, for example {@code $message.payload#/challenge}.</li>
 *     <li>A {@code GET} (the operation's HTTP binding {@code method}): a W3C WebSub verification of intent, with
 *     the challenge in the {@code hub.challenge} query parameter of the binding's {@code query} schema.</li>
 *     <li>The reply message's HTTP binding {@code statusCode} is the response status, {@code 200} if unset.</li>
 * </ul>
 */
public final class HandshakeExtractor {

    private static final String PAYLOAD_EXPRESSION = "$message.payload#/";
    private static final String HEADER_EXPRESSION = "$message.header";
    private static final int DEFAULT_STATUS_CODE = 200;
    private static final String WEBSUB_CHALLENGE = "hub.challenge";

    private final AsyncApiSpec asyncApiSpec;

    public HandshakeExtractor(AsyncApiSpec asyncApiSpec) {
        this.asyncApiSpec = asyncApiSpec;
    }

    /**
     * Whether an operation is a handshake rather than an event.
     *
     * @param operation an operation from the definition
     * @return {@code true} for a {@code receive} operation with a {@code reply}
     */
    public static boolean isHandshake(AsyncApiOperation operation) {
        return operation.action() == AsyncApiOperation.Action.RECEIVE && operation.reply() != null;
    }

    /**
     * Reads the handshake from the definition.
     *
     * @return the handshake, or empty if no operation has a reply
     * @throws GeneratorException if there is more than one, or one cannot be answered
     */
    public Optional<HandshakeConfig> extract() throws GeneratorException {
        Map<String, AsyncApiOperation> operations = asyncApiSpec.getAsyncApiOperations().orElse(Map.of());
        String handshakeId = null;
        AsyncApiOperation handshake = null;
        for (Map.Entry<String, AsyncApiOperation> entry : operations.entrySet()) {
            if (!isHandshake(entry.getValue())) {
                continue;
            }
            if (handshake != null) {
                throw new GeneratorException(String.format("Operations '%s' and '%s' both have a reply; the "
                        + "listener answers one verification handshake", handshakeId, entry.getKey()));
            }
            handshakeId = entry.getKey();
            handshake = entry.getValue();
        }
        return handshake == null ? Optional.empty() : Optional.of(read(handshakeId, handshake));
    }

    private static HandshakeConfig read(String operationId, AsyncApiOperation operation) throws GeneratorException {
        HttpOperationBindings http = operation.bindings() != null
                ? operation.bindings().httpOperationBindings() : null;
        HttpOperationBindings.HttpMethod method = http != null && http.method() != null
                ? http.method() : HttpOperationBindings.HttpMethod.POST;
        if (method == HttpOperationBindings.HttpMethod.GET) {
            return readWebSub(operationId, operation, http.query());
        }
        if (method != HttpOperationBindings.HttpMethod.POST) {
            throw new GeneratorException(String.format("Handshake operation '%s' uses %s; a handshake is a POST with "
                    + "the challenge in the body, or a GET WebSub verification", operationId, method));
        }
        Map<String, AsyncApiMessage> messages = operation.messages();
        if (messages == null || messages.size() != 1) {
            throw new GeneratorException(String.format(
                    "Handshake operation '%s' must have exactly one request message", operationId));
        }
        AsyncApiMessage request = messages.values().iterator().next();

        Map<String, JsonNode> when = new LinkedHashMap<>();
        if (request.payload() instanceof AsyncApiSchema payload) {
            collectConstants(payload, "", when);
        }
        if (when.isEmpty()) {
            throw new GeneratorException(String.format("Handshake operation '%s' needs a 'const' in its request "
                    + "payload schema, such as type: {const: url_verification}, to tell it apart from events",
                    operationId));
        }
        return new HandshakeConfig(when, challengePath(operationId, request.correlationId()), null, List.of(),
                statusCode(operation));
    }

    /**
     * Reads a {@code GET} handshake as a W3C WebSub verification of intent: the hub sends the challenge in the
     * {@code hub.challenge} query parameter, which a runtime expression cannot point at, so the standard names it.
     * The listener confirms a request whose query has the schema's {@code const} values, such as
     * {@code hub.mode: {const: subscribe}}, and every parameter the schema marks {@code required}, such as
     * {@code hub.topic}.
     */
    private static HandshakeConfig readWebSub(String operationId, AsyncApiOperation operation, AsyncApiSchema query)
            throws GeneratorException {
        if (query == null || query.properties() == null || !query.properties().containsKey(WEBSUB_CHALLENGE)) {
            throw new GeneratorException(String.format("Handshake operation '%s' is a GET, which is read as a WebSub "
                    + "verification of intent; its HTTP binding's query schema needs %s", operationId,
                    WEBSUB_CHALLENGE));
        }
        Map<String, JsonNode> when = new LinkedHashMap<>();
        for (Map.Entry<String, AsyncApiSchema> parameter : query.properties().entrySet()) {
            if (parameter.getValue().constValue() != null) {
                when.put(parameter.getKey(), parameter.getValue().constValue());
            }
        }
        List<String> required = query.required() == null ? List.of() : query.required().stream()
                .filter(name -> !WEBSUB_CHALLENGE.equals(name) && !when.containsKey(name)).toList();
        return new HandshakeConfig(when, null, WEBSUB_CHALLENGE, required, statusCode(operation));
    }

    // Walks the payload's object properties, collecting each const with its dot-notation path.
    private static void collectConstants(AsyncApiSchema schema, String prefix, Map<String, JsonNode> when) {
        if (schema.properties() == null) {
            return;
        }
        for (Map.Entry<String, AsyncApiSchema> property : schema.properties().entrySet()) {
            String path = prefix + property.getKey();
            if (property.getValue().constValue() != null) {
                when.put(path, property.getValue().constValue());
            } else {
                collectConstants(property.getValue(), path + ".", when);
            }
        }
    }

    private static String challengePath(String operationId, AsyncApiCorrelationId correlationId)
            throws GeneratorException {
        String location = correlationId != null ? correlationId.location() : null;
        if (location == null || location.isBlank()) {
            throw new GeneratorException(String.format("Handshake operation '%s' needs a correlationId on its "
                    + "request message pointing at the challenge, such as $message.payload#/challenge", operationId));
        }
        if (location.startsWith(HEADER_EXPRESSION)) {
            throw new GeneratorException(String.format("Handshake operation '%s' reads its challenge from a "
                    + "header (%s); only a challenge in the payload is supported", operationId, location));
        }
        if (!location.startsWith(PAYLOAD_EXPRESSION) || location.length() == PAYLOAD_EXPRESSION.length()) {
            throw new GeneratorException(String.format("Handshake operation '%s' has correlationId location '%s'; "
                    + "it must be a runtime expression such as $message.payload#/challenge", operationId, location));
        }
        // A JSON Pointer's segments become a dot-notation path, with ~1 and ~0 unescaped (RFC 6901).
        String[] segments = location.substring(PAYLOAD_EXPRESSION.length()).split("/");
        StringBuilder path = new StringBuilder();
        for (String segment : segments) {
            if (!path.isEmpty()) {
                path.append('.');
            }
            path.append(segment.replace("~1", "/").replace("~0", "~"));
        }
        return path.toString();
    }

    private static int statusCode(AsyncApiOperation operation) {
        if (operation.reply().messages() == null) {
            return DEFAULT_STATUS_CODE;
        }
        for (AsyncApiMessage reply : operation.reply().messages().values()) {
            if (reply.bindings() != null && reply.bindings().httpMessageBindings() != null
                    && reply.bindings().httpMessageBindings().statusCode() != null) {
                return reply.bindings().httpMessageBindings().statusCode();
            }
        }
        return DEFAULT_STATUS_CODE;
    }
}
