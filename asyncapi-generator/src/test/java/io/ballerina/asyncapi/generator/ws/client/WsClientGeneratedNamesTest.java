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
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Checks that the names a WebSocket client is generated with are unique and valid Ballerina.
 */
public class WsClientGeneratedNamesTest {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    private static final String V3_HEAD = """
            asyncapi: 3.0.0
            info: {title: %s, version: 1.0.0}
            x-dispatcherKey: type
            servers:
              s: {host: example.com, protocol: wss}
            """;

    private static final String HEARTBEAT = V3_HEAD.formatted("Agent") + """
            channels:
              ws:
                address: /ws
                messages:
                  hb: {$ref: '#/components/messages/Heartbeat'}
            operations:
              sendHeartbeat:
                action: send
                channel: {$ref: '#/channels/ws'}
                messages: [{$ref: '#/channels/ws/messages/hb'}]
              receiveHeartbeat:
                action: receive
                channel: {$ref: '#/channels/ws'}
                messages: [{$ref: '#/channels/ws/messages/hb'}]
            components:
              messages:
                Heartbeat:
                  payload:
                    type: object
                    required: [type]
                    properties:
                      type: {type: string, const: heartbeat}
            """;

    private static final String V2_INLINE = """
            asyncapi: 2.6.0
            info: {title: Hub, version: 1.0.0}
            x-dispatcherKey: type
            servers:
              s: {url: 'wss://example.com/ws', protocol: wss}
            channels:
              /:
                publish:
                  message:
                    oneOf:
                      - payload:
                          type: object
                          required: [type]
                          properties:
                            type: {type: string, const: authenticate}
                            token: {type: string}
                      - payload:
                          type: object
                          required: [type]
                          properties:
                            type: {type: string}
                subscribe:
                  message:
                    oneOf:
                      - payload:
                          type: object
                          required: [type]
                          properties:
                            type: {type: string, enum: [unseen]}
                            count: {type: integer}
                      - messageId: inboxChanged
                        payload:
                          type: object
                          required: [type]
                          properties:
                            type: {type: string}
                      - payload:
                          type: object
                          required: [type]
                          properties:
                            type: {type: string}
            """;

    @Test
    void testMessageSentAndReceivedGetsAReceiveFunction() throws Exception {
        Map<String, String> files = generate(HEARTBEAT, true);
        String client = files.get("client.bal");
        Assert.assertTrue(client.contains("remote isolated function doHb(Heartbeat heartbeat, decimal timeout)"),
                client);
        Assert.assertTrue(client.contains("remote isolated function doHbReceived(decimal timeout) returns "
                + "Heartbeat|error"), client);
        Assert.assertEquals(count(client, "function doHb("), 1, client);
        Assert.assertEquals(count(files.get("test.bal"), "function testHb()"), 1, files.get("test.bal"));
    }

    @Test
    void testMessageTwoOperationsSendGetsOneFunction() throws Exception {
        String spec = HEARTBEAT.replace("""
                  receiveHeartbeat:
                    action: receive""", """
                  sendHeartbeatAgain:
                    action: send""");
        String client = generate(spec, false).get("client.bal");
        Assert.assertEquals(count(client, "function doHb("), 1, client);
        Assert.assertFalse(client.contains("doHb2"), client);
    }

    @Test
    void testInlineV2MessagesAreNamedFromTheirPayloads() throws Exception {
        Map<String, String> files = generate(V2_INLINE, false);
        String client = files.get("client.bal");
        String types = files.get("types.bal");
        // Named after the fixed dispatcher value, then messageId, then numbered
        Assert.assertTrue(types.contains("public type Authenticate record {"), types);
        Assert.assertTrue(types.contains("public type Unseen record {"), types);
        Assert.assertTrue(types.contains("public type InboxChanged record {"), types);
        Assert.assertTrue(types.contains("public type Message1 record {"), types);
        Assert.assertTrue(types.contains("public type Message2 record {"), types);
        Assert.assertTrue(client.contains("doAuthenticate(Authenticate authenticate, decimal timeout)"), client);
        Assert.assertTrue(client.contains("doUnseen(decimal timeout) returns Unseen|error"), client);
        Assert.assertTrue(client.contains("doInboxChanged(decimal timeout) returns InboxChanged|error"), client);
        Assert.assertFalse(client.contains("do0"), client);
    }

    @Test
    void testTitleCharactersThatCantBeInANameAreDropped() throws Exception {
        String spec = HEARTBEAT.replace("title: Agent", "title: 'Training Assistant — Participant'");
        String client = generate(spec, false).get("client.bal");
        Assert.assertTrue(client.contains("public client isolated class TrainingAssistantParticipantWsClient {"),
                client);
    }

    @Test
    void testReplyTypeNamedAfterABuiltinGetsAFreeVariable() throws Exception {
        String spec = V3_HEAD.formatted("Api") + """
                channels:
                  ws:
                    address: /ws
                    messages:
                      ask: {$ref: '#/components/messages/Ask'}
                      err: {$ref: '#/components/messages/Err'}
                operations:
                  ask:
                    action: send
                    channel: {$ref: '#/channels/ws'}
                    messages: [{$ref: '#/channels/ws/messages/ask'}]
                components:
                  schemas:
                    Error:
                      type: object
                      required: [type]
                      properties:
                        type: {type: string}
                  messages:
                    Ask:
                      payload:
                        type: object
                        required: [type]
                        properties:
                          type: {type: string}
                      x-response: {$ref: '#/components/messages/Err'}
                    Err:
                      payload: {$ref: '#/components/schemas/Error'}
                """;
        String client = generate(spec, false).get("client.bal");
        Assert.assertTrue(client.contains("Error|error errorResponse = responseMessage.cloneWithType();"), client);
        Assert.assertFalse(client.contains("'error = responseMessage"), client);
    }

    @Test
    void testSchemaWithAGeneratedTypeNameIsRenamed() throws Exception {
        String spec = """
                asyncapi: 2.4.0
                info: {title: Feed, version: 1.0.0}
                x-dispatcherKey: type
                servers:
                  s: {url: 'wss://example.com/ws', protocol: wss}
                channels:
                  /:
                    publish:
                      message: {$ref: '#/components/messages/ChannelCancel'}
                components:
                  schemas:
                    Message:
                      type: object
                      required: [type, channel]
                      properties:
                        type: {type: string}
                        channel: {type: number}
                    Envelope:
                      type: object
                      properties:
                        message: {$ref: '#/components/schemas/Message'}
                  messages:
                    ChannelCancel:
                      payload: {$ref: '#/components/schemas/Message'}
                """;
        Map<String, String> files = generate(spec, false);
        String types = files.get("types.bal");
        Assert.assertTrue(types.contains("public type Message readonly & record {string 'type;};"), types);
        Assert.assertTrue(types.contains("public type Message2 record {\n    string 'type;\n    decimal channel;\n};"),
                types);
        Assert.assertTrue(types.contains("Message2 message?;"), types);
        Assert.assertTrue(files.get("client.bal").contains("doChannelCancel(Message2 message2, decimal timeout)"),
                files.get("client.bal"));
    }

    private static Map<String, String> generate(String yaml, boolean withTests) throws Exception {
        Path outDir = Files.createTempDirectory("ws-names");
        try {
            String json = YAML.readTree(yaml).toString();
            new WsClientCodeGenerator(AsyncApiParser.parseFromJsonString(json), null, withTests).generate(outDir);
            Map<String, String> files = new HashMap<>();
            try (Stream<Path> paths = Files.walk(outDir)) {
                for (Path path : paths.filter(path -> path.toString().endsWith(".bal")).toList()) {
                    // Generated files use the platform's line endings
                    files.put(path.getFileName().toString(), Files.readString(path).replace("\r\n", "\n"));
                }
            }
            return files;
        } finally {
            try (Stream<Path> paths = Files.walk(outDir)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
            }
        }
    }

    private static int count(String text, String part) {
        return text.split(Pattern.quote(part), -1).length - 1;
    }
}
