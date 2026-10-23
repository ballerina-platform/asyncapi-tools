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
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Unit tests for {@link DocCommentsGenerator} verifying that multi-line description
 * strings produce one node per line, and that parameter documentation nodes embed
 * both the parameter name and the description text.
 */
public class DocCommentsGeneratorTest {

    @Test
    void testCreateAPIDescriptionDoc_oneNodePerLine() {
        String description = "First line\nSecond line\nThird line";
        List<MarkdownDocumentationLineNode> nodes =
                DocCommentsGenerator.createAPIDescriptionDoc(description, false);
        Assert.assertEquals(nodes.size(), 3,
                "createAPIDescriptionDoc should produce exactly one node per newline-delimited line");
        boolean hasFirstLine = nodes.stream().anyMatch(n -> n.toString().contains("First line"));
        Assert.assertTrue(hasFirstLine,
                "Documentation nodes should contain the first line of the description text");
    }

    @Test
    void testCreateAPIParamDoc_containsParamNameAndDescription() {
        MarkdownParameterDocumentationLineNode node =
                DocCommentsGenerator.createAPIParamDoc("payload", "The message payload to send");
        String result = node.toString();
        Assert.assertTrue(result.contains("payload"),
                "Parameter documentation node should contain the parameter name 'payload'");
        Assert.assertTrue(result.contains("The message payload"),
                "Parameter documentation node should contain the description text");
    }
}
