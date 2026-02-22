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
import io.ballerina.asyncapi.core.implementation.v3.component.MessageMapperV3;
import io.ballerina.asyncapi.core.implementation.v3.doc.ExternalDocMapperV3;
import io.ballerina.asyncapi.core.implementation.v3.tag.TagMapperV3;
import io.ballerina.asyncapi.core.model.channel.AsyncApiChannel;
import io.ballerina.asyncapi.core.model.server.AsyncApiServer;
import io.ballerina.asyncapi.core.model.tag.AsyncApiTag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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
     * @return the mapped channels map, or null if channels is null
     */
    public static Map<String, AsyncApiChannel> map(
            AsyncApiChannels channels,
            AsyncApiComponents components,
            Map<String, AsyncApiServer> serversMap) {
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
            Object item = null;
            if (channels instanceof MappedNode<?> mappedNode) {
                item = mappedNode.getItem(name);
            }
            if (item != null) {
                AsyncApiChannel mapped = mapOne(name, (Node) item, components, serversMap);
                if (mapped != null) {
                    result.put(name, mapped);
                }
            }
        }
        return result;
    }

    /**
     * Maps a single channel node, resolving any {@code $ref} before delegating to
     * {@link #buildChannel(Node, AsyncApiComponents, Map)}.
     *
     * @param name       the channel name (key in the channels map)
     * @param channelNode the Apicurio channel node
     * @param components  the AsyncAPI components (for $ref resolution)
     * @param serversMap  the map of server names to pre-built AsyncApiServer objects
     * @return the mapped AsyncApiChannel, or null if the channel cannot be resolved
     */
    private static AsyncApiChannel mapOne(
            String name,
            Node channelNode,
            AsyncApiComponents components,
            Map<String, AsyncApiServer> serversMap) {
        // Add additional version checks here as new AsyncAPI 3.x versions are supported.
        if (channelNode instanceof AsyncApi30Channel typedChannel) {
            String $ref = typedChannel.get$ref();
            if ($ref != null) {
                AsyncApi30Channel resolved = resolveChannelRef($ref, components);
                if (resolved == null) {
                    LOG.warn("Could not resolve channel $ref: {}. Skipping channel '{}'.", $ref, name);
                    return null;
                }
                if (resolved.get$ref() != null) {
                    LOG.warn("Resolved channel $ref '{}' points to another $ref. Skipping channel '{}'.",
                            resolved.get$ref(), name);
                    return null;
                }
                return buildChannel(resolved, components, serversMap);
            }
        }
        return buildChannel(channelNode, components, serversMap);
    }

    /**
     * Resolves a {@code $ref} string to an {@link AsyncApi30Channel} from components.
     * In AsyncAPI 3.0, component channel refs use the format {@code #/components/channels/name}.
     *
     * @param $ref       the $ref string (e.g., {@code #/components/channels/MyChannel})
     * @param components the AsyncAPI components object
     * @return the resolved channel, or null if the ref format is unsupported or the channel is not found
     */
    private static AsyncApi30Channel resolveChannelRef(String $ref, AsyncApiComponents components) {
        if (!$ref.startsWith(Constants.CHANNELS_REF_PREFIX)) {
            LOG.warn("Unsupported channel $ref format: {}. Skipping.", $ref);
            return null;
        }
        String name = $ref.substring(Constants.CHANNELS_REF_PREFIX.length());
        // Add additional version checks here as new AsyncAPI 3.x versions are supported.
        if (components instanceof AsyncApi30Components typedComponents) {
            Map<String, AsyncApi30Channel> channelsMap = typedComponents.getChannels();
            return channelsMap != null ? channelsMap.get(name) : null;
        }
        return null;
    }

    /**
     * Builds an {@link AsyncApiChannel} from an Apicurio channel object.
     * Add additional version checks here as new AsyncAPI 3.x versions are supported.
     *
     * @param channel    the Apicurio channel object
     * @param components the AsyncAPI components (for $ref resolution)
     * @param serversMap the map of server names to AsyncApiServer objects (for server resolution)
     * @return the mapped AsyncApiChannel, or null if the channel type is not supported
     */
    public static AsyncApiChannel buildChannel(
            Node channel,
            AsyncApiComponents components,
            Map<String, AsyncApiServer> serversMap) {
        // Add additional version checks here as new AsyncAPI 3.x versions are supported.
        if (channel instanceof AsyncApi30Channel typedChannel) {
            List<AsyncApiTag> tags = null;
            if (typedChannel.getTags() != null) {
                tags = typedChannel.getTags().stream()
                        .map(tag -> TagMapperV3.map(tag, null))
                        .toList();
            }
            return new AsyncApiChannel(
                    typedChannel.getAddress(),
                    MessageMapperV3.extractMessages(typedChannel, components),
                    typedChannel.getTitle(),
                    typedChannel.getSummary(),
                    typedChannel.getDescription(),
                    extractServers(channel, serversMap),
                    ChannelParameterMapperV3.mapParameters(
                            typedChannel.getParameters()),
                    tags,
                    ExternalDocMapperV3.map(typedChannel.getExternalDocs(), null),
                    ChannelBindingsMapperV3.map(typedChannel.getBindings()),
                    typedChannel.getExtensions()
            );
        }
        return null;
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
            Node channel,
            Map<String, AsyncApiServer> serversMap) {
        if (channel == null || serversMap == null) {
            return null;
        }
        // Add additional version checks here as new AsyncAPI 3.x versions are supported.
        if (channel instanceof AsyncApi30Channel typedChannel) {
            List<? extends AsyncApiReferenceable> serverRefs = typedChannel.getServers();
            if (serverRefs == null || serverRefs.isEmpty()) {
                return null;
            }
            List<AsyncApiServer> result = new ArrayList<>();
            for (AsyncApiReferenceable serverRef : serverRefs) {
                if (serverRef == null) {
                    continue;
                }
                String $ref = serverRef.get$ref();
                if ($ref == null) {
                    LOG.warn("Server reference in channel has no $ref. Skipping.");
                    continue;
                }
                String serverName = resolveServerRef($ref);
                if (serverName == null) {
                    LOG.warn("Unsupported server $ref format: {}. Skipping server.", $ref);
                    continue;
                }
                AsyncApiServer server = serversMap.get(serverName);
                if (server != null) {
                    result.add(server);
                } else {
                    LOG.warn("Server reference '{}' not found in servers map. Skipping.", serverName);
                }
            }
            return result.isEmpty() ? null : result;
        }
        return null;
    }

    /**
     * Resolves a $ref to a server name.
     *
     * @param $ref the $ref string (e.g., {@code #/servers/production})
     * @return the server name, or null if invalid format
     */
    private static String resolveServerRef(String $ref) {
        if (!$ref.startsWith(Constants.SERVERS_REF_PREFIX)) {
            return null;
        }
        return $ref.substring(Constants.SERVERS_REF_PREFIX.length());
    }
}
