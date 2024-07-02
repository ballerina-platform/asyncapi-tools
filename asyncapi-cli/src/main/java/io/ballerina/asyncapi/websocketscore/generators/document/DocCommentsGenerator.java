/*
 *  Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
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
package io.ballerina.asyncapi.websocketscore.generators.document;

import io.ballerina.asyncapi.websocketscore.GeneratorUtils;
import io.ballerina.compiler.syntax.tree.MarkdownDocumentationLineNode;
import io.ballerina.compiler.syntax.tree.MarkdownParameterDocumentationLineNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;

import java.util.ArrayList;
import java.util.List;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyMinutiaeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createLiteralValueToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMarkdownDocumentationLineNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMarkdownParameterDocumentationLineNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.DOCUMENTATION_DESCRIPTION;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.HASH_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.MARKDOWN_DOCUMENTATION_LINE;

/**
 * This class util for maintain the API doc comment related functions.
 *
 */
public class DocCommentsGenerator {

    public static List<MarkdownDocumentationLineNode> createAPIDescriptionDoc(String description,
                                                                              boolean addExtraLine) {
        // Capitalize the first letter of the description. This is to maintain consistency
        String[] descriptionLines = description.split("\n");
        List<MarkdownDocumentationLineNode> documentElements = new ArrayList<>();
        Token hashToken = createToken(HASH_TOKEN, createEmptyMinutiaeList(), GeneratorUtils.SINGLE_WS_MINUTIAE);
        for (String line : descriptionLines) {
            MarkdownDocumentationLineNode documentationLineNode =
                    createMarkdownDocumentationLineNode(MARKDOWN_DOCUMENTATION_LINE, hashToken,
                            createNodeList(createLiteralValueToken(DOCUMENTATION_DESCRIPTION, line,
                                    createEmptyMinutiaeList(),
                                    GeneratorUtils.SINGLE_END_OF_LINE_MINUTIAE)));
            documentElements.add(documentationLineNode);
        }
        if (addExtraLine) {
            MarkdownDocumentationLineNode newLine = createMarkdownDocumentationLineNode(MARKDOWN_DOCUMENTATION_LINE,
                    createToken(SyntaxKind.HASH_TOKEN), createEmptyNodeList());
            documentElements.add(newLine);
        }
        return documentElements;
    }

    public static MarkdownParameterDocumentationLineNode createAPIParamDoc(String paramName, String description) {
        String[] paramDescriptionLines = description.split("\n");
        List<Node> documentElements = new ArrayList<>();
        for (String line : paramDescriptionLines) {
            if (!line.isBlank()) {
                documentElements.add(createIdentifierToken(line + " "));
            }
        }
        return createMarkdownParameterDocumentationLineNode(null, createToken(SyntaxKind.HASH_TOKEN),
                createToken(SyntaxKind.PLUS_TOKEN), createIdentifierToken(paramName),
                createToken(SyntaxKind.MINUS_TOKEN), createNodeList(documentElements));
    }
}
