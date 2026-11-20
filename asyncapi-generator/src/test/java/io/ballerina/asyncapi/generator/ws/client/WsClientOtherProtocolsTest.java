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
import io.ballerina.asyncapi.generator.GeneratorException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Checks that a spec for a protocol or framing a WebSocket client can't speak stops with the reason, or gets a
 * warning when only its description names one, and that the client connects to the spec's WebSocket server.
 */
public class WsClientOtherProtocolsTest {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    // A ticker; %s is its info.description, then its servers
    private static final String TICKER = """
            asyncapi: 3.0.0
            info: {title: Ticker, version: 1.0.0, description: '%s'}
            servers:
            %s
            channels:
              ticks:
                address: /ticks
                messages:
                  Tick: {$ref: '#/components/messages/Tick'}
            operations:
              onTick:
                action: receive
                channel: {$ref: '#/channels/ticks'}
                messages: [{$ref: '#/channels/ticks/messages/Tick'}]
            components:
              messages:
                Tick: {payload: {type: object, required: [price], properties: {price: {type: number}}}}
            """;

    @Test
    void testServersOfAnotherProtocolStop() {
        GeneratorException stop = Assert.expectThrows(GeneratorException.class, () -> generate(TICKER.formatted(
                "Ticks", "  broker: {host: example.com/ws, protocol: stomp}"), new ArrayList<>()));
        Assert.assertEquals(stop.getMessage(), "the spec's servers use stomp, not ws or wss, so a WebSocket client "
                + "can't talk to them. If a server is a plain WebSocket endpoint, set its protocol to ws or wss.");
    }

    @Test
    void testSocketIoEndpointStops() {
        GeneratorException stop = Assert.expectThrows(GeneratorException.class, () -> generate(TICKER.formatted(
                "Ticks", "  live: {host: example.com, pathname: /socket.io, protocol: wss}"), new ArrayList<>()));
        Assert.assertTrue(stop.getMessage().startsWith("server 'live' is a Socket.IO endpoint (example.com/socket.io)."
                + " Socket.IO wraps every message in its own framing"), stop.getMessage());
    }

    @Test
    void testNamedFramingWarnsAndGenerates() throws Exception {
        List<SpecWarning> warnings = new ArrayList<>();
        String client = generate(TICKER.formatted("Ticks over Socket.IO 4.x",
                "  live: {host: example.com, protocol: wss}"), warnings);
        Assert.assertTrue(client.contains("remote isolated function doTick("), client);
        Assert.assertEquals(warnings, List.of(new SpecWarning("/info", "the spec's description names Socket.IO, and "
                + "the client sends and reads plain JSON frames, so it can't talk to a server that speaks Socket.IO",
                "Generate from a spec of the server's plain WebSocket endpoint, if it has one; Socket.IO itself isn't "
                        + "supported.")));
    }

    @Test
    void testClientConnectsToTheWebSocketServer() throws Exception {
        // The Kafka broker comes first, but the client can only connect to the WebSocket server
        String client = generate(TICKER.formatted("Ticks", String.join("\n",
                "  broker: {host: 'kafka.example.com:9092', protocol: kafka}",
                "  live: {host: example.com, protocol: wss}")), new ArrayList<>());
        Assert.assertTrue(client.contains("string serviceUrl = \"wss://example.com\""), client);
    }

    private static String generate(String spec, List<SpecWarning> warnings) throws Exception {
        Path outDir = Files.createTempDirectory("ws-other-protocols");
        try {
            new WsClientCodeGenerator(AsyncApiParser.parseFromJsonString(YAML.readTree(spec).toString()), null, false)
                    .withWarnings(warnings::add).generate(outDir);
            return Files.readString(outDir.resolve("client.bal")).replace("\r\n", "\n");
        } finally {
            try (Stream<Path> paths = Files.walk(outDir)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
            }
        }
    }
}
