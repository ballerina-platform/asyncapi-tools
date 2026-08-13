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
package io.ballerina.asyncapi.generator.http.node;

import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.http.model.ConnectionAuthConfig;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Optional;

/**
 * Unit tests for {@link GenerateListenerConfigNode}, asserting directly on the generated node's
 * source text ({@link TypeDefinitionNode#toString()}) rather than round-tripping through the
 * parser/formatter - the cheapest way to exercise every {@code connectionAuthFieldNames}/
 * {@code connectionAuthFields} branch, including the doc-comment-vs-{@code @display} retrofit.
 */
public class GenerateListenerConfigNodeTest {

    @Test
    void testGenerateNoConnectionAuthWebhookSecretDocCommentOnly() throws GeneratorException {
        String result = GenerateListenerConfigNode.generate(List.of(), Optional.empty()).toString();
        Assert.assertTrue(result.contains("#Webhook Secret"),
                "webhookSecret field should carry a doc comment: " + result);
        Assert.assertFalse(result.contains("@display"),
                "No field should use a @display annotation anymore: " + result);
    }

    @Test
    void testGenerateUserPasswordDocCommentNotDisplay() throws GeneratorException {
        ConnectionAuthConfig config = new ConnectionAuthConfig(
                ConnectionAuthConfig.TYPE_USER_PASSWORD, null, null, null);
        String result = GenerateListenerConfigNode.generate(List.of(), Optional.of(config)).toString();

        Assert.assertTrue(result.contains("username"), "Should declare a username field: " + result);
        Assert.assertTrue(result.contains("password"), "Should declare a password field: " + result);
        Assert.assertTrue(result.contains("#Username"), "username field should have a doc comment: " + result);
        Assert.assertTrue(result.contains("#Password"), "password field should have a doc comment: " + result);
        Assert.assertFalse(result.contains("@display"),
                "Retrofit: userPassword fields must not use @display anymore: " + result);
    }

    @Test
    void testGenerateOAuth2AuthorizationCodeStillWorks() throws GeneratorException {
        ConnectionAuthConfig config = new ConnectionAuthConfig(ConnectionAuthConfig.TYPE_OAUTH2,
                ConnectionAuthConfig.FLOW_AUTHORIZATION_CODE, null, "https://example.com/refresh");
        String result = GenerateListenerConfigNode.generate(List.of(), Optional.of(config)).toString();

        Assert.assertTrue(result.contains("clientId"), "Should declare a clientId field: " + result);
        Assert.assertTrue(result.contains("clientSecret"), "Should declare a clientSecret field: " + result);
        Assert.assertTrue(result.contains("refreshUrl"), "Should declare a refreshUrl field: " + result);
        Assert.assertTrue(result.contains("refreshToken"), "Should declare a refreshToken field: " + result);
        Assert.assertTrue(result.contains("https://example.com/refresh"),
                "refreshUrl should default to the spec's value: " + result);
        Assert.assertFalse(result.contains("@display"), "Retrofit regression: " + result);
    }

    @Test
    void testGenerateOAuth2ClientCredentialsStillWorks() throws GeneratorException {
        ConnectionAuthConfig config = new ConnectionAuthConfig(ConnectionAuthConfig.TYPE_OAUTH2,
                ConnectionAuthConfig.FLOW_CLIENT_CREDENTIALS, "https://example.com/token", null);
        String result = GenerateListenerConfigNode.generate(List.of(), Optional.of(config)).toString();

        Assert.assertTrue(result.contains("clientId"), "Should declare a clientId field: " + result);
        Assert.assertTrue(result.contains("clientSecret"), "Should declare a clientSecret field: " + result);
        Assert.assertTrue(result.contains("tokenUrl"), "Should declare a tokenUrl field: " + result);
        Assert.assertFalse(result.contains("refreshToken"),
                "clientCredentials flow should not generate a refreshToken field: " + result);
        Assert.assertFalse(result.contains("@display"), "Retrofit regression: " + result);
    }

    @Test
    void testGenerateTokenUrlWithQuoteIsEscaped() throws GeneratorException {
        // A tokenUrl/refreshUrl containing a literal '"' must not be embedded raw into the
        // generated string literal - doing so would prematurely close it and produce malformed
        // generated source. Contrived input, but proves the escape actually runs.
        ConnectionAuthConfig config = new ConnectionAuthConfig(ConnectionAuthConfig.TYPE_OAUTH2,
                ConnectionAuthConfig.FLOW_CLIENT_CREDENTIALS, "https://example.com/token?q=\"quoted\"", null);
        String result = GenerateListenerConfigNode.generate(List.of(), Optional.of(config)).toString();

        Assert.assertTrue(result.contains("https://example.com/token?q=\\\"quoted\\\""),
                "Embedded quote should be escaped, not left raw: " + result);
        Assert.assertFalse(result.contains("q=\"quoted\"\";"),
                "Unescaped quote would prematurely close the string literal: " + result);
    }

    @Test
    void testGenerateHttpApiKeyHeaderDynamicDescription() throws GeneratorException {
        ConnectionAuthConfig config = new ConnectionAuthConfig(ConnectionAuthConfig.TYPE_HTTP_API_KEY,
                null, null, null, "X-API-Key", ConnectionAuthConfig.API_KEY_IN_HEADER);
        String result = GenerateListenerConfigNode.generate(List.of(), Optional.of(config)).toString();

        Assert.assertTrue(result.contains("apiKeyValue"), "Should declare an apiKeyValue field: " + result);
        Assert.assertTrue(result.contains("#API key sent as the 'X-API-Key' HTTP header"),
                "Doc comment should name the header and location: " + result);
        Assert.assertFalse(result.contains("@display"), "httpApiKey field must use a doc comment: " + result);
    }

    @Test
    void testGenerateHttpApiKeyQueryDynamicDescription() throws GeneratorException {
        ConnectionAuthConfig config = new ConnectionAuthConfig(ConnectionAuthConfig.TYPE_HTTP_API_KEY,
                null, null, null, "api_key", ConnectionAuthConfig.API_KEY_IN_QUERY);
        String result = GenerateListenerConfigNode.generate(List.of(), Optional.of(config)).toString();

        Assert.assertTrue(result.contains("#API key sent as the 'api_key' HTTP query"),
                "Doc comment text should be dynamic, not hardcoded to 'header': " + result);
    }

    @Test
    void testGenerateHttpApiKeyCookieDynamicDescription() throws GeneratorException {
        ConnectionAuthConfig config = new ConnectionAuthConfig(ConnectionAuthConfig.TYPE_HTTP_API_KEY,
                null, null, null, "session_key", ConnectionAuthConfig.API_KEY_IN_COOKIE);
        String result = GenerateListenerConfigNode.generate(List.of(), Optional.of(config)).toString();

        Assert.assertTrue(result.contains("#API key sent as the 'session_key' HTTP cookie"),
                "Doc comment text should be dynamic, not hardcoded to 'header': " + result);
    }

    @Test
    void testGenerateX509UnionFields() throws GeneratorException {
        ConnectionAuthConfig config = new ConnectionAuthConfig(ConnectionAuthConfig.TYPE_X509, null, null, null);
        String result = GenerateListenerConfigNode.generate(List.of(), Optional.of(config)).toString();

        Assert.assertTrue(result.contains("crypto:TrustStore"),
                "cert field should reference crypto:TrustStore: " + result);
        Assert.assertTrue(result.contains("cert"), "Should declare a cert field: " + result);
        Assert.assertTrue(result.contains("crypto:KeyStore"),
                "keyConfig field should reference crypto:KeyStore: " + result);
        Assert.assertTrue(result.contains("http:CertKey"),
                "keyConfig field should reference http:CertKey: " + result);
        Assert.assertTrue(result.contains("keyConfig"), "Should declare a keyConfig field: " + result);
        Assert.assertTrue(result.contains("#Client certificate for mutual TLS"),
                "cert field should have a doc comment: " + result);
        Assert.assertTrue(result.contains("#Client private key for mutual TLS"),
                "keyConfig field should have a doc comment: " + result);
        Assert.assertFalse(result.contains("@display"), "X509 fields must use doc comments: " + result);
    }

    @Test
    void testGenerateHttpApiKeyValueCollidesWithExtraConfigFieldThrows() {
        ConnectionAuthConfig config = new ConnectionAuthConfig(ConnectionAuthConfig.TYPE_HTTP_API_KEY,
                null, null, null, "X-API-Key", ConnectionAuthConfig.API_KEY_IN_HEADER);
        try {
            GenerateListenerConfigNode.generate(List.of("apiKeyValue"), Optional.of(config));
            Assert.fail("Expected GeneratorException for apiKeyValue field name collision");
        } catch (GeneratorException e) {
            Assert.assertTrue(e.getMessage().contains("apiKeyValue"),
                    "Exception should name the colliding field: " + e.getMessage());
        }
    }

    @Test
    void testGenerateX509CertCollidesWithExtraConfigFieldThrows() {
        ConnectionAuthConfig config = new ConnectionAuthConfig(ConnectionAuthConfig.TYPE_X509, null, null, null);
        try {
            GenerateListenerConfigNode.generate(List.of("cert"), Optional.of(config));
            Assert.fail("Expected GeneratorException for cert field name collision");
        } catch (GeneratorException e) {
            Assert.assertTrue(e.getMessage().contains("cert"),
                    "Exception should name the colliding field: " + e.getMessage());
        }
    }

    @Test
    void testGenerateUnrecognizedTypeFlowThrows() {
        // oauth2 with no flow set at all doesn't match either the authorizationCode or
        // clientCredentials branch inside connectionAuthFieldNames - the final throw at the
        // bottom of that method, otherwise untested even for the 3 pre-existing mechanisms.
        ConnectionAuthConfig config = new ConnectionAuthConfig(ConnectionAuthConfig.TYPE_OAUTH2, null, null, null);
        try {
            GenerateListenerConfigNode.generate(List.of(), Optional.of(config));
            Assert.fail("Expected GeneratorException for an unrecognized type/flow combination");
        } catch (GeneratorException e) {
            Assert.assertTrue(e.getMessage().contains("Unrecognized connection auth type/flow"),
                    "Exception should identify the combination as unrecognized: " + e.getMessage());
        }
    }
}
