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
package io.ballerina.asyncapi.generator.ws.client.extractor;

import com.fasterxml.jackson.databind.JsonNode;
import io.ballerina.asyncapi.core.SpecWarning;
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.core.model.channel.AsyncApiChannel;
import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.ws.client.model.ChannelSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * Extracts the channels a client is generated for, with their WebSocket bindings, from an AsyncAPI specification.
 * Each channel is a connection of its own, so each gets its own client.
 */
public class ChannelExtractor {

    private static final String NO_OPERATIONS = "channel '%s' has no operations, so no client is generated for it";
    private static final String NO_OPERATIONS_FIX = "Add a send or receive operation on the channel to get a "
            + "client for it.";
    private static final String NO_CHANNEL_WITH_OPERATIONS = "None of the %d channels in the AsyncAPI spec has an "
            + "operation, so there is no client to generate. Add a send or receive operation on a channel.";

    /**
     * Holds the channel name, channel definition, and its WebSocket binding schemas.
     *
     * @param channelName  the channel map key
     * @param channel      the channel definition
     * @param querySchema  the WS binding query schema node, or {@code null} if absent
     * @param headerSchema the WS binding header schema node, or {@code null} if absent
     * @param spec         the spec the channel's client is generated from: only this channel and its operations
     *                     when the spec has several
     */
    public record ChannelInfo(String channelName, AsyncApiChannel channel,
                               JsonNode querySchema, JsonNode headerSchema, AsyncApiSpec spec) { }

    private final AsyncApiSpec asyncApiSpec;

    /**
     * Creates a new extractor backed by the given spec.
     *
     * @param asyncApiSpec the parsed AsyncAPI specification
     */
    public ChannelExtractor(AsyncApiSpec asyncApiSpec) {
        this.asyncApiSpec = asyncApiSpec;
    }

    /**
     * Extracts every channel a client is generated for, in channel name order. A spec with one channel gets a
     * client for it as is. With several, a channel without operations is left out with a warning.
     *
     * @param warnings where to report a channel that is left out
     * @return the channels to generate a client for, never empty
     * @throws GeneratorException if no channels are defined, or none of several has an operation
     */
    public List<ChannelInfo> extract(Consumer<SpecWarning> warnings) throws GeneratorException {
        Map<String, AsyncApiChannel> channelMap = asyncApiSpec.getAsyncApiChannels().orElse(null);
        if (channelMap == null || channelMap.isEmpty()) {
            throw new GeneratorException("AsyncAPI spec must have at least one channel");
        }
        if (channelMap.size() == 1) {
            Map.Entry<String, AsyncApiChannel> entry = channelMap.entrySet().iterator().next();
            return List.of(channelInfo(entry.getKey(), entry.getValue(), asyncApiSpec));
        }
        List<ChannelInfo> result = new ArrayList<>();
        // Sorted, as the parsed channel map has no fixed order
        for (Map.Entry<String, AsyncApiChannel> entry : new TreeMap<>(channelMap).entrySet()) {
            ChannelSpec channelSpec = ChannelSpec.of(asyncApiSpec, entry.getKey(), entry.getValue());
            if (channelSpec.getAsyncApiOperations().orElse(Map.of()).isEmpty()) {
                warnings.accept(new SpecWarning("/channels/" + pointerToken(entry.getKey()),
                        String.format(NO_OPERATIONS, entry.getKey()), NO_OPERATIONS_FIX));
                continue;
            }
            result.add(channelInfo(entry.getKey(), entry.getValue(), channelSpec));
        }
        if (result.isEmpty()) {
            throw new GeneratorException(String.format(NO_CHANNEL_WITH_OPERATIONS, channelMap.size()));
        }
        return result;
    }

    private static ChannelInfo channelInfo(String name, AsyncApiChannel channel, AsyncApiSpec spec) {
        JsonNode querySchema = null;
        JsonNode headerSchema = null;
        if (channel.bindings() != null && channel.bindings().wsChannelBindings() != null) {
            querySchema = channel.bindings().wsChannelBindings().query();
            headerSchema = channel.bindings().wsChannelBindings().headers();
        }
        return new ChannelInfo(name, channel, querySchema, headerSchema, spec);
    }

    private static String pointerToken(String name) {
        return name.replace("~", "~0").replace("/", "~1");
    }
}
