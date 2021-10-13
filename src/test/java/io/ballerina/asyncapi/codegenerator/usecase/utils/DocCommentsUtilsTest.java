/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.asyncapi.codegenerator.usecase.utils;

import io.ballerina.compiler.syntax.tree.MarkdownDocumentationNode;
import io.ballerina.compiler.syntax.tree.Node;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMarkdownDocumentationNode;

/**
 * Test the utils which are being used to generate the doc comments in Ballerina.
 */
public class DocCommentsUtilsTest {
    private final DocCommentsUtils docCommentsUtils = new DocCommentsUtils();

    @Test(
            description = "Test the functionality of the createDescriptionComments function"
    )
    public void testCreateDescriptionComments() {
        List<Node> commentsDoc1 =
                new ArrayList<>(docCommentsUtils.createDescriptionComments("Foo bar\nFoo bar", false));
        MarkdownDocumentationNode documentationNode1 =
                createMarkdownDocumentationNode(
                        createNodeList(commentsDoc1));
        Assert.assertEquals(documentationNode1.toSourceCode(), "#Foo bar#Foo bar");

        List<Node> commentsDoc2 =
                new ArrayList<>(docCommentsUtils.createDescriptionComments("Foo bar\nFoo bar", true));
        MarkdownDocumentationNode documentationNode2 =
                createMarkdownDocumentationNode(
                        createNodeList(commentsDoc2));
        Assert.assertEquals(documentationNode2.toSourceCode(), "#Foo bar#Foo bar#");
    }
}
