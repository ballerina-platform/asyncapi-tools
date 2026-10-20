/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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
package io.ballerina.asyncapi.generator.http;

import io.ballerina.asyncapi.generator.CodeGenOrchestrator;
import io.ballerina.asyncapi.generator.Protocol;
import io.ballerina.asyncapi.generator.spi.SpecToCodeOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Tests that the HTTP generator names the types it has to invent in the order the definition lists its schemas and
 * payloads, so that names do not depend on how a hash map happens to order them.
 */
public class HttpTypeNamingOrderTest {

    private static final Pattern TYPE_NAME = Pattern.compile("public type\\s+'?(\\w+)");

    private static final String FIRST = """
            First:
              type: object
              properties:
                kind:
                  type: string
                data:
                  type: object
                  properties:
                    a:
                      type: string
            """;
    private static final String SECOND = """
            Second:
              type: object
              properties:
                kind:
                  type: string
                data:
                  type: object
                  properties:
                    b:
                      type: string
            """;

    private static String spec(String schemas) {
        return """
                asyncapi: 3.0.0
                x-ballerina-event-identifier:
                  type: "body"
                  path: "kind"
                info:
                  title: Order
                  version: 1.0.0
                channels:
                  order:
                    address: order
                    messages:
                      first:
                        x-ballerina-event-type: "first.event"
                        payload:
                          $ref: "#/components/schemas/First"
                      second:
                        x-ballerina-event-type: "second.event"
                        payload:
                          $ref: "#/components/schemas/Second"
                operations:
                  order:
                    action: receive
                    channel:
                      $ref: "#/channels/order"
                    messages:
                      - $ref: "#/channels/order/messages/first"
                      - $ref: "#/channels/order/messages/second"
                components:
                  schemas:
                """ + schemas.indent(4);
    }

    // Two inline payloads with an inline `data` object each, received in the given order.
    private static String inlinePayloadSpec(String... messageOrder) {
        StringBuilder refs = new StringBuilder();
        for (String message : messageOrder) {
            refs.append("      - $ref: \"#/channels/order/messages/").append(message).append("\"\n");
        }
        return """
                asyncapi: 3.0.0
                x-ballerina-event-identifier:
                  type: "body"
                  path: "kind"
                info:
                  title: Order
                  version: 1.0.0
                channels:
                  order:
                    address: order
                    messages:
                      created:
                        x-ballerina-event-type: "order.created"
                        payload:
                          type: object
                          properties:
                            kind:
                              type: string
                            data:
                              type: object
                              properties:
                                a:
                                  type: string
                      deleted:
                        x-ballerina-event-type: "order.deleted"
                        payload:
                          type: object
                          properties:
                            kind:
                              type: string
                            data:
                              type: object
                              properties:
                                b:
                                  type: string
                operations:
                  order:
                    action: receive
                    channel:
                      $ref: "#/channels/order"
                    messages:
                """ + refs;
    }

    private static String generate(Path dir, String name, String yaml) throws Exception {
        Path specFile = dir.resolve(name + ".yaml");
        Files.writeString(specFile, yaml);
        Path out = Files.createDirectory(dir.resolve(name + "-out"));
        CodeGenOrchestrator.specToCode(Protocol.HTTP, specFile, out, new SpecToCodeOptions.Builder().build());
        return Files.readString(out.resolve("types.bal"));
    }

    private static List<String> typeNames(String source) {
        List<String> names = new ArrayList<>();
        Matcher matcher = TYPE_NAME.matcher(source);
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }

    // The field names declared in the record called `name`.
    private static String recordBody(String source, String name) {
        int start = source.indexOf("public type " + name + " record {");
        Assert.assertTrue(start >= 0, "Expected a record named " + name + " in:\n" + source);
        return source.substring(start, source.indexOf("};", start));
    }

    private static void deleteRecursively(Path dir) throws IOException {
        try (Stream<Path> files = Files.walk(dir)) {
            files.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        }
    }

    @Test(description = "Of two schemas that both want the name Data for an inline object, the one listed first gets "
            + "it, whatever the names of the schemas hash to")
    public void testContestedNameGoesToTheSchemaListedFirst() throws Exception {
        Path dir = Files.createTempDirectory("naming-order-");
        try {
            String firstThenSecond = generate(dir, "a", spec(FIRST + SECOND));
            Assert.assertTrue(recordBody(firstThenSecond, "Data").contains("string a"),
                    "First is listed first, so its object gets Data:\n" + firstThenSecond);
            Assert.assertTrue(recordBody(firstThenSecond, "SecondData").contains("string b"), firstThenSecond);

            String secondThenFirst = generate(dir, "b", spec(SECOND + FIRST));
            Assert.assertTrue(recordBody(secondThenFirst, "Data").contains("string b"),
                    "Second is listed first now, so its object gets Data:\n" + secondThenFirst);
            Assert.assertTrue(recordBody(secondThenFirst, "FirstData").contains("string a"), secondThenFirst);
        } finally {
            deleteRecursively(dir);
        }
    }

    @Test(description = "Of two inline payloads that both want the name Data, the one received first gets it")
    public void testContestedNameGoesToThePayloadReceivedFirst() throws Exception {
        Path dir = Files.createTempDirectory("naming-order-");
        try {
            String createdFirst = generate(dir, "a", inlinePayloadSpec("created", "deleted"));
            Assert.assertTrue(recordBody(createdFirst, "Data").contains("string a"),
                    "order.created is received first, so its object gets Data:\n" + createdFirst);

            String deletedFirst = generate(dir, "b", inlinePayloadSpec("deleted", "created"));
            Assert.assertTrue(recordBody(deletedFirst, "Data").contains("string b"),
                    "order.deleted is received first now, so its object gets Data:\n" + deletedFirst);
        } finally {
            deleteRecursively(dir);
        }
    }

    @Test(description = "The types are written in the order the schemas are listed")
    public void testTypesFollowTheOrderOfTheDefinition() throws Exception {
        Path dir = Files.createTempDirectory("naming-order-");
        try {
            List<String> names = typeNames(generate(dir, "a", spec(FIRST + SECOND)));
            Assert.assertTrue(names.indexOf("First") < names.indexOf("Second"),
                    "First is listed before Second. Got: " + names);
        } finally {
            deleteRecursively(dir);
        }
    }

    @Test(description = "Generating the same definition twice gives the same text")
    public void testGenerationIsRepeatable() throws Exception {
        Path dir = Files.createTempDirectory("naming-order-");
        try {
            Assert.assertEquals(generate(dir, "a", spec(FIRST + SECOND)), generate(dir, "b", spec(FIRST + SECOND)));
        } finally {
            deleteRecursively(dir);
        }
    }
}
