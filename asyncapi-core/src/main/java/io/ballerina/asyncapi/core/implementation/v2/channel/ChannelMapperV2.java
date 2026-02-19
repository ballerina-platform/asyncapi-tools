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
package io.ballerina.asyncapi.core.implementation.v2.channel;

import com.fasterxml.jackson.databind.JsonNode;
import io.apicurio.datamodels.models.MappedNode;
import io.apicurio.datamodels.models.asyncapi.*;
import io.apicurio.datamodels.models.asyncapi.v20.AsyncApi20ChannelItem;
import io.apicurio.datamodels.models.asyncapi.v20.AsyncApi20Operation;
import io.apicurio.datamodels.models.asyncapi.v21.AsyncApi21ChannelItem;
import io.apicurio.datamodels.models.asyncapi.v21.AsyncApi21Operation;
import io.apicurio.datamodels.models.asyncapi.v22.AsyncApi22ChannelItem;
import io.apicurio.datamodels.models.asyncapi.v22.AsyncApi22Operation;
import io.apicurio.datamodels.models.asyncapi.v23.AsyncApi23ChannelItem;
import io.apicurio.datamodels.models.asyncapi.v23.AsyncApi23Operation;
import io.apicurio.datamodels.models.asyncapi.v24.AsyncApi24ChannelItem;
import io.apicurio.datamodels.models.asyncapi.v24.AsyncApi24Operation;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25ChannelItem;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25Operation;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26ChannelItem;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26Operation;
import io.ballerina.asyncapi.core.implementation.v2.component.MessageRefResolverV2;
import io.ballerina.asyncapi.core.implementation.v2.component.MessageMapperV2;
import io.ballerina.asyncapi.core.model.channel.AsyncApiChannel;
import io.ballerina.asyncapi.core.model.message.AsyncApiMessage;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps Apicurio Channel models to {@link AsyncApiChannel} for AsyncAPI 2.x.
 */
public final class ChannelMapperV2 {

    private static final PrintStream outStream = System.err;

    private ChannelMapperV2() {

    }

    /**
     * Maps Apicurio {@link AsyncApiChannels} to a map of channel names to
     * {@link AsyncApiChannel}. In v2, the channel name (map key) serves as the
     * channel address.
     *
     * @param channels   the Apicurio channels object
     * @param components the AsyncAPI components (for $ref resolution)
     * @param serversMap the map of server names to AsyncApiServer objects (for server name resolution)
     * @return the mapped channels map, or an empty map if channels is null
     */
    public static Map<String, AsyncApiChannel> map(
            AsyncApiChannels channels,
            AsyncApiComponents components,
            Map<String, io.ballerina.asyncapi.core.model.server.AsyncApiServer> serversMap) {
        if (channels == null) {
            return null;
        }

        List<String> channelNames = null;
        if (channels instanceof MappedNode<?> mappedNode) {
            channelNames = mappedNode.getItemNames();
        }

        if (channelNames == null || channelNames.isEmpty()) {
            return Map.of();
        }

        Map<String, AsyncApiChannel> result = new LinkedHashMap<>();
        for (String name : channelNames) {
            AsyncApiChannelItem channelItem = null;
            if (channels instanceof MappedNode<?> mappedNode) {
                channelItem = (AsyncApiChannelItem) mappedNode.getItem(name);
            }
            if (channelItem != null) {
                result.put(name, buildChannel(name, channelItem, components, serversMap));
            }
        }
        return result;
    }

    /**
     * Maps a single Apicurio channel item to {@link AsyncApiChannel}.
     *
     * @param name        the channel name (used as address in v2)
     * @param channelItem the Apicurio channel item object
     * @param components  the AsyncAPI components (for $ref resolution)
     * @return the mapped AsyncApiChannel, or null if channelItem is null
     */
    public static AsyncApiChannel mapChannel(
            String name,
            AsyncApiChannelItem channelItem,
            AsyncApiComponents components) {
        if (channelItem == null) {
            return null;
        }
        // Component channels don't have server references to resolve, pass null for serversMap
        return buildChannel(name, channelItem, components, null);
    }

    /**
     * Builds an {@link AsyncApiChannel} from an Apicurio {@link AsyncApiChannelItem}.
     * The channel name is used as the address since v2 has no explicit address field.
     *
     * @param name        the channel name (used as address)
     * @param channelItem the Apicurio channel item object
     * @param components  the AsyncAPI components (for $ref resolution)
     * @param serversMap  the map of server names to AsyncApiServer objects (for server name resolution)
     * @return the mapped AsyncApiChannel
     */
    private static AsyncApiChannel buildChannel(
            String name,
            AsyncApiChannelItem channelItem,
            AsyncApiComponents components,
            Map<String, io.ballerina.asyncapi.core.model.server.AsyncApiServer> serversMap) {

        Map<String, JsonNode> extensions = null;
        if (channelItem instanceof AsyncApiExtensible extensible) {
            extensions = extensible.getExtensions();
        }

        // Extract server names (v2.2-2.6 only)
        List<String> serverNames = switch (channelItem) {
            case AsyncApi26ChannelItem typed -> typed.getServers();
            case AsyncApi25ChannelItem typed -> typed.getServers();
            case AsyncApi24ChannelItem typed -> typed.getServers();
            case AsyncApi23ChannelItem typed -> typed.getServers();
            case AsyncApi22ChannelItem typed -> typed.getServers();
            case AsyncApi21ChannelItem typed -> null;  // getServers() not available
            case AsyncApi20ChannelItem typed -> null;  // getServers() not available
            default -> null;
        };

        // Resolve server names to AsyncApiServer objects
        List<io.ballerina.asyncapi.core.model.server.AsyncApiServer> servers = null;
        if (serverNames != null && !serverNames.isEmpty() && serversMap != null) {
            servers = new ArrayList<>();
            for (String serverName : serverNames) {
                io.ballerina.asyncapi.core.model.server.AsyncApiServer server = serversMap.get(serverName);
                if (server != null) {
                    servers.add(server);
                } else {
                    outStream.println("Server reference '" + serverName
                            + "' in channel '" + name + "' not found in servers map. Skipping.");
                }
            }
            if (servers.isEmpty()) {
                servers = null;
            }
        }

        return new AsyncApiChannel(
                name,
                extractMessages(channelItem, components),
                null,
                null,
                channelItem.getDescription(),
                servers,
                ChannelParameterMapperV2.mapParameters(channelItem.getParameters()),
                null,
                null,
                ChannelBindingsMapperV2.map(channelItem.getBindings(), components),
                extensions
        );
    }

    /**
     * Extracts messages from a channel items' publish and subscribe operations.
     *
     * @param channelItem the Apicurio channel item object
     * @param components  the AsyncAPI components (for $ref resolution)
     * @return a map of message names to AsyncApiMessage objects, or null if no messages found
     */
    private static Map<String, AsyncApiMessage> extractMessages(
            AsyncApiChannelItem channelItem, AsyncApiComponents components) {
        Map<String, AsyncApiMessage> messages = new LinkedHashMap<>();

        // Extract from publish operation
        if (channelItem.getPublish() != null) {
            extractMessagesFromOperation(channelItem.getPublish(), messages, components);
        }

        // Extract from subscribe operation
        if (channelItem.getSubscribe() != null) {
            extractMessagesFromOperation(channelItem.getSubscribe(), messages, components);
        }

        return messages.isEmpty() ? null : messages;
    }

    /**
     * Extracts messages from a single operation and adds them to the messages map.
     *
     * @param operation  the Apicurio operation object (publish or subscribe)
     * @param messages   the map to populate with extracted messages
     * @param components the AsyncAPI components (for $ref resolution)
     */
    private static void extractMessagesFromOperation(
            io.apicurio.datamodels.models.asyncapi.AsyncApiOperation operation,
            Map<String, AsyncApiMessage> messages,
            AsyncApiComponents components) {
        if (operation == null) {
            return;
        }

        // Get the single message (if present) - version-specific access
        io.apicurio.datamodels.models.asyncapi.AsyncApiMessage message = switch (operation) {
            case AsyncApi26Operation typed ->
                    typed.getMessage();
            case AsyncApi25Operation typed ->
                    typed.getMessage();
            case AsyncApi24Operation typed ->
                    typed.getMessage();
            case AsyncApi23Operation typed ->
                    typed.getMessage();
            case AsyncApi22Operation typed ->
                    typed.getMessage();
            case AsyncApi21Operation typed ->
                    typed.getMessage();
            case AsyncApi20Operation typed ->
                    typed.getMessage();
            default -> null;
        };

        if (message != null) {
            // Check for $ref and resolve (v2.1-2.6 only)
            if (message instanceof AsyncApiReferenceable referenceable) {
                String $ref = referenceable.get$ref();
                if ($ref != null) {
                    io.apicurio.datamodels.models.asyncapi.AsyncApiMessage resolved =
                            MessageRefResolverV2.resolveMessageRef($ref, components);
                    if (resolved == null) {
                        outStream.println("Could not resolve message $ref: " + $ref + ". Skipping message.");
                        return;
                    }

                    // Guard against chained $refs
                    if (resolved instanceof AsyncApiReferenceable resolvedRef
                            && resolvedRef.get$ref() != null) {
                        outStream.println("Resolved message $ref points to another $ref: "
                                + resolvedRef.get$ref() + ". Skipping message.");
                        return;
                    }

                    // Use the resolved message
                    message = resolved;
                }
            }

            AsyncApiMessage mappedMessage = MessageMapperV2.map(message);
            if (mappedMessage != null) {
                String key = message.getName();
                if (key == null || key.isBlank()) {
                    // Generate a key from message title or use a default
                    key = message.getTitle();
                    if (key == null || key.isBlank()) {
                        key = "message_" + messages.size();
                    }
                }
                messages.put(key, mappedMessage);
            }
        }
    }
}
