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

import io.ballerina.compiler.syntax.tree.MarkdownDocumentationLineNode;
import io.ballerina.compiler.syntax.tree.MarkdownParameterDocumentationLineNode;
import io.ballerina.compiler.syntax.tree.MinutiaeList;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;

import java.util.ArrayList;
import java.util.List;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyMinutiaeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEndOfLineMinutiae;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createLiteralValueToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createMinutiaeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createWhitespaceMinutiae;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMarkdownDocumentationLineNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMarkdownParameterDocumentationLineNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.DOCUMENTATION_DESCRIPTION;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.HASH_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.MARKDOWN_DOCUMENTATION_LINE;

/**
 * Utility class for generating Ballerina API documentation comment nodes.
 */
public final class DocCommentsGenerator {

    private DocCommentsGenerator() {
    }

    /**
     * Creates a list of {@link MarkdownDocumentationLineNode}s from a description string.
     *
     * @param description  the documentation text
     * @param addExtraLine if {@code true}, appends a blank {@code #} line at the end
     * @return list of documentation line nodes
     */
    public static List<MarkdownDocumentationLineNode> createAPIDescriptionDoc(String description,
                                                                              boolean addExtraLine) {
        String[] descriptionLines = description.split("\n");
        List<MarkdownDocumentationLineNode> documentElements = new ArrayList<>();
        MinutiaeList singleWs = createMinutiaeList(createWhitespaceMinutiae(" "));
        MinutiaeList endOfLine = createMinutiaeList(createEndOfLineMinutiae(System.lineSeparator()));
        Token hashToken = createToken(HASH_TOKEN, createEmptyMinutiaeList(), singleWs);
        for (String line : descriptionLines) {
            MarkdownDocumentationLineNode documentationLineNode =
                    createMarkdownDocumentationLineNode(MARKDOWN_DOCUMENTATION_LINE, hashToken,
                            createNodeList(createLiteralValueToken(DOCUMENTATION_DESCRIPTION, line,
                                    createEmptyMinutiaeList(), endOfLine)));
            documentElements.add(documentationLineNode);
        }
        if (addExtraLine) {
            MarkdownDocumentationLineNode newLine = createMarkdownDocumentationLineNode(MARKDOWN_DOCUMENTATION_LINE,
                    createToken(SyntaxKind.HASH_TOKEN), createEmptyNodeList());
            documentElements.add(newLine);
        }
        return documentElements;
    }

    /**
     * Creates a {@link MarkdownParameterDocumentationLineNode} for a single parameter.
     *
     * @param paramName   the parameter name
     * @param description the parameter description
     * @return the parameter documentation line node
     */
    public static MarkdownParameterDocumentationLineNode createAPIParamDoc(String paramName,
                                                                           String description) {
        String[] paramDescriptionLines = description.split("\n");
        List<Node> documentElements = new ArrayList<>();
        for (String line : paramDescriptionLines) {
            if (!line.isBlank()) {
                documentElements.add(createIdentifierToken(
                        String.format("%s ", line)));
            }
        }
        return createMarkdownParameterDocumentationLineNode(null, createToken(SyntaxKind.HASH_TOKEN),
                createToken(SyntaxKind.PLUS_TOKEN), createIdentifierToken(paramName),
                createToken(SyntaxKind.MINUS_TOKEN), createNodeList(documentElements));
    }
}
