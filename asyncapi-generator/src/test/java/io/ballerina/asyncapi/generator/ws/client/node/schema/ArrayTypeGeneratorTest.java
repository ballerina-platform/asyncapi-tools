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

import io.ballerina.asyncapi.core.model.component.AsyncApiSchema;
import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link ArrayTypeGenerator} verifying element-type propagation and the
 * {@code anydata} fallback when {@code items} is absent.
 */
public class ArrayTypeGeneratorTest {

    @Test
    void testGenerateTypeDescriptor_withItems() throws GeneratorException {
        AsyncApiSchema itemSchema = AsyncApiSchema.builder().type("string").build();
        AsyncApiSchema schema = AsyncApiSchema.builder().type("array").items(itemSchema).build();
        TypeDescriptorNode node = new ArrayTypeGenerator(schema, "Foo").generateTypeDescriptorNode();
        String result = node.toString();
        Assert.assertTrue(result.contains("string"),
                "Array schema with string items should include 'string' in the type descriptor");
        Assert.assertTrue(result.contains("["),
                "Array type descriptor should contain the open-bracket character of the array dimension");
    }

    @Test
    void testGenerateTypeDescriptor_nullItemsThrows() throws GeneratorException {
        AsyncApiSchema schema = AsyncApiSchema.builder().type("array").build();
        TypeDescriptorNode node = new ArrayTypeGenerator(schema, "Foo").generateTypeDescriptorNode();
        String result = node.toString();
        Assert.assertTrue(result.contains("anydata"),
                "Array schema with null items should fall back to 'anydata' element type");
        Assert.assertTrue(result.contains("["),
                "Array type descriptor with null items should still include the array dimension brackets");
    }
}
