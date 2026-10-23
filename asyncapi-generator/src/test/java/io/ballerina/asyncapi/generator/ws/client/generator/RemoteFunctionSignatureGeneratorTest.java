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
import io.ballerina.asyncapi.core.model.component.AsyncApiSchema;
import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for {@link RemoteFunctionSignatureGenerator} covering parameter string
 * construction, return type string derivation, data type resolution, and full
 * function signature node generation.
 */
public class RemoteFunctionSignatureGeneratorTest {

    private static final String BASE_SPEC =
            "{\"asyncapi\":\"2.0.0\",\"info\":{\"title\":\"T\",\"version\":\"1\"},\"channels\":{}}";

    private static final String SPEC_WITH_DISPATCHER_KEY_AND_SCHEMA =
            "{\"asyncapi\":\"2.0.0\",\"info\":{\"title\":\"T\",\"version\":\"1\"},\"channels\":{},"
                    + "\"x-dispatcherKey\":\"event\","
                    + "\"components\":{\"schemas\":{\"ChatMessage\":{\"type\":\"object\"}}}}";

    @Test
    void testBuildParamString_includesPayloadAndTimeout()
            throws AsyncApiParserException, GeneratorException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(BASE_SPEC);
        AsyncApiSchema payload = AsyncApiSchema.builder().name("ChatMessage").build();
        String result = new RemoteFunctionSignatureGenerator(spec, new ArrayList<>())
                .buildParamString(payload);
        Assert.assertTrue(result.contains("ChatMessage"),
                "buildParamString should include the payload type name when payload is non-null");
        Assert.assertTrue(result.contains("decimal"),
                "buildParamString should include the 'decimal' type for the timeout parameter");
        Assert.assertTrue(result.contains("timeout"),
                "buildParamString should include the 'timeout' parameter name");
    }

    @Test
    void testBuildParamString_timeoutOnlyWhenNoPayload()
            throws AsyncApiParserException, GeneratorException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(BASE_SPEC);
        String result = new RemoteFunctionSignatureGenerator(spec, new ArrayList<>())
                .buildParamString(null);
        Assert.assertEquals(result, "decimal timeout",
                "buildParamString with null payload should return exactly 'decimal timeout'");
    }

    @Test
    void testBuildReturnTypeString_streamType_whenServerStreaming() throws AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(BASE_SPEC);
        Map<String, JsonNode> extensions = Map.of(
                "x-response", new ObjectMapper().createObjectNode().put("description", "Response"),
                "x-response-type", TextNode.valueOf("server-streaming")
        );
        String result = new RemoteFunctionSignatureGenerator(spec, new ArrayList<>())
                .buildReturnTypeString(extensions, "ChatMessage");
        Assert.assertTrue(result.contains("stream<"),
                "Server-streaming response type should produce a 'stream<T,error?>' return type");
        Assert.assertTrue(result.contains("|error"),
                "Server-streaming return type should be joined with '|error'");
    }

    @Test
    void testBuildReturnTypeString_simpleType_whenDefaultResponse() throws AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(BASE_SPEC);
        Map<String, JsonNode> extensions = Map.of(
                "x-response", new ObjectMapper().createObjectNode()
        );
        String result = new RemoteFunctionSignatureGenerator(spec, new ArrayList<>())
                .buildReturnTypeString(extensions, "ChatMessage");
        Assert.assertEquals(result, "ChatMessage|error",
                "Non-streaming response with x-response extension should return 'TypeName|error'");
    }

    @Test
    void testBuildReturnTypeString_errorOnly_whenNoResponse() throws AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(BASE_SPEC);
        String result = new RemoteFunctionSignatureGenerator(spec, new ArrayList<>())
                .buildReturnTypeString(null, "ChatMessage");
        Assert.assertEquals(result, "error?",
                "Absent x-response extension should produce 'error?' as the return type");
    }

    @Test
    void testGetDataType_returnsType_whenSchemaValid()
            throws AsyncApiParserException, GeneratorException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(BASE_SPEC);
        AsyncApiSchema schema = AsyncApiSchema.builder().type("integer").build();
        String result = new RemoteFunctionSignatureGenerator(spec, new ArrayList<>())
                .getDataType(schema);
        Assert.assertEquals(result, "int",
                "Schema with type 'integer' should resolve to Ballerina 'int' via TYPE_MAP");
    }

    @Test
    void testGetDataType_throwsGeneratorException_whenDispatcherKeyMissing()
            throws AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(SPEC_WITH_DISPATCHER_KEY_AND_SCHEMA);
        AsyncApiSchema payload = AsyncApiSchema.builder().name("ChatMessage").build();
        try {
            new RemoteFunctionSignatureGenerator(spec, new ArrayList<>()).getDataType(payload);
            Assert.fail("Expected GeneratorException when dispatcherKey is missing from the schema properties");
        } catch (GeneratorException e) {
            Assert.assertNotNull(e.getMessage(),
                    "Exception message should not be null when the dispatcher key is absent from the schema");
        }
    }

    @Test
    void testGetFunctionSignatureNode_nonNull_whenValidInputs()
            throws AsyncApiParserException, GeneratorException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(BASE_SPEC);
        List<Node> doc = new ArrayList<>();
        List<String> streamReturns = new ArrayList<>();
        List<TypeDefinitionNode> typeDefs = new ArrayList<>();
        FunctionSignatureNode node = new RemoteFunctionSignatureGenerator(spec, typeDefs)
                .getFunctionSignatureNode(null, doc, null, "null", streamReturns);
        Assert.assertNotNull(node,
                "getFunctionSignatureNode should return a non-null FunctionSignatureNode");
        Assert.assertTrue(node.toString().contains("timeout"),
                "The generated signature should include the mandatory timeout parameter");
    }
}
