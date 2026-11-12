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
