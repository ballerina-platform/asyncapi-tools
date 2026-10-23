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
package io.ballerina.asyncapi.generator.ws.client;

import io.ballerina.asyncapi.core.AsyncApiParser;
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.generator.GeneratorException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Integration tests for {@link WsClientCodeGenerator} that parse AsyncAPI JSON spec fixtures
 * and verify the generated source files.
 */
public class WsClientCodeGeneratorIntegrationTest {

    @Test
    void testGenerateSimpleWsClient() throws Exception {
        Path spec = specPath("ws_simple.json");
        Path outDir = Files.createTempDirectory("ws-simple-gen");
        try {
            AsyncApiSpec asyncApiSpec = AsyncApiParser.parseFromJsonString(Files.readString(spec));
            new WsClientCodeGenerator(asyncApiSpec, null, false).generate(outDir);

            String client = readFile(outDir, "client.bal");
            String utils = readFile(outDir, "utils.bal");
            String types = readFile(outDir, "types.bal");

            Assert.assertTrue(client.contains("class"),
                    "client.bal should define a Ballerina client class");
            Assert.assertTrue(client.contains("websocket"),
                    "client.bal should import the 'websocket' module");
            Assert.assertTrue(utils.contains("PipesMap"),
                    "utils.bal should always contain the PipesMap class");
            Assert.assertTrue(types.contains("Message"),
                    "types.bal should always contain the synthetic Message type");
        } finally {
            deleteDir(outDir);
        }
    }

    @Test
    void testGenerateWithStreamId() throws Exception {
        Path spec = specPath("ws_with_stream_id.json");
        Path outDir = Files.createTempDirectory("ws-stream-id-gen");
        try {
            AsyncApiSpec asyncApiSpec = AsyncApiParser.parseFromJsonString(Files.readString(spec));
            new WsClientCodeGenerator(asyncApiSpec, null, false).generate(outDir);

            String types = readFile(outDir, "types.bal");

            Assert.assertTrue(types.contains("MessageWithId"),
                    "types.bal should contain MessageWithId when x-dispatcherStreamId is present in the spec");
        } finally {
            deleteDir(outDir);
        }
    }

    @Test
    void testGenerateWithServerStreaming() throws Exception {
        Path spec = specPath("ws_with_server_streaming.json");
        Path outDir = Files.createTempDirectory("ws-streaming-gen");
        try {
            AsyncApiSpec asyncApiSpec = AsyncApiParser.parseFromJsonString(Files.readString(spec));
            new WsClientCodeGenerator(asyncApiSpec, null, false).generate(outDir);

            String client = readFile(outDir, "client.bal");
            String utils = readFile(outDir, "utils.bal");

            Assert.assertTrue(utils.contains("StreamGeneratorsMap"),
                    "utils.bal should contain StreamGeneratorsMap when a server-streaming operation is present");
            Assert.assertTrue(client.contains("stream<"),
                    "client.bal should use a stream<> return type for server-streaming remote functions");
        } finally {
            deleteDir(outDir);
        }
    }

    @Test
    void testGenerateWithApiKeyAuth() throws Exception {
        Path spec = specPath("ws_with_apikey_auth.json");
        Path outDir = Files.createTempDirectory("ws-apikey-gen");
        try {
            AsyncApiSpec asyncApiSpec = AsyncApiParser.parseFromJsonString(Files.readString(spec));
            new WsClientCodeGenerator(asyncApiSpec, null, false).generate(outDir);

            String client = readFile(outDir, "client.bal");

            Assert.assertTrue(client.contains("ApiKeysConfig"),
                    "client.bal should contain the ApiKeysConfig type definition when httpApiKey auth is configured");
            Assert.assertTrue(client.contains("apiKeyConfig"),
                    "client.bal should reference apiKeyConfig in the init function when httpApiKey auth is configured");
        } finally {
            deleteDir(outDir);
        }
    }

    @Test
    void testGenerateMissingDispatcherKeyThrows() throws Exception {
        Path spec = specPath("ws_missing_dispatcher_key.json");
        AsyncApiSpec asyncApiSpec = AsyncApiParser.parseFromJsonString(Files.readString(spec));
        Path outDir = Files.createTempDirectory("ws-missing-key-gen");
        try {
            new WsClientCodeGenerator(asyncApiSpec, null, false).generate(outDir);
            Assert.fail("Expected GeneratorException due to missing x-dispatcherKey extension");
        } catch (GeneratorException e) {
            Assert.assertTrue(e.getMessage().contains("x-dispatcherKey"),
                    "Exception message should mention the missing x-dispatcherKey extension");
        } finally {
            deleteDir(outDir);
        }
    }

    private static Path specPath(String filename) throws URISyntaxException {
        URL resource = WsClientCodeGeneratorIntegrationTest.class.getClassLoader()
                .getResource("specs/" + filename);
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
