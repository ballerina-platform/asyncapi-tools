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
import org.semver4j.Semver;

import java.net.URI;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * A view of a spec without its binary messages. The client sends and reads text frames, so a message whose content
 * type is binary, or whose payload is a {@code format: binary} string, is left out with a warning.
 */
public final class BinaryMessageSpec implements AsyncApiSpec {

    private static final String BINARY_FORMAT = "binary";
    private static final Set<String> BINARY_CONTENT_TYPES = Set.of("application/octet-stream", "application/x-binary",
            "application/protobuf", "application/x-protobuf", "application/vnd.google.protobuf", "application/cbor",
            "application/msgpack", "application/x-msgpack", "application/vnd.msgpack");
    private static final Set<String> BINARY_MEDIA = Set.of("image", "audio", "video");

    private final AsyncApiSpec spec;
    private final Map<String, AsyncApiOperation> operations;

    private BinaryMessageSpec(AsyncApiSpec spec, Map<String, AsyncApiOperation> operations) {
        this.spec = spec;
        this.operations = operations;
    }

    /**
     * Returns {@code spec} without the binary messages of its operations, warning about each one left out.
     *
     * @param spec     the spec
     * @param warnings where to report a message left out
     * @return the spec, or a view of it without those messages
     */
    public static AsyncApiSpec of(AsyncApiSpec spec, Consumer<SpecWarning> warnings) {
        if (spec == null || spec.getAsyncApiOperations().isEmpty()) {
            return spec;
        }
        String defaultContentType = spec.getAsyncApiContentType().orElse(null);
        Map<String, AsyncApiOperation> kept = new LinkedHashMap<>();
        Set<String> reported = new HashSet<>();
        boolean changed = false;
        for (Map.Entry<String, AsyncApiOperation> entry : spec.getAsyncApiOperations().get().entrySet()) {
            AsyncApiOperation op = entry.getValue();
            if (op == null || op.messages() == null) {
                kept.put(entry.getKey(), op);
                continue;
            }
            Map<String, AsyncApiMessage> messages = new LinkedHashMap<>();
            for (Map.Entry<String, AsyncApiMessage> message : op.messages().entrySet()) {
                String why = binaryReason(message.getValue(), defaultContentType);
                if (why == null) {
                    messages.put(message.getKey(), message.getValue());
                    continue;
                }
                changed = true;
                if (reported.add(message.getKey())) {
                    warnings.accept(new SpecWarning("/operations/" + pointerToken(entry.getKey()),
                            String.format("message '%s' is binary (%s), and the client sends and reads text "
                                    + "frames, so it leaves the message out", message.getKey(), why),
                            "Binary WebSocket frames aren't supported. If the server also takes the data as JSON "
                                    + "or text, describe that form of the message instead."));
                }
            }
            kept.put(entry.getKey(), messages.size() == op.messages().size() ? op
                    : new AsyncApiOperation(op.action(), op.channelId(), op.channel(), op.title(), op.summary(),
                    op.description(), messages, op.security(), op.reply(), op.tags(), op.externalDocs(),
                    op.bindings(), op.traits(), op.extensions()));
        }
        return changed ? new BinaryMessageSpec(spec, kept) : spec;
    }

    // Why the message is binary, or null when it isn't
    private static String binaryReason(AsyncApiMessage message, String defaultContentType) {
        if (message == null) {
            return null;
        }
        String contentType = message.contentType() != null ? message.contentType() : defaultContentType;
        if (contentType != null && isBinary(contentType)) {
            return "contentType '" + contentType + "'";
        }
        if (message.payload() instanceof AsyncApiSchema payload && "string".equals(payload.type())
                && BINARY_FORMAT.equals(payload.format())) {
            return "a 'format: binary' string payload";
        }
        return null;
    }

    private static boolean isBinary(String contentType) {
        String mediaType = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        int slash = mediaType.indexOf('/');
        return BINARY_CONTENT_TYPES.contains(mediaType)
                || (slash > 0 && BINARY_MEDIA.contains(mediaType.substring(0, slash)));
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
        return spec.getAsyncApiComponents();
    }

    @Override
    public Optional<Map<String, JsonNode>> getAsyncApiExtensions() {
        return spec.getAsyncApiExtensions();
    }
}
