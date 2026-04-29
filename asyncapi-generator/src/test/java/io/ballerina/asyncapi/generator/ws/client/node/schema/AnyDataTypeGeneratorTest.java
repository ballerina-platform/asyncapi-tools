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
package io.ballerina.asyncapi.generator.ws.client.node.schema;

import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link AnyDataTypeGenerator} verifying that the generated type descriptor
 * is the Ballerina {@code anydata} built-in keyword.
 */
public class AnyDataTypeGeneratorTest {

    @Test
    void testGenerateTypeDescriptor_returnsAnydata() throws GeneratorException {
        TypeDescriptorNode node = new AnyDataTypeGenerator().generateTypeDescriptorNode();
        Assert.assertEquals(node.toString(), "anydata",
                "AnyDataTypeGenerator should emit exactly the Ballerina 'anydata' type keyword");
    }
}
