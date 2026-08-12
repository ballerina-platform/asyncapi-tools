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
import io.ballerina.asyncapi.generator.http.model.WebhookAuthConfig;
import org.testng.Assert;
import org.testng.annotations.Test;

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
}
