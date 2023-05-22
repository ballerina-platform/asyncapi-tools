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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.apicurio.datamodels.models.Schema;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25DocumentImpl;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25SchemaImpl;
import io.ballerina.asyncapi.core.GeneratorUtils;
import io.ballerina.asyncapi.core.exception.BallerinaAsyncApiException;
import io.ballerina.asyncapi.core.generators.asyncspec.model.BalAsyncApi25SchemaImpl;
import io.ballerina.asyncapi.core.generators.schema.BallerinaTypesGenerator;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static io.ballerina.asyncapi.core.GeneratorConstants.DEFAULT_RETURN;
import static io.ballerina.asyncapi.core.GeneratorConstants.ERROR;
import static io.ballerina.asyncapi.core.GeneratorConstants.SIMPLE_RPC;
import static io.ballerina.asyncapi.core.GeneratorConstants.STREAMING;
import static io.ballerina.asyncapi.core.GeneratorConstants.X_RESPONSE;
import static io.ballerina.asyncapi.core.GeneratorConstants.X_RESPONSE_TYPE;
import static io.ballerina.asyncapi.core.GeneratorUtils.convertAsyncAPITypeToBallerina;
import static io.ballerina.asyncapi.core.GeneratorUtils.extractReferenceType;
import static io.ballerina.asyncapi.core.GeneratorUtils.getValidName;
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

    public FunctionReturnTypeGenerator(AsyncApi25DocumentImpl asyncAPI,
                                       BallerinaTypesGenerator ballerinaSchemaGenerator,
                                       List<TypeDefinitionNode> typeDefinitionNodeList) {

        this.asyncAPI = asyncAPI;
        this.ballerinaSchemaGenerator = ballerinaSchemaGenerator;
        this.typeDefinitionNodeList = typeDefinitionNodeList;
    }

    /**
     * Get return type of the remote function.
     * <p>
     * //     * @param operation swagger operation.
     *
     * @return string with return type.
     * @throws BallerinaAsyncApiException - throws exception if creating return type fails.
     */
    public String getReturnType(Map<String, JsonNode> extensions) throws BallerinaAsyncApiException {
        //TODO: Handle multiple media-type
        Set<String> returnTypes = new HashSet<>();
//        boolean noContentResponseFound = false;
//        if (operation.getResponses() != null) {
//            ApiResponses responses = operation.getResponses();
//            for (Map.Entry<String, ApiResponse> entry : responses.entrySet()) {
//                String statusCode = entry.getKey();
//                ApiResponse response = entry.getValue();
//                if (statusCode.startsWith("2")) {
//                    Content content = response.getContent();
//                    if (content != null && content.size() > 0) {
//                        Set<Map.Entry<String, MediaType>> mediaTypes = content.entrySet();
//                        for (Map.Entry<String, MediaType> media : mediaTypes) {
//                            String type = "";
//                            if (media.getValue().getSchema() != null) {
//                                Schema schema = media.getValue().getSchema();
//                                type = getDataType(operation, isSignature, response, media, type, schema);
//                            } else {
//                                type = GeneratorUtils.getBallerinaMediaType(media.getKey().trim(), false);
//                            }
//                            returnTypes.add(type);
//                            // Currently support for first media type
//                            break;
//                        }
//                    } else {
//                        noContentResponseFound = true;
//                    }
//                }
//            }
//        }
        String type = null;

        if (extensions.get(X_RESPONSE).get("oneOf") != null) {  //Handle Union references
            if(extensions.get(X_RESPONSE).get("oneOf") instanceof ArrayNode){
                ArrayNode test= (ArrayNode) extensions.get(X_RESPONSE).get("oneOf");
                if(extensions.get(X_RESPONSE_TYPE)!=null) {
                    if (extensions.get(X_RESPONSE_TYPE) == new TextNode(STREAMING)){

                    } else if (extensions.get(X_RESPONSE_TYPE) == new TextNode(SIMPLE_RPC)) {

                    }
                    for (Iterator<JsonNode> it = test.iterator(); it.hasNext(); ) {
                        JsonNode jsonNode = it.next();
                        if (jsonNode.get("$ref") != null) {
                            String reference = jsonNode.get("$ref").asText();
                            String schemaName = getValidName(extractReferenceType(reference), true);
                            AsyncApi25SchemaImpl refSchema = (AsyncApi25SchemaImpl) asyncAPI.getComponents().getSchemas().get(
                                    schemaName);
                            type = getDataType(schemaName, refSchema);
                            returnTypes.add(type);


                        } else if (jsonNode.get("payload") != null) {
                            throw new BallerinaAsyncApiException("Ballerina service file cannot be generate to the " +
                                    "given AsyncAPI specification, Response type must be a Record");
                        }

                        System.out.println("test");
                    }
                }else{
                    throw new BallerinaAsyncApiException("x-response-type must be included ex:-" +
                            " x-response-type: streaming || x-response-type: simple-rpc");
                }
            }

        } else if (extensions.get(X_RESPONSE).get("payload")!=null && extensions.get(X_RESPONSE).get("payload")
                .get("type") != new TextNode("object")) { //Handle payload references
            throw new BallerinaAsyncApiException("Ballerina service file cannot be generate to the " +
                    "given AsyncAPI specification, Response type must be a Record");



        } else if (extensions.get(X_RESPONSE).get("$ref") != null) { //Handle reference responses
            String reference = extensions.get(X_RESPONSE).get("$ref").asText();
            String schemaName = getValidName(extractReferenceType(reference), true);
            AsyncApi25SchemaImpl refSchema = (AsyncApi25SchemaImpl) asyncAPI.getComponents().getSchemas().get(
                    schemaName);
            type = getDataType(schemaName, refSchema);
            returnTypes.add(type);

        }

        //Add |error to the response
        if (returnTypes.size() > 0) {
            String finalReturnType = String.join(PIPE_TOKEN.stringValue(), returnTypes) +
                    PIPE_TOKEN.stringValue() +
                    ERROR;
//            if (noContentResponseFound) {
            //TODO: change this after figure out

//                finalReturnType.append(NILLABLE);
//            }
            return finalReturnType;
        } else {
            return DEFAULT_RETURN;
        }
    }


    /**
     * Get return data type by traversing AsyncAPI schemas.
     */
    private String getDataType(String schemaName,AsyncApi25SchemaImpl schema)
            throws BallerinaAsyncApiException {
        String type = null;

        if(schema.getType().equals("object")){
            type = handleInLineRecordInResponse(schemaName, schema);

        }else{
            throw new BallerinaAsyncApiException("Response type must be a record");
        }
//
//        if (((schema.getProperties() != null &&
//                (schema.getOneOf() != null || schema.getAllOf() != null || schema.getAnyOf() != null)))) {
//            type = generateReturnDataTypeForComposedSchema(schemaName, schema, type);
//        } else if (schema.getType().equals("object")) {
//            type = handleInLineRecordInResponse(schemaName, schema);
////        } else if (schema instanceof MapSchema) {
////            type = handleResponseWithMapSchema(operation, media, mapSchema);
//        } else if (schema.get$ref() != null) {
//            type = getValidName(extractReferenceType(schema.get$ref()), true);
//            Schema componentSchema = asyncAPI.getComponents().getSchemas().get(type);
//            if (!isValidSchemaName(type)) {
////                String operationId = operation.getOperationId();
////                type = Character.toUpperCase(operationId.charAt(0)) + operationId.substring(1) +
////                        "Response";
//                List<Node> responseDocs = new ArrayList<>();
//                if (schema.getDescription() != null && !schema.getDescription().isBlank()) {
//                    responseDocs.addAll(DocCommentsGenerator.createAPIDescriptionDoc(schema.getDescription(),
//                            false));
//                }
//                TypeDefinitionNode typeDefinitionNode = ballerinaSchemaGenerator.getTypeDefinitionNode
//                        ((AsyncApi25SchemaImpl) componentSchema, type, responseDocs);
//                GeneratorUtils.updateTypeDefNodeList(type, typeDefinitionNode, typeDefinitionNodeList);
//            }
//        } else if (schema.getType().equals("array")) {
//            // TODO: Nested array when response has
//            type = generateReturnTypeForArraySchema(schema);
//        } else if (schema.getType() != null) {
//            type = convertAsyncAPITypeToBallerina(schema.getType());
////        } else if (media.getKey().trim().equals("application/xml")) {
////            type = generateCustomTypeDefine("xml", "XML", isSignature);
////        } else {
////            type = GeneratorUtils.getBallerinaMediaType(media.getKey().trim(), false);
//        }
        return type;
    }


    /**
     * Get the return data type according to the AsyncAPI ArraySchema.
     */
    private String generateReturnTypeForArraySchema(AsyncApi25SchemaImpl arraySchema) throws
            BallerinaAsyncApiException {

        String type;
        AsyncApi25SchemaImpl arraySchemaItems = (AsyncApi25SchemaImpl) arraySchema.getItems().asSchema();
        if (arraySchemaItems.get$ref() != null) {
            String name = getValidName(extractReferenceType(arraySchemaItems.get$ref()), true);
            type = name + "[]";
            String typeName = name + "Arr";
            TypeDefinitionNode typeDefNode = createTypeDefinitionNode(null, null,
                    createIdentifierToken("public type"),
                    createIdentifierToken(typeName),
                    createSimpleNameReferenceNode(createIdentifierToken(type)),
                    createToken(SEMICOLON_TOKEN));
            // Check already typeDescriptor has same name
            GeneratorUtils.updateTypeDefNodeList(typeName, typeDefNode, typeDefinitionNodeList);
//            if (!isSignature) {
//                type = typeName;
//            }
//        } else if (arraySchema.getItems().getType() == null) {
//            if (media.getKey().trim().equals("application/xml")) {
//                type = generateCustomTypeDefine("xml[]", "XMLArr", isSignature);
//            } else if (media.getKey().trim().equals("application/pdf") ||
//                    media.getKey().trim().equals("image/png") ||
//                    media.getKey().trim().equals("application/octet-stream")) {
//                String typeName = GeneratorUtils.getBallerinaMediaType(media.getKey().trim(), false) + "Arr";
//                String mappedType = GeneratorUtils.getBallerinaMediaType(media.getKey().trim(), false);
//                type = generateCustomTypeDefine(mappedType, typeName, isSignature);
//            } else {
//                String typeName = GeneratorUtils.getBallerinaMediaType(media.getKey().trim(), false) + "Arr";
//                String mappedType = GeneratorUtils.getBallerinaMediaType(media.getKey().trim(), false) + "[]";
//                type = generateCustomTypeDefine(mappedType, typeName, isSignature);
//            }
        } else {
            String typeName;
            ObjectMapper objMapper = new ObjectMapper();
            AsyncApi25SchemaImpl nestedSchema = null;
//            try {
//                nestedSchema = objMapper.treeToValue(arraySchema.getItems(), AsyncApi25SchemaImpl.class);
//            } catch (JsonProcessingException e) {
//                throw new RuntimeException(e);
//            }
            nestedSchema = (AsyncApi25SchemaImpl) arraySchemaItems.asSchema();
            if (arraySchema.getItems() != null) {
//                AsyncApi25SchemaImpl nestedSchema=objMapper.treeToValue(arraySchema.getItems(),
//                        AsyncApi25SchemaImpl.class);
                AsyncApi25SchemaImpl nestedArraySchema = null;
//                try {
//                    nestedArraySchema = objMapper.treeToValue(nestedSchema.getItems(), AsyncApi25SchemaImpl.class);
//                } catch (JsonProcessingException e) {
//                    throw new RuntimeException(e);
//                }
                nestedArraySchema = (AsyncApi25SchemaImpl) nestedSchema.getItems();
//                Schema nestedSchema = arraySchema.getItems();
//                ArraySchema nestedArraySchema = (ArraySchema) nestedSchema;
                String inlineArrayType = convertAsyncAPITypeToBallerina(nestedArraySchema.getType());
                typeName = inlineArrayType + "NestedArr";
                type = inlineArrayType + "[][]";
            } else {
                typeName = convertAsyncAPITypeToBallerina(Objects.requireNonNull(nestedSchema).getType()) +
                        "Arr";
                type = convertAsyncAPITypeToBallerina(nestedSchema.getType()) + "[]";
            }
            type = generateCustomTypeDefine(type, getValidName(typeName, true));
        }
        return type;
    }

    /**
     * Get the return data type according to the AsyncAPI ComposedSchemas ex: AllOf, OneOf, AnyOf.
     */
    private String generateReturnDataTypeForComposedSchema(String schemaName, BalAsyncApi25SchemaImpl composedSchema,
                                                           String type)
            throws BallerinaAsyncApiException {

        if (composedSchema.getOneOf() != null) {
            // Get oneOfUnionType name
            String typeName = "OneOf" + getValidName(schemaName.trim(), true) + "Response";
            TypeDefinitionNode typeDefNode = ballerinaSchemaGenerator.getTypeDefinitionNode(
                    composedSchema, typeName, new ArrayList<>());
            GeneratorUtils.updateTypeDefNodeList(typeName, typeDefNode, typeDefinitionNodeList);
            type = typeDefNode.typeDescriptor().toString();
//            if (!isSignature) {
//                type = typeName;
//            }
        } else if (composedSchema.getAllOf() != null) {
            String recordName = "Compound" + getValidName(schemaName, true) +
                    "Response";
            TypeDefinitionNode allOfTypeDefinitionNode = ballerinaSchemaGenerator.getTypeDefinitionNode
                    (composedSchema, recordName, new ArrayList<>());
            GeneratorUtils.updateTypeDefNodeList(recordName, allOfTypeDefinitionNode, typeDefinitionNodeList);
            type = recordName;
        }
        return type;
    }

    /**
     * Handle inline record by generating record with name for response in AsyncAPI type ObjectSchema.
     */
    private String handleInLineRecordInResponse(String schemaName, AsyncApi25SchemaImpl objectSchema)
            throws BallerinaAsyncApiException {

        Map<String, Schema> properties = objectSchema.getProperties();
        String ref = objectSchema.get$ref();
//        String type = getValidName(schemaName, true) + "Response";
        String type = getValidName(schemaName, true) ;


        if (ref != null) {
            type = extractReferenceType(ref.trim());
        } else if (properties != null) {
//            if (properties.isEmpty()) {
//                type = GeneratorUtils.getBallerinaMediaType(media.getKey().trim(), false);
//            } else {
//            List<Node> returnTypeDocs = new ArrayList<>();
//                String description = operation.getResponses().entrySet().iterator().next().getValue().
//               getDescription();
//                if (description != null) {
//                    returnTypeDocs.addAll(DocCommentsGenerator.createAPIDescriptionDoc(
//                            description, false));
//                }
            //create return type schema
//            TypeDefinitionNode recordNode = ballerinaSchemaGenerator.getTypeDefinitionNode
//                    (objectSchema, type, returnTypeDocs);
//            GeneratorUtils.updateTypeDefNodeList(type, recordNode, typeDefinitionNodeList);
//            }
//        } else {
//            type = GeneratorUtils.getBallerinaMediaType(media.getKey().trim(), false);
        }
        return type;


    }

//    /**
//     * Get the return data type according to the AsyncAPI MapSchema type.
//     */
//    private String handleResponseWithMapSchema(Operation operation, Map.Entry<String, MediaType> media,
//                                               MapSchema mapSchema) throws BallerinaOpenApiException {
//
//        Map<String, Schema> properties = mapSchema.getProperties();
//        String ref = mapSchema.get$ref();
//        String type = getValidName(operation.getOperationId(), true) + "Response";
//
//        if (ref != null) {
//            type = extractReferenceType(ref.trim());
//        } else if (properties != null) {
//            if (properties.isEmpty()) {
//                type = GeneratorUtils.getBallerinaMediaType(media.getKey().trim(), false);
//            } else {
//                List<Node> schemaDocs = new ArrayList<>();
//                String description = operation.getResponses().entrySet().iterator().next().getValue().
//                getDescription();
//                if (description != null) {
//                    schemaDocs.addAll(DocCommentsGenerator.createAPIDescriptionDoc(
//                            description, false));
//                }
//                TypeDefinitionNode recordNode = ballerinaSchemaGenerator.getTypeDefinitionNode
//                        (mapSchema, type, schemaDocs);
//                GeneratorUtils.updateTypeDefNodeList(type, recordNode, typeDefinitionNodeList);
//            }
//        } else {
//            type = GeneratorUtils.getBallerinaMediaType(media.getKey().trim(), false);
//        }
//        return type;
//    }

    /**
     * Generate Type for datatype that can not bind to the targetType.
     *
     * @param type     - Data Type.
     * @param typeName - Created datType name.
     * @return return dataType
     */
    private String generateCustomTypeDefine(String type, String typeName) {

        TypeDefinitionNode typeDefNode = createTypeDefinitionNode(null,
                null, createIdentifierToken("public type"),
                createIdentifierToken(typeName),
                createSimpleNameReferenceNode(createIdentifierToken(type)),
                createToken(SEMICOLON_TOKEN));
        GeneratorUtils.updateTypeDefNodeList(typeName, typeDefNode, typeDefinitionNodeList);
//        if (!isSignature) {
//            return typeName;
//        } else {
        return type;
//        }
    }
}
