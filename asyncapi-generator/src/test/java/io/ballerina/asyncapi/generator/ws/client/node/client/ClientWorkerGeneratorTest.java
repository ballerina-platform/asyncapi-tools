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
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link ClientWorkerGenerator} verifying that the writing and reading
 * worker function nodes contain the expected queue, pipe, and message access statements.
 */
public class ClientWorkerGeneratorTest {

    private static final String BASE_SPEC =
            "{\"asyncapi\":\"2.0.0\",\"info\":{\"title\":\"T\",\"version\":\"1\"},\"channels\":{}}";

    @Test
    void testBuildStartMessageWriting_containsQueueConsumeAndWrite() throws AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(BASE_SPEC);
        FunctionDefinitionNode node = new ClientWorkerGenerator(spec, "event", null)
                .buildStartMessageWriting();
        String src = node.toString();
        Assert.assertTrue(src.contains("consume"),
                "startMessageWriting worker should call 'consume' to dequeue a message from writeMessageQueue");
        Assert.assertTrue(src.contains("writeMessage"),
                "startMessageWriting worker should call 'writeMessage' to push the message to the WebSocket");
    }

    @Test
    void testBuildStartMessageReading_containsDispatcherKeyLookupAndProduce() throws AsyncApiParserException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(BASE_SPEC);
        FunctionDefinitionNode node = new ClientWorkerGenerator(spec, "event", null)
                .buildStartMessageReading();
        String src = node.toString();
        Assert.assertTrue(src.contains("getPipeName"),
                "startMessageReading worker should call 'getPipeName' to resolve the pipe from the dispatcher key");
        Assert.assertTrue(src.contains("produce"),
                "startMessageReading worker should call 'produce' to forward the received message into the pipe");
    }
}
