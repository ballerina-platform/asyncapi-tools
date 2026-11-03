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
package io.ballerina.asyncapi.generator.ws.client.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.ballerina.asyncapi.core.AsyncApiParser;
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.core.model.channel.AsyncApiChannel;
import io.ballerina.asyncapi.core.model.component.AsyncApiComponent;
import io.ballerina.asyncapi.core.model.component.AsyncApiSchema;
import io.ballerina.asyncapi.core.model.info.AsyncApiInfo;
import io.ballerina.asyncapi.core.model.message.AsyncApiMessage;
import io.ballerina.asyncapi.core.model.operation.AsyncApiOperation;
import io.ballerina.asyncapi.core.model.server.AsyncApiServer;
import io.ballerina.asyncapi.generator.ws.client.utils.CodegenUtils;
import org.semver4j.Semver;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A view of a spec in which every message payload has a schema name, so each one maps to a generated type.
 * Inline payloads move into {@code components.schemas} under their message's name, and inline
 * {@code x-response} payloads become references to component messages.
 */
public final class NamedPayloadSpec implements AsyncApiSpec {

    private static final String X_RESPONSE = "x-response";
    private static final String PAYLOAD = "payload";
    private static final String ONE_OF = "oneOf";
    private static final String REF = "$ref";
    private static final String RESPONSE_SUFFIX = "Response";
    private static final String MESSAGES_REF_PREFIX = "#/components/messages/";
    // Type names the client generator writes itself
    private static final Set<String> GENERATED_TYPE_NAMES = Set.of("Message", "MessageWithId", "PipesMap",
            "StreamGeneratorsMap", "Generator", "SimpleBasicType");

    private final AsyncApiSpec spec;
    private final Map<String, AsyncApiSchema> schemas;
    private final Map<String, AsyncApiMessage> messages;
    private final Set<String> takenTypeNames = new HashSet<>(GENERATED_TYPE_NAMES);
    private final Map<AsyncApiMessage, AsyncApiMessage> named = new HashMap<>();
    private final AsyncApiComponent components;
    private final Map<String, AsyncApiOperation> operations;

    private NamedPayloadSpec(AsyncApiSpec spec) {
        this.spec = spec;
        AsyncApiComponent original = spec.getAsyncApiComponents().orElse(null);
        this.schemas = original != null && original.schemas() != null
                ? new LinkedHashMap<>(original.schemas()) : new LinkedHashMap<>();
        this.messages = original != null && original.messages() != null
                ? new LinkedHashMap<>(original.messages()) : new LinkedHashMap<>();
        for (String schemaName : schemas.keySet()) {
            takenTypeNames.add(CodegenUtils.getValidName(schemaName.trim(), true));
        }

        // Name every payload before rewriting replies, so an inline reply can match an existing reply message
        Map<String, AsyncApiMessage> originals = new LinkedHashMap<>(messages);
        for (Map.Entry<String, AsyncApiMessage> entry : originals.entrySet()) {
            messages.put(entry.getKey(), withNamedPayload(entry.getKey(), entry.getValue()));
        }
        for (Map.Entry<String, AsyncApiMessage> entry : originals.entrySet()) {
            AsyncApiMessage result = withResponseRefs(entry.getKey(), messages.get(entry.getKey()));
            messages.put(entry.getKey(), result);
            named.put(entry.getValue(), result);
        }
        this.operations = spec.getAsyncApiOperations().map(this::nameOperations).orElse(null);
        this.components = withSchemasAndMessages(original);
    }

    /**
     * Returns a view of {@code spec} in which every message payload is named.
     *
     * @param spec the parsed spec
     * @return the named view, or {@code spec} itself when it is {@code null} or already a named view
     */
    public static AsyncApiSpec of(AsyncApiSpec spec) {
        if (spec == null || spec instanceof NamedPayloadSpec) {
            return spec;
        }
        return new NamedPayloadSpec(spec);
    }

    private Map<String, AsyncApiOperation> nameOperations(Map<String, AsyncApiOperation> original) {
        Map<String, AsyncApiOperation> result = new LinkedHashMap<>();
        for (Map.Entry<String, AsyncApiOperation> entry : original.entrySet()) {
            AsyncApiOperation op = entry.getValue();
            if (op.messages() == null) {
                result.put(entry.getKey(), op);
                continue;
            }
            Map<String, AsyncApiMessage> opMessages = new LinkedHashMap<>();
            for (Map.Entry<String, AsyncApiMessage> msgEntry : op.messages().entrySet()) {
                opMessages.put(msgEntry.getKey(), name(msgEntry.getKey(), msgEntry.getValue()));
            }
            result.put(entry.getKey(), new AsyncApiOperation(op.action(), op.channelId(), op.channel(), op.title(),
                    op.summary(), op.description(), opMessages, op.security(), op.reply(), op.tags(),
                    op.externalDocs(), op.bindings(), op.traits(), op.extensions()));
        }
        return result;
    }

    // An operation message equal to a component message reuses the names given to the component message
    private AsyncApiMessage name(String messageName, AsyncApiMessage message) {
        AsyncApiMessage done = named.get(message);
        if (done != null) {
            return done;
        }
        AsyncApiMessage result = withResponseRefs(messageName, withNamedPayload(messageName, message));
        named.put(message, result);
        return result;
    }

    private AsyncApiMessage withNamedPayload(String messageName, AsyncApiMessage message) {
        if (!(message.payload() instanceof AsyncApiSchema payload) || payload.name() != null
                || CodegenUtils.isCloseFrameSchema(payload)) {
            return message;
        }
        String schemaName = addSchema(messageName, payload);
        return withPayload(message, payload.withName(schemaName), message.extensions());
    }

    private AsyncApiMessage withResponseRefs(String messageName, AsyncApiMessage message) {
        Map<String, JsonNode> extensions = message.extensions();
        if (extensions == null || extensions.get(X_RESPONSE) == null) {
            return message;
        }
        JsonNode xResponse = extensions.get(X_RESPONSE);
        JsonNode newXResponse = xResponse;
        String baseName = messageName + RESPONSE_SUFFIX;
        if (xResponse.get(PAYLOAD) != null) {
            newXResponse = responseRef(baseName, xResponse);
        } else if (xResponse.get(ONE_OF) instanceof ArrayNode oneOf) {
            ArrayNode newOneOf = JsonNodeFactory.instance.arrayNode();
            for (JsonNode entry : oneOf) {
                newOneOf.add(entry.get(PAYLOAD) != null ? responseRef(baseName, entry) : entry);
            }
            ObjectNode copy = ((ObjectNode) xResponse).deepCopy();
            copy.set(ONE_OF, newOneOf);
            newXResponse = copy;
        }
        if (newXResponse == xResponse) {
            return message;
        }
        Map<String, JsonNode> newExtensions = new LinkedHashMap<>(extensions);
        newExtensions.put(X_RESPONSE, newXResponse);
        return withPayload(message, message.payload(), newExtensions);
    }

    // Points an inline reply at a component message, reusing `<name>Response` when it carries the same payload
    private JsonNode responseRef(String baseName, JsonNode inlineResponse) {
        // A $ref payload maps to a stub that only carries the referenced schema's name
        AsyncApiSchema payload = AsyncApiParser.parseSchema(inlineResponse.get(PAYLOAD));
        if (payload == null) {
            return inlineResponse;
        }
        String messageName = baseName;
        if (!samePayload(messages.get(baseName), payload)) {
            messageName = freeName(baseName, messages.keySet());
            AsyncApiSchema namedPayload = payload.name() != null
                    ? payload : payload.withName(addSchema(messageName, payload));
            messages.put(messageName, new AsyncApiMessage(null, namedPayload, null, null, null, null, null,
                    null, null, null, null, null, null, null));
        }
        ObjectNode ref = JsonNodeFactory.instance.objectNode();
        ref.put(REF, MESSAGES_REF_PREFIX + messageName);
        return ref;
    }

    private boolean samePayload(AsyncApiMessage existing, AsyncApiSchema payload) {
        if (existing == null || !(existing.payload() instanceof AsyncApiSchema existingPayload)
                || existingPayload.name() == null) {
            return false;
        }
        if (payload.name() != null) {
            return payload.name().equals(existingPayload.name());
        }
        return payload.equals(schemas.get(existingPayload.name()));
    }

    private String addSchema(String messageName, AsyncApiSchema schema) {
        String schemaName = freeName(CodegenUtils.getValidName(messageName.trim(), true), takenTypeNames);
        schemas.put(schemaName, schema);
        takenTypeNames.add(schemaName);
        return schemaName;
    }

    private static String freeName(String base, Set<String> taken) {
        String candidate = base;
        for (int i = 2; taken.contains(candidate); i++) {
            candidate = base + i;
        }
        return candidate;
    }

    private static AsyncApiMessage withPayload(AsyncApiMessage m, Object payload, Map<String, JsonNode> extensions) {
        return new AsyncApiMessage(m.headers(), payload, m.correlationId(), m.contentType(), m.name(), m.title(),
                m.summary(), m.description(), m.tags(), m.externalDocs(), m.bindings(), m.examples(), m.traits(),
                extensions);
    }

    private AsyncApiComponent withSchemasAndMessages(AsyncApiComponent c) {
        if (c == null) {
            return schemas.isEmpty() && messages.isEmpty() ? null : new AsyncApiComponent(schemas, null, null,
                    null, null, messages, null, null, null, null, null, null, null, null, null, null, null, null,
                    null, null);
        }
        return new AsyncApiComponent(schemas, c.servers(), c.serverVariables(), c.channels(), c.operations(),
                messages, c.securitySchemes(), c.parameters(), c.correlationIds(), c.operationTraits(),
                c.messageTraits(), c.replies(), c.replyAddresses(), c.externalDocs(), c.tags(), c.serverBindings(),
                c.channelBindings(), c.operationBindings(), c.messageBindings(), c.extensions());
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
        return Optional.ofNullable(operations);
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
