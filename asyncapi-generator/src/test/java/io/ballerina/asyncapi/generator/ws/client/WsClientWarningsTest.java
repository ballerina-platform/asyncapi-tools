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
 * Checks the warnings a WebSocket client generation reports for the parts of a spec it leaves out.
 */
public class WsClientWarningsTest {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    @Test
    void testSentMessageWithoutPayloadIsLeftOutWithAWarning() throws Exception {
        String spec = """
                asyncapi: 3.0.0
                info: {title: Echo, version: 1.0.0}
                x-dispatcherKey: type
                servers:
                  s: {host: example.com, protocol: wss}
                channels:
                  ws:
                    address: /ws
                    messages:
                      ping:
                        payload:
                          type: object
                          required: [type]
                          properties: {type: {type: string}}
                      echo: {contentType: text/plain}
                operations:
                  sendPing:
                    action: send
                    channel: {$ref: '#/channels/ws'}
                    messages: [{$ref: '#/channels/ws/messages/ping'}]
                  send/echo:
                    action: send
                    channel: {$ref: '#/channels/ws'}
                    messages: [{$ref: '#/channels/ws/messages/echo'}]
                """;
        List<SpecWarning> warnings = new ArrayList<>();
        String client = generateClient(spec, warnings);
        Assert.assertTrue(client.contains("remote isolated function doPing("), client);
        Assert.assertFalse(client.contains("'null"), client);
        Assert.assertEquals(warnings, List.of(new SpecWarning("/operations/send~1echo",
                "message 'echo' has no JSON Schema payload, so the client has no function to send it",
                "Give the message a JSON Schema 'payload' to get a function for it.")));
    }

    @Test
    void testReceivedMessageWithoutPayloadIsLeftOutWithAWarning() throws Exception {
        String spec = """
                asyncapi: 3.0.0
                info: {title: Chat, version: 1.0.0}
                x-dispatcherKey: type
                servers:
                  s: {host: example.com, protocol: wss}
                channels:
                  ws:
                    address: /ws
                    messages:
                      join: {$ref: '#/components/messages/Join'}
                      notice: {$ref: '#/components/messages/Notice'}
                operations:
                  sendJoin:
                    action: send
                    channel: {$ref: '#/channels/ws'}
                    messages: [{$ref: '#/channels/ws/messages/join'}]
                  receiveNotice:
                    action: receive
                    channel: {$ref: '#/channels/ws'}
                    messages: [{$ref: '#/channels/ws/messages/notice'}]
                components:
                  messages:
                    Join:
                      payload:
                        type: object
                        required: [type]
                        properties: {type: {type: string, const: join}, room: {type: string}}
                    Notice: {contentType: text/plain}
                """;
        List<SpecWarning> warnings = new ArrayList<>();
        String client = generateClient(spec, warnings);
        Assert.assertTrue(client.contains("remote isolated function doJoin("), client);
        Assert.assertFalse(client.contains("Notice"), client);
        Assert.assertEquals(warnings, List.of(new SpecWarning("/operations/receiveNotice",
                "message 'notice' has no JSON Schema payload, so the client has no type to receive it as",
                "Give the message a JSON Schema 'payload' to get a type for it.")));
    }

    @Test
    void testLeftOutSendGetsNoReplyRoute() throws Exception {
        String spec = """
                asyncapi: 3.0.0
                info: {title: Echo, version: 1.0.0}
                x-dispatcherKey: type
                servers:
                  s: {host: example.com, protocol: wss}
                channels:
                  ws:
                    address: /ws
                    messages:
                      ping: {$ref: '#/components/messages/Ping'}
                      echo: {$ref: '#/components/messages/Echo'}
                      pong: {$ref: '#/components/messages/Pong'}
                operations:
                  sendPing:
                    action: send
                    channel: {$ref: '#/channels/ws'}
                    messages: [{$ref: '#/channels/ws/messages/ping'}]
                  sendEcho:
                    action: send
                    channel: {$ref: '#/channels/ws'}
                    messages: [{$ref: '#/channels/ws/messages/echo'}]
                components:
                  messages:
                    Ping:
                      payload:
                        type: object
                        required: [type]
                        properties: {type: {type: string, const: ping}}
                      x-response: {$ref: '#/components/messages/Pong'}
                    Echo:
                      contentType: text/plain
                      x-response: {$ref: '#/components/messages/Pong'}
                    Pong:
                      payload:
                        type: object
                        required: [type]
                        properties: {type: {type: string, const: pong}}
                """;
        List<SpecWarning> warnings = new ArrayList<>();
        String client = generateClient(spec, warnings);
        Assert.assertTrue(client.contains(
                "remote isolated function doPing(Ping ping, decimal timeout) returns Pong|error"), client);
        Assert.assertEquals(warnings.size(), 1, warnings.toString());
    }

    @Test
    void testSchemeInTheHostStaysInTheServiceUrl() throws Exception {
        String spec = """
                asyncapi: 3.0.0
                info: {title: Echo, version: 1.0.0}
                x-dispatcherKey: type
                servers:
                  s: {host: 'wss://example.com/ws'}
                channels:
                  ws:
                    messages:
                      ping:
                        payload:
                          type: object
                          required: [type]
                          properties: {type: {type: string, const: ping}}
                operations:
                  sendPing:
                    action: send
                    channel: {$ref: '#/channels/ws'}
                    messages: [{$ref: '#/channels/ws/messages/ping'}]
                """;
        String client = generateClient(spec, new ArrayList<>());
        Assert.assertTrue(client.contains("\"wss://example.com/ws\""), client);
    }

    @Test
    void testPayloadInAnotherFileSaysToBundleTheSpec() throws Exception {
        String spec = """
                asyncapi: 3.0.0
                info: {title: Feed, version: 1.0.0}
                servers:
                  s: {host: example.com, protocol: wss}
                channels:
                  feed:
                    address: /feed
                    messages:
                      Tick: {payload: {$ref: 'common.yaml#/Tick'}}
                operations:
                  onTick:
                    action: receive
                    channel: {$ref: '#/channels/feed'}
                    messages: [{$ref: '#/channels/feed/messages/Tick'}]
                """;
        List<SpecWarning> warnings = new ArrayList<>();
        generateClient(spec, warnings);
        Assert.assertEquals(warnings, List.of(new SpecWarning("/operations/onTick",
                "message 'Tick' takes its payload from 'common.yaml#/Tick', another file, and the tool only reads "
                        + "references inside the spec, so the client has no type to receive it as",
                "Bundle the spec into one file first, for example with 'asyncapi bundle' from the AsyncAPI CLI, "
                        + "or copy the schema into components/schemas.")));
    }

    @Test
    void testPayloadInAnotherFileOfA2xSpecSaysToBundleTheSpec() throws Exception {
        String spec = """
                asyncapi: 2.6.0
                info: {title: Feed, version: 1.0.0}
                servers:
                  s: {url: example.com, protocol: wss}
                channels:
                  /feed:
                    subscribe:
                      message:
                        name: Tick
                        payload: {$ref: 'common.yaml#/Tick'}
                """;
        List<SpecWarning> warnings = new ArrayList<>();
        generateClient(spec, warnings);
        Assert.assertEquals(warnings.size(), 1, warnings.toString());
        Assert.assertTrue(warnings.get(0).message().contains("takes its payload from 'common.yaml#/Tick'"),
                warnings.toString());
    }

    @Test
    void testBinaryMessagesAreLeftOutWithAWarning() throws Exception {
        String spec = """
                asyncapi: 3.0.0
                info: {title: Media, version: 1.0.0}
                x-dispatcherKey: type
                servers:
                  s: {host: example.com, protocol: wss}
                channels:
                  media:
                    address: /media
                    messages:
                      Frame: {contentType: image/png, payload: {type: object}}
                      Chunk: {payload: {type: string, format: binary}}
                      Started:
                        payload:
                          type: object
                          required: [type, id]
                          properties: {type: {type: string, const: started}, id: {type: string}}
                operations:
                  sendFrame:
                    action: send
                    channel: {$ref: '#/channels/media'}
                    messages: [{$ref: '#/channels/media/messages/Frame'}]
                  onChunk:
                    action: receive
                    channel: {$ref: '#/channels/media'}
                    messages: [{$ref: '#/channels/media/messages/Chunk'}]
                  onStarted:
                    action: receive
                    channel: {$ref: '#/channels/media'}
                    messages: [{$ref: '#/channels/media/messages/Started'}]
                """;
        List<SpecWarning> warnings = new ArrayList<>();
        String client = generateClient(spec, warnings);
        Assert.assertTrue(client.contains("function doStarted("), client);
        Assert.assertFalse(client.contains("function doFrame(") || client.contains("function doChunk("), client);
        String fix = "Binary WebSocket frames aren't supported. If the server also takes the data as JSON or text, "
                + "describe that form of the message instead.";
        Assert.assertEquals(warnings, List.of(
                new SpecWarning("/operations/sendFrame", "message 'Frame' is binary (contentType 'image/png'), and "
                        + "the client sends and reads text frames, so it leaves the message out", fix),
                new SpecWarning("/operations/onChunk", "message 'Chunk' is binary (a 'format: binary' string "
                        + "payload), and the client sends and reads text frames, so it leaves the message out", fix)));
    }

    private static String generateClient(String spec, List<SpecWarning> warnings) throws Exception {
        Path outDir = Files.createTempDirectory("ws-warnings");
        try {
            new WsClientCodeGenerator(AsyncApiParser.parseFromJsonString(YAML.readTree(spec).toString()), null, false)
                    .withWarnings(warnings::add).generate(outDir);
            return Files.readString(outDir.resolve("client.bal"));
        } finally {
            try (Stream<Path> paths = Files.walk(outDir)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
            }
        }
    }
}
