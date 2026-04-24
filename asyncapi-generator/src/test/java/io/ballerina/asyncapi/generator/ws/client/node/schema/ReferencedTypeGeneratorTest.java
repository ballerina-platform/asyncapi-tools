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
 * Unit tests for {@link ReferencedTypeGenerator} verifying that a schema carrying a
 * {@code name} (the resolved component key) is emitted as a simple name reference.
 */
public class ReferencedTypeGeneratorTest {

    @Test
    void testGenerateTypeDescriptor_returnsSimpleNameRef() throws GeneratorException {
        AsyncApiSchema schema = AsyncApiSchema.builder().name("FooEvent").build();
        TypeDescriptorNode node = new ReferencedTypeGenerator(schema).generateTypeDescriptorNode();
        Assert.assertTrue(node.toString().contains("FooEvent"),
                "ReferencedTypeGenerator should emit the schema name as a simple name reference");
    }
}
