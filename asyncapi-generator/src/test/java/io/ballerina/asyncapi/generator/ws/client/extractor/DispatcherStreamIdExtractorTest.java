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
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Optional;

/**
 * Unit tests for {@link DispatcherStreamIdExtractor} covering the three branches:
 * extension present, extension absent, and extension value blank.
 */
public class DispatcherStreamIdExtractorTest {

    private static final String PREFIX =
            "{\"asyncapi\":\"2.0.0\",\"info\":{\"title\":\"T\",\"version\":\"1\"},\"channels\":{}";

    @Test
    void testExtract_returnsValue_whenExtensionPresent() throws AsyncApiParserException {
        String json = PREFIX + ",\"x-dispatcherStreamId\":\"id\"}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        Optional<String> result = new DispatcherStreamIdExtractor(spec).extract();
        Assert.assertTrue(result.isPresent(),
                "Should return a non-empty Optional when x-dispatcherStreamId extension is present");
        Assert.assertEquals(result.get(), "id",
                "Should return the exact string value of the x-dispatcherStreamId extension");
    }

    @Test
    void testExtract_returnsEmpty_whenExtensionAbsent() throws AsyncApiParserException {
        String json = PREFIX + "}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        Optional<String> result = new DispatcherStreamIdExtractor(spec).extract();
        Assert.assertFalse(result.isPresent(),
                "Should return an empty Optional when x-dispatcherStreamId extension is absent");
    }

    @Test
    void testExtract_returnsEmpty_whenExtensionBlank() throws AsyncApiParserException {
        String json = PREFIX + ",\"x-dispatcherStreamId\":\"\"}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        Optional<String> result = new DispatcherStreamIdExtractor(spec).extract();
        Assert.assertFalse(result.isPresent(),
                "Should return an empty Optional when x-dispatcherStreamId extension value is blank");
    }
}
