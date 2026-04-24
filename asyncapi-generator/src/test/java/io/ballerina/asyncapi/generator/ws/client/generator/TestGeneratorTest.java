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
 * Integration tests for {@link TestGenerator} verifying that {@code generateTest()} produces
 * a Ballerina source with a client initializer and per-message test functions, returns {@code ""}
 * when no operations are present, and that {@code generateConfig()} emits the expected API-key
 * config block or an empty string depending on the security scheme configuration.
 */
public class TestGeneratorTest {

    private static final String SPEC_WITH_ONE_OPERATION =
            "{\"asyncapi\":\"2.0.0\",\"info\":{\"title\":\"T\",\"version\":\"1\"},"
                    + "\"channels\":{\"chat\":{\"publish\":{"
                    + "\"operationId\":\"sendChat\","
                    + "\"message\":{\"$ref\":\"#/components/messages/ChatMessage\"}}}},"
                    + "\"components\":{\"messages\":{"
                    + "\"ChatMessage\":{\"payload\":{\"type\":\"string\"}}}}}";

    private static final String SPEC_NO_OPERATIONS =
            "{\"asyncapi\":\"2.0.0\",\"info\":{\"title\":\"T\",\"version\":\"1\"},\"channels\":{}}";

    private static final String SPEC_WITH_API_KEY_SCHEME =
            "{\"asyncapi\":\"2.0.0\",\"info\":{\"title\":\"T\",\"version\":\"1\"},"
                    + "\"channels\":{},"
                    + "\"components\":{\"securitySchemes\":{"
                    + "\"myScheme\":{\"type\":\"apiKey\",\"name\":\"myKey\",\"in\":\"query\"}}}}";

    private static String generateTest(String specJson) throws AsyncApiParserException, GeneratorException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(specJson);
        WsClientConfig config = new WsClientConfig.Builder().withAsyncApi(spec).build();
        return new TestGenerator(config).generateTest();
    }

    private static String generateConfig(String specJson) throws AsyncApiParserException, GeneratorException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(specJson);
        WsClientConfig config = new WsClientConfig.Builder().withAsyncApi(spec).build();
        return new TestGenerator(config).generateConfig();
    }

    @Test
    void testGenerateTest_containsClientInitializer() throws AsyncApiParserException, GeneratorException {
        String source = generateTest(SPEC_WITH_ONE_OPERATION);
        Assert.assertTrue(source.contains("baseClient"),
                "generateTest() should emit a module-level variable named 'baseClient' for the client initializer");
    }

    @Test
    void testGenerateTest_containsOneTestFunctionPerMessage()
            throws AsyncApiParserException, GeneratorException {
        String source = generateTest(SPEC_WITH_ONE_OPERATION);
        Assert.assertTrue(source.contains("testChatMessage"),
                "generateTest() should emit a test function 'testChatMessage' for the ChatMessage operation");
    }

    @Test
    void testGenerateTest_returnsEmptyString_whenNoOperations()
            throws AsyncApiParserException, GeneratorException {
        String source = generateTest(SPEC_NO_OPERATIONS);
        Assert.assertEquals(source, "",
                "generateTest() should return an empty string when the spec has no operations");
    }

    @Test
    void testGenerateConfig_containsApiKeyConfigBlock_whenApiKeyScheme()
            throws AsyncApiParserException, GeneratorException {
        String config = generateConfig(SPEC_WITH_API_KEY_SCHEME);
        Assert.assertTrue(config.contains("[apiKeyConfig]"),
                "generateConfig() should emit '[apiKeyConfig]' header when an 'apiKey' security scheme is present");
        Assert.assertTrue(config.contains("myKey"),
                "generateConfig() should include the API-key field name 'myKey' from the security scheme");
    }

    @Test
    void testGenerateConfig_returnsEmptyString_whenNoApiKeySchemes()
            throws AsyncApiParserException, GeneratorException {
        String config = generateConfig(SPEC_NO_OPERATIONS);
        Assert.assertEquals(config, "",
                "generateConfig() should return an empty string when no 'apiKey' security schemes are present");
    }
}
