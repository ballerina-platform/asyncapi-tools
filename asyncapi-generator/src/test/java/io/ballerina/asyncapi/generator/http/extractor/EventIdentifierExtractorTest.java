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

import io.ballerina.asyncapi.core.AsyncApiParser;
import io.ballerina.asyncapi.core.AsyncApiParserException;
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.http.model.EventIdentifierConfig;
import io.ballerina.asyncapi.generator.http.model.HandshakeConfig;
import io.ballerina.compiler.syntax.tree.NodeParser;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link EventIdentifierExtractor} covering all branches of
 * {@code x-ballerina-event-identifier} extraction including body, header, composite, error paths,
 * and Ballerina keyword escaping.
 */
public class EventIdentifierExtractorTest {

    private static final String ASYNCAPI_PREFIX =
            "{\"asyncapi\":\"2.0.0\",\"info\":{\"title\":\"T\",\"version\":\"1\"},\"channels\":{}";

    @Test
    void testExtractBodyType() throws AsyncApiParserException, GeneratorException {
        String json = ASYNCAPI_PREFIX
                + ",\"x-ballerina-event-identifier\":{\"type\":\"body\",\"path\":\"event.action\"}}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        EventIdentifierConfig config = new EventIdentifierExtractor(spec).extract();

        Assert.assertEquals(config.type(), "body");
        Assert.assertEquals(config.path(), "event.action",
                "Non-keyword path segments should be returned unchanged");
    }

    @Test
    void testExtractHeaderType() throws AsyncApiParserException, GeneratorException {
        String json = ASYNCAPI_PREFIX
                + ",\"x-ballerina-event-identifier\":{\"type\":\"header\",\"name\":\"X-Event-Type\"}}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        EventIdentifierConfig config = new EventIdentifierExtractor(spec).extract();

        Assert.assertEquals(config.type(), "header");
        Assert.assertEquals(config.name(), "X-Event-Type");
    }

    @Test
    void testExtractHeaderTypeWithBallerinaKeyword() throws AsyncApiParserException, GeneratorException {
        String json = ASYNCAPI_PREFIX
                + ",\"x-ballerina-event-identifier\":{\"type\":\"header\",\"name\":\"type\"}}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        EventIdentifierConfig config = new EventIdentifierExtractor(spec).extract();

        Assert.assertEquals(config.type(), "header");
        Assert.assertEquals(config.name(), "'type",
                "Ballerina keyword 'type' must be escaped with a leading apostrophe");
    }

    @Test
    void testExtractBodyTypeWithKeywordSegment() throws AsyncApiParserException, GeneratorException {
        String json = ASYNCAPI_PREFIX
                + ",\"x-ballerina-event-identifier\":{\"type\":\"body\",\"path\":\"event.check.value\"}}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        EventIdentifierConfig config = new EventIdentifierExtractor(spec).extract();

        Assert.assertEquals(config.type(), "body");
        Assert.assertEquals(config.path(), "event.'check.value",
                "Keyword segment 'check' in dot-notation path must be escaped");
    }

    @Test
    void testExtractBodyKeyType() throws AsyncApiParserException, GeneratorException {
        String json = ASYNCAPI_PREFIX
                + ",\"x-ballerina-event-identifier\":{\"type\":\"body-key\",\"path\":\"events\"}}";
        EventIdentifierConfig config = new EventIdentifierExtractor(AsyncApiParser.parseFromJsonString(json)).extract();

        Assert.assertEquals(config.type(), "body-key");
        Assert.assertEquals(config.path(), "events");
        Assert.assertNull(config.name(), "A body-key identifier has no header");
    }

    @Test
    void testBodyKeyTypeNeedsAPath() throws AsyncApiParserException {
        String json = ASYNCAPI_PREFIX + ",\"x-ballerina-event-identifier\":{\"type\":\"body-key\"}}";
        GeneratorException e = Assert.expectThrows(GeneratorException.class, () ->
                new EventIdentifierExtractor(AsyncApiParser.parseFromJsonString(json)).extract());
        Assert.assertTrue(e.getMessage().contains("path"), e.getMessage());
    }

    @Test
    void testUnknownTypeListsBodyKey() throws AsyncApiParserException {
        String json = ASYNCAPI_PREFIX + ",\"x-ballerina-event-identifier\":{\"type\":\"query\",\"path\":\"a\"}}";
        GeneratorException e = Assert.expectThrows(GeneratorException.class, () ->
                new EventIdentifierExtractor(AsyncApiParser.parseFromJsonString(json)).extract());
        Assert.assertTrue(e.getMessage().contains("body-key"), e.getMessage());
    }

    @Test
    void testMissingExtensionThrows() throws AsyncApiParserException {
        String json = ASYNCAPI_PREFIX + "}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        try {
            new EventIdentifierExtractor(spec).extract();
            Assert.fail("Expected GeneratorException for missing x-ballerina-event-identifier");
        } catch (GeneratorException e) {
            Assert.assertTrue(e.getMessage().contains("x-ballerina-event-identifier"),
                    "Exception should mention the missing extension name");
        }
    }

    @Test
    void testExtractCompositeType() throws AsyncApiParserException, GeneratorException {
        String json = ASYNCAPI_PREFIX
                + ",\"x-ballerina-event-identifier\":{\"type\":\"composite\","
                + "\"name\":\"X-GitHub-Event\",\"path\":\"action\"}}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        EventIdentifierConfig config = new EventIdentifierExtractor(spec).extract();

        Assert.assertEquals(config.type(), "composite");
        Assert.assertEquals(config.name(), "X-GitHub-Event",
                "Header name should be extracted for composite type");
        Assert.assertEquals(config.path(), "action",
                "Body path should be extracted for composite type");
    }

    @Test
    void testInvalidTypeThrows() throws AsyncApiParserException {
        String json = ASYNCAPI_PREFIX
                + ",\"x-ballerina-event-identifier\":{\"type\":\"unknown\",\"path\":\"event\"}}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        try {
            new EventIdentifierExtractor(spec).extract();
            Assert.fail("Expected GeneratorException for unsupported identifier type");
        } catch (GeneratorException e) {
            Assert.assertTrue(
                    e.getMessage().contains("header") && e.getMessage().contains("body")
                            && e.getMessage().contains("composite"),
                    "Exception should mention all three valid types: 'header', 'body', 'composite'");
        }
    }

    @Test
    void testMissingTypeFieldThrows() throws AsyncApiParserException {
        String json = ASYNCAPI_PREFIX
                + ",\"x-ballerina-event-identifier\":{\"path\":\"event.type\"}}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        try {
            new EventIdentifierExtractor(spec).extract();
            Assert.fail("Expected GeneratorException for missing 'type' field");
        } catch (GeneratorException e) {
            Assert.assertTrue(e.getMessage().contains("type"),
                    "Exception should mention the missing 'type' attribute");
        }
    }

    @Test
    void testMissingHeaderNameThrows() throws AsyncApiParserException {
        String json = ASYNCAPI_PREFIX
                + ",\"x-ballerina-event-identifier\":{\"type\":\"header\"}}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        try {
            new EventIdentifierExtractor(spec).extract();
            Assert.fail("Expected GeneratorException for missing header 'name' field");
        } catch (GeneratorException e) {
            Assert.assertTrue(e.getMessage().contains("name"),
                    "Exception should mention the missing 'name' attribute");
        }
    }

    private static EventIdentifierConfig extractWithHandshake(String handshake)
            throws AsyncApiParserException, GeneratorException {
        String json = ASYNCAPI_PREFIX + ",\"x-ballerina-event-identifier\":{\"type\":\"body\",\"path\":\"event.type\","
                + "\"handshake\":" + handshake + "}}";
        return new EventIdentifierExtractor(AsyncApiParser.parseFromJsonString(json)).extract();
    }

    private static void assertHandshakeRejected(String handshake, String expectedMessagePart)
            throws AsyncApiParserException {
        try {
            extractWithHandshake(handshake);
            Assert.fail("Expected the handshake to be rejected: " + handshake);
        } catch (GeneratorException e) {
            Assert.assertTrue(e.getMessage().contains(expectedMessagePart),
                    "Expected '" + expectedMessagePart + "' in: " + e.getMessage());
        }
    }

    @Test
    void testNoHandshakeByDefault() throws AsyncApiParserException, GeneratorException {
        String json = ASYNCAPI_PREFIX + ",\"x-ballerina-event-identifier\":{\"type\":\"body\",\"path\":\"type\"}}";
        Assert.assertNull(new EventIdentifierExtractor(AsyncApiParser.parseFromJsonString(json)).extract()
                .handshake());
    }

    @Test
    void testExtractPostHandshake() throws AsyncApiParserException, GeneratorException {
        HandshakeConfig handshake = extractWithHandshake("{\"method\":\"post\","
                + "\"when\":{\"path\":\"type\",\"equals\":\"url_verification\"},"
                + "\"challenge\":{\"in\":\"body\",\"path\":\"challenge\"}}").handshake();
        Assert.assertEquals(handshake.method(), "POST", "The method is case-insensitive and normalised");
        Assert.assertFalse(handshake.isGet());
        Assert.assertEquals(handshake.whenPath(), "type", "Paths are kept unescaped; code generation escapes them");
        Assert.assertEquals(handshake.whenEquals(), "url_verification");
        Assert.assertEquals(handshake.challengeKey(), "challenge");
    }

    @Test
    void testExtractGetHandshake() throws AsyncApiParserException, GeneratorException {
        HandshakeConfig handshake = extractWithHandshake(
                "{\"method\":\"GET\",\"challenge\":{\"in\":\"query\",\"name\":\"hub.challenge\"}}").handshake();
        Assert.assertTrue(handshake.isGet());
        Assert.assertEquals(handshake.challengeKey(), "hub.challenge");
        Assert.assertNull(handshake.whenPath());
    }

    @Test
    void testInvalidHandshakesAreRejected() throws AsyncApiParserException {
        assertHandshakeRejected("\"yes\"", "must be an object");
        assertHandshakeRejected("{\"challenge\":{\"in\":\"query\",\"name\":\"c\"}}", "handshake.method is required");
        assertHandshakeRejected("{\"method\":\"PUT\",\"challenge\":{\"in\":\"query\",\"name\":\"c\"}}",
                "must be GET or POST");
        assertHandshakeRejected("{\"method\":\"GET\"}", "handshake.challenge is required");
        assertHandshakeRejected("{\"method\":\"GET\",\"challenge\":{\"in\":\"body\",\"path\":\"c\"}}",
                "must be 'query' for a GET handshake");
        assertHandshakeRejected("{\"method\":\"GET\",\"challenge\":{\"in\":\"query\"}}", "challenge.name is required");
        assertHandshakeRejected("{\"method\":\"GET\",\"when\":{\"path\":\"a\",\"equals\":\"b\"},"
                + "\"challenge\":{\"in\":\"query\",\"name\":\"c\"}}", "only used with a POST handshake");
        assertHandshakeRejected("{\"method\":\"POST\",\"challenge\":{\"in\":\"query\",\"name\":\"c\"}}",
                "must be 'body' for a POST handshake");
        assertHandshakeRejected("{\"method\":\"POST\",\"challenge\":{\"in\":\"body\",\"path\":\"c\"}}",
                "needs x-ballerina-event-identifier.handshake.when");
        assertHandshakeRejected("{\"method\":\"POST\",\"when\":{\"path\":\"type\"},"
                + "\"challenge\":{\"in\":\"body\",\"path\":\"c\"}}", "when.equals is required");
        assertHandshakeRejected("{\"method\":\"POST\",\"when\":{\"path\":\"type\",\"equals\":\"v\"},"
                + "\"challenge\":{\"in\":\"body\"}}", "challenge.path is required");
    }

    @Test
    void testBodyPathSegmentsAreEscapedIntoValidFieldAccess() {
        Assert.assertEquals(EventIdentifierExtractor.escapeBodyPath("event.type"), "event.'type");
        Assert.assertEquals(EventIdentifierExtractor.escapeBodyPath("event-type"), "event\\-type");
        Assert.assertEquals(EventIdentifierExtractor.escapeBodyPath("data.1st"), "data.'1st");
        for (String path : new String[]{"event.type", "event-type", "data.1st", "hub.mode$x", "plain"}) {
            String access = "payload." + EventIdentifierExtractor.escapeBodyPath(path);
            Assert.assertFalse(NodeParser.parseExpression(access).hasDiagnostics(),
                    "Not a valid field access: " + access);
        }
    }

    @Test
    void testMissingBodyPathThrows() throws AsyncApiParserException {
        String json = ASYNCAPI_PREFIX
                + ",\"x-ballerina-event-identifier\":{\"type\":\"body\"}}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        try {
            new EventIdentifierExtractor(spec).extract();
            Assert.fail("Expected GeneratorException for missing body 'path' field");
        } catch (GeneratorException e) {
            Assert.assertTrue(e.getMessage().contains("path"),
                    "Exception should mention the missing 'path' attribute");
        }
    }
}
