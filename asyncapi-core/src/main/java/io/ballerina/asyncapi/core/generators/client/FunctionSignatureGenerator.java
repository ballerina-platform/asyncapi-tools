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
import io.apicurio.datamodels.models.Schema;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25DocumentImpl;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25SchemaImpl;
import io.ballerina.asyncapi.core.GeneratorUtils;
import io.ballerina.asyncapi.core.exception.BallerinaAsyncApiException;
import io.ballerina.asyncapi.core.generators.asyncspec.model.BalAsyncApi25SchemaImpl;
import io.ballerina.asyncapi.core.generators.document.DocCommentsGenerator;
import io.ballerina.asyncapi.core.generators.schema.BallerinaTypesGenerator;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.ReturnTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.StreamTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.StreamTypeParamsNode;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.ballerina.asyncapi.core.GeneratorConstants.DEFAULT_RETURN;
import static io.ballerina.asyncapi.core.GeneratorConstants.ERROR;
import static io.ballerina.asyncapi.core.GeneratorConstants.SERVER_STREAMING;
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
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRequiredExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRequiredParameterNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createReturnTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createStreamTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createStreamTypeParamsNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.COMMA_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ERROR_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.GT_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.LT_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PIPE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RETURNS_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.STREAM_KEYWORD;

/**
 * This util class uses for generating {@link FunctionSignatureNode} for given OAS
 * operation.
 *
 * @since 1.3.0
 */
public class FunctionSignatureGenerator {
    private final AsyncApi25DocumentImpl asyncAPI;
    private final BallerinaTypesGenerator ballerinaSchemaGenerator;
    private final List<TypeDefinitionNode> typeDefinitionNodeList;
    //    /**
//     * Generate function parameters.
//     */
//    private void setFunctionParameters(Operation operation, List<Node> parameterList, Token comma,
//                                       List<Node> remoteFunctionDoc) throws BallerinaAsyncApiException {
//
//        List<Parameter> parameters = operation.getParameters();
//        List<Node> defaultable = new ArrayList<>();
    List<Node> deprecatedParamDocComments = new ArrayList<>();
    private FunctionReturnTypeGenerator functionReturnType;
    private final boolean deprecatedParamFound = false;

    public FunctionSignatureGenerator(AsyncApi25DocumentImpl asyncAPI,
                                      BallerinaTypesGenerator ballerinaSchemaGenerator,
                                      List<TypeDefinitionNode> typeDefinitionNodeList) throws BallerinaAsyncApiException {

        this.asyncAPI = asyncAPI;
        this.ballerinaSchemaGenerator = ballerinaSchemaGenerator;
        this.typeDefinitionNodeList = typeDefinitionNodeList;
        this.functionReturnType = new FunctionReturnTypeGenerator
                (this.asyncAPI, ballerinaSchemaGenerator, typeDefinitionNodeList);


    }

    public List<TypeDefinitionNode> getTypeDefinitionNodeList() {
        return typeDefinitionNodeList;
    }

    /**
     * This function for generate function signatures.
     *
     * @param payload - openapi operation
     * @return {@link FunctionSignatureNode}
     * @throws BallerinaAsyncApiException - throws exception when node creation fails.
     */
    public FunctionSignatureNode getFunctionSignatureNode(JsonNode payload, List<Node> remoteFunctionDoc,
                                                          Map<String, JsonNode> extensions)
            throws BallerinaAsyncApiException {
        // Store the parameters for method.
        List<Node> parameterList = new ArrayList<>();
//        String reference=payload.get("$ref").toString();

//        setFunctionParameters(operation, parameterList, createToken(COMMA_TOKEN), remoteFunctionDoc);
//        getParameterNode(reference,parameterList);

//

        if(payload!=null) {
            String parameterType = getDType(payload);
            Node requestTypeParamNode = getRequestTypeParameterNode(parameterType);
            parameterList.add(requestTypeParamNode);
            parameterList.add(createToken(COMMA_TOKEN));
        }


        String timeoutType="decimal";
        String paramName="timeout";
        Node timeoutNode = getTimeOutParameterNode(timeoutType,paramName);
        parameterList.add(timeoutNode);

        functionReturnType = new FunctionReturnTypeGenerator
                (asyncAPI, ballerinaSchemaGenerator, typeDefinitionNodeList);
//        if (parameterList.size() >= 2) {
//            parameterList.remove(parameterList.size() - 1);
//        }


        SeparatedNodeList<ParameterNode> parameters = createSeparatedNodeList(parameterList);
        //Create Return type - function with response

        //TODO: thushalya  check and Uncomment after figure out
//        ApiResponses responses = operation.getResponses();
//        Collection<ApiResponse> values = responses.values();
//        Iterator<ApiResponse> iteratorRes = values.iterator();
//        if (iteratorRes.hasNext()) {
//            ApiResponse response = iteratorRes.next();
//            if (response.getDescription() != null && !response.getDescription().isBlank()) {
//                MarkdownParameterDocumentationLineNode returnDoc =
//                        DocCommentsGenerator.createAPIParamDoc("return",
//                        response.getDescription());
//                remoteFunctionDoc.add(returnDoc);
//            }
//        }
        ReturnTypeDescriptorNode returnTypeDescriptorNode;
        if(extensions!=null) {
            JsonNode x_response=extensions.get(X_RESPONSE);
            JsonNode x_response_type=extensions.get(X_RESPONSE_TYPE);
            String returnType = functionReturnType.getReturnType(x_response,x_response_type);
            if(x_response_type.equals(new TextNode(SERVER_STREAMING))){
                returnType="stream<"+returnType+",error?>";
            }
            //Add |error to the response
//            if (returnTypes.size() > 0) {
                String finalReturnType = returnType+
                        PIPE_TOKEN.stringValue() +
                        ERROR;
//            if (noContentResponseFound) {
                //TODO: change this after figure out

//                finalReturnType.append(NILLABLE);
//            }
//                return finalReturnType;
//            } else {
//                return DEFAULT_RETURN;
//            }

            // Return Type
            returnTypeDescriptorNode = createReturnTypeDescriptorNode(createToken(RETURNS_KEYWORD),
                    createEmptyNodeList(), createBuiltinSimpleNameReferenceNode(null,
                            createIdentifierToken(finalReturnType)));
        }else{
            returnTypeDescriptorNode = createReturnTypeDescriptorNode(createToken(RETURNS_KEYWORD),
                    createEmptyNodeList(),createSimpleNameReferenceNode(createIdentifierToken("error?")));

        }
        return createFunctionSignatureNode(createToken(OPEN_PAREN_TOKEN), parameters, createToken(CLOSE_PAREN_TOKEN),
                returnTypeDescriptorNode);
    }

    private Node getTimeOutParameterNode(String timeoutType,String paramName) {
        TypeDescriptorNode typeName = createBuiltinSimpleNameReferenceNode(null, createIdentifierToken(timeoutType));
        IdentifierToken paramNameNode =createIdentifierToken(paramName);



        return createRequiredParameterNode(createNodeList(),typeName,paramNameNode);
    }

    public String getDType(JsonNode payload) throws BallerinaAsyncApiException {
        String type = "";
        if (payload.get("$ref") != null) {
            type = getValidName(extractReferenceType(payload.get("$ref").textValue()), true);
            Schema componentSchema = asyncAPI.getComponents().
                    getSchemas().get(type).asSchema();
            if (!isValidSchemaName(type)) {
//                String operationId = operation.getOperationId();
//                type = Character.toUpperCase(operationId.charAt(0)) + operationId.substring(1) +
//                        "Response";
                List<Node> responseDocs = new ArrayList<>();
                if (payload.get("description") != null) {
                    responseDocs.addAll(DocCommentsGenerator.createAPIDescriptionDoc(
                            payload.get("description").toString(), false));
                }
                TypeDefinitionNode typeDefinitionNode = ballerinaSchemaGenerator.getTypeDefinitionNode
                        ((BalAsyncApi25SchemaImpl) componentSchema, type, responseDocs);
                GeneratorUtils.updateTypeDefNodeList(type, typeDefinitionNode, typeDefinitionNodeList);
            }
        }
        return type;


    }
//        if (parameters != null) {
//            for (Parameter parameter : parameters) {
//                if (parameter.getDescription() != null && !parameter.getDescription().isBlank()) {
//                    MarkdownParameterDocumentationLineNode paramAPIDoc =
//                            DocCommentsGenerator.createAPIParamDoc(getValidName(
//                                    parameter.getName(), false), parameter.getDescription());
//                    remoteFunctionDoc.add(paramAPIDoc);
//                }
//                List<AnnotationNode> parameterAnnotationNodeList =
//                        getParameterAnnotationNodeList(parameter, deprecatedParamDocComments);
//                String in = parameter.getIn();
//                switch (in) {
//                    case "path":
//                        Node param = getPathParameters(parameter, createNodeList(parameterAnnotationNodeList));
//                        // Path parameters are always required.
//                        if (!isResource) {
//                            parameterList.add(param);
//                            parameterList.add(comma);
//                        }
//                        break;
//                    case "query":
//                        Node paramq = getQueryParameters(parameter, createNodeList(parameterAnnotationNodeList));
//                        if (paramq instanceof RequiredParameterNode) {
//                            parameterList.add(paramq);
//                            parameterList.add(comma);
//                        } else {
//                            defaultable.add(paramq);
//                            defaultable.add(comma);
//                        }
//                        break;
//                    case "header":
//                        Node paramh = getHeaderParameter(parameter, createNodeList(parameterAnnotationNodeList));
//                        if (paramh instanceof RequiredParameterNode) {
//                            parameterList.add(paramh);
//                            parameterList.add(comma);
//                        } else {
//                            defaultable.add(paramh);
//                            defaultable.add(comma);
//                        }
//                        break;
//                    default:
//                        break;
//                }
//            }
//        }

//        // Handle RequestBody
//        if (operation.getRequestBody() != null) {
//            setRequestBodyParameters(operation.getOperationId(), operation.getRequestBody(), remoteFunctionDoc,
//                    parameterList, defaultable);
//        }
//        remoteFunctionDoc.addAll(deprecatedParamDocComments);
//        //Filter defaultable parameters
//        if (!defaultable.isEmpty()) {
//            parameterList.addAll(defaultable);
//        }
//    }

//    private List<AnnotationNode> getParameterAnnotationNodeList(Parameter parameter,
//                                                                List<Node> deprecatedParamDocComments) {
//        List<AnnotationNode> parameterAnnotationNodeList = new ArrayList<>();
//        DocCommentsGenerator.extractDisplayAnnotation(parameter.getExtensions(), parameterAnnotationNodeList);
//        if (parameter.getDeprecated() != null && parameter.getDeprecated()) {
//            if (!this.deprecatedParamFound) {
//                deprecatedParamDocComments.addAll(DocCommentsGenerator.createAPIDescriptionDoc(
//                        "# Deprecated parameters", false));
//                this.deprecatedParamFound = true;
//            }
//            String deprecatedDescription = "";
//            if (parameter.getExtensions() != null) {
//                for (Map.Entry<String, Object> extension : parameter.getExtensions().entrySet()) {
//                    if (extension.getKey().trim().equals(X_BALLERINA_DEPRECATED_REASON)) {
//                        deprecatedDescription = extension.getValue().toString();
//                        break;
//                    }
//                }
//            }
//            MarkdownParameterDocumentationLineNode paramAPIDoc =
//                    DocCommentsGenerator.createAPIParamDoc(getValidName(
//                            parameter.getName(), false), deprecatedDescription);
//            deprecatedParamDocComments.add(paramAPIDoc);
//            parameterAnnotationNodeList.add(createAnnotationNode(createToken(AT_TOKEN),
//                    createSimpleNameReferenceNode(createIdentifierToken("deprecated")), null));
//        }
//        return parameterAnnotationNodeList;
//    }

    /**
     * Create parameter for remote function.
     * <p>
     */
    public Node getRequestTypeParameterNode(String paramType)
            throws BallerinaAsyncApiException {


        TypeDescriptorNode typeName;
        typeName = createBuiltinSimpleNameReferenceNode(null, createIdentifierToken(paramType));
        IdentifierToken paramName =
                createIdentifierToken(getValidName(paramType, false));
        return createRequiredParameterNode(createNodeList(new ArrayList<>()), typeName, paramName);
//        if (parameter.getRequired()) {
//            typeName = createBuiltinSimpleNameReferenceNode(null, createIdentifierToken(paramType));
//            IdentifierToken paramName =
//                    createIdentifierToken(getValidName(parameter.getName().trim(), false));
//            return createRequiredParameterNode(createNodeList(new ArrayList<>()), typeName, paramName);
//        } else {
//            IdentifierToken paramName =
//                    createIdentifierToken(getValidName(parameter.getName().trim(), false));
//            // Handle given default values in query parameter.
//            if (parameterSchema.getDefault() != null) {
//                typeName = createBuiltinSimpleNameReferenceNode(null, createIdentifierToken(paramType));
//                LiteralValueToken literalValueToken;
//                if (parameterSchema.getType().equals(STRING)) {
//                    literalValueToken = createLiteralValueToken(null,
//                            '"' + parameterSchema.getDefault().toString() + '"', createEmptyMinutiaeList(),
//                            createEmptyMinutiaeList());
//                } else {
//                    literalValueToken =
//                            createLiteralValueToken(null, parameterSchema.getDefault().toString(),
//                                    createEmptyMinutiaeList(),
//                                    createEmptyMinutiaeList());
//
//                }
//                return createDefaultableParameterNode(parameterAnnotationNodeList, typeName, paramName,
//                        createToken(EQUAL_TOKEN), literalValueToken);
//            } else {
//                typeName = createOptionalTypeDescriptorNode(createBuiltinSimpleNameReferenceNode(null,
//                        createIdentifierToken(paramType)), createToken(QUESTION_MARK_TOKEN));
//                NilLiteralNode nilLiteralNode =
//                        createNilLiteralNode(createToken(OPEN_PAREN_TOKEN), createToken(CLOSE_PAREN_TOKEN));
//                return createDefaultableParameterNode(parameterAnnotationNodeList, typeName, paramName,
//                        createToken(EQUAL_TOKEN), nilLiteralNode);
//            }
//        }
    }

//    /**
//     * Create query parameters.
//     * <p>
//     * Note: currently ballerina supports for this data type.
//     * type BasicType boolean|int|float|decimal|string;
//     * public type QueryParamType ()|BasicType|BasicType[];
//     */
//    public Node getQueryParameters(Parameter parameter, NodeList<AnnotationNode> parameterAnnotationNodeList)
//            throws BallerinaOpenApiException {
//
//        TypeDescriptorNode typeName;
//
//        Schema parameterSchema = parameter.getSchema();
//        String paramType = "";
//        if (parameterSchema.get$ref() != null) {
//            paramType = getValidName(extractReferenceType(parameterSchema.get$ref()), true);
//        } else {
//            paramType = convertOpenAPITypeToBallerina(parameterSchema.getType().trim());
//            if (parameterSchema.getType().equals(NUMBER)) {
//                if (parameterSchema.getFormat() != null) {
//                    paramType = convertOpenAPITypeToBallerina(parameterSchema.getFormat().trim());
//                }
//            } else if (parameterSchema instanceof ArraySchema) {
//                ArraySchema arraySchema = (ArraySchema) parameterSchema;
//                if (arraySchema.getItems().getType() != null) {
//                    String itemType = arraySchema.getItems().getType();
//                    if (itemType.equals(STRING) || itemType.equals(INTEGER) || itemType.equals(BOOLEAN)) {
//                        paramType = convertOpenAPITypeToBallerina(itemType) + SQUARE_BRACKETS;
//                    } else if (itemType.equals(NUMBER)) {
//                        paramType = convertOpenAPITypeToBallerina
//                                (arraySchema.getItems().getFormat().trim()) + SQUARE_BRACKETS;
//                    } else {
//                        throw new BallerinaOpenApiException("Unsupported parameter type is found in the parameter
//                        : " +
//                                parameter.getName());
//                    }
//                } else if (arraySchema.getItems().get$ref() != null) {
//                    paramType = getValidName(extractReferenceType(
//                            arraySchema.getItems().get$ref().trim()), true) + SQUARE_BRACKETS;
//                } else {
//                    throw new BallerinaOpenApiException("Please define the array item type of the parameter : " +
//                            parameter.getName());
//                }
//            }
//        }
//        if (parameter.getRequired()) {
//            typeName = createBuiltinSimpleNameReferenceNode(null, createIdentifierToken(paramType));
//            IdentifierToken paramName =
//                    createIdentifierToken(getValidName(parameter.getName().trim(), false));
//            return createRequiredParameterNode(parameterAnnotationNodeList, typeName, paramName);
//        } else {
//            IdentifierToken paramName =
//                    createIdentifierToken(getValidName(parameter.getName().trim(), false));
//            // Handle given default values in query parameter.
//            if (parameterSchema.getDefault() != null) {
//                typeName = createBuiltinSimpleNameReferenceNode(null, createIdentifierToken(paramType));
//                LiteralValueToken literalValueToken;
//                if (parameterSchema.getType().equals(STRING)) {
//                    literalValueToken = createLiteralValueToken(null,
//                            '"' + parameterSchema.getDefault().toString() + '"', createEmptyMinutiaeList(),
//                            createEmptyMinutiaeList());
//                } else {
//                    literalValueToken =
//                            createLiteralValueToken(null, parameterSchema.getDefault().toString(),
//                                    createEmptyMinutiaeList(),
//                                    createEmptyMinutiaeList());
//
//                }
//                return createDefaultableParameterNode(parameterAnnotationNodeList, typeName, paramName,
//                        createToken(EQUAL_TOKEN), literalValueToken);
//            } else {
//                typeName = createOptionalTypeDescriptorNode(createBuiltinSimpleNameReferenceNode(null,
//                        createIdentifierToken(paramType)), createToken(QUESTION_MARK_TOKEN));
//                NilLiteralNode nilLiteralNode =
//                        createNilLiteralNode(createToken(OPEN_PAREN_TOKEN), createToken(CLOSE_PAREN_TOKEN));
//                return createDefaultableParameterNode(parameterAnnotationNodeList, typeName, paramName,
//                        createToken(EQUAL_TOKEN), nilLiteralNode);
//            }
//        }
//    }

//    /**
//     * Create path parameters.
//     */
//    public Node getPathParameters(Parameter parameter, NodeList<AnnotationNode> parameterAnnotationNodeList)
//            throws BallerinaOpenApiException {
//
//        IdentifierToken paramName = createIdentifierToken(getValidName(parameter.getName(), false));
//        String type = "";
//        Schema parameterSchema = parameter.getSchema();
//        if (parameterSchema.get$ref() != null) {
//            type = getValidName(extractReferenceType(parameterSchema.get$ref()), true);
//            Schema schema = asyncAPI.getComponents().getSchemas().get(type.trim());
//            if (schema instanceof ObjectSchema) {
//                throw new BallerinaOpenApiException("Ballerina does not support object type path parameters.");
//            }
//        } else {
//            type = convertOpenAPITypeToBallerina(parameter.getSchema().getType().trim());
//            if (type.equals("anydata") || type.equals(SQUARE_BRACKETS) || type.equals("record {}")) {
//                throw new BallerinaOpenApiException(invalidPathParamType(parameter.getName().trim()));
//            }
//        }
//        BuiltinSimpleNameReferenceNode typeName = createBuiltinSimpleNameReferenceNode(null,
//                createIdentifierToken(type));
//        return createRequiredParameterNode(parameterAnnotationNodeList, typeName, paramName);
//    }

//    /**
//     * Create header when it comes under the parameter section in swagger.
//     */
//    private Node getHeaderParameter(Parameter parameter, NodeList<AnnotationNode> parameterAnnotationNodeList)
//            throws BallerinaOpenApiException {
//
//        Schema schema = parameter.getSchema();
//        IdentifierToken paramName = createIdentifierToken(getValidName(parameter.getName().trim(), false));
//        return getHeader(parameter.getRequired(), schema, paramName, parameterAnnotationNodeList);
//    }
//
//    /**
//     * Create header when it comes under the multipart/form-data encoding section in swagger.
//     */
//    private Node getHeaderEncoding(Map.Entry<String, Header> parameter, List<Node> remoteFunctionDoc)
//            throws BallerinaOpenApiException {
//
//        boolean isRequired = false;
//        Schema schema = parameter.getValue().getSchema();
//        IdentifierToken paramName = createIdentifierToken(getValidName(parameter.getKey(), false));
//        List<AnnotationNode> parameterAnnotationNodeList = new ArrayList<>();
//
//        if (parameter.getValue().getDescription() != null && !parameter.getValue().getDescription().isBlank()) {
//            MarkdownParameterDocumentationLineNode paramAPIDoc = DocCommentsGenerator.createAPIParamDoc(
//                    getValidName(parameter.getKey(), false), parameter.getValue().getDescription());
//            remoteFunctionDoc.add(paramAPIDoc);
//        }
//        DocCommentsGenerator.extractDisplayAnnotation(parameter.getValue().getExtensions(),
//                parameterAnnotationNodeList);
//
//        if (parameter.getValue().getRequired() != null) {
//            isRequired = parameter.getValue().getRequired();
//        }
//        return getHeader(isRequired, schema, paramName, createNodeList(parameterAnnotationNodeList));
//    }
//
//    /**
//     * Create header for header parameter and encoding.
//     */
//    private Node getHeader(boolean isRequiredHeader, Schema schema, IdentifierToken paramName,
//                           NodeList<AnnotationNode> parameterAnnotationNodeList) throws BallerinaOpenApiException {
//
//        if (isRequiredHeader) {
//            String type = convertOpenAPITypeToBallerina(schema.getType().trim());
//            if (schema instanceof ArraySchema) {
//                ArraySchema arraySchema = (ArraySchema) schema;
//                if (arraySchema.getItems().get$ref() != null) {
//                    type = extractReferenceType(arraySchema.getItems().get$ref()) + SQUARE_BRACKETS;
//                } else {
//                    type = convertOpenAPITypeToBallerina(arraySchema.getItems().getType().trim()) + SQUARE_BRACKETS;
//                }
//            }
//            BuiltinSimpleNameReferenceNode typeName = createBuiltinSimpleNameReferenceNode(null,
//                    createIdentifierToken(type));
//            return createRequiredParameterNode(parameterAnnotationNodeList, typeName, paramName);
//        } else {
//            if (schema.getDefault() != null) {
//                BuiltinSimpleNameReferenceNode typeName = createBuiltinSimpleNameReferenceNode(null,
//                        createIdentifierToken(convertOpenAPITypeToBallerina(schema.getType().trim())));
//                LiteralValueToken literalValueToken;
//                if (schema.getType().equals(STRING)) {
//                    literalValueToken = createLiteralValueToken(null,
//                            '"' + schema.getDefault().toString() + '"', createEmptyMinutiaeList(),
//                            createEmptyMinutiaeList());
//                } else {
//                    literalValueToken = createLiteralValueToken(null, schema.getDefault().toString(),
//                            createEmptyMinutiaeList(), createEmptyMinutiaeList());
//                }
//                return createDefaultableParameterNode(parameterAnnotationNodeList, typeName, paramName,
//                        createToken(EQUAL_TOKEN), literalValueToken);
//            } else {
//                String type = convertOpenAPITypeToBallerina(schema.getType().trim()) + NILLABLE;
//                if (schema instanceof ArraySchema) {
//                    ArraySchema arraySchema = (ArraySchema) schema;
//                    if (arraySchema.getItems().get$ref() != null) {
//                        type = extractReferenceType(arraySchema.getItems().get$ref()) + SQUARE_BRACKETS + NILLABLE;
//                    } else {
//                        type = convertOpenAPITypeToBallerina(arraySchema.getItems().getType().trim()) +
//                        SQUARE_BRACKETS
//                                + NILLABLE;
//                    }
//                }
//                BuiltinSimpleNameReferenceNode typeName = createBuiltinSimpleNameReferenceNode(null,
//                        createIdentifierToken(type));
//                NilLiteralNode nilLiteralNode =
//                        createNilLiteralNode(createToken(OPEN_PAREN_TOKEN), createToken(CLOSE_PAREN_TOKEN));
//                return createDefaultableParameterNode(parameterAnnotationNodeList, typeName, paramName,
//                        createToken(EQUAL_TOKEN), nilLiteralNode);
//            }
//        }
//    }

//    /**
//     * Create request body parameter.
//     */
//    private void setRequestBodyParameters(String operationId, RequestBody requestBody, List<Node> requestBodyDoc,
//                                          List<Node> parameterList, List<Node> defaultable)
//            throws BallerinaAsyncApiException {
//
//        Content requestBodyContent;
//        String referencedRequestBodyName = "";
//        if (requestBody.get$ref() != null) {
//            referencedRequestBodyName = extractReferenceType(requestBody.get$ref()).trim();
//            RequestBody referencedRequestBody = asyncAPI.getComponents()
//                    .getRequestBodies().get(referencedRequestBodyName);
//            requestBodyContent = referencedRequestBody.getContent();
//            // note : when there is referenced request body, the description at the reference is ignored.
//            // Need to consider the description at the component level
//            requestBody.setDescription(referencedRequestBody.getDescription());
//        } else {
//            requestBodyContent = requestBody.getContent();
//        }
//
//        Iterator<Map.Entry<String, MediaType>> iterator = requestBodyContent.entrySet().iterator();
//        while (iterator.hasNext()) {
//            // This implementation currently for first content type
//            Map.Entry<String, MediaType> mediaTypeEntry = iterator.next();
//            Schema schema = mediaTypeEntry.getValue().getSchema();
//            String paramType = "";
//            //Take payload type
//            if (schema != null && GeneratorUtils.isSupportedMediaType(mediaTypeEntry)) {
//                String mediaTypeEntryKey = mediaTypeEntry.getKey();
//                if (mediaTypeEntryKey.equals(APPLICATION_OCTET_STREAM) ||
//                        mediaTypeEntryKey.matches("application/.*\\+octet-stream")) {
//                     paramType = getBallerinaMediaType(mediaTypeEntryKey, true);
//                } else {
//                    if (schema.get$ref() != null) {
//                        paramType = getValidName(extractReferenceType(schema.get$ref().trim()), true);
//                    } else if (schema.getType() != null && !schema.getType().equals(ARRAY) && !schema.getType().
//                    equals(
//                            OBJECT)) {
//                        String typeOfPayload = schema.getType().trim();
//                        if (typeOfPayload.equals(STRING) && schema.getFormat() != null
//                                && (schema.getFormat().equals(BINARY) || schema.getFormat().equals(BYTE))) {
//                            paramType = convertAsyncAPITypeToBallerina(schema.getFormat());
//                        } else {
//                            paramType = convertAsyncAPITypeToBallerina(typeOfPayload);
//                        }
//                    } else if (schema instanceof ArraySchema) {
//                        //TODO: handle nested array - this is impossible to handle
//                        ArraySchema arraySchema = (ArraySchema) schema;
//                        paramType = getRequestBodyParameterForArraySchema(operationId, mediaTypeEntry, arraySchema);
//                    } else if (schema instanceof ObjectSchema) {
//                        ObjectSchema objectSchema = (ObjectSchema) schema;
//                        paramType = referencedRequestBodyName.isBlank() ? paramType : referencedRequestBodyName;
//                        getRequestBodyParameterForObjectSchema(referencedRequestBodyName, objectSchema);
//                    } else { // composed and object schemas are handled by the flatten
//                        paramType = getBallerinaMediaType(mediaTypeEntryKey, true);
//                    }
//                }
//            } else {
//                paramType = getBallerinaMediaType(mediaTypeEntry.getKey(), true);
//            }
//
//            String paramName = paramType.equals(HTTP_REQUEST) ? REQUEST : PAYLOAD;
//            if (!paramType.isBlank()) {
//                List<AnnotationNode> annotationNodes = new ArrayList<>();
//                DocCommentsGenerator.extractDisplayAnnotation(requestBody.getExtensions(), annotationNodes);
//                SimpleNameReferenceNode typeName = createSimpleNameReferenceNode(createIdentifierToken(paramType));
//                IdentifierToken paramNameToken = createIdentifierToken(paramName);
//                RequiredParameterNode payload = createRequiredParameterNode(
//                        createNodeList(annotationNodes), typeName, paramNameToken);
//                if (requestBody.getDescription() != null && !requestBody.getDescription().isBlank()) {
//                    MarkdownParameterDocumentationLineNode paramAPIDoc =
//                            DocCommentsGenerator.createAPIParamDoc(escapeIdentifier(paramName),
//                                    requestBody.getDescription().split("\n")[0]);
//                    requestBodyDoc.add(paramAPIDoc);
//                }
//                parameterList.add(payload);
//                parameterList.add(createToken((COMMA_TOKEN)));
//            }
//
//            if (mediaTypeEntry.getKey().equals(javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA)
//                    && mediaTypeEntry.getValue().getEncoding() != null) {
//                List<String> headerList = new ArrayList<>();
//                for (Map.Entry<String, Encoding> entry : mediaTypeEntry.getValue().getEncoding().entrySet()) {
//                    if (entry.getValue().getHeaders() != null) {
//                        for (Map.Entry<String, Header> header : entry.getValue().getHeaders().entrySet()) {
//                            if (!headerList.contains(header.getKey())) {
//                                Node headerParameter = getHeaderEncoding(header, requestBodyDoc);
//                                if (headerParameter instanceof RequiredParameterNode) {
//                                    parameterList.add(headerParameter);
//                                    parameterList.add(createToken((COMMA_TOKEN)));
//                                } else {
//                                    defaultable.add(headerParameter);
//                                    defaultable.add(createToken((COMMA_TOKEN)));
//                                }
//                                headerList.add(header.getKey());
//                            }
//                        }
//                    }
//                }
//            }
//            break;
//        }
//    }

//    private void getRequestBodyParameterForObjectSchema (String recordName, ObjectSchema objectSchema)
//            throws BallerinaAsyncApiException {
//        TypeDefinitionNode record =
//                ballerinaSchemaGenerator.getTypeDefinitionNode(objectSchema, recordName, new ArrayList<>());
//        GeneratorUtils.updateTypeDefNodeList(recordName, record, typeDefinitionNodeList);
//    }

//    /**
//     * Generate RequestBody for array type schema.
//     */
//    private String getRequestBodyParameterForArraySchema(String operationId, Map.Entry<String, MediaType> next,
//                                                         ArraySchema arraySchema) throws BallerinaAsyncApiException {
//
//        String paramType;
//        Schema<?> arrayItems = arraySchema.getItems();
//        if (arrayItems.getType() != null) {
//            paramType = convertAsyncAPITypeToBallerina(arrayItems.getType()) + SQUARE_BRACKETS;
//        } else if (arrayItems.get$ref() != null) {
//            paramType = getValidName(extractReferenceType(arrayItems.get$ref()), true) + SQUARE_BRACKETS;
//        } else if (arrayItems instanceof ComposedSchema) {
//            paramType = "CompoundArrayItem" + getValidName(operationId, true) + "Request";
//            // TODO - Add API doc by checking requestBody
//            TypeDefinitionNode arrayTypeNode =
//                    ballerinaSchemaGenerator.getTypeDefinitionNode(arraySchema, paramType, new ArrayList<>());
//            GeneratorUtils.updateTypeDefNodeList(paramType, arrayTypeNode, typeDefinitionNodeList);
//        } else {
//            paramType = getBallerinaMediaType(next.getKey().trim(), true) + SQUARE_BRACKETS;
//        }
//        return paramType;
//    }

}
