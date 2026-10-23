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
import java.util.Map;

/**
 * Unit tests for {@link AllOfRecordTypeGenerator} verifying that fields from multiple inline
 * {@code allOf} object schemas are merged into a single Ballerina record type.
 */
public class AllOfRecordTypeGeneratorTest {

    @Test
    void testGenerateTypeDescriptor_mergesTwoObjectSchemas() throws GeneratorException {
        Map<String, AsyncApiSchema> props1 = Map.of(
                "name", AsyncApiSchema.builder().type("string").build());
        AsyncApiSchema member1 = AsyncApiSchema.builder()
                .type("object")
                .properties(props1)
                .required(List.of("name"))
                .build();

        Map<String, AsyncApiSchema> props2 = Map.of(
                "age", AsyncApiSchema.builder().type("integer").build());
        AsyncApiSchema member2 = AsyncApiSchema.builder()
                .type("object")
                .properties(props2)
                .required(List.of("age"))
                .build();

        AsyncApiSchema schema = AsyncApiSchema.builder().allOf(List.of(member1, member2)).build();
        TypeDescriptorNode node =
                new AllOfRecordTypeGenerator(schema, "MergedEvent").generateTypeDescriptorNode();
        String result = node.toString();

        Assert.assertTrue(result.contains("record"),
                "allOf of two object schemas should produce a Ballerina record type descriptor");
        Assert.assertTrue(result.contains("name"),
                "Merged record should include the 'name' field from the first allOf member");
        Assert.assertTrue(result.contains("age"),
                "Merged record should include the 'age' field from the second allOf member");
    }
}
