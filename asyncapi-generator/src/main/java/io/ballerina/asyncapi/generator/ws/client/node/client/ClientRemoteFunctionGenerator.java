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
package io.ballerina.asyncapi.generator.ws.client.node.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.core.model.component.AsyncApiSchema;
import io.ballerina.asyncapi.core.model.message.AsyncApiMessage;
import io.ballerina.asyncapi.core.model.operation.AsyncApiOperation;
import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.ws.client.generator.ClientGenerator;
import io.ballerina.asyncapi.generator.ws.client.generator.RemoteFunctionBodyGenerator;
import io.ballerina.asyncapi.generator.ws.client.generator.RemoteFunctionReturnTypeGenerator;
import io.ballerina.asyncapi.generator.ws.client.generator.RemoteFunctionSignatureGenerator;
import io.ballerina.asyncapi.generator.ws.client.utils.CodegenUtils;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds the remote function definition nodes for a WebSocket client class.
 */
public class ClientRemoteFunctionGenerator {

    private static final String REMOTE_METHOD_NAME_PREFIX = "do";
    private static final String DESCRIPTION = "description";

    private final AsyncApiSpec asyncApiSpec;
    private final String dispatcherStreamId;
    private final List<String> streamReturns;
    private final List<ImportDeclarationNode> imports;
    private final List<TypeDefinitionNode> typeDefinitionNodeList;
    private final Map<String, String> responseMap;

    /**
     * Creates a new remote function generator.
     *
     * @param asyncApiSpec           the parsed AsyncAPI spec
     * @param dispatcherStreamId     optional stream ID field name (may be {@code null})
     * @param streamReturns          list of streaming return types
     * @param imports                accumulated import declaration nodes
     * @param typeDefinitionNodeList accumulated type definition nodes
     * @param responseMap            response type to pipe name map
     */
    public ClientRemoteFunctionGenerator(AsyncApiSpec asyncApiSpec, String dispatcherStreamId,
                                          List<String> streamReturns, List<ImportDeclarationNode> imports,
                                          List<TypeDefinitionNode> typeDefinitionNodeList,
                                          Map<String, String> responseMap) {
        this.asyncApiSpec = asyncApiSpec;
        this.dispatcherStreamId = dispatcherStreamId;
        this.streamReturns = streamReturns;
        this.imports = imports;
        this.typeDefinitionNodeList = typeDefinitionNodeList;
        this.responseMap = responseMap;
    }

    /**
     * Builds remote function definitions for all SEND and RECEIVE-only operations.
     *
     * @return list of function definition nodes
     * @throws GeneratorException if generation fails
     */
    public List<FunctionDefinitionNode> buildRemoteFunctions() throws GeneratorException {
        List<FunctionDefinitionNode> remoteFunctions = new ArrayList<>();
        Map<String, AsyncApiOperation> operations = asyncApiSpec.getAsyncApiOperations().orElse(null);
        if (operations == null) {
            return remoteFunctions;
        }

        RemoteFunctionSignatureGenerator sigGen =
                new RemoteFunctionSignatureGenerator(asyncApiSpec, typeDefinitionNodeList);
        RemoteFunctionBodyGenerator bodyGen = new RemoteFunctionBodyGenerator(imports);
        RemoteFunctionReturnTypeGenerator returnTypeGen = new RemoteFunctionReturnTypeGenerator(asyncApiSpec);

        // Track response messages already handled by SEND ops
        Set<String> remainingResponseMessages = new HashSet<>();

        // First pass: SEND operations
        for (Map.Entry<String, AsyncApiOperation> opEntry : operations.entrySet()) {
            AsyncApiOperation op = opEntry.getValue();
            if (op.action() != AsyncApiOperation.Action.SEND) {
                continue;
            }
            if (op.messages() == null) {
                continue;
            }
            for (Map.Entry<String, AsyncApiMessage> msgEntry : op.messages().entrySet()) {
                String msgName = msgEntry.getKey();
                AsyncApiMessage message = msgEntry.getValue();

                // Skip close-frame schemas
                if (message.payload() instanceof AsyncApiSchema) {
                    AsyncApiSchema payloadSchema = (AsyncApiSchema) message.payload();
                    if (CodegenUtils.isCloseFrameSchema(payloadSchema)) {
                        continue;
                    }
                }

                // Collect response message names for this SEND op
                if (message.extensions() != null && message.extensions().containsKey(ClientGenerator.X_RESPONSE)) {
                    JsonNode xResponse = message.extensions().get(ClientGenerator.X_RESPONSE);
                    JsonNode xResponseType = message.extensions().get(ClientGenerator.X_RESPONSE_TYPE);
                    List<String> responseMessages = new ArrayList<>();
                    returnTypeGen.getReturnType(xResponse, xResponseType, responseMessages);
                    remainingResponseMessages.addAll(responseMessages);
                }

                FunctionDefinitionNode fn = getRemoteFunctionDefinitionNode(asyncApiSpec, msgName, message,
                        dispatcherStreamId, false, sigGen, bodyGen, returnTypeGen, streamReturns, responseMap);
                remoteFunctions.add(fn);
            }
        }

        // Second pass: RECEIVE-only operations
        for (Map.Entry<String, AsyncApiOperation> opEntry : operations.entrySet()) {
            AsyncApiOperation op = opEntry.getValue();
            if (op.action() != AsyncApiOperation.Action.RECEIVE) {
                continue;
            }
            if (op.messages() == null) {
                continue;
            }
            for (Map.Entry<String, AsyncApiMessage> msgEntry : op.messages().entrySet()) {
                String msgName = msgEntry.getKey();
                AsyncApiMessage message = msgEntry.getValue();

                // Skip close-frame schemas
                if (message.payload() instanceof AsyncApiSchema) {
                    AsyncApiSchema payloadSchema = (AsyncApiSchema) message.payload();
                    if (CodegenUtils.isCloseFrameSchema(payloadSchema)) {
                        continue;
                    }
                }

                // Skip if already in remainingResponseMessages
                if (remainingResponseMessages.contains(msgName)) {
                    continue;
                }

                // Build synthetic extensions for receive-only messages
                AsyncApiMessage enrichedMessage = buildReceiveOnlyMessage(message, msgName);

                FunctionDefinitionNode fn = getRemoteFunctionDefinitionNode(asyncApiSpec, msgName, enrichedMessage,
                        dispatcherStreamId, true, sigGen, bodyGen, returnTypeGen, streamReturns, responseMap);
                remoteFunctions.add(fn);
            }
        }

        return remoteFunctions;
    }

    /**
     * Builds a synthetic message with x-response and x-response-type extensions for receive-only messages.
     *
     * @param original the original message
     * @param msgName  the message name
     * @return enriched message with synthetic extensions
     */
    private AsyncApiMessage buildReceiveOnlyMessage(AsyncApiMessage original, String msgName) {
        Map<String, JsonNode> syntheticExt = new HashMap<>();
        if (original.extensions() != null) {
            syntheticExt.putAll(original.extensions());
        }
        if (!syntheticExt.containsKey(ClientGenerator.X_RESPONSE)) {
            ObjectNode xResponseNode = JsonNodeFactory.instance.objectNode();
            xResponseNode.put("$ref", String.format("#/components/messages/%s", msgName));
            syntheticExt.put(ClientGenerator.X_RESPONSE, xResponseNode);
            syntheticExt.put(ClientGenerator.X_RESPONSE_TYPE,
                    JsonNodeFactory.instance.textNode(ClientGenerator.SIMPLE_RPC));
        }
        return new AsyncApiMessage(original.headers(), original.payload(), original.correlationId(),
                original.contentType(), original.name(), original.title(), original.summary(),
                original.description(), original.tags(), original.externalDocs(), original.bindings(),
                original.examples(), original.traits(), syntheticExt);
    }

    /**
     * Builds a single remote function definition node.
     * Uses {@link NodeParser#parseObjectMember} so all tokens carry proper whitespace trivia.
     *
     * @param asyncApiSpec       the spec
     * @param msgName            the message key name
     * @param message            the message definition
     * @param dispatcherStreamId optional stream ID
     * @param isSubscribe        true for receive-only operations
     * @param sigGen             signature generator
     * @param bodyGen            body generator
     * @param returnTypeGen      return type generator
     * @param streamReturns      list of streaming return types
     * @param responseMap        response type to pipe name map
     * @return the function definition node
     * @throws GeneratorException if generation fails
     */
    private FunctionDefinitionNode getRemoteFunctionDefinitionNode(AsyncApiSpec asyncApiSpec, String msgName,
                                                                    AsyncApiMessage message,
                                                                    String dispatcherStreamId,
                                                                    boolean isSubscribe,
                                                                    RemoteFunctionSignatureGenerator sigGen,
                                                                    RemoteFunctionBodyGenerator bodyGen,
                                                                    RemoteFunctionReturnTypeGenerator returnTypeGen,
                                                                    List<String> streamReturns,
                                                                    Map<String, String> responseMap)
            throws GeneratorException {
        Map<String, JsonNode> msgExtensions = message.extensions();
        AsyncApiSchema payload = null;
        if (!isSubscribe && message.payload() instanceof AsyncApiSchema) {
            payload = (AsyncApiSchema) message.payload();
        }

        // Compute return type
        String returnType = "null";
        if (msgExtensions != null && msgExtensions.containsKey(ClientGenerator.X_RESPONSE)) {
            JsonNode xResponse = msgExtensions.get(ClientGenerator.X_RESPONSE);
            JsonNode xResponseType = msgExtensions.get(ClientGenerator.X_RESPONSE_TYPE);
            List<String> responseMessages = new ArrayList<>();
            returnType = returnTypeGen.getReturnType(xResponse, xResponseType, responseMessages);
            if ("null".equals(returnType)) {
                throw new GeneratorException(String.format(
                        "x-response for message '%s' does not resolve to a valid return type", msgName));
            }
            if (!"null".equals(returnType)
                    && xResponseType != null && ClientGenerator.SERVER_STREAMING.equals(xResponseType.asText())
                    && !streamReturns.contains(returnType)) {
                streamReturns.add(returnType);
            }
        }

        // Build doc comment string
        String description = message.description() != null ? message.description()
                : (message.summary() != null ? message.summary() : "");
        StringBuilder docBuilder = new StringBuilder();
        if (!description.isBlank()) {
            docBuilder.append("\n# ").append(description).append("\n#\n");
        }

        // Build param doc lines
        if (payload != null && payload.description() != null) {
            String typeName = sigGen.getDataType(payload);
            String varName = CodegenUtils.getValidName(typeName, false);
            docBuilder.append("# + ").append(varName).append(" - ").append(payload.description()).append("\n");
            docBuilder.append("# + timeout - waiting period to keep the event in the buffer in seconds\n");
        }

        // Build return doc line
        if (msgExtensions != null && msgExtensions.containsKey(ClientGenerator.X_RESPONSE)) {
            JsonNode xResponse = msgExtensions.get(ClientGenerator.X_RESPONSE);
            if (xResponse != null && xResponse.get(DESCRIPTION) != null) {
                docBuilder.append("# + return - ").append(xResponse.get(DESCRIPTION).asText()).append("\n");
            }
        }

        // Build param string
        String paramStr = sigGen.buildParamString(payload);

        // Build return type string
        String fullReturnType = sigGen.buildReturnTypeString(msgExtensions, returnType);

        // Function name
        String funcName = String.format("%s%s",
                REMOTE_METHOD_NAME_PREFIX, CodegenUtils.getValidName(msgName, true));

        // Build body source — use the canonical pipe key from responseMap so all functions sharing
        // the same response type route through the same pipe
        String requestType = payload != null ? sigGen.getDataType(payload) : "null";
        String pipeName = responseMap.getOrDefault(returnType, requestType);
        String bodySource = bodyGen.buildBodySource(msgExtensions, requestType, dispatcherStreamId,
                isSubscribe, returnType, pipeName);

        // Build complete function source and parse
        String funcSource = String.format(
                "%sremote isolated function %s(%s) returns %s {\n%s}",
                docBuilder.toString(), funcName, paramStr, fullReturnType, bodySource);
        return (FunctionDefinitionNode) NodeParser.parseObjectMember(funcSource);
    }
}
