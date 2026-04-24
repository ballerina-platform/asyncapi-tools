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
package io.ballerina.asyncapi.generator.ws.client.generator;

import io.ballerina.asyncapi.core.AsyncApiParser;
import io.ballerina.asyncapi.core.AsyncApiParserException;
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.ws.client.model.WsClientConfig;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Integration tests for {@link ClientGenerator} verifying that {@code generate()} produces
 * well-formed Ballerina source containing the expected client class, imports, and field content.
 */
public class ClientGeneratorTest {

    /**
     * A spec with one SEND operation whose x-response points back to the same message/schema.
     * ChatMessage is in both components.messages (with x-response extension) and components.schemas
     * (with the dispatcher key property), ensuring that buildResponseMap produces a non-empty map
     * and that the Ballerina Formatter receives a valid syntax tree.
     */
    private static final String SPEC_WITH_OPERATIONS =
            "{\"asyncapi\":\"2.0.0\",\"info\":{\"title\":\"Chat\",\"version\":\"1\"},"
                    + "\"x-dispatcherKey\":\"event\","
                    + "\"channels\":{\"chat\":{\"publish\":{"
                    + "\"operationId\":\"sendChatMessage\","
                    + "\"message\":{\"$ref\":\"#/components/messages/ChatMessage\"}}}},"
                    + "\"components\":{"
                    + "\"messages\":{\"ChatMessage\":{"
                    + "\"payload\":{\"$ref\":\"#/components/schemas/ChatMessage\"},"
                    + "\"x-response\":{\"$ref\":\"#/components/messages/ChatMessage\"}}},"
                    + "\"schemas\":{\"ChatMessage\":{"
                    + "\"type\":\"object\","
                    + "\"required\":[\"event\"],"
                    + "\"properties\":{\"event\":{\"type\":\"string\"}}}}}}";

    private static final String SPEC_NO_DISPATCHER_KEY =
            "{\"asyncapi\":\"2.0.0\",\"info\":{\"title\":\"T\",\"version\":\"1\"},"
                    + "\"channels\":{\"chat\":{}}}";

    private static String generate(String specJson) throws AsyncApiParserException, GeneratorException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(specJson);
        WsClientConfig config = new WsClientConfig.Builder().withAsyncApi(spec).build();
        return new ClientGenerator(config).generate();
    }

    @Test
    void testGenerate_returnsNonEmptySource() throws AsyncApiParserException, GeneratorException {
        String source = generate(SPEC_WITH_OPERATIONS);
        Assert.assertFalse(source.isBlank(),
                "generate() must return a non-empty Ballerina source string for a valid spec");
    }

    @Test
    void testGenerate_sourceContainsClientClass() throws AsyncApiParserException, GeneratorException {
        String source = generate(SPEC_WITH_OPERATIONS);
        Assert.assertTrue(source.contains("class"),
                "generate() source must contain a 'class' keyword for the generated client class");
        Assert.assertTrue(source.contains("Client"),
                "generate() source must contain 'Client' as part of the generated class name");
    }

    @Test
    void testGenerate_sourceContainsWebSocketImport() throws AsyncApiParserException, GeneratorException {
        String source = generate(SPEC_WITH_OPERATIONS);
        Assert.assertTrue(source.contains("websocket"),
                "generate() source must import the 'websocket' module for WebSocket client usage");
    }

    @Test
    void testGenerate_sourceContainsPipeImport() throws AsyncApiParserException, GeneratorException {
        String source = generate(SPEC_WITH_OPERATIONS);
        Assert.assertTrue(source.contains("pipe"),
                "generate() source must import the 'pipe' module (xlibb) for message queuing");
    }

    @Test
    void testGenerate_usesDefaultUrl_whenNoServersInSpec()
            throws AsyncApiParserException, GeneratorException {
        String source = generate(SPEC_WITH_OPERATIONS);
        Assert.assertTrue(source.contains("localhost:9090"),
                "generate() should use 'localhost:9090' as the default server URL when no servers are defined");
    }

    @Test
    void testGenerate_responseMapContainsExpectedRequestType()
            throws AsyncApiParserException, GeneratorException {
        String source = generate(SPEC_WITH_OPERATIONS);
        Assert.assertTrue(source.contains("ChatMessage"),
                "generate() should embed 'ChatMessage' in the responseMap field as the response type key");
        Assert.assertTrue(source.contains("chatMessage"),
                "generate() should embed 'chatMessage' in the responseMap field as the mapped request type value");
    }

    @Test
    void testGenerate_throwsGeneratorException_whenDispatcherKeyAbsent()
            throws AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(SPEC_NO_DISPATCHER_KEY);
        WsClientConfig config = new WsClientConfig.Builder().withAsyncApi(spec).build();
        try {
            new ClientGenerator(config).generate();
            Assert.fail("generate() must throw GeneratorException when x-dispatcherKey is absent from the spec");
        } catch (GeneratorException e) {
            Assert.assertTrue(e.getMessage().contains("x-dispatcherKey"),
                    "Exception message should identify the missing x-dispatcherKey extension");
        }
    }
}
