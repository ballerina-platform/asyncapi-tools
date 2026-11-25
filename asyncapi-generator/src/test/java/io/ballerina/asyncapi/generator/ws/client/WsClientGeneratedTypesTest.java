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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.ballerina.asyncapi.core.AsyncApiParser;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Checks that the types a WebSocket client is generated with are valid Ballerina for free-form maps, number formats
 * and {@code allOf} parts that redeclare an included field.
 */
public class WsClientGeneratedTypesTest {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    private static final String SPEC = """
            asyncapi: 3.0.0
            info: {title: Api, version: 1.0.0}
            x-dispatcherKey: type
            servers:
              s: {host: example.com, protocol: wss}
            channels:
              ws:
                address: /ws
                messages:
                  ping: {payload: {$ref: '#/components/schemas/Ping'}}
            operations:
              sendPing:
                action: send
                channel: {$ref: '#/channels/ws'}
                messages: [{$ref: '#/channels/ws/messages/ping'}]
            components:
              schemas:
                Ping:
            %s
            """;

    @Test
    void testFreeFormMapValuesAreBallerinaTypes() throws Exception {
        String types = types("""
                      type: object
                      required: [type]
                      properties:
                        type: {type: string}
                        attributes: {type: object, additionalProperties: {type: object}}
                        products: {type: object, additionalProperties: {type: array}}
                        labels: {type: object, additionalProperties: {type: string}}
                        any: {type: object, additionalProperties: true}
                """);
        Assert.assertTrue(types.contains("map<record {}> attributes?;"), types);
        Assert.assertTrue(types.contains("map<anydata[]> products?;"), types);
        Assert.assertTrue(types.contains("map<string> labels?;"), types);
        Assert.assertTrue(types.contains("map<json> 'any?;"), types);
    }

    @Test
    void testNumberFormatsAreBallerinaTypes() throws Exception {
        String types = types("""
                      type: object
                      required: [type]
                      properties:
                        type: {type: string}
                        count: {type: number, format: int32}
                        big: {type: number, format: int64}
                        ratio: {type: number, format: float}
                        price: {type: number, format: double}
                        small: {type: number, format: uint8}
                """);
        Assert.assertTrue(types.contains("int count?;"), types);
        Assert.assertTrue(types.contains("int big?;"), types);
        Assert.assertTrue(types.contains("float ratio?;"), types);
        Assert.assertTrue(types.contains("decimal price?;"), types);
        Assert.assertTrue(types.contains("decimal small?;"), types);
    }

    @Test
    void testTypelessRedeclarationKeepsTheIncludedField() throws Exception {
        String types = types("""
                      allOf:
                        - {$ref: '#/components/schemas/Envelope'}
                        - type: object
                          properties:
                            type: {const: accepted}
                            id: {type: string}
                    Envelope:
                      type: object
                      required: [type]
                      properties:
                        type: {type: string}
                """);
        Assert.assertTrue(types.contains("public type Ping record {\n    *Envelope;\n    string id?;\n};"), types);
    }

    @Test
    void testRedeclarationWithAnotherTypeCopiesTheIncludedFields() throws Exception {
        String types = types("""
                      allOf:
                        - {$ref: '#/components/schemas/Common'}
                        - type: object
                          properties:
                            data: {type: array, items: {type: string}}
                    Common:
                      type: object
                      required: [type]
                      properties:
                        type: {type: string}
                        data: {type: object}
                """);
        Assert.assertTrue(types.contains("public type Ping record {\n    string 'type;\n    string[] data?;\n};"),
                types);
    }

    @Test
    void testRedeclarationWithTheSameTypeKeepsTheInclusion() throws Exception {
        String types = types("""
                      allOf:
                        - {$ref: '#/components/schemas/Common'}
                        - type: object
                          required: [type]
                          properties:
                            type: {type: string}
                    Common:
                      type: object
                      required: [type]
                      properties:
                        type: {type: string}
                """);
        Assert.assertTrue(types.contains("public type Ping record {\n    *Common;\n    string 'type;\n};"), types);
    }

    @Test
    void testRedeclarationOfAFieldInAnIncludedSchemasOwnAllOf() throws Exception {
        String types = types("""
                      allOf:
                        - {$ref: '#/components/schemas/Common'}
                        - type: object
                          properties:
                            data: {type: array, items: {type: string}}
                    Common:
                      allOf:
                        - type: object
                          required: [type]
                          properties:
                            type: {type: string}
                            data: {type: object}
                """);
        Assert.assertTrue(types.contains("public type Ping record {\n    string 'type;\n    string[] data?;\n};"),
                types);
    }

    @Test
    void testRedeclarationKeepsAFieldTheIncludedSchemaRequires() throws Exception {
        String types = types("""
                      allOf:
                        - {$ref: '#/components/schemas/Common'}
                        - type: object
                          properties:
                            data: {type: integer}
                    Common:
                      type: object
                      required: [type, data]
                      properties:
                        type: {type: string}
                        data: {type: number}
                """);
        Assert.assertTrue(types.contains("public type Ping record {\n    string 'type;\n    int data;\n};"), types);
    }

    @Test
    void testMapValueWithOnlyPropertiesIsARecord() throws Exception {
        String types = types("""
                      type: object
                      required: [type]
                      properties:
                        type: {type: string}
                        settings: {type: object, additionalProperties: {properties: {id: {type: string}}}}
                """);
        Assert.assertTrue(types.contains("map<record {"), types);
        Assert.assertTrue(types.contains("string id?;"), types);
        Assert.assertFalse(types.contains("map<json> settings"), types);
    }

    @Test
    void testTypelessRedeclarationThatRequiresTheFieldMakesItRequired() throws Exception {
        String types = types("""
                      allOf:
                        - {$ref: '#/components/schemas/Envelope'}
                        - type: object
                          required: [data]
                          properties:
                            data: {description: The data}
                    Envelope:
                      type: object
                      required: [type]
                      properties:
                        type: {type: string}
                        data: {type: string}
                """);
        Assert.assertTrue(types.contains("public type Ping record {\n    *Envelope;\n    string data;\n};"), types);
    }

    @Test
    void testCopiedBaseWritesItsOwnRedeclarationOnce() throws Exception {
        String types = types("""
                      allOf:
                        - {$ref: '#/components/schemas/Common'}
                        - type: object
                          properties:
                            tags: {type: array, items: {type: string}}
                    Common:
                      allOf:
                        - {$ref: '#/components/schemas/Base'}
                        - type: object
                          properties:
                            data: {type: string}
                    Base:
                      type: object
                      required: [type]
                      properties:
                        type: {type: string}
                        data: {type: string}
                        tags: {type: array}
                """);
        Assert.assertTrue(types.contains(
                "public type Ping record {\n    string 'type;\n    string data?;\n    string[] tags?;\n};"), types);
    }

    @Test
    void testMapValueWithOnlyAdditionalPropertiesIsAMap() throws Exception {
        String types = types("""
                      type: object
                      required: [type]
                      properties:
                        type: {type: string}
                        nested: {type: object, additionalProperties: {additionalProperties: {type: string}}}
                """);
        Assert.assertTrue(types.contains("map<map<string>> nested?;"), types);
    }

    @Test
    void testTypelessRedeclarationInsideAFieldKeepsTheIncludedField() throws Exception {
        String types = types("""
                      type: object
                      required: [type]
                      properties:
                        type: {type: string}
                        response:
                          allOf:
                            - {$ref: '#/components/schemas/View'}
                            - {type: object, properties: {status: {const: in_progress}}}
                    View:
                      type: object
                      properties: {status: {type: string}}
                """);
        // A field's included schema is a reference too, so its status stays the included string
        Assert.assertFalse(types.contains("anydata status"), types);
        Assert.assertTrue(types.contains("*View;"), types);
    }

    @Test
    void testInlinePartsThatRedeclareAFieldGiveOneField() throws Exception {
        // The second part only narrows fields the first declares, so each stays one field, required as before
        String types = types("""
                      allOf:
                        - type: object
                          additionalProperties: false
                          required: [type, kind, data]
                          properties:
                            type: {type: string}
                            kind: {type: string, const: signal}
                            data: {type: object}
                        - type: object
                          properties:
                            kind: {description: The signal kind.}
                            data: {type: object}
                """);
        Assert.assertTrue(types.contains("public type Ping record {\n    string 'type;\n    string kind;\n"
                + "    record {} data;\n};"), types);
    }

    @Test
    void testNestedAllOfIsOneAllOf() throws Exception {
        // An allOf inside an allOf part adds its parts, so a field the outer part redeclares isn't written twice
        String types = types("""
                      allOf:
                        - allOf:
                            - {type: object, required: [symbol], properties: {symbol: {type: string}}}
                            - type: object
                              required: [type, bids]
                              properties:
                                type: {type: string}
                                bids: {type: array, maxItems: 1000, items: {type: string}}
                        - type: object
                          properties:
                            bids: {type: array, maxItems: 100, items: {type: string}}
                """);
        Assert.assertTrue(types.contains("public type Ping record {\n    string symbol;\n    string 'type;\n"
                + "    string[] bids;\n};"), types);
    }

    @Test
    void testTypelessRedeclarationOfAFieldIncludedInANestedAllOf() throws Exception {
        String types = types("""
                      allOf:
                        - allOf:
                            - {$ref: '#/components/schemas/Base'}
                        - type: object
                          properties:
                            data: {description: Narrowed}
                    Base:
                      type: object
                      required: [type]
                      properties:
                        type: {type: string}
                        data: {type: string}
                """);
        Assert.assertTrue(types.contains("public type Ping record {\n    *Base;\n};"), types);
    }

    @Test
    void testTwoCopiedPartsWriteASharedFieldAndInclusionOnce() throws Exception {
        String types = types("""
                      allOf:
                        - {$ref: '#/components/schemas/A'}
                        - {$ref: '#/components/schemas/B'}
                        - type: object
                          properties:
                            data: {type: array, items: {type: string}}
                            other: {type: integer}
                    A:
                      allOf:
                        - {$ref: '#/components/schemas/Base'}
                        - type: object
                          required: [id]
                          properties: {id: {type: string}, data: {type: object}}
                    B:
                      allOf:
                        - {$ref: '#/components/schemas/Base'}
                        - type: object
                          properties: {id: {type: string}, other: {type: string}}
                    Base:
                      type: object
                      required: [type]
                      properties: {type: {type: string}}
                """);
        Assert.assertTrue(types.contains("public type Ping record {\n    *Base;\n    string id;\n"
                + "    string[] data?;\n    int other?;\n};"), types);
    }

    @Test
    void testAllOfInAMapValueKeepsTheIncludedField() throws Exception {
        String types = types("""
                      type: object
                      required: [type]
                      properties:
                        type: {type: string}
                        views:
                          type: object
                          additionalProperties:
                            allOf:
                              - {$ref: '#/components/schemas/View'}
                              - {type: object, properties: {status: {const: in_progress}}}
                    View:
                      type: object
                      properties: {status: {type: string}}
                """);
        Assert.assertFalse(types.contains("anydata status"), types);
        Assert.assertTrue(types.contains("map<record {*View;}> views?;"), types);
    }

    private static String types(String pingSchema) throws Exception {
        String json = YAML.readTree(SPEC.formatted(pingSchema.stripTrailing())).toString();
        Path outDir = Files.createTempDirectory("ws-types");
        try {
            new WsClientCodeGenerator(AsyncApiParser.parseFromJsonString(json), null, false).generate(outDir);
            // Generated files use the platform's line endings
            return Files.readString(outDir.resolve("types.bal")).replace("\r\n", "\n");
        } finally {
            try (Stream<Path> paths = Files.walk(outDir)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
            }
        }
    }
}
