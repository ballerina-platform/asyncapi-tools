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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Maps Apicurio channel bindings to {@link AsyncApiChannelBindings} for AsyncAPI 3.0.
 */
public final class ChannelBindingsMapperV3 {

    private static final Logger LOG = LogManager.getLogger(ChannelBindingsMapperV3.class);

    private ChannelBindingsMapperV3() {
    }

    /**
     * @param bindings   the Apicurio channel bindings object
     * @param components the AsyncAPI components (for $ref resolution)
     * @return the mapped AsyncApiChannelBindings, or null if bindings is null or empty
     */
    public static AsyncApiChannelBindings map(io.apicurio.datamodels.models.asyncapi.AsyncApiChannelBindings bindings,
                                       AsyncApiComponents components) {
        if (bindings == null) {
            return null;
        }

        // Handle $ref
        if (bindings instanceof AsyncApiReferenceable referenceable && referenceable.get$ref() != null) {
            String $ref = referenceable.get$ref();
            io.apicurio.datamodels.models.asyncapi.AsyncApiChannelBindings resolved = resolveRef($ref, components);
            if (resolved == null) {
                LOG.warn("Could not resolve $ref: {}. Skipping channelBindings.", $ref);
                return null;
            }
            return map(resolved, components);
        }

        Map<String, JsonNode> extensions = null;
        if (bindings instanceof AsyncApiExtensible extensible) {
            extensions = extensible.getExtensions();
        }
        HttpChannelBindings http = HttpChannelBindingMapper.map(bindings.getHttp());
        WsChannelBindings ws = WsChannelBindingMapper.map(bindings.getWs(), components);

        if (http == null && ws == null && extensions == null) {
            return null;
        }
        return new AsyncApiChannelBindings(http, ws, extensions);
    }

    /**
     * Resolves a {@code $ref} to a component channel bindings object, following chained references
     * and detecting cycles.
     *
     * @param $ref       the reference string (e.g. {@code #/components/channelBindings/MyBindings})
     * @param components the Apicurio components object
     * @return the resolved channel bindings, or null if not found or unsupported
     */
    private static io.apicurio.datamodels.models.asyncapi.AsyncApiChannelBindings resolveRef(
            String $ref, AsyncApiComponents components) {
        if (components == null) {
            LOG.warn("Cannot resolve $ref: {}. Components is null.", $ref);
            return null;
        }
        Set<String> visited = new HashSet<>();
        String current = $ref;
        while (current != null) {
            if (!current.startsWith(Constants.CHANNEL_BINDINGS_REF_PREFIX)) {
                LOG.warn("Unsupported $ref format: {}. Skipping channelBindings.", current);
                return null;
            }
            if (!visited.add(current)) {
                LOG.warn("Cyclic $ref detected: {}. Skipping channelBindings.", current);
                return null;
            }
            String name = current.substring(Constants.CHANNEL_BINDINGS_REF_PREFIX.length());
            Map<String, ? extends io.apicurio.datamodels.models.asyncapi.AsyncApiChannelBindings> bindingsMap =
                    components.getChannelBindings();
            io.apicurio.datamodels.models.asyncapi.AsyncApiChannelBindings resolved =
                    bindingsMap != null ? bindingsMap.get(name) : null;
            if (resolved == null) {
                LOG.warn("Could not resolve $ref: '{}'. No matching channelBindings found.", current);
                return null;
            }
            if (resolved instanceof AsyncApiReferenceable resolvedTyped && resolvedTyped.get$ref() != null) {
                current = resolvedTyped.get$ref();
            } else {
                return resolved;
            }
        }
        return null;
    }
}
