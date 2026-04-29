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
import com.fasterxml.jackson.databind.node.TextNode;
import io.ballerina.asyncapi.generator.ws.client.utils.CodegenUtils;
import io.ballerina.compiler.syntax.tree.FunctionBodyNode;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for {@link RemoteFunctionBodyGenerator} covering the three body patterns
 * (server-streaming, simple-rpc, no-response), the function body node factory, and
 * import list retrieval.
 */
public class RemoteFunctionBodyGeneratorTest {

    private static final Map<String, JsonNode> STREAM_EXTENSIONS =
            Map.of("x-response-type", TextNode.valueOf("server-streaming"));

    private static final Map<String, JsonNode> SIMPLE_RPC_EXTENSIONS =
            Map.of("x-response-type", TextNode.valueOf("simple-rpc"));

    @Test
    void testBuildBodySource_containsStreamGenerator_whenSubscribe() {
        String body = new RemoteFunctionBodyGenerator(new ArrayList<>())
                .buildBodySource(STREAM_EXTENSIONS, "ChatMessage", null, true, "Response", "chatMessage");
        Assert.assertTrue(body.contains("stream<"),
                "Server-streaming body should declare a 'stream<T,error?>' variable");
        Assert.assertTrue(body.contains("StreamGenerator"),
                "Server-streaming body should instantiate a StreamGenerator for the response type");
    }

    @Test
    void testBuildBodySource_containsProduceAndConsume_whenSimpleRpc() {
        String body = new RemoteFunctionBodyGenerator(new ArrayList<>())
                .buildBodySource(SIMPLE_RPC_EXTENSIONS, "Send", null, false, "Reply", "reply");
        Assert.assertTrue(body.contains("produce"),
                "Simple-RPC body should call 'produce' to write the request message to the queue");
        Assert.assertTrue(body.contains("consume"),
                "Simple-RPC body should call 'consume' to read the response from the pipe");
    }

    @Test
    void testBuildBodySource_containsProduceOnly_whenNoResponse() {
        String body = new RemoteFunctionBodyGenerator(new ArrayList<>())
                .buildBodySource(null, "Notify", null, false, "null", "notify");
        Assert.assertTrue(body.contains("produce"),
                "No-response body should call 'produce' to write the request to the write-message queue");
        Assert.assertFalse(body.contains("consume"),
                "No-response body must not call 'consume' because there is no response to receive");
    }

    @Test
    void testGetFunctionBodyNode_nonNull_forEachPattern() {
        RemoteFunctionBodyGenerator gen = new RemoteFunctionBodyGenerator(new ArrayList<>());

        FunctionBodyNode simpleRpcNode = gen.getFunctionBodyNode(
                SIMPLE_RPC_EXTENSIONS, "Send", null, false, "Reply", "reply");
        Assert.assertNotNull(simpleRpcNode,
                "getFunctionBodyNode should return a non-null node for the simple-rpc pattern");

        FunctionBodyNode noResponseNode = gen.getFunctionBodyNode(
                null, "Notify", null, false, "null", "notify");
        Assert.assertNotNull(noResponseNode,
                "getFunctionBodyNode should return a non-null node for the no-response pattern");

        FunctionBodyNode streamNode = gen.getFunctionBodyNode(
                STREAM_EXTENSIONS, "Publish", null, true, "Event", "publish");
        Assert.assertNotNull(streamNode,
                "getFunctionBodyNode should return a non-null node for the server-streaming pattern");
    }

    @Test
    void testGetImports_nonEmpty_afterBodyGeneration() {
        ImportDeclarationNode importNode = CodegenUtils.getImportDeclarationNode("ballerina", "log");
        List<ImportDeclarationNode> imports = new ArrayList<>();
        imports.add(importNode);
        RemoteFunctionBodyGenerator gen = new RemoteFunctionBodyGenerator(imports);
        gen.buildBodySource(null, "Ping", null, false, "null", "ping");
        Assert.assertFalse(gen.getImports().isEmpty(),
                "getImports() should return the non-empty import list supplied to the constructor");
    }
}
