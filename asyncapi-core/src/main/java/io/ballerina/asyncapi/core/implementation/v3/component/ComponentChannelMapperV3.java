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
package io.ballerina.asyncapi.core.implementation.v3.component;

import io.apicurio.datamodels.models.asyncapi.AsyncApiComponents;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Channel;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Components;
import io.ballerina.asyncapi.core.implementation.v3.channel.ChannelMapperV3;
import io.ballerina.asyncapi.core.model.channel.AsyncApiChannel;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps channel definitions from AsyncAPI 3.x components.
 */
final class ComponentChannelMapperV3 {

    private ComponentChannelMapperV3() {

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

        // Add additional version cases here as new AsyncAPI 3.x versions are supported.
        Map<String, ? extends AsyncApi30Channel> rawChannels = switch (components) {
                    case AsyncApi30Components typed -> typed.getChannels();
                    default -> null;
                };

        if (rawChannels == null || rawChannels.isEmpty()) {
            return null;
        }

        Map<String, AsyncApiChannel> result = new HashMap<>();
        rawChannels.forEach((name, channel) -> {
            if (channel != null) {
                AsyncApiChannel mapped = ChannelMapperV3.mapChannelItem(name, channel, components, null);
                if (mapped != null) {
                    result.put(name, mapped);
                }
            }
        });

        return result.isEmpty() ? null : result;
    }

}
