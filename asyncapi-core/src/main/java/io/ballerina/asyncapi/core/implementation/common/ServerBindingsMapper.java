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
import io.apicurio.datamodels.models.asyncapi.AsyncApiComponents;
import io.apicurio.datamodels.models.asyncapi.AsyncApiExtensible;
import io.apicurio.datamodels.models.asyncapi.AsyncApiReferenceable;
import io.apicurio.datamodels.models.asyncapi.AsyncApiServerBindings;
import io.ballerina.asyncapi.core.Constants;
import io.ballerina.asyncapi.core.model.server.HttpServerBindings;
import io.ballerina.asyncapi.core.model.server.WsServerBindings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * Maps Apicurio {@link AsyncApiServerBindings} to
 * {@link io.ballerina.asyncapi.core.model.server.AsyncApiServerBindings},
 * version-independently.
 */
public final class ServerBindingsMapper {

    private static final Logger LOG = LogManager.getLogger(ServerBindingsMapper.class);

    private ServerBindingsMapper() {

    }

    /**
     * Maps Apicurio {@link AsyncApiServerBindings} to
     * {@link io.ballerina.asyncapi.core.model.server.AsyncApiServerBindings},
     * resolving any {@code $ref} via the provided components object.
     *
     * @param bindings   the Apicurio server bindings object (may be a reference)
     * @param components the Apicurio components object used for {@code $ref} resolution
     *                   (may be null)
     * @return the mapped AsyncApiServerBindings, or null if bindings is null, empty,
     *         or an unresolvable ref
     */
    public static io.ballerina.asyncapi.core.model.server.AsyncApiServerBindings mapBindings(
            AsyncApiServerBindings bindings, AsyncApiComponents components) {
        if (bindings == null) {
            return null;
        }
        String $ref = null;
        if (bindings instanceof AsyncApiReferenceable referenceable) {
            $ref = referenceable.get$ref();
        }
        if ($ref != null) {
            AsyncApiServerBindings resolved = resolveRef($ref, components);
            if (resolved == null) {
                LOG.warn("Could not resolve $ref: {}. Skipping server bindings.", $ref);
                return null;
            }
            if (resolved instanceof AsyncApiReferenceable resolvedTyped && resolvedTyped.get$ref() != null) {
                LOG.warn("Resolved $ref points to another $ref: {}. Skipping server bindings.",
                        resolvedTyped.get$ref());
                return null;
            }
            return mapBindings(resolved, components);
        }
        HttpServerBindings http = bindings.getHttp() != null ? new HttpServerBindings() : null;
        WsServerBindings ws = bindings.getWs() != null ? new WsServerBindings() : null;
        Map<String, JsonNode> extensions = null;
        if (bindings instanceof AsyncApiExtensible extensible) {
            extensions = extensible.getExtensions();
        }
        if (http == null && ws == null && extensions == null) {
            return null;
        }
        return new io.ballerina.asyncapi.core.model.server.AsyncApiServerBindings(http, ws, extensions);
    }

    /**
     * Resolves a {@code $ref} to a component server binding by extracting the name from the
     * reference string and looking it up in the components map.
     *
     * @param $ref       the reference string (e.g. {@code #/components/serverBindings/MyBinding})
     * @param components the Apicurio components object
     * @return the resolved server bindings, or null if not found
     */
    private static AsyncApiServerBindings resolveRef(String $ref, AsyncApiComponents components) {
        if (!$ref.startsWith(Constants.SERVER_BINDINGS_REF_PREFIX)) {
            LOG.warn("Unsupported $ref format: {}. Skipping server bindings.", $ref);
            return null;
        }
        String name = $ref.substring(Constants.SERVER_BINDINGS_REF_PREFIX.length());
        if (components == null) {
            return null;
        }
        Map<String, ? extends AsyncApiServerBindings> bindingsMap = components.getServerBindings();
        return bindingsMap != null ? bindingsMap.get(name) : null;
    }
}
