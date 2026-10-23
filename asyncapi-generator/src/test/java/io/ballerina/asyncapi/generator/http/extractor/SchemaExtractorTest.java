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
import io.ballerina.asyncapi.core.model.component.AsyncApiSchema;
import io.ballerina.asyncapi.generator.GeneratorException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * Unit tests for {@link SchemaExtractor} covering all three branches:
 * components with schemas, no components, and components with empty schemas.
 */
public class SchemaExtractorTest {

    private static final String PREFIX = "{\"asyncapi\":\"2.0.0\",\"info\":{\"title\":\"T\",\"version\":\"1\"}";

    @Test
    void testWithSchemas() throws AsyncApiParserException, GeneratorException {
        String json = PREFIX + ",\"channels\":{},\"components\":{\"schemas\":{"
                + "\"MyEvent\":{\"type\":\"object\","
                + "\"properties\":{\"id\":{\"type\":\"string\"}}}}}}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        Map<String, AsyncApiSchema> schemas = new SchemaExtractor(spec).extract();
        Assert.assertFalse(schemas.isEmpty(), "Should return a non-empty schema map when components.schemas exist");
        Assert.assertTrue(schemas.containsKey("MyEvent"), "Schema map should contain 'MyEvent'");
    }

    @Test
    void testWithNoComponents() throws AsyncApiParserException, GeneratorException {
        String json = PREFIX + ",\"channels\":{}}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        Map<String, AsyncApiSchema> schemas = new SchemaExtractor(spec).extract();
        Assert.assertTrue(schemas.isEmpty(), "Should return an empty map when no components section exists");
    }

    @Test
    void testWithEmptyComponentsSection() throws AsyncApiParserException, GeneratorException {
        String json = PREFIX + ",\"channels\":{},\"components\":{}}";
        AsyncApiSpec spec = AsyncApiParser.parseFromJsonString(json);
        Map<String, AsyncApiSchema> schemas = new SchemaExtractor(spec).extract();
        Assert.assertTrue(schemas.isEmpty(),
                "Should return an empty map when components section has no schemas");
    }
}
