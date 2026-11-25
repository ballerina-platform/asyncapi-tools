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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.ballerina.asyncapi.core.AsyncApiParser;
import io.ballerina.asyncapi.core.AsyncApiParserException;
import io.ballerina.asyncapi.core.SpecWarning;
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.generator.GeneratorException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Unit tests for {@link ChannelExtractor}: one channel with WS bindings, no channels, and a spec with several
 * channels, where each gets its own client.
 */
public class ChannelExtractorTest {

    private static final String PREFIX =
            "{\"asyncapi\":\"2.0.0\",\"info\":{\"title\":\"T\",\"version\":\"1\"}";

    // Two connections: a chat room with a send and a receive, and a presence feed
    private static final String TWO_CHANNELS = """
            asyncapi: 3.0.0
            info: {title: Chat, version: 1.0.0}
            channels:
              room:
                address: /rooms/{roomId}
                parameters: {roomId: {}}
                messages:
                  say: {payload: {type: object, properties: {type: {type: string, const: say}}}}
                  said: {payload: {type: object, properties: {type: {type: string, const: said}}}}
              presence:
                address: /presence
                messages:
                  online: {payload: {type: object, properties: {type: {type: string, const: online}}}}
            operations:
              sendSay:
                {action: send, channel: {$ref: '#/channels/room'}, messages: [{$ref: '#/channels/room/messages/say'}]}
              onSaid:
                action: receive
                channel: {$ref: '#/channels/room'}
                messages: [{$ref: '#/channels/room/messages/said'}]
              onOnline:
                action: receive
                channel: {$ref: '#/channels/presence'}
                messages: [{$ref: '#/channels/presence/messages/online'}]
            """;

    @Test
    void testExtract_returnsChannelInfo_whenExactlyOneChannel() throws AsyncApiParserException, GeneratorException {
        // Single channel "chat" with a WS query binding so query schema extraction is exercised
        String json = PREFIX + ",\"channels\":{\"chat\":{\"bindings\":{\"ws\":"
                + "{\"query\":{\"type\":\"object\","
                + "\"properties\":{\"token\":{\"type\":\"string\"}}}}}}}}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        ChannelExtractor.ChannelInfo info = new ChannelExtractor(spec).extract(warning -> { }).get(0);

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
            new ChannelExtractor(spec).extract(warning -> { });
            Assert.fail("Expected GeneratorException when the channels map is empty");
        } catch (GeneratorException e) {
            Assert.assertNotNull(e.getMessage(),
                    "Exception message should not be null when the channels map is empty");
        }
    }

    @Test
    void testExtract_throwsGeneratorException_whenNoneOfSeveralChannelsHasAnOperation()
            throws AsyncApiParserException {
        String json = PREFIX + ",\"channels\":{\"chat\":{},\"notifications\":{}}}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        GeneratorException e = Assert.expectThrows(GeneratorException.class,
                () -> new ChannelExtractor(spec).extract(warning -> { }));
        Assert.assertTrue(e.getMessage().contains("None of the 2 channels"), e.getMessage());
    }

    @Test
    void testEachChannelGetsOnlyItsOwnOperations() throws Exception {
        List<ChannelExtractor.ChannelInfo> channels = new ChannelExtractor(spec(TWO_CHANNELS)).extract(warning -> {
            throw new AssertionError("no channel is left out: " + warning.message());
        });
        Assert.assertEquals(channels.stream().map(ChannelExtractor.ChannelInfo::channelName).toList(),
                List.of("presence", "room"), "clients come in channel name order");
        Assert.assertEquals(operationIds(channels.get(0)), Set.of("onOnline"));
        Assert.assertEquals(operationIds(channels.get(1)), Set.of("sendSay", "onSaid"));
        Assert.assertEquals(channels.get(1).spec().getAsyncApiChannels().orElseThrow().keySet(), Set.of("room"));
    }

    @Test
    void testChannelWithoutOperationsIsLeftOutWithAWarning() throws Exception {
        String spec = TWO_CHANNELS.substring(0, TWO_CHANNELS.indexOf("  onOnline:"));
        List<SpecWarning> warnings = new ArrayList<>();
        List<ChannelExtractor.ChannelInfo> channels = new ChannelExtractor(spec(spec)).extract(warnings::add);
        Assert.assertEquals(channels.size(), 1);
        Assert.assertEquals(channels.get(0).channelName(), "room");
        Assert.assertEquals(warnings.size(), 1, warnings.toString());
        Assert.assertEquals(warnings.get(0).location(), "/channels/presence");
        Assert.assertTrue(warnings.get(0).message().contains("channel 'presence' has no operations"),
                warnings.get(0).message());
    }

    @Test
    void testSingleChannelIsUsedAsIs() throws Exception {
        String spec = TWO_CHANNELS.substring(0, TWO_CHANNELS.indexOf("  presence:"))
                + TWO_CHANNELS.substring(TWO_CHANNELS.indexOf("operations:"), TWO_CHANNELS.indexOf("  onOnline:"));
        AsyncApiSpec parsed = spec(spec);
        ChannelExtractor.ChannelInfo info = new ChannelExtractor(parsed).extract(warning -> { }).get(0);
        Assert.assertSame(info.spec(), parsed, "a spec with one channel generates as it did before");
    }

    private static AsyncApiSpec spec(String yaml) throws Exception {
        return AsyncApiParser.parseFromJsonString(new ObjectMapper(new YAMLFactory()).readTree(yaml).toString());
    }

    private static Set<String> operationIds(ChannelExtractor.ChannelInfo info) {
        return info.spec().getAsyncApiOperations().orElseThrow().keySet();
    }
}
