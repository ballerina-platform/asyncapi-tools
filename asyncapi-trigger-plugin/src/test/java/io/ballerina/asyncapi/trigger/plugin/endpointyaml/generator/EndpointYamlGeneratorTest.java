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
package io.ballerina.asyncapi.trigger.plugin.endpointyaml.generator;

import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.tools.text.TextDocuments;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests that a trigger service's base path is computed the way the ballerina/http plugin computes it for an
 * HTTP service; the expected values were taken from bal build --export-endpoints on HTTP services.
 */
public class EndpointYamlGeneratorTest {

    @DataProvider
    public Object[][] attachPoints() {
        return new Object[][]{
                {"service github:PushService on l {}", ""},
                {"service github:PushService /github on l {}", "/github"},
                {"service github:PushService /org/events on l {}", "/org/events"},
                {"service github:PushService \"/quoted/path\" on l {}", "/quoted/path"},
                {"service github:PushService \"/a/b/\" on l {}", "/a/b/"},
                {"service github:PushService /team\\-a on l {}", "/team\\-a"},
        };
    }

    @Test(dataProvider = "attachPoints")
    public void testBasePath(String source, String expected) {
        ModulePartNode root = SyntaxTree.from(TextDocuments.from(source)).rootNode();
        ServiceDeclarationNode service = (ServiceDeclarationNode) root.members().get(0);
        Assert.assertEquals(EndpointYamlGenerator.getBasePath(service), expected);
    }
}
