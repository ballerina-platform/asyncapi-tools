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
