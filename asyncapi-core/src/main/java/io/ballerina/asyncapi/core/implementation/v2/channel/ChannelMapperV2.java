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
import io.apicurio.datamodels.models.asyncapi.AsyncApiChannelItem;
import io.apicurio.datamodels.models.asyncapi.AsyncApiChannels;
import io.apicurio.datamodels.models.asyncapi.AsyncApiComponents;
import io.apicurio.datamodels.models.asyncapi.AsyncApiExtensible;
import io.apicurio.datamodels.models.asyncapi.AsyncApiReferenceable;
import io.apicurio.datamodels.models.asyncapi.v20.AsyncApi20ChannelItem;
import io.apicurio.datamodels.models.asyncapi.v21.AsyncApi21ChannelItem;
import io.apicurio.datamodels.models.asyncapi.v22.AsyncApi22ChannelItem;
import io.apicurio.datamodels.models.asyncapi.v23.AsyncApi23ChannelItem;
import io.apicurio.datamodels.models.asyncapi.v23.AsyncApi23Components;
import io.apicurio.datamodels.models.asyncapi.v24.AsyncApi24ChannelItem;
import io.apicurio.datamodels.models.asyncapi.v24.AsyncApi24Components;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25ChannelItem;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25Components;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26ChannelItem;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26Components;
import io.ballerina.asyncapi.core.Constants;
import io.ballerina.asyncapi.core.implementation.v2.message.MessageMapperV2;
import io.ballerina.asyncapi.core.model.channel.AsyncApiChannel;
import io.ballerina.asyncapi.core.model.server.AsyncApiServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps Apicurio Channel models to {@link AsyncApiChannel} for AsyncAPI 2.x.
 */
public final class ChannelMapperV2 {

    private static final Logger LOG = LogManager.getLogger(ChannelMapperV2.class);

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
            Map<String, AsyncApiServer> serversMap) {
        if (channels == null) {
            return Map.of();
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
                result.put(name, mapOne(name, channelItem, components, serversMap));
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
        return mapOne(name, channelItem, components, null);
    }

    /**
     * Resolves a {@code $ref} string to an {@link AsyncApiChannel} from components.
     * Component channels are only available in AsyncAPI 2.3+.
     *
     * @param name        the channel name (used as address)
     * @param $ref        the $ref string (e.g. {@code #/components/channels/MyChannel})
     * @param components  the AsyncAPI components object
     * @param serversMap  the map of server names to AsyncApiServer objects (for server name resolution)
     * @return the resolved channel model, or null if not found or unsupported
     */
    private static AsyncApiChannel resolveRef(
            String name,
            String $ref,
            AsyncApiComponents components,
            Map<String, AsyncApiServer> serversMap) {
        if (!$ref.startsWith(Constants.CHANNELS_REF_PREFIX)) {
            LOG.warn("Unsupported $ref format: {}. Skipping channel.", $ref);
            return null;
        }
        String channelName = $ref.substring(Constants.CHANNELS_REF_PREFIX.length());
        if (components == null) {
            return null;
        }
        Map<String, ? extends AsyncApiChannelItem> channelsMap = switch (components) {
            case AsyncApi26Components typed -> typed.getChannels();
            case AsyncApi25Components typed -> typed.getChannels();
            case AsyncApi24Components typed -> typed.getChannels();
            case AsyncApi23Components typed -> typed.getChannels();
            default -> null;
        };
        AsyncApiChannelItem resolved = channelsMap != null ? channelsMap.get(channelName) : null;
        if (resolved == null) {
            return null;
        }
        if (resolved instanceof AsyncApiReferenceable resolvedTyped && resolvedTyped.get$ref() != null) {
            LOG.warn("Resolved $ref points to another $ref: {}. Skipping channel.", resolvedTyped.get$ref());
            return null;
        }
        return buildChannel(name, resolved, components, serversMap);
    }

    /**
     * Maps a single channel item, resolving {@code $ref} if present.
     *
     * @param name        the channel name (used as address)
     * @param channelItem the Apicurio channel item object
     * @param components  the AsyncAPI components (for $ref resolution)
     * @param serversMap  the map of server names to AsyncApiServer objects
     * @return the mapped AsyncApiChannel, or null if the $ref cannot be resolved
     */
    private static AsyncApiChannel mapOne(
            String name,
            AsyncApiChannelItem channelItem,
            AsyncApiComponents components,
            Map<String, AsyncApiServer> serversMap) {
        String $ref = null;
        if (channelItem instanceof AsyncApiReferenceable referenceable) {
            $ref = referenceable.get$ref();
        }
        if ($ref != null) {
            AsyncApiChannel resolved = resolveRef(name, $ref, components, serversMap);
            if (resolved == null) {
                LOG.warn("Could not resolve $ref: {}. Skipping channel.", $ref);
                return null;
            }
            return resolved;
        }
        return buildChannel(name, channelItem, components, serversMap);
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
            Map<String, AsyncApiServer> serversMap) {

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
        List<AsyncApiServer> servers = null;
        if (serverNames != null && !serverNames.isEmpty() && serversMap != null) {
            servers = new ArrayList<>();
            for (String serverName : serverNames) {
                AsyncApiServer server = serversMap.get(serverName);
                if (server != null) {
                    servers.add(server);
                } else {
                    LOG.warn("Server reference '{}' in channel '{}' not found in servers map. Skipping.",
                            serverName, name);
                }
            }
            if (servers.isEmpty()) {
                servers = null;
            }
        }

        return new AsyncApiChannel(
                name,
                MessageMapperV2.extractMessages(channelItem, components),
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
}
