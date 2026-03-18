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
            AsyncApiSpec asyncApiSpec = AsyncApiParser.parseFromFile(spec);
            new HttpCodeGenerator(asyncApiSpec).generate(outDir);

            String serviceTypes = readFile(outDir, "service_types.bal");
            String types = readFile(outDir, "types.bal");
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
                    "types.bal should contain PushEvent schema type");
            Assert.assertTrue(types.contains("ListenerConfiguration"),
                    "types.bal should always contain ListenerConfiguration");
            Assert.assertTrue(types.contains("GenericDataType"),
                    "types.bal should contain GenericDataType union");

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
            AsyncApiSpec asyncApiSpec = AsyncApiParser.parseFromFile(spec);
            new HttpCodeGenerator(asyncApiSpec).generate(outDir);

            String serviceTypes = readFile(outDir, "service_types.bal");
            String dispatcher = readFile(outDir, "dispatcher_service.bal");
            String listener = readFile(outDir, "listener.bal");
            String types = readFile(outDir, "types.bal");

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
                    "types.bal should contain PaymentIntent schema type");
            Assert.assertTrue(types.contains("Customer"),
                    "types.bal should contain Customer schema type");
        } finally {
            deleteDir(outDir);
        }
    }

    @Test
    void testGenerateWithInlineSchema() throws Exception {
        Path spec = specPath("sendgrid_minimal.json");
        Path outDir = Files.createTempDirectory("sendgrid-gen");
        try {
            AsyncApiSpec asyncApiSpec = AsyncApiParser.parseFromFile(spec);
            new HttpCodeGenerator(asyncApiSpec).generate(outDir);

            String serviceTypes = readFile(outDir, "service_types.bal");
            String types = readFile(outDir, "types.bal");
            String listener = readFile(outDir, "listener.bal");

            Assert.assertTrue(serviceTypes.contains("EmailService"),
                    "service_types.bal should contain EmailService from channel 'email'");
            Assert.assertTrue(serviceTypes.contains("onEmailDelivered"),
                    "service_types.bal should contain onEmailDelivered remote function");
            Assert.assertTrue(serviceTypes.contains("GenericServiceType"),
                    "service_types.bal should contain GenericServiceType union");

            Assert.assertTrue(types.contains("ListenerConfiguration"),
                    "types.bal should contain ListenerConfiguration");
            Assert.assertFalse(types.isBlank(), "types.bal should not be blank");

            Assert.assertTrue(listener.contains("Listener"),
                    "listener.bal should contain the Listener class");
        } finally {
            deleteDir(outDir);
        }
    }

    @Test
    void testGenerateWithNullOutputPath() throws Exception {
        Path spec = specPath("sendgrid_minimal.json");
        AsyncApiSpec asyncApiSpec = AsyncApiParser.parseFromFile(spec);
        try {
            new HttpCodeGenerator(asyncApiSpec).generate(null);
        } finally {
            for (String file : List.of("types.bal", "service_types.bal", "listener.bal", "dispatcher_service.bal")) {
                Files.deleteIfExists(Path.of(file));
            }
        }
    }

    @Test
    void testGenerateMissingEventIdentifierThrows() throws Exception {
        Path spec = specPath("missing_event_identifier.json");
        AsyncApiSpec asyncApiSpec = AsyncApiParser.parseFromFile(spec);
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
