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
import io.ballerina.asyncapi.core.model.channel.AsyncApiChannel;
import io.ballerina.asyncapi.core.model.component.AsyncApiSchema;
import io.ballerina.asyncapi.core.model.message.AsyncApiMessage;
import io.ballerina.asyncapi.core.model.operation.AsyncApiOperation;
import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.http.model.EventIdentifierConfig;
import io.ballerina.asyncapi.generator.http.model.HttpRemoteFunction;
import io.ballerina.asyncapi.generator.http.model.HttpServiceType;
import io.ballerina.asyncapi.generator.http.utils.CodegenUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extracts {@link HttpServiceType} definitions from an {@link AsyncApiSpec}.
 *
 */
public final class ServiceTypeExtractor {

    public static final String X_BALLERINA_EVENT_TYPE = "x-ballerina-event-type";
    private static final String X_BALLERINA_EVENT_LABEL = "x-ballerina-event-label";
    private static final String X_BALLERINA_SERVICE_TYPE = "x-ballerina-service-type";

    private final AsyncApiSpec asyncApiSpec;
    private final EventIdentifierConfig identifierConfig;
    private final Map<String, AsyncApiSchema> inlineSchemas = new HashMap<>();

    /**
     * Creates an extractor that does not have access to the resolved event identifier
     * configuration, so every remote function it produces matches on the composite identifier
     * (the pre-existing behavior). Prefer {@link #ServiceTypeExtractor(AsyncApiSpec,
     * EventIdentifierConfig)} so free-form (non-enumerable) action fields are detected.
     *
     * @param asyncApiSpec the parsed AsyncAPI specification
     */
    public ServiceTypeExtractor(AsyncApiSpec asyncApiSpec) {
        this(asyncApiSpec, null);
    }

    /**
     * Creates an extractor that uses the resolved event identifier configuration to detect,
     * for {@code "composite"} identifiers, which events have no enumerable action field and
     * therefore must match on the bare event type instead of the composite identifier.
     *
     * @param asyncApiSpec     the parsed AsyncAPI specification
     * @param identifierConfig the resolved event identifier type and path, or {@code null} if
     *                         not yet known (every remote function will match on the composite
     *                         identifier)
     */
    public ServiceTypeExtractor(AsyncApiSpec asyncApiSpec, EventIdentifierConfig identifierConfig) {
        this.asyncApiSpec = asyncApiSpec;
        this.identifierConfig = identifierConfig;
    }

    /**
     * Returns inline payload schemas collected during {@link #extract()}.
     * Keyed by the {@code x-ballerina-event-type} value of each message.
     *
     * @return a non-null, possibly empty map of event-type name to schema
     */
    public Map<String, AsyncApiSchema> getInlineSchemas() {
        return inlineSchemas;
    }

    /**
     * Extracts the list of HTTP service types defined in the spec.
     *
     * @return a non-null list of {@link HttpServiceType} objects; empty if no qualifying operations exist
     * @throws GeneratorException if the spec is malformed or required extensions are missing
     */
    public List<HttpServiceType> extract() throws GeneratorException {
        Map<String, AsyncApiOperation> operations = asyncApiSpec.getAsyncApiOperations().orElse(null);
        if (operations == null) {
            return Collections.emptyList();
        }

        Map<String, HttpServiceType> serviceTypeMap = new HashMap<>();

        for (Map.Entry<String, AsyncApiOperation> entry : operations.entrySet()) {
            AsyncApiOperation operation = entry.getValue();

            if (operation.action() != AsyncApiOperation.Action.RECEIVE) {
                continue;
            }

            AsyncApiChannel operationChannel = operation.channel();
            String serviceTypeName;
            String channelId = operation.channelId();
            if (channelId != null && !channelId.isBlank()) {
                serviceTypeName = channelId;
            } else {
                Map<String, JsonNode> channelExtensions = operationChannel.extensions();
                if (channelExtensions != null && channelExtensions.containsKey(X_BALLERINA_SERVICE_TYPE)) {
                    serviceTypeName = channelExtensions.get(X_BALLERINA_SERVICE_TYPE).asText();
                } else {
                    serviceTypeName = CodegenUtils.getValidName(operationChannel.address(), true);
                }
            }

            serviceTypeMap.computeIfAbsent(serviceTypeName, k -> new HttpServiceType(k, new ArrayList<>()));

            Map<String, AsyncApiMessage> messages = operation.messages();
            if (messages == null || messages.isEmpty()) {
                continue;
            }

            for (Map.Entry<String, AsyncApiMessage> msgEntry : messages.entrySet()) {
                String messageId = msgEntry.getKey();
                AsyncApiMessage message = msgEntry.getValue();
                validateMessage(message, messageId, operationChannel.address());
                String eventType = message.extensions().get(X_BALLERINA_EVENT_TYPE).asText();
                JsonNode labelNode = message.extensions().get(X_BALLERINA_EVENT_LABEL);
                String displayLabel = labelNode != null && !labelNode.asText().isBlank()
                        ? labelNode.asText() : null;
                Object payload = message.payload();
                String payloadTypeName = eventType;
                boolean matchOnEventType = false;
                if (payload instanceof AsyncApiSchema schema) {
                    String name = schema.name();
                    if (name != null && !name.isBlank()) {
                        payloadTypeName = name;
                    } else {
                        inlineSchemas.put(eventType, schema);
                    }
                    matchOnEventType = hasFreeFormActionField(schema);
                }
                serviceTypeMap.get(serviceTypeName).remoteFunctions()
                        .add(new HttpRemoteFunction(eventType, payloadTypeName, matchOnEventType, displayLabel));
            }
        }

        return new ArrayList<>(serviceTypeMap.values());
    }

    /**
     * Determines whether a message's payload schema has a free-form action field at the
     * configured identifier {@code path} -- present, but declaring no fixed {@code enum} of
     * possible values. Such a field's real value can never be known at generation time, so no
     * composite {@code eventType_action} literal can ever be safely written into the generated
     * match clause for it.
     *
     * <p>Returns {@code false} (no change from the pre-existing composite-match behavior) when
     * the identifier type isn't {@code "composite"}, or when the payload has no property at the
     * configured path at all -- in that case the field is simply absent from every real delivery,
     * so the composite identifier already degrades to the bare event type at runtime and matches
     * correctly without any special handling.
     *
     * @param payloadSchema the resolved payload schema for one message
     * @return {@code true} only if the field at {@code path} exists but declares no {@code enum}
     */
    private boolean hasFreeFormActionField(AsyncApiSchema payloadSchema) {
        if (identifierConfig == null
                || !EventIdentifierExtractor.X_BALLERINA_EVENT_TYPE_COMPOSITE.equals(identifierConfig.type())) {
            return false;
        }
        String path = identifierConfig.path();
        if (path == null || path.isBlank()) {
            return false;
        }
        AsyncApiSchema current = payloadSchema;
        for (String rawSegment : path.split("\\.")) {
            if (current == null || current.properties() == null) {
                return false;
            }
            String segment = rawSegment.startsWith("'") ? rawSegment.substring(1) : rawSegment;
            current = current.properties().get(segment);
        }
        if (current == null) {
            return false;
        }
        List<JsonNode> enumValue = current.enumValue();
        return enumValue == null || enumValue.isEmpty();
    }

    private void validateMessage(AsyncApiMessage message, String messageId, String channelAddress)
            throws GeneratorException {
        Map<String, JsonNode> extensions = message.extensions();
        if (extensions == null || !extensions.containsKey(X_BALLERINA_EVENT_TYPE)) {
            throw new GeneratorException(String.format(
                    "Could not find the %s attribute in the message '%s' of the channel %s",
                    X_BALLERINA_EVENT_TYPE, messageId, channelAddress));
        }
        String eventType = extensions.get(X_BALLERINA_EVENT_TYPE).asText();
        if (eventType.isBlank()) {
            throw new GeneratorException(String.format(
                    "Resolved event type is blank for message '%s' in channel %s",
                    messageId, channelAddress));
        }

        if (message.payload() == null) {
            throw new GeneratorException(String.format(
                    "Could not find the payload reference in the message of the channel %s",
                    channelAddress));
        }
    }
}
