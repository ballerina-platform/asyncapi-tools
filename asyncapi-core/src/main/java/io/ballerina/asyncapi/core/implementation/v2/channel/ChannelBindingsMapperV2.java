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
import io.apicurio.datamodels.models.asyncapi.AsyncApiComponents;
import io.apicurio.datamodels.models.asyncapi.AsyncApiExtensible;
import io.apicurio.datamodels.models.asyncapi.AsyncApiReferenceable;
import io.ballerina.asyncapi.core.Constants;
import io.ballerina.asyncapi.core.implementation.common.HttpChannelBindingMapper;
import io.ballerina.asyncapi.core.implementation.common.WsChannelBindingMapper;
import io.ballerina.asyncapi.core.model.channel.AsyncApiChannelBindings;
import io.ballerina.asyncapi.core.model.channel.HttpChannelBindings;
import io.ballerina.asyncapi.core.model.channel.WsChannelBindings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * Maps Apicurio channel bindings to {@link AsyncApiChannelBindings} for AsyncAPI 2.x.
 */
public final class ChannelBindingsMapperV2 {

    private static final Logger LOG = LogManager.getLogger(ChannelBindingsMapperV2.class);

    private ChannelBindingsMapperV2() {
    }

    /**
     * @param bindings   the Apicurio channel bindings object
     * @param components the AsyncAPI components (for $ref resolution)
     * @return the mapped AsyncApiChannelBindings, or null if bindings is null or empty
     */
    public static AsyncApiChannelBindings map(
            io.apicurio.datamodels.models.asyncapi.AsyncApiChannelBindings bindings,
            AsyncApiComponents components) {
        if (bindings == null) {
            return null;
        }

        Map<String, JsonNode> extensions = null;

        if (bindings instanceof AsyncApiReferenceable referenceable) {
            String $ref = referenceable.get$ref();
            if ($ref != null) {
                return resolveAndMap($ref, components);
            }
        }

        if (bindings instanceof AsyncApiExtensible extensible) {
            extensions = extensible.getExtensions();
        }

        // Extract protocol-specific bindings
        HttpChannelBindings http = HttpChannelBindingMapper.map(bindings.getHttp());
        WsChannelBindings ws = WsChannelBindingMapper.map(bindings.getWs());

        if (http == null && ws == null && extensions == null) {
            return null;
        }

        return new AsyncApiChannelBindings(http, ws, extensions);
    }

    private static AsyncApiChannelBindings resolveAndMap(
            String $ref,
            AsyncApiComponents components) {

        io.apicurio.datamodels.models.asyncapi.AsyncApiChannelBindings resolved =
                resolveRefFromComponents($ref, components);
        if (resolved == null) {
            LOG.warn("Could not resolve $ref: {}. Skipping channelBindings.", $ref);
            return null;
        }

        // Guard against chained $refs
        if (resolved instanceof AsyncApiReferenceable referenceable
                && referenceable.get$ref() != null) {
            LOG.warn("Resolved $ref points to another $ref: {}. Skipping channelBindings.",
                    referenceable.get$ref());
            return null;
        }
        return map(resolved, components);
    }

    /**
     * Resolves a $ref to a channel binding in components.
     *
     * @param $ref       the $ref string (e.g., "#/components/channelBindings/MyBinding")
     * @param components the AsyncAPI components object
     * @return the resolved channel bindings, or null if not found or invalid
     */
    private static io.apicurio.datamodels.models.asyncapi.AsyncApiChannelBindings resolveRefFromComponents(
            String $ref, AsyncApiComponents components) {
        if (!$ref.startsWith(Constants.CHANNEL_BINDINGS_REF_PREFIX)) {
            LOG.warn("Unsupported $ref format: {}. Skipping channelBindings.", $ref);
            return null;
        }

        String name = $ref.substring(Constants.CHANNEL_BINDINGS_REF_PREFIX.length());

        if (components == null) {
            return null;
        }

        Map<String, ? extends io.apicurio.datamodels.models.asyncapi.AsyncApiChannelBindings>
                bindingsMap = components.getChannelBindings();
        return bindingsMap != null ? bindingsMap.get(name) : null;
    }
}
