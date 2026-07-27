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
import io.ballerina.asyncapi.generator.http.model.ConnectionAuthConfig;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.MarkdownDocumentationNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.RecordTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMarkdownDocumentationNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMetadataNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRecordFieldNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRecordFieldWithDefaultValueNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRecordTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSpecificFieldNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTypeDefinitionNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.AT_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.COLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CONST_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.EQUAL_TOKEN;
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
     * field, one additional {@code string} field (default {@code ""}) per name in
     * {@code extraConfigFields} (populated from {@code $config('name')} references in the DSL),
     * and - when {@code connectionAuthConfig} is present - the outbound API auth fields matching
     * its type/flow (e.g. {@code clientId}/{@code clientSecret}/{@code refreshToken} for an
     * {@code oauth2} {@code authorizationCode} scheme). Secret-shaped fields (client secrets,
     * refresh/access tokens, passwords) are required, with no default. The token/refresh URL
     * field is defaulted to the value declared in the spec: the AsyncAPI {@code OAuthFlow}
     * object has no way to mark a URL as varying per deployment (unlike {@code servers}, which
     * has variables), so a fixed provider endpoint and a per-tenant one (e.g. a multi-tenant
     * identity provider's org-scoped token URL) look identical in the spec. Defaulting - rather
     * than hardcoding as a constant - lets the common case (a fixed global endpoint) work
     * unmodified while still leaving the field overridable for the per-tenant case.
     *
     * @param extraConfigFields    additional configurable field names beyond {@code webhookSecret}
     * @param connectionAuthConfig the resolved outbound auth configuration, if any
     * @return the generated {@link TypeDefinitionNode}
     * @throws GeneratorException if the connection auth configuration's type/flow is unrecognized
     */
    public static TypeDefinitionNode generate(List<String> extraConfigFields,
            Optional<ConnectionAuthConfig> connectionAuthConfig) throws GeneratorException {
        List<Node> recordFields = new ArrayList<>();
        recordFields.add(createRecordFieldWithDefaultValueNode(
                buildDisplayMetadata("Webhook Secret"),
                null,
                createBuiltinSimpleNameReferenceNode(null, createIdentifierToken("string")),
                createIdentifierToken(WEBHOOK_SECRET_FIELD),
                createToken(EQUAL_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken(DEFAULT_SECRET_CONST)),
                createToken(SEMICOLON_TOKEN)));

        for (String fieldName : extraConfigFields) {
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

        if (connectionAuthConfig.isPresent()) {
            recordFields.addAll(connectionAuthFields(connectionAuthConfig.get()));
        }

        RecordTypeDescriptorNode recordType = createRecordTypeDescriptorNode(
                createToken(RECORD_KEYWORD),
                createToken(OPEN_BRACE_TOKEN),
                createNodeList(recordFields),
                null,
                createToken(CLOSE_BRACE_TOKEN));

        List<Node> schemaDoc = new ArrayList<>();
        MarkdownDocumentationNode documentationNode =
                createMarkdownDocumentationNode(createNodeList(schemaDoc));
        MetadataNode metadataNode = createMetadataNode(documentationNode, createEmptyNodeList());

        return createTypeDefinitionNode(metadataNode, createToken(PUBLIC_KEYWORD), createToken(TYPE_KEYWORD),
                createIdentifierToken(LISTENER_CONFIG_TYPE), recordType, createToken(SEMICOLON_TOKEN));
    }

    /**
     * Builds the {@code ListenerConfig} record fields required for a given outbound auth
     * configuration, in the same field order used by the hand-written triggers this mirrors
     * (e.g. {@code clientId}, {@code clientSecret}, {@code refreshUrl}, {@code refreshToken}).
     *
     * @param config the resolved outbound auth configuration
     * @return the ordered list of generated field nodes
     * @throws GeneratorException if the configuration's type/flow combination is unrecognized
     */
    private static List<Node> connectionAuthFields(ConnectionAuthConfig config) throws GeneratorException {
        if (ConnectionAuthConfig.TYPE_USER_PASSWORD.equals(config.type())) {
            return List.of(
                    createRequiredStringField("username"),
                    createRequiredStringField("password"));
        }
        if (ConnectionAuthConfig.TYPE_OAUTH2.equals(config.type())) {
            if (ConnectionAuthConfig.FLOW_AUTHORIZATION_CODE.equals(config.flow())) {
                return List.of(
                        createRequiredStringField("clientId"),
                        createRequiredStringField("clientSecret"),
                        createUrlFieldWithDefault("refreshUrl", config.refreshUrl()),
                        createRequiredStringField("refreshToken"));
            }
            if (ConnectionAuthConfig.FLOW_CLIENT_CREDENTIALS.equals(config.flow())) {
                return List.of(
                        createRequiredStringField("clientId"),
                        createRequiredStringField("clientSecret"),
                        createUrlFieldWithDefault("tokenUrl", config.tokenUrl()));
            }
        }
        throw new GeneratorException(
                "Unrecognized connection auth type/flow combination: " + config.type() + "/" + config.flow());
    }

    private static Node createRequiredStringField(String fieldName) {
        return createRecordFieldNode(
                buildDisplayMetadata(toDisplayLabel(fieldName)),
                null,
                createBuiltinSimpleNameReferenceNode(null, createIdentifierToken("string")),
                createIdentifierToken(fieldName),
                null,
                createToken(SEMICOLON_TOKEN));
    }

    /**
     * Builds a {@code string} field defaulted to the URL extracted from the spec (e.g.
     * {@code string refreshUrl = "https://oauth2.googleapis.com/token";}). Still overridable at
     * deployment time, since the spec cannot distinguish a provider-fixed URL from a per-tenant
     * one (see {@link #generate}).
     */
    private static Node createUrlFieldWithDefault(String fieldName, String url) {
        return createRecordFieldWithDefaultValueNode(
                buildDisplayMetadata(toDisplayLabel(fieldName)),
                null,
                createBuiltinSimpleNameReferenceNode(null, createIdentifierToken("string")),
                createIdentifierToken(fieldName),
                createToken(EQUAL_TOKEN),
                createBasicLiteralNode(STRING_LITERAL,
                        createLiteralValueToken(STRING_LITERAL_TOKEN, "\"" + url + "\"",
                                createEmptyMinutiaeList(), createEmptyMinutiaeList())),
                createToken(SEMICOLON_TOKEN));
    }

    /**
     * Converts a camelCase field name (e.g. {@code clientSecret}) to a display label
     * (e.g. {@code "Client Secret"}).
     */
    private static String toDisplayLabel(String fieldName) {
        StringBuilder label = new StringBuilder();
        for (int i = 0; i < fieldName.length(); i++) {
            char current = fieldName.charAt(i);
            if (i == 0) {
                label.append(Character.toUpperCase(current));
            } else if (Character.isUpperCase(current)) {
                label.append(' ').append(current);
            } else {
                label.append(current);
            }
        }
        return label.toString();
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

    private static MetadataNode buildDisplayMetadata(String label) {
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
                                                        "\"" + label + "\"",
                                                        createEmptyMinutiaeList(), createEmptyMinutiaeList())))),
                        createToken(CLOSE_BRACE_TOKEN)));
        return createMetadataNode(null, createNodeList(annotation));
    }
}
