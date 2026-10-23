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

import java.util.List;

/**
 * Unit tests for {@link UnionTypeGenerator} verifying that {@code oneOf} sub-schemas are
 * joined with a Ballerina pipe ({@code |}) union separator.
 */
public class UnionTypeGeneratorTest {

    @Test
    void testGenerateTypeDescriptor_oneOfTwoSchemas() throws GeneratorException {
        List<AsyncApiSchema> oneOfSchemas = List.of(
                AsyncApiSchema.builder().type("string").build(),
                AsyncApiSchema.builder().type("integer").build()
        );
        AsyncApiSchema schema = AsyncApiSchema.builder().oneOf(oneOfSchemas).build();
        TypeDescriptorNode node = new UnionTypeGenerator(schema, "Event").generateTypeDescriptorNode();
        String result = node.toString();
        Assert.assertTrue(result.contains("|"),
                "Union type descriptor from two oneOf members should contain the pipe separator");
        Assert.assertTrue(result.contains("string"),
                "Union type descriptor should include 'string' from the first oneOf member");
        Assert.assertTrue(result.contains("int"),
                "Union type descriptor should include 'int' from the integer oneOf member via TYPE_MAP");
    }
}
