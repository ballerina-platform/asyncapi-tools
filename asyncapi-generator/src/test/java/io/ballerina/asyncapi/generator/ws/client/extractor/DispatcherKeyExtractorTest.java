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
package io.ballerina.asyncapi.generator.ws.client.extractor;

import io.ballerina.asyncapi.core.AsyncApiParser;
import io.ballerina.asyncapi.core.AsyncApiParserException;
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.generator.GeneratorException;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link DispatcherKeyExtractor} covering the happy path and both
 * error branches: absent extension and blank extension value.
 */
public class DispatcherKeyExtractorTest {

    private static final String PREFIX =
            "{\"asyncapi\":\"2.0.0\",\"info\":{\"title\":\"T\",\"version\":\"1\"},\"channels\":{}";

    @Test
    void testExtract_returnsKey_whenExtensionPresent() throws AsyncApiParserException, GeneratorException {
        String json = PREFIX + ",\"x-dispatcherKey\":\"event\"}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        String key = new DispatcherKeyExtractor(spec).extract();
        Assert.assertEquals(key, "event",
                "Should return the exact string value of the x-dispatcherKey extension");
    }

    @Test
    void testExtract_throwsGeneratorException_whenExtensionAbsent() throws AsyncApiParserException {
        String json = PREFIX + "}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        try {
            new DispatcherKeyExtractor(spec).extract();
            Assert.fail("Expected GeneratorException when x-dispatcherKey extension is absent");
        } catch (GeneratorException e) {
            Assert.assertTrue(e.getMessage().contains("x-dispatcherKey"),
                    "Exception message should mention the missing x-dispatcherKey extension name");
        }
    }

    @Test
    void testExtract_throwsGeneratorException_whenExtensionBlank() throws AsyncApiParserException {
        String json = PREFIX + ",\"x-dispatcherKey\":\"\"}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        try {
            new DispatcherKeyExtractor(spec).extract();
            Assert.fail("Expected GeneratorException when x-dispatcherKey extension is blank");
        } catch (GeneratorException e) {
            Assert.assertNotNull(e.getMessage(),
                    "Exception message should not be null when x-dispatcherKey extension is blank");
        }
    }
}
