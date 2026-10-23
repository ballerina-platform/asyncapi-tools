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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import io.ballerina.asyncapi.core.AsyncApiParser;
import io.ballerina.asyncapi.core.AsyncApiParserException;
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.generator.GeneratorException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;

/**
 * Unit tests for {@link RemoteFunctionReturnTypeGenerator} covering single-ref return type
 * derivation, oneOf pipe-joined types, and the error path for invalid message references.
 */
public class RemoteFunctionReturnTypeGeneratorTest {

    private static final String BASE_SPEC =
            "{\"asyncapi\":\"2.0.0\",\"info\":{\"title\":\"T\",\"version\":\"1\"},\"channels\":{}}";

    private static final String SPEC_WITH_CHAT_MESSAGE =
            "{\"asyncapi\":\"2.0.0\",\"info\":{\"title\":\"T\",\"version\":\"1\"},\"channels\":{},"
                    + "\"components\":{\"messages\":{"
                    + "\"ChatMessage\":{\"payload\":{\"type\":\"object\"}}}}}";

    private static final String SPEC_WITH_TWO_MESSAGES =
            "{\"asyncapi\":\"2.0.0\",\"info\":{\"title\":\"T\",\"version\":\"1\"},\"channels\":{},"
                    + "\"components\":{\"messages\":{"
                    + "\"Msg1\":{\"payload\":{\"type\":\"object\"}},"
                    + "\"Msg2\":{\"payload\":{\"type\":\"object\"}}}}}";

    @Test
    void testGetReturnType_singleType_whenRefInput() throws AsyncApiParserException, GeneratorException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(SPEC_WITH_CHAT_MESSAGE);
        JsonNode xResponse = new ObjectMapper().createObjectNode()
                .put("$ref", "#/components/messages/ChatMessage");
        String result = new RemoteFunctionReturnTypeGenerator(spec)
                .getReturnType(xResponse, null, new ArrayList<>());
        Assert.assertEquals(result, "ChatMessage",
                "A single $ref to an existing message should resolve to the PascalCase schema name");
    }

    @Test
    void testGetReturnType_pipeJoined_whenOneOf() throws AsyncApiParserException, GeneratorException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(SPEC_WITH_TWO_MESSAGES);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode refMsg1 = mapper.createObjectNode().put("$ref", "#/components/messages/Msg1");
        JsonNode refMsg2 = mapper.createObjectNode().put("$ref", "#/components/messages/Msg2");
        JsonNode xResponse = mapper.createObjectNode()
                .set("oneOf", mapper.createArrayNode().add(refMsg1).add(refMsg2));
        JsonNode xResponseType = TextNode.valueOf("simple-rpc");
        String result = new RemoteFunctionReturnTypeGenerator(spec)
                .getReturnType(xResponse, xResponseType, new ArrayList<>());
        Assert.assertTrue(result.contains("Msg1"),
                "oneOf return type should include 'Msg1' from the first reference");
        Assert.assertTrue(result.contains("Msg2"),
                "oneOf return type should include 'Msg2' from the second reference");
        Assert.assertTrue(result.contains("|"),
                "oneOf with two members should produce a pipe-separated union return type");
    }

    @Test
    void testGetReturnType_throwsGeneratorException_whenRefInvalid() throws AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(BASE_SPEC);
        JsonNode xResponse = new ObjectMapper().createObjectNode()
                .put("$ref", "#/components/messages/NonExistent");
        try {
            new RemoteFunctionReturnTypeGenerator(spec)
                    .getReturnType(xResponse, null, new ArrayList<>());
            Assert.fail("Expected GeneratorException when the referenced message does not exist in components");
        } catch (GeneratorException e) {
            Assert.assertTrue(e.getMessage().contains("NonExistent"),
                    "Exception message should identify the missing message name 'NonExistent'");
        }
    }
}
