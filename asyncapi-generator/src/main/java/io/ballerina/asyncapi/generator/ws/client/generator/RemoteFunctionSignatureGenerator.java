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
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.core.model.component.AsyncApiSchema;
import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.ws.client.utils.CodegenUtils;
import io.ballerina.asyncapi.generator.ws.client.utils.CommonFunctionUtils;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.MarkdownParameterDocumentationLineNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.ReturnTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.ballerina.asyncapi.generator.ws.client.extractor.DispatcherKeyExtractor.X_DISPATCHER_KEY;
import static io.ballerina.asyncapi.generator.ws.client.generator.ClientGenerator.SERVER_STREAMING;
import static io.ballerina.asyncapi.generator.ws.client.generator.ClientGenerator.X_RESPONSE;
import static io.ballerina.asyncapi.generator.ws.client.generator.ClientGenerator.X_RESPONSE_TYPE;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createSeparatedNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBuiltinSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionSignatureNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createOptionalTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRequiredParameterNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createReturnTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.COMMA_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ERROR_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PIPE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.QUESTION_MARK_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RETURNS_KEYWORD;

/**
 * Generates a {@link FunctionSignatureNode} for a WebSocket client remote function.
 */
public class RemoteFunctionSignatureGenerator {

    private static final String TIMEOUT = "timeout";
    private static final String DECIMAL = "decimal";
    private static final String ERROR = "error";
    private static final String DESCRIPTION = "description";

    private final AsyncApiSpec asyncApiSpec;
    private final List<TypeDefinitionNode> typeDefinitionNodeList;

    /**
     * Creates a new signature generator backed by the given spec.
     *
     * @param asyncApiSpec           the parsed AsyncAPI spec
     * @param typeDefinitionNodeList list to which extra type definitions may be appended
     */
    public RemoteFunctionSignatureGenerator(AsyncApiSpec asyncApiSpec,
                                            List<TypeDefinitionNode> typeDefinitionNodeList) {
        this.asyncApiSpec = asyncApiSpec;
        this.typeDefinitionNodeList = typeDefinitionNodeList;
    }

    /**
     * Returns the list of extra type definitions accumulated during signature generation.
     *
     * @return type definition node list
     */
    public List<TypeDefinitionNode> getTypeDefinitionNodeList() {
        return typeDefinitionNodeList;
    }

    /**
     * Builds the parameter source string for a remote function.
     * Returns {@code "TypeName varName, decimal timeout"} when a payload is present,
     * or {@code "decimal timeout"} otherwise.
     *
     * @param payload the message payload schema (may be {@code null})
     * @return the parameter source string
     * @throws GeneratorException if the payload schema is invalid
     */
    public String buildParamString(AsyncApiSchema payload) throws GeneratorException {
        if (payload == null) {
            return String.format("%s %s", DECIMAL, TIMEOUT);
        }
        String typeName = getDataType(payload);
        String varName = CodegenUtils.getValidName(typeName, false);
        return String.format("%s %s, %s %s", typeName, varName, DECIMAL, TIMEOUT);
    }

    /**
     * Builds the return type source string for a remote function signature.
     * Accounts for streaming and non-streaming response types.
     *
     * @param extensions message-level extensions
     * @param returnType the pre-computed Ballerina return type name
     * @return the full return type string (e.g. {@code "Response|error"})
     */
    public String buildReturnTypeString(Map<String, JsonNode> extensions, String returnType) {
        if (extensions != null && extensions.containsKey(X_RESPONSE)) {
            JsonNode xResponseType = extensions.get(X_RESPONSE_TYPE);
            String rt = returnType;
            if (xResponseType != null && SERVER_STREAMING.equals(xResponseType.asText())) {
                rt = String.format("stream<%s,error?>", returnType);
            }
            return String.format("%s|%s", rt, ERROR);
        }
        return "error?";
    }

    /**
     * Generates the function signature (parameters and return type) for a remote function.
     *
     * @param payload            the message payload schema (may be {@code null} for receive-only operations)
     * @param remoteFunctionDoc  documentation nodes to which param/return doc lines are appended
     * @param extensions         message-level extensions (e.g. {@code x-response}, {@code x-response-type})
     * @param returnType         the Ballerina return type string (pre-computed)
     * @param streamReturns      list to which stream return types are recorded
     * @return the function signature node
     * @throws GeneratorException if the payload schema is invalid or required fields are missing
     */
    public FunctionSignatureNode getFunctionSignatureNode(AsyncApiSchema payload, List<Node> remoteFunctionDoc,
                                                          Map<String, JsonNode> extensions, String returnType,
                                                          List<String> streamReturns) throws GeneratorException {
        List<Node> parameterList = new ArrayList<>();
        if (payload != null) {
            String parameterType = getDataType(payload);
            parameterList.add(getRequestTypeParameterNode(parameterType));
            parameterList.add(createToken(COMMA_TOKEN));
            if (payload.description() != null) {
                MarkdownParameterDocumentationLineNode paramDoc = DocCommentsGenerator.createAPIParamDoc(
                        CodegenUtils.getValidName(parameterType, false), payload.description());
                MarkdownParameterDocumentationLineNode timeoutDoc = DocCommentsGenerator.createAPIParamDoc(
                        TIMEOUT, "waiting period to keep the event in the buffer in seconds");
                remoteFunctionDoc.add(paramDoc);
                remoteFunctionDoc.add(timeoutDoc);
            }
        }
        parameterList.add(getTimeOutParameterNode());
        SeparatedNodeList<ParameterNode> parameters = createSeparatedNodeList(parameterList);

        ReturnTypeDescriptorNode returnTypeDescriptorNode;
        if (extensions != null && extensions.containsKey(X_RESPONSE)) {
            JsonNode xResponse = extensions.get(X_RESPONSE);
            JsonNode xResponseType = extensions.get(X_RESPONSE_TYPE);
            if (xResponseType != null && SERVER_STREAMING.equals(xResponseType.asText())) {
                if (!streamReturns.contains(returnType)) {
                    streamReturns.add(returnType);
                }
                returnType = String.format("stream<%s,error?>", returnType);
            }
            String finalReturnType = String.format("%s%s%s",
                    returnType, PIPE_TOKEN.stringValue(), ERROR);
            if (xResponse.get(DESCRIPTION) != null) {
                remoteFunctionDoc.add(DocCommentsGenerator.createAPIParamDoc(
                        "return", xResponse.get(DESCRIPTION).asText()));
            }
            returnTypeDescriptorNode = createReturnTypeDescriptorNode(createToken(RETURNS_KEYWORD),
                    createEmptyNodeList(), createBuiltinSimpleNameReferenceNode(null,
                            createIdentifierToken(finalReturnType)));
        } else {
            returnTypeDescriptorNode = createReturnTypeDescriptorNode(createToken(RETURNS_KEYWORD),
                    createEmptyNodeList(),
                    createOptionalTypeDescriptorNode(createToken(ERROR_KEYWORD), createToken(QUESTION_MARK_TOKEN)));
        }
        return createFunctionSignatureNode(createToken(OPEN_PAREN_TOKEN), parameters,
                createToken(CLOSE_PAREN_TOKEN), returnTypeDescriptorNode);
    }

    /**
     * Resolves the Ballerina type name for the given payload schema.
     * For resolved references, validates that the dispatcher key is present in the schema properties.
     *
     * @param payload the message payload schema
     * @return the Ballerina type name
     * @throws GeneratorException if the schema is missing the dispatcher key or is structurally invalid
     */
    public String getDataType(AsyncApiSchema payload) throws GeneratorException {
        if (payload == null) {
            return "anydata";
        }
        if (payload.name() != null) {
            String typeName = CodegenUtils.getValidName(payload.name(), true);
            validateDispatcherPresence(payload.name());
            return typeName;
        }
        if (payload.type() != null) {
            return CodegenUtils.TYPE_MAP.getOrDefault(payload.type(), payload.type());
        }
        return "anydata";
    }

    /**
     * Creates a required parameter node for the given request type name.
     *
     * @param paramType the Ballerina type name for the parameter
     * @return the parameter node
     */
    public Node getRequestTypeParameterNode(String paramType) {
        TypeDescriptorNode typeName = createBuiltinSimpleNameReferenceNode(null,
                createIdentifierToken(CodegenUtils.getValidName(paramType, true)));
        IdentifierToken paramName = createIdentifierToken(CodegenUtils.getValidName(paramType, false));
        return createRequiredParameterNode(createNodeList(new ArrayList<>()), typeName, paramName);
    }

    private Node getTimeOutParameterNode() {
        TypeDescriptorNode typeName = createBuiltinSimpleNameReferenceNode(null,
                createIdentifierToken(DECIMAL));
        IdentifierToken paramName = createIdentifierToken(TIMEOUT);
        return createRequiredParameterNode(createNodeList(), typeName, paramName);
    }

    private void validateDispatcherPresence(String schemaName) throws GeneratorException {
        Map<String, AsyncApiSchema> schemas = asyncApiSpec.getAsyncApiComponents()
                .map(c -> c.schemas()).orElse(null);
        if (schemas == null) {
            return;
        }
        AsyncApiSchema componentSchema = schemas.get(schemaName);
        if (componentSchema == null) {
            return;
        }
        String dispatcherKey = asyncApiSpec.getAsyncApiExtensions()
                .map(ext -> {
                    JsonNode node = ext.get(X_DISPATCHER_KEY);
                    return node != null ? node.asText() : null;
                }).orElse(null);
        if (dispatcherKey == null) {
            return;
        }
        CommonFunctionUtils utils = new CommonFunctionUtils(asyncApiSpec);
        if (!utils.isDispatcherPresent(schemaName, componentSchema, dispatcherKey, true)) {
            throw new GeneratorException(String.format(
                    "dispatcherKey must be inside %s schema properties", schemaName));
        }
    }
}
