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
package io.ballerina.asyncapi.generator.ws.client.extractor;

import io.ballerina.asyncapi.core.AsyncApiParser;
import io.ballerina.asyncapi.core.AsyncApiParserException;
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.generator.GeneratorException;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link ChannelExtractor} covering the happy path (exactly one channel
 * with WS bindings) and both error branches: no channels and more than one channel.
 */
public class ChannelExtractorTest {

    private static final String PREFIX =
            "{\"asyncapi\":\"2.0.0\",\"info\":{\"title\":\"T\",\"version\":\"1\"}";

    @Test
    void testExtract_returnsChannelInfo_whenExactlyOneChannel() throws AsyncApiParserException, GeneratorException {
        // Single channel "chat" with a WS query binding so query schema extraction is exercised
        String json = PREFIX + ",\"channels\":{\"chat\":{\"bindings\":{\"ws\":"
                + "{\"query\":{\"type\":\"object\","
                + "\"properties\":{\"token\":{\"type\":\"string\"}}}}}}}}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        ChannelExtractor.ChannelInfo info = new ChannelExtractor(spec).extract();

        Assert.assertNotNull(info,
                "ChannelInfo should not be null when exactly one channel is present");
        Assert.assertNotNull(info.channelName(),
                "Channel name should not be null");
        Assert.assertFalse(info.channelName().isBlank(),
                "Channel name should not be blank");
        Assert.assertNotNull(info.channel(),
                "AsyncApiChannel object should not be null");
        Assert.assertNotNull(info.querySchema(),
                "Query schema should be extracted from WS channel bindings");
        Assert.assertNull(info.headerSchema(),
                "Header schema should be null when not defined in WS channel bindings");
    }

    @Test
    void testExtract_throwsGeneratorException_whenNoChannels() throws AsyncApiParserException {
        String json = PREFIX + ",\"channels\":{}}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        try {
            new ChannelExtractor(spec).extract();
            Assert.fail("Expected GeneratorException when the channels map is empty");
        } catch (GeneratorException e) {
            Assert.assertNotNull(e.getMessage(),
                    "Exception message should not be null when the channels map is empty");
        }
    }

    @Test
    void testExtract_throwsGeneratorException_whenMoreThanOneChannel() throws AsyncApiParserException {
        String json = PREFIX + ",\"channels\":{\"chat\":{},\"notifications\":{}}}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        try {
            new ChannelExtractor(spec).extract();
            Assert.fail("Expected GeneratorException when more than one channel is defined");
        } catch (GeneratorException e) {
            Assert.assertNotNull(e.getMessage(),
                    "Exception message should not be null when more than one channel is defined");
        }
    }
}
