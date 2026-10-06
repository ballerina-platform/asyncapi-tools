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
import io.ballerina.asyncapi.generator.http.model.ConnectionAuthConfig;
import io.ballerina.asyncapi.generator.http.model.WebhookAuthConfig;
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
        String result = new DataTypesGenerator(Map.of("LeadEvent", schema),
                Optional.empty(), Optional.empty()).generate();

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
        String result = new DataTypesGenerator(Map.of("LeadEvent", schema),
                Optional.empty(), Optional.empty()).generate();

        Assert.assertTrue(result.contains("import ballerina/http;"),
                "The 'X-Test-Header' field name requires @http:Header, so the http import "
                        + "must be generated: " + result);
        Assert.assertTrue(result.contains("@http:Header"), "The hyphenated field should get an @http:Header "
                + "annotation: " + result);
    }

    @Test
    void testUriPropertyNameBecomesACleanFieldCarryingItsUri() throws GeneratorException {
        // WSO2 Identity Platform events are keyed by URI. A URI contains a hyphen, so it used to be taken
        // for an HTTP header name and emitted verbatim as a field identifier, which does not parse.
        String uri = "https://schemas.identity.wso2.org/events/token/event-type/accessTokenIssued";
        AsyncApiSchema schema = AsyncApiSchema.builder()
                .type("object")
                .required(List.of(uri))
                .properties(Map.of(uri, AsyncApiSchema.builder().type("object").build()))
                .build();
        String result = new DataTypesGenerator(Map.of("EventData", schema),
                Optional.empty(), Optional.empty()).generate();

        Assert.assertTrue(result.contains("accessTokenIssued"),
                "The field should be named after the last segment of the URI: " + result);
        Assert.assertTrue(result.contains("@jsondata:Name {value: \"" + uri + "\"}")
                        || result.contains("@jsondata:Name { value: \"" + uri + "\" }"),
                "The field must keep the full URI as its JSON key: " + result);
        Assert.assertFalse(result.contains("@http:Header"),
                "A URI is not an HTTP header name: " + result);
        Assert.assertFalse(result.contains("import ballerina/http;"),
                "Without a header annotation the http import would be unused: " + result);
        Assert.assertTrue(result.contains("import ballerina/data.jsondata;"),
                "The annotation requires the data.jsondata import: " + result);
        Assert.assertFalse(result.matches("(?s).*[A-Za-z]+ https://.*"),
                "The URI must not be emitted as a field identifier: " + result);
    }

    @Test
    void testUriNamesThatDeriveTheSameIdentifierStayDistinctAndValid() throws GeneratorException {
        // Two URIs ending in the same segment derive the same name; the second must not duplicate it.
        String first = "https://a.example.org/events/login/loginSuccess";
        String second = "https://b.example.org/events/login/loginSuccess";
        Map<String, AsyncApiSchema> properties = new LinkedHashMap<>();
        properties.put(first, AsyncApiSchema.builder().type("string").build());
        properties.put(second, AsyncApiSchema.builder().type("string").build());
        AsyncApiSchema schema = AsyncApiSchema.builder().type("object").properties(properties).build();

        String result = new DataTypesGenerator(Map.of("EventData", schema),
                Optional.empty(), Optional.empty()).generate();

        long loginSuccessFields = result.lines().filter(line -> line.matches("\\s*string loginSuccess\\??;")).count();
        Assert.assertEquals(loginSuccessFields, 1L,
                "Exactly one field may take the derived name, or the record would not compile: " + result);
        Assert.assertFalse(result.contains("string https://"),
                "The other URI must fall back to an escaped identifier, never a raw one: " + result);
    }

    @Test
    void testDerivedNameThatIsAKeywordFallsBackToTheRawSpelling() throws GeneratorException {
        // "import" is a Ballerina keyword; a field cannot take it as a derived name.
        String uri = "https://example.org/events/import";
        AsyncApiSchema schema = AsyncApiSchema.builder()
                .type("object")
                .properties(Map.of(uri, AsyncApiSchema.builder().type("string").build()))
                .build();
        String result = new DataTypesGenerator(Map.of("EventData", schema),
                Optional.empty(), Optional.empty()).generate();

        Assert.assertFalse(result.contains("string import;"),
                "A keyword cannot be used as a field name: " + result);
        Assert.assertFalse(result.contains("string https://"),
                "The raw URI must still be escaped, not emitted as written: " + result);
    }

    @Test
    void testHeaderStyleNamesKeepTheirHeaderAnnotation() throws GeneratorException {
        // The narrower URI rule must not change how real header names are generated.
        AsyncApiSchema schema = AsyncApiSchema.builder()
                .type("object")
                .properties(Map.of("X-GitHub-Event", AsyncApiSchema.builder().type("string").build()))
                .build();
        String result = new DataTypesGenerator(Map.of("WebhookHeaders", schema),
                Optional.empty(), Optional.empty()).generate();

        Assert.assertTrue(result.contains("@http:Header {name: \"X-GitHub-Event\"}")
                        || result.contains("@http:Header { name: \"X-GitHub-Event\" }"),
                "A hyphenated header name keeps @http:Header: " + result);
        Assert.assertTrue(result.contains("xGitHubEvent"), "and its camelCase field name: " + result);
    }

    @Test
    void testGenerateWithNoPropertiesOmitsHttpImport() throws GeneratorException {
        // A schema with no properties at all has nothing that could ever need @http:Header.
        AsyncApiSchema schema = AsyncApiSchema.builder().type("object").build();
        String result = new DataTypesGenerator(Map.of("EmptyEvent", schema),
                Optional.empty(), Optional.empty()).generate();

        Assert.assertFalse(result.contains("import ballerina/http;"),
                "A schema with no properties can't need @http:Header: " + result);
    }

    @Test
    void testSnakeCaseFieldsAreCamelCasedAndAnnotatedWithTheirJsonKey() throws GeneratorException {
        AsyncApiSchema schema = AsyncApiSchema.builder()
                .type("object")
                .properties(Map.of(
                        "country_code", AsyncApiSchema.builder().type("string").build(),
                        "id", AsyncApiSchema.builder().type("integer").build()))
                .build();
        String result = new DataTypesGenerator(Map.of("Address", schema),
                Optional.<WebhookAuthConfig>empty(), Optional.<ConnectionAuthConfig>empty()).generate();

        Assert.assertTrue(result.contains("countryCode"),
                "A snake_case property should be emitted as a camelCase field: " + result);
        Assert.assertTrue(result.contains("@jsondata:Name {value: \"country_code\"}")
                        || result.contains("@jsondata:Name { value: \"country_code\" }"),
                "The renamed field must carry its original JSON key: " + result);
        Assert.assertTrue(result.contains("import ballerina/data.jsondata;"),
                "The annotation requires the data.jsondata import: " + result);
        Assert.assertFalse(result.contains("string country_code"),
                "The raw snake_case field name should no longer be emitted: " + result);
    }

    @Test
    void testAlreadyCamelCaseFieldsAreLeftAloneAndNeedNoImport() throws GeneratorException {
        AsyncApiSchema schema = AsyncApiSchema.builder()
                .type("object")
                .properties(Map.of(
                        "eventId", AsyncApiSchema.builder().type("string").build(),
                        "id", AsyncApiSchema.builder().type("integer").build()))
                .build();
        String result = new DataTypesGenerator(Map.of("LeadEvent", schema),
                Optional.<WebhookAuthConfig>empty(), Optional.<ConnectionAuthConfig>empty()).generate();

        Assert.assertFalse(result.contains("@jsondata:Name"),
                "A field that needs no rename should carry no annotation: " + result);
        Assert.assertFalse(result.contains("import ballerina/data.jsondata;"),
                "Ballerina treats an unused import as a compile error, so it must be omitted: " + result);
    }

    @Test
    void testCollidingCamelCaseNameKeepsItsRawSpellingInsteadOfDuplicating() throws GeneratorException {
        // "first_name" camelCases to "firstName", which this schema already declares - emitting
        // both would be a duplicate-field compile error, so the snake_case one keeps its raw name
        // (which binds correctly with no annotation).
        Map<String, AsyncApiSchema> properties = new LinkedHashMap<>();
        properties.put("firstName", AsyncApiSchema.builder().type("string").build());
        properties.put("first_name", AsyncApiSchema.builder().type("string").build());
        AsyncApiSchema schema = AsyncApiSchema.builder().type("object").properties(properties).build();

        String result = new DataTypesGenerator(Map.of("Customer", schema),
                Optional.<WebhookAuthConfig>empty(), Optional.<ConnectionAuthConfig>empty()).generate();

        Assert.assertTrue(result.contains("first_name"),
                "The colliding property should keep its raw spelling: " + result);
        int firstNameFields = result.split("firstName", -1).length - 1;
        Assert.assertEquals(firstNameFields, 1,
                "Exactly one 'firstName' field should be emitted, not a duplicate pair: " + result);
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

        String result = new DataTypesGenerator(schemas, Optional.<WebhookAuthConfig>empty(),
                Optional.<ConnectionAuthConfig>empty()).generate();

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

        String result = new DataTypesGenerator(schemas, Optional.<WebhookAuthConfig>empty(),
                Optional.<ConnectionAuthConfig>empty()).generate();

        int unionStart = result.indexOf("public type GenericDataType");
        String unionDecl = result.substring(unionStart, result.indexOf(';', unionStart));
        Assert.assertTrue(unionDecl.indexOf("FirstPayload") < unionDecl.indexOf("SecondPayload"),
                "Two concrete schemas should keep their original relative order: " + unionDecl);
    }
}
