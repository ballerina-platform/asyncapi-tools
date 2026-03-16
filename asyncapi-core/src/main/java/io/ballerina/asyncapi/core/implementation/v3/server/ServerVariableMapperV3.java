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
package io.ballerina.asyncapi.core.implementation.v3.server;

import io.apicurio.datamodels.models.ServerVariable;
import io.apicurio.datamodels.models.asyncapi.AsyncApiComponents;
import io.apicurio.datamodels.models.asyncapi.AsyncApiReferenceable;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Components;
import io.ballerina.asyncapi.core.Constants;
import io.ballerina.asyncapi.core.implementation.common.ServerVariableMapper;
import io.ballerina.asyncapi.core.model.server.AsyncApiServerVariable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Maps Apicurio {@link ServerVariable} to {@link AsyncApiServerVariable} for AsyncAPI 3.0.
 */
final class ServerVariableMapperV3 {

    private static final Logger LOG = LogManager.getLogger(ServerVariableMapperV3.class);

    private ServerVariableMapperV3() {
    }

    /**
     * Maps a map of Apicurio server variables to a map of {@link AsyncApiServerVariable},
     * resolving any {@code $ref} entries via the provided components object.
     *
     * @param variables  the Apicurio server variables map
     * @param components the Apicurio components object used for {@code $ref} resolution
     *                   (may be null)
     * @return the mapped variables map, or null if variables is null or empty
     */
    static Map<String, AsyncApiServerVariable> map(
            Map<String, ? extends ServerVariable> variables, AsyncApiComponents components) {
        if (variables == null || variables.isEmpty()) {
            return null;
        }
        Map<String, AsyncApiServerVariable> result = new HashMap<>();
        for (Map.Entry<String, ? extends ServerVariable> entry : variables.entrySet()) {
            AsyncApiServerVariable mapped = mapVariableItem(entry.getValue(), components);
            if (mapped != null) {
                result.put(entry.getKey(), mapped);
            }
        }
        return result;
    }

    /**
     * Maps a single Apicurio {@link ServerVariable}, resolving any {@code $ref} before.
     *
     * @param variable   the Apicurio server variable object (may be a reference)
     * @param components the Apicurio components object used for ref resolution
     * @return the mapped AsyncApiServerVariable, or null for unresolvable refs
     */
    private static AsyncApiServerVariable mapVariableItem(ServerVariable variable, AsyncApiComponents components) {
        String ref = null;
        if (variable instanceof AsyncApiReferenceable referenceable) {
            ref = referenceable.get$ref();
        }
        if (ref != null) {
            ServerVariable resolved = resolveRef(ref, components);
            if (resolved == null) {
                LOG.warn("Could not resolve $ref: {}. Skipping server variable.", ref);
                return null;
            }
            return mapVariableItem(resolved, components);
        }
        return ServerVariableMapper.mapVariable(variable);
    }

    /**
     * Resolves a {@code $ref} to a component server variable by extracting the name from the
     * reference string and looking it up in the AsyncAPI 3.0 components map.
     *
     * @param ref        the reference string (e.g. {@code #/components/serverVariables/MyVar})
     * @param components the Apicurio components object
     * @return the resolved server variable, or null if not found
     */
    private static ServerVariable resolveRef(String ref, AsyncApiComponents components) {
        if (components == null) {
            LOG.warn("Cannot resolve $ref: {}. Components is null.", ref);
            return null;
        }
        Set<String> visited = new HashSet<>();
        String current = ref;
        while (current != null) {
            if (!current.startsWith(Constants.SERVER_VARIABLES_REF_PREFIX)) {
                LOG.warn("Unsupported $ref format: {}. Skipping server variable.", current);
                return null;
            }
            if (!visited.add(current)) {
                LOG.warn("Cyclic $ref detected: {}. Skipping server variable.", current);
                return null;
            }
            String name = current.substring(Constants.SERVER_VARIABLES_REF_PREFIX.length());
            // Add additional version checks here as new AsyncAPI 3.x versions are supported.
            ServerVariable resolved = null;
            if (components instanceof AsyncApi30Components typed) {
                resolved = typed.getServerVariables() != null
                        ? typed.getServerVariables().get(name) : null;
            }
            if (resolved == null) {
                LOG.warn("Could not resolve $ref: '{}'. No matching server variable found.", current);
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
