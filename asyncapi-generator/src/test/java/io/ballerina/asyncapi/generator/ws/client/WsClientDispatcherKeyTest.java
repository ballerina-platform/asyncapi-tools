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
package io.ballerina.asyncapi.generator.ws.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.ballerina.asyncapi.core.AsyncApiParser;
import io.ballerina.asyncapi.core.SpecWarning;
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.generator.ws.client.model.DispatcherKeySpec;
import io.ballerina.asyncapi.generator.ws.client.model.NamedPayloadSpec;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Checks how a WebSocket client is generated from a spec that describes its dispatcher key loosely.
 */
public class WsClientDispatcherKeyTest {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    // %s is the Join payload, %s the Said payload, and %s any extra component schemas
    private static final String SPEC = """
            asyncapi: 3.0.0
            info: {title: Chat, version: 1.0.0}
            x-dispatcherKey: type
            servers:
              s: {host: example.com, protocol: wss}
            channels:
              ws:
                address: /ws
                messages:
                  Join: {$ref: '#/components/messages/Join'}
                  Said: {$ref: '#/components/messages/Said'}
            operations:
              sendJoin:
                action: send
                channel: {$ref: '#/channels/ws'}
                messages: [{$ref: '#/channels/ws/messages/Join'}]
              receiveSaid:
                action: receive
                channel: {$ref: '#/channels/ws'}
                messages: [{$ref: '#/channels/ws/messages/Said'}]
            components:
              messages:
                Join:
                  payload: %s
                Said:
                  payload: %s
              schemas:
                Unused: {type: string}
            %s
            """;
    private static final String JOIN = "{type: object, required: [type], "
            + "properties: {type: {type: string, const: join}, room: {type: string}}}";
    private static final String SAID = "{type: object, required: [type], "
            + "properties: {type: {type: string, const: said}, text: {type: string}}}";

    @Test
    void testKeyWithOnlyAConstIsAString() throws Exception {
        List<SpecWarning> warnings = new ArrayList<>();
        String join = "{type: object, required: [type], properties: {type: {const: join}, room: {type: string}}}";
        String said = "{type: object, required: [type], properties: {type: {enum: [said]}, text: {type: string}}}";
        String client = generateClient(SPEC.formatted(join, said, ""), warnings);
        Assert.assertTrue(client.contains("remote isolated function doJoin("), client);
        Assert.assertTrue(client.contains("remote isolated function doSaid("), client);
        Assert.assertEquals(warnings, List.of());
    }

    @Test
    void testKeyNotRequiredIsMadeRequiredWithAWarning() throws Exception {
        List<SpecWarning> warnings = new ArrayList<>();
        String join = "{type: object, properties: {type: {type: string, const: join}, room: {type: string}}}";
        String client = generateClient(SPEC.formatted(join, SAID, ""), warnings);
        Assert.assertTrue(client.contains("remote isolated function doJoin("), client);
        Assert.assertEquals(warnings, List.of(new SpecWarning("/operations/sendJoin",
                "'type' isn't in the required list of 'Join', so the client treats it as required, as it routes "
                        + "each message on it",
                "Add 'type' to the schema's 'required' list.")));
    }

    @Test
    void testMessageWithoutTheKeyIsLeftOutWithAWarning() throws Exception {
        List<SpecWarning> warnings = new ArrayList<>();
        String said = "{type: object, properties: {text: {type: string}}}";
        String client = generateClient(SPEC.formatted(JOIN, said, ""), warnings);
        Assert.assertTrue(client.contains("remote isolated function doJoin("), client);
        Assert.assertFalse(client.contains("doSaid"), client);
        Assert.assertEquals(warnings, List.of(new SpecWarning("/operations/receiveSaid",
                "message 'Said' has no 'type' field, so the client can't tell it apart from the other messages and "
                        + "leaves it out",
                "Add a required 'type' string field to the message's payload.")));
    }

    @Test
    void testKeyInAnIncludedSchemaIsFixedThere() throws Exception {
        List<SpecWarning> warnings = new ArrayList<>();
        String join = "{allOf: [{$ref: '#/components/schemas/Envelope'}, {properties: {room: {type: string}}}]}";
        String envelope = "    Envelope: {type: object, properties: {type: {const: join}}}";
        String client = generateClient(SPEC.formatted(join, SAID, envelope), warnings);
        Assert.assertTrue(client.contains("remote isolated function doJoin("), client);
        Assert.assertEquals(warnings, List.of(new SpecWarning("/components/schemas/Envelope",
                "'type' isn't in the required list of 'Envelope', so the client treats it as required, as it routes "
                        + "each message on it",
                "Add 'type' to the schema's 'required' list.")));
    }

    @Test
    void testKeyRequiredByTheIncludingSchemaNeedsNoWarning() throws Exception {
        List<SpecWarning> warnings = new ArrayList<>();
        String join = "{required: [type], allOf: [{$ref: '#/components/schemas/Envelope'}, "
                + "{properties: {room: {type: string}}}]}";
        String envelope = "    Envelope: {type: object, properties: {type: {type: string, const: join}}}";
        String client = generateClient(SPEC.formatted(join, SAID, envelope), warnings);
        Assert.assertTrue(client.contains("remote isolated function doJoin("), client);
        Assert.assertEquals(warnings, List.of());
    }

    @Test
    void testKeyFieldThatIsARefIsReadFromTheReferencedSchema() throws Exception {
        List<SpecWarning> warnings = new ArrayList<>();
        String join = "{type: object, required: [type], properties: {type: {$ref: '#/components/schemas/JoinType'}}}";
        String joinType = "    JoinType: {type: string, const: join}";
        String client = generateClient(SPEC.formatted(join, SAID, joinType), warnings);
        Assert.assertTrue(client.contains("remote isolated function doJoin("), client);
        Assert.assertEquals(warnings, List.of());
    }

    @Test
    void testSchemaThatIncludesItselfEndsTheWalk() throws Exception {
        // JoinBody includes Base, which includes JoinBody again; the key is in JoinBody's inline part
        String join = "{$ref: '#/components/schemas/JoinBody'}";
        String schemas = "    JoinBody: {allOf: [{$ref: '#/components/schemas/Base'}, "
                + "{type: object, required: [type], properties: {type: {type: string, const: join}}}]}\n"
                + "    Base: {allOf: [{$ref: '#/components/schemas/JoinBody'}]}";
        List<SpecWarning> warnings = new ArrayList<>();
        AsyncApiSpec view = keyView(SPEC.formatted(join, SAID, schemas), warnings);
        Assert.assertTrue(view.getAsyncApiOperations().orElseThrow().get("sendJoin").messages().containsKey("Join"));
    }

    @Test
    void testSchemaUsedDirectlyAfterARequiringIncludeStillWarns() throws Exception {
        // Join requires the key it includes from Base, and Said uses Base directly, where the key is optional
        String join = "{required: [type], allOf: [{$ref: '#/components/schemas/Base'}, "
                + "{properties: {room: {type: string}}}]}";
        String said = "{$ref: '#/components/schemas/Base'}";
        String base = "    Base: {type: object, properties: {type: {type: string}}}";
        List<SpecWarning> warnings = new ArrayList<>();
        keyView(SPEC.formatted(join, said, base), warnings);
        Assert.assertEquals(warnings, List.of(new SpecWarning("/components/schemas/Base",
                "'type' isn't in the required list of 'Base', so the client treats it as required, as it routes "
                        + "each message on it",
                "Add 'type' to the schema's 'required' list.")));
    }

    // The spec as the client generator sees it once the key is fixed up
    private static AsyncApiSpec keyView(String spec, List<SpecWarning> warnings) throws Exception {
        AsyncApiSpec parsed = AsyncApiParser.parseFromJsonString(YAML.readTree(spec).toString());
        Set<String> names = parsed.getAsyncApiComponents().map(c -> c.schemas()).map(Map::keySet).orElse(Set.of());
        return DispatcherKeySpec.of(NamedPayloadSpec.of(parsed), names, warnings::add);
    }

    private static String generateClient(String spec, List<SpecWarning> warnings) throws Exception {
        Path outDir = Files.createTempDirectory("ws-key");
        try {
            new WsClientCodeGenerator(AsyncApiParser.parseFromJsonString(YAML.readTree(spec).toString()), null, false)
                    .withWarnings(warnings::add).generate(outDir);
            return Files.readString(outDir.resolve("client.bal"));
        } finally {
            try (Stream<Path> paths = Files.walk(outDir)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
            }
        }
    }
}
