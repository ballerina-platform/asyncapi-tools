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
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25DocumentImpl;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25SchemaImpl;
import io.ballerina.asyncapi.core.GeneratorUtils;
import io.ballerina.asyncapi.core.exception.BallerinaAsyncApiException;
import io.ballerina.asyncapi.core.generators.document.DocCommentsGenerator;
import io.ballerina.asyncapi.core.generators.schema.BallerinaTypesGenerator;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static io.ballerina.asyncapi.core.GeneratorConstants.*;
import static io.ballerina.asyncapi.core.GeneratorUtils.*;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTypeDefinitionNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PIPE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SEMICOLON_TOKEN;

/**
 * This util class for maintain the operation response with ballerina return type.
 *
 * @since 1.3.0
 */
public class FunctionReturnTypeGenerator {
    private AsyncApi25DocumentImpl asyncAPI;
    private BallerinaTypesGenerator ballerinaSchemaGenerator;
    private List<TypeDefinitionNode> typeDefinitionNodeList = new LinkedList<>();

    public FunctionReturnTypeGenerator() {

    }

    public FunctionReturnTypeGenerator(AsyncApi25DocumentImpl asyncAPI, BallerinaTypesGenerator ballerinaSchemaGenerator,
                                       List<TypeDefinitionNode> typeDefinitionNodeList) {

        this.asyncAPI = asyncAPI;
        this.ballerinaSchemaGenerator = ballerinaSchemaGenerator;
        this.typeDefinitionNodeList = typeDefinitionNodeList;
    }

    /**
     * Get return type of the remote function.
     *
     * @param operation swagger operation.
     * @return string with return type.
     * @throws BallerinaAsyncApiException - throws exception if creating return type fails.
     */
    public String getReturnType(JsonNode payload, boolean isSignature) throws BallerinaAsyncApiException {
        //TODO: Handle multiple media-type
        Set<String> returnTypes = new HashSet<>();
        boolean noContentResponseFound = false;
        if (operation.getResponses() != null) {
            ApiResponses responses = operation.getResponses();
            for (Map.Entry<String, ApiResponse> entry : responses.entrySet()) {
                String statusCode = entry.getKey();
                ApiResponse response = entry.getValue();
                if (statusCode.startsWith("2")) {
                    Content content = response.getContent();
                    if (content != null && content.size() > 0) {
                        Set<Map.Entry<String, MediaType>> mediaTypes = content.entrySet();
                        for (Map.Entry<String, MediaType> media : mediaTypes) {
                            String type = "";
                            if (media.getValue().getSchema() != null) {
                                Schema schema = media.getValue().getSchema();
                                type = getDataType(operation, isSignature, response, media, type, schema);
                            } else {
                                type = GeneratorUtils.getBallerinaMediaType(media.getKey().trim(), false);
                            }
                            returnTypes.add(type);
                            // Currently support for first media type
                            break;
                        }
                    } else {
                        noContentResponseFound = true;
                    }
                }
            }
        }
        if (returnTypes.size() > 0) {
            StringBuilder finalReturnType = new StringBuilder();
            finalReturnType.append(String.join(PIPE_TOKEN.stringValue(), returnTypes));
            finalReturnType.append(PIPE_TOKEN.stringValue());
            finalReturnType.append(ERROR);
            if (noContentResponseFound) {
                finalReturnType.append(NILLABLE);
            }
            return finalReturnType.toString();
        } else {
            return DEFAULT_RETURN;
        }
    }

    public String getDType(JsonNode payload) throws BallerinaAsyncApiException {
        String type="";
        if (payload.get("$ref") != null) {
            type = getValidName(extractReferenceType(payload.get("$ref").toString()), true);
            AsyncApi25SchemaImpl componentSchema = (AsyncApi25SchemaImpl) asyncAPI.getComponents().getSchemas().get(type);
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
                        (componentSchema, type, responseDocs);
                GeneratorUtils.updateTypeDefNodeList(type, typeDefinitionNode, typeDefinitionNodeList);
            }
        }


    }



    /**
     * Get the return data type according to the OAS ArraySchema.
     */
    private String generateReturnTypeForArraySchema(Map.Entry<String, MediaType> media, ArraySchema arraySchema,
                                                    boolean isSignature) throws BallerinaOpenApiException {

        String type;
        if (arraySchema.getItems().get$ref() != null) {
            String name = getValidName(extractReferenceType(arraySchema.getItems().get$ref()), true);
            type = name + "[]";
            String typeName = name + "Arr";
            TypeDefinitionNode typeDefNode = createTypeDefinitionNode(null, null,
                    createIdentifierToken("public type"),
                    createIdentifierToken(typeName),
                    createSimpleNameReferenceNode(createIdentifierToken(type)),
                    createToken(SEMICOLON_TOKEN));
            // Check already typeDescriptor has same name
            GeneratorUtils.updateTypeDefNodeList(typeName, typeDefNode, typeDefinitionNodeList);
            if (!isSignature) {
                type = typeName;
            }
        } else if (arraySchema.getItems().getType() == null) {
            if (media.getKey().trim().equals("application/xml")) {
                type = generateCustomTypeDefine("xml[]", "XMLArr", isSignature);
            } else if (media.getKey().trim().equals("application/pdf") ||
                    media.getKey().trim().equals("image/png") ||
                    media.getKey().trim().equals("application/octet-stream")) {
                String typeName = GeneratorUtils.getBallerinaMediaType(media.getKey().trim(), false) + "Arr";
                String mappedType = GeneratorUtils.getBallerinaMediaType(media.getKey().trim(), false);
                type = generateCustomTypeDefine(mappedType, typeName, isSignature);
            } else {
                String typeName = GeneratorUtils.getBallerinaMediaType(media.getKey().trim(), false) + "Arr";
                String mappedType = GeneratorUtils.getBallerinaMediaType(media.getKey().trim(), false) + "[]";
                type = generateCustomTypeDefine(mappedType, typeName, isSignature);
            }
        } else {
            String typeName;
            if (arraySchema.getItems() instanceof ArraySchema) {
                Schema nestedSchema = arraySchema.getItems();
                ArraySchema nestedArraySchema = (ArraySchema) nestedSchema;
                String inlineArrayType = convertOpenAPITypeToBallerina(nestedArraySchema.getItems().getType());
                typeName = inlineArrayType + "NestedArr";
                type = inlineArrayType + "[][]";
            } else {
                typeName = convertOpenAPITypeToBallerina(Objects.requireNonNull(arraySchema.getItems()).getType()) +
                        "Arr";
                type = convertOpenAPITypeToBallerina(arraySchema.getItems().getType()) + "[]";
            }
            type = generateCustomTypeDefine(type, getValidName(typeName, true), isSignature);
        }
        return type;
    }

    /**
     * Get the return data type according to the OAS ComposedSchemas ex: AllOf, OneOf, AnyOf.
     */
    private String generateReturnDataTypeForComposedSchema(Operation operation, String type,
                                                           ComposedSchema composedSchema, boolean isSignature)
            throws BallerinaOpenApiException {

        if (composedSchema.getOneOf() != null) {
            // Get oneOfUnionType name
            String typeName = "OneOf" + getValidName(operation.getOperationId().trim(), true) + "Response";
            TypeDefinitionNode typeDefNode = ballerinaSchemaGenerator.getTypeDefinitionNode(
                    composedSchema, typeName, new ArrayList<>());
            GeneratorUtils.updateTypeDefNodeList(typeName, typeDefNode, typeDefinitionNodeList);
            type = typeDefNode.typeDescriptor().toString();
            if (!isSignature) {
                type = typeName;
            }
        } else if (composedSchema.getAllOf() != null) {
            String recordName = "Compound" + getValidName(operation.getOperationId(), true) +
                    "Response";
            TypeDefinitionNode allOfTypeDefinitionNode = ballerinaSchemaGenerator.getTypeDefinitionNode
                    (composedSchema, recordName, new ArrayList<>());
            GeneratorUtils.updateTypeDefNodeList(recordName, allOfTypeDefinitionNode, typeDefinitionNodeList);
            type = recordName;
        }
        return type;
    }

    /**
     * Handle inline record by generating record with name for response in OAS type ObjectSchema.
     */
    private String handleInLineRecordInResponse(Operation operation, Map.Entry<String, MediaType> media,
                                                ObjectSchema objectSchema)
            throws BallerinaOpenApiException {

        Map<String, Schema> properties = objectSchema.getProperties();
        String ref = objectSchema.get$ref();
        String type = getValidName(operation.getOperationId(), true) + "Response";

        if (ref != null) {
            type = extractReferenceType(ref.trim());
        } else if (properties != null) {
            if (properties.isEmpty()) {
                type = GeneratorUtils.getBallerinaMediaType(media.getKey().trim(), false);
            } else {
                List<Node> returnTypeDocs = new ArrayList<>();
                String description = operation.getResponses().entrySet().iterator().next().getValue().getDescription();
                if (description != null) {
                    returnTypeDocs.addAll(DocCommentsGenerator.createAPIDescriptionDoc(
                            description, false));
                }
                TypeDefinitionNode recordNode = ballerinaSchemaGenerator.getTypeDefinitionNode
                        (objectSchema, type, returnTypeDocs);
                GeneratorUtils.updateTypeDefNodeList(type, recordNode, typeDefinitionNodeList);
            }
        } else {
            type = GeneratorUtils.getBallerinaMediaType(media.getKey().trim(), false);
        }
        return type;
    }

    /**
     * Get the return data type according to the OAS MapSchema type.
     */
    private String handleResponseWithMapSchema(Operation operation, Map.Entry<String, MediaType> media,
                                               MapSchema mapSchema) throws BallerinaOpenApiException {

        Map<String, Schema> properties = mapSchema.getProperties();
        String ref = mapSchema.get$ref();
        String type = getValidName(operation.getOperationId(), true) + "Response";

        if (ref != null) {
            type = extractReferenceType(ref.trim());
        } else if (properties != null) {
            if (properties.isEmpty()) {
                type = GeneratorUtils.getBallerinaMediaType(media.getKey().trim(), false);
            } else {
                List<Node> schemaDocs = new ArrayList<>();
                String description = operation.getResponses().entrySet().iterator().next().getValue().getDescription();
                if (description != null) {
                    schemaDocs.addAll(DocCommentsGenerator.createAPIDescriptionDoc(
                            description, false));
                }
                TypeDefinitionNode recordNode = ballerinaSchemaGenerator.getTypeDefinitionNode
                        (mapSchema, type, schemaDocs);
                GeneratorUtils.updateTypeDefNodeList(type, recordNode, typeDefinitionNodeList);
            }
        } else {
            type = GeneratorUtils.getBallerinaMediaType(media.getKey().trim(), false);
        }
        return type;
    }

    /**
     * Generate Type for datatype that can not bind to the targetType.
     *
     * @param type     - Data Type.
     * @param typeName - Created datType name.
     * @return return dataType
     */
    private String generateCustomTypeDefine(String type, String typeName, boolean isSignature) {

        TypeDefinitionNode typeDefNode = createTypeDefinitionNode(null,
                null, createIdentifierToken("public type"),
                createIdentifierToken(typeName),
                createSimpleNameReferenceNode(createIdentifierToken(type)),
                createToken(SEMICOLON_TOKEN));
        GeneratorUtils.updateTypeDefNodeList(typeName, typeDefNode, typeDefinitionNodeList);
        if (!isSignature) {
            return typeName;
        } else {
            return type;
        }
    }
}
