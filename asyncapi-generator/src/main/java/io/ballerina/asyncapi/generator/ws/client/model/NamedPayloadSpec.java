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
import com.fasterxml.jackson.databind.node.TextNode;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static io.ballerina.asyncapi.generator.ws.client.extractor.DispatcherKeyExtractor.X_DISPATCHER_KEY;

/**
 * A view of a spec in which every message payload has a schema name, so each one maps to a generated type.
 * Inline payloads move into {@code components.schemas} under their message's name, and inline
 * {@code x-response} payloads become references to component messages. A request message also records the
 * dispatcher value its payload fixes, as {@link #X_DISPATCHER_VALUE}.
 */
public final class NamedPayloadSpec implements AsyncApiSpec {

    /**
     * Message extension holding the value a request message fixes for the dispatcher field, which the generated
     * client sets before sending.
     */
    public static final String X_DISPATCHER_VALUE = "x-dispatcherValue";

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
    private final String dispatcherKey;
    private final Map<String, AsyncApiSchema> schemas;
    private final Map<String, AsyncApiMessage> messages;
    private final Set<String> takenTypeNames = new HashSet<>(GENERATED_TYPE_NAMES);
    private final SchemaRenames renames;
    private final Map<String, AsyncApiMessage> originalMessages;
    // Operation messages that differ from the component message of the same name, before and after naming
    private final Map<String, AsyncApiMessage> operationOriginals = new HashMap<>();
    private final Map<String, AsyncApiMessage> operationNamed = new HashMap<>();
    private final AsyncApiComponent components;
    private final Map<String, AsyncApiOperation> operations;

    /**
     * Names every payload of {@code spec} up front, so the getters only return the result.
     */
    private NamedPayloadSpec(AsyncApiSpec spec) {
        this.spec = spec;
        JsonNode dispatcherKeyNode = spec.getAsyncApiExtensions().map(ext -> ext.get(X_DISPATCHER_KEY)).orElse(null);
        this.dispatcherKey = dispatcherKeyNode != null ? dispatcherKeyNode.asText() : null;
        AsyncApiComponent original = spec.getAsyncApiComponents().orElse(null);
        Map<String, AsyncApiSchema> specSchemas = original != null && original.schemas() != null
                ? original.schemas() : Map.of();
        this.renames = reservedNameRenames(specSchemas.keySet());
        this.schemas = renames.schemas(specSchemas);
        this.messages = new LinkedHashMap<>();
        if (original != null && original.messages() != null) {
            original.messages().forEach((name, message) -> messages.put(name, renames.message(message)));
        }
        for (String schemaName : schemas.keySet()) {
            takenTypeNames.add(CodegenUtils.getValidName(schemaName.trim(), true));
        }

        // Name every payload before rewriting replies, so an inline reply can match an existing reply message
        this.originalMessages = new LinkedHashMap<>(messages);
        for (Map.Entry<String, AsyncApiMessage> entry : originalMessages.entrySet()) {
            messages.put(entry.getKey(), withNamedPayload(entry.getKey(), entry.getValue()));
        }
        for (String messageName : originalMessages.keySet()) {
            messages.put(messageName, withResponseRefs(messageName, messages.get(messageName)));
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

    /**
     * Renames each spec schema whose type name the client generator writes itself, such as {@code Message}, to the
     * next free name, so the spec's type isn't replaced by the generated one.
     */
    private static SchemaRenames reservedNameRenames(Set<String> schemaNames) {
        Set<String> taken = new HashSet<>(GENERATED_TYPE_NAMES);
        schemaNames.forEach(name -> taken.add(CodegenUtils.getValidName(name.trim(), true)));
        Map<String, String> result = new HashMap<>();
        for (String name : schemaNames) {
            String typeName = CodegenUtils.getValidName(name.trim(), true);
            if (GENERATED_TYPE_NAMES.contains(typeName)) {
                String free = freeName(typeName, taken);
                taken.add(free);
                result.put(name, free);
            }
        }
        return new SchemaRenames(result);
    }

    /**
     * Copies the operations with each message payload named.
     */
    private Map<String, AsyncApiOperation> nameOperations(Map<String, AsyncApiOperation> original) {
        // A 2.x inline message without a name, title or ref is keyed by its position, which can't name a function
        Set<String> messageNames = new HashSet<>();
        original.values().stream().filter(op -> op.messages() != null)
                .forEach(op -> op.messages().keySet().stream().filter(key -> !isPosition(key))
                        .forEach(key -> messageNames.add(CodegenUtils.getValidName(key, true))));
        Map<String, AsyncApiOperation> result = new LinkedHashMap<>();
        int unnamed = 0;
        for (Map.Entry<String, AsyncApiOperation> entry : original.entrySet()) {
            AsyncApiOperation op = entry.getValue();
            if (op.messages() == null) {
                result.put(entry.getKey(), op);
                continue;
            }
            Map<String, AsyncApiMessage> opMessages = new LinkedHashMap<>();
            for (Map.Entry<String, AsyncApiMessage> msgEntry : op.messages().entrySet()) {
                AsyncApiMessage message = renames.message(msgEntry.getValue());
                String key = msgEntry.getKey();
                if (isPosition(key)) {
                    String name = positionalMessageName(message);
                    if (name != null) {
                        key = freeName(name, messageNames);
                    } else {
                        do {
                            key = "Message" + ++unnamed;
                        } while (messageNames.contains(key));
                    }
                    messageNames.add(key);
                }
                opMessages.put(key, name(key, message));
            }
            result.put(entry.getKey(), new AsyncApiOperation(op.action(), op.channelId(), op.channel(), op.title(),
                    op.summary(), op.description(), opMessages, op.security(), op.reply(), op.tags(),
                    op.externalDocs(), op.bindings(), op.traits(), op.extensions()));
        }
        return result;
    }

    private static boolean isPosition(String key) {
        return key.matches("\\d+");
    }

    /**
     * Names a message keyed by its position after the dispatcher value its payload fixes, else its payload's title.
     *
     * @return the name, or {@code null} if the payload gives none
     */
    private String positionalMessageName(AsyncApiMessage message) {
        AsyncApiSchema payload = message.payload() instanceof AsyncApiSchema schema ? schema : null;
        String value = CodegenUtils.getFixedDispatchValue(payload, dispatcherKey);
        if (value == null && payload != null) {
            value = payload.title();
        }
        // A value without a letter, such as "1", can't start a name either
        if (value != null && value.matches("(?s).*\\p{L}.*")) {
            String name = CodegenUtils.getValidName(value.trim(), true);
            if (Character.isLetter(name.charAt(0))) {
                return name;
            }
        }
        return null;
    }

    /**
     * An operation message that is the component message of the same name reuses its names. Matching by name
     * first keeps two messages with equal content apart.
     */
    private AsyncApiMessage name(String messageName, AsyncApiMessage message) {
        if (message.equals(originalMessages.get(messageName))) {
            return messages.get(messageName);
        }
        if (message.equals(operationOriginals.get(messageName))) {
            return operationNamed.get(messageName);
        }
        // Listed under a channel key that differs from its component key
        List<String> sameContent = originalMessages.entrySet().stream()
                .filter(entry -> entry.getValue().equals(message)).map(Map.Entry::getKey).toList();
        if (sameContent.size() == 1) {
            return messages.get(sameContent.get(0));
        }
        AsyncApiMessage result = withResponseRefs(messageName, withNamedPayload(messageName, message));
        operationOriginals.put(messageName, message);
        operationNamed.put(messageName, result);
        return result;
    }

    /**
     * Moves an unnamed payload into {@code components.schemas} under its message name, unless it is a named type with
     * only its dispatcher value pinned, and records the dispatcher value the payload fixes.
     */
    private AsyncApiMessage withNamedPayload(String messageName, AsyncApiMessage message) {
        if (!(message.payload() instanceof AsyncApiSchema payload) || CodegenUtils.isCloseFrameSchema(payload)) {
            return message;
        }
        if (payload.name() != null) {
            return withDispatchValue(message, schemas.get(payload.name()));
        }
        AsyncApiSchema constraint = dispatcherConstraint(payload);
        if (constraint != null) {
            List<AsyncApiSchema> allOf = payload.allOf();
            AsyncApiSchema type = allOf.get(0) == constraint ? allOf.get(1) : allOf.get(0);
            AsyncApiMessage typed = withPayload(message, schemas.get(type.name()).withName(type.name()),
                    message.extensions());
            return withDispatchValue(typed, constraint);
        }
        String schemaName = addSchema(messageName, payload);
        return withDispatchValue(withPayload(message, payload.withName(schemaName), message.extensions()), payload);
    }

    /**
     * The part of an {@code allOf} payload that fixes only the dispatcher field, when the other part is a named
     * schema, so the payload is still that named type. {@code null} for any other payload.
     */
    private AsyncApiSchema dispatcherConstraint(AsyncApiSchema payload) {
        List<AsyncApiSchema> allOf = payload.allOf();
        if (dispatcherKey == null || allOf == null || allOf.size() != 2) {
            return null;
        }
        AsyncApiSchema type = allOf.get(0).name() != null ? allOf.get(0) : allOf.get(1);
        AsyncApiSchema constraint = type == allOf.get(0) ? allOf.get(1) : allOf.get(0);
        // The named type has to declare the dispatcher field, so the client can set it before sending
        AsyncApiSchema named = type.name() == null ? null : schemas.get(type.name());
        if (named == null || named.properties() == null || !named.properties().containsKey(dispatcherKey)
                || constraint.name() != null || constraint.properties() == null
                || !constraint.properties().keySet().equals(Set.of(dispatcherKey))) {
            return null;
        }
        return constraint;
    }

    /**
     * Records the value {@code schema} fixes for the dispatcher field as {@link #X_DISPATCHER_VALUE}, so the client
     * sends that value whatever the caller passes.
     */
    private AsyncApiMessage withDispatchValue(AsyncApiMessage message, AsyncApiSchema schema) {
        String value = CodegenUtils.getFixedDispatchValue(schema, dispatcherKey);
        if (value == null) {
            return message;
        }
        Map<String, JsonNode> extensions = message.extensions() == null
                ? new LinkedHashMap<>() : new LinkedHashMap<>(message.extensions());
        extensions.put(X_DISPATCHER_VALUE, TextNode.valueOf(value));
        return withPayload(message, message.payload(), extensions);
    }

    /**
     * Replaces each inline {@code x-response} payload, directly or in a {@code oneOf}, with a reference to a component
     * message.
     */
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

    /**
     * Points an inline reply at a component message, reusing {@code <name>Response} when it carries the same
     * payload.
     */
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

    /**
     * Whether {@code existing} carries {@code payload}: the same named schema, or an equal inline one.
     */
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

    /**
     * Adds {@code schema} to {@code components.schemas} under a free name taken from the message name, and returns that
     * name.
     */
    private String addSchema(String messageName, AsyncApiSchema schema) {
        String schemaName = freeName(CodegenUtils.getValidName(messageName.trim(), true), takenTypeNames);
        schemas.put(schemaName, schema);
        takenTypeNames.add(schemaName);
        return schemaName;
    }

    /**
     * Returns {@code base}, numbered from 2 until it is not in {@code taken}.
     */
    private static String freeName(String base, Set<String> taken) {
        String candidate = base;
        for (int i = 2; taken.contains(candidate); i++) {
            candidate = base + i;
        }
        return candidate;
    }

    /**
     * Copies {@code m} with the given payload and extensions.
     */
    private static AsyncApiMessage withPayload(AsyncApiMessage m, Object payload, Map<String, JsonNode> extensions) {
        return new AsyncApiMessage(m.headers(), payload, m.correlationId(), m.contentType(), m.name(), m.title(),
                m.summary(), m.description(), m.tags(), m.externalDocs(), m.bindings(), m.examples(), m.traits(),
                extensions);
    }

    /**
     * Copies the components with the named schemas and messages, creating components when the spec had none.
     */
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
