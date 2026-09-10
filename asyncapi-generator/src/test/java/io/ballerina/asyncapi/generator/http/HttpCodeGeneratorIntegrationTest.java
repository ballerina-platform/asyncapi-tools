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
package io.ballerina.asyncapi.generator.http;

import io.ballerina.asyncapi.core.AsyncApiParser;
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.generator.GeneratorException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Integration tests for {@link HttpCodeGenerator} that parse real AsyncAPI JSON spec fixtures
 * and verify the four generated Ballerina source files.
 */
public class HttpCodeGeneratorIntegrationTest {

    @Test
    void testGenerateWithBodyIdentifierMultipleServiceTypes() throws Exception {
        Path spec = specPath("github.json");
        Path outDir = Files.createTempDirectory("github-gen");
        try {
            AsyncApiSpec asyncApiSpec = AsyncApiParser.parseFromJsonString(Files.readString(spec));
            new HttpCodeGenerator(asyncApiSpec).generate(outDir);

            String serviceTypes = readFile(outDir, "service_types.bal");
            String types = readFile(outDir, "data_types.bal");
            String listener = readFile(outDir, "listener.bal");
            String dispatcher = readFile(outDir, "dispatcher_service.bal");

            Assert.assertTrue(serviceTypes.contains("RepositoryService"),
                    "service_types.bal should contain RepositoryService");
            Assert.assertTrue(serviceTypes.contains("IssueService"),
                    "service_types.bal should contain IssueService");
            Assert.assertTrue(serviceTypes.contains("DeploymentService"),
                    "service_types.bal should contain DeploymentService");
            Assert.assertTrue(serviceTypes.contains("GenericServiceType"),
                    "service_types.bal should contain GenericServiceType union");
            Assert.assertTrue(serviceTypes.contains("onPush"),
                    "service_types.bal should contain onPush remote function");

            Assert.assertTrue(types.contains("PushEvent"),
                    "data_types.bal should contain PushEvent schema type");
            Assert.assertTrue(types.contains("ListenerConfig"),
                    "data_types.bal should always contain ListenerConfig");
            Assert.assertTrue(types.contains("GenericDataType"),
                    "data_types.bal should contain GenericDataType union");

            Assert.assertTrue(listener.contains("Listener"),
                    "listener.bal should contain the Listener class");
            Assert.assertTrue(listener.contains("getServiceTypeStr"),
                    "listener.bal should contain getServiceTypeStr method");

            Assert.assertTrue(dispatcher.contains("DispatcherService"),
                    "dispatcher_service.bal should contain DispatcherService class");
            Assert.assertTrue(dispatcher.contains("matchRemoteFunc"),
                    "dispatcher_service.bal should contain matchRemoteFunc method");
            Assert.assertTrue(dispatcher.contains("event.'type"),
                    "dispatcher_service.bal should reference the keyword-escaped body identifier path");
        } finally {
            deleteDir(outDir);
        }
    }

    @Test
    void testGenerateWithHeaderIdentifier() throws Exception {
        Path spec = specPath("stripe.json");
        Path outDir = Files.createTempDirectory("stripe-gen");
        try {
            AsyncApiSpec asyncApiSpec = AsyncApiParser.parseFromJsonString(Files.readString(spec));
            new HttpCodeGenerator(asyncApiSpec).generate(outDir);

            String serviceTypes = readFile(outDir, "service_types.bal");
            String dispatcher = readFile(outDir, "dispatcher_service.bal");
            String listener = readFile(outDir, "listener.bal");
            String types = readFile(outDir, "data_types.bal");

            Assert.assertTrue(serviceTypes.contains("PaymentService"),
                    "service_types.bal should contain PaymentService");
            Assert.assertTrue(serviceTypes.contains("CustomerService"),
                    "service_types.bal should contain CustomerService");
            Assert.assertTrue(serviceTypes.contains("GenericServiceType"),
                    "service_types.bal should contain GenericServiceType union");

            Assert.assertFalse(dispatcher.isBlank(),
                    "dispatcher_service.bal should not be blank");
            Assert.assertTrue(dispatcher.contains("DispatcherService"),
                    "dispatcher_service.bal should contain DispatcherService");

            Assert.assertTrue(listener.contains("Listener"),
                    "listener.bal should contain the Listener class");
            Assert.assertTrue(types.contains("PaymentIntent"),
                    "data_types.bal should contain PaymentIntent schema type");
            Assert.assertTrue(types.contains("Customer"),
                    "data_types.bal should contain Customer schema type");
        } finally {
            deleteDir(outDir);
        }
    }

    @Test
    void testGenerateWithInlineSchema() throws Exception {
        Path spec = specPath("sendgrid_minimal.json");
        Path outDir = Files.createTempDirectory("sendgrid-gen");
        try {
            AsyncApiSpec asyncApiSpec = AsyncApiParser.parseFromJsonString(Files.readString(spec));
            new HttpCodeGenerator(asyncApiSpec).generate(outDir);

            String serviceTypes = readFile(outDir, "service_types.bal");
            String types = readFile(outDir, "data_types.bal");
            String listener = readFile(outDir, "listener.bal");

            Assert.assertTrue(serviceTypes.contains("EmailService"),
                    "service_types.bal should contain EmailService from channel 'email'");
            Assert.assertTrue(serviceTypes.contains("onEmailDelivered"),
                    "service_types.bal should contain onEmailDelivered remote function");
            Assert.assertTrue(serviceTypes.contains("GenericServiceType"),
                    "service_types.bal should contain GenericServiceType union");

            Assert.assertTrue(types.contains("ListenerConfig"),
                    "data_types.bal should contain ListenerConfig");
            Assert.assertFalse(types.isBlank(), "data_types.bal should not be blank");

            Assert.assertTrue(listener.contains("Listener"),
                    "listener.bal should contain the Listener class");
        } finally {
            deleteDir(outDir);
        }
    }

    @Test
    void testGenerateWithNullOutputPath() throws Exception {
        Path spec = specPath("sendgrid_minimal.json");
        AsyncApiSpec asyncApiSpec = AsyncApiParser.parseFromJsonString(Files.readString(spec));
        try {
            new HttpCodeGenerator(asyncApiSpec).generate(null);
        } finally {
            List<String> generatedFiles = List.of(
                    "data_types.bal", "service_types.bal", "listener.bal", "dispatcher_service.bal");
            for (String file : generatedFiles) {
                Files.deleteIfExists(Path.of(file));
            }
        }
    }

    @Test
    void testGenerateWithOAuth2AuthorizationCodeConnectionAuth() throws Exception {
        Path spec = specPath("connection_auth_oauth2_authcode.json");
        Path outDir = Files.createTempDirectory("oauth2-authcode-gen");
        try {
            AsyncApiSpec asyncApiSpec = AsyncApiParser.parseFromJsonString(Files.readString(spec));
            new HttpCodeGenerator(asyncApiSpec).generate(outDir);

            String types = readFile(outDir, "data_types.bal");
            Assert.assertTrue(
                    types.contains("refreshUrl = \"https://oauth2.googleapis.com/token\""),
                    "ListenerConfig should declare a refreshUrl field defaulted to the spec's refreshUrl");
            Assert.assertTrue(types.contains("clientId"), "ListenerConfig should declare a clientId field");
            Assert.assertTrue(types.contains("clientSecret"), "ListenerConfig should declare a clientSecret field");
            Assert.assertTrue(types.contains("refreshToken"), "ListenerConfig should declare a refreshToken field");
            Assert.assertFalse(types.contains("username"),
                    "authorizationCode flow should not generate username/password fields");
        } finally {
            deleteDir(outDir);
        }
    }

    @Test
    void testGenerateWithOAuth2ClientCredentialsConnectionAuth() throws Exception {
        Path spec = specPath("connection_auth_oauth2_clientcreds.json");
        Path outDir = Files.createTempDirectory("oauth2-clientcreds-gen");
        try {
            AsyncApiSpec asyncApiSpec = AsyncApiParser.parseFromJsonString(Files.readString(spec));
            new HttpCodeGenerator(asyncApiSpec).generate(outDir);

            String types = readFile(outDir, "data_types.bal");
            Assert.assertTrue(
                    types.contains("tokenUrl = \"https://api.example.com/oauth/token\""),
                    "ListenerConfig should declare a tokenUrl field defaulted to the spec's tokenUrl");
            Assert.assertTrue(types.contains("clientId"), "ListenerConfig should declare a clientId field");
            Assert.assertTrue(types.contains("clientSecret"), "ListenerConfig should declare a clientSecret field");
            Assert.assertFalse(types.contains("refreshToken"),
                    "clientCredentials flow should not generate a refreshToken field");
        } finally {
            deleteDir(outDir);
        }
    }

    @Test
    void testGenerateWithUserPasswordConnectionAuth() throws Exception {
        Path spec = specPath("connection_auth_userpassword.json");
        Path outDir = Files.createTempDirectory("userpassword-gen");
        try {
            AsyncApiSpec asyncApiSpec = AsyncApiParser.parseFromJsonString(Files.readString(spec));
            new HttpCodeGenerator(asyncApiSpec).generate(outDir);

            String types = readFile(outDir, "data_types.bal");
            Assert.assertFalse(types.contains("refreshUrl") || types.contains("tokenUrl"),
                    "userPassword has no token/refresh endpoint, so no URL field should be generated");
            Assert.assertTrue(types.contains("username"), "ListenerConfig should declare a username field");
            Assert.assertTrue(types.contains("password"), "ListenerConfig should declare a password field");
            Assert.assertFalse(types.contains("clientId"),
                    "userPassword should not generate OAuth2 client fields");
        } finally {
            deleteDir(outDir);
        }
    }

    @Test
    void testGenerateWithHttpApiKeyConnectionAuth() throws Exception {
        Path spec = specPath("connection_auth_httpapikey.json");
        Path outDir = Files.createTempDirectory("httpapikey-gen");
        try {
            AsyncApiSpec asyncApiSpec = AsyncApiParser.parseFromJsonString(Files.readString(spec));
            new HttpCodeGenerator(asyncApiSpec).generate(outDir);

            String types = readFile(outDir, "data_types.bal");
            Assert.assertTrue(types.contains("apiKeyValue"), "ListenerConfig should declare an apiKeyValue field");
            Assert.assertTrue(types.contains("API key sent as the 'X-API-Key' HTTP header"),
                    "Field doc comment should name the header and location");
            Assert.assertFalse(types.contains("@display"), "Generated fields should use doc comments, not @display");
        } finally {
            deleteDir(outDir);
        }
    }

    @Test
    void testGenerateWithX509ConnectionAuth() throws Exception {
        Path spec = specPath("connection_auth_x509.json");
        Path outDir = Files.createTempDirectory("x509-gen");
        try {
            AsyncApiSpec asyncApiSpec = AsyncApiParser.parseFromJsonString(Files.readString(spec));
            new HttpCodeGenerator(asyncApiSpec).generate(outDir);

            String types = readFile(outDir, "data_types.bal");
            Assert.assertTrue(types.contains("crypto:TrustStore|string cert"),
                    "ListenerConfig should declare a crypto:TrustStore|string cert field");
            Assert.assertTrue(types.contains("crypto:KeyStore|http:CertKey keyConfig"),
                    "ListenerConfig should declare a crypto:KeyStore|http:CertKey keyConfig field");
            Assert.assertTrue(types.contains("import ballerina/crypto;"),
                    "data_types.bal should import ballerina/crypto for the X509 union fields");
            Assert.assertFalse(types.contains("@display"), "Generated fields should use doc comments, not @display");
        } finally {
            deleteDir(outDir);
        }
    }

    @Test
    void testGenerateWithoutX509OmitsCryptoImport() throws Exception {
        // Regression check for the conditional crypto import's false branch -- without this,
        // replacing the `if` in DataTypesGenerator with an unconditional import would go
        // unnoticed by every other test here (they only check for the import's presence).
        Path spec = specPath("connection_auth_userpassword.json");
        Path outDir = Files.createTempDirectory("no-x509-gen");
        try {
            AsyncApiSpec asyncApiSpec = AsyncApiParser.parseFromJsonString(Files.readString(spec));
            new HttpCodeGenerator(asyncApiSpec).generate(outDir);

            String types = readFile(outDir, "data_types.bal");
            Assert.assertFalse(types.contains("import ballerina/crypto;"),
                    "data_types.bal should not import ballerina/crypto unless X509 is in use");
        } finally {
            deleteDir(outDir);
        }
    }

    @Test
    void testGenerateWithNoSecuritySchemesOmitsConnectionAuthArtifacts() throws Exception {
        // Regression check: a spec with no components.securitySchemes at all must generate
        // exactly what it did before this feature existed -- none of the extra credential
        // or URL fields.
        Path spec = specPath("sendgrid_minimal.json");
        Path outDir = Files.createTempDirectory("no-connection-auth-gen");
        try {
            AsyncApiSpec asyncApiSpec = AsyncApiParser.parseFromJsonString(Files.readString(spec));
            new HttpCodeGenerator(asyncApiSpec).generate(outDir);

            String types = readFile(outDir, "data_types.bal");
            Assert.assertFalse(types.contains("clientId"), "No OAuth2 fields should be generated");
            Assert.assertFalse(types.contains("username"), "No userPassword fields should be generated");
            Assert.assertFalse(types.contains("refreshUrl") || types.contains("tokenUrl"),
                    "No connection-auth URL field should be generated");
            Assert.assertFalse(types.contains("@display"),
                    "webhookSecret should use a doc comment, not @display, even with no connection auth");
            Assert.assertTrue(types.contains("Webhook Secret"), "webhookSecret field should have a doc comment");
        } finally {
            deleteDir(outDir);
        }
    }

    @Test
    void testGenerateMissingEventIdentifierThrows() throws Exception {
        Path spec = specPath("missing_event_identifier.json");
        AsyncApiSpec asyncApiSpec = AsyncApiParser.parseFromJsonString(Files.readString(spec));
        Path outDir = Files.createTempDirectory("missing-id-gen");
        try {
            new HttpCodeGenerator(asyncApiSpec).generate(outDir);
            Assert.fail("Expected GeneratorException due to missing x-ballerina-event-identifier");
        } catch (GeneratorException e) {
            Assert.assertTrue(e.getMessage().contains("x-ballerina-event-identifier"),
                    "Exception message should mention missing x-ballerina-event-identifier");
        } finally {
            deleteDir(outDir);
        }
    }

    private static Path specPath(String filename) throws URISyntaxException {
        URL resource = HttpCodeGeneratorIntegrationTest.class.getClassLoader().getResource("specs/" + filename);
        Objects.requireNonNull(resource, "Test spec resource not found: " + filename);
        return Path.of(resource.toURI());
    }

    private static String readFile(Path dir, String filename) throws Exception {
        return Files.readString(dir.resolve(filename));
    }

    private static void deleteDir(Path dir) throws Exception {
        if (Files.exists(dir)) {
            try (var stream = Files.walk(dir)) {
                stream.sorted(java.util.Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.delete(p);
                            } catch (Exception ignored) {
                            }
                        });
            }
        }
    }
}
