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
package io.ballerina.asyncapi.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.ballerina.asyncapi.generator.Protocol;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Tells which protocol a definition is for, so {@code --protocol} is only needed when the definition doesn't say.
 * A webhook trigger definition has {@code x-ballerina-event-identifier}, which the HTTP generator needs anyway, and
 * a WebSocket definition has a server whose protocol is {@code ws} or {@code wss}. A definition with neither is
 * not guessed: WebSocket servers are often labelled {@code http} or {@code https}.
 */
final class ProtocolDetector {

    static final String EVENT_IDENTIFIER = "x-ballerina-event-identifier";
    private static final Set<String> WEBSOCKET_PROTOCOLS = Set.of("ws", "wss");

    private ProtocolDetector() {
    }

    /**
     * Reads the definition and returns its protocol.
     *
     * @param spec the definition
     * @return the protocol, or empty when the definition does not tell
     * @throws IOException if the definition can't be read or parsed
     */
    static Optional<Protocol> detect(Path spec) throws IOException {
        JsonNode root = new ObjectMapper(new YAMLFactory()).readTree(Files.readString(spec));
        if (root == null || !root.isObject()) {
            return Optional.empty();
        }
        if (root.has(EVENT_IDENTIFIER)) {
            return Optional.of(Protocol.HTTP);
        }
        for (JsonNode server : root.path("servers")) {
            String protocol = server.path("protocol").asText("").trim().toLowerCase(Locale.ROOT);
            if (WEBSOCKET_PROTOCOLS.contains(protocol)) {
                return Optional.of(Protocol.WS);
            }
        }
        return Optional.empty();
    }
}
