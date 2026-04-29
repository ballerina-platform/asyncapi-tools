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
package io.ballerina.asyncapi.generator.ws.client.utils;

import io.ballerina.asyncapi.core.AsyncApiParser;
import io.ballerina.asyncapi.core.AsyncApiParserException;
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.core.model.component.AsyncApiSchema;
import io.ballerina.asyncapi.generator.GeneratorException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

/**
 * Unit tests for {@link CommonFunctionUtils#isDispatcherPresent} covering direct-property lookup,
 * allOf traversal, the throw-when-absent-and-key-matches branch, and the silent-false return.
 */
public class CommonFunctionUtilsTest {

    private static final String BASE_SPEC =
            "{\"asyncapi\":\"2.0.0\",\"info\":{\"title\":\"T\",\"version\":\"1\"},\"channels\":{}}";

    private static final String SPEC_WITH_DISPATCHER_KEY =
            "{\"asyncapi\":\"2.0.0\",\"info\":{\"title\":\"T\",\"version\":\"1\"},\"channels\":{},"
                    + "\"x-dispatcherKey\":\"event\"}";

    @Test
    void testIsDispatcherPresent_trueWhenKeyInDirectProperties()
            throws AsyncApiParserException, GeneratorException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(BASE_SPEC);
        AsyncApiSchema dispatcherField = AsyncApiSchema.builder().type("string").build();
        AsyncApiSchema schema = AsyncApiSchema.builder()
                .type("object")
                .properties(Map.of("event", dispatcherField))
                .required(List.of("event"))
                .build();

        boolean result = new CommonFunctionUtils(spec).isDispatcherPresent("MyEvent", schema, "event", true);

        Assert.assertTrue(result,
                "Should return true when the dispatcher field is a string property inside the required list");
    }

    @Test
    void testIsDispatcherPresent_trueWhenKeyInAllOf()
            throws AsyncApiParserException, GeneratorException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(BASE_SPEC);
        AsyncApiSchema dispatcherField = AsyncApiSchema.builder().type("string").build();
        AsyncApiSchema allOfMember = AsyncApiSchema.builder()
                .type("object")
                .properties(Map.of("event", dispatcherField))
                .required(List.of("event"))
                .build();
        AsyncApiSchema schema = AsyncApiSchema.builder()
                .allOf(List.of(allOfMember))
                .build();

        boolean result = new CommonFunctionUtils(spec).isDispatcherPresent("MyEvent", schema, "event", true);

        Assert.assertTrue(result,
                "Should return true when an allOf member contains the dispatcher field as a required string property");
    }

    @Test
    void testIsDispatcherPresent_throwsWhenKeyAbsentAndSpecKeyMatches() throws AsyncApiParserException {
        AsyncApiSpec spec;
        try {
            spec = AsyncApiParser.parseFromJsonString(SPEC_WITH_DISPATCHER_KEY);
        } catch (AsyncApiParserException e) {
            Assert.fail("Spec parsing should not fail: " + e.getMessage());
            return;
        }
        // allOf member that is a plain object with no dispatcher property
        AsyncApiSchema allOfMember = AsyncApiSchema.builder().type("object").build();
        AsyncApiSchema schema = AsyncApiSchema.builder()
                .allOf(List.of(allOfMember))
                .build();

        try {
            new CommonFunctionUtils(spec).isDispatcherPresent("MyEvent", schema, "event", true);
            Assert.fail("Expected GeneratorException when no allOf member contains the dispatcher key"
                    + " and dispatcherVal matches the spec-level x-dispatcherKey");
        } catch (GeneratorException e) {
            Assert.assertNotNull(e.getMessage(),
                    "Exception message should not be null when dispatcher key is missing from all allOf members");
        }
    }

    @Test
    void testIsDispatcherPresent_falseWhenSchemaIsObjectWithNoProperties()
            throws AsyncApiParserException, GeneratorException {
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(BASE_SPEC);
        // Schema is a plain object with no properties, oneOf, or allOf
        AsyncApiSchema schema = AsyncApiSchema.builder().type("object").build();

        boolean result = new CommonFunctionUtils(spec).isDispatcherPresent("MyEvent", schema, "event", true);

        Assert.assertFalse(result,
                "Should return false when the schema is a plain object with no properties and no composite members");
    }
}
