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
import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.ws.client.utils.CodegenUtils;
import io.ballerina.asyncapi.generator.ws.client.utils.IncomingMessages;
import org.semver4j.Semver;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static io.ballerina.asyncapi.generator.ws.client.extractor.DispatcherKeyExtractor.X_DISPATCHER_KEY;

/**
 * A view of a spec without {@code x-dispatcherKey} that uses the field telling its messages apart as the key.
 * The field must have a fixed string value, a {@code const} or a one-value {@code enum}, in every record message,
 * and a different one in each. When no field does that for every message, only the messages each connection
 * receives have to be told apart, and a connection that receives one kind of message needs no key. Exactly one
 * such field is used, with a warning; none or several stop generation, as picking one would be a guess.
 */
public final class InferredKeySpec implements AsyncApiSpec {

    private static final String STRING = "string";
    private static final String OBJECT = "object";
    private static final String KEY_REQUIRED = "x-dispatcherKey extension is required in the AsyncAPI spec, and %s. "
            + "Add x-dispatcherKey naming the field that tells the messages apart.";
    private static final String FIXED_VALUE = "a different fixed string value (a const or a one-value enum)";

    private final AsyncApiSpec spec;
    private final Map<String, JsonNode> extensions;

    private InferredKeySpec(AsyncApiSpec spec, String key) {
        this.spec = spec;
        this.extensions = new LinkedHashMap<>(spec.getAsyncApiExtensions().orElse(Map.of()));
        this.extensions.put(X_DISPATCHER_KEY, TextNode.valueOf(key));
    }

    /**
     * Returns {@code spec} with a dispatcher key: its own {@code x-dispatcherKey}, the one field that tells its
     * messages apart, or the one field that tells apart the messages each connection receives. A spec whose
     * connections each receive one kind of message needs no key, so it is returned as it is.
     *
     * @param spec     the parsed spec
     * @param warnings where to report an inferred key
     * @return the spec, or a view of it with the inferred key
     * @throws GeneratorException if the spec has no {@code x-dispatcherKey}, a connection receives more than one
     *                            kind of message, and no single field can stand in for the key
     */
    public static AsyncApiSpec of(AsyncApiSpec spec, Consumer<SpecWarning> warnings) throws GeneratorException {
        if (spec == null || spec.getAsyncApiExtensions().map(ext -> ext.containsKey(X_DISPATCHER_KEY)).orElse(false)) {
            return spec;
        }
        Map<String, AsyncApiSchema> schemas = spec.getAsyncApiComponents().map(AsyncApiComponent::schemas)
                .orElse(Map.of());
        List<String> candidates = candidates(List.of(allRecords(spec, schemas)), schemas);
        if (candidates.size() == 1) {
            String key = candidates.get(0);
            warnings.accept(new SpecWarning("", String.format("the spec has no x-dispatcherKey, so the client routes "
                    + "messages on '%s', the only field with a different fixed value in every message", key),
                    String.format("Add 'x-dispatcherKey: %s' to the spec to make it explicit.", key)));
            return new InferredKeySpec(spec, key);
        }
        // Only a connection that receives more than one kind of message has to tell them apart
        List<IncomingMessages.Connection> keyed = new ArrayList<>();
        List<List<AsyncApiSchema>> received = new ArrayList<>();
        for (IncomingMessages.Connection connection : IncomingMessages.connections(spec)) {
            List<AsyncApiSchema> payloads = IncomingMessages.payloads(spec, connection.operations());
            if (payloads.size() > 1) {
                keyed.add(connection);
                received.add(payloads.stream().filter(InferredKeySpec::isRoutableRecord).toList());
            }
        }
        if (keyed.isEmpty()) {
            return spec;
        }
        candidates = candidates(received, schemas);
        if (candidates.size() != 1) {
            throw new GeneratorException(String.format(KEY_REQUIRED,
                    noKeyReason(candidates, keyed, received, schemas)));
        }
        String key = candidates.get(0);
        warnings.accept(new SpecWarning("", String.format("the spec has no x-dispatcherKey, so the client routes "
                + "received messages on '%s', the only field with a different fixed value in every message a "
                + "connection receives", key),
                String.format("Add 'x-dispatcherKey: %s' to the spec to make it explicit.", key)));
        return new InferredKeySpec(spec, key);
    }

    // Why no field can be the key, naming a channel whose received messages no field tells apart, if there is one
    private static String noKeyReason(List<String> candidates, List<IncomingMessages.Connection> keyed,
                                      List<List<AsyncApiSchema>> received, Map<String, AsyncApiSchema> schemas) {
        if (!candidates.isEmpty()) {
            return String.format("%s each have a different fixed value in every message a connection receives, so "
                    + "it isn't clear which one the messages are routed on", quotedList(candidates));
        }
        for (int i = 0; i < keyed.size(); i++) {
            if (candidates(List.of(received.get(i)), schemas).isEmpty()) {
                return String.format("no field has %s in every message received on channel '%s'", FIXED_VALUE,
                        keyed.get(i).channelName());
            }
        }
        return String.format("no one field has %s in every message received on each of the channels %s",
                FIXED_VALUE, quotedList(keyed.stream().map(IncomingMessages.Connection::channelName).toList()));
    }

    // The record messages the spec sends or receives; a message sent and received is one message, so counts once
    private static List<AsyncApiSchema> allRecords(AsyncApiSpec spec, Map<String, AsyncApiSchema> schemas) {
        Set<AsyncApiSchema> records = new LinkedHashSet<>();
        for (AsyncApiOperation op : spec.getAsyncApiOperations().orElse(Map.of()).values()) {
            if (op.messages() == null) {
                continue;
            }
            for (AsyncApiMessage message : op.messages().values()) {
                if (message.payload() instanceof AsyncApiSchema payload) {
                    AsyncApiSchema schema = payload.name() != null && schemas.containsKey(payload.name())
                            ? schemas.get(payload.name()) : payload;
                    if (isRoutableRecord(schema)) {
                        records.add(schema);
                    }
                }
            }
        }
        return new ArrayList<>(records);
    }

    private static boolean isRoutableRecord(AsyncApiSchema schema) {
        return isRecord(schema) && schema.oneOf() == null && !CodegenUtils.isCloseFrameSchema(schema);
    }

    // The fields with a fixed string value in every record of every group, different in each record of a group
    private static List<String> candidates(List<List<AsyncApiSchema>> groups, Map<String, AsyncApiSchema> schemas) {
        List<List<Map<String, String>>> fixedValues = new ArrayList<>();
        for (List<AsyncApiSchema> group : groups) {
            if (group.isEmpty()) {
                return List.of();
            }
            List<Map<String, String>> groupValues = new ArrayList<>();
            for (AsyncApiSchema record : group) {
                groupValues.add(fixedStringValues(record, schemas));
            }
            fixedValues.add(groupValues);
        }
        if (fixedValues.isEmpty()) {
            return List.of();
        }
        List<String> candidates = new ArrayList<>();
        for (String field : fixedValues.get(0).get(0).keySet()) {
            if (fixedValues.stream().allMatch(group -> separates(field, group))) {
                candidates.add(field);
            }
        }
        return candidates;
    }

    private static boolean separates(String field, List<Map<String, String>> group) {
        Set<String> values = new HashSet<>();
        for (Map<String, String> fields : group) {
            String value = fields.get(field);
            if (value == null || !values.add(value)) {
                return false;
            }
        }
        return true;
    }

    // The schema's fields that fix a string value, including those from its allOf parts
    private static Map<String, String> fixedStringValues(AsyncApiSchema schema, Map<String, AsyncApiSchema> schemas) {
        Map<String, String> values = new LinkedHashMap<>();
        if (schema.allOf() != null) {
            for (AsyncApiSchema part : schema.allOf()) {
                AsyncApiSchema resolved = part.name() != null && schemas.containsKey(part.name())
                        ? schemas.get(part.name()) : part;
                fixedStringValues(resolved, schemas).forEach(values::putIfAbsent);
            }
        }
        if (schema.properties() != null) {
            for (Map.Entry<String, AsyncApiSchema> field : schema.properties().entrySet()) {
                // A field that is a $ref keeps only the name, so its value is in the referenced schema
                AsyncApiSchema fieldSchema = field.getValue();
                if (fieldSchema != null && fieldSchema.name() != null && schemas.containsKey(fieldSchema.name())) {
                    fieldSchema = schemas.get(fieldSchema.name());
                }
                String value = fixedStringValue(fieldSchema);
                if (value != null) {
                    values.put(field.getKey(), value);
                }
            }
        }
        return values;
    }

    private static String fixedStringValue(AsyncApiSchema field) {
        if (field == null || field.type() != null && !STRING.equals(field.type())) {
            return null;
        }
        if (field.constValue() != null) {
            return field.constValue().isTextual() ? field.constValue().asText() : null;
        }
        List<JsonNode> enumValues = field.enumValue();
        return enumValues != null && enumValues.size() == 1 && enumValues.get(0) != null
                && enumValues.get(0).isTextual() ? enumValues.get(0).asText() : null;
    }

    private static boolean isRecord(AsyncApiSchema schema) {
        return schema.type() == null ? schema.properties() != null || schema.allOf() != null
                : OBJECT.equals(schema.type());
    }

    private static String quotedList(List<String> names) {
        List<String> quoted = names.stream().map(name -> "'" + name + "'").toList();
        return String.join(", ", quoted.subList(0, quoted.size() - 1)) + " and " + quoted.get(quoted.size() - 1);
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
