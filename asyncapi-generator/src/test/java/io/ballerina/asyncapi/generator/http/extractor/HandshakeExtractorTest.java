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
package io.ballerina.asyncapi.generator.http.extractor;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.ballerina.asyncapi.core.AsyncApiParser;
import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.http.model.HandshakeConfig;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Optional;

/**
 * Tests reading a verification handshake from a {@code receive} operation with a {@code reply}.
 */
public class HandshakeExtractorTest {

    // The request message's payload and correlationId, and the reply's binding, are filled in per test.
    private static final String SPEC = """
            asyncapi: 3.0.0
            info: {title: T, version: '1'}
            channels:
              events:
                address: events
                messages:
                  event:
                    payload: {type: object, properties: {type: {type: string}}}
                  verify:
                    payload: %s
                    %s
                  verifyReply:
                    payload: {type: string}
                    %s
            operations:
              receiveEvents:
                action: receive
                channel: {$ref: '#/channels/events'}
                messages: [{$ref: '#/channels/events/messages/event'}]
              verifyUrl:
                action: receive
                channel: {$ref: '#/channels/events'}
                messages: [{$ref: '#/channels/events/messages/verify'}]
                %s
                reply:
                  messages: [{$ref: '#/channels/events/messages/verifyReply'}]
            """;
    private static final String SLACK_PAYLOAD =
            "{type: object, properties: {type: {type: string, const: url_verification}, challenge: {type: string}}}";
    private static final String CHALLENGE_ID = "correlationId: {location: '$message.payload#/challenge'}";

    private static Optional<HandshakeConfig> extract(String payload, String correlationId, String replyBinding,
                                                     String operationBinding) throws Exception {
        String yaml = String.format(SPEC, payload, correlationId, replyBinding, operationBinding);
        String json = new YAMLMapper().readTree(yaml).toString();
        return new HandshakeExtractor(AsyncApiParser.parseFromJsonString(json)).extract();
    }

    private static void assertRejected(String payload, String correlationId, String operationBinding,
                                       String expectedMessagePart) throws Exception {
        try {
            extract(payload, correlationId, "", operationBinding);
            Assert.fail("Expected the handshake to be rejected");
        } catch (GeneratorException e) {
            Assert.assertTrue(e.getMessage().contains(expectedMessagePart),
                    "Expected '" + expectedMessagePart + "' in: " + e.getMessage());
        }
    }

    @Test
    void testSlackUrlVerification() throws Exception {
        HandshakeConfig handshake = extract(SLACK_PAYLOAD, CHALLENGE_ID, "", "").orElseThrow();
        Assert.assertEquals(handshake.when().keySet().iterator().next(), "type");
        Assert.assertEquals(handshake.when().get("type").asText(), "url_verification");
        Assert.assertEquals(handshake.challengePath(), "challenge");
        Assert.assertEquals(handshake.statusCode(), 200, "Without a binding the reply is 200");
    }

    @Test
    void testNestedConstAndPointerAndReplyStatusCode() throws Exception {
        String payload = "{type: object, properties: {meta: {type: object, properties: "
                + "{kind: {const: verify}, version: {const: 2}}}, data: {type: object}}}";
        HandshakeConfig handshake = extract(payload, "correlationId: {location: '$message.payload#/data/a~1b'}",
                "bindings: {http: {statusCode: 202}}", "bindings: {http: {method: POST}}").orElseThrow();
        Assert.assertEquals(handshake.when().get("meta.kind").asText(), "verify");
        Assert.assertEquals(handshake.when().get("meta.version").asInt(), 2);
        Assert.assertEquals(handshake.challengePath(), "data.a/b", "A JSON Pointer's ~1 is a '/'");
        Assert.assertEquals(handshake.statusCode(), 202);
    }

    @Test
    void testNoReplyMeansNoHandshake() throws Exception {
        String json = new YAMLMapper().readTree("""
                asyncapi: 3.0.0
                info: {title: T, version: '1'}
                channels: {events: {address: events, messages: {event: {payload: {type: object}}}}}
                operations:
                  receiveEvents:
                    action: receive
                    channel: {$ref: '#/channels/events'}
                    messages: [{$ref: '#/channels/events/messages/event'}]
                """).toString();
        Assert.assertTrue(new HandshakeExtractor(AsyncApiParser.parseFromJsonString(json)).extract().isEmpty());
    }

    @Test
    void testInvalidHandshakesAreRejected() throws Exception {
        assertRejected("{type: object, properties: {challenge: {type: string}}}", CHALLENGE_ID, "",
                "needs a 'const' in its request payload schema");
        assertRejected(SLACK_PAYLOAD, "", "", "needs a correlationId");
        assertRejected(SLACK_PAYLOAD, "correlationId: {location: '$message.header#/X-Challenge'}", "",
                "only a challenge in the payload is supported");
        assertRejected(SLACK_PAYLOAD, CHALLENGE_ID, "bindings: {http: {method: GET}}",
                "only a POST handshake is supported");
    }
}
