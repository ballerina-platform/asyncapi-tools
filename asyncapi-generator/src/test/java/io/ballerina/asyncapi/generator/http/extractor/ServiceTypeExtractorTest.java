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
import io.ballerina.asyncapi.generator.http.model.HttpServiceType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Unit tests for {@link ServiceTypeExtractor} covering happy paths and all error branches.
 */
public class ServiceTypeExtractorTest {

    private static final String PREFIX = "{\"asyncapi\":\"2.0.0\",\"info\":{\"title\":\"T\",\"version\":\"1\"}";

    @Test
    void testNoOperationsReturnsEmpty() throws AsyncApiParserException, GeneratorException {
        String json = PREFIX + ",\"channels\":{}}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        List<HttpServiceType> result = new ServiceTypeExtractor(spec).extract();
        Assert.assertTrue(result.isEmpty(), "Spec with no channels should return empty service type list");
    }

    @Test
    void testSendOnlyOperationsSkipped() throws AsyncApiParserException, GeneratorException {
        String json = PREFIX + ",\"channels\":{\"notifications\":{\"publish\":{\"message\":"
                + "{\"payload\":{\"type\":\"object\",\"properties\":"
                + "{\"id\":{\"type\":\"string\"}},\"required\":[\"id\"]}}}}}}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        List<HttpServiceType> result = new ServiceTypeExtractor(spec).extract();
        Assert.assertTrue(result.isEmpty(),
                "Publish-only (SEND) operations should be excluded from service types");
    }

    @Test
    void testExtractWithNamedSchemaPayload() throws AsyncApiParserException, GeneratorException {
        String json = PREFIX + ",\"channels\":{\"events\":{\"subscribe\":{\"message\":"
                + "{\"x-ballerina-event-type\":\"my_event\","
                + "\"payload\":{\"$ref\":\"#/components/schemas/MyEvent\"}}}}},"
                + "\"components\":{\"schemas\":{\"MyEvent\":"
                + "{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"string\"}}}}}}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        ServiceTypeExtractor extractor = new ServiceTypeExtractor(spec);
        List<HttpServiceType> result = extractor.extract();

        Assert.assertEquals(result.size(), 1, "Should extract one service type");
        Assert.assertEquals(result.get(0).remoteFunctions().size(), 1,
                "Should have one remote function");
        Assert.assertEquals(result.get(0).remoteFunctions().get(0).eventType(), "MyEvent",
                "Payload type name should match the component schema name after $ref resolution");
        Assert.assertTrue(extractor.getInlineSchemas().isEmpty(),
                "Named schemas via $ref should not be added to inline schemas");
    }

    @Test
    void testExtractWithInlineSchemaPayload() throws AsyncApiParserException, GeneratorException {
        String json = PREFIX + ",\"channels\":{\"events\":{\"subscribe\":{\"message\":"
                + "{\"x-ballerina-event-type\":\"my_event\","
                + "\"payload\":{\"type\":\"object\","
                + "\"properties\":{\"id\":{\"type\":\"string\"}}}}}}}}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        ServiceTypeExtractor extractor = new ServiceTypeExtractor(spec);
        List<HttpServiceType> result = extractor.extract();

        Assert.assertEquals(result.size(), 1, "Should extract one service type");
        Assert.assertFalse(extractor.getInlineSchemas().isEmpty(),
                "Inline payload schema should be collected into inlineSchemas");
        Assert.assertTrue(extractor.getInlineSchemas().containsKey("my_event"),
                "Inline schema should be keyed by the event type value");
    }

    @Test
    void testMissingEventTypeExtensionThrows() throws AsyncApiParserException {
        String json = PREFIX + ",\"channels\":{\"events\":{\"subscribe\":{\"message\":"
                + "{\"payload\":{\"type\":\"object\","
                + "\"properties\":{\"id\":{\"type\":\"string\"}}}}}}}}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        try {
            new ServiceTypeExtractor(spec).extract();
            Assert.fail("Expected GeneratorException for missing x-ballerina-event-type");
        } catch (GeneratorException e) {
            Assert.assertTrue(e.getMessage().contains("x-ballerina-event-type"),
                    "Exception should mention the missing extension");
        }
    }

    @Test
    void testBlankEventTypeThrows() throws AsyncApiParserException {
        String json = PREFIX + ",\"channels\":{\"events\":{\"subscribe\":{\"message\":"
                + "{\"x-ballerina-event-type\":\"  \","
                + "\"payload\":{\"type\":\"object\","
                + "\"properties\":{\"id\":{\"type\":\"string\"}}}}}}}}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        try {
            new ServiceTypeExtractor(spec).extract();
            Assert.fail("Expected GeneratorException for blank x-ballerina-event-type");
        } catch (GeneratorException e) {
            Assert.assertNotNull(e.getMessage(), "Exception message should not be null");
        }
    }

    @Test
    void testNullPayloadThrows() throws AsyncApiParserException {
        String json = PREFIX + ",\"channels\":{\"events\":{\"subscribe\":{\"message\":"
                + "{\"x-ballerina-event-type\":\"my_event\"}}}}}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        try {
            new ServiceTypeExtractor(spec).extract();
            Assert.fail("Expected GeneratorException for missing payload");
        } catch (GeneratorException e) {
            Assert.assertNotNull(e.getMessage(), "Exception message should not be null");
        }
    }
}
