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
import io.ballerina.asyncapi.core.SpecWarning;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static io.ballerina.asyncapi.generator.ws.client.extractor.DispatcherKeyExtractor.X_DISPATCHER_KEY;

/**
 * A view of a spec whose dispatcher key is described in a way the client can route on. Specs often describe the
 * key field loosely but clearly, so before generation:
 * <ul>
 *     <li>a key with only a {@code const} or {@code enum} of strings is read as {@code type: string};</li>
 *     <li>a key a payload doesn't list in {@code required} is made required, with a warning;</li>
 *     <li>a record message without the key is left out of its operation, with a warning, as the client can't
 *     tell it apart from the others.</li>
 * </ul>
 * Payloads that aren't records, such as a string, and {@code oneOf} payloads are left as they are.
 */
public final class DispatcherKeySpec implements AsyncApiSpec {

    private static final String STRING = "string";
    private static final String OBJECT = "object";

    private final AsyncApiSpec spec;
    private final String key;
    private final Consumer<SpecWarning> warnings;
    private final Set<String> specSchemaNames;
    private final Map<String, AsyncApiSchema> schemas;
    private final Set<String> warnedSchemas = new HashSet<>();
    // The operation whose message is being fixed up, to point a warning about an unnamed schema at
    private String currentOperation;
    private final Map<String, AsyncApiOperation> operations;
    private final AsyncApiComponent components;

    private DispatcherKeySpec(AsyncApiSpec spec, String key, Set<String> specSchemaNames,
                              Consumer<SpecWarning> warnings) {
        this.spec = spec;
        this.key = key;
        this.warnings = warnings;
        this.specSchemaNames = specSchemaNames;
        AsyncApiComponent original = spec.getAsyncApiComponents().orElse(null);
        this.schemas = original != null && original.schemas() != null
                ? new LinkedHashMap<>(original.schemas()) : new LinkedHashMap<>();
        this.operations = spec.getAsyncApiOperations().map(this::withRoutableMessages).orElse(null);
        this.components = withSchemas(original);
    }

    /**
     * Returns a view of {@code spec} with its dispatcher key fixed up, or {@code spec} itself when it states none.
     *
     * @param spec            the spec, with every payload named (see {@link NamedPayloadSpec})
     * @param specSchemaNames the schema names the spec itself defines, to point warnings at them
     * @param warnings        where to report the changes
     * @return the fixed-up view
     */
    public static AsyncApiSpec of(AsyncApiSpec spec, Set<String> specSchemaNames, Consumer<SpecWarning> warnings) {
        JsonNode keyNode = spec == null ? null
                : spec.getAsyncApiExtensions().map(ext -> ext.get(X_DISPATCHER_KEY)).orElse(null);
        if (keyNode == null || keyNode.asText().isBlank()) {
            return spec;
        }
        return new DispatcherKeySpec(spec, keyNode.asText(), specSchemaNames, warnings);
    }

    private Map<String, AsyncApiOperation> withRoutableMessages(Map<String, AsyncApiOperation> original) {
        Map<String, AsyncApiOperation> result = new LinkedHashMap<>();
        for (Map.Entry<String, AsyncApiOperation> entry : original.entrySet()) {
            AsyncApiOperation op = entry.getValue();
            if (op.messages() == null) {
                result.put(entry.getKey(), op);
                continue;
            }
            Map<String, AsyncApiMessage> kept = new LinkedHashMap<>();
            for (Map.Entry<String, AsyncApiMessage> msgEntry : op.messages().entrySet()) {
                AsyncApiMessage message = routable(entry.getKey(), msgEntry.getKey(), msgEntry.getValue());
                if (message != null) {
                    kept.put(msgEntry.getKey(), message);
                }
            }
            result.put(entry.getKey(), new AsyncApiOperation(op.action(), op.channelId(), op.channel(), op.title(),
                    op.summary(), op.description(), kept, op.security(), op.reply(), op.tags(),
                    op.externalDocs(), op.bindings(), op.traits(), op.extensions()));
        }
        return result;
    }

    /**
     * The message with its payload's key fixed up, or {@code null} when it's a record without the key.
     */
    private AsyncApiMessage routable(String operationId, String messageName, AsyncApiMessage message) {
        if (!(message.payload() instanceof AsyncApiSchema payload)) {
            return message;
        }
        AsyncApiSchema schema = payload.name() != null && schemas.containsKey(payload.name())
                ? schemas.get(payload.name()) : payload;
        if (!isRecord(schema) || schema.oneOf() != null || CodegenUtils.isCloseFrameSchema(schema)) {
            return message;
        }
        currentOperation = operationId;
        AsyncApiSchema fixed = withKey(payload.name(), schema, false);
        if (fixed == null) {
            warnings.accept(new SpecWarning("/operations/" + pointerToken(operationId),
                    String.format("message '%s' has no '%s' field, so the client can't tell it apart from the other "
                            + "messages and leaves it out", messageName, key),
                    String.format("Add a required '%s' string field to the message's payload.", key)));
            return null;
        }
        // A payload that carries its schema, rather than only its name, carries the fixed one
        if (fixed == schema || payload.properties() == null && payload.allOf() == null) {
            return message;
        }
        AsyncApiSchema newPayload = payload.name() != null ? fixed.withName(payload.name()) : fixed;
        return new AsyncApiMessage(message.headers(), newPayload, message.correlationId(), message.contentType(),
                message.name(), message.title(), message.summary(), message.description(), message.tags(),
                message.externalDocs(), message.bindings(), message.examples(), message.traits(),
                message.extensions());
    }

    /**
     * {@code schema} with its key field fixed up, or {@code null} when neither it nor its allOf parts declare the
     * key. A named schema's fix is also stored under its name, so every payload that uses it gets it.
     *
     * @param requiredAbove whether a schema that includes this one already requires the key
     */
    private AsyncApiSchema withKey(String name, AsyncApiSchema schema, boolean requiredAbove) {
        boolean required = requiredAbove || schema.required() != null && schema.required().contains(key);
        AsyncApiSchema fixed = null;
        if (schema.properties() != null && schema.properties().containsKey(key)) {
            fixed = withKeyField(name, schema, required);
        } else if (schema.allOf() != null) {
            List<AsyncApiSchema> parts = new ArrayList<>(schema.allOf());
            for (int i = 0; i < parts.size() && fixed == null; i++) {
                AsyncApiSchema part = parts.get(i);
                if (part.name() != null && schemas.containsKey(part.name())) {
                    // An included schema is fixed under its own name, so this one stays as it is
                    fixed = withKey(part.name(), schemas.get(part.name()), required) != null ? schema : null;
                } else {
                    AsyncApiSchema fixedPart = withKey(null, part, required);
                    if (fixedPart != null) {
                        parts.set(i, fixedPart);
                        fixed = fixedPart == part ? schema : withAllOf(schema, parts);
                    }
                }
            }
        }
        if (fixed != null && fixed != schema && name != null && schemas.containsKey(name)) {
            schemas.put(name, fixed);
        }
        return fixed;
    }

    // The key field read as a string when its const or enum only has strings, and required
    private AsyncApiSchema withKeyField(String name, AsyncApiSchema schema, boolean required) {
        AsyncApiSchema keySchema = schema.properties().get(key);
        AsyncApiSchema fixedKey = keySchema.type() == null && hasOnlyStringValues(keySchema)
                ? keySchema.withType(STRING) : keySchema;
        List<String> requiredNames = schema.required() == null ? new ArrayList<>() : new ArrayList<>(schema.required());
        if (!requiredNames.contains(key)) {
            requiredNames.add(key);
            if (!required) {
                warnNotRequired(name);
            }
        }
        if (fixedKey == keySchema && requiredNames.equals(schema.required())) {
            return schema;
        }
        Map<String, AsyncApiSchema> properties = new LinkedHashMap<>(schema.properties());
        properties.put(key, fixedKey);
        return schema.withProperties(properties, requiredNames);
    }

    private void warnNotRequired(String name) {
        String label = name != null ? name : "an inline schema";
        if (!warnedSchemas.add(label)) {
            return;
        }
        String location = name != null && specSchemaNames.contains(name)
                ? "/components/schemas/" + pointerToken(name) : "/operations/" + pointerToken(currentOperation);
        warnings.accept(new SpecWarning(location,
                String.format("'%s' isn't in the required list of %s, so the client treats it as required, as it "
                        + "routes each message on it", key, name != null ? "'" + name + "'" : label),
                String.format("Add '%s' to the schema's 'required' list.", key)));
    }

    // A copy of schema with other allOf parts
    private static AsyncApiSchema withAllOf(AsyncApiSchema s, List<AsyncApiSchema> allOf) {
        return new AsyncApiSchema(s.title(), s.type(), s.required(), s.multipleOf(), s.maximum(), s.exclusiveMaximum(),
                s.minimum(), s.exclusiveMinimum(), s.maxLength(), s.minLength(), s.pattern(), s.maxItems(),
                s.minItems(), s.uniqueItems(), s.maxProperties(), s.minProperties(), s.enumValue(), s.constValue(),
                s.examples(), s.ifBranch(), s.then(), s.elseBranch(), s.readOnly(), s.writeOnly(), s.properties(),
                s.patternProperties(), s.additionalProperties(), s.additionalItems(), s.items(), s.propertyNames(),
                s.contains(), allOf, s.oneOf(), s.anyOf(), s.not(), s.description(), s.format(), s.defaultValue(),
                s.discriminator(), s.externalDocs(), s.deprecated(), s.extensions(), s.name(), s.nullable());
    }

    private static boolean hasOnlyStringValues(AsyncApiSchema schema) {
        if (schema.constValue() != null) {
            return schema.constValue().isTextual();
        }
        return schema.enumValue() != null && !schema.enumValue().isEmpty()
                && schema.enumValue().stream().allMatch(value -> value != null && value.isTextual());
    }

    // An object, or a schema that only gives properties or allOf parts; a string, number or array has no fields
    private static boolean isRecord(AsyncApiSchema schema) {
        return schema.type() == null ? schema.properties() != null || schema.allOf() != null
                : OBJECT.equals(schema.type());
    }

    private static String pointerToken(String name) {
        return name.replace("~", "~0").replace("/", "~1");
    }

    private AsyncApiComponent withSchemas(AsyncApiComponent c) {
        if (c == null) {
            return null;
        }
        return new AsyncApiComponent(schemas, c.servers(), c.serverVariables(), c.channels(), c.operations(),
                c.messages(), c.securitySchemes(), c.parameters(), c.correlationIds(), c.operationTraits(),
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
