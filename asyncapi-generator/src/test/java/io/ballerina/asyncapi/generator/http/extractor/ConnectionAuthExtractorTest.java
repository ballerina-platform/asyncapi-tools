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
package io.ballerina.asyncapi.generator.http.extractor;

import io.ballerina.asyncapi.core.AsyncApiParser;
import io.ballerina.asyncapi.core.AsyncApiParserException;
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.http.model.ConnectionAuthConfig;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Optional;

/**
 * Unit tests for {@link ConnectionAuthExtractor} covering all supported scheme/flow
 * combinations, the no-scheme case, and the unsupported-flow error branch.
 */
public class ConnectionAuthExtractorTest {

    private static final String PREFIX = "{\"asyncapi\":\"2.0.0\",\"info\":{\"title\":\"T\",\"version\":\"1\"}"
            + ",\"channels\":{}";

    @Test
    void testNoComponentsReturnsEmpty() throws AsyncApiParserException, GeneratorException {
        String json = PREFIX + "}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        Optional<ConnectionAuthConfig> result = new ConnectionAuthExtractor(spec).extract();
        Assert.assertTrue(result.isEmpty(), "Spec with no components should return empty");
    }

    @Test
    void testNoSecuritySchemesReturnsEmpty() throws AsyncApiParserException, GeneratorException {
        String json = PREFIX + ",\"components\":{\"schemas\":{}}}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        Optional<ConnectionAuthConfig> result = new ConnectionAuthExtractor(spec).extract();
        Assert.assertTrue(result.isEmpty(), "Spec with no securitySchemes should return empty");
    }

    @Test
    void testUnrelatedSchemeTypeReturnsEmpty() throws AsyncApiParserException, GeneratorException {
        String json = PREFIX + ",\"components\":{\"securitySchemes\":{\"apiKeyAuth\":"
                + "{\"type\":\"apiKey\",\"in\":\"header\",\"name\":\"X-Api-Key\"}}}}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        Optional<ConnectionAuthConfig> result = new ConnectionAuthExtractor(spec).extract();
        Assert.assertTrue(result.isEmpty(), "apiKey scheme type is out of scope and should return empty");
    }

    @Test
    void testUserPasswordScheme() throws AsyncApiParserException, GeneratorException {
        String json = PREFIX + ",\"components\":{\"securitySchemes\":{\"basicAuth\":"
                + "{\"type\":\"userPassword\"}}}}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        Optional<ConnectionAuthConfig> result = new ConnectionAuthExtractor(spec).extract();

        Assert.assertTrue(result.isPresent(), "userPassword scheme should be extracted");
        Assert.assertEquals(result.get().type(), ConnectionAuthConfig.TYPE_USER_PASSWORD);
        Assert.assertNull(result.get().flow(), "userPassword has no OAuth flow");
        Assert.assertNull(result.get().tokenUrl());
        Assert.assertNull(result.get().refreshUrl());
    }

    @Test
    void testMultipleRecognizedSchemesThrows() throws AsyncApiParserException {
        // Two recognized scheme entries in the same spec -- which one the trigger should
        // actually use is ambiguous, so this must fail loudly rather than silently picking
        // whichever one the map iterates to first.
        String json = PREFIX + ",\"components\":{\"securitySchemes\":{"
                + "\"basicAuth\":{\"type\":\"userPassword\"},"
                + "\"oauthAuth\":{\"type\":\"oauth2\",\"flows\":{\"clientCredentials\":"
                + "{\"tokenUrl\":\"https://example.com/token\","
                + "\"scopes\":{\"read\":\"Read access\"}}}}}}}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        try {
            new ConnectionAuthExtractor(spec).extract();
            Assert.fail("Expected GeneratorException for multiple recognized auth schemes");
        } catch (GeneratorException e) {
            Assert.assertTrue(e.getMessage().contains("Multiple outbound auth schemes"),
                    "Exception should identify the ambiguity: " + e.getMessage());
            Assert.assertTrue(e.getMessage().contains("basicAuth") && e.getMessage().contains("oauthAuth"),
                    "Exception should name both conflicting scheme keys: " + e.getMessage());
        }
    }

    @Test
    void testMultipleRecognizedSchemesIncludingNewTypesThrows() throws AsyncApiParserException {
        // Same ambiguity guard, but proving it generalizes to the two new types too, not just
        // the original oauth2/userPassword pair.
        String json = PREFIX + ",\"components\":{\"securitySchemes\":{"
                + "\"apiKeyAuth\":{\"type\":\"httpApiKey\",\"name\":\"X-API-Key\",\"in\":\"header\"},"
                + "\"mtlsAuth\":{\"type\":\"X509\"}}}}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        try {
            new ConnectionAuthExtractor(spec).extract();
            Assert.fail("Expected GeneratorException for multiple recognized auth schemes");
        } catch (GeneratorException e) {
            Assert.assertTrue(e.getMessage().contains("Multiple outbound auth schemes"),
                    "Exception should identify the ambiguity: " + e.getMessage());
            Assert.assertTrue(e.getMessage().contains("apiKeyAuth") && e.getMessage().contains("mtlsAuth"),
                    "Exception should name both conflicting scheme keys: " + e.getMessage());
        }
    }

    @Test
    void testHttpApiKeyHeaderScheme() throws AsyncApiParserException, GeneratorException {
        String json = PREFIX + ",\"components\":{\"securitySchemes\":{\"apiKeyAuth\":"
                + "{\"type\":\"httpApiKey\",\"name\":\"X-API-Key\",\"in\":\"header\"}}}}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        Optional<ConnectionAuthConfig> result = new ConnectionAuthExtractor(spec).extract();

        Assert.assertTrue(result.isPresent(), "httpApiKey scheme should be extracted");
        Assert.assertEquals(result.get().type(), ConnectionAuthConfig.TYPE_HTTP_API_KEY);
        Assert.assertEquals(result.get().apiKeyName(), "X-API-Key");
        Assert.assertEquals(result.get().apiKeyIn(), ConnectionAuthConfig.API_KEY_IN_HEADER);
        Assert.assertNull(result.get().flow(), "httpApiKey has no OAuth flow");
    }

    @Test
    void testHttpApiKeyQueryScheme() throws AsyncApiParserException, GeneratorException {
        String json = PREFIX + ",\"components\":{\"securitySchemes\":{\"apiKeyAuth\":"
                + "{\"type\":\"httpApiKey\",\"name\":\"api_key\",\"in\":\"query\"}}}}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        Optional<ConnectionAuthConfig> result = new ConnectionAuthExtractor(spec).extract();

        Assert.assertTrue(result.isPresent(), "httpApiKey scheme should be extracted");
        Assert.assertEquals(result.get().apiKeyName(), "api_key");
        Assert.assertEquals(result.get().apiKeyIn(), ConnectionAuthConfig.API_KEY_IN_QUERY);
    }

    @Test
    void testHttpApiKeyCookieScheme() throws AsyncApiParserException, GeneratorException {
        String json = PREFIX + ",\"components\":{\"securitySchemes\":{\"apiKeyAuth\":"
                + "{\"type\":\"httpApiKey\",\"name\":\"session_key\",\"in\":\"cookie\"}}}}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        Optional<ConnectionAuthConfig> result = new ConnectionAuthExtractor(spec).extract();

        Assert.assertTrue(result.isPresent(), "httpApiKey scheme should be extracted");
        Assert.assertEquals(result.get().apiKeyName(), "session_key");
        Assert.assertEquals(result.get().apiKeyIn(), ConnectionAuthConfig.API_KEY_IN_COOKIE);
    }

    @Test
    void testHttpApiKeyMissingNameThrows() throws AsyncApiParserException {
        // A name that's entirely absent is already rejected by the upstream AsyncAPI parser
        // itself ("API Key Security Scheme is missing a parameter name"), before this ever
        // reaches our extractor - so a spec-valid fixture can't omit the field outright (same
        // situation as testOAuth2ClientCredentialsMalformedTokenUrlThrows above). A blank string
        // satisfies the parser's presence check but still fails our own isBlank() guard, which is
        // the one way this branch is reachable through a real parsed spec.
        String json = PREFIX + ",\"components\":{\"securitySchemes\":{\"apiKeyAuth\":"
                + "{\"type\":\"httpApiKey\",\"name\":\"\",\"in\":\"header\"}}}}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        try {
            new ConnectionAuthExtractor(spec).extract();
            Assert.fail("Expected GeneratorException for httpApiKey scheme with a blank name");
        } catch (GeneratorException e) {
            Assert.assertTrue(e.getMessage().contains("'name'"),
                    "Exception should mention the missing name: " + e.getMessage());
        }
    }

    @Test
    void testHttpApiKeyInvalidInThrows() throws AsyncApiParserException {
        String json = PREFIX + ",\"components\":{\"securitySchemes\":{\"apiKeyAuth\":"
                + "{\"type\":\"httpApiKey\",\"name\":\"X-API-Key\",\"in\":\"body\"}}}}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        try {
            new ConnectionAuthExtractor(spec).extract();
            Assert.fail("Expected GeneratorException for httpApiKey scheme with an invalid 'in' value");
        } catch (GeneratorException e) {
            Assert.assertTrue(e.getMessage().contains("'in'"),
                    "Exception should mention the invalid in value: " + e.getMessage());
        }
    }

    @Test
    void testX509Scheme() throws AsyncApiParserException, GeneratorException {
        // AsyncAPI intentionally carries no certificate material in the spec itself -- the
        // scheme just declares that mutual TLS is required.
        String json = PREFIX + ",\"components\":{\"securitySchemes\":{\"mtlsAuth\":"
                + "{\"type\":\"X509\"}}}}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        Optional<ConnectionAuthConfig> result = new ConnectionAuthExtractor(spec).extract();

        Assert.assertTrue(result.isPresent(), "X509 scheme should be extracted");
        Assert.assertEquals(result.get().type(), ConnectionAuthConfig.TYPE_X509);
        Assert.assertNull(result.get().flow());
        Assert.assertNull(result.get().tokenUrl());
        Assert.assertNull(result.get().refreshUrl());
        Assert.assertNull(result.get().apiKeyName());
        Assert.assertNull(result.get().apiKeyIn());
    }

    @Test
    void testApiKeyTypeStaysUnrelatedDistinctFromHttpApiKey() throws AsyncApiParserException, GeneratorException {
        // apiKey (generic, non-HTTP) and httpApiKey (supported) are different `type` strings --
        // locking this in explicitly so a future "fix" can't silently conflate the two.
        String json = PREFIX + ",\"components\":{\"securitySchemes\":{\"apiKeyAuth\":"
                + "{\"type\":\"apiKey\",\"in\":\"user\"}}}}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        Optional<ConnectionAuthConfig> result = new ConnectionAuthExtractor(spec).extract();
        Assert.assertTrue(result.isEmpty(), "apiKey (not httpApiKey) is still out of scope");
    }

    @Test
    void testOAuth2AuthorizationCodeFlow() throws AsyncApiParserException, GeneratorException {
        String json = PREFIX + ",\"components\":{\"securitySchemes\":{\"oauthAuth\":"
                + "{\"type\":\"oauth2\",\"flows\":{\"authorizationCode\":"
                + "{\"authorizationUrl\":\"https://example.com/authorize\","
                + "\"tokenUrl\":\"https://example.com/token\","
                + "\"refreshUrl\":\"https://example.com/refresh\","
                + "\"scopes\":{\"read\":\"Read access\"}}}}}}}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        Optional<ConnectionAuthConfig> result = new ConnectionAuthExtractor(spec).extract();

        Assert.assertTrue(result.isPresent(), "oauth2 authorizationCode scheme should be extracted");
        Assert.assertEquals(result.get().type(), ConnectionAuthConfig.TYPE_OAUTH2);
        Assert.assertEquals(result.get().flow(), ConnectionAuthConfig.FLOW_AUTHORIZATION_CODE);
        Assert.assertEquals(result.get().refreshUrl(), "https://example.com/refresh");
        Assert.assertNull(result.get().tokenUrl(), "authorizationCode flow should not populate tokenUrl");
    }

    @Test
    void testOAuth2ClientCredentialsFlow() throws AsyncApiParserException, GeneratorException {
        String json = PREFIX + ",\"components\":{\"securitySchemes\":{\"oauthAuth\":"
                + "{\"type\":\"oauth2\",\"flows\":{\"clientCredentials\":"
                + "{\"tokenUrl\":\"https://example.com/token\","
                + "\"scopes\":{\"read\":\"Read access\"}}}}}}}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        Optional<ConnectionAuthConfig> result = new ConnectionAuthExtractor(spec).extract();

        Assert.assertTrue(result.isPresent(), "oauth2 clientCredentials scheme should be extracted");
        Assert.assertEquals(result.get().type(), ConnectionAuthConfig.TYPE_OAUTH2);
        Assert.assertEquals(result.get().flow(), ConnectionAuthConfig.FLOW_CLIENT_CREDENTIALS);
        Assert.assertEquals(result.get().tokenUrl(), "https://example.com/token");
        Assert.assertNull(result.get().refreshUrl(), "clientCredentials flow should not populate refreshUrl");
    }

    @Test
    void testOAuth2BothFlowsOnSameSchemeThrows() throws AsyncApiParserException {
        // A single oauth2 scheme declaring both flows is genuinely ambiguous for the generated
        // trigger's outbound calls - the spec has no way to say which flow this trigger should
        // actually use, so this must fail loudly rather than silently preferring
        // authorizationCode.
        String json = PREFIX + ",\"components\":{\"securitySchemes\":{\"oauthAuth\":"
                + "{\"type\":\"oauth2\",\"flows\":{"
                + "\"authorizationCode\":{\"authorizationUrl\":\"https://example.com/authorize\","
                + "\"tokenUrl\":\"https://example.com/token\","
                + "\"refreshUrl\":\"https://example.com/refresh\","
                + "\"scopes\":{\"read\":\"Read access\"}},"
                + "\"clientCredentials\":{\"tokenUrl\":\"https://example.com/token\","
                + "\"scopes\":{\"read\":\"Read access\"}}}}}}}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        try {
            new ConnectionAuthExtractor(spec).extract();
            Assert.fail("Expected GeneratorException for a scheme declaring both oauth2 flows");
        } catch (GeneratorException e) {
            Assert.assertTrue(e.getMessage().contains("authorizationCode")
                            && e.getMessage().contains("clientCredentials"),
                    "Exception should name both conflicting flows: " + e.getMessage());
        }
    }

    @Test
    void testOAuth2AuthorizationCodeMissingRefreshUrlThrows() throws AsyncApiParserException {
        // tokenUrl and scopes are required by the AsyncAPI spec itself for authorizationCode,
        // so the parser accepts this; refreshUrl is spec-optional, so its absence is only
        // caught by our own extractor, not by upstream spec validation.
        String json = PREFIX + ",\"components\":{\"securitySchemes\":{\"oauthAuth\":"
                + "{\"type\":\"oauth2\",\"flows\":{\"authorizationCode\":"
                + "{\"authorizationUrl\":\"https://example.com/authorize\","
                + "\"tokenUrl\":\"https://example.com/token\","
                + "\"scopes\":{\"read\":\"Read access\"}}}}}}}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        try {
            new ConnectionAuthExtractor(spec).extract();
            Assert.fail("Expected GeneratorException for authorizationCode flow missing refreshUrl");
        } catch (GeneratorException e) {
            Assert.assertTrue(e.getMessage().contains("refreshUrl"),
                    "Exception should mention the missing refreshUrl");
        }
    }

    @Test
    void testOAuth2ClientCredentialsMalformedTokenUrlThrows() throws AsyncApiParserException {
        // The AsyncAPI spec requires clientCredentials to declare a tokenUrl, so a spec-valid
        // fixture can never omit it outright (the parser itself rejects that). A malformed URI
        // string still satisfies the parser's string/presence check but fails java.net.URI
        // parsing, so it surfaces to our extractor as a null tokenUrl -- the one way this guard
        // is still reachable through a real parsed spec.
        String json = PREFIX + ",\"components\":{\"securitySchemes\":{\"oauthAuth\":"
                + "{\"type\":\"oauth2\",\"flows\":{\"clientCredentials\":"
                + "{\"tokenUrl\":\"not a valid uri\","
                + "\"scopes\":{\"read\":\"Read access\"}}}}}}}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        try {
            new ConnectionAuthExtractor(spec).extract();
            Assert.fail("Expected GeneratorException for clientCredentials flow with an unparsable tokenUrl");
        } catch (GeneratorException e) {
            Assert.assertTrue(e.getMessage().contains("tokenUrl"),
                    "Exception should mention the missing tokenUrl");
        }
    }

    @Test
    void testOAuth2UnsupportedFlowThrows() throws AsyncApiParserException {
        String json = PREFIX + ",\"components\":{\"securitySchemes\":{\"oauthAuth\":"
                + "{\"type\":\"oauth2\",\"flows\":{\"implicit\":"
                + "{\"authorizationUrl\":\"https://example.com/authorize\","
                + "\"scopes\":{\"read\":\"Read access\"}}}}}}}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        try {
            new ConnectionAuthExtractor(spec).extract();
            Assert.fail("Expected GeneratorException for unsupported oauth2 flow (implicit only)");
        } catch (GeneratorException e) {
            Assert.assertTrue(e.getMessage().contains("Unsupported oauth2 flow"),
                    "Exception should identify the flow as unsupported");
        }
    }
}
