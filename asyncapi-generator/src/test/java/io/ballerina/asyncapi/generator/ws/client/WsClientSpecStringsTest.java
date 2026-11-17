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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.ballerina.asyncapi.core.AsyncApiParser;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.tools.text.TextDocuments;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Checks that the spec text a WebSocket client is generated with stays inside the string literals it's written to.
 */
public class WsClientSpecStringsTest {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

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
                  Ping:
                    payload: {type: object, required: [type], properties: {type: {type: string, const: ping}}}
            operations:
              sendPing:
                action: send
                channel: {$ref: '#/channels/ws'}
                messages: [{$ref: '#/channels/ws/messages/Ping'}]
            """;

    @Test
    void testServerUrlIsEscaped() throws Exception {
        String client = generateClient("example.com/a\"b\\c");
        Assert.assertTrue(client.contains("string serviceUrl = \"wss://example.com/a\\\"b\\\\c\""), client);
        assertParses(client);
    }

    @Test
    void testServerUrlWithCodeInItStaysAString() throws Exception {
        // As in an open-source spec whose server URL closes the string and adds a line of code
        String client = generateClient("real\")\n    let pwned = malicious();\n");
        Assert.assertFalse(client.contains("\n    let pwned"), client);
        assertParses(client);
    }

    // Spec text that escaped its string would leave syntax errors behind
    private static void assertParses(String source) {
        SyntaxTree tree = SyntaxTree.from(TextDocuments.from(source));
        Assert.assertFalse(tree.hasDiagnostics(), tree.diagnostics().toString());
    }

    private static String generateClient(String host) throws Exception {
        ObjectNode spec = (ObjectNode) YAML.readTree(SPEC);
        ((ObjectNode) spec.path("servers").path("s")).put("host", host);
        Path outDir = Files.createTempDirectory("ws-strings");
        try {
            new WsClientCodeGenerator(AsyncApiParser.parseFromJsonString(spec.toString()), null, false)
                    .generate(outDir);
            return Files.readString(outDir.resolve("client.bal"));
        } finally {
            try (Stream<Path> paths = Files.walk(outDir)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
            }
        }
    }
}
