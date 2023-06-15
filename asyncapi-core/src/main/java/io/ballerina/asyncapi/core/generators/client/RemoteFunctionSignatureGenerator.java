/*
 * Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.asyncapi.core.generators.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25DocumentImpl;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25SchemaImpl;
import io.ballerina.asyncapi.core.GeneratorUtils;
import io.ballerina.asyncapi.core.exception.BallerinaAsyncApiException;
import io.ballerina.asyncapi.core.generators.document.DocCommentsGenerator;
import io.ballerina.asyncapi.core.generators.schema.BallerinaTypesGenerator;
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

import static io.ballerina.asyncapi.core.GeneratorConstants.DECIMAL;
import static io.ballerina.asyncapi.core.GeneratorConstants.DESCRIPTION;
import static io.ballerina.asyncapi.core.GeneratorConstants.ERROR;
import static io.ballerina.asyncapi.core.GeneratorConstants.SERVER_STREAMING;
import static io.ballerina.asyncapi.core.GeneratorConstants.TIMEOUT;
import static io.ballerina.asyncapi.core.GeneratorConstants.X_DISPATCHER_KEY;
import static io.ballerina.asyncapi.core.GeneratorConstants.X_RESPONSE;
import static io.ballerina.asyncapi.core.GeneratorConstants.X_RESPONSE_TYPE;
import static io.ballerina.asyncapi.core.GeneratorUtils.extractReferenceType;
import static io.ballerina.asyncapi.core.GeneratorUtils.getValidName;
import static io.ballerina.asyncapi.core.GeneratorUtils.isValidSchemaName;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createSeparatedNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBuiltinSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionSignatureNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRequiredParameterNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createReturnTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.COMMA_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PIPE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RETURNS_KEYWORD;

/**
 * This util class uses for generating {@link FunctionSignatureNode} for given OAS
 * operation.
 *
 * @since 1.3.0
 */
public class RemoteFunctionSignatureGenerator {
    private final AsyncApi25DocumentImpl asyncAPI;
    private final List<TypeDefinitionNode> typeDefinitionNodeList;
    private final BallerinaTypesGenerator ballerinaSchemaGenerator;


    public RemoteFunctionSignatureGenerator(AsyncApi25DocumentImpl asyncAPI,
                                            BallerinaTypesGenerator ballerinaSchemaGenerator,
                                            List<TypeDefinitionNode> typeDefinitionNodeList) {
        this.asyncAPI = asyncAPI;
        this.ballerinaSchemaGenerator = ballerinaSchemaGenerator;
        this.typeDefinitionNodeList = typeDefinitionNodeList;


    }

    public List<TypeDefinitionNode> getTypeDefinitionNodeList() {
        return typeDefinitionNodeList;
    }

    /**
     * This function for generate function signatures.
     *
     * @param payload - asyncAPI operation
     * @return {@link FunctionSignatureNode}
     * @throws BallerinaAsyncApiException - throws exception when node creation fails.
     */
    public FunctionSignatureNode getFunctionSignatureNode(JsonNode payload, List<Node> remoteFunctionDoc,
                                                          Map<String, JsonNode> extensions,
                                                          String returnType,
                                                          List<String> streamReturns)
            throws BallerinaAsyncApiException {

        List<Node> parameterList = new ArrayList<>();


        if (payload != null) {
            String parameterType = getDataType(payload);
            Node requestTypeParamNode = getRequestTypeParameterNode(parameterType);
            parameterList.add(requestTypeParamNode);
            parameterList.add(createToken(COMMA_TOKEN));
            TextNode descriptionNode = (TextNode) payload.get(DESCRIPTION);
            if (descriptionNode != null) {
                MarkdownParameterDocumentationLineNode paramDoc =
                        DocCommentsGenerator.createAPIParamDoc(getValidName(parameterType, false),
                                descriptionNode.asText());
                MarkdownParameterDocumentationLineNode timeoutDoc =
                        DocCommentsGenerator.createAPIParamDoc(TIMEOUT,
                                "waiting period to keep the event in the buffer in seconds");
                remoteFunctionDoc.add(paramDoc);
                remoteFunctionDoc.add(timeoutDoc);
            }
        }


        Node timeoutNode = getTimeOutParameterNode(DECIMAL, TIMEOUT);
        parameterList.add(timeoutNode);


        SeparatedNodeList<ParameterNode> parameters = createSeparatedNodeList(parameterList);
        //Create Return type - function with response

        ReturnTypeDescriptorNode returnTypeDescriptorNode;
        if (extensions != null) {
            JsonNode xResponse = extensions.get(X_RESPONSE);
            JsonNode xResponseType = extensions.get(X_RESPONSE_TYPE);
//            String returnType = functionReturnType.getReturnType(xResponse, xResponseType, responseMessages);
            if (xResponseType != null && xResponseType.equals(new TextNode(SERVER_STREAMING))) {
                if (!streamReturns.contains(returnType)) {
                    streamReturns.add(returnType);
                }
                returnType = "stream<" + returnType + ",error?>";
            }
            String finalReturnType = returnType +
                    PIPE_TOKEN.stringValue() +
                    ERROR;
            TextNode responseDescription = (TextNode) xResponse.get(DESCRIPTION);
            if (xResponse.get(DESCRIPTION) != null) {
                MarkdownParameterDocumentationLineNode returnDoc =
                        DocCommentsGenerator.createAPIParamDoc("return", responseDescription.asText());
                remoteFunctionDoc.add(returnDoc);
            }

            // Return Type
            returnTypeDescriptorNode = createReturnTypeDescriptorNode(createToken(RETURNS_KEYWORD),
                    createEmptyNodeList(), createBuiltinSimpleNameReferenceNode(null,
                            createIdentifierToken(finalReturnType)));
        } else {
            returnTypeDescriptorNode = createReturnTypeDescriptorNode(createToken(RETURNS_KEYWORD),
                    createEmptyNodeList(), createSimpleNameReferenceNode(createIdentifierToken("error?")));

        }
        return createFunctionSignatureNode(createToken(OPEN_PAREN_TOKEN), parameters, createToken(CLOSE_PAREN_TOKEN),
                returnTypeDescriptorNode);
    }

    private Node getTimeOutParameterNode(String timeoutType, String paramName) {
        TypeDescriptorNode typeName = createBuiltinSimpleNameReferenceNode(null,
                createIdentifierToken(timeoutType));
        IdentifierToken paramNameNode = createIdentifierToken(paramName);


        return createRequiredParameterNode(createNodeList(), typeName, paramNameNode);
    }

    public String getDataType(JsonNode payload) throws BallerinaAsyncApiException {
        String type = "";
        if (payload.get("$ref") != null) {
            // TODO: Consider adding getValidName here , removed because of lowercase and uppercase error
            type = extractReferenceType(payload.get("$ref").textValue());
            AsyncApi25SchemaImpl componentSchema = (AsyncApi25SchemaImpl) asyncAPI.getComponents().
                    getSchemas().get(type).asSchema();
            TextNode textNode = (TextNode) asyncAPI.getExtensions().get(X_DISPATCHER_KEY);
            String dispatcherKey = textNode.asText();
            CommonFunctionUtils commonFunctionUtils = new CommonFunctionUtils(asyncAPI);
            if (!commonFunctionUtils.isDispatcherPresent(type, componentSchema, dispatcherKey, true)) {
                throw new BallerinaAsyncApiException(String.format(
                        "dispatcherKey must be inside %s schema properties", type));
            }

            if (!isValidSchemaName(type)) {
                List<Node> responseDocs = new ArrayList<>();
                if (payload.get(DESCRIPTION) != null) {
                    responseDocs.addAll(DocCommentsGenerator.createAPIDescriptionDoc(
                            payload.get(DESCRIPTION).asText(), false));
                }
                TypeDefinitionNode typeDefinitionNode = ballerinaSchemaGenerator.getTypeDefinitionNode
                        (componentSchema, type, responseDocs);
                GeneratorUtils.updateTypeDefNodeList(type, typeDefinitionNode, typeDefinitionNodeList);
            }
        }
        return type;


    }

    /**
     * Create parameter for remote function.
     * <p>
     */
    public Node getRequestTypeParameterNode(String paramType) {


        TypeDescriptorNode typeName;
        typeName = createBuiltinSimpleNameReferenceNode(null, createIdentifierToken(
                getValidName(paramType, true)));
        IdentifierToken paramName =
                createIdentifierToken(getValidName(paramType, false));
        return createRequiredParameterNode(createNodeList(new ArrayList<>()), typeName, paramName);
    }

}
