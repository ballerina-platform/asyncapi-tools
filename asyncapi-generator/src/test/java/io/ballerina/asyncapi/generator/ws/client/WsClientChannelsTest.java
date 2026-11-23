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
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.ws.client.model.InferredKeySpec;
import io.ballerina.asyncapi.generator.ws.client.model.KeyPathSpec;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Checks that a spec with several channels gets one WebSocket client per channel, each with only its own
 * channel's functions and parameters.
 */
public class WsClientChannelsTest {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    // A chat room the user talks in, and a presence feed on its own connection
    private static final String SPEC = """
            asyncapi: 3.0.0
            info: {title: Chat, version: 1.0.0}
            x-dispatcherKey: type
            servers:
              s: {host: example.com, protocol: wss}
            channels:
              room:
                address: /rooms/{roomId}
                parameters: {roomId: {}}
                messages:
                  Say: {$ref: '#/components/messages/Say'}
                  Said: {$ref: '#/components/messages/Said'}
              presence:
                address: /presence
                messages:
                  Online: {$ref: '#/components/messages/Online'}
            operations:
              sendSay:
                action: send
                channel: {$ref: '#/channels/room'}
                messages: [{$ref: '#/channels/room/messages/Say'}]
              receiveSaid:
                action: receive
                channel: {$ref: '#/channels/room'}
                messages: [{$ref: '#/channels/room/messages/Said'}]
              receiveOnline:
                action: receive
                channel: {$ref: '#/channels/presence'}
                messages: [{$ref: '#/channels/presence/messages/Online'}]
            components:
              messages:
                Say:
                  payload: {type: object, required: [type], properties: {type: {type: string, const: say}}}
                Said:
                  payload: {type: object, required: [type], properties: {type: {type: string, const: said}}}
                Online:
                  payload: {type: object, required: [type], properties: {type: {type: string, const: online}}}
            """;

    @Test
    void testEachChannelGetsItsOwnClient() throws Exception {
        List<SpecWarning> warnings = new ArrayList<>();
        String client = generateClient(SPEC, warnings);
        int presence = client.indexOf("class ChatPresenceClient {");
        int room = client.indexOf("class ChatRoomClient {");
        Assert.assertTrue(presence >= 0 && room > presence, client);

        String presenceClient = client.substring(presence, room);
        String roomClient = client.substring(room);
        Assert.assertTrue(presenceClient.contains("remote isolated function doOnline("), presenceClient);
        Assert.assertFalse(presenceClient.contains("doSay(") || presenceClient.contains("doSaid("), presenceClient);
        Assert.assertTrue(roomClient.contains("remote isolated function doSay("), roomClient);
        Assert.assertTrue(roomClient.contains("remote isolated function doSaid("), roomClient);
        Assert.assertFalse(roomClient.contains("doOnline("), roomClient);
        Assert.assertEquals(warnings, List.of(), warnings.toString());
    }

    @Test
    void testParameterRecordsAreNamedAfterTheirChannel() throws Exception {
        String client = generateClient(SPEC, new ArrayList<>());
        Assert.assertTrue(client.contains("public type RoomPathParams record {|"), client);
        Assert.assertTrue(client.contains("RoomPathParams pathParams"), client);
        Assert.assertFalse(client.contains("type PathParams"), client);
    }

    @Test
    void testChannelWithoutOperationsGetsNoClient() throws Exception {
        String spec = SPEC.replace("""
                  receiveOnline:
                    action: receive
                    channel: {$ref: '#/channels/presence'}
                    messages: [{$ref: '#/channels/presence/messages/Online'}]
                """, "");
        Assert.assertFalse(spec.contains("receiveOnline"), spec);
        List<SpecWarning> warnings = new ArrayList<>();
        String client = generateClient(spec, warnings);
        Assert.assertFalse(client.contains("ChatPresenceClient"), client);
        Assert.assertTrue(client.contains("class ChatRoomClient {"), client);
        Assert.assertEquals(warnings, List.of(new SpecWarning("/channels/presence",
                "channel 'presence' has no operations, so no client is generated for it",
                "Add a send or receive operation on the channel to get a client for it.")));
    }

    @Test
    void testHelpersAreAddedForAnyChannelThatNeedsThem() throws Exception {
        // Only 'room', the second client, has a path parameter
        String utils = generateFile(SPEC, new ArrayList<>(), "utils.bal");
        Assert.assertTrue(utils.contains("function getEncodedUri("), utils);
    }

    @Test
    void testEachChannelConnectsToItsOwnServer() throws Exception {
        String spec = """
                asyncapi: 3.0.0
                info: {title: Split, version: 1.0.0}
                x-dispatcherKey: type
                servers:
                  rooms: {host: rooms.example.com, protocol: wss}
                  alerts: {host: alerts.example.com, protocol: wss}
                channels:
                  rooms:
                    address: /rooms
                    servers: [{$ref: '#/servers/rooms'}]
                    messages:
                      Ping: {payload: {type: object, required: [type], properties: {type: {type: string, const: ping}}}}
                  alerts:
                    address: /alerts
                    servers: [{$ref: '#/servers/alerts'}]
                    messages:
                      Alert: {payload: {type: object, required: [type], properties: {type: {type: string, const: a}}}}
                operations:
                  sendPing: {action: send, channel: {$ref: '#/channels/rooms'}}
                  onAlert: {action: receive, channel: {$ref: '#/channels/alerts'}}
                """;
        String client = generateClient(spec, new ArrayList<>());
        int alerts = client.indexOf("class SplitAlertsClient {");
        int rooms = client.indexOf("class SplitRoomsClient {");
        Assert.assertTrue(alerts >= 0 && rooms > alerts, client);
        String alertsClient = client.substring(alerts, rooms);
        Assert.assertTrue(alertsClient.contains("\"wss://alerts.example.com\""), alertsClient);
        Assert.assertFalse(alertsClient.contains("rooms.example.com"), alertsClient);
        Assert.assertTrue(client.substring(rooms).contains("\"wss://rooms.example.com\""), client);
    }

    @Test
    void testChannelNamesGiveValidDistinctRecordNames() throws Exception {
        String spec = """
                asyncapi: 3.0.0
                info: {title: Names, version: 1.0.0}
                x-dispatcherKey: type
                servers:
                  prod: {host: example.com, protocol: wss}
                channels:
                  chat-room: {address: '/a/{x}', parameters: {x: {}}, messages: {A: {payload: {type: string}}}}
                  chat_room: {address: '/b/{y}', parameters: {y: {}}, messages: {B: {payload: {type: string}}}}
                  "123": {address: '/c/{z}', parameters: {z: {}}, messages: {C: {payload: {type: string}}}}
                operations:
                  sendA: {action: send, channel: {$ref: '#/channels/chat-room'}}
                  sendB: {action: send, channel: {$ref: '#/channels/chat_room'}}
                  sendC: {action: send, channel: {$ref: '#/channels/123'}}
                """;
        String client = generateClient(spec, new ArrayList<>());
        Assert.assertTrue(client.contains("public type ChatRoomPathParams record {|\n    string x;"), client);
        Assert.assertTrue(client.contains("public type ChatRoom2PathParams record {|\n    string y;"), client);
        Assert.assertTrue(client.contains("public type Channel123PathParams record {|\n    string z;"), client);
    }

    @Test
    void testV2ChannelGivenAsARefKeepsItsOperations() throws Exception {
        String spec = """
                asyncapi: 2.6.0
                info: {title: V2Ref, version: 1.0.0}
                x-dispatcherKey: type
                servers:
                  prod: {url: example.com, protocol: wss}
                channels:
                  /chat: {$ref: '#/components/channels/Chat'}
                components:
                  channels:
                    Chat:
                      publish:
                        message:
                          name: Ping
                          payload: {type: object, required: [type], properties: {type: {type: string, const: ping}}}
                """;
        String client = generateClient(spec, new ArrayList<>());
        Assert.assertTrue(client.contains("remote isolated function doPing("), client);
    }

    @Test
    void testSchemasThatIncludeEachOtherStopWithTheCycle() throws Exception {
        // Base and JoinBody include each other through allOf, which no Ballerina record can do
        String spec = CYCLE;
        GeneratorException stop = Assert.expectThrows(GeneratorException.class,
                () -> generateClient(spec, new ArrayList<>()));
        Assert.assertEquals(stop.getMessage(), "schemas 'Base' and 'JoinBody' include each other through allOf "
                + "(Base -> JoinBody -> Base), and a Ballerina record can't include itself\n"
                + "Remove 'Base' from the allOf of 'JoinBody' at /components/schemas/JoinBody, and add the fields it "
                + "needs from 'Base' to its properties.");
    }

    @Test
    void testKeyInferenceEndsOnSchemasThatIncludeEachOther() throws Exception {
        // Inference runs on the spec as given, so it mustn't walk the allOf cycle for ever
        AsyncApiSpec parsed = AsyncApiParser.parseFromJsonString(YAML.readTree(CYCLE).toString());
        AsyncApiSpec keyed = InferredKeySpec.of(KeyPathSpec.of(parsed), warning -> { });
        Assert.assertNotNull(keyed);
    }

    // Base and JoinBody include each other, and neither names a key
    private static final String CYCLE = """
                asyncapi: 3.0.0
                info: {title: Cycle, version: 1.0.0}
                servers:
                  prod: {host: example.com, protocol: wss}
                channels:
                  room:
                    address: /room
                    messages:
                      Join: {payload: {$ref: '#/components/schemas/JoinBody'}}
                      Leave: {payload: {$ref: '#/components/schemas/LeaveBody'}}
                operations:
                  onRoom:
                    action: receive
                    channel: {$ref: '#/channels/room'}
                    messages: [{$ref: '#/channels/room/messages/Join'}, {$ref: '#/channels/room/messages/Leave'}]
                components:
                  schemas:
                    Base: {allOf: [{$ref: '#/components/schemas/JoinBody'}], properties: {id: {type: string}}}
                    JoinBody:
                      allOf: [{$ref: '#/components/schemas/Base'}]
                      type: object
                      properties: {type: {type: string, const: join}}
                    LeaveBody: {type: object, required: [type], properties: {type: {type: string, const: leave}}}
                """;

    private static String generateClient(String spec, List<SpecWarning> warnings) throws Exception {
        return generateFile(spec, warnings, "client.bal");
    }

    private static String generateFile(String spec, List<SpecWarning> warnings, String file) throws Exception {
        Path outDir = Files.createTempDirectory("ws-channels");
        try {
            new WsClientCodeGenerator(AsyncApiParser.parseFromJsonString(YAML.readTree(spec).toString()), null, false)
                    .withWarnings(warnings::add).generate(outDir);
            // Line ends follow the OS, so the checks see them as \n
            return Files.readString(outDir.resolve(file)).replace("\r\n", "\n");
        } finally {
            try (Stream<Path> paths = Files.walk(outDir)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
            }
        }
    }
}
