/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
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

package io.ballerina.asyncapi.codegenerator.usecase;

import io.apicurio.datamodels.asyncapi.models.AaiDocument;
import io.apicurio.datamodels.asyncapi.models.AaiSchema;
import io.ballerina.asyncapi.codegenerator.configuration.BallerinaAsyncApiException;
import io.ballerina.asyncapi.codegenerator.configuration.Constants;
import io.ballerina.asyncapi.codegenerator.usecase.utils.CodegenUtils;
import io.ballerina.asyncapi.codegenerator.usecase.utils.DocCommentsUtils;
import io.ballerina.compiler.syntax.tree.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.*;
import static io.ballerina.compiler.syntax.tree.NodeFactory.*;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SEMICOLON_TOKEN;

public class GenerateRecordNode implements UseCase {
    private final AaiDocument asyncApiSpec;
    private final Map.Entry<String, AaiSchema> recordFields;

    private final CodegenUtils codegenUtils = new CodegenUtils();
    private final DocCommentsUtils commentsUtils = new DocCommentsUtils();

    public GenerateRecordNode(AaiDocument asyncApiSpec, Map.Entry<String, AaiSchema> recordFields) {
        this.asyncApiSpec = asyncApiSpec;
        this.recordFields = recordFields;
    }

    @Override
    public TypeDefinitionNode execute() throws BallerinaAsyncApiException {
        var typeName = AbstractNodeFactory
                .createIdentifierToken(codegenUtils.escapeIdentifier(recordFields.getKey().trim()));
        Token typeKeyWord = AbstractNodeFactory.createIdentifierToken("public type");
        TypeDefinitionNode typeDefinitionNode;
        List<Node> schemaDoc = new ArrayList<>();
        List<Node> recordFieldList = new ArrayList<>();
        for (Map.Entry<String, AaiSchema> field : recordFields.getValue().properties.entrySet()) {
            addRecordField(recordFields.getValue().required, recordFieldList, field, asyncApiSpec);
        }
        NodeList<Node> fieldNodes = createNodeList(recordFieldList);
        var recordTypeDescriptorNode =
                NodeFactory.createRecordTypeDescriptorNode(createToken(SyntaxKind.RECORD_KEYWORD),
                        createToken(OPEN_BRACE_TOKEN), fieldNodes, null,
                        createToken(SyntaxKind.CLOSE_BRACE_TOKEN));
        var documentationNode =
                createMarkdownDocumentationNode(createNodeList(schemaDoc));
        var metadataNode = createMetadataNode(documentationNode, createEmptyNodeList());
        typeDefinitionNode = NodeFactory.createTypeDefinitionNode(metadataNode,
                null, typeKeyWord, typeName, recordTypeDescriptorNode, createToken(SEMICOLON_TOKEN));
        return typeDefinitionNode;
    }

    /**
     * This method generates a record field with given schema properties.
     */
    private void addRecordField(List<String> required, List<Node> recordFieldList, Map.Entry<String, AaiSchema> field,
                                AaiDocument asyncApiSpec) throws BallerinaAsyncApiException {
        // TODO: Handle allOf , oneOf, anyOf
        RecordFieldNode recordFieldNode;
        // API doc
        List<Node> schemaDoc = new ArrayList<>();
        String fieldName = codegenUtils.escapeIdentifier(field.getKey().trim());
        if (field.getValue().description != null) {
            schemaDoc.addAll(commentsUtils.createAPIDescriptionDoc(
                    field.getValue().description, false));
        } else if (field.getValue().$ref != null) {
            String[] split = field.getValue().$ref.trim().split("/");
            String componentName = codegenUtils.getValidName(split[split.length - 1], true);
            if (asyncApiSpec.components.schemas.get(componentName) != null) {
                AaiSchema schema = asyncApiSpec.components.schemas.get(componentName);
                if (schema.description != null) {
                    schemaDoc.addAll(commentsUtils.createAPIDescriptionDoc(
                            schema.description, false));
                }
            }
        }

        var fieldNameToken = AbstractNodeFactory.createIdentifierToken(fieldName);
        var fieldTypeName = getTypeDescriptorNode(field.getValue(), asyncApiSpec);
        Token semicolonToken = AbstractNodeFactory.createIdentifierToken(";");
        Token questionMarkToken = AbstractNodeFactory.createIdentifierToken("?");
        var documentationNode = createMarkdownDocumentationNode(createNodeList(schemaDoc));
        var metadataNode = createMetadataNode(documentationNode, createEmptyNodeList());
        if (required != null) {
            if (!required.contains(field.getKey().trim())) {
                recordFieldNode = NodeFactory.createRecordFieldNode(metadataNode, null,
                        fieldTypeName, fieldNameToken, questionMarkToken, semicolonToken);
            } else {
                recordFieldNode = NodeFactory.createRecordFieldNode(metadataNode, null,
                        fieldTypeName, fieldNameToken, null, semicolonToken);
            }
        } else {
            recordFieldNode = NodeFactory.createRecordFieldNode(metadataNode, null,
                    fieldTypeName, fieldNameToken, questionMarkToken, semicolonToken);
        }
        recordFieldList.add(recordFieldNode);
    }

    private TypeDescriptorNode getTypeDescriptorNode(AaiSchema schema, AaiDocument asyncApiSpec) throws BallerinaAsyncApiException {
        if (schema.type != null || schema.properties != null) {
            if (schema.type != null && ((schema.type.equals("integer") || schema.type.equals("number"))
                    || schema.type.equals("string") || schema.type.equals("boolean"))) {
                String type = convertAsyncAPITypeToBallerina(schema.type.trim());
                if (schema.type.equals("number") && schema.format != null) {
                    type = convertAsyncAPITypeToBallerina(schema.format.trim());
                }
                type = getNullableType(schema, type);
                Token typeName = AbstractNodeFactory.createIdentifierToken(type);
                return createBuiltinSimpleNameReferenceNode(null, typeName);
            } else if (schema.type != null && schema.type.equals("array")) {
                if (schema.items != null) {
                    return getTypeDescriptorNodeForArraySchema(asyncApiSpec, schema);
                } else {
                    throw new BallerinaAsyncApiException("Array does not contain the the items attribute");
                }
            } else if ((schema.type != null && schema.type.equals("object")) ||
                    (schema.properties != null)) {
                if (schema.properties != null) {
                    Map<String, AaiSchema> properties = schema.properties;
                    Token recordKeyWord = AbstractNodeFactory.createIdentifierToken("record ");
                    Token bodyStartDelimiter = AbstractNodeFactory.createIdentifierToken("{ ");
                    Token bodyEndDelimiter = AbstractNodeFactory.createIdentifierToken("} ");
                    List<Node> recordFList = new ArrayList<>();
                    List<String> required = schema.required;
                    for (Map.Entry<String, AaiSchema> property : properties.entrySet()) {
                        addRecordField(required, recordFList, property, asyncApiSpec);
                    }
                    NodeList<Node> fieldNodes = AbstractNodeFactory.createNodeList(recordFList);

                    return NodeFactory.createRecordTypeDescriptorNode(recordKeyWord, bodyStartDelimiter, fieldNodes,
                            null, bodyEndDelimiter);
                } else if (schema.$ref != null) {
                    String type = codegenUtils.getValidName(
                            codegenUtils.extractReferenceType(schema.$ref), true);
                    AaiSchema refSchema = asyncApiSpec.components.schemas.get(type);
                    type = getNullableType(refSchema, type);
                    Token typeName = AbstractNodeFactory.createIdentifierToken(type);
                    return createBuiltinSimpleNameReferenceNode(null, typeName);
                } else {
                    Token typeName = AbstractNodeFactory.createIdentifierToken(
                            convertAsyncAPITypeToBallerina(schema.type.trim()));
                    return createBuiltinSimpleNameReferenceNode(null, typeName);
                }
            } else {
                throw new BallerinaAsyncApiException("Unsupported Async Api Spec data type `" + schema.type + "`.");
            }
        } else if (schema.$ref != null) {
            String type = codegenUtils.extractReferenceType(schema.$ref);
            type = codegenUtils.getValidName(type, true);
            AaiSchema refSchema = asyncApiSpec.components.schemas.get(type);
            type = getNullableType(refSchema, type);
            Token typeName = AbstractNodeFactory.createIdentifierToken(type);
            return createBuiltinSimpleNameReferenceNode(null, typeName);
        } else {
            // This contains a fallback to Ballerina common type `anydata` if the Async Api specification type is not
            // defined.
            String type = "anydata";
            type = getNullableType(schema, type);
            Token typeName = AbstractNodeFactory.createIdentifierToken(type);
            return createBuiltinSimpleNameReferenceNode(null, typeName);
        }
    }

    /**
     * Method for convert async api type to ballerina type.
     *
     * @param type AsyncApi parameter types
     * @return ballerina type
     */
    public static String convertAsyncAPITypeToBallerina(String type) throws BallerinaAsyncApiException {
        String convertedType;
        switch (type) {
            case Constants.INTEGER:
                convertedType = "int";
                break;
            case Constants.STRING:
                convertedType = "string";
                break;
            case Constants.BOOLEAN:
                convertedType = "boolean";
                break;
            case Constants.ARRAY:
                convertedType = "[]";
                break;
            case Constants.OBJECT:
                convertedType = "record {}";
                break;
            case Constants.DECIMAL:
            case Constants.NUMBER:
                convertedType = "decimal";
                break;
            case Constants.DOUBLE:
            case Constants.FLOAT:
                convertedType = "float";
                break;
            default:
                throw new BallerinaAsyncApiException("Unsupported Async Api Spec data type `" + type + "`");
        }
        return convertedType;
    }


    private String getNullableType(AaiSchema schema, String type) {
        // TODO: find a way to know the nullable attributes from the Async API spec ( Not the optional ones )
        //  Ex: string? testString;
        return type;
    }


    public TypeDescriptorNode getTypeDescriptorNodeForArraySchema(AaiDocument asyncApi, AaiSchema schema)
            throws BallerinaAsyncApiException {
        String type;
        Token typeName;
        TypeDescriptorNode memberTypeDesc;
        Token openSBracketToken = AbstractNodeFactory.createIdentifierToken("[");
        // TODO: handle this when schema.items is a List<AaiSchema> in the below line
        var schemaItem = (AaiSchema) schema.items;
        Token closeSBracketToken = AbstractNodeFactory.createIdentifierToken(getNullableType(schema, "]"));
        if (schemaItem.$ref != null) {
            type = codegenUtils.getValidName(codegenUtils.extractReferenceType(
                    schemaItem.$ref), true);
            typeName = AbstractNodeFactory.createIdentifierToken(type);
            memberTypeDesc = createBuiltinSimpleNameReferenceNode(null, typeName);
            return NodeFactory.createArrayTypeDescriptorNode(memberTypeDesc, openSBracketToken,
                    null, closeSBracketToken);
        } else if (schemaItem.type != null && (schemaItem.type.equals("array") || schemaItem.type.equals("object"))) {
            memberTypeDesc = getTypeDescriptorNode(schemaItem, asyncApi);
            return NodeFactory.createArrayTypeDescriptorNode(memberTypeDesc, openSBracketToken,
                    null, closeSBracketToken);
        } else if (schemaItem.type != null) {
            type = schemaItem.type;
            closeSBracketToken = AbstractNodeFactory.createIdentifierToken(getNullableType(schema, "]"));
            typeName = AbstractNodeFactory.createIdentifierToken(convertAsyncAPITypeToBallerina(type));
            memberTypeDesc = createBuiltinSimpleNameReferenceNode(null, typeName);
            return NodeFactory.createArrayTypeDescriptorNode(memberTypeDesc, openSBracketToken,
                    null, closeSBracketToken);
        } else {
            type = "anydata";
            closeSBracketToken = AbstractNodeFactory.createIdentifierToken(getNullableType(schema, "]"));
            typeName = AbstractNodeFactory.createIdentifierToken(type);
            memberTypeDesc = createBuiltinSimpleNameReferenceNode(null, typeName);
            return NodeFactory.createArrayTypeDescriptorNode(memberTypeDesc, openSBracketToken,
                    null, closeSBracketToken);
        }
    }

}
