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
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

/**
 * Unit tests for {@link TypeGenerator#of(AsyncApiSchema, String)} verifying that each schema
 * shape is dispatched to the correct concrete generator and produces the expected Ballerina type.
 */
public class TypeGeneratorFactoryTest {

    @Test
    void testOf_dispatchesToCorrectGenerator_forEachSchemaShape() throws GeneratorException {
        // ref: schema with name() set → ReferencedTypeGenerator → emits the component name
        AsyncApiSchema refSchema = AsyncApiSchema.builder().name("FooEvent").build();
        String refResult = TypeGenerator.of(refSchema, "T").generateTypeDescriptorNode().toString();
        Assert.assertTrue(refResult.contains("FooEvent"),
                "Schema with name() should be dispatched to ReferencedTypeGenerator and emit the name");

        // array: type='array' → ArrayTypeGenerator → emits '[' as part of the array dimension
        AsyncApiSchema arraySchema = AsyncApiSchema.builder().type("array").build();
        String arrayResult = TypeGenerator.of(arraySchema, "T").generateTypeDescriptorNode().toString();
        Assert.assertTrue(arrayResult.contains("["),
                "Schema with type='array' should be dispatched to ArrayTypeGenerator");

        // oneOf: oneOf list → UnionTypeGenerator → emits '|' between member types
        List<AsyncApiSchema> oneOfList = List.of(
                AsyncApiSchema.builder().type("string").build(),
                AsyncApiSchema.builder().type("integer").build()
        );
        AsyncApiSchema oneOfSchema = AsyncApiSchema.builder().oneOf(oneOfList).build();
        String oneOfResult = TypeGenerator.of(oneOfSchema, "T").generateTypeDescriptorNode().toString();
        Assert.assertTrue(oneOfResult.contains("|"),
                "Schema with oneOf should be dispatched to UnionTypeGenerator and emit a pipe union");

        // primitive: type='integer' → PrimitiveTypeGenerator → emits 'int' via TYPE_MAP
        AsyncApiSchema primitiveSchema = AsyncApiSchema.builder().type("integer").build();
        String primitiveResult =
                TypeGenerator.of(primitiveSchema, "T").generateTypeDescriptorNode().toString();
        Assert.assertEquals(primitiveResult, "int",
                "Schema with type='integer' should be dispatched to PrimitiveTypeGenerator");

        // object: type='object', no properties → RecordTypeGenerator → emits 'record'
        AsyncApiSchema objectSchema = AsyncApiSchema.builder().type("object").build();
        String objectResult = TypeGenerator.of(objectSchema, "T").generateTypeDescriptorNode().toString();
        Assert.assertTrue(objectResult.contains("record"),
                "Schema with type='object' should be dispatched to RecordTypeGenerator");

        // fallback: no type, name, or composite keywords → AnyDataTypeGenerator → emits 'anydata'
        AsyncApiSchema fallbackSchema = AsyncApiSchema.builder().build();
        String fallbackResult =
                TypeGenerator.of(fallbackSchema, "T").generateTypeDescriptorNode().toString();
        Assert.assertEquals(fallbackResult, "anydata",
                "Schema with no type, name, or composite fields should fall back to AnyDataTypeGenerator");
    }

    @Test
    void testOf_keepsTheRecord_whenAnyOfOnlyRequiresItsFields() throws GeneratorException {
        // "text or attachmentIds" says which fields must be there; the second member also restates a field's minItems
        AsyncApiSchema text = AsyncApiSchema.builder().type("string").build();
        AsyncApiSchema ids = AsyncApiSchema.builder().type("array")
                .items(AsyncApiSchema.builder().type("string").build()).build();
        AsyncApiSchema send = AsyncApiSchema.builder().type("object")
                .properties(Map.of("text", text, "attachmentIds", ids))
                .anyOf(List.of(AsyncApiSchema.builder().required(List.of("text")).build(),
                        AsyncApiSchema.builder().required(List.of("attachmentIds"))
                                .properties(Map.of("attachmentIds", AsyncApiSchema.builder().minItems(1).build()))
                                .build()))
                .build();
        // The node isn't formatted, so it is compared without whitespace
        String result = TypeGenerator.of(send, "Send").generateTypeDescriptorNode().toSourceCode()
                .replaceAll("\\s", "");
        Assert.assertTrue(result.startsWith("record{"), result);
        Assert.assertTrue(result.contains("stringtext?;") && result.contains("string[]attachmentIds?;"), result);
    }

    @Test
    void testOf_isAUnion_whenAnAnyOfMemberHasAShapeOfItsOwn() throws GeneratorException {
        // A member with a field the object doesn't declare is another shape, so the type stays a union
        AsyncApiSchema send = AsyncApiSchema.builder().type("object")
                .properties(Map.of("text", AsyncApiSchema.builder().type("string").build()))
                .anyOf(List.of(AsyncApiSchema.builder().required(List.of("text")).build(),
                        AsyncApiSchema.builder().properties(Map.of("file",
                                AsyncApiSchema.builder().type("string").build())).build()))
                .build();
        String result = TypeGenerator.of(send, "Send").generateTypeDescriptorNode().toSourceCode();
        Assert.assertTrue(result.contains("|"), result);
    }
}
