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
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.MarkdownDocumentationNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.RecordTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;

import java.util.ArrayList;
import java.util.List;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyMinutiaeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createLiteralValueToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createSeparatedNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createAnnotationNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBasicLiteralNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBuiltinSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createConstantDeclarationNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMappingConstructorExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMarkdownDocumentationLineNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMarkdownDocumentationNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMetadataNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRecordFieldWithDefaultValueNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRecordTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSpecificFieldNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTypeDefinitionNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.AT_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.COLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CONST_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.DOCUMENTATION_DESCRIPTION;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.EQUAL_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.HASH_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PUBLIC_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RECORD_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SEMICOLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.STRING_LITERAL;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.STRING_LITERAL_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.TYPE_KEYWORD;

/**
 * Generates the {@code public type ListenerConfig record { string webhookSecret = DEFAULT_SECRET; };}
 * type definition (and its {@code DEFAULT_SECRET} constant) for {@code data_types.bal}.
 *
 * <p>Deliberately minimal -- unlike {@code http:ListenerConfiguration}, this type is not spread
 * into the record, matching the shape used by every other currently-shipped trigger. Users who
 * need custom HTTP-level tuning construct their own {@code http:Listener} and pass it via the
 * listener's {@code listenOn} parameter instead.
 */
public class GenerateListenerConfigNode {

    public static final String LISTENER_CONFIG_TYPE = "ListenerConfig";
    public static final String WEBHOOK_SECRET_FIELD = "webhookSecret";
    public static final String DEFAULT_SECRET_CONST = "DEFAULT_SECRET";

    /**
     * Generates the {@code ListenerConfig} open-record type definition, with the webhook secret
     * field plus one additional {@code string} field (default {@code ""}) per name in
     * {@code extraConfigFields} (populated from {@code $config('name')} references in the DSL).
     *
     * @param extraConfigFields additional configurable field names beyond {@code webhookSecret}
     * @return the generated {@link TypeDefinitionNode}
     * @throws GeneratorException never thrown; declared for consistency with other node generators
     */
    public static TypeDefinitionNode generate(List<String> extraConfigFields) throws GeneratorException {
        List<Node> recordFields = new ArrayList<>();
        recordFields.add(createRecordFieldWithDefaultValueNode(
                buildWebhookSecretDisplayMetadata(),
                null,
                createBuiltinSimpleNameReferenceNode(null, createIdentifierToken("string")),
                createIdentifierToken(WEBHOOK_SECRET_FIELD),
                createToken(EQUAL_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken(DEFAULT_SECRET_CONST)),
                createToken(SEMICOLON_TOKEN)));

        for (String fieldName : extraConfigFields) {
            recordFields.add(createRecordFieldWithDefaultValueNode(
                    buildFieldDoc(String.format(
                            "Configurable value referenced by the webhook signature verification DSL"
                                    + " (`$config('%s')`).", fieldName)),
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
                createToken(OPEN_BRACE_TOKEN),
                createNodeList(recordFields),
                null,
                createToken(CLOSE_BRACE_TOKEN));

        List<Node> schemaDoc = new ArrayList<>();
        schemaDoc.add(createMarkdownDocumentationLineNode(DOCUMENTATION_DESCRIPTION,
                createToken(HASH_TOKEN), createNodeList(createIdentifierToken(
                        "Configuration for the webhook listener, including the secret used to verify"
                                + " incoming requests."))));
        MarkdownDocumentationNode documentationNode =
                createMarkdownDocumentationNode(createNodeList(schemaDoc));
        MetadataNode metadataNode = createMetadataNode(documentationNode, createEmptyNodeList());

        return createTypeDefinitionNode(metadataNode, createToken(PUBLIC_KEYWORD), createToken(TYPE_KEYWORD),
                createIdentifierToken(LISTENER_CONFIG_TYPE), recordType, createToken(SEMICOLON_TOKEN));
    }

    /**
     * Generates the {@code const string DEFAULT_SECRET = "";} declaration referenced by the
     * {@code webhookSecret} field's default value and the listener's {@code init()} default.
     *
     * @return the generated {@link ModuleMemberDeclarationNode}
     */
    public static ModuleMemberDeclarationNode generateDefaultSecretConst() {
        return createConstantDeclarationNode(
                null,
                null,
                createToken(CONST_KEYWORD),
                createBuiltinSimpleNameReferenceNode(null, createIdentifierToken("string")),
                createIdentifierToken(DEFAULT_SECRET_CONST),
                createToken(EQUAL_TOKEN),
                createBasicLiteralNode(STRING_LITERAL,
                        createLiteralValueToken(STRING_LITERAL_TOKEN, "\"\"",
                                createEmptyMinutiaeList(), createEmptyMinutiaeList())),
                createToken(SEMICOLON_TOKEN));
    }

    private static MetadataNode buildWebhookSecretDisplayMetadata() {
        AnnotationNode annotation = createAnnotationNode(
                createToken(AT_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken("display")),
                createMappingConstructorExpressionNode(
                        createToken(OPEN_BRACE_TOKEN),
                        createSeparatedNodeList(
                                createSpecificFieldNode(
                                        null,
                                        createIdentifierToken("label"),
                                        createToken(COLON_TOKEN),
                                        createBasicLiteralNode(STRING_LITERAL,
                                                createLiteralValueToken(STRING_LITERAL_TOKEN,
                                                        "\"Webhook Secret\"",
                                                        createEmptyMinutiaeList(), createEmptyMinutiaeList())))),
                        createToken(CLOSE_BRACE_TOKEN)));
        List<Node> docLines = new ArrayList<>();
        docLines.add(createMarkdownDocumentationLineNode(DOCUMENTATION_DESCRIPTION,
                createToken(HASH_TOKEN), createNodeList(createIdentifierToken(
                        "The secret used to verify incoming webhook signatures."))));
        MarkdownDocumentationNode documentationNode = createMarkdownDocumentationNode(createNodeList(docLines));
        return createMetadataNode(documentationNode, createNodeList(annotation));
    }

    private static MetadataNode buildFieldDoc(String text) {
        List<Node> docLines = new ArrayList<>();
        docLines.add(createMarkdownDocumentationLineNode(DOCUMENTATION_DESCRIPTION,
                createToken(HASH_TOKEN), createNodeList(createIdentifierToken(text))));
        MarkdownDocumentationNode documentationNode = createMarkdownDocumentationNode(createNodeList(docLines));
        return createMetadataNode(documentationNode, createEmptyNodeList());
    }
}
