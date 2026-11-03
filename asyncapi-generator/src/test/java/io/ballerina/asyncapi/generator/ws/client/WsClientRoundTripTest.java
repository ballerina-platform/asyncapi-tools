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
        Assert.assertTrue(client.contains("Message|error message = timeoutRequest.cloneWithType();"), client);
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
        Assert.assertTrue(utils.contains("check turn.immediateClose();"), utils);
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
                files.put(name, Files.readString(outDir.resolve(name)));
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
