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
package io.ballerina.asyncapi.generator.ws.client.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.core.model.component.AsyncApiSchema;
import io.ballerina.asyncapi.core.model.message.AsyncApiMessage;
import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.ws.client.utils.CodegenUtils;
import io.ballerina.asyncapi.generator.ws.client.utils.CommonFunctionUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static io.ballerina.asyncapi.generator.ws.client.extractor.DispatcherKeyExtractor.X_DISPATCHER_KEY;

/**
 * Computes the Ballerina return type string for a remote function based on
 * {@code x-response} and {@code x-response-type} message extensions.
 */
public class RemoteFunctionReturnTypeGenerator {

    private static final String DEFAULT_RETURN = "null";
    private static final String PIPE = "|";

    private final AsyncApiSpec asyncApiSpec;

    /**
     * Creates a new return type generator for the given spec.
     *
     * @param asyncApiSpec the parsed AsyncAPI spec
     */
    public RemoteFunctionReturnTypeGenerator(AsyncApiSpec asyncApiSpec) {
        this.asyncApiSpec = asyncApiSpec;
    }

    /**
     * Derives the Ballerina return type string from the {@code x-response} and
     * {@code x-response-type} extension values of a message.
     *
     * @param xResponse      the {@code x-response} extension value
     * @param xResponseType  the {@code x-response-type} extension value (may be {@code null})
     * @param responseMessages list to collect response schema names (may be {@code null})
     * @return the Ballerina return type string, or {@code "null"} if no response
     * @throws GeneratorException if the response schema structure is invalid
     */
    public String getReturnType(JsonNode xResponse, JsonNode xResponseType,
                                List<String> responseMessages) throws GeneratorException {
        ArrayList<String> returnTypes = new ArrayList<>();
        Map<String, AsyncApiMessage> messages = asyncApiSpec.getAsyncApiComponents()
                .map(c -> c.messages()).orElse(null);

        if (xResponse.get("oneOf") != null) {
            if (xResponse.get("oneOf") instanceof ArrayNode) {
                ArrayNode oneOfArray = (ArrayNode) xResponse.get("oneOf");
                if (xResponseType == null) {
                    throw new GeneratorException(
                            "x-response-type must be included ex:- "
                                    + "x-response-type: streaming || x-response-type: simple-rpc");
                }
                for (Iterator<JsonNode> it = oneOfArray.iterator(); it.hasNext();) {
                    JsonNode jsonNode = it.next();
                    if (jsonNode.get("$ref") != null) {
                        handleReferenceReturn(jsonNode, messages, responseMessages, returnTypes);
                    } else if (jsonNode.get("payload") != null) {
                        throw new GeneratorException(
                                "Ballerina service file cannot be generated for the given AsyncAPI "
                                        + "specification, Response type must be a Record");
                    }
                }
            }
        } else if (xResponse.get("$ref") != null) {
            handleReferenceReturn(xResponse, messages, responseMessages, returnTypes);
        } else if (xResponse.get("payload") != null) {
            throw new GeneratorException(
                    "Ballerina service file cannot be generated for the given AsyncAPI "
                            + "specification, Response type must be a Record");
        }

        if (!returnTypes.isEmpty()) {
            return String.join(PIPE, returnTypes);
        } else {
            return DEFAULT_RETURN;
        }
    }

    private void handleReferenceReturn(JsonNode jsonNode, Map<String, AsyncApiMessage> messages,
                                       List<String> responseMessages, List<String> returnTypes)
            throws GeneratorException {
        String dispatcherKey = asyncApiSpec.getAsyncApiExtensions()
                .map(ext -> {
                    JsonNode node = ext.get(X_DISPATCHER_KEY);
                    return node != null ? node.asText() : null;
                })
                .orElse(null);

        String reference = jsonNode.get("$ref").asText();
        String messageName = CodegenUtils.extractReferenceType(reference);

        if (messages == null || !messages.containsKey(messageName)) {
            throw new GeneratorException(
                    String.format("Message not found in components: %s", messageName));
        }
        AsyncApiMessage message = messages.get(messageName);
        Object payload = message.payload();
        if (!(payload instanceof AsyncApiSchema)) {
            throw new GeneratorException(
                    String.format("Message payload is not a schema for message: %s",
                            messageName));
        }
        AsyncApiSchema payloadSchema = (AsyncApiSchema) payload;
        String schemaName = payloadSchema.name() != null ? payloadSchema.name() : messageName;

        AsyncApiSchema refSchema = asyncApiSpec.getAsyncApiComponents()
                .map(c -> c.schemas() != null ? c.schemas().get(schemaName) : null)
                .orElse(null);

        if (responseMessages != null) {
            responseMessages.add(schemaName);
        }

        CommonFunctionUtils commonFunctionUtils = new CommonFunctionUtils(asyncApiSpec);
        if (dispatcherKey != null
                && !commonFunctionUtils.isDispatcherPresent(schemaName, refSchema, dispatcherKey, true)) {
            throw new GeneratorException(
                    String.format("dispatcherKey must be inside %s schema properties", schemaName));
        }
        returnTypes.add(CodegenUtils.getValidName(schemaName, true));
    }
}
