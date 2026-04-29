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
 * Integration tests for {@link UtilGenerator} verifying that {@code generate()} includes
 * {@code PipesMap} unconditionally, {@code StreamGeneratorsMap} and per-return-type stream
 * generator classes when server-streaming operations are present, and query/header helper
 * functions when the corresponding channel bindings are defined.
 */
public class UtilGeneratorTest {

    private static final String SPEC_MINIMAL =
            "{\"asyncapi\":\"2.0.0\",\"info\":{\"title\":\"T\",\"version\":\"1\"},\"channels\":{}}";

    /**
     * A spec without x-dispatcherKey so that handleReferenceReturn skips dispatcher validation
     * when resolving the x-response $ref inside the extension. The SEND message's x-response-type
     * is "server-streaming", causing UtilGenerator to emit StreamGeneratorsMap and a per-type
     * stream generator class named PongStreamGenerator.
     */
    private static final String SPEC_WITH_SERVER_STREAMING =
            "{\"asyncapi\":\"2.0.0\",\"info\":{\"title\":\"T\",\"version\":\"1\"},"
                    + "\"channels\":{\"chat\":{\"publish\":{"
                    + "\"operationId\":\"sendChat\","
                    + "\"message\":{\"$ref\":\"#/components/messages/ChatMessage\"}}}},"
                    + "\"components\":{\"messages\":{"
                    + "\"ChatMessage\":{"
                    + "\"payload\":{\"type\":\"string\"},"
                    + "\"x-response\":{\"$ref\":\"#/components/messages/Pong\"},"
                    + "\"x-response-type\":\"server-streaming\"},"
                    + "\"Pong\":{\"payload\":{\"type\":\"object\"}}}}}";

    private static final String SPEC_WITH_QUERY_BINDING =
            "{\"asyncapi\":\"2.0.0\",\"info\":{\"title\":\"T\",\"version\":\"1\"},"
                    + "\"channels\":{\"chat\":{\"bindings\":{\"ws\":{\"query\":{"
                    + "\"type\":\"object\","
                    + "\"properties\":{\"token\":{\"type\":\"string\"}}}}}}}}";

    private static final String SPEC_WITH_HEADER_BINDING =
            "{\"asyncapi\":\"2.0.0\",\"info\":{\"title\":\"T\",\"version\":\"1\"},"
                    + "\"channels\":{\"chat\":{\"bindings\":{\"ws\":{\"headers\":{"
                    + "\"type\":\"object\","
                    + "\"properties\":{\"Authorization\":{\"type\":\"string\"}}}}}}}}";

    private static String generate(String specJson) throws AsyncApiParserException, GeneratorException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(specJson);
        WsClientConfig config = new WsClientConfig.Builder().withAsyncApi(spec).build();
        return new UtilGenerator(config).generate();
    }

    @Test
    void testGenerate_containsPipesMapClass() throws AsyncApiParserException, GeneratorException {
        String source = generate(SPEC_MINIMAL);
        Assert.assertTrue(source.contains("PipesMap"),
                "generate() should always emit the 'PipesMap' class from the utility template");
    }

    @Test
    void testGenerate_containsStreamGeneratorsMap_whenServerStreamingOps()
            throws AsyncApiParserException, GeneratorException {
        String source = generate(SPEC_WITH_SERVER_STREAMING);
        Assert.assertTrue(source.contains("StreamGeneratorsMap"),
                "generate() should emit 'StreamGeneratorsMap' when a server-streaming operation is present");
    }

    @Test
    void testGenerate_containsStreamGeneratorClass_perReturnType()
            throws AsyncApiParserException, GeneratorException {
        String source = generate(SPEC_WITH_SERVER_STREAMING);
        Assert.assertTrue(source.contains("PongStreamGenerator"),
                "generate() should emit a 'PongStreamGenerator' class for each server-streaming return type");
    }

    @Test
    void testGenerate_containsQueryHelper_whenQueryBindingPresent()
            throws AsyncApiParserException, GeneratorException {
        String source = generate(SPEC_WITH_QUERY_BINDING);
        Assert.assertTrue(source.contains("getPathForQueryParam"),
                "generate() should include the 'getPathForQueryParam' helper when the WS binding has query params");
    }

    @Test
    void testGenerate_containsHeaderHelper_whenHeaderBindingPresent()
            throws AsyncApiParserException, GeneratorException {
        String source = generate(SPEC_WITH_HEADER_BINDING);
        Assert.assertTrue(source.contains("getCombineHeaders"),
                "generate() should include the 'getCombineHeaders' helper when the WS binding has header params");
    }
}
