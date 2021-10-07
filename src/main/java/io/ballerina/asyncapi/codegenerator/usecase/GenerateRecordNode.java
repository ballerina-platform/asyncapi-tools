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

import io.ballerina.asyncapi.codegenerator.configuration.BallerinaAsyncApiException;
import io.ballerina.asyncapi.codegenerator.configuration.Constants;
import io.ballerina.asyncapi.codegenerator.entity.Schema;
import io.ballerina.asyncapi.codegenerator.usecase.utils.CodegenUtils;
import io.ballerina.asyncapi.codegenerator.usecase.utils.DocCommentsUtils;
import io.ballerina.compiler.syntax.tree.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.*;
import static io.ballerina.compiler.syntax.tree.NodeFactory.*;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.*;

public class GenerateRecordNode implements GenerateUseCase {
    private final Map<String, Schema> schemas;
    private final Map.Entry<String, Schema> recordFields;

    private final CodegenUtils codegenUtils = new CodegenUtils();
    private final DocCommentsUtils commentsUtils = new DocCommentsUtils();

    public GenerateRecordNode(Map<String, Schema> schemas, Map.Entry<String, Schema> recordFields) {
        this.schemas = schemas;
        this.recordFields = recordFields;
    }

    @Override
    public TypeDefinitionNode generate() throws BallerinaAsyncApiException {
        IdentifierToken typeName = AbstractNodeFactory
                .createIdentifierToken(codegenUtils.escapeIdentifier(recordFields.getKey().trim()));
        TypeDefinitionNode typeDefinitionNode;
        List<Node> schemaDoc = new ArrayList<>();
        List<Node> recordFieldList = new ArrayList<>();
        for (Map.Entry<String, Schema> field : recordFields.getValue().getSchemaProperties().entrySet()) {
            addRecordField(recordFields.getValue().getRequired(), recordFieldList, field, schemas);
        }
        NodeList<Node> fieldNodes = createNodeList(recordFieldList);
        RecordTypeDescriptorNode recordTypeDescriptorNode =
                createRecordTypeDescriptorNode(createToken(SyntaxKind.RECORD_KEYWORD),
                        createToken(OPEN_BRACE_TOKEN), fieldNodes, null,
                        createToken(SyntaxKind.CLOSE_BRACE_TOKEN));
        MarkdownDocumentationNode documentationNode =
                createMarkdownDocumentationNode(createNodeList(schemaDoc));
        MetadataNode metadataNode = createMetadataNode(documentationNode, createEmptyNodeList());
        typeDefinitionNode = createTypeDefinitionNode(metadataNode,
                createToken(PUBLIC_KEYWORD), createToken(TYPE_KEYWORD), typeName, recordTypeDescriptorNode, createToken(SEMICOLON_TOKEN));
        return typeDefinitionNode;
    }

    /**
     * This method generates a record field with given schema properties.
     */
    private void addRecordField(List<String> required, List<Node> recordFieldList, Map.Entry<String, Schema> field,
                                Map<String, Schema> schemas) throws BallerinaAsyncApiException {
        // TODO: Handle allOf , oneOf, anyOf
        RecordFieldNode recordFieldNode;
        List<Node> schemaDoc = new ArrayList<>();
        String fieldName = codegenUtils.escapeIdentifier(field.getKey().trim());
        if (field.getValue().getTitle() != null) {
            schemaDoc.addAll(commentsUtils.createDescriptionComments(
                    field.getValue().getTitle(), false));
        } else if (field.getValue().get$ref() != null) {
            String[] split = field.getValue().get$ref().trim().split("/");
            if (schemas.get(split[split.length - 1]) != null) {
                Schema schema = schemas.get(split[split.length - 1]);
                if (schema.getTitle() != null) {
                    schemaDoc.addAll(commentsUtils.createDescriptionComments(
                            schema.getTitle(), false));
                }
            }
        }

        IdentifierToken fieldNameToken = AbstractNodeFactory.createIdentifierToken(fieldName);
        TypeDescriptorNode fieldTypeName = getTypeDescriptorNode(field.getValue(), schemas);
        Token semicolonToken = AbstractNodeFactory.createIdentifierToken(";");
        Token questionMarkToken = AbstractNodeFactory.createIdentifierToken("?");
        MarkdownDocumentationNode documentationNode = createMarkdownDocumentationNode(createNodeList(schemaDoc));
        MetadataNode metadataNode = createMetadataNode(documentationNode, createEmptyNodeList());
        if (required != null) {
            if (!required.contains(field.getKey().trim())) {
                recordFieldNode = createRecordFieldNode(metadataNode, null,
                        fieldTypeName, fieldNameToken, questionMarkToken, semicolonToken);
            } else {
                recordFieldNode = createRecordFieldNode(metadataNode, null,
                        fieldTypeName, fieldNameToken, null, semicolonToken);
            }
        } else {
            recordFieldNode = createRecordFieldNode(metadataNode, null,
                    fieldTypeName, fieldNameToken, questionMarkToken, semicolonToken);
        }
        recordFieldList.add(recordFieldNode);
    }

    private TypeDescriptorNode getTypeDescriptorNode(Schema schema, Map<String, Schema> schemas)
            throws BallerinaAsyncApiException {
        if (schema.getType() != null || schema.getSchemaProperties() != null) {
            if (schema.getType() != null && ((schema.getType().equals("integer") || schema.getType().equals("number"))
                    || schema.getType().equals("string") || schema.getType().equals("boolean"))) {
                String type = convertAsyncAPITypeToBallerina(schema.getType().trim());
                if (schema.getType().equals("number") && schema.getFormat() != null) {
                    type = convertAsyncAPITypeToBallerina(schema.getFormat().trim());
                }
                type = getNullableType(schema, type);
                Token typeName = AbstractNodeFactory.createIdentifierToken(type);
                return createBuiltinSimpleNameReferenceNode(null, typeName);
            } else if (schema.getType() != null && schema.getType().equals("array")) {
                if (schema.getItems() != null) {
                    return getTypeDescriptorNodeForArraySchema(schemas, schema);
                } else {
                    throw new BallerinaAsyncApiException("Array does not contain the the items attribute");
                }
            } else if ((schema.getType() != null && schema.getType().equals("object")) ||
                    (schema.getSchemaProperties() != null)) {
                if (schema.getSchemaProperties() != null) {
                    Map<String, Schema> properties = schema.getSchemaProperties();
                    Token recordKeyWord = AbstractNodeFactory.createIdentifierToken("record ");
                    Token bodyStartDelimiter = AbstractNodeFactory.createIdentifierToken("{ ");
                    Token bodyEndDelimiter = AbstractNodeFactory.createIdentifierToken("} ");
                    List<Node> recordFList = new ArrayList<>();
                    List<String> required = schema.getRequired();
                    for (Map.Entry<String, Schema> property : properties.entrySet()) {
                        addRecordField(required, recordFList, property, schemas);
                    }
                    NodeList<Node> fieldNodes = AbstractNodeFactory.createNodeList(recordFList);

                    return createRecordTypeDescriptorNode(recordKeyWord, bodyStartDelimiter, fieldNodes,
                            null, bodyEndDelimiter);
                } else if (schema.get$ref() != null) {
                    String type = codegenUtils.getValidName(
                            codegenUtils.extractReferenceType(schema.get$ref()), true);
                    Schema refSchema = schemas.get(type);
                    type = getNullableType(refSchema, type);
                    Token typeName = AbstractNodeFactory.createIdentifierToken(type);
                    return createBuiltinSimpleNameReferenceNode(null, typeName);
                } else {
                    Token typeName = AbstractNodeFactory.createIdentifierToken(
                            convertAsyncAPITypeToBallerina(schema.getType().trim()));
                    return createBuiltinSimpleNameReferenceNode(null, typeName);
                }
            } else {
                throw new BallerinaAsyncApiException(
                        "Unsupported Async Api Spec data type `" + schema.getType() + "`.");
            }
        } else if (schema.get$ref() != null) {
            String type = codegenUtils.extractReferenceType(schema.get$ref());
            type = codegenUtils.getValidName(type, true);
            Schema refSchema = schemas.get(type);
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


    private String getNullableType(Schema schema, String type) {
        // TODO: find a way to know the nullable attributes from the Async API spec ( Not the optional ones )
        //  Ex: string? testString;
        return type;
    }


    public TypeDescriptorNode getTypeDescriptorNodeForArraySchema
            (Map<String, Schema> schemas, Schema schema) throws BallerinaAsyncApiException {
        String type;
        Token typeName;
        TypeDescriptorNode memberTypeDesc;
        Token openSBracketToken = AbstractNodeFactory.createIdentifierToken("[");
        // TODO: handle this when schema.items is a List<Schema> in the below line
        Schema schemaItem = (Schema) schema.getItems();
        Token closeSBracketToken = AbstractNodeFactory.createIdentifierToken(getNullableType(schema, "]"));
        if (schemaItem.get$ref() != null) {
            type = codegenUtils.getValidName(codegenUtils.extractReferenceType(
                    schemaItem.get$ref()), true);
            typeName = AbstractNodeFactory.createIdentifierToken(type);
            memberTypeDesc = createBuiltinSimpleNameReferenceNode(null, typeName);
            return createArrayTypeDescriptorNode(memberTypeDesc, openSBracketToken,
                    null, closeSBracketToken);
        } else if (schemaItem.getType() != null
                && (schemaItem.getType().equals("array") || schemaItem.getType().equals("object"))) {
            memberTypeDesc = getTypeDescriptorNode(schemaItem, schemas);
            return createArrayTypeDescriptorNode(memberTypeDesc, openSBracketToken,
                    null, closeSBracketToken);
        } else if (schemaItem.getType() != null) {
            type = schemaItem.getType();
            closeSBracketToken = AbstractNodeFactory.createIdentifierToken(getNullableType(schema, "]"));
            typeName = AbstractNodeFactory.createIdentifierToken(convertAsyncAPITypeToBallerina(type));
            memberTypeDesc = createBuiltinSimpleNameReferenceNode(null, typeName);
            return createArrayTypeDescriptorNode(memberTypeDesc, openSBracketToken,
                    null, closeSBracketToken);
        } else {
            type = "anydata";
            closeSBracketToken = AbstractNodeFactory.createIdentifierToken(getNullableType(schema, "]"));
            typeName = AbstractNodeFactory.createIdentifierToken(type);
            memberTypeDesc = createBuiltinSimpleNameReferenceNode(null, typeName);
            return createArrayTypeDescriptorNode(memberTypeDesc, openSBracketToken,
                    null, closeSBracketToken);
        }
    }

}
