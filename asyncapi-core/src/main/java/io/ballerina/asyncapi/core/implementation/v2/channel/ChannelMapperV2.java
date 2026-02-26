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
import java.util.Collections;
import java.util.HashMap;
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
    public static Map<String, AsyncApiChannel> map(AsyncApiChannels channels, AsyncApiComponents components,
                                                   Map<String, AsyncApiServer> serversMap) {
        if (channels == null) {
            return Collections.emptyMap();
        }
        if (!(channels instanceof MappedNode<?> mappedNode)) {
            return Collections.emptyMap();
        }
        List<String> channelNames = mappedNode.getItemNames();
        if (channelNames == null || channelNames.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, AsyncApiChannel> result = new HashMap<>();
        for (String name : channelNames) {
            AsyncApiChannelItem channelItem = (AsyncApiChannelItem) mappedNode.getItem(name);
            if (channelItem != null) {
                result.put(name, mapChannelItem(name, channelItem, components, serversMap));
            }
        }
        return result;
    }

    public static AsyncApiChannel mapChannelItem(String name, AsyncApiChannelItem channelItem,
                                                 AsyncApiComponents components,
                                                 Map<String, AsyncApiServer> serversMap) {
        // Handle $ref
        if (channelItem instanceof AsyncApiReferenceable referenceable && referenceable.get$ref() != null) {
            String $ref = referenceable.get$ref();
            if (!$ref.startsWith(Constants.CHANNELS_REF_PREFIX)) {
                LOG.warn("Unsupported $ref format: {}. Skipping channel.", $ref);
                return null;
            }
            if (components == null) {
                return null;
            }
            String channelName = $ref.substring(Constants.CHANNELS_REF_PREFIX.length());
            Map<String, ? extends AsyncApiChannelItem> channelsMap = switch (components) {
                case AsyncApi26Components typed -> typed.getChannels();
                case AsyncApi25Components typed -> typed.getChannels();
                case AsyncApi24Components typed -> typed.getChannels();
                case AsyncApi23Components typed -> typed.getChannels();
                default -> null;
            };
            AsyncApiChannelItem resolved = channelsMap != null ? channelsMap.get(channelName) : null;
            if (resolved == null) {
                LOG.warn("Could not resolve $ref: {}. Skipping channel.", $ref);
                return null;
            }
            if (resolved instanceof AsyncApiReferenceable resolvedTyped && resolvedTyped.get$ref() != null) {
                LOG.warn("Resolved $ref points to another $ref: {}. Skipping channel.",
                        resolvedTyped.get$ref());
                return null;
            }
            return mapChannelItem(name, resolved, components, serversMap);
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
            case AsyncApi21ChannelItem typed -> null;
            case AsyncApi20ChannelItem typed -> null;
            default -> null;
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
