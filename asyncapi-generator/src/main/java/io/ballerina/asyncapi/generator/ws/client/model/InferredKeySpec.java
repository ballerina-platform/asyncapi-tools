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
import io.ballerina.asyncapi.generator.ws.client.utils.ContentRoutes;
import io.ballerina.asyncapi.generator.ws.client.utils.IncomingMessages;
import org.semver4j.Semver;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
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
            + "Add x-dispatcherKey naming the field to route on, such as:";
    private static final String FIXED_VALUE = "a different fixed string value (a const or a one-value enum)";
    private static final String SEE_DOCS = "See " + CodegenUtils.DISPATCHER_KEY_DOCS
            + " for how the client tells messages apart.";
    private static final List<String> USUAL_KEYS = List.of("type", "event", "op", "action", "kind", "method");
    private static final String DEFAULT_KEY = "type";
    // Where a reply a call waits for is defined
    private static final String COMPONENT_MESSAGES = "/components/messages/";

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
        // Keys the spec names as paths are what its messages are routed on, so no one field is inferred
        boolean namedKeys = KeyPathSpec.routesByKeys(spec);
        List<String> candidates = namedKeys ? List.of() : candidates(List.of(allRecords(spec, schemas)), schemas);
        if (candidates.size() == 1) {
            String key = candidates.get(0);
            warnings.accept(new SpecWarning("", String.format("the spec has no x-dispatcherKey, so the client routes "
                    + "messages on '%s', the only field with a different fixed value in every message", key),
                    String.format("Add 'x-dispatcherKey: %s' to the spec to make it explicit.", key)));
            return new InferredKeySpec(spec, key);
        }
        // Only a connection that receives more than one kind of message has to tell them apart
        List<IncomingMessages.Connection> keyed = new ArrayList<>();
        List<List<IncomingMessages.Received>> receivedOn = new ArrayList<>();
        List<List<AsyncApiSchema>> received = new ArrayList<>();
        for (IncomingMessages.Connection connection : IncomingMessages.connections(spec)) {
            List<IncomingMessages.Received> messages = IncomingMessages.received(spec, connection.operations());
            if (messages.size() > 1) {
                keyed.add(connection);
                receivedOn.add(messages);
                received.add(messages.stream().map(IncomingMessages.Received::payload)
                        .filter(InferredKeySpec::isRoutableRecord).toList());
            }
        }
        if (keyed.isEmpty()) {
            return spec;
        }
        candidates = namedKeys ? List.of() : candidates(received, schemas);
        if (candidates.isEmpty()) {
            // With no one key, each connection's messages may still be told apart by what they carry
            String stop = routeByContent(spec, keyed, receivedOn, schemas, warnings);
            if (stop == null) {
                return spec;
            }
            throw new GeneratorException(stop);
        }
        if (candidates.size() != 1) {
            throw new GeneratorException(ambiguousKeyMessage(candidates));
        }
        String key = candidates.get(0);
        warnings.accept(new SpecWarning("", String.format("the spec has no x-dispatcherKey, so the client routes "
                + "received messages on '%s', the only field with a different fixed value in every message a "
                + "connection receives", key),
                String.format("Add 'x-dispatcherKey: %s' to the spec to make it explicit.", key)));
        return new InferredKeySpec(spec, key);
    }

    /**
     * Routes each connection's received messages by their content, warning about each route and each message left
     * out, or returns why a connection can't be routed. A connection can't be when none of its messages can be told
     * apart, or when a reply a call waits for can't be.
     */
    private static String routeByContent(AsyncApiSpec spec, List<IncomingMessages.Connection> keyed,
                                         List<List<IncomingMessages.Received>> receivedOn,
                                         Map<String, AsyncApiSchema> schemas, Consumer<SpecWarning> warnings) {
        List<ContentRoutes> routes = new ArrayList<>();
        for (int i = 0; i < keyed.size(); i++) {
            ContentRoutes connectionRoutes = ContentRoutes.of(spec, keyed.get(i).operations());
            boolean replyLeftOut = connectionRoutes.leftOut().stream()
                    .anyMatch(message -> message.location().startsWith(COMPONENT_MESSAGES));
            if (!connectionRoutes.routable() || connectionRoutes.routes().isEmpty() || replyLeftOut) {
                return noFieldOnChannel(keyed.get(i).channelName(), receivedOn.get(i), schemas);
            }
            routes.add(connectionRoutes);
        }
        for (int i = 0; i < keyed.size(); i++) {
            String channel = keyed.get(i).channelName();
            String location = "/channels/" + channel.replace("~", "~0").replace("/", "~1");
            List<ContentRoutes.Route> connectionRoutes = routes.get(i).routes().stream()
                    .filter(route -> !route.named()).toList();
            // A route that follows a key the spec names needs no announcing
            String key = sharedKey(connectionRoutes);
            if (key != null) {
                warnings.accept(new SpecWarning(location, String.format("the spec has no x-dispatcherKey, so the "
                        + "client routes the messages received on channel '%s' on '%s', the field with a different "
                        + "fixed value in each", channel, key), String.format("Name '%s' in x-dispatcherKey to make it "
                        + "explicit, if the messages every channel receives carry it. See %s", key,
                        CodegenUtils.DISPATCHER_KEY_DOCS)));
            } else if (!connectionRoutes.isEmpty()) {
                List<String> fields = connectionRoutes.stream().map(route -> route.value() == null
                        ? String.format("'%s' for '%s'", route.path().get(0), route.message().name())
                        : String.format("'%s' = '%s' for '%s'", route.path().get(0), route.value(),
                        route.message().name())).toList();
                warnings.accept(new SpecWarning(location, String.format("the spec has no x-dispatcherKey, so the "
                        + "client tells apart the messages received on channel '%s' by a field only each has: %s",
                        channel, String.join(", ", fields)), String.format("Give each a field with its own fixed "
                        + "value and name it in x-dispatcherKey if a frame can carry another message's field. See %s",
                        CodegenUtils.DISPATCHER_KEY_DOCS)));
            }
            for (IncomingMessages.Received message : routes.get(i).leftOut()) {
                List<String> path = ContentRoutes.keyPath(spec, message);
                if (path != null) {
                    warnings.accept(namedKeyLeftOut(message, channel, path));
                    continue;
                }
                warnings.accept(new SpecWarning(message.location(), String.format("message '%s' received on "
                        + "channel '%s' has no field only it has and no field with its own fixed value, so the "
                        + "client can't tell it apart from the others and leaves it out", message.name(), channel),
                        String.format("Give it a required field the channel's other messages don't declare, or give "
                        + "each of them a field with its own fixed value. See %s", CodegenUtils.DISPATCHER_KEY_DOCS)));
            }
        }
        return null;
    }

    // The one field every route the tool worked out on a connection looks at for a fixed value, if they share one
    private static String sharedKey(List<ContentRoutes.Route> routes) {
        if (routes.isEmpty() || routes.stream().anyMatch(route -> route.value() == null || route.path().size() != 1)) {
            return null;
        }
        String field = routes.get(0).path().get(0);
        return routes.stream().allMatch(route -> route.path().get(0).equals(field)) ? field : null;
    }

    // A message whose key names a path it has no fixed value at, or the value another received message fixes there
    private static SpecWarning namedKeyLeftOut(IncomingMessages.Received message, String channel, List<String> path) {
        String pointer = "/" + String.join("/", path.stream().map(name -> name.replace("~", "~0")
                .replace("/", "~1")).toList());
        StringBuilder fix = new StringBuilder(String.format("Give its payload a fixed value of its own at '%s', "
                + "such as:", pointer));
        // Each object on the path, then the field itself, as the payload's properties
        String indent = "\n  ";
        for (int i = 0; i < path.size() - 1; i++) {
            fix.append(indent).append(path.get(i)).append(':').append(indent).append("  type: object")
                    .append(indent).append("  properties:");
            indent += "    ";
        }
        fix.append(indent).append(String.format("%s: {type: string, const: %s}", path.get(path.size() - 1),
                message.name().toLowerCase(Locale.ROOT)));
        fix.append("\nSee ").append(CodegenUtils.DISPATCHER_KEY_DOCS);
        return new SpecWarning(message.location(), String.format("message '%s' received on channel '%s' has no "
                + "fixed value of its own at '%s', the dispatcher key it's routed on, so the client can't tell it "
                + "apart from the others and leaves it out", message.name(), channel, pointer), fix.toString());
    }

    // Several fields could be the key, so the one the messages are routed on has to be named
    private static String ambiguousKeyMessage(List<String> candidates) {
        return String.format(KEY_REQUIRED + "\n  x-dispatcherKey: %s\n%s", String.format("%s each have a "
                + "different fixed value in every message a connection receives, so it isn't clear which one "
                + "the messages are routed on", quotedList(candidates)), candidates.get(0), SEE_DOCS);
    }

    // The messages a channel receives, where each is received, and a field that would tell them apart
    private static String noFieldOnChannel(String channel, List<IncomingMessages.Received> messages,
                                           Map<String, AsyncApiSchema> schemas) {
        String field = suggestedField(messages, schemas);
        StringBuilder text = new StringBuilder(String.format("x-dispatcherKey extension is required in the AsyncAPI "
                + "spec, and no field has %s in every message received on channel '%s', so the client can't tell "
                + "them apart:", FIXED_VALUE, channel));
        for (IncomingMessages.Received message : messages) {
            text.append(String.format("\n  - '%s', received at %s%s", message.name(), message.location(),
                    isRoutableRecord(message.payload()) ? "" : " (not a JSON object, so it can't carry a field)"));
        }
        text.append(String.format("\nGive each received JSON object a field with its own fixed value, and name "
                + "the field in x-dispatcherKey, such as:\n  x-dispatcherKey: %s", field));
        for (IncomingMessages.Received message : messages) {
            if (isRoutableRecord(message.payload())) {
                text.append(String.format("\n  %s payload: %s: {type: string, const: %s}", message.name(), field,
                        message.name().toLowerCase(Locale.ROOT)));
            }
        }
        return text.append("\n").append(SEE_DOCS).toString();
    }

    // A usual key name every received object already declares, such as 'event'; else 'type'
    private static String suggestedField(List<IncomingMessages.Received> messages,
                                         Map<String, AsyncApiSchema> schemas) {
        List<Set<String>> declared = new ArrayList<>();
        for (IncomingMessages.Received message : messages) {
            if (isRoutableRecord(message.payload())) {
                declared.add(stringFields(message.payload(), schemas));
            }
        }
        List<String> shared = declared.isEmpty() ? List.of() : declared.get(0).stream()
                .filter(name -> declared.stream().allMatch(fields -> fields.contains(name))).toList();
        // Any other shared field holds data, so giving it a fixed value would change what the messages say
        return USUAL_KEYS.stream().filter(shared::contains).findFirst().orElse(DEFAULT_KEY);
    }

    private static Set<String> stringFields(AsyncApiSchema schema, Map<String, AsyncApiSchema> schemas) {
        Set<String> names = new LinkedHashSet<>();
        if (schema.allOf() != null) {
            for (AsyncApiSchema part : schema.allOf()) {
                AsyncApiSchema resolved = part.name() != null && schemas.containsKey(part.name())
                        ? schemas.get(part.name()) : part;
                names.addAll(stringFields(resolved, schemas));
            }
        }
        if (schema.properties() != null) {
            schema.properties().forEach((name, field) -> {
                if (field != null && (field.type() == null || STRING.equals(field.type()))) {
                    names.add(name);
                }
            });
        }
        return names;
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
