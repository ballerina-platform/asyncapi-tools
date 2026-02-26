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

import io.apicurio.datamodels.models.MappedNode;
import io.apicurio.datamodels.models.Node;
import io.apicurio.datamodels.models.asyncapi.AsyncApiChannels;
import io.apicurio.datamodels.models.asyncapi.AsyncApiComponents;
import io.apicurio.datamodels.models.asyncapi.AsyncApiReferenceable;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Channel;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Components;
import io.ballerina.asyncapi.core.Constants;
import io.ballerina.asyncapi.core.implementation.v3.doc.ExternalDocMapperV3;
import io.ballerina.asyncapi.core.implementation.v3.message.MessageMapperV3;
import io.ballerina.asyncapi.core.implementation.v3.tag.TagMapperV3;
import io.ballerina.asyncapi.core.model.channel.AsyncApiChannel;
import io.ballerina.asyncapi.core.model.server.AsyncApiServer;
import io.ballerina.asyncapi.core.model.tag.AsyncApiTag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps Apicurio Channel models to {@link AsyncApiChannel} for AsyncAPI 3.0.
 */
public final class ChannelMapperV3 {

    private static final Logger LOG = LogManager.getLogger(ChannelMapperV3.class);

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
            Node item = (Node) mappedNode.getItem(name);
            // Add additional version checks here as new AsyncAPI 3.x versions are supported.
            if (item instanceof AsyncApi30Channel channelItem) {
                AsyncApiChannel mapped = mapChannelItem(name, channelItem, components, serversMap);
                if (mapped != null) {
                    result.put(name, mapped);
                }
            }
        }
        return result;
    }

    /**
     * Maps a single {@link AsyncApi30Channel}, resolving any {@code $ref} and building the
     * {@link AsyncApiChannel} model.
     *
     * @param name        the channel name (key in the channels map)
     * @param channelItem the Apicurio channel object
     * @param components  the AsyncAPI components (for $ref resolution)
     * @param serversMap  the map of server names to pre-built AsyncApiServer objects
     * @return the mapped AsyncApiChannel, or null if the channel cannot be resolved
     */
    public static AsyncApiChannel mapChannelItem(String name, AsyncApi30Channel channelItem,
            AsyncApiComponents components, Map<String, AsyncApiServer> serversMap) {
        // Handle $ref
        String $ref = channelItem.get$ref();
        if ($ref != null) {
            if (!$ref.startsWith(Constants.CHANNELS_REF_PREFIX)) {
                LOG.warn("Unsupported channel $ref format: {}. Skipping channel '{}'.", $ref, name);
                return null;
            }
            if (components == null) {
                return null;
            }
            // Add additional version checks here as new AsyncAPI 3.x versions are supported.
            if (components instanceof AsyncApi30Components typedComponents) {
                String channelName = $ref.substring(Constants.CHANNELS_REF_PREFIX.length());
                Map<String, AsyncApi30Channel> channelsMap = typedComponents.getChannels();
                AsyncApi30Channel resolved = channelsMap != null ? channelsMap.get(channelName) : null;
                if (resolved == null) {
                    LOG.warn("Could not resolve channel $ref: {}. Skipping channel '{}'.", $ref, name);
                    return null;
                }
                if (resolved.get$ref() != null) {
                    LOG.warn("Resolved channel $ref '{}' points to another $ref. Skipping.", resolved.get$ref());
                    return null;
                }
                return mapChannelItem(name, resolved, components, serversMap);
            }
            return null;
        }

        // Extract servers
        List<AsyncApiServer> servers = null;
        List<? extends AsyncApiReferenceable> serverRefs = channelItem.getServers();
        if (serverRefs != null && !serverRefs.isEmpty() && serversMap != null) {
            List<AsyncApiServer> serverList = new ArrayList<>();
            for (AsyncApiReferenceable serverRef : serverRefs) {
                if (serverRef == null) {
                    continue;
                }
                String serverRefStr = serverRef.get$ref();
                if (serverRefStr == null) {
                    LOG.warn("Server reference in channel has no $ref. Skipping.");
                    continue;
                }
                if (!serverRefStr.startsWith(Constants.SERVERS_REF_PREFIX)) {
                    LOG.warn("Unsupported server $ref format: {}. Skipping server.", serverRefStr);
                    continue;
                }
                String serverName = serverRefStr.substring(Constants.SERVERS_REF_PREFIX.length());
                AsyncApiServer server = serversMap.get(serverName);
                if (server != null) {
                    serverList.add(server);
                } else {
                    LOG.warn("Server reference '{}' not found in servers map. Skipping.", serverName);
                }
            }
            if (!serverList.isEmpty()) {
                servers = serverList;
            }
        }

        // Build channel
        List<AsyncApiTag> tags = null;
        if (channelItem.getTags() != null) {
            tags = channelItem.getTags().stream()
                    .map(tag -> TagMapperV3.map(tag, null))
                    .toList();
        }
        return new AsyncApiChannel(
                channelItem.getAddress(),
                MessageMapperV3.extractMessages(channelItem, components),
                channelItem.getTitle(),
                channelItem.getSummary(),
                channelItem.getDescription(),
                servers,
                ChannelParameterMapperV3.mapParameters(channelItem.getParameters()),
                tags,
                ExternalDocMapperV3.map(channelItem.getExternalDocs(), null),
                ChannelBindingsMapperV3.map(channelItem.getBindings(), components),
                channelItem.getExtensions()
        );
    }
}
