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
package io.ballerina.asyncapi.generator.ws.client.generator;

import com.fasterxml.jackson.databind.JsonNode;
import io.ballerina.asyncapi.core.model.component.AsyncApiComponent;
import io.ballerina.asyncapi.core.model.component.AsyncApiSchema;
import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.ws.client.extractor.DispatcherStreamIdExtractor;
import io.ballerina.asyncapi.generator.ws.client.model.WsClientConfig;
import io.ballerina.asyncapi.generator.ws.client.node.schema.TypeGenerator;
import io.ballerina.asyncapi.generator.ws.client.utils.CodegenUtils;
import io.ballerina.compiler.syntax.tree.AbstractNodeFactory;
import io.ballerina.compiler.syntax.tree.MarkdownDocumentationNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocuments;
import org.ballerinalang.formatter.core.Formatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.ballerina.asyncapi.generator.ws.client.extractor.DispatcherKeyExtractor.X_DISPATCHER_KEY;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PUBLIC_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SEMICOLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.TYPE_KEYWORD;

/**
 * Generates {@code types.bal} content for a WebSocket client.
 */
public class DataTypesGenerator {

    private static final String MESSAGE_TYPE_NAME = "Message";
    private static final String MESSAGE_WITH_ID_TYPE_NAME = "MessageWithId";
    private static final String DEFAULT_DISPATCHER_KEY = "event";

    private final WsClientConfig config;

    /**
     * Creates a new data types generator.
     *
     * @param config the WebSocket client generation configuration
     */
    public DataTypesGenerator(WsClientConfig config) {
        this.config = config;
    }

    /**
     * Generates the Ballerina source content for {@code types.bal}.
     *
     * @return generated source as a string
     * @throws GeneratorException if a schema cannot be converted to a Ballerina type
     */
    public String generate() throws GeneratorException {
        List<TypeDefinitionNode> typeDefinitionNodeList = new ArrayList<>();

        // Always generate the synthetic Message type first
        addMessageType(typeDefinitionNodeList);

        // Generate MessageWithId type if dispatcherStreamId extension is present
        Optional<String> streamIdOpt = new DispatcherStreamIdExtractor(config.getAsyncApi()).extract();
        if (streamIdOpt.isPresent()) {
            String dispatcherStreamId = streamIdOpt.get();
            String dispatcherKey = DEFAULT_DISPATCHER_KEY;
            Map<String, JsonNode> extensions = config.getAsyncApi().getAsyncApiExtensions().orElse(null);
            if (extensions != null && extensions.containsKey(X_DISPATCHER_KEY)) {
                dispatcherKey = extensions.get(X_DISPATCHER_KEY).asText();
            }
            addMessageWithIdType(dispatcherKey, dispatcherStreamId, typeDefinitionNodeList);
        }

        // Generate types from component schemas (if any)
        AsyncApiComponent component = config.getAsyncApi().getAsyncApiComponents().orElse(null);
        if (component != null && component.schemas() != null && !component.schemas().isEmpty()) {
            for (Map.Entry<String, AsyncApiSchema> entry : component.schemas().entrySet()) {
                AsyncApiSchema schema = entry.getValue();
                if (CodegenUtils.isCloseFrameSchema(schema)) {
                    continue;
                }
                String typeName = CodegenUtils.getValidName(entry.getKey().trim(), true);
                TypeDescriptorNode typeDesc = TypeGenerator.of(schema, typeName).generateTypeDescriptorNode();

                MetadataNode metadata = null;
                if (schema.description() != null) {
                    List<Node> docLines = new ArrayList<>(
                            DocCommentsGenerator.createAPIDescriptionDoc(schema.description(), false));
                    MarkdownDocumentationNode docNode = NodeFactory.createMarkdownDocumentationNode(
                            AbstractNodeFactory.createNodeList(docLines));
                    metadata = NodeFactory.createMetadataNode(docNode, AbstractNodeFactory.createEmptyNodeList());
                }

                TypeDefinitionNode typeDefNode = NodeFactory.createTypeDefinitionNode(
                        metadata,
                        createToken(PUBLIC_KEYWORD),
                        createToken(TYPE_KEYWORD),
                        AbstractNodeFactory.createIdentifierToken(typeName),
                        typeDesc,
                        createToken(SEMICOLON_TOKEN));
                CodegenUtils.updateTypeDefNodeList(typeName, typeDefNode, typeDefinitionNodeList);
            }
        }

        NodeList<ModuleMemberDeclarationNode> moduleMembers = AbstractNodeFactory.createNodeList(
                typeDefinitionNodeList.toArray(new TypeDefinitionNode[0]));
        ModulePartNode modulePartNode = NodeFactory.createModulePartNode(
                AbstractNodeFactory.createEmptyNodeList(),
                moduleMembers,
                AbstractNodeFactory.createIdentifierToken(""));
        TextDocument textDoc = TextDocuments.from("");
        SyntaxTree syntaxTree = SyntaxTree.from(textDoc).modifyWith(modulePartNode);

        try {
            String source = Formatter.format(syntaxTree).toSourceCode();
            String license = config.getLicenseHeader();
            if (license == null || license.isBlank()) {
                return source;
            }
            return license.stripTrailing() + "\n\n" + source;
        } catch (Exception e) {
            throw new GeneratorException("Failed to format generated types source", e);
        }
    }

    private void addMessageWithIdType(String dispatcherKey, String dispatcherStreamId,
                                      List<TypeDefinitionNode> typeDefinitionNodeList) {
        String typeSource = String.format("readonly & record { string %s; string %s; }",
                CodegenUtils.escapeIdentifier(dispatcherKey),
                CodegenUtils.escapeIdentifier(dispatcherStreamId));
        TypeDescriptorNode typeDesc = (TypeDescriptorNode) NodeParser.parseTypeDescriptor(typeSource);
        TypeDefinitionNode typeDef = NodeFactory.createTypeDefinitionNode(
                null,
                createToken(PUBLIC_KEYWORD),
                createToken(TYPE_KEYWORD),
                AbstractNodeFactory.createIdentifierToken(MESSAGE_WITH_ID_TYPE_NAME),
                typeDesc,
                createToken(SEMICOLON_TOKEN));
        typeDefinitionNodeList.add(typeDef);
    }

    private void addMessageType(List<TypeDefinitionNode> typeDefinitionNodeList) {
        Map<String, JsonNode> extensions = config.getAsyncApi().getAsyncApiExtensions().orElse(null);
        String dispatcherKey = DEFAULT_DISPATCHER_KEY;
        if (extensions != null && extensions.containsKey(X_DISPATCHER_KEY)) {
            dispatcherKey = extensions.get(X_DISPATCHER_KEY).asText();
        }
        String messageTypeSource = String.format("readonly & record { string %s; }",
                CodegenUtils.escapeIdentifier(dispatcherKey));
        TypeDescriptorNode messageTypeDesc = (TypeDescriptorNode) NodeParser.parseTypeDescriptor(messageTypeSource);
        TypeDefinitionNode messageTypeDef = NodeFactory.createTypeDefinitionNode(
                null,
                createToken(PUBLIC_KEYWORD),
                createToken(TYPE_KEYWORD),
                AbstractNodeFactory.createIdentifierToken(MESSAGE_TYPE_NAME),
                messageTypeDesc,
                createToken(SEMICOLON_TOKEN));
        typeDefinitionNodeList.add(messageTypeDef);
    }
}
