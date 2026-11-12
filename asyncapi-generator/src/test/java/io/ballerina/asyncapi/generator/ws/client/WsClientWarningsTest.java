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
import io.ballerina.asyncapi.core.SpecWarning;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Checks the warnings a WebSocket client generation reports for the parts of a spec it leaves out.
 */
public class WsClientWarningsTest {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    @Test
    void testSentMessageWithoutPayloadIsLeftOutWithAWarning() throws Exception {
        String spec = """
                asyncapi: 3.0.0
                info: {title: Echo, version: 1.0.0}
                x-dispatcherKey: type
                servers:
                  s: {host: example.com, protocol: wss}
                channels:
                  ws:
                    address: /ws
                    messages:
                      ping:
                        payload:
                          type: object
                          required: [type]
                          properties: {type: {type: string}}
                      echo: {contentType: text/plain}
                operations:
                  sendPing:
                    action: send
                    channel: {$ref: '#/channels/ws'}
                    messages: [{$ref: '#/channels/ws/messages/ping'}]
                  send/echo:
                    action: send
                    channel: {$ref: '#/channels/ws'}
                    messages: [{$ref: '#/channels/ws/messages/echo'}]
                """;
        List<SpecWarning> warnings = new ArrayList<>();
        Path outDir = Files.createTempDirectory("ws-warnings");
        try {
            new WsClientCodeGenerator(AsyncApiParser.parseFromJsonString(YAML.readTree(spec).toString()), null, false)
                    .withWarnings(warnings::add).generate(outDir);
            String client = Files.readString(outDir.resolve("client.bal"));
            Assert.assertTrue(client.contains("remote isolated function doPing("), client);
            Assert.assertFalse(client.contains("'null"), client);
            Assert.assertEquals(warnings, List.of(new SpecWarning("/operations/send~1echo",
                    "message 'echo' has no payload schema, so the client has no function to send it",
                    "Add a 'payload' schema to the message to get a function for it.")));
        } finally {
            try (Stream<Path> paths = Files.walk(outDir)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
            }
        }
    }
}
