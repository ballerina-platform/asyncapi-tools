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
    void testNoGenericDataTypeUnion() throws GeneratorException {
        // Payloads bind to each handler's own type, so nothing uses a union of them all.
        AsyncApiSchema pushPayload = AsyncApiSchema.builder()
                .type("object")
                .required(List.of("ref"))
                .properties(Map.of("ref", AsyncApiSchema.builder().type("string").build()))
                .build();
        Map<String, AsyncApiSchema> schemas = new LinkedHashMap<>();
        schemas.put("PushPayload", pushPayload);

        String result = new DataTypesGenerator(schemas, Optional.<WebhookAuthConfig>empty(),
                Optional.<ConnectionAuthConfig>empty()).generate();

        Assert.assertTrue(result.contains("public type PushPayload record"), result);
        Assert.assertFalse(result.contains("GenericDataType"), "No GenericDataType union expected: " + result);
    }

    @Test
    void testGenerateWrapsLongFieldDescriptionsWithinLineBudget() throws GeneratorException {
        // A real Shopify description that previously rendered as one ~210-character doc line.
        String longDescription = "The value of the discrepancy between the calculated refund and the actual "
                + "refund. If the kind property's value is shipping_refund, then amount returns the value of "
                + "shipping charges refunded to the customer.";
        AsyncApiSchema schema = AsyncApiSchema.builder()
                .type("object")
                .properties(Map.of("amount", AsyncApiSchema.builder()
                        .type("string")
                        .description(longDescription)
                        .build()))
                .build();

        String result = new DataTypesGenerator(Map.of("OrderAdjustment", schema),
                Optional.<WebhookAuthConfig>empty(), Optional.<ConnectionAuthConfig>empty()).generate();

        for (String line : result.split("\n")) {
            Assert.assertTrue(line.stripTrailing().length() <= 120,
                    "Generated line exceeds the 120-character budget (" + line.stripTrailing().length()
                            + " chars): " + line);
        }
        Assert.assertTrue(result.contains("# The value of the discrepancy"),
                "The description should still be emitted as a doc comment: " + result);
        Assert.assertTrue(result.lines().filter(line -> line.stripLeading().startsWith("#")).count() > 1,
                "A description this long should be split across several doc-comment lines: " + result);
    }

    @Test
    void testGenerateWrapsLongListenerConfigDescriptionWithinLineBudget() throws GeneratorException {
        // ListenerConfig field docs are built from the spec too (here, an API key header name), so they wrap the same.
        String longHeaderName = "header part ".repeat(20).stripTrailing();
        ConnectionAuthConfig apiKey = new ConnectionAuthConfig(ConnectionAuthConfig.TYPE_HTTP_API_KEY,
                null, null, null, longHeaderName, ConnectionAuthConfig.API_KEY_IN_HEADER);
        AsyncApiSchema schema = AsyncApiSchema.builder().type("object").build();

        String result = new DataTypesGenerator(Map.of("Event", schema),
                Optional.<WebhookAuthConfig>empty(), Optional.of(apiKey)).generate();

        for (String line : result.split("\n")) {
            Assert.assertTrue(line.stripTrailing().length() <= 120,
                    "Generated line exceeds the 120-character budget (" + line.stripTrailing().length()
                            + " chars): " + line);
        }
        Assert.assertTrue(result.contains("# API key sent as the 'header part"),
                "The description should still be emitted as a doc comment: " + result);
        Assert.assertTrue(result.lines().filter(line -> line.stripLeading().startsWith("# header part")).count() > 0,
                "The long API key description should continue on further doc-comment lines: " + result);
    }
}
