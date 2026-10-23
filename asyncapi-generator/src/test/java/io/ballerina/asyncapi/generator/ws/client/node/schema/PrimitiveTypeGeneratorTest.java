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
 * Unit tests for {@link PrimitiveTypeGenerator} verifying that AsyncAPI primitive types
 * map to the correct Ballerina built-in type keywords.
 */
public class PrimitiveTypeGeneratorTest {

    @Test
    void testGenerateTypeDescriptor_string() throws GeneratorException {
        AsyncApiSchema schema = AsyncApiSchema.builder().type("string").build();
        TypeDescriptorNode node = new PrimitiveTypeGenerator(schema).generateTypeDescriptorNode();
        Assert.assertEquals(node.toString(), "string",
                "Schema of type 'string' should map to Ballerina 'string'");
    }

    @Test
    void testGenerateTypeDescriptor_integer() throws GeneratorException {
        AsyncApiSchema schema = AsyncApiSchema.builder().type("integer").build();
        TypeDescriptorNode node = new PrimitiveTypeGenerator(schema).generateTypeDescriptorNode();
        Assert.assertEquals(node.toString(), "int",
                "Schema of type 'integer' should map to Ballerina 'int' via TYPE_MAP");
    }

    @Test
    void testGenerateTypeDescriptor_boolean() throws GeneratorException {
        AsyncApiSchema schema = AsyncApiSchema.builder().type("boolean").build();
        TypeDescriptorNode node = new PrimitiveTypeGenerator(schema).generateTypeDescriptorNode();
        Assert.assertEquals(node.toString(), "boolean",
                "Schema of type 'boolean' should map to Ballerina 'boolean'");
    }
}
