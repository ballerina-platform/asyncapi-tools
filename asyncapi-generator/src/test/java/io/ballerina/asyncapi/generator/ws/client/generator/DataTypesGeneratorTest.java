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
 * Integration tests for {@link DataTypesGenerator} verifying that {@code generate()} produces
 * a Ballerina source containing the synthetic {@code Message} type, optional {@code MessageWithId},
 * one type per component schema, and that close-frame schemas are excluded.
 */
public class DataTypesGeneratorTest {

    private static final String SPEC_BASE =
            "{\"asyncapi\":\"2.0.0\",\"info\":{\"title\":\"T\",\"version\":\"1\"},\"channels\":{}}";

    private static final String SPEC_WITH_STREAM_ID =
            "{\"asyncapi\":\"2.0.0\",\"info\":{\"title\":\"T\",\"version\":\"1\"},"
                    + "\"x-dispatcherStreamId\":\"streamId\",\"channels\":{}}";

    private static final String SPEC_WITH_TWO_SCHEMAS =
            "{\"asyncapi\":\"2.0.0\",\"info\":{\"title\":\"T\",\"version\":\"1\"},\"channels\":{},"
                    + "\"components\":{\"schemas\":{"
                    + "\"Foo\":{\"type\":\"object\"},"
                    + "\"Bar\":{\"type\":\"object\"}}}}";

    private static final String SPEC_WITH_CLOSE_FRAME_SCHEMA =
            "{\"asyncapi\":\"2.0.0\",\"info\":{\"title\":\"T\",\"version\":\"1\"},\"channels\":{},"
                    + "\"components\":{\"schemas\":{"
                    + "\"CloseFrame\":{\"type\":\"object\",\"x-close-frame\":true},"
                    + "\"ValidSchema\":{\"type\":\"object\"}}}}";

    private static String generate(String specJson) throws AsyncApiParserException, GeneratorException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(specJson);
        WsClientConfig config = new WsClientConfig.Builder().withAsyncApi(spec).build();
        return new DataTypesGenerator(config).generate();
    }

    @Test
    void testGenerate_alwaysContainsMessageType() throws AsyncApiParserException, GeneratorException {
        String source = generate(SPEC_BASE);
        Assert.assertTrue(source.contains("Message"),
                "generate() should always emit the synthetic 'Message' type regardless of component schemas");
    }

    @Test
    void testGenerate_containsMessageWithIdType_whenStreamIdPresent()
            throws AsyncApiParserException, GeneratorException {
        String source = generate(SPEC_WITH_STREAM_ID);
        Assert.assertTrue(source.contains("MessageWithId"),
                "generate() should emit 'MessageWithId' when the x-dispatcherStreamId extension is present");
        Assert.assertTrue(source.contains("streamId"),
                "generate() should embed the actual stream ID field name 'streamId' in MessageWithId");
    }

    @Test
    void testGenerate_containsOneTypePerSchema() throws AsyncApiParserException, GeneratorException {
        String source = generate(SPEC_WITH_TWO_SCHEMAS);
        Assert.assertTrue(source.contains("Foo"),
                "generate() should emit a Ballerina type definition for the component schema 'Foo'");
        Assert.assertTrue(source.contains("Bar"),
                "generate() should emit a Ballerina type definition for the component schema 'Bar'");
    }

    @Test
    void testGenerate_skipsCloseFrameSchemas() throws AsyncApiParserException, GeneratorException {
        String source = generate(SPEC_WITH_CLOSE_FRAME_SCHEMA);
        Assert.assertTrue(source.contains("ValidSchema"),
                "generate() should include 'ValidSchema' which has no x-close-frame extension");
        Assert.assertFalse(source.contains("CloseFrame"),
                "generate() must skip 'CloseFrame' because its x-close-frame extension marks it as a close frame");
    }
}
