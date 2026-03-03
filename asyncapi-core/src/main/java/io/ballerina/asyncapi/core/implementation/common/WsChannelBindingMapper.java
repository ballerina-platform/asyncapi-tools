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
package io.ballerina.asyncapi.core.implementation.common;

import com.fasterxml.jackson.databind.JsonNode;
import io.apicurio.datamodels.models.asyncapi.AsyncApiBinding;
import io.apicurio.datamodels.models.asyncapi.AsyncApiChannelBindings;
import io.apicurio.datamodels.models.asyncapi.AsyncApiComponents;
import io.apicurio.datamodels.models.asyncapi.AsyncApiReferenceable;
import io.ballerina.asyncapi.core.Constants;
import io.ballerina.asyncapi.core.implementation.utils.BindingUtils;
import io.ballerina.asyncapi.core.model.channel.WsChannelBindings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * Maps Apicurio WebSocket channel bindings to {@link WsChannelBindings},
 * version-independently (AsyncAPI 2.x and 3.x).
 */
public final class WsChannelBindingMapper {

    private static final Logger LOG = LogManager.getLogger(WsChannelBindingMapper.class);

    private WsChannelBindingMapper() {
    }

    /**
     * Maps an Apicurio WebSocket channel binding to {@link WsChannelBindings},
     * resolving any {@code $ref} references before building the model.
     *
     * @param binding    the Apicurio WebSocket binding object (may be a reference)
     * @param components the AsyncAPI components (for $ref resolution)
     * @return the mapped WsChannelBindings, or null if binding is null
     */
    public static WsChannelBindings map(AsyncApiBinding binding, AsyncApiComponents components) {
        if (binding == null) {
            return null;
        }

        String $ref = binding instanceof AsyncApiReferenceable ? ((AsyncApiReferenceable) binding).get$ref() : null;

        if ($ref != null) {
            if (!$ref.startsWith(Constants.CHANNEL_BINDINGS_REF_PREFIX)) {
                LOG.warn("Unsupported $ref format: {}. Skipping ws binding.", $ref);
                return null;
            }
            if (components == null) {
                LOG.warn("Cannot resolve $ref: {}. Components is null.", $ref);
                return null;
            }
            String name = $ref.substring(Constants.CHANNEL_BINDINGS_REF_PREFIX.length());
            Map<String, ? extends AsyncApiChannelBindings> bindingsMap = components.getChannelBindings();
            AsyncApiChannelBindings resolved = bindingsMap != null ? bindingsMap.get(name) : null;
            if (resolved == null) {
                LOG.warn("Could not resolve $ref: '{}'. No matching channelBindings found.", $ref);
                return null;
            }
            return map(resolved.getWs(), components);
        }

        String method = BindingUtils.getItemAsText(binding, "method");
        JsonNode query = binding.getItem("query");
        JsonNode headers = binding.getItem("headers");
        String bindingVersion = BindingUtils.getItemAsText(binding, "bindingVersion");
        return new WsChannelBindings(method, query, headers, bindingVersion);
    }
}
