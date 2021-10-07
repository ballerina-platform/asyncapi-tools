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

package io.ballerina.asyncapi.codegenerator.usecase.utils;

import io.ballerina.compiler.syntax.tree.MarkdownDocumentationLineNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;

import java.util.ArrayList;
import java.util.List;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.*;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMarkdownDocumentationLineNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.DOCUMENTATION_DESCRIPTION;

/**
 * This class util for maintain the API doc comment related functions.
 */
public class DocCommentsUtils {
    public List<MarkdownDocumentationLineNode> createDescriptionComments(
            String description, boolean addExtraLine) {
        String[] descriptionLines = description.split("\n");
        List<MarkdownDocumentationLineNode> documentElements = new ArrayList<>();
        for (String line : descriptionLines) {
            MarkdownDocumentationLineNode documentationLineNode =
                    createMarkdownDocumentationLineNode(DOCUMENTATION_DESCRIPTION,
                            createToken(SyntaxKind.HASH_TOKEN), createNodeList(createIdentifierToken(line)));
            documentElements.add(documentationLineNode);
        }
        if (addExtraLine) {
            MarkdownDocumentationLineNode newLine = createMarkdownDocumentationLineNode(null,
                    createToken(SyntaxKind.HASH_TOKEN), createEmptyNodeList());
            documentElements.add(newLine);
        }
        return documentElements;
    }
}
