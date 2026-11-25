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

    private static String generateClient(String spec, List<SpecWarning> warnings) throws Exception {
        return generateFile(spec, warnings, "client.bal");
    }

    private static String generateFile(String spec, List<SpecWarning> warnings, String file) throws Exception {
        Path outDir = Files.createTempDirectory("ws-channels");
        try {
            new WsClientCodeGenerator(AsyncApiParser.parseFromJsonString(YAML.readTree(spec).toString()), null, false)
                    .withWarnings(warnings::add).generate(outDir);
            return Files.readString(outDir.resolve(file));
        } finally {
            try (Stream<Path> paths = Files.walk(outDir)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
            }
        }
    }
}
