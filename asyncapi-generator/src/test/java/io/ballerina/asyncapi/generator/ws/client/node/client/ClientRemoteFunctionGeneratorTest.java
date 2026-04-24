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
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for {@link ClientRemoteFunctionGenerator} verifying function count per message,
 * the {@code do}-prefixed naming convention, and deduplication of RECEIVE-only messages
 * whose types are already covered by a SEND operation's {@code x-response}.
 */
public class ClientRemoteFunctionGeneratorTest {

    private static final String SPEC_ONE_SEND =
            "{\"asyncapi\":\"2.0.0\",\"info\":{\"title\":\"T\",\"version\":\"1\"},"
                    + "\"channels\":{\"chat\":{\"publish\":{"
                    + "\"operationId\":\"sendChatMessage\","
                    + "\"message\":{\"$ref\":\"#/components/messages/ChatMessage\"}}}},"
                    + "\"components\":{\"messages\":{"
                    + "\"ChatMessage\":{\"payload\":{\"type\":\"string\"}}}}}";

    private static final String SPEC_SEND_WITH_RESPONSE_AND_RECEIVE =
            "{\"asyncapi\":\"2.0.0\",\"info\":{\"title\":\"T\",\"version\":\"1\"},"
                    + "\"channels\":{\"chat\":{"
                    + "\"publish\":{\"operationId\":\"sendChatSend\","
                    + "\"message\":{\"$ref\":\"#/components/messages/ChatSend\"}},"
                    + "\"subscribe\":{\"operationId\":\"receivePong\","
                    + "\"message\":{\"$ref\":\"#/components/messages/Pong\"}}}},"
                    + "\"components\":{\"messages\":{"
                    + "\"ChatSend\":{\"payload\":{\"type\":\"string\"},"
                    + "\"x-response\":{\"$ref\":\"#/components/messages/Pong\"}},"
                    + "\"Pong\":{\"payload\":{\"type\":\"object\"}}}}}";

    private static ClientRemoteFunctionGenerator generator(AsyncApiSpec spec,
                                                            Map<String, String> responseMap) {
        List<String> streamReturns = new ArrayList<>();
        List<ImportDeclarationNode> imports = new ArrayList<>();
        List<TypeDefinitionNode> typeDefs = new ArrayList<>();
        return new ClientRemoteFunctionGenerator(spec, null, streamReturns, imports, typeDefs, responseMap);
    }

    @Test
    void testBuildRemoteFunctions_returnsOneFunctionPerMessage()
            throws AsyncApiParserException, GeneratorException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(SPEC_ONE_SEND);
        List<FunctionDefinitionNode> functions = generator(spec, Map.of()).buildRemoteFunctions();
        Assert.assertEquals(functions.size(), 1,
                "buildRemoteFunctions should generate exactly one function for a spec with one SEND message");
    }

    @Test
    void testBuildRemoteFunctions_prefixesNamesWithDo()
            throws AsyncApiParserException, GeneratorException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(SPEC_ONE_SEND);
        List<FunctionDefinitionNode> functions = generator(spec, Map.of()).buildRemoteFunctions();
        Assert.assertFalse(functions.isEmpty(),
                "buildRemoteFunctions must return at least one function for a single-message SEND spec");
        String src = functions.get(0).toString();
        Assert.assertTrue(src.contains("do"),
                "Remote function names should be prefixed with 'do' per the naming convention");
    }

    @Test
    void testBuildRemoteFunctions_deduplicatesSameMessageType()
            throws AsyncApiParserException, GeneratorException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(SPEC_SEND_WITH_RESPONSE_AND_RECEIVE);
        Map<String, String> responseMap = Map.of("Pong", "pong");
        List<FunctionDefinitionNode> functions = generator(spec, responseMap).buildRemoteFunctions();
        Assert.assertEquals(functions.size(), 1,
                "buildRemoteFunctions should skip the RECEIVE-only 'Pong' message because it is already "
                        + "covered as the x-response type of the SEND 'ChatSend' message");
    }
}
