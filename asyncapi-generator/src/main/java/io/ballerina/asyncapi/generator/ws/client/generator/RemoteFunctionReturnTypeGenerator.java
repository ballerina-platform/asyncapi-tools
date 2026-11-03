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
import io.ballerina.asyncapi.core.model.operation.AsyncApiOperation;
import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.ws.client.utils.CodegenUtils;
import io.ballerina.asyncapi.generator.ws.client.utils.CommonFunctionUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static io.ballerina.asyncapi.generator.ws.client.extractor.DispatcherKeyExtractor.X_DISPATCHER_KEY;
import static io.ballerina.asyncapi.generator.ws.client.generator.ClientGenerator.SERVER_STREAMING;
import static io.ballerina.asyncapi.generator.ws.client.generator.ClientGenerator.X_RESPONSE;
import static io.ballerina.asyncapi.generator.ws.client.generator.ClientGenerator.X_RESPONSE_TYPE;

/**
 * Computes the Ballerina return type string for a remote function based on
 * {@code x-response} and {@code x-response-type} message extensions.
 */
public class RemoteFunctionReturnTypeGenerator {

    /** Pipe the reader puts every frame without the dispatcher key on, such as a plain string reply. */
    public static final String NO_DISPATCHER_KEY_PIPE = "#noDispatcherKey";

    private static final String DEFAULT_RETURN = "null";
    private static final String PIPE = "|";
    private static final String OBJECT = "object";
    private static final String STRING = "string";
    private static final String INLINE_RESPONSE_NOT_A_SCHEMA =
            "x-response payload must be a schema object";
    private static final String REPLY_WITHOUT_DISPATCHER_KEY_NOT_ALONE = "Reply type '%s' has no dispatcher key, "
            + "so it must be the only reply of a simple-rpc message, not part of a oneOf or a stream: %s";

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
                        throw new GeneratorException(INLINE_RESPONSE_NOT_A_SCHEMA);
                    }
                }
            }
        } else if (xResponse.get("$ref") != null) {
            handleReferenceReturn(xResponse, messages, responseMessages, returnTypes);
        } else if (xResponse.get("payload") != null) {
            throw new GeneratorException(INLINE_RESPONSE_NOT_A_SCHEMA);
        }

        boolean streaming = xResponseType != null && SERVER_STREAMING.equals(xResponseType.asText());
        for (String returnType : returnTypes) {
            if (isReplyWithoutDispatcherKey(returnType) && (returnTypes.size() > 1 || streaming)) {
                throw new GeneratorException(String.format(REPLY_WITHOUT_DISPATCHER_KEY_NOT_ALONE, returnType,
                        xResponse));
            }
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
        if (dispatcherKey != null && !isSchemaWithoutDispatcherKey(refSchema)
                && !commonFunctionUtils.isDispatcherPresent(schemaName, refSchema, dispatcherKey, true)) {
            throw new GeneratorException(
                    String.format("dispatcherKey must be inside %s schema properties", schemaName));
        }
        returnTypes.add(CodegenUtils.getValidName(schemaName, true));
    }

    /**
     * Returns {@code true} if the reply type can't carry the dispatcher key (a string, number, boolean or array),
     * so its frames reach the client on {@link #NO_DISPATCHER_KEY_PIPE}.
     *
     * @param typeName the Ballerina reply type name
     * @return whether the reply has no dispatcher key
     */
    public boolean isReplyWithoutDispatcherKey(String typeName) {
        return isSchemaWithoutDispatcherKey(findSchema(typeName));
    }

    /**
     * Returns {@code true} if the reply type is a string, whose frame text is the value itself rather than JSON.
     *
     * @param typeName the Ballerina reply type name
     * @return whether the reply is a string
     */
    public boolean isTextReply(String typeName) {
        AsyncApiSchema schema = findSchema(typeName);
        return isSchemaWithoutDispatcherKey(schema) && STRING.equals(schema.type());
    }

    /**
     * Returns {@code true} if any remote function waits for a reply without the dispatcher key.
     *
     * @return whether the reader needs to route such frames
     * @throws GeneratorException if a reply type can't be resolved
     */
    public boolean hasRepliesWithoutDispatcherKey() throws GeneratorException {
        Map<String, AsyncApiOperation> operations = asyncApiSpec.getAsyncApiOperations().orElse(Map.of());
        for (AsyncApiOperation op : operations.values()) {
            if (op.messages() == null) {
                continue;
            }
            for (AsyncApiMessage message : op.messages().values()) {
                if (op.action() == AsyncApiOperation.Action.RECEIVE) {
                    if (message.payload() instanceof AsyncApiSchema payload && payload.name() != null
                            && isReplyWithoutDispatcherKey(CodegenUtils.getValidName(payload.name(), true))) {
                        return true;
                    }
                } else if (message.extensions() != null && message.extensions().get(X_RESPONSE) != null) {
                    String returnType = getReturnType(message.extensions().get(X_RESPONSE),
                            message.extensions().get(X_RESPONSE_TYPE), null);
                    if (isReplyWithoutDispatcherKey(returnType)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * The component schema whose Ballerina type name is {@code typeName}, or {@code null}.
     */
    private AsyncApiSchema findSchema(String typeName) {
        Map<String, AsyncApiSchema> schemas = asyncApiSpec.getAsyncApiComponents()
                .map(c -> c.schemas()).orElse(null);
        if (schemas == null) {
            return null;
        }
        for (Map.Entry<String, AsyncApiSchema> entry : schemas.entrySet()) {
            if (CodegenUtils.getValidName(entry.getKey().trim(), true).equals(typeName)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Whether the schema is a string, number, boolean or array, which can not carry the dispatcher key.
     */
    private static boolean isSchemaWithoutDispatcherKey(AsyncApiSchema schema) {
        return schema != null && schema.type() != null && !OBJECT.equals(schema.type())
                && schema.properties() == null && schema.oneOf() == null && schema.anyOf() == null
                && schema.allOf() == null;
    }
}
