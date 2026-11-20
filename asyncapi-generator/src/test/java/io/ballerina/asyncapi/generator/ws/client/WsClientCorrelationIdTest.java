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
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Checks that a request and its reply that name the same correlationId field are paired on it, as an
 * x-dispatcherStreamId, so each reply reaches the call that sent its request.
 */
public class WsClientCorrelationIdTest {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    // A lookup whose request and reply carry requestId; %s is extra spec-level lines, then Lookup's required fields
    private static final String LOOKUP = """
            asyncapi: 3.0.0
            info: {title: Lookup, version: 1.0.0}
            servers: {s: {host: example.com, protocol: wss}}
            x-dispatcherKey: type
            %s
            channels:
              lookup:
                address: /lookup
                messages:
                  Lookup: {$ref: '#/components/messages/Lookup'}
            operations:
              lookup:
                action: send
                channel: {$ref: '#/channels/lookup'}
                messages: [{$ref: '#/channels/lookup/messages/Lookup'}]
            components:
              messages:
                Lookup:
                  correlationId: {location: '$message.payload#/requestId'}
                  payload: {$ref: '#/components/schemas/Lookup'}
                  x-response: {$ref: '#/components/messages/Found'}
                Found:
                  correlationId: {location: '$message.payload#/requestId'}
                  payload: {$ref: '#/components/schemas/Found'}
              schemas:
                Lookup:
                  type: object
                  required: %s
                  properties:
                    type: {type: string, const: lookup}
                    requestId: {type: string}
                    word: {type: string}
                Found:
                  type: object
                  required: [type, requestId]
                  properties:
                    type: {type: string, const: found}
                    requestId: {type: string}
                    meaning: {type: string}
            """;

    @Test
    void testRequestAndReplyArePairedOnTheirCorrelationId() throws Exception {
        List<SpecWarning> warnings = new ArrayList<>();
        Map<String, String> files = generate(LOOKUP.formatted("", "[type, requestId, word]"), warnings);
        String client = files.get("client.bal");
        Assert.assertTrue(client.contains("import ballerina/uuid;"), client);
        Assert.assertTrue(client.contains("if lookup.requestId == \"\" {"), client);
        Assert.assertTrue(client.contains("lookup.requestId = uuid:createType1AsString();"), client);
        // The reply goes to a pipe of the request's own id
        Assert.assertTrue(client.contains("self.pipes.getPipe(lookup.requestId).consume(timeout)"), client);
        Assert.assertTrue(files.get("types.bal").contains("MessageWithId"), files.get("types.bal"));
        Assert.assertEquals(warnings, List.of(new SpecWarning("", "the spec has no x-dispatcherStreamId, so the "
                + "client pairs each reply with its request on 'requestId', the correlationId of its messages, and "
                + "gives a request an id of its own when 'requestId' is empty",
                "Add 'x-dispatcherStreamId: requestId' to the spec to make it explicit.")));
    }

    @Test
    void testNamedStreamIdIsLeftAsItIs() throws Exception {
        List<SpecWarning> warnings = new ArrayList<>();
        String client = generate(LOOKUP.formatted("x-dispatcherStreamId: requestId", "[type, requestId, word]"),
                warnings).get("client.bal");
        Assert.assertFalse(client.contains("uuid:"), client);
        Assert.assertTrue(client.contains("self.pipes.getPipe(lookup.requestId).consume(timeout)"), client);
        Assert.assertEquals(warnings, List.of());
    }

    @Test
    void testFieldARequestMayLeaveOutIsNotUsed() throws Exception {
        // The client couldn't name a pipe after an id the request might not carry
        List<SpecWarning> warnings = new ArrayList<>();
        Map<String, String> files = generate(LOOKUP.formatted("", "[type, word]"), warnings);
        Assert.assertFalse(files.get("client.bal").contains("uuid:"), files.get("client.bal"));
        Assert.assertFalse(files.get("types.bal").contains("MessageWithId"), files.get("types.bal"));
        Assert.assertEquals(warnings, List.of());
    }

    // Every generated file by name
    private static Map<String, String> generate(String spec, List<SpecWarning> warnings) throws Exception {
        Path outDir = Files.createTempDirectory("ws-correlation-id");
        try {
            new WsClientCodeGenerator(AsyncApiParser.parseFromJsonString(YAML.readTree(spec).toString()), null, false)
                    .withWarnings(warnings::add).generate(outDir);
            Map<String, String> files = new TreeMap<>();
            try (Stream<Path> paths = Files.list(outDir)) {
                for (Path path : paths.toList()) {
                    files.put(path.getFileName().toString(), Files.readString(path).replace("\r\n", "\n"));
                }
            }
            return files;
        } finally {
            try (Stream<Path> paths = Files.walk(outDir)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
            }
        }
    }
}
