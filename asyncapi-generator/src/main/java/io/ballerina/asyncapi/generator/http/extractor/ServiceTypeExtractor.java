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
import io.ballerina.asyncapi.generator.http.Constants;
import io.ballerina.asyncapi.generator.http.model.HttpRemoteFunction;
import io.ballerina.asyncapi.generator.http.model.HttpServiceType;
import io.ballerina.asyncapi.generator.http.utils.CodegenUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extracts {@link HttpServiceType} definitions from an {@link AsyncApiSpec}.
 *
 */
public final class ServiceTypeExtractor {

    private final AsyncApiSpec asyncApiSpec;
    private final Map<String, AsyncApiSchema> inlineSchemas = new HashMap<>();

    public ServiceTypeExtractor(AsyncApiSpec asyncApiSpec) {
        this.asyncApiSpec = asyncApiSpec;
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
            return new ArrayList<>();
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
                if (channelExtensions != null && channelExtensions.containsKey(Constants.X_BALLERINA_SERVICE_TYPE)) {
                    serviceTypeName = channelExtensions.get(Constants.X_BALLERINA_SERVICE_TYPE).asText();
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
                String eventType = message.extensions().get(Constants.X_BALLERINA_EVENT_TYPE).asText();
                Object payload = message.payload();
                String payloadTypeName = eventType;
                if (payload instanceof AsyncApiSchema schema) {
                    String name = schema.name();
                    if (name != null && !name.isBlank()) {
                        payloadTypeName = name;
                    } else {
                        inlineSchemas.put(eventType, schema);
                    }
                }
                serviceTypeMap.get(serviceTypeName).remoteFunctions()
                        .add(new HttpRemoteFunction(eventType, payloadTypeName));
            }
        }

        return new ArrayList<>(serviceTypeMap.values());
    }

    private void validateMessage(AsyncApiMessage message, String messageId, String channelAddress)
            throws GeneratorException {
        Map<String, JsonNode> extensions = message.extensions();
        if (extensions == null || !extensions.containsKey(Constants.X_BALLERINA_EVENT_TYPE)) {
            throw new GeneratorException("Could not find the " + Constants.X_BALLERINA_EVENT_TYPE
                    + " attribute in the message '" + messageId + "' of the channel " + channelAddress);
        }
        String eventType = extensions.get(Constants.X_BALLERINA_EVENT_TYPE).asText();
        if (eventType.isBlank()) {
            throw new GeneratorException("Resolved event type is blank for message '" + messageId
                    + "' in channel " + channelAddress);
        }

        if (message.payload() == null) {
            throw new GeneratorException("Could not find the payload reference in the message of the channel "
                    + channelAddress);
        }
    }
}
