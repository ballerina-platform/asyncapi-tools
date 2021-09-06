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
    public TypeDefinitionNode execute() {
        IdentifierToken typeName = AbstractNodeFactory
                .createIdentifierToken(codegenUtils.escapeIdentifier(recordFields.getKey().trim()));
        Token typeKeyWord = AbstractNodeFactory.createIdentifierToken("public type");
        TypeDefinitionNode typeDefinitionNode;
        List<Node> schemaDoc = new ArrayList<>();
        List<Node> recordFieldList = new ArrayList<>();
        for (Map.Entry<String, AaiSchema> field : recordFields.getValue().properties.entrySet()) {
            addRecordField(field.getValue().required, recordFieldList, field, asyncApiSpec);
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
                                AaiDocument asyncApiSpec) {
        // TODO: Handle allOf , oneOf, anyOf
        RecordFieldNode recordFieldNode;
        // API doc
        List<Node> schemaDoc = new ArrayList<>();
        String fieldN = codegenUtils.escapeIdentifier(field.getKey().trim());
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

        //FiledName
        var fieldName = AbstractNodeFactory.createIdentifierToken(fieldN);
        // TODO: handle other types of identifiers too
        Token typeName = AbstractNodeFactory.createIdentifierToken("string");
        TypeDescriptorNode fieldTypeName = createBuiltinSimpleNameReferenceNode(null, typeName);
        Token semicolonToken = AbstractNodeFactory.createIdentifierToken(";");
        Token questionMarkToken = AbstractNodeFactory.createIdentifierToken("?");
        var documentationNode = createMarkdownDocumentationNode(createNodeList(schemaDoc));
        var metadataNode = createMetadataNode(documentationNode, createEmptyNodeList());
        if (required != null) {
            if (!required.contains(field.getKey().trim())) {
                recordFieldNode = NodeFactory.createRecordFieldNode(metadataNode, null,
                        fieldTypeName, fieldName, questionMarkToken, semicolonToken);
            } else {
                recordFieldNode = NodeFactory.createRecordFieldNode(metadataNode, null,
                        fieldTypeName, fieldName, null, semicolonToken);
            }
        } else {
            recordFieldNode = NodeFactory.createRecordFieldNode(metadataNode, null,
                    fieldTypeName, fieldName, questionMarkToken, semicolonToken);
        }
        recordFieldList.add(recordFieldNode);
    }
}
