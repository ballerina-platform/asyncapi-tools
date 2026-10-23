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

import io.ballerina.asyncapi.core.AsyncApiParser;
import io.ballerina.asyncapi.core.AsyncApiParserException;
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.ws.client.generator.BallerinaAuthConfigGenerator;
import io.ballerina.compiler.syntax.tree.ObjectFieldNode;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Unit tests for {@link ClientFieldGenerator} verifying that the generated object field
 * nodes include the mandatory client fields, optional stream-generators and API-key fields,
 * and a correctly initialised responseMap literal.
 */
public class ClientFieldGeneratorTest {

    private static final String SPEC_WITH_API_KEY =
            "{\"asyncapi\":\"2.0.0\",\"info\":{\"title\":\"T\",\"version\":\"1\"},\"channels\":{},"
                    + "\"components\":{\"securitySchemes\":{"
                    + "\"apiKey\":{\"type\":\"httpApiKey\",\"name\":\"X-Api-Key\",\"in\":\"header\"}}}}";

    @Test
    void testBuildFields_containsCoreFields() throws GeneratorException {
        BallerinaAuthConfigGenerator authGen = new BallerinaAuthConfigGenerator();
        ClientFieldGenerator gen = new ClientFieldGenerator(false, authGen, Map.of());
        List<ObjectFieldNode> fields = gen.buildFields();
        String src = fields.stream().map(ObjectFieldNode::toString).collect(Collectors.joining("\n"));
        Assert.assertTrue(src.contains("clientEp"),
                "buildFields must include the 'clientEp' WebSocket client field");
        Assert.assertTrue(src.contains("writeMessageQueue"),
                "buildFields must include the 'writeMessageQueue' pipe field for outbound messages");
        Assert.assertTrue(src.contains("pipes"),
                "buildFields must include the 'pipes' PipesMap field for response routing");
        Assert.assertTrue(src.contains("isActive"),
                "buildFields must include the 'isActive' boolean field for connection lifecycle tracking");
    }

    @Test
    void testBuildFields_containsStreamGeneratorsField_whenServerStreamingOps() throws GeneratorException {
        BallerinaAuthConfigGenerator authGen = new BallerinaAuthConfigGenerator();
        ClientFieldGenerator gen = new ClientFieldGenerator(true, authGen, Map.of());
        List<ObjectFieldNode> fields = gen.buildFields();
        String src = fields.stream().map(ObjectFieldNode::toString).collect(Collectors.joining("\n"));
        Assert.assertTrue(src.contains("streamGenerators"),
                "buildFields with isStreamPresent=true must add the 'streamGenerators' field");
    }

    @Test
    void testBuildFields_containsApiKeysField_whenApiKeyAuth()
            throws GeneratorException, AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(SPEC_WITH_API_KEY);
        BallerinaAuthConfigGenerator authGen = new BallerinaAuthConfigGenerator();
        authGen.addAuthRelatedRecords(spec);
        ClientFieldGenerator gen = new ClientFieldGenerator(false, authGen, Map.of());
        List<ObjectFieldNode> fields = gen.buildFields();
        String src = fields.stream().map(ObjectFieldNode::toString).collect(Collectors.joining("\n"));
        Assert.assertTrue(src.contains("apiKeyConfig"),
                "buildFields must add the 'apiKeyConfig' field when API-key auth is configured");
    }

    @Test
    void testBuildFields_responseMapContainsExpectedEntries() throws GeneratorException {
        BallerinaAuthConfigGenerator authGen = new BallerinaAuthConfigGenerator();
        Map<String, String> responseMap = Map.of("ChatMessage", "chatMessagePipe");
        ClientFieldGenerator gen = new ClientFieldGenerator(false, authGen, responseMap);
        List<ObjectFieldNode> fields = gen.buildFields();
        String src = fields.stream().map(ObjectFieldNode::toString).collect(Collectors.joining("\n"));
        Assert.assertTrue(src.contains("ChatMessage"),
                "buildFields responseMap field should embed the response type key 'ChatMessage'");
        Assert.assertTrue(src.contains("chatMessagePipe"),
                "buildFields responseMap field should embed the pipe name value 'chatMessagePipe'");
    }

    @Test
    void testBuildFields_responseMapEmptyGeneratesEmptyMapLiteral() throws GeneratorException {
        BallerinaAuthConfigGenerator authGen = new BallerinaAuthConfigGenerator();
        ClientFieldGenerator gen = new ClientFieldGenerator(false, authGen, Map.of());
        List<ObjectFieldNode> fields = gen.buildFields();
        String src = fields.stream().map(ObjectFieldNode::toString).collect(Collectors.joining("\n"));
        Assert.assertTrue(src.contains("responseMap = {}"),
                "buildFields should initialize responseMap with an empty map literal when no entries exist");
    }
}
