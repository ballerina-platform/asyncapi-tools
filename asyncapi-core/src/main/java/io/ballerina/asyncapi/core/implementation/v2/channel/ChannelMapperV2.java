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
import io.ballerina.asyncapi.core.implementation.utils.StringUtils;
import io.ballerina.asyncapi.core.implementation.v2.message.MessageMapperV2;
import io.ballerina.asyncapi.core.model.channel.AsyncApiChannel;
import io.ballerina.asyncapi.core.model.server.AsyncApiServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Maps Apicurio Channel models to {@link AsyncApiChannel} for AsyncAPI 2.x.
 */
public final class ChannelMapperV2 {

    private static final Logger LOG = LogManager.getLogger(ChannelMapperV2.class);

    private ChannelMapperV2() {
    }

    /**
     * Maps Apicurio {@link AsyncApiChannels} to a map of channel names to
     * {@link AsyncApiChannel}. In v2, the channel id (map key) serves as the
     * channel address.
     *
     * @param channels   the Apicurio channels object
     * @param components the AsyncAPI components (for $ref resolution)
     * @param serversMap the map of server names to AsyncApiServer objects (for server name resolution)
     * @return the mapped channels map, or an empty map if channels is null
     */
    public static Map<String, AsyncApiChannel> map(AsyncApiChannels channels, AsyncApiComponents components,
                                                   Map<String, AsyncApiServer> serversMap) {
        if (!(channels instanceof MappedNode<?> mappedNode)) {
            return Collections.emptyMap();
        }
        List<String> channelIds = mappedNode.getItemNames();
        if (channelIds == null || channelIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, AsyncApiChannel> result = new HashMap<>();
        for (String id : channelIds) {
            AsyncApiChannelItem channelItem = (AsyncApiChannelItem) mappedNode.getItem(id);
            if (channelItem != null) {
                AsyncApiChannel mapped = mapChannelItem(id, channelItem, components, serversMap);
                if (mapped != null) {
                    result.put(StringUtils.toPascalCase(id), mapped);
                }
            }
        }
        return result;
    }

    public static AsyncApiChannel mapChannelItem(String id, AsyncApiChannelItem channelItem,
                                                 AsyncApiComponents components,
                                                 Map<String, AsyncApiServer> serversMap) {

        if (channelItem instanceof AsyncApiReferenceable referenceable && referenceable.get$ref() != null) {
            AsyncApiChannelItem resolved = resolveChannelRef(referenceable.get$ref(), components);
            if (resolved == null) {
                return null;
            }
            return mapChannelItem(id, resolved, components, serversMap);
        }

        // Build channel
        Map<String, JsonNode> extensions = null;
        if (channelItem instanceof AsyncApiExtensible extensible) {
            extensions = extensible.getExtensions();
        }
        List<String> serverNames = switch (channelItem) {
            case AsyncApi26ChannelItem typed -> typed.getServers();
            case AsyncApi25ChannelItem typed -> typed.getServers();
            case AsyncApi24ChannelItem typed -> typed.getServers();
            case AsyncApi23ChannelItem typed -> typed.getServers();
            case AsyncApi22ChannelItem typed -> typed.getServers();
            case AsyncApi21ChannelItem typed -> null; // servers fields not supported in 2.0/2.1 channels
            case AsyncApi20ChannelItem typed -> null;
            default -> throw new IllegalArgumentException("Unsupported AsyncAPI channel item version: "
                                    + channelItem.getClass().getName());
        };
        List<AsyncApiServer> servers = null;
        if (serverNames != null && !serverNames.isEmpty() && serversMap != null) {
            servers = new ArrayList<>();
            for (String serverName : serverNames) {
                AsyncApiServer server = serversMap.get(serverName);
                if (server != null) {
                    servers.add(server);
                } else {
                    LOG.warn("Server reference '{}' in channel '{}' not found in servers map. Skipping.",
                            serverName, id);
                }
            }
            if (servers.isEmpty()) {
                servers = null;
            }
        }
        return new AsyncApiChannel(
                id,
                MessageMapperV2.map(channelItem, components),
                null, //there is no channel title in 2.x
                null, //there is no channel summary in 2.x
                channelItem.getDescription(),
                servers,
                ChannelParameterMapperV2.map(channelItem.getParameters(), components),
                null, //there is no channel tags in 2.x
                null, //there is no channel external docs in 2.x
                ChannelBindingsMapperV2.map(channelItem.getBindings(), components),
                extensions
        );
    }

    /**
     * Resolves a channel {@code $ref} string, following any chain of refs, to the
     * final concrete {@link AsyncApiChannelItem}. Detects cyclic references.
     *
     * @param $ref       the initial $ref string (e.g., "#/components/channels/myChannel")
     * @param components the AsyncAPI components used for lookup
     * @return the concrete channel item, or {@code null} if resolution fails
     */
    private static AsyncApiChannelItem resolveChannelRef(String $ref, AsyncApiComponents components) {
        if (components == null) {
            LOG.warn("Cannot resolve $ref: {}. Components is null.", $ref);
            return null;
        }
        Set<String> visited = new HashSet<>();
        String current = $ref;
        while (current != null) {
            if (!current.startsWith(Constants.CHANNELS_REF_PREFIX)) {
                LOG.warn("Unsupported $ref format: {}. Skipping channel.", current);
                return null;
            }
            if (!visited.add(current)) {
                LOG.warn("Cyclic $ref detected: {}. Skipping channel.", current);
                return null;
            }
            String channelId = current.substring(Constants.CHANNELS_REF_PREFIX.length());
            Map<String, ? extends AsyncApiChannelItem> channelsMap = switch (components) {
                case AsyncApi26Components typed -> typed.getChannels();
                case AsyncApi25Components typed -> typed.getChannels();
                case AsyncApi24Components typed -> typed.getChannels();
                case AsyncApi23Components typed -> typed.getChannels();
                default -> null;
            };
            AsyncApiChannelItem resolved = channelsMap != null ? channelsMap.get(channelId) : null;
            if (resolved == null) {
                LOG.warn("Could not resolve $ref: '{}'. No matching channel found.", current);
                return null;
            }
            if (resolved instanceof AsyncApiReferenceable resolvedTyped && resolvedTyped.get$ref() != null) {
                current = resolvedTyped.get$ref();
            } else {
                return resolved;
            }
        }
        return null;
    }
}
