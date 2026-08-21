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
package io.ballerina.asyncapi.generator.http.generator;

import io.ballerina.asyncapi.core.model.component.AsyncApiSchema;
import io.ballerina.asyncapi.generator.GeneratorException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Unit tests for {@link DataTypesGenerator}, specifically covering when {@code import
 * ballerina/http;} should and should not be emitted. Ballerina rejects an unused import as a
 * compile error, so the import must only be added when a schema field actually needs the
 * {@code @http:Header} annotation it enables.
 */
public class DataTypesGeneratorTest {

    @Test
    void testGenerateWithNoHyphenatedFieldsOmitsHttpImport() throws GeneratorException {
        AsyncApiSchema schema = AsyncApiSchema.builder()
                .type("object")
                .properties(Map.of(
                        "eventId", AsyncApiSchema.builder().type("string").build(),
                        "leadId", AsyncApiSchema.builder().type("string").build()))
                .build();
        String result = new DataTypesGenerator(Map.of("LeadEvent", schema), Optional.empty()).generate();

        Assert.assertFalse(result.contains("import ballerina/http;"),
                "No field needs @http:Header, so the http import should not be generated: " + result);
        Assert.assertFalse(result.contains("@http:Header"), "No field should get an @http:Header annotation");
    }

    @Test
    void testGenerateWithHyphenatedFieldIncludesHttpImportAndHeaderAnnotation() throws GeneratorException {
        AsyncApiSchema schema = AsyncApiSchema.builder()
                .type("object")
                .properties(Map.of(
                        "X-Test-Header", AsyncApiSchema.builder().type("string").build(),
                        "leadId", AsyncApiSchema.builder().type("string").build()))
                .build();
        String result = new DataTypesGenerator(Map.of("LeadEvent", schema), Optional.empty()).generate();

        Assert.assertTrue(result.contains("import ballerina/http;"),
                "The 'X-Test-Header' field name requires @http:Header, so the http import "
                        + "must be generated: " + result);
        Assert.assertTrue(result.contains("@http:Header"), "The hyphenated field should get an @http:Header "
                + "annotation: " + result);
    }

    @Test
    void testGenerateWithNoPropertiesOmitsHttpImport() throws GeneratorException {
        // A schema with no properties at all has nothing that could ever need @http:Header.
        AsyncApiSchema schema = AsyncApiSchema.builder().type("object").build();
        String result = new DataTypesGenerator(Map.of("EmptyEvent", schema), Optional.empty()).generate();

        Assert.assertFalse(result.contains("import ballerina/http;"),
                "A schema with no properties can't need @http:Header: " + result);
    }

    @Test
    void testGenericDataTypeUnionOrdersLooseSchemasLast() throws GeneratorException {
        // "Installation" (all fields optional) declared FIRST, "PushPayload" (a required field)
        // declared SECOND - a LinkedHashMap so schemas.entrySet() iterates in this exact order,
        // which is deliberately the wrong order for the union: without the ordering fix, the loose
        // schema would stay first, at real risk of capturing payloads meant for concrete types via
        // cloneWithType's first-match resolution. The generator must reorder it to last regardless
        // of input order.
        AsyncApiSchema installation = AsyncApiSchema.builder()
                .type("object")
                .properties(Map.of(
                        "id", AsyncApiSchema.builder().type("integer").build(),
                        "node_id", AsyncApiSchema.builder().type("string").build()))
                .build();
        AsyncApiSchema pushPayload = AsyncApiSchema.builder()
                .type("object")
                .required(List.of("ref"))
                .properties(Map.of("ref", AsyncApiSchema.builder().type("string").build()))
                .build();
        Map<String, AsyncApiSchema> schemas = new LinkedHashMap<>();
        schemas.put("Installation", installation);
        schemas.put("PushPayload", pushPayload);

        String result = new DataTypesGenerator(schemas, Optional.empty()).generate();

        int unionStart = result.indexOf("public type GenericDataType");
        Assert.assertTrue(unionStart >= 0, "Expected a GenericDataType union declaration: " + result);
        String unionDecl = result.substring(unionStart, result.indexOf(';', unionStart));
        Assert.assertTrue(unionDecl.indexOf("PushPayload") < unionDecl.indexOf("Installation"),
                "The loose 'Installation' schema must be ordered after the concrete 'PushPayload' "
                        + "schema in the union, regardless of input order: " + unionDecl);
    }

    @Test
    void testGenericDataTypeUnionKeepsRequiredFieldSchemasBeforeEachOtherInOriginalOrder() throws GeneratorException {
        // Two concrete (non-loose) schemas should keep their relative order - the ordering fix
        // should only ever move loose schemas to the end, not reorder everything else.
        AsyncApiSchema first = AsyncApiSchema.builder()
                .type("object")
                .required(List.of("a"))
                .properties(Map.of("a", AsyncApiSchema.builder().type("string").build()))
                .build();
        AsyncApiSchema second = AsyncApiSchema.builder()
                .type("object")
                .required(List.of("b"))
                .properties(Map.of("b", AsyncApiSchema.builder().type("string").build()))
                .build();
        Map<String, AsyncApiSchema> schemas = new LinkedHashMap<>();
        schemas.put("FirstPayload", first);
        schemas.put("SecondPayload", second);

        String result = new DataTypesGenerator(schemas, Optional.empty()).generate();

        int unionStart = result.indexOf("public type GenericDataType");
        String unionDecl = result.substring(unionStart, result.indexOf(';', unionStart));
        Assert.assertTrue(unionDecl.indexOf("FirstPayload") < unionDecl.indexOf("SecondPayload"),
                "Two concrete schemas should keep their original relative order: " + unionDecl);
    }
}
