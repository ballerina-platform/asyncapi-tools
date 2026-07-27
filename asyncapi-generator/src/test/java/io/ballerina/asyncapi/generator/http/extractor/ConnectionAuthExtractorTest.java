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
