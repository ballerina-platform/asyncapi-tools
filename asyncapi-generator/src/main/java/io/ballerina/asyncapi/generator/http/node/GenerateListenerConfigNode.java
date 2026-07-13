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
package io.ballerina.asyncapi.generator.http.node;

import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.compiler.syntax.tree.MarkdownDocumentationNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.RecordTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;

import java.util.ArrayList;
import java.util.List;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyMinutiaeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createLiteralValueToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBasicLiteralNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBuiltinSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMarkdownDocumentationNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMetadataNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createQualifiedNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRecordFieldWithDefaultValueNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRecordTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTypeDefinitionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTypeReferenceNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ASTERISK_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACE_PIPE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.COLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.EQUAL_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_PIPE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PUBLIC_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RECORD_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SEMICOLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.STRING_LITERAL;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.STRING_LITERAL_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.TYPE_KEYWORD;

/**
 * Generates the {@code public type ListenerConfiguration record {| *http:ListenerConfiguration;
 * string webhookSecret = ""; |};} type definition node for {@code data_types.bal}.
 */
public class GenerateListenerConfigNode {

    public static final String LISTENER_CONFIG_TYPE = "ListenerConfiguration";
    public static final String WEBHOOK_SECRET_FIELD = "webhookSecret";

    /**
     * Generates the {@code ListenerConfiguration} closed-record type definition, with the webhook
     * secret field plus one additional {@code string} field (default {@code ""}) per name in
     * {@code extraConfigFields} (populated from {@code $config('name')} references in the DSL).
     *
     * @param extraConfigFields additional configurable field names beyond {@code webhookSecret}
     * @return the generated {@link TypeDefinitionNode}
     * @throws GeneratorException never thrown; declared for consistency with other node generators
     */
    public static TypeDefinitionNode generate(List<String> extraConfigFields) throws GeneratorException {
        QualifiedNameReferenceNode includedType = createQualifiedNameReferenceNode(
                createIdentifierToken(GenerateHttpImportNode.HTTP_MODULE),
                createToken(COLON_TOKEN),
                createIdentifierToken(LISTENER_CONFIG_TYPE));

        List<Node> recordFields = new ArrayList<>();
        recordFields.add(createTypeReferenceNode(
                createToken(ASTERISK_TOKEN), includedType, createToken(SEMICOLON_TOKEN)));

        List<String> fieldNames = new ArrayList<>();
        fieldNames.add(WEBHOOK_SECRET_FIELD);
        fieldNames.addAll(extraConfigFields);
        for (String fieldName : fieldNames) {
            recordFields.add(createRecordFieldWithDefaultValueNode(
                    null,
                    null,
                    createBuiltinSimpleNameReferenceNode(null, createIdentifierToken("string")),
                    createIdentifierToken(fieldName),
                    createToken(EQUAL_TOKEN),
                    createBasicLiteralNode(STRING_LITERAL,
                            createLiteralValueToken(STRING_LITERAL_TOKEN, "\"\"",
                                    createEmptyMinutiaeList(), createEmptyMinutiaeList())),
                    createToken(SEMICOLON_TOKEN)));
        }

        RecordTypeDescriptorNode recordType = createRecordTypeDescriptorNode(
                createToken(RECORD_KEYWORD),
                createToken(OPEN_BRACE_PIPE_TOKEN),
                createNodeList(recordFields),
                null,
                createToken(CLOSE_BRACE_PIPE_TOKEN));

        List<Node> schemaDoc = new ArrayList<>();
        MarkdownDocumentationNode documentationNode =
                createMarkdownDocumentationNode(createNodeList(schemaDoc));
        MetadataNode metadataNode = createMetadataNode(documentationNode, createEmptyNodeList());

        return createTypeDefinitionNode(metadataNode, createToken(PUBLIC_KEYWORD), createToken(TYPE_KEYWORD),
                createIdentifierToken(LISTENER_CONFIG_TYPE), recordType, createToken(SEMICOLON_TOKEN));
    }
}
