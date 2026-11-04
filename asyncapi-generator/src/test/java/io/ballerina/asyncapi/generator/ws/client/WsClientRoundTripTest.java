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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.ballerina.asyncapi.core.AsyncApiParser;
import io.ballerina.asyncapi.generator.GeneratorException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Generates clients from specs the tool itself produced from WebSocket services (round trips), and checks the
 * parts of the client each spec used to break.
 */
public class WsClientRoundTripTest {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    @Test
    void testEchoReplyGetsItsOwnVariable() throws Exception {
        Map<String, String> files = generate(spec("echo"));
        String client = files.get("client.bal");
        Assert.assertTrue(client.contains("doChatMessage(ChatMessage chatMessage, decimal timeout)"), client);
        Assert.assertTrue(client.contains("ChatMessage|error chatMessageResponse = responseMessage.cloneWithType();"),
                client);
        Assert.assertTrue(client.contains("return chatMessageResponse;"), client);
    }

    @Test
    void testDispatcherConstraintKeepsTheRequestType() throws Exception {
        // The request payload is allOf ChatMessage and the dispatcher's const; the echo reply has its own message.
        Map<String, String> files = generate(spec("echo_allof"));
        String client = files.get("client.bal");
        Assert.assertTrue(client.contains(
                "doChatMessage(ChatMessage chatMessage, decimal timeout) returns ChatMessage|error"), client);
        Assert.assertFalse(client.contains("doChatMessageReply"), client);
        Assert.assertFalse(files.get("types.bal").contains("ChatMessage2"), files.get("types.bal"));
    }

    @Test
    void testFunctionsSharingARequestTypeEachGetAMethod() throws Exception {
        String client = generate(spec("shared_request")).get("client.bal");
        Assert.assertTrue(client.contains("doSubscribe(Topic topic, decimal timeout) returns Subscribed|error"),
                client);
        Assert.assertTrue(client.contains(
                "doUnsubscribe(Topic topic, decimal timeout) returns UnsubscribeResponse|error"), client);
        // Each method sends its own dispatcher value, whatever event the caller put in the Topic
        Assert.assertTrue(client.contains("request.event = \"subscribe\";"), client);
        Assert.assertTrue(client.contains("request.event = \"unsubscribe\";"), client);
    }

    @Test
    void testRequestWithoutAFixedValueIsSentAsGiven() throws Exception {
        // shared_response's request schemas fix no event value in this test copy, so nothing is overwritten
        JsonNode spec = spec("shared_response");
        ((ObjectNode) spec.at("/components/schemas/Subscribe/properties/event")).remove("const");
        String client = generate(spec).get("client.bal");
        Assert.assertTrue(client.contains("Message|error message = subscribe.cloneWithType();"), client);
    }

    @Test
    void testReceiveOnlyMessageWaitsOnTheReadersPipe() throws Exception {
        String client = generate(specFile("ws_receive_only.yaml")).get("client.bal");
        Assert.assertTrue(client.contains("\"Notice\": \"notice\""), client);
        Assert.assertTrue(client.contains(
                "Message|pipe:Error responseMessage = self.pipes.getPipe(\"notice\").consume(timeout);"), client);
    }

    @Test
    void testFixedReplyValueIsRoutedExactly() throws Exception {
        // ack doesn't convert to the Ack type name's pipe on its own, so it is routed by its exact value
        JsonNode spec = specFile("ws_receive_only.yaml");
        ((ObjectNode) spec.at("/components/schemas/Ack/properties/event")).put("const", "acknowledged");
        String client = generate(spec).get("client.bal");
        Assert.assertTrue(client.contains("dispatchValueMap = {\n        \"acknowledged\": \"subscribe\"\n    };"),
                client);
        Assert.assertTrue(client.contains("if self.dispatchValueMap.hasKey(responseType) {"), client);
    }

    @Test
    void testFramesNoCallWaitsForAreDropped() throws Exception {
        String client = generate(spec("echo")).get("client.bal");
        Assert.assertTrue(client.contains("string? pipeName = self.getPipeName(message.event);"), client);
        Assert.assertTrue(client.contains("log:printWarn(\"Dropped a message that no call is waiting for\""), client);
    }

    @Test
    void testDispatcherConstraintNeedsTheFieldInTheType() throws Exception {
        // ChatMessage without an event field can't be the request type, since the client sets that field, so the
        // allOf payload becomes a type of its own. The reply gets its own Ack type, which has the field.
        JsonNode spec = spec("echo_allof");
        ((ObjectNode) spec.at("/components/schemas/ChatMessage/properties")).remove("event");
        ((ArrayNode) spec.at("/components/schemas/ChatMessage/required")).removeAll();
        ((ObjectNode) spec.at("/components/schemas")).set("Ack",
                YAML.readTree("{type: object, required: [event], properties: {event: {type: string}}}"));
        ((ObjectNode) spec.at("/components/messages/ChatMessageReply/payload"))
                .put("$ref", "#/components/schemas/Ack");
        String client = generate(spec).get("client.bal");
        Assert.assertTrue(client.contains("doChatMessage(ChatMessage2 chatMessage2, decimal timeout)"), client);
    }

    @Test
    void testMessagesWithEqualContentKeepTheirOwnNames() throws Exception {
        // HelloDataResponse, InitDataResponse and CustomValueResponse are all {type: string}
        String client = generate(spec("mixed")).get("client.bal");
        Assert.assertTrue(client.contains("returns HelloDataResponse|error"), client);
        Assert.assertTrue(client.contains("returns InitDataResponse|error"), client);
        Assert.assertTrue(client.contains("returns CustomValueResponse|error"), client);
    }

    @Test
    void testOneOfReplyWaitsOnTheReadersPipe() throws Exception {
        // The reader files ConnectionAck and Complete under connectionInit, so doConnectionInit waits there
        JsonNode spec = spec("stream");
        ObjectNode init = (ObjectNode) spec.at("/components/messages/ConnectionInit");
        init.set("x-response", YAML.readTree("{oneOf: [{$ref: '#/components/messages/ConnectionAck'}, "
                + "{$ref: '#/components/messages/Complete'}]}"));
        String client = generate(spec).get("client.bal");
        Assert.assertTrue(client.contains("\"ConnectionAck\": \"connectionInit\""), client);
        Assert.assertTrue(client.contains("self.pipes.getPipe(\"connectionInit\").consume(timeout);"), client);
    }

    @Test
    void testKeyedRepliesKeepTheTypedReader() throws Exception {
        String client = generate(spec("echo")).get("client.bal");
        Assert.assertTrue(client.contains("Message|websocket:Error message = self.clientEp->readMessage(Message);"),
                client);
        Assert.assertFalse(client.contains("#noDispatcherKey"), client);
    }

    @Test
    void testInlineRequestPayloadBecomesANamedRecord() throws Exception {
        Map<String, String> files = generate(spec("stream"));
        String client = files.get("client.bal");
        String types = files.get("types.bal");
        Assert.assertTrue(types.contains("public type ConnectionInit record {"), types);
        Assert.assertTrue(types.contains("public type Subscription record {"), types);
        Assert.assertTrue(client.contains("doConnectionInit(ConnectionInit connectionInit, decimal timeout)"),
                client);
        Assert.assertTrue(client.contains("doSubscription(Subscription subscription, decimal timeout)"), client);
        Assert.assertFalse(client.contains("'object"), client);
    }

    @Test
    void testInlinePayloadNameAvoidsAnExistingSchema() throws Exception {
        JsonNode spec = spec("stream");
        ((ObjectNode) spec.at("/components/schemas")).set("ConnectionInit",
                YAML.readTree("{type: object, properties: {other: {type: string}}}"));
        Map<String, String> files = generate(spec);
        Assert.assertTrue(files.get("types.bal").contains("public type ConnectionInit2 record {"),
                files.get("types.bal"));
        String client = files.get("client.bal");
        Assert.assertTrue(client.contains("doConnectionInit(ConnectionInit2 connectionInit2, decimal timeout)"),
                client);
    }

    @Test
    void testParameterAvoidsTheFunctionsOwnLocals() throws Exception {
        JsonNode spec = YAML.readTree(resource("echo").replace("ChatMessage", "Timeout"));
        String client = generate(spec).get("client.bal");
        Assert.assertTrue(client.contains("doTimeout(Timeout timeoutRequest, decimal timeout)"), client);
        Assert.assertTrue(client.contains("Timeout request = timeoutRequest.clone();"), client);
        Assert.assertTrue(client.contains("Timeout|error timeoutResponse = responseMessage.cloneWithType();"), client);
    }

    @Test
    void testRepliesWithoutDispatcherKeyGetTheirOwnPipe() throws Exception {
        Map<String, String> files = generate(spec("mixed"));
        String client = files.get("client.bal");
        String types = files.get("types.bal");
        Assert.assertTrue(types.contains("public type HelloDataResponse string;"), types);
        Assert.assertTrue(types.contains("public type UsersQueryResponse User[];"), types);
        Assert.assertTrue(client.contains("string|websocket:Error frame = self.clientEp->readMessage(string);"),
                client);
        Assert.assertTrue(client.contains("Message|error parsedMessage = frame.fromJsonStringWithType();"), client);
        Assert.assertTrue(client.contains("self.pipes.getPipe(\"#noDispatcherKey\").produce(frame, 5);"), client);
        Assert.assertTrue(client.contains(
                "string|pipe:Error responseMessage = self.pipes.getPipe(\"#noDispatcherKey\").consume(timeout);"),
                client);
        // A string reply is the frame text itself; any other reply is JSON text
        Assert.assertTrue(client.contains(
                "HelloDataResponse|error helloDataResponse = responseMessage.cloneWithType();"), client);
        Assert.assertTrue(client.contains(
                "UsersQueryResponse|error usersQueryResponse = responseMessage.fromJsonStringWithType();"), client);
    }

    @Test
    void testInlineReplyReusesTheMatchingReplyMessage() throws Exception {
        Map<String, String> files = generate(spec("mixed"));
        Assert.assertFalse(files.get("types.bal").contains("HelloDataResponse2"), files.get("types.bal"));
        // The reply message is consumed by doHelloData, so it gets no receive-only function of its own
        Assert.assertFalse(files.get("client.bal").contains("doHelloDataResponse"), files.get("client.bal"));
    }

    @Test
    void testInlineReplyWithoutAMatchGetsANewMessage() throws Exception {
        JsonNode spec = spec("mixed");
        ((ObjectNode) spec.at("/components/messages")).remove("HelloDataResponse");
        ((ObjectNode) spec.at("/channels/root/messages")).remove("HelloDataResponse");
        ((ObjectNode) spec.at("/operations")).remove("receiveHelloDataResponse");
        Map<String, String> files = generate(spec);
        Assert.assertTrue(files.get("types.bal").contains("public type HelloDataResponse string;"),
                files.get("types.bal"));
        Assert.assertTrue(files.get("client.bal").contains("returns HelloDataResponse|error"), files.get("client.bal"));
    }

    @Test
    void testCallsSharingAReplyPipeTakeTurns() throws Exception {
        Map<String, String> files = generate(spec("shared_response"));
        String client = files.get("client.bal");
        Assert.assertEquals(count(client, "self.pipes.takeTurn(\"unsubscribe\", timeout);"), 2, client);
        Assert.assertEquals(count(client, "self.pipes.giveTurn(\"unsubscribe\");"), 2, client);
        // The turn is released only after the reply is consumed
        Assert.assertTrue(client.indexOf("giveTurn") > client.indexOf("getPipe(\"unsubscribe\").consume"), client);
        String utils = files.get("utils.bal");
        Assert.assertTrue(utils.contains("public isolated function takeTurn(string id, decimal timeout)"), utils);
        Assert.assertTrue(utils.contains("public isolated function giveTurn(string id)"), utils);
        // Turns close before the reply pipes, so a pipe that fails to close can't leave a call waiting for its turn
        Assert.assertTrue(utils.indexOf("turn.immediateClose()") < utils.indexOf("pipe.gracefulClose()"), utils);
        // Handing the turn on never fails the call that already has its reply
        Assert.assertTrue(client.contains("self.pipes.giveTurn(\"unsubscribe\");"), client);
    }

    @Test
    void testInlineReplyKeepsReferencesInOneOf() throws Exception {
        JsonNode spec = spec("shared_response");
        ((ObjectNode) spec.at("/components/messages/Subscribe")).set("x-response",
                YAML.readTree("{payload: {oneOf: [{$ref: '#/components/schemas/Ack'}]}}"));
        Map<String, String> files = generate(spec);
        Assert.assertTrue(files.get("types.bal").contains("public type SubscribeResponse Ack;"),
                files.get("types.bal"));
        Assert.assertTrue(files.get("client.bal").contains("returns SubscribeResponse|error"), files.get("client.bal"));
    }

    @Test
    void testPerRequestPipesDoNotTakeTurns() throws Exception {
        JsonNode spec = YAML.readTree(Files.readString(specsDir().resolve("ws_with_stream_id.json")));
        String client = generate(spec).get("client.bal");
        Assert.assertFalse(client.contains("takeTurn"), client);
    }

    @Test
    void testRootPathTitleFallsBackToADefaultClassName() throws Exception {
        Map<String, String> files = generate(spec("array_response"));
        Assert.assertTrue(files.get("client.bal").contains("public client isolated class AsyncApiClient {"),
                files.get("client.bal"));
    }

    @Test
    void testReplyWithoutDispatcherKeyInOneOfIsRejected() throws Exception {
        JsonNode spec = spec("mixed");
        ((ObjectNode) spec.at("/components/messages/HelloData")).set("x-response", YAML.readTree("{oneOf: ["
                + "{$ref: '#/components/messages/HelloDataResponse'}, {$ref: '#/components/messages/InitData'}]}"));
        assertGenerationFails(spec, "Reply type 'HelloDataResponse' has no dispatcher key");
    }

    @Test
    void testStreamedReplyWithoutDispatcherKeyIsRejected() throws Exception {
        JsonNode spec = spec("mixed");
        ((ObjectNode) spec.at("/components/messages/HelloData")).put("x-response-type", "server-streaming");
        assertGenerationFails(spec, "Reply type 'HelloDataResponse' has no dispatcher key");
    }

    private static void assertGenerationFails(JsonNode spec, String expectedMessage) throws Exception {
        try {
            generate(spec);
            Assert.fail("Generation should fail with: " + expectedMessage);
        } catch (GeneratorException e) {
            Assert.assertTrue(e.getMessage().contains(expectedMessage), e.getMessage());
        }
    }

    private static Map<String, String> generate(JsonNode spec) throws Exception {
        Path outDir = Files.createTempDirectory("ws-roundtrip");
        try {
            new WsClientCodeGenerator(AsyncApiParser.parseFromJsonString(spec.toString()), null, false)
                    .generate(outDir);
            Map<String, String> files = new HashMap<>();
            for (String name : new String[]{"client.bal", "types.bal", "utils.bal"}) {
                // Generated files use the platform's line endings
                files.put(name, Files.readString(outDir.resolve(name)).replace("\r\n", "\n"));
            }
            return files;
        } finally {
            try (Stream<Path> paths = Files.walk(outDir)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
            }
        }
    }

    private static JsonNode spec(String name) throws IOException {
        return YAML.readTree(resource(name));
    }

    private static JsonNode specFile(String fileName) throws Exception {
        return YAML.readTree(Files.readString(specsDir().resolve(fileName)));
    }

    private static String resource(String name) throws IOException {
        try (InputStream in = WsClientRoundTripTest.class.getClassLoader()
                .getResourceAsStream("specs/ws_roundtrip_" + name + ".yaml")) {
            return new String(Objects.requireNonNull(in).readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Path specsDir() throws Exception {
        return Path.of(Objects.requireNonNull(
                WsClientRoundTripTest.class.getClassLoader().getResource("specs")).toURI());
    }

    private static int count(String text, String part) {
        Matcher matcher = Pattern.compile(Pattern.quote(part)).matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }
}
