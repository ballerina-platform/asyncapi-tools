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
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.core.model.channel.AsyncApiChannel;
import io.ballerina.asyncapi.core.model.component.AsyncApiComponent;
import io.ballerina.asyncapi.core.model.info.AsyncApiInfo;
import io.ballerina.asyncapi.core.model.message.AsyncApiMessage;
import io.ballerina.asyncapi.core.model.operation.AsyncApiOperation;
import io.ballerina.asyncapi.core.model.server.AsyncApiServer;
import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.ws.client.utils.CodegenUtils;
import org.semver4j.Semver;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.ballerina.asyncapi.generator.ws.client.extractor.DispatcherKeyExtractor.X_DISPATCHER_KEY;

/**
 * A view of a spec whose {@code x-dispatcherKey} is a JSON Pointer, such as {@code /d/t}, or whose messages name a
 * key of their own with a message-level {@code x-dispatcherKey}.
 *
 * <p>A pointer to a top-level field, such as {@code /type}, is that field's name, so the spec is routed on it as
 * before. A nested pointer, or a message with a key of its own, can't be one field of the {@code Message} type every
 * frame is read as, so the view drops {@code x-dispatcherKey} and the client tells each received message apart by
 * the fixed value at its key's path instead (see {@code ContentRoutes}).
 */
public final class KeyPathSpec implements AsyncApiSpec {

    // The spec-wide key's path, and that the messages are routed by their keys, for ContentRoutes to read
    static final String X_KEY_PATH = "x-ballerina-dispatcherKeyPath";
    static final String X_ROUTE_BY_KEYS = "x-ballerina-routeByKeys";
    private static final String INVALID_KEY = "x-dispatcherKey '%s' at %s is not a field name or a JSON Pointer to "
            + "one, such as 'type' or '/data/type'. See " + CodegenUtils.DISPATCHER_KEY_DOCS;

    private final AsyncApiSpec spec;
    private final Map<String, JsonNode> extensions;

    private KeyPathSpec(AsyncApiSpec spec, Map<String, JsonNode> extensions) {
        this.spec = spec;
        this.extensions = extensions;
    }

    /**
     * Returns {@code spec} with its keys read as paths: unchanged when its key is a field name and no message has a
     * key of its own.
     *
     * @param spec the parsed spec
     * @return the spec, or a view of it
     * @throws GeneratorException if a key is neither a field name nor a JSON Pointer to one
     */
    public static AsyncApiSpec of(AsyncApiSpec spec) throws GeneratorException {
        if (spec == null) {
            return null;
        }
        Map<String, JsonNode> extensions = spec.getAsyncApiExtensions().orElse(Map.of());
        JsonNode keyNode = extensions.get(X_DISPATCHER_KEY);
        String key = keyNode != null && keyNode.isTextual() ? keyNode.asText() : null;
        List<String> path = key != null ? parse(key, "the spec's top level") : null;
        boolean ownKeys = false;
        for (Map.Entry<String, AsyncApiMessage> message : messages(spec).entrySet()) {
            String own = ownKey(message.getValue());
            if (own != null) {
                parse(own, message.getKey());
                ownKeys = true;
            }
        }
        if (path == null || !ownKeys && path.size() == 1 && !key.startsWith("/")) {
            return ownKeys ? routedByKeys(spec, extensions, null) : spec;
        }
        if (!ownKeys && path.size() == 1) {
            Map<String, JsonNode> named = new LinkedHashMap<>(extensions);
            named.put(X_DISPATCHER_KEY, TextNode.valueOf(path.get(0)));
            return new KeyPathSpec(spec, named);
        }
        return routedByKeys(spec, extensions, key);
    }

    /**
     * Returns whether {@code spec}'s received messages are told apart by the paths their keys name.
     *
     * @param spec the spec
     * @return whether the messages are routed by their keys
     */
    public static boolean routesByKeys(AsyncApiSpec spec) {
        return spec.getAsyncApiExtensions().map(ext -> ext.get(X_ROUTE_BY_KEYS)).map(JsonNode::asBoolean)
                .orElse(false);
    }

    /**
     * Returns the path of {@code spec}'s spec-wide key when its messages are routed by their keys.
     *
     * @param spec the spec
     * @return the path, or {@code null} when the spec has no key of its own
     */
    public static List<String> specKeyPath(AsyncApiSpec spec) {
        JsonNode key = spec.getAsyncApiExtensions().map(ext -> ext.get(X_KEY_PATH)).orElse(null);
        return key != null ? split(key.asText()) : null;
    }

    /**
     * Returns the key a message names for itself with a message-level {@code x-dispatcherKey}.
     *
     * @param message the message
     * @return the key as written, or {@code null} when it has none
     */
    public static String ownKey(AsyncApiMessage message) {
        JsonNode key = message != null && message.extensions() != null ? message.extensions().get(X_DISPATCHER_KEY)
                : null;
        return key != null && key.isTextual() ? key.asText() : null;
    }

    /**
     * Splits a key into the field names on its path: a field name is a path of one, and a JSON Pointer is decoded.
     *
     * @param key the key, already checked
     * @return the field names, outermost first
     */
    public static List<String> split(String key) {
        if (!key.startsWith("/")) {
            return List.of(key);
        }
        List<String> path = new ArrayList<>();
        for (String token : key.substring(1).split("/", -1)) {
            path.add(token.replace("~1", "/").replace("~0", "~"));
        }
        return path;
    }

    private static List<String> parse(String key, String where) throws GeneratorException {
        List<String> path = key.isBlank() || key.equals("/") ? List.of() : split(key);
        if (path.isEmpty() || path.stream().anyMatch(String::isEmpty)) {
            throw new GeneratorException(String.format(INVALID_KEY, key, where));
        }
        return path;
    }

    private static KeyPathSpec routedByKeys(AsyncApiSpec spec, Map<String, JsonNode> extensions, String key) {
        Map<String, JsonNode> routed = new LinkedHashMap<>(extensions);
        routed.remove(X_DISPATCHER_KEY);
        if (key != null) {
            routed.put(X_KEY_PATH, TextNode.valueOf(key));
        }
        routed.put(X_ROUTE_BY_KEYS, BooleanNode.TRUE);
        return new KeyPathSpec(spec, routed);
    }

    // The spec's messages by name: its component messages, then the ones written inline in an operation
    private static Map<String, AsyncApiMessage> messages(AsyncApiSpec spec) {
        Map<String, AsyncApiMessage> messages = new LinkedHashMap<>();
        spec.getAsyncApiComponents().map(AsyncApiComponent::messages).ifPresent(messages::putAll);
        for (AsyncApiOperation op : spec.getAsyncApiOperations().orElse(Map.of()).values()) {
            if (op.messages() != null) {
                op.messages().forEach(messages::putIfAbsent);
            }
        }
        return messages;
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
