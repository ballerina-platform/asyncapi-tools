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

import io.ballerina.asyncapi.generator.Protocol;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Registry of the protocols the CLI can run. Supporting a new protocol means adding one
 * {@link ProtocolCommand} here; the {@code --protocol} flag, its validation and the usage line follow.
 */
final class ProtocolCommands {

    private static final Map<Protocol, ProtocolCommand> COMMANDS = new EnumMap<>(Protocol.class);

    static {
        register(new HttpProtocolCommand());
        register(new WsProtocolCommand());
    }

    private ProtocolCommands() {
    }

    private static void register(ProtocolCommand command) {
        COMMANDS.put(command.protocol(), command);
    }

    /**
     * Finds the command for a protocol name given on the command line.
     *
     * @param name the {@code --protocol} value; matched case-insensitively
     * @return the command, or empty if the name is null or unknown
     */
    static Optional<ProtocolCommand> find(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return COMMANDS.values().stream()
                .filter(c -> c.protocol().name().equalsIgnoreCase(name.trim()))
                .findFirst();
    }

    /**
     * Returns the supported protocol names in lower case, e.g. {@code "http|ws"}.
     *
     * @param separator text placed between names
     * @return the joined names
     */
    static String names(String separator) {
        return COMMANDS.keySet().stream()
                .map(p -> p.name().toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(separator));
    }
}
