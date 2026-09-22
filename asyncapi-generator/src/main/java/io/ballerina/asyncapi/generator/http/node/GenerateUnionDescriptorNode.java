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
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;

import java.util.ArrayList;
import java.util.List;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMarkdownDocumentationLineNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMarkdownDocumentationNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMetadataNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTypeDefinitionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createUnionTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.DOCUMENTATION_DESCRIPTION;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.HASH_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PUBLIC_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SEMICOLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.TYPE_KEYWORD;

/**
 * Generates a {@code public type <name> T1|T2|...;} union type definition node.
 */
public class GenerateUnionDescriptorNode implements Generator {

    private final List<TypeDescriptorNode> nodes;
    private final String identifierName;
    private final String description;

    /**
     * Creates a generator for a union type definition.
     *
     * @param nodes          the list of type descriptors to union
     * @param identifierName the name of the resulting union type
     * @param description    a one-line doc comment for the union type
     */
    public GenerateUnionDescriptorNode(List<TypeDescriptorNode> nodes, String identifierName, String description)
            throws GeneratorException {
        if (nodes == null) {
            throw new GeneratorException("nodes must not be null");
        }
        if (identifierName == null || identifierName.isBlank()) {
            throw new GeneratorException("identifierName must not be null or blank");
        }
        this.nodes = nodes;
        this.identifierName = identifierName;
        this.description = description;
    }

    @Override
    public TypeDefinitionNode generate() throws GeneratorException {
        if (nodes.isEmpty()) {
            throw new GeneratorException("Nodes list is empty, hence can't generate the Union Node");
        }
        List<Node> docLines = new ArrayList<>();
        if (description != null && !description.isBlank()) {
            docLines.add(createMarkdownDocumentationLineNode(DOCUMENTATION_DESCRIPTION,
                    createToken(HASH_TOKEN), createNodeList(createIdentifierToken(description))));
        }
        MetadataNode metadataNode = createMetadataNode(
                createMarkdownDocumentationNode(createNodeList(docLines)), createEmptyNodeList());
        return createTypeDefinitionNode(metadataNode, createToken(PUBLIC_KEYWORD),
                createToken(TYPE_KEYWORD), createIdentifierToken(identifierName),
                buildUnionDescriptor(nodes), createToken(SEMICOLON_TOKEN));
    }

    private TypeDescriptorNode buildUnionDescriptor(List<TypeDescriptorNode> nodeList) {
        TypeDescriptorNode result = nodeList.get(0);
        for (int i = 1; i < nodeList.size(); i++) {
            result = createUnionTypeDescriptorNode(
                    result, createToken(SyntaxKind.PIPE_TOKEN), nodeList.get(i));
        }
        return result;
    }
}
