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
package io.ballerina.asyncapi.generator.ws.client.node.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.ballerina.asyncapi.core.AsyncApiParser;
import io.ballerina.asyncapi.core.AsyncApiParserException;
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.core.model.channel.AsyncApiChannel;
import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.ws.client.generator.BallerinaAuthConfigGenerator;
import io.ballerina.asyncapi.generator.ws.client.model.WsClientConfig;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * Unit tests for {@link ClientInitGenerator} verifying that the generated {@code init}
 * function includes service URL parameters, optional query and header parameters,
 * WebSocket client creation, and worker start calls.
 */
public class ClientInitGeneratorTest {

    private static final String BASE_SPEC =
            "{\"asyncapi\":\"2.0.0\",\"info\":{\"title\":\"T\",\"version\":\"1\"},"
                    + "\"channels\":{\"chat\":{}}}";

    private static AsyncApiChannel firstChannel(AsyncApiSpec spec) {
        Map<String, AsyncApiChannel> channels = spec.getAsyncApiChannels().orElse(null);
        Assert.assertNotNull(channels, "Spec must define at least one channel");
        return channels.values().iterator().next();
    }

    @Test
    void testBuildInitFunction_containsServiceUrlParam()
            throws AsyncApiParserException, GeneratorException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(BASE_SPEC);
        WsClientConfig config = new WsClientConfig.Builder().withAsyncApi(spec).build();
        AsyncApiChannel channel = firstChannel(spec);
        BallerinaAuthConfigGenerator authGen = new BallerinaAuthConfigGenerator();
        FunctionDefinitionNode node = new ClientInitGenerator(
                config, channel, null, null, "wss://example.com", authGen, false)
                .buildInitFunction();
        Assert.assertTrue(node.toString().contains("serviceUrl"),
                "init function should declare a 'serviceUrl' parameter for the WebSocket endpoint URL");
    }

    @Test
    void testBuildInitFunction_containsQueryParam_whenQueryBinding()
            throws AsyncApiParserException, GeneratorException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(BASE_SPEC);
        WsClientConfig config = new WsClientConfig.Builder().withAsyncApi(spec).build();
        AsyncApiChannel channel = firstChannel(spec);
        BallerinaAuthConfigGenerator authGen = new BallerinaAuthConfigGenerator();
        ObjectNode querySchema = new ObjectMapper().createObjectNode();
        ObjectNode properties = querySchema.putObject("properties");
        properties.putObject("token").put("type", "string");
        FunctionDefinitionNode node = new ClientInitGenerator(
                config, channel, querySchema, null, "wss://example.com", authGen, false)
                .buildInitFunction();
        Assert.assertTrue(node.toString().contains("QueryParams"),
                "init function should include 'QueryParams' as a parameter type for query binding schemas");
    }

    @Test
    void testBuildInitFunction_containsHeaderParam_whenHeaderBinding()
            throws AsyncApiParserException, GeneratorException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(BASE_SPEC);
        WsClientConfig config = new WsClientConfig.Builder().withAsyncApi(spec).build();
        AsyncApiChannel channel = firstChannel(spec);
        BallerinaAuthConfigGenerator authGen = new BallerinaAuthConfigGenerator();
        ObjectNode headerSchema = new ObjectMapper().createObjectNode();
        ObjectNode properties = headerSchema.putObject("properties");
        properties.putObject("Authorization").put("type", "string");
        FunctionDefinitionNode node = new ClientInitGenerator(
                config, channel, null, headerSchema, "wss://example.com", authGen, false)
                .buildInitFunction();
        Assert.assertTrue(node.toString().contains("HeaderParams"),
                "init function should include 'HeaderParams' as a parameter type for header binding schemas");
    }

    @Test
    void testBuildInitFunction_bodyContainsWsClientCreation()
            throws AsyncApiParserException, GeneratorException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(BASE_SPEC);
        WsClientConfig config = new WsClientConfig.Builder().withAsyncApi(spec).build();
        AsyncApiChannel channel = firstChannel(spec);
        BallerinaAuthConfigGenerator authGen = new BallerinaAuthConfigGenerator();
        FunctionDefinitionNode node = new ClientInitGenerator(
                config, channel, null, null, "wss://example.com", authGen, false)
                .buildInitFunction();
        Assert.assertTrue(node.toString().contains("websocket:Client"),
                "init body should create a 'websocket:Client' to establish the WebSocket connection");
    }

    @Test
    void testBuildInitFunction_bodyContainsWorkerStartCalls()
            throws AsyncApiParserException, GeneratorException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(BASE_SPEC);
        WsClientConfig config = new WsClientConfig.Builder().withAsyncApi(spec).build();
        AsyncApiChannel channel = firstChannel(spec);
        BallerinaAuthConfigGenerator authGen = new BallerinaAuthConfigGenerator();
        FunctionDefinitionNode node = new ClientInitGenerator(
                config, channel, null, null, "wss://example.com", authGen, false)
                .buildInitFunction();
        String src = node.toString();
        Assert.assertTrue(src.contains("startMessageWriting"),
                "init body should call 'startMessageWriting' to start the outbound message worker");
        Assert.assertTrue(src.contains("startMessageReading"),
                "init body should call 'startMessageReading' to start the inbound message worker");
    }
}
