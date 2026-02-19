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
package io.ballerina.asyncapi.core.implementation.v3.channel;

import io.apicurio.datamodels.models.asyncapi.AsyncApiChannelBindings;
import io.apicurio.datamodels.models.asyncapi.AsyncApiChannels;
import io.apicurio.datamodels.models.asyncapi.AsyncApiReferenceable;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Channel;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Channels;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Components;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Message;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Reference;
import io.ballerina.asyncapi.core.Constants;
import io.ballerina.asyncapi.core.implementation.v3.component.MessageMapperV3;
import io.ballerina.asyncapi.core.implementation.v3.doc.ExternalDocMapperV3;
import io.ballerina.asyncapi.core.implementation.v3.tag.TagMapperV3;
import io.ballerina.asyncapi.core.model.channel.AsyncApiChannel;
import io.ballerina.asyncapi.core.model.channel.HttpChannelBindings;
import io.ballerina.asyncapi.core.model.channel.WsChannelBindings;
import io.ballerina.asyncapi.core.model.message.AsyncApiMessage;
import io.ballerina.asyncapi.core.model.server.AsyncApiServer;
import io.ballerina.asyncapi.core.model.tag.AsyncApiTag;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps Apicurio Channel models to {@link AsyncApiChannel} for AsyncAPI 3.0.
 */
public final class ChannelMapperV3 {

    private static final PrintStream outStream = System.err;

    private ChannelMapperV3() {

    }

    /**
     * Maps Apicurio {@link AsyncApiChannels} to a map of channel names to
     * {@link AsyncApiChannel}.
     *
     * @param channels   the Apicurio channels object
     * @param components the AsyncAPI components (for $ref resolution)
     * @param serversMap the map of server names to AsyncApiServer objects (for server resolution)
     * @return the mapped channels map, or an empty map if channels is null
     */
    public static Map<String, AsyncApiChannel> map(
            AsyncApiChannels channels,
            AsyncApi30Components components,
            Map<String, AsyncApiServer> serversMap) {
        if (!(channels instanceof AsyncApi30Channels typedChannels)) {
            return Map.of();
        }
        List<String> channelNames = typedChannels.getItemNames();
        if (channelNames == null || channelNames.isEmpty()) {
            return Map.of();
        }
        Map<String, AsyncApiChannel> result = new LinkedHashMap<>();
        for (String name : channelNames) {
            AsyncApi30Channel channel = typedChannels.getItem(name);
            if (channel != null) {
                result.put(name, buildChannel(channel, components, serversMap));
            }
        }
        return result;
    }

    /**
     * Builds an {@link AsyncApiChannel} from an Apicurio
     * {@link AsyncApi30Channel} object.
     *
     * @param channel    the Apicurio AsyncAPI 3.0 channel object
     * @param components the AsyncAPI components (for $ref resolution)
     * @param serversMap the map of server names to AsyncApiServer objects (for server resolution)
     * @return the mapped AsyncApiChannel
     */
    public static AsyncApiChannel buildChannel(
            AsyncApi30Channel channel,
            AsyncApi30Components components,
            Map<String, AsyncApiServer> serversMap) {
        List<AsyncApiTag> tags = null;
        if (channel.getTags() != null) {
            tags = channel.getTags().stream()
                    .map(tag -> TagMapperV3.map(tag, null))
                    .toList();
        }
        return new AsyncApiChannel(
                channel.getAddress(),
                extractMessages(channel, components),
                channel.getTitle(),
                channel.getSummary(),
                channel.getDescription(),
                extractServers(channel, serversMap),
                ChannelParameterMapperV3.mapParameters(
                        channel.getParameters()),
                tags,
                ExternalDocMapperV3.map(channel.getExternalDocs(), null),
                mapBindings(channel.getBindings()),
                channel.getExtensions()
        );
    }

    /**
     * Maps Apicurio {@link AsyncApiChannelBindings} to
     * {@link io.ballerina.asyncapi.core.model.channel.AsyncApiChannelBindings}.
     *
     * @param bindings the Apicurio channel bindings object
     * @return the mapped AsyncApiChannelBindings, or null if bindings
     *         is null or empty
     */
    public static io.ballerina.asyncapi.core.model.channel.AsyncApiChannelBindings
            mapBindings(AsyncApiChannelBindings bindings) {
        if (bindings == null) {
            return null;
        }
        HttpChannelBindings http = bindings.getHttp() != null
                ? new HttpChannelBindings() : null;
        WsChannelBindings ws = mapWsChannelBinding(bindings.getWs());
        if (http == null && ws == null) {
            return null;
        }
        return new io.ballerina.asyncapi.core.model.channel
                .AsyncApiChannelBindings(http, ws, null);
    }

    /**
     * Maps an Apicurio WebSocket channel binding to {@link WsChannelBindings}.
     *
     * @param binding the Apicurio WebSocket binding object
     * @return the mapped WsChannelBindings, or null if binding is null
     */
    private static WsChannelBindings mapWsChannelBinding(
            io.apicurio.datamodels.models.asyncapi.AsyncApiBinding binding) {
        if (binding == null) {
            return null;
        }
        String method = getBindingItemAsText(binding, "method");
        Object query = binding.getItem("query");
        Object headers = binding.getItem("headers");
        String bindingVersion = getBindingItemAsText(binding, "bindingVersion");
        return new WsChannelBindings(method, query, headers, bindingVersion);
    }

    /**
     * Gets a binding item as text.
     *
     * @param binding the binding object
     * @param key     the item key
     * @return the text value, or null if not found or not textual
     */
    private static String getBindingItemAsText(
            io.apicurio.datamodels.models.asyncapi.AsyncApiBinding binding, String key) {
        com.fasterxml.jackson.databind.JsonNode node = binding.getItem(key);
        if (node == null) {
            return null;
        }
        return node.isTextual() ? node.asText() : node.toString();
    }

    /**
     * Extracts messages from an AsyncAPI 3.0 channel.
     * In v3, channels have a messages map where each entry can be inline or a $ref.
     *
     * @param channel    the AsyncAPI 3.0 channel object
     * @param components the AsyncAPI components (for $ref resolution)
     * @return a map of message names to AsyncApiMessage objects, or null if no messages found
     */
    private static Map<String, AsyncApiMessage> extractMessages(
            AsyncApi30Channel channel,
            AsyncApi30Components components) {
        if (channel == null) {
            return null;
        }

        Map<String, AsyncApi30Message> channelMessages = channel.getMessages();
        if (channelMessages == null || channelMessages.isEmpty()) {
            return null;
        }

        Map<String, AsyncApiMessage> result = new LinkedHashMap<>();
        for (Map.Entry<String, AsyncApi30Message> entry : channelMessages.entrySet()) {
            String messageName = entry.getKey();
            AsyncApi30Message message = entry.getValue();

            if (message == null) {
                continue;
            }

            // Check for $ref and resolve
            if (message instanceof AsyncApiReferenceable referenceable) {
                String $ref = referenceable.get$ref();
                if ($ref != null) {
                    AsyncApi30Message resolved = resolveMessageRef($ref, components);
                    if (resolved == null) {
                        outStream.println("Could not resolve message $ref: " + $ref
                                + " in channel message '" + messageName + "'. Skipping message.");
                        continue;
                    }

                    // Guard against chained $refs
                    if (resolved instanceof AsyncApiReferenceable resolvedRef
                            && resolvedRef.get$ref() != null) {
                        outStream.println("Resolved message $ref points to another $ref: "
                                + resolvedRef.get$ref() + ". Skipping message '" + messageName + "'.");
                        continue;
                    }

                    // Use the resolved message
                    message = resolved;
                }
            }

            // Map using MessageMapperV3
            AsyncApiMessage mappedMessage = MessageMapperV3.map(message);
            if (mappedMessage != null) {
                result.put(messageName, mappedMessage);
            }
        }

        return result.isEmpty() ? null : result;
    }

    /**
     * Resolves a $ref to a message in components.
     *
     * @param $ref       the $ref string (e.g., "#/components/messages/MyMessage")
     * @param components the AsyncAPI components object
     * @return the resolved message, or null if not found or invalid
     */
    private static AsyncApi30Message resolveMessageRef(
            String $ref,
            AsyncApi30Components components) {
        if (!$ref.startsWith(Constants.MESSAGES_REF_PREFIX)) {
            outStream.println("Unsupported message $ref format: " + $ref + ". Skipping message.");
            return null;
        }

        String name = $ref.substring(Constants.MESSAGES_REF_PREFIX.length());

        if (components == null) {
            return null;
        }

        Map<String, ? extends io.apicurio.datamodels.models.asyncapi.AsyncApiMessage> messagesMap =
                components.getMessages();
        if (messagesMap == null) {
            return null;
        }
        io.apicurio.datamodels.models.asyncapi.AsyncApiMessage message = messagesMap.get(name);
        return message instanceof AsyncApi30Message typedMessage ? typedMessage : null;
    }

    /**
     * Extracts servers from an AsyncAPI 3.0 channel.
     * In v3, channel servers are always $refs to servers defined in the top-level servers map.
     *
     * @param channel    the AsyncAPI 3.0 channel object
     * @param serversMap the map of server names to pre-built AsyncApiServer objects
     * @return a list of AsyncApiServer objects, or null if no servers found
     */
    private static List<AsyncApiServer> extractServers(
            AsyncApi30Channel channel,
            Map<String, AsyncApiServer> serversMap) {
        if (channel == null || serversMap == null) {
            return null;
        }

        List<AsyncApi30Reference> serverRefs = channel.getServers();
        if (serverRefs == null || serverRefs.isEmpty()) {
            return null;
        }

        List<AsyncApiServer> result = new ArrayList<>();
        for (AsyncApi30Reference serverRef : serverRefs) {
            if (serverRef == null) {
                continue;
            }

            String $ref = serverRef.get$ref();
            if ($ref == null) {
                outStream.println("Server reference in channel has no $ref. Skipping.");
                continue;
            }

            String serverName = resolveServerRef($ref);
            if (serverName == null) {
                outStream.println("Unsupported server $ref format: " + $ref + ". Skipping server.");
                continue;
            }

            AsyncApiServer server = serversMap.get(serverName);
            if (server != null) {
                result.add(server);
            } else {
                outStream.println("Server reference '" + serverName
                        + "' not found in servers map. Skipping.");
            }
        }

        return result.isEmpty() ? null : result;
    }

    /**
     * Resolves a $ref to a server name.
     *
     * @param $ref the $ref string (e.g., "#/servers/production")
     * @return the server name, or null if invalid format
     */
    private static String resolveServerRef(String $ref) {
        if (!$ref.startsWith(Constants.SERVERS_REF_PREFIX)) {
            return null;
        }
        return $ref.substring(Constants.SERVERS_REF_PREFIX.length());
    }
}
