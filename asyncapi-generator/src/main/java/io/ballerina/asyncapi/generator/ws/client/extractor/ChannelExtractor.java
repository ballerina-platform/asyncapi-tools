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
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.core.model.channel.AsyncApiChannel;
import io.ballerina.asyncapi.generator.GeneratorException;

import java.util.Map;

/**
 * Extracts the single channel and its WebSocket bindings from an AsyncAPI specification.
 * Enforces the constraint that exactly one channel must be present.
 */
public class ChannelExtractor {

    /**
     * Holds the channel name, channel definition, and its WebSocket binding schemas.
     *
     * @param channelName  the channel map key
     * @param channel      the channel definition
     * @param querySchema  the WS binding query schema node, or {@code null} if absent
     * @param headerSchema the WS binding header schema node, or {@code null} if absent
     */
    public record ChannelInfo(String channelName, AsyncApiChannel channel,
                               JsonNode querySchema, JsonNode headerSchema) { }

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
     * Extracts and validates the single channel and its WebSocket bindings from the spec.
     *
     * @return a {@link ChannelInfo} containing the channel name, channel, and binding schemas
     * @throws GeneratorException if no channels are defined or more than one channel is present
     */
    public ChannelInfo extract() throws GeneratorException {
        Map<String, AsyncApiChannel> channelMap = asyncApiSpec.getAsyncApiChannels().orElse(null);
        if (channelMap == null || channelMap.isEmpty()) {
            throw new GeneratorException("AsyncAPI spec must have at least one channel");
        }
        if (channelMap.size() > 1) {
            throw new GeneratorException(
                    "AsyncAPI spec must have exactly one channel; multiple channels are not supported");
        }
        Map.Entry<String, AsyncApiChannel> entry = channelMap.entrySet().iterator().next();
        AsyncApiChannel channel = entry.getValue();

        JsonNode querySchema = null;
        JsonNode headerSchema = null;
        if (channel.bindings() != null && channel.bindings().wsChannelBindings() != null) {
            querySchema = channel.bindings().wsChannelBindings().query();
            headerSchema = channel.bindings().wsChannelBindings().headers();
        }
        return new ChannelInfo(entry.getKey(), channel, querySchema, headerSchema);
    }
}
