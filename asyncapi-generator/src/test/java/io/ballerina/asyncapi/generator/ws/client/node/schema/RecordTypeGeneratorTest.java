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

import java.util.Map;

/**
 * Unit tests for {@link RecordTypeGenerator} verifying that {@code additionalProperties} controls
 * whether the generated Ballerina record is open ({@code record {}}) or closed ({@code record {||}}).
 */
public class RecordTypeGeneratorTest {

    @Test
    void testGenerateTypeDescriptor_closedRecord() throws GeneratorException {
        Map<String, AsyncApiSchema> props = Map.of(
                "id", AsyncApiSchema.builder().type("string").build());
        AsyncApiSchema schema = AsyncApiSchema.builder()
                .type("object")
                .properties(props)
                .additionalProperties(Boolean.FALSE)
                .build();
        TypeDescriptorNode node = new RecordTypeGenerator(schema, "Foo").generateTypeDescriptorNode();
        String result = node.toString();
        Assert.assertTrue(result.contains("{|"),
                "Record with additionalProperties=false should use the closed-record '{|' open brace token");
        Assert.assertTrue(result.contains("|}"),
                "Record with additionalProperties=false should use the closed-record '|}' close brace token");
    }

    @Test
    void testGenerateTypeDescriptor_openRecordWhenAdditionalPropertiesTrue() throws GeneratorException {
        Map<String, AsyncApiSchema> props = Map.of(
                "id", AsyncApiSchema.builder().type("string").build());
        AsyncApiSchema schema = AsyncApiSchema.builder()
                .type("object")
                .properties(props)
                .additionalProperties(Boolean.TRUE)
                .build();
        TypeDescriptorNode node = new RecordTypeGenerator(schema, "Foo").generateTypeDescriptorNode();
        String result = node.toString();
        Assert.assertTrue(result.contains("record"),
                "Record with additionalProperties=true should still emit a Ballerina record type");
        Assert.assertFalse(result.contains("{|"),
                "Record with additionalProperties=true should use open-record '{' brace, not closed '{|'");
    }
}
