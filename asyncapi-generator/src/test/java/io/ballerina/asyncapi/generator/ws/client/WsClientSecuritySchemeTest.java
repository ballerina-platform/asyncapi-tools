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
 * Checks that a {@code userPassword} or {@code apiKey} security scheme no longer stops generation: each is read as a
 * scheme a WebSocket client can send when it connects, with a warning, or left out with one.
 */
public class WsClientSecuritySchemeTest {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    // A ticker whose one security scheme is %s
    private static final String TICKER = """
            asyncapi: 3.0.0
            info: {title: Ticker, version: 1.0.0}
            servers:
              live:
                host: example.com
                protocol: wss
                security: [{$ref: '#/components/securitySchemes/auth'}]
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
              securitySchemes:
                auth: %s
            """;
    private static final String HTTP_BASIC = "{type: http, scheme: basic}";

    @Test
    void testUserPasswordIsSentAsBasicCredentials() throws Exception {
        List<SpecWarning> warnings = new ArrayList<>();
        Assert.assertEquals(generate(TICKER.formatted("{type: userPassword}"), warnings),
                generate(TICKER.formatted(HTTP_BASIC), new ArrayList<>()));
        Assert.assertEquals(warnings, List.of(new SpecWarning("/components/securitySchemes/auth",
                "security scheme 'auth' is userPassword, so the client sends the user and password as HTTP Basic "
                        + "credentials when it connects",
                "Set them in the client's 'auth' configuration. If the server expects them another way, describe the "
                        + "scheme as 'http' or 'httpApiKey' instead.")));
    }

    @Test
    void testApiKeySentAsTheUserIsSentAsBasicCredentials() throws Exception {
        List<SpecWarning> warnings = new ArrayList<>();
        Assert.assertEquals(generate(TICKER.formatted("{type: apiKey, in: user}"), warnings),
                generate(TICKER.formatted(HTTP_BASIC), new ArrayList<>()));
        Assert.assertEquals(warnings.get(0).message(), "security scheme 'auth' is an apiKey sent as the user, so the "
                + "client sends it in HTTP Basic credentials when it connects");
        Assert.assertEquals(warnings.get(0).suggestion(), "Set the key as the user in the client's 'auth' "
                + "configuration.");
    }

    @Test
    void testApiKeyInAQueryIsAnHttpApiKey() throws Exception {
        List<SpecWarning> warnings = new ArrayList<>();
        Map<String, String> files = generate(TICKER.formatted("{type: apiKey, in: query, name: ticket}"), warnings);
        Assert.assertEquals(files, generate(TICKER.formatted("{type: httpApiKey, in: query, name: ticket}"),
                new ArrayList<>()));
        Assert.assertTrue(files.get("client.bal").contains("map<anydata> queryParam = {\"ticket\": "
                + "apiKeyConfig.ticket};"), files.get("client.bal"));
        Assert.assertEquals(warnings, List.of(new SpecWarning("/components/securitySchemes/auth",
                "security scheme 'auth' is an apiKey sent in a query, which AsyncAPI calls an httpApiKey, so the "
                        + "client sends it as the query 'ticket'", "Change its type to 'httpApiKey'.")));
    }

    @Test
    void testApiKeyWithNowhereToSendItIsLeftOut() throws Exception {
        List<SpecWarning> warnings = new ArrayList<>();
        Map<String, String> files = generate(TICKER.formatted("{type: apiKey, description: A subprotocol}"),
                warnings);
        Assert.assertFalse(files.get("client.bal").contains("ApiKeysConfig"), files.get("client.bal"));
        Assert.assertEquals(warnings, List.of(new SpecWarning("/components/securitySchemes/auth",
                "security scheme 'auth' is an apiKey without a place the client can send it, so the client leaves "
                        + "it out", "Give it 'in: user' or 'in: password', or describe it as an 'httpApiKey' with "
                        + "'in' and 'name'. Until then, set the client's configuration by hand.")));
    }

    // Every generated file by name
    private static Map<String, String> generate(String spec, List<SpecWarning> warnings) throws Exception {
        Path outDir = Files.createTempDirectory("ws-security-scheme");
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
