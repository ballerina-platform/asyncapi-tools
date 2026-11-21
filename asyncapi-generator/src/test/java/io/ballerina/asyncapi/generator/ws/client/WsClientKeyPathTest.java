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
import io.ballerina.asyncapi.generator.GeneratorException;
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
 * Checks dispatcher keys named as paths: a nested x-dispatcherKey such as {@code /data/type}, a message's own
 * x-dispatcherKey, a pointer to a top-level field, and a message told apart by a field it fixes to a value no other
 * message can carry.
 */
public class WsClientKeyPathTest {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    // A gateway whose events carry their kind inside 'data'; the heartbeat ack names its own key. %s is the key,
    // then Posted's data.type
    private static final String GATEWAY = """
            asyncapi: 3.0.0
            info: {title: Gateway, version: 1.0.0}
            servers: {s: {host: example.com, protocol: wss}}
            x-dispatcherKey: %s
            channels:
              gateway:
                address: /gateway
                messages:
                  Identify: {$ref: '#/components/messages/Identify'}
                  Ready: {$ref: '#/components/messages/Ready'}
                  Posted: {$ref: '#/components/messages/Posted'}
                  Ack: {$ref: '#/components/messages/Ack'}
            operations:
              identify:
                action: send
                channel: {$ref: '#/channels/gateway'}
                messages: [{$ref: '#/channels/gateway/messages/Identify'}]
              onReady:
                action: receive
                channel: {$ref: '#/channels/gateway'}
                messages: [{$ref: '#/channels/gateway/messages/Ready'}]
              onPosted:
                action: receive
                channel: {$ref: '#/channels/gateway'}
                messages: [{$ref: '#/channels/gateway/messages/Posted'}]
              onAck:
                action: receive
                channel: {$ref: '#/channels/gateway'}
                messages: [{$ref: '#/channels/gateway/messages/Ack'}]
            components:
              messages:
                Identify:
                  payload: {type: object, required: [token], properties: {token: {type: string}}}
                Ready:
                  payload:
                    type: object
                    properties:
                      data:
                        type: object
                        properties: {type: {type: string, const: ready}, session: {type: string}}
                Posted:
                  payload:
                    type: object
                    properties:
                      data:
                        type: object
                        properties: {type: %s, text: {type: string}}
                Ack:
                  x-dispatcherKey: /ack
                  payload: {type: object, properties: {ack: {type: string, const: heartbeat}}}
            """;

    @Test
    void testNestedKeyRoutesEachMessageByTheValueAtItsPath() throws Exception {
        List<SpecWarning> warnings = new ArrayList<>();
        String client = generateClient(GATEWAY.formatted("/data/type", "{type: string, const: posted}"), warnings);
        Assert.assertTrue(client.contains("if self.getValueAt(message, [\"data\", \"type\"]) == \"ready\" {"), client);
        Assert.assertTrue(client.contains("if self.getValueAt(message, [\"data\", \"type\"]) == \"posted\" {"),
                client);
        // The ack is routed on the key it names for itself, a top-level field
        Assert.assertTrue(client.contains("if message[\"ack\"] == \"heartbeat\" {"), client);
        Assert.assertTrue(client.contains("private isolated function getValueAt(map<json> message, string[] path) "
                + "returns json {"), client);
        Assert.assertTrue(client.contains("remote isolated function doPosted("), client);
        // Keys the spec names need no warning
        Assert.assertEquals(warnings, List.of());
    }

    @Test
    void testMessageWithoutAValueAtItsKeyIsLeftOut() throws Exception {
        List<SpecWarning> warnings = new ArrayList<>();
        String client = generateClient(GATEWAY.formatted("/data/type", "{type: string}"), warnings);
        Assert.assertTrue(client.contains("remote isolated function doReady("), client);
        Assert.assertFalse(client.contains("doPosted"), client);
        Assert.assertEquals(warnings, List.of(new SpecWarning("/operations/onPosted",
                "message 'Posted' received on channel 'gateway' has no fixed value of its own at '/data/type', the "
                        + "dispatcher key it's routed on, so the client can't tell it apart from the others and "
                        + "leaves it out",
                "Give its payload a fixed value of its own at '/data/type', such as:\n  data:\n    type: object\n"
                        + "    properties:\n      type: {type: string, const: posted}\nSee "
                        + CodegenUtils.DISPATCHER_KEY_DOCS)));
    }

    @Test
    void testPointerToATopLevelFieldIsThatField() throws Exception {
        String spec = """
                asyncapi: 3.0.0
                info: {title: Chat, version: 1.0.0}
                servers: {s: {host: example.com, protocol: wss}}
                x-dispatcherKey: %s
                channels:
                  room:
                    address: /room
                    messages:
                      Joined: {$ref: '#/components/messages/Joined'}
                      Said: {$ref: '#/components/messages/Said'}
                operations:
                  onJoined:
                    action: receive
                    channel: {$ref: '#/channels/room'}
                    messages: [{$ref: '#/channels/room/messages/Joined'}]
                  onSaid:
                    action: receive
                    channel: {$ref: '#/channels/room'}
                    messages: [{$ref: '#/channels/room/messages/Said'}]
                components:
                  messages:
                    Joined:
                      payload: {type: object, required: [type], properties: {type: {type: string, const: joined}}}
                    Said:
                      payload: {type: object, required: [type], properties: {type: {type: string, const: said}}}
                """;
        Assert.assertEquals(generateClient(spec.formatted("/type"), new ArrayList<>()),
                generateClient(spec.formatted("type"), new ArrayList<>()));
    }

    @Test
    void testKeyThatIsNoPathStops() {
        GeneratorException stop = Assert.expectThrows(GeneratorException.class, () -> generateClient(
                GATEWAY.formatted("/data//type", "{type: string, const: posted}"), new ArrayList<>()));
        Assert.assertEquals(stop.getMessage(), "x-dispatcherKey '/data//type' at the spec's top level is not a "
                + "field name or a JSON Pointer to one, such as 'type' or '/data/type'. See "
                + CodegenUtils.DISPATCHER_KEY_DOCS);
    }

    @Test
    void testMessageIsRoutedOnAFieldItFixesThatNoOtherCanCarry() throws Exception {
        // No field has a fixed value in all three, but 'status' is only Online's and each fixes 'kind' differently
        String spec = """
                asyncapi: 3.0.0
                info: {title: Market, version: 1.0.0}
                servers: {s: {host: example.com, protocol: wss}}
                channels:
                  market:
                    address: /market
                    messages:
                      Online: {$ref: '#/components/messages/Online'}
                      Trade: {$ref: '#/components/messages/Trade'}
                      Book: {$ref: '#/components/messages/Book'}
                operations:
                  onOnline:
                    action: receive
                    channel: {$ref: '#/channels/market'}
                    messages: [{$ref: '#/channels/market/messages/Online'}]
                  onTrade:
                    action: receive
                    channel: {$ref: '#/channels/market'}
                    messages: [{$ref: '#/channels/market/messages/Trade'}]
                  onBook:
                    action: receive
                    channel: {$ref: '#/channels/market'}
                    messages: [{$ref: '#/channels/market/messages/Book'}]
                components:
                  messages:
                    Online: {payload: {type: object, properties: {status: {type: string, const: online}}}}
                    Trade:
                      payload: {type: object, properties: {kind: {type: string, const: trade}, price: {type: number}}}
                    Book:
                      payload: {type: object, properties: {kind: {type: string, const: book}, bids: {type: array}}}
                """;
        List<SpecWarning> warnings = new ArrayList<>();
        String client = generateClient(spec, warnings);
        Assert.assertTrue(client.contains("if message[\"status\"] == \"online\" {"), client);
        Assert.assertTrue(client.contains("if message[\"kind\"] == \"trade\" {"), client);
        Assert.assertTrue(client.contains("if message[\"kind\"] == \"book\" {"), client);
        Assert.assertFalse(client.contains("getValueAt"), client);
        Assert.assertEquals(warnings, List.of(new SpecWarning("/channels/market",
                "the spec has no x-dispatcherKey, so the client tells apart the messages received on channel "
                        + "'market' by a field only each has: 'status' = 'online' for 'Online', 'kind' = 'trade' for "
                        + "'Trade', 'kind' = 'book' for 'Book'",
                "Give each a field with its own fixed value and name it in x-dispatcherKey if a frame can carry "
                        + "another message's field. See " + CodegenUtils.DISPATCHER_KEY_DOCS)));
    }

    private static String generateClient(String spec, List<SpecWarning> warnings) throws Exception {
        Path outDir = Files.createTempDirectory("ws-key-path");
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
