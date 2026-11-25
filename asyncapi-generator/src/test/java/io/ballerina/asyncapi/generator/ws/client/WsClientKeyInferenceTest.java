/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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
 * Checks that a WebSocket client is generated from a spec without {@code x-dispatcherKey} when one field tells its
 * messages apart, and that generation stops with a reason when none or several do.
 */
public class WsClientKeyInferenceTest {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private static final String SEE_DOCS = "See " + CodegenUtils.DISPATCHER_KEY_DOCS
            + " for how the client tells messages apart.";
    // A request without the key is cloned and queued as it is, whatever its parameter is named
    private static final String SENT_AS_IT_IS = "(?s).*readonly & anydata message = \\S+\\.cloneReadOnly\\(\\);.*";

    // %s is the Join payload and %s the Said payload; there's no x-dispatcherKey
    private static final String SPEC = """
            asyncapi: 3.0.0
            info: {title: Chat, version: 1.0.0}
            servers:
              s: {host: example.com, protocol: wss}
            channels:
              ws:
                address: /ws
                messages:
                  Join: {$ref: '#/components/messages/Join'}
                  Said: {$ref: '#/components/messages/Said'}
            operations:
              sendJoin:
                action: send
                channel: {$ref: '#/channels/ws'}
                messages: [{$ref: '#/channels/ws/messages/Join'}]
              receiveSaid:
                action: receive
                channel: {$ref: '#/channels/ws'}
                messages: [{$ref: '#/channels/ws/messages/Said'}]
            components:
              messages:
                Join:
                  payload: %s
                Said:
                  payload: %s
            """;
    private static final String JOIN = "{type: object, required: [type], "
            + "properties: {type: {type: string, const: join}, room: {type: string}}}";
    private static final String SAID = "{type: object, required: [type], "
            + "properties: {type: {type: string, const: said}, text: {type: string}}}";

    @Test
    void testTheOneFieldThatTellsMessagesApartIsTheKey() throws Exception {
        List<SpecWarning> warnings = new ArrayList<>();
        String client = generateClient(SPEC.formatted(JOIN, SAID), warnings);
        Assert.assertTrue(client.contains("remote isolated function doJoin("), client);
        Assert.assertTrue(client.contains("remote isolated function doSaid("), client);
        Assert.assertEquals(warnings, List.of(new SpecWarning("",
                "the spec has no x-dispatcherKey, so the client routes messages on 'type', the only field with a "
                        + "different fixed value in every message",
                "Add 'x-dispatcherKey: type' to the spec to make it explicit.")));
    }

    @Test
    void testInferredKeyGetsTheKeyRules() throws Exception {
        // An enum of one value with no type is read as a string and made required, as an explicit key would be
        String join = "{type: object, properties: {type: {enum: [join]}, room: {type: string}}}";
        List<SpecWarning> warnings = new ArrayList<>();
        String client = generateClient(SPEC.formatted(join, SAID), warnings);
        Assert.assertTrue(client.contains("remote isolated function doJoin("), client);
        Assert.assertEquals(warnings.size(), 2, warnings.toString());
    }

    @Test
    void testMessageSentAndReceivedCountsOnce() throws Exception {
        String receiveSaid = "  receiveSaid:\n";
        String sendSaid = "  sendSaid:\n    action: send\n    channel: {$ref: '#/channels/ws'}\n"
                + "    messages: [{$ref: '#/channels/ws/messages/Said'}]\n";
        String spec = SPEC.formatted(JOIN, SAID).replace(receiveSaid, sendSaid + receiveSaid);
        Assert.assertTrue(spec.contains("sendSaid:"), spec);
        List<SpecWarning> warnings = new ArrayList<>();
        String client = generateClient(spec, warnings);
        Assert.assertTrue(client.contains("remote isolated function doSaid("), client);
        Assert.assertEquals(warnings.size(), 1, warnings.toString());
    }

    @Test
    void testKeyFieldThatIsARefIsInferred() throws Exception {
        String join = "{type: object, required: [type], properties: {type: {$ref: '#/components/schemas/JoinType'}}}";
        String spec = SPEC.formatted(join, SAID) + "  schemas:\n    JoinType: {type: string, const: join}\n";
        List<SpecWarning> warnings = new ArrayList<>();
        String client = generateClient(spec, warnings);
        Assert.assertTrue(client.contains("remote isolated function doJoin("), client);
        Assert.assertEquals(warnings.size(), 1, warnings.toString());
    }

    @Test
    void testTwoFieldsThatTellMessagesApartStop() {
        String join = "{type: object, required: [type, kind], "
                + "properties: {type: {type: string, const: join}, kind: {type: string, const: a}}}";
        String said = "{type: object, required: [type, kind], "
                + "properties: {type: {type: string, const: said}, kind: {type: string, const: b}}}";
        GeneratorException e = Assert.expectThrows(GeneratorException.class,
                () -> generateClient(bothReceived(SPEC.formatted(join, said)), new ArrayList<>()));
        Assert.assertEquals(e.getMessage(), "x-dispatcherKey extension is required in the AsyncAPI spec, and 'type' "
                + "and 'kind' each have a different fixed value in every message a connection receives, so it isn't "
                + "clear which one the messages are routed on. Add x-dispatcherKey naming the field to route on, "
                + "such as:\n  x-dispatcherKey: type\n" + SEE_DOCS);
    }

    @Test
    void testNoFieldThatTellsMessagesApartStops() {
        String said = "{type: object, properties: {text: {type: string}}}";
        GeneratorException e = Assert.expectThrows(GeneratorException.class,
                () -> generateClient(bothReceived(SPEC.formatted(JOIN, said)), new ArrayList<>()));
        // Each received message is named with where the spec receives it, and the fix is shown for each
        Assert.assertEquals(e.getMessage(), "x-dispatcherKey extension is required in the AsyncAPI spec, and no "
                + "field has a different fixed string value (a const or a one-value enum) in every message received "
                + "on channel 'ws', so the client can't tell them apart:\n"
                + "  - 'Join', received at /operations/sendJoin\n"
                + "  - 'Said', received at /operations/receiveSaid\n"
                + "Give each received JSON object a field with its own fixed value, and name the field in "
                + "x-dispatcherKey, such as:\n"
                + "  x-dispatcherKey: type\n"
                + "  Join payload: type: {type: string, const: join}\n"
                + "  Said payload: type: {type: string, const: said}\n" + SEE_DOCS);
    }

    @Test
    void testStopSuggestsAFieldTheReceivedMessagesShare() {
        // Both already declare 'event', so the fix gives it a fixed value instead of adding a field
        String join = "{type: object, properties: {event: {type: string}, room: {type: string}}}";
        String said = "{type: object, properties: {event: {type: string}, text: {type: string}}}";
        GeneratorException e = Assert.expectThrows(GeneratorException.class,
                () -> generateClient(bothReceived(SPEC.formatted(join, said)), new ArrayList<>()));
        Assert.assertTrue(e.getMessage().contains("  x-dispatcherKey: event\n"
                + "  Join payload: event: {type: string, const: join}\n"), e.getMessage());
    }

    @Test
    void testStopMarksAReceivedMessageThatIsntAnObject() {
        // Join has no fixed field, so nothing tells it from the text frames Said arrives as
        String join = "{type: object, properties: {room: {type: string}}}";
        String said = "{type: string}";
        GeneratorException e = Assert.expectThrows(GeneratorException.class,
                () -> generateClient(bothReceived(SPEC.formatted(join, said)), new ArrayList<>()));
        Assert.assertTrue(e.getMessage().contains("  - 'Said', received at /operations/receiveSaid (not a JSON "
                + "object, so it can't carry a field)\n"), e.getMessage());
        Assert.assertFalse(e.getMessage().contains("Said payload:"), e.getMessage());
    }

    @Test
    void testConnectionThatReceivesOneKindOfMessageNeedsNoKey() throws Exception {
        // Nothing tells Join and Said apart, but only Said is received, so there is nothing to tell apart
        String join = "{type: object, properties: {room: {type: string}}}";
        String said = "{type: object, properties: {text: {type: string}}}";
        List<SpecWarning> warnings = new ArrayList<>();
        String client = generateClient(SPEC.formatted(join, said), warnings);
        Assert.assertTrue(client.matches(SENT_AS_IT_IS), client);
        Assert.assertTrue(client.contains("Said|error said = responseMessage.fromJsonStringWithType();"), client);
        Assert.assertFalse(client.contains("getPipeName(message."), client);
        Assert.assertEquals(warnings, List.of());
    }

    @Test
    void testOnlyReceivedMessagesNeedTheKey() throws Exception {
        // Join has no type, but the two received messages are told apart by it
        String join = "{type: object, properties: {room: {type: string}}}";
        String left = "  receiveLeft:\n    action: receive\n    channel: {$ref: '#/channels/ws'}\n"
                + "    messages: [{$ref: '#/channels/ws/messages/Left'}]\n";
        String spec = SPEC.formatted(join, SAID).replace("components:\n", left + "components:\n")
                .replace("      Said: {$ref: '#/components/messages/Said'}\n",
                        "      Said: {$ref: '#/components/messages/Said'}\n"
                                + "      Left: {$ref: '#/components/messages/Left'}\n")
                + "    Left:\n      payload: {type: object, required: [type], "
                + "properties: {type: {type: string, const: left}}}\n";
        Assert.assertTrue(spec.contains("receiveLeft:") && spec.contains("Left: {$ref"), spec);
        List<SpecWarning> warnings = new ArrayList<>();
        String client = generateClient(spec, warnings);
        Assert.assertTrue(client.contains("remote isolated function doLeft("), client);
        Assert.assertTrue(client.matches(SENT_AS_IT_IS), client);
        Assert.assertTrue(client.contains("string? pipeName = self.getPipeName(message.'type);"), client);
        Assert.assertEquals(warnings, List.of(new SpecWarning("",
                "the spec has no x-dispatcherKey, so the client routes received messages on 'type', the only field "
                        + "with a different fixed value in every message a connection receives",
                "Add 'x-dispatcherKey: type' to the spec to make it explicit.")));
    }

    private static String bothReceived(String spec) {
        return spec.replace("action: send", "action: receive");
    }

    private static String generateClient(String spec, List<SpecWarning> warnings) throws Exception {
        Path outDir = Files.createTempDirectory("ws-infer");
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
