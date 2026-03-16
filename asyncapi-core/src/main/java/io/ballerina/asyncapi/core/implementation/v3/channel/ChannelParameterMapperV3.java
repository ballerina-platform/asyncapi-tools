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

import io.apicurio.datamodels.models.Parameter;
import io.apicurio.datamodels.models.asyncapi.AsyncApiComponents;
import io.apicurio.datamodels.models.asyncapi.AsyncApiParameter;
import io.apicurio.datamodels.models.asyncapi.AsyncApiParameters;
import io.apicurio.datamodels.models.asyncapi.AsyncApiReferenceable;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Components;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Parameter;
import io.ballerina.asyncapi.core.Constants;
import io.ballerina.asyncapi.core.model.channel.AsyncApiChannelParameter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Maps Apicurio Parameter models to {@link AsyncApiChannelParameter}
 * for AsyncAPI 3.0.
 */
final class ChannelParameterMapperV3 {

    private static final Logger LOG = LogManager.getLogger(ChannelParameterMapperV3.class);

    private ChannelParameterMapperV3() {
    }

    /**
     * Maps Apicurio {@link AsyncApiParameters} to a map of parameter names to
     * {@link AsyncApiChannelParameter}, resolving any {@code $ref} references before building the model.
     *
     * @param parameters the Apicurio parameters object
     * @param components the AsyncAPI components (for $ref resolution)
     * @return the mapped parameters map, or null if parameters is null or empty
     */
    static Map<String, AsyncApiChannelParameter> map(AsyncApiParameters parameters,
                                                     AsyncApiComponents components) {
        if (parameters == null) {
            return null;
        }
        List<String> names = parameters.getItemNames();
        if (names == null || names.isEmpty()) {
            return null;
        }
        Map<String, AsyncApiChannelParameter> result = new HashMap<>();
        for (String name : names) {
            AsyncApiChannelParameter mapped = mapParameterItem(name, parameters.getItem(name), components);
            if (mapped != null) {
                result.put(name, mapped);
            }
        }
        return result.isEmpty() ? null : result;
    }

    /**
     * Maps a single Apicurio {@link AsyncApiParameter} to an {@link AsyncApiChannelParameter},
     * resolving any {@code $ref} references before building the model.
     *
     * @param name       the parameter name (used for logging)
     * @param param      the Apicurio parameter object (may be a reference)
     * @param components the Apicurio components object used for ref resolution
     * @return the mapped AsyncApiChannelParameter, or null for unresolvable refs or unknown types
     */
    static AsyncApiChannelParameter mapParameterItem(String name, AsyncApiParameter param,
                                                     AsyncApiComponents components) {
        if (param instanceof AsyncApiReferenceable referenceable && referenceable.get$ref() != null) {
            param = resolveRef(referenceable.get$ref(), components);
            if (param == null) {
                LOG.warn("Could not resolve $ref for parameter '{}'. Skipping.", name);
                return null;
            }
        }

        // Add additional version checks here as new AsyncAPI 3.x versions are supported.
        if (param instanceof AsyncApi30Parameter typed) {
            return new AsyncApiChannelParameter(
                    typed.getDescription(),
                    typed.getDefault(),
                    typed.getEnum(),
                    typed.getExamples(),
                    typed.getLocation(),
                    typed.getExtensions()
            );
        }

        LOG.warn("Unsupported AsyncAPI parameter type: {}. Skipping.", param.getClass().getName());
        return null;
    }

    /**
     * Resolves a {@code $ref} to a component parameter by extracting the name from the reference
     * string and looking it up in the components parameters map.
     *
     * @param ref        the reference string (e.g. {@code #/components/parameters/myParam})
     * @param components the Apicurio components object
     * @return the resolved parameter, or null if not found or invalid
     */
    private static AsyncApiParameter resolveRef(String ref, AsyncApiComponents components) {
        if (components == null) {
            LOG.warn("Cannot resolve $ref: {}. Components is null.", ref);
            return null;
        }
        // Add additional version checks here as new AsyncAPI 3.x versions are supported.
        if (!(components instanceof AsyncApi30Components typedComponents)) {
            LOG.warn("Cannot resolve $ref: {}. Unsupported components version.", ref);
            return null;
        }
        Set<String> visited = new HashSet<>();
        String current = ref;
        while (current != null) {
            if (!current.startsWith(Constants.PARAMETERS_REF_PREFIX)) {
                LOG.warn("Unsupported $ref format: {}. Skipping parameter.", current);
                return null;
            }
            if (!visited.add(current)) {
                LOG.warn("Cyclic $ref detected: {}. Skipping parameter.", current);
                return null;
            }
            String paramName = current.substring(Constants.PARAMETERS_REF_PREFIX.length());
            Map<String, Parameter> parametersMap = typedComponents.getParameters();
            Parameter entry = parametersMap != null ? parametersMap.get(paramName) : null;
            AsyncApiParameter resolved = entry instanceof AsyncApiParameter p ? p : null;
            if (resolved == null) {
                LOG.warn("Could not resolve $ref: '{}'. No matching parameter found.", current);
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
