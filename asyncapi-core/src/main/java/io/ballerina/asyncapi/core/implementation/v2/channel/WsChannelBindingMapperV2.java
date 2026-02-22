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
import io.ballerina.asyncapi.core.model.channel.WsChannelBindings;
import io.ballerina.asyncapi.core.model.component.AsyncApiSchema;

/**
 * Maps Apicurio WebSocket channel binding to {@link WsChannelBindings} for AsyncAPI 2.x.
 */
final class WsChannelBindingMapperV2 {

    private WsChannelBindingMapperV2() {
    }

    /**
     * Maps an Apicurio WebSocket channel binding to {@link WsChannelBindings}.
     *
     * @param binding the Apicurio WebSocket binding object
     * @return the mapped WsChannelBindings, or null if binding is null
     */
    static WsChannelBindings map(io.apicurio.datamodels.models.asyncapi.AsyncApiBinding binding) {
        if (binding == null) {
            return null;
        }

        String method = getBindingItemAsText(binding, "method");
        Object query = binding.getItem("query");
        Object headers = binding.getItem("headers");
        String bindingVersion = getBindingItemAsText(binding, "bindingVersion");

        return new WsChannelBindings(method, (AsyncApiSchema) query, (AsyncApiSchema) headers, bindingVersion);
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
        JsonNode node = binding.getItem(key);
        if (node == null) {
            return null;
        }
        return node.isTextual() ? node.asText() : node.toString();
    }
}
