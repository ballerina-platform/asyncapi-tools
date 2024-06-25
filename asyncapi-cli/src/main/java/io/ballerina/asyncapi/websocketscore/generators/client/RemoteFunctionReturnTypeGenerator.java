/*
 *  Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
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
package io.ballerina.asyncapi.websocketscore.generators.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.apicurio.datamodels.models.asyncapi.AsyncApiMessage;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25DocumentImpl;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25SchemaImpl;
import io.ballerina.asyncapi.websocketscore.exception.BallerinaAsyncApiExceptionWs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.DEFAULT_RETURN;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.PIPE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.X_DISPATCHER_KEY;
import static io.ballerina.asyncapi.websocketscore.GeneratorUtils.extractReferenceType;
import static io.ballerina.asyncapi.websocketscore.GeneratorUtils.getValidName;

/**
 * This util class for maintain the operation response with ballerina return type.
 *
 */
public class RemoteFunctionReturnTypeGenerator {
    private AsyncApi25DocumentImpl asyncAPI;

    public RemoteFunctionReturnTypeGenerator(AsyncApi25DocumentImpl asyncAPI) {
        this.asyncAPI = asyncAPI;
    }

    /**
     * Get return type of the remote function.
     * <p>
     * //     * @param operation swagger operation.
     *
     * @return string with return type.
     * @throws BallerinaAsyncApiExceptionWs - throws exception if creating return type fails.
     */
    public String getReturnType(JsonNode xResponse, JsonNode xResponseType, ArrayList<String> responseMessages
    ) throws BallerinaAsyncApiExceptionWs {
        //TODO: Handle multiple media-type
        ArrayList<String> returnTypes = new ArrayList<>();
        Map<String, AsyncApiMessage> messages = asyncAPI.getComponents().getMessages();

        if (xResponse.get("oneOf") != null) {  //Handle Union references
            if (xResponse.get("oneOf") instanceof ArrayNode) {
                ArrayNode test = (ArrayNode) xResponse.get("oneOf");
                if (xResponseType != null) {
                    for (Iterator<JsonNode> it = test.iterator(); it.hasNext(); ) {
                        JsonNode jsonNode = it.next();
                        if (jsonNode.get("$ref") != null) {
                            handleReferenceReturn(jsonNode, messages, responseMessages, returnTypes);
                        } else if (jsonNode.get("payload") != null) {
                            throw new BallerinaAsyncApiExceptionWs("Ballerina service file cannot be generate to the " +
                                    "given AsyncAPI specification, Response type must be a Record");
                        }
                    }
                } else {
                    throw new BallerinaAsyncApiExceptionWs("x-response-type must be included ex:-" +
                            " x-response-type: streaming || x-response-type: simple-rpc");
                }
            }
        } else if (xResponse.get("payload") != null && xResponse.get("payload")
                .get("type") != new TextNode("object")) { //Handle payload references
            throw new BallerinaAsyncApiExceptionWs("Ballerina service file cannot be generate to the " +
                    "given AsyncAPI specification, Response type must be a Record");
        } else if (xResponse.get("$ref") != null) { //Handle reference responses
            handleReferenceReturn(xResponse, messages, responseMessages, returnTypes);

        }
        //Add |error to the response
        if (!returnTypes.isEmpty()) {
            return String.join(PIPE, returnTypes);
        } else {
            return DEFAULT_RETURN;
        }
    }

    private void handleReferenceReturn(JsonNode jsonNode, Map<String, AsyncApiMessage> messages,
                                         ArrayList<String> responseMessages, ArrayList<String> returnTypes)
            throws BallerinaAsyncApiExceptionWs {
        TextNode textNode = (TextNode) asyncAPI.getExtensions().get(X_DISPATCHER_KEY);
        String dispatcherKey = textNode.asText();
        String reference = jsonNode.get("$ref").asText();
        // TODO: Consider adding getValidName here , removed because of lowercase and
        //  uppercase error
        String messageName = extractReferenceType(reference);
        AsyncApiMessage message = messages.get(messageName);
        TextNode schemaReference = (TextNode) message.getPayload().get("$ref");
        String schemaName = extractReferenceType(schemaReference.asText());
        AsyncApi25SchemaImpl refSchema = (AsyncApi25SchemaImpl) asyncAPI.getComponents().getSchemas().get(schemaName);
        if (responseMessages != null) {
            responseMessages.add(schemaName);
        }
        CommonFunctionUtils commonFunctionUtils = new CommonFunctionUtils(asyncAPI);
        if (!commonFunctionUtils.isDispatcherPresent(schemaName, refSchema, dispatcherKey, true)) {
            throw new BallerinaAsyncApiExceptionWs(String.format(
                    "dispatcherKey must be inside %s schema properties", schemaName));
        }
        String type = getValidName(schemaName, true);
        returnTypes.add(type);
    }
}
