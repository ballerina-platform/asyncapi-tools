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
import io.ballerina.asyncapi.generator.http.model.DispatchTestCase;
import io.ballerina.asyncapi.generator.http.utils.CodegenUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Extracts one {@link DispatchTestCase} per event message in the spec, independent of and
 * without modifying {@link ServiceTypeExtractor}'s codegen-facing extraction -- this walks the
 * same raw operations/messages but additionally captures each message's bare {@code name}
 * (the real header value a delivery carries), which the codegen-facing models don't need and
 * don't carry.
 */
public final class DispatchTestCaseExtractor {

    private static final String X_BALLERINA_EVENT_TYPE = "x-ballerina-event-type";
    private static final String X_BALLERINA_EVENT_LABEL = "x-ballerina-event-label";
    private static final String X_BALLERINA_SERVICE_TYPE = "x-ballerina-service-type";

    private final AsyncApiSpec asyncApiSpec;

    public DispatchTestCaseExtractor(AsyncApiSpec asyncApiSpec) {
        this.asyncApiSpec = asyncApiSpec;
    }

    /**
     * Extracts one test case per event message defined in the spec.
     *
     * @return a non-null, possibly empty list of {@link DispatchTestCase} records
     * @throws GeneratorException if the spec is malformed
     */
    public List<DispatchTestCase> extract() throws GeneratorException {
        Map<String, AsyncApiOperation> operations = asyncApiSpec.getAsyncApiOperations().orElse(null);
        if (operations == null) {
            return Collections.emptyList();
        }

        List<DispatchTestCase> testCases = new ArrayList<>();

        for (AsyncApiOperation operation : operations.values()) {
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

            Map<String, AsyncApiMessage> messages = operation.messages();
            if (messages == null || messages.isEmpty()) {
                continue;
            }

            for (AsyncApiMessage message : messages.values()) {
                Map<String, JsonNode> extensions = message.extensions();
                if (extensions == null || !extensions.containsKey(X_BALLERINA_EVENT_TYPE)) {
                    continue;
                }
                String eventIdentifier = extensions.get(X_BALLERINA_EVENT_TYPE).asText();
                if (eventIdentifier.isBlank()) {
                    continue;
                }
                JsonNode labelNode = extensions.get(X_BALLERINA_EVENT_LABEL);
                String displayLabel = labelNode != null && !labelNode.asText().isBlank()
                        ? labelNode.asText() : null;
                String headerValue = message.name() != null && !message.name().isBlank()
                        ? message.name()
                        : eventIdentifier;

                Object payload = message.payload();
                String payloadTypeName = eventIdentifier;
                if (payload instanceof AsyncApiSchema schema && schema.name() != null
                        && !schema.name().isBlank()) {
                    payloadTypeName = schema.name();
                }
                payloadTypeName = CodegenUtils.getValidName(
                        CodegenUtils.escapeIdentifier(payloadTypeName.trim()), true);

                String namingBasis = displayLabel != null ? displayLabel : eventIdentifier;
                String functionName = CodegenUtils.getFunctionNameByEventName(namingBasis);
                String serviceTypeDeclName = CodegenUtils.getServiceTypeNameByServiceName(serviceTypeName);
                testCases.add(new DispatchTestCase(
                        serviceTypeDeclName, functionName, eventIdentifier, headerValue, payloadTypeName));
            }
        }

        return testCases;
    }
}
