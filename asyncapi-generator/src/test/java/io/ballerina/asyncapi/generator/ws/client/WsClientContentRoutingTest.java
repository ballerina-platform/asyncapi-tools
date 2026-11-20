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
package io.ballerina.asyncapi.generator.ws.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.ballerina.asyncapi.core.AsyncApiParser;
import io.ballerina.asyncapi.core.SpecWarning;
import io.ballerina.asyncapi.generator.ws.client.utils.CodegenUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Checks that a WebSocket client tells apart the messages a connection receives by what they carry when the spec
 * has no dispatcher key: a field with its own fixed value on that connection, or else a field only one of them has.
 */
public class WsClientContentRoutingTest {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    // A price feed with no x-dispatcherKey; %s is the Note payload
    private static final String FEED = """
            asyncapi: 3.0.0
            info: {title: Feed, version: 1.0.0}
            servers: {s: {host: example.com, protocol: wss}}
            channels:
              feed:
                address: /feed
                messages:
                  Subscribe: {$ref: '#/components/messages/Subscribe'}
                  Trade: {$ref: '#/components/messages/Trade'}
                  Status: {$ref: '#/components/messages/Status'}
                  Note: {$ref: '#/components/messages/Note'}
            operations:
              subscribe:
                action: send
                channel: {$ref: '#/channels/feed'}
                messages: [{$ref: '#/channels/feed/messages/Subscribe'}]
              onTrade:
                action: receive
                channel: {$ref: '#/channels/feed'}
                messages: [{$ref: '#/channels/feed/messages/Trade'}]
              onStatus:
                action: receive
                channel: {$ref: '#/channels/feed'}
                messages: [{$ref: '#/channels/feed/messages/Status'}]
              onNote:
                action: receive
                channel: {$ref: '#/channels/feed'}
                messages: [{$ref: '#/channels/feed/messages/Note'}]
            components:
              messages:
                Subscribe:
                  payload:
                    {type: object, required: [symbols], properties: {symbols: {type: array, items: {type: string}}}}
                Trade:
                  payload:
                    type: object
                    required: [symbol, price]
                    properties: {symbol: {type: string}, price: {type: number}}
                Status:
                  payload:
                    type: object
                    required: [online]
                    properties: {online: {type: boolean}, symbol: {type: string}}
                Note:
                  payload: %s
            """;
    private static final String NOTE_WITH_OWN_FIELD =
            "{type: object, required: [text], properties: {text: {type: string}, symbol: {type: string}}}";
    private static final String NOTE_WITHOUT_OWN_FIELD =
            "{type: object, properties: {text: {type: string}, symbol: {type: string}}}";

    @Test
    void testMessagesAreRoutedByAFieldOnlyEachHas() throws Exception {
        List<SpecWarning> warnings = new ArrayList<>();
        String client = generateClient(FEED.formatted(NOTE_WITH_OWN_FIELD), warnings);
        Assert.assertTrue(client.contains("private isolated function getPipeNameByContent(map<json> message) "
                + "returns string? {"), client);
        Assert.assertTrue(client.indexOf("message.hasKey(\"price\")") < client.indexOf("message.hasKey(\"online\")")
                && client.contains("message.hasKey(\"text\")"), client);
        Assert.assertTrue(client.contains("json|error parsedFrame = frame.fromJsonString();"), client);
        Assert.assertTrue(client.contains("string? pipeName = self.getPipeNameByContent(parsedFrame);"), client);
        // Each type waits on its own pipe and reads the frame text as itself
        Assert.assertTrue(client.contains("string|pipe:Error responseMessage = self.pipes.getPipe(\"trade\")"
                + ".consume(timeout);"), client);
        Assert.assertTrue(client.contains("Trade|error trade = responseMessage.fromJsonStringWithType();"), client);
        Assert.assertEquals(warnings, List.of(new SpecWarning("/channels/feed",
                "the spec has no x-dispatcherKey, so the client tells apart the messages received on channel 'feed' "
                        + "by a field only each has: 'price' for 'Trade', 'online' for 'Status', 'text' for 'Note'",
                "Give each a field with its own fixed value and name it in x-dispatcherKey if a frame can carry "
                        + "another message's field. See " + CodegenUtils.DISPATCHER_KEY_DOCS)));
    }

    @Test
    void testMessageNothingTellsApartIsLeftOutWithAWarning() throws Exception {
        List<SpecWarning> warnings = new ArrayList<>();
        String client = generateClient(FEED.formatted(NOTE_WITHOUT_OWN_FIELD), warnings);
        Assert.assertTrue(client.contains("remote isolated function doTrade("), client);
        Assert.assertTrue(client.contains("remote isolated function doStatus("), client);
        Assert.assertFalse(client.contains("doNote"), client);
        Assert.assertEquals(warnings.size(), 2, warnings.toString());
        Assert.assertEquals(warnings.get(1), new SpecWarning("/operations/onNote",
                "message 'Note' received on channel 'feed' has no field only it has and no field with its own fixed "
                        + "value, so the client can't tell it apart from the others and leaves it out",
                "Give it a required field the channel's other messages don't declare, or give each of them a field "
                        + "with its own fixed value. See " + CodegenUtils.DISPATCHER_KEY_DOCS));
    }

    @Test
    void testEachChannelIsRoutedOnItsOwnKey() throws Exception {
        // 'type' tells apart what the room receives and 'kind' what presence receives, so no one key fits both
        String spec = """
                asyncapi: 3.0.0
                info: {title: Chat, version: 1.0.0}
                servers: {s: {host: example.com, protocol: wss}}
                channels:
                  room:
                    address: /room
                    messages:
                      Joined: {$ref: '#/components/messages/Joined'}
                      Said: {$ref: '#/components/messages/Said'}
                  presence:
                    address: /presence
                    messages:
                      Online: {$ref: '#/components/messages/Online'}
                      Offline: {$ref: '#/components/messages/Offline'}
                operations:
                  onJoined:
                    action: receive
                    channel: {$ref: '#/channels/room'}
                    messages: [{$ref: '#/channels/room/messages/Joined'}]
                  onSaid:
                    action: receive
                    channel: {$ref: '#/channels/room'}
                    messages: [{$ref: '#/channels/room/messages/Said'}]
                  onOnline:
                    action: receive
                    channel: {$ref: '#/channels/presence'}
                    messages: [{$ref: '#/channels/presence/messages/Online'}]
                  onOffline:
                    action: receive
                    channel: {$ref: '#/channels/presence'}
                    messages: [{$ref: '#/channels/presence/messages/Offline'}]
                components:
                  messages:
                    Joined: {payload: {$ref: '#/components/schemas/Joined'}}
                    Said: {payload: {$ref: '#/components/schemas/Said'}}
                    Online: {payload: {$ref: '#/components/schemas/Online'}}
                    Offline: {payload: {$ref: '#/components/schemas/Offline'}}
                  schemas:
                    Joined: {type: object, required: [type], properties: {type: {type: string, const: joined}}}
                    Said: {type: object, required: [type], properties: {type: {type: string, const: said}}}
                    Online: {type: object, required: [kind], properties: {kind: {type: string, const: online}}}
                    Offline: {type: object, required: [kind], properties: {kind: {type: string, const: offline}}}
                """;
        List<SpecWarning> warnings = new ArrayList<>();
        String client = generateClient(spec, warnings);
        Assert.assertTrue(client.contains("if message[\"type\"] == \"said\" {"), client);
        Assert.assertTrue(client.contains("if message[\"kind\"] == \"offline\" {"), client);
        Assert.assertEquals(warnings.size(), 2, warnings.toString());
        Assert.assertTrue(warnings.get(0).message().contains("on channel 'presence' on 'kind', the field with a "
                + "different fixed value in each"), warnings.toString());
    }

    @Test
    void testTextFramesStillReachTheirFunction() throws Exception {
        // A text frame isn't a JSON object, so it takes the path a frame without a key takes
        String spec = FEED.formatted("{type: string}");
        List<SpecWarning> warnings = new ArrayList<>();
        String client = generateClient(spec, warnings);
        Assert.assertTrue(client.contains("remote isolated function doNote("), client);
        Assert.assertTrue(client.contains("\"#noDispatcherKey\" : \"#noDispatcherKeyEvents\").produce(frame, 5);"),
                client);
    }

    private static String generateClient(String spec, List<SpecWarning> warnings) throws Exception {
        Path outDir = Files.createTempDirectory("ws-content-routing");
        try {
            new WsClientCodeGenerator(AsyncApiParser.parseFromJsonString(YAML.readTree(spec).toString()), null, false)
                    .withWarnings(warnings::add).generate(outDir);
            return Files.readString(outDir.resolve("client.bal")).replace("\r\n", "\n");
        } finally {
            try (Stream<Path> paths = Files.walk(outDir)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
            }
        }
    }
}
