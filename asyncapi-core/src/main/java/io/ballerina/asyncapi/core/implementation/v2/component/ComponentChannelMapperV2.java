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
package io.ballerina.asyncapi.core.implementation.v2.component;

import io.apicurio.datamodels.models.asyncapi.AsyncApiChannelItem;
import io.apicurio.datamodels.models.asyncapi.AsyncApiComponents;
import io.apicurio.datamodels.models.asyncapi.v20.AsyncApi20Components;
import io.apicurio.datamodels.models.asyncapi.v21.AsyncApi21Components;
import io.apicurio.datamodels.models.asyncapi.v22.AsyncApi22Components;
import io.apicurio.datamodels.models.asyncapi.v23.AsyncApi23Components;
import io.apicurio.datamodels.models.asyncapi.v24.AsyncApi24Components;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25Components;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26Components;
import io.ballerina.asyncapi.core.implementation.v2.channel.ChannelMapperV2;
import io.ballerina.asyncapi.core.model.channel.AsyncApiChannel;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps channel definitions from AsyncAPI 2.x components.
 * Channels in components are available from AsyncAPI 2.3 onwards.
 */
final class ComponentChannelMapperV2 {

    private ComponentChannelMapperV2() {

    }

    /**
     * Maps channels from AsyncAPI components.
     *
     * @param components the Apicurio components object
     * @return the map of channel names to channel objects, or null if no channels
     */
    static Map<String, AsyncApiChannel> map(AsyncApiComponents components) {
        if (components == null) {
            return null;
        }

        Map<String, ? extends AsyncApiChannelItem> rawChannels =
                switch (components) {
                    case AsyncApi26Components typed -> typed.getChannels();
                    case AsyncApi25Components typed -> typed.getChannels();
                    case AsyncApi24Components typed -> typed.getChannels();
                    case AsyncApi23Components typed -> typed.getChannels();
                    case AsyncApi22Components typed -> null;  // Not available in v2.2
                    case AsyncApi21Components typed -> null;  // Not available in v2.1
                    case AsyncApi20Components typed -> null;  // Not available in v2.0
                    default -> null;
                };

        if (rawChannels == null || rawChannels.isEmpty()) {
            return null;
        }

        Map<String, AsyncApiChannel> result = new HashMap<>();
        rawChannels.forEach((name, channelItem) -> {
            if (channelItem != null) {
                AsyncApiChannel mapped = mapChannel(name, channelItem, components);
                if (mapped != null) {
                    result.put(name, mapped);
                }
            }
        });

        return result.isEmpty() ? null : result;
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
        return ChannelMapperV2.mapChannelItem(name, channelItem, components, null);
    }
}
