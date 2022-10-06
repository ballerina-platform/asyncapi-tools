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

import com.fasterxml.jackson.databind.node.TextNode;
import io.apicurio.datamodels.core.models.Extension;
import io.ballerina.asyncapi.codegenerator.configuration.BallerinaAsyncApiException;
import io.ballerina.asyncapi.codegenerator.configuration.Constants;
import io.ballerina.asyncapi.codegenerator.entity.Schema;
import io.ballerina.asyncapi.codegenerator.usecase.utils.CodegenUtils;
import io.ballerina.asyncapi.codegenerator.usecase.utils.DocCommentsUtils;
import io.ballerina.compiler.syntax.tree.AbstractNodeFactory;
import io.ballerina.compiler.syntax.tree.ArrayDimensionNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.MarkdownDocumentationNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.RecordFieldNode;
import io.ballerina.compiler.syntax.tree.RecordTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createSeparatedNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createArrayTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBuiltinSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createEnumDeclarationNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createEnumMemberNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMarkdownDocumentationNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMetadataNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createOptionalTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRecordFieldNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRecordTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTypeDefinitionNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ENUM_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PUBLIC_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.QUESTION_MARK_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SEMICOLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.TYPE_KEYWORD;

/**
 * Generate the record node for data_types.bal.
 */
public class GenerateModuleMemberDeclarationNode implements Generator {
    private final Map.Entry<String, Schema> recordFields;

    private final CodegenUtils codegenUtils = new CodegenUtils();
    private final DocCommentsUtils commentsUtils = new DocCommentsUtils();

    public GenerateModuleMemberDeclarationNode(Map.Entry<String, Schema> recordFields) {
        this.recordFields = recordFields;
    }

    @Override
    public ModuleMemberDeclarationNode generate() throws BallerinaAsyncApiException {
        IdentifierToken typeName = AbstractNodeFactory
                .createIdentifierToken(codegenUtils.getValidName(
                        codegenUtils.escapeIdentifier(recordFields.getKey().trim()), true));
        TypeDefinitionNode typeDefinitionNode;
        List<Node> schemaDoc = new ArrayList<>();
        MarkdownDocumentationNode documentationNode =
                createMarkdownDocumentationNode(createNodeList(schemaDoc));
        MetadataNode metadataNode = createMetadataNode(documentationNode, createEmptyNodeList());
        List<Node> recordFieldList = new ArrayList<>();
        List<String> requiredList = (recordFields.getValue().getRequired() != null)
                ? recordFields.getValue().getRequired() : new ArrayList<>();
        if (recordFields.getValue().getEnum() != null) {
            // Handle enums
            List<Node> enums = new ArrayList<>();
            for (int i = 0; i < recordFields.getValue().getEnum().size(); i++) {
                if (i > 0) {
                    enums.add(createToken(SyntaxKind.COMMA_TOKEN));
                }
                String enumName = ((TextNode) recordFields.getValue().getEnum().get(i)).textValue();
                enums.add(createEnumMemberNode(
                        null, createIdentifierToken(enumName), null, null));
            }
            return createEnumDeclarationNode(metadataNode, createToken(PUBLIC_KEYWORD), createToken(ENUM_KEYWORD),
                    typeName, createToken(OPEN_BRACE_TOKEN),
                    createSeparatedNodeList(enums), createToken(CLOSE_BRACE_TOKEN));
        } else if (recordFields.getValue().getSchemaProperties() == null && recordFields.getValue().getType() != null) {
            // Handle when the schema is defined directly under the name
            // (i.e. there is no properties attribute under the schema)
            TypeDescriptorNode fieldTypeName = getTypeDescriptorNode(recordFields.getValue());
            typeDefinitionNode = createTypeDefinitionNode(metadataNode,
                    createToken(PUBLIC_KEYWORD), createToken(TYPE_KEYWORD),
                    typeName, createOptionalTypeDescriptorNode(
                            fieldTypeName,
                            createToken(QUESTION_MARK_TOKEN)), createToken(SEMICOLON_TOKEN));
            return typeDefinitionNode;
        } else if (recordFields.getValue().getSchemaProperties() != null) {
            // Handle when the properties attribute is there inside the schema
            for (Map.Entry<String, Schema> field : recordFields.getValue().getSchemaProperties().entrySet()) {
                addRecordField(requiredList, recordFieldList, field);
            }
        } else if (recordFields.getValue().hasExtraProperties()) {
            // TODO: handle when the fields are directly under the Schema.
        }
        NodeList<Node> fieldNodes = createNodeList(recordFieldList);
        RecordTypeDescriptorNode recordTypeDescriptorNode =
                createRecordTypeDescriptorNode(createToken(SyntaxKind.RECORD_KEYWORD),
                        createToken(OPEN_BRACE_TOKEN), fieldNodes, null,
                        createToken(SyntaxKind.CLOSE_BRACE_TOKEN));
        typeDefinitionNode = createTypeDefinitionNode(metadataNode,
                createToken(PUBLIC_KEYWORD), createToken(TYPE_KEYWORD),
                typeName, recordTypeDescriptorNode, createToken(SEMICOLON_TOKEN));
        return typeDefinitionNode;
    }

    /**
     * This method generates a record field with given schema properties.
     */
    private void addRecordField(List<String> required, List<Node> recordFieldList, Map.Entry<String, Schema> field)
            throws BallerinaAsyncApiException {
        // TODO: Handle allOf , oneOf, anyOf
        RecordFieldNode recordFieldNode;
        List<Node> schemaDoc = new ArrayList<>();
        String fieldName = codegenUtils.escapeIdentifier(field.getKey().trim());
        if (field.getValue().getTitle() != null) {
            schemaDoc.addAll(commentsUtils.createDescriptionComments(
                    field.getValue().getTitle(), false));
        } else if (field.getValue().getDescription() != null) {
            schemaDoc.addAll(commentsUtils.createDescriptionComments(
                    field.getValue().getDescription(), false));
        }

        IdentifierToken fieldNameToken = AbstractNodeFactory.createIdentifierToken(fieldName);
        TypeDescriptorNode fieldTypeName = getTypeDescriptorNode(field.getValue());
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

    private TypeDescriptorNode getTypeDescriptorNode(Schema schema)
            throws BallerinaAsyncApiException {
        if (schema.getType() != null || schema.getSchemaProperties() != null) {
            TypeDescriptorNode originalTypeDesc = getTypeDescriptorNodeForObjects(schema);
            return addNullableType(schema, originalTypeDesc);
        } else if (schema.getRef() != null) {
            String type = codegenUtils.extractReferenceType(schema.getRef());
            type = codegenUtils.getValidName(type, true);
            Token typeName = AbstractNodeFactory.createIdentifierToken(type);
            TypeDescriptorNode originalTypeDesc = createBuiltinSimpleNameReferenceNode(null, typeName);
            return addNullableType(schema, originalTypeDesc);
        } else {
            // This contains a fallback to Ballerina common type `anydata` if the Async Api specification type is not
            // defined.
            String type = "anydata";
            Token typeName = AbstractNodeFactory.createIdentifierToken(type);
            return createBuiltinSimpleNameReferenceNode(null, typeName);
        }
    }

    private TypeDescriptorNode getTypeDescriptorNodeForObjects(Schema schema) throws BallerinaAsyncApiException {
        if (schema.getSchemaProperties() != null) {
            return getRecordTypeDescriptorNode(schema);
        } else if (schema.getType() != null) {
            return getTypeDescriptorNodeFroPreDefined(schema);
        } else {
            throw new BallerinaAsyncApiException(
                    "Unsupported Async Api Spec data type `" + schema.getType() + "`");
        }
    }

    private TypeDescriptorNode getTypeDescriptorNodeFroPreDefined(Schema schema) throws BallerinaAsyncApiException {
        switch (schema.getType()) {
            case Constants.INTEGER:
            case Constants.STRING:
            case Constants.BOOLEAN:
                String type = convertAsyncAPITypeToBallerina(schema.getType().trim());
                Token typeName = AbstractNodeFactory.createIdentifierToken(type);
                return createBuiltinSimpleNameReferenceNode(null, typeName);
            case Constants.NUMBER:
                type = convertAsyncAPITypeToBallerina(schema.getType().trim());
                if (schema.getType().equals("number") && schema.getFormat() != null) {
                    type = convertAsyncAPITypeToBallerina(schema.getFormat().trim());
                }
                typeName = AbstractNodeFactory.createIdentifierToken(type);
                return createBuiltinSimpleNameReferenceNode(null, typeName);
            case Constants.ARRAY:
                return getTypeDescriptorNodeForArraySchema(schema);
            case Constants.OBJECT:
                if (schema.getRef() != null) {
                    type = codegenUtils.getValidName(
                            codegenUtils.extractReferenceType(schema.getRef()), true);
                    typeName = AbstractNodeFactory.createIdentifierToken(type);
                } else {
                    typeName = AbstractNodeFactory.createIdentifierToken(
                            convertAsyncAPITypeToBallerina(schema.getType().trim()));
                }
                return createBuiltinSimpleNameReferenceNode(null, typeName);
            default:
                throw new BallerinaAsyncApiException(
                        "Unsupported Async Api Spec data type `" + schema.getType() + "`");
        }
    }

    private RecordTypeDescriptorNode getRecordTypeDescriptorNode(Schema schema) throws BallerinaAsyncApiException {
        Map<String, Schema> properties = schema.getSchemaProperties();
        Token recordKeyWord = AbstractNodeFactory.createIdentifierToken("record ");
        Token bodyStartDelimiter = AbstractNodeFactory.createIdentifierToken("{ ");
        Token bodyEndDelimiter = AbstractNodeFactory.createIdentifierToken("} ");
        List<Node> recordFList = new ArrayList<>();
        List<String> required = schema.getRequired();
        for (Map.Entry<String, Schema> property : properties.entrySet()) {
            addRecordField(required, recordFList, property);
        }
        NodeList<Node> fieldNodes = AbstractNodeFactory.createNodeList(recordFList);

        return createRecordTypeDescriptorNode(recordKeyWord, bodyStartDelimiter, fieldNodes,
                null, bodyEndDelimiter);
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


    public TypeDescriptorNode getTypeDescriptorNodeForArraySchema(Schema schema) throws BallerinaAsyncApiException {
        if (schema.getItems() != null) {
            String type;
            Token typeName;
            TypeDescriptorNode memberTypeDesc;
            Token openSBracketToken = AbstractNodeFactory.createIdentifierToken("[");
            // TODO: handle this when schema.items is a List<Schema> in the below line
            Schema schemaItem = (Schema) schema.getItems();
            Token closeSBracketToken = AbstractNodeFactory.createIdentifierToken("]");
            ArrayDimensionNode arrayDimensionNode = NodeFactory.createArrayDimensionNode(openSBracketToken,
                    null, closeSBracketToken);
            if (schemaItem.getRef() != null) {
                type = codegenUtils.getValidName(codegenUtils.extractReferenceType(
                        schemaItem.getRef()), true);
                typeName = AbstractNodeFactory.createIdentifierToken(type);
                memberTypeDesc = createBuiltinSimpleNameReferenceNode(null, typeName);
                // TODO: memberTypeDesc != ArrayTypeDescriptorNode
                return createArrayTypeDescriptorNode(memberTypeDesc, createNodeList(arrayDimensionNode));
            } else if (schemaItem.getType() != null
                    && (schemaItem.getType().equals("array") || schemaItem.getType().equals("object"))) {
                memberTypeDesc = getTypeDescriptorNode(schemaItem);
                return createArrayTypeDescriptorNode(memberTypeDesc, createNodeList(arrayDimensionNode));
            } else if (schemaItem.getType() != null) {
                type = schemaItem.getType();
                typeName = AbstractNodeFactory.createIdentifierToken(convertAsyncAPITypeToBallerina(type));
                memberTypeDesc = createBuiltinSimpleNameReferenceNode(null, typeName);
                return createArrayTypeDescriptorNode(memberTypeDesc, createNodeList(arrayDimensionNode));
            } else {
                type = "anydata";
                typeName = AbstractNodeFactory.createIdentifierToken(type);
                memberTypeDesc = createBuiltinSimpleNameReferenceNode(null, typeName);
                return createArrayTypeDescriptorNode(memberTypeDesc, createNodeList(arrayDimensionNode));
            }
        } else {
            throw new BallerinaAsyncApiException("Array does not contain the 'items' attribute");
        }
    }

    private TypeDescriptorNode addNullableType(Schema schema, TypeDescriptorNode originalTypeDesc) {
        if (schema.getExtension("x-nullable") != null) {
            Extension identifier = (Extension) schema.getExtension("x-nullable");
            if (identifier.value.equals(true)) {
                return createOptionalTypeDescriptorNode(originalTypeDesc, createToken(QUESTION_MARK_TOKEN));
            }
        }
        return originalTypeDesc;
    }
}
