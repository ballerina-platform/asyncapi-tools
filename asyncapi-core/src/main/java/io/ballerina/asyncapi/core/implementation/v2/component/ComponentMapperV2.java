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

import com.fasterxml.jackson.databind.JsonNode;
import io.apicurio.datamodels.models.Parameter;
import io.apicurio.datamodels.models.SecurityScheme;
import io.apicurio.datamodels.models.asyncapi.AsyncApiComponents;
import io.apicurio.datamodels.models.asyncapi.AsyncApiExtensible;
import io.apicurio.datamodels.models.asyncapi.AsyncApiParameter;
import io.apicurio.datamodels.models.asyncapi.AsyncApiSchema;
import io.apicurio.datamodels.models.asyncapi.v20.AsyncApi20Parameter;
import io.apicurio.datamodels.models.asyncapi.v20.AsyncApi20SecurityScheme;
import io.apicurio.datamodels.models.asyncapi.v21.AsyncApi21Parameter;
import io.apicurio.datamodels.models.asyncapi.v22.AsyncApi22Parameter;
import io.apicurio.datamodels.models.asyncapi.v23.AsyncApi23Parameter;
import io.apicurio.datamodels.models.asyncapi.v24.AsyncApi24Parameter;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25Parameter;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26Components;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26Parameter;
import io.apicurio.datamodels.models.asyncapi.v20.AsyncApi20Components;
import io.apicurio.datamodels.models.asyncapi.v21.AsyncApi21Components;
import io.apicurio.datamodels.models.asyncapi.v22.AsyncApi22Components;
import io.apicurio.datamodels.models.asyncapi.v23.AsyncApi23Components;
import io.apicurio.datamodels.models.asyncapi.v24.AsyncApi24Components;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25Components;
import io.ballerina.asyncapi.core.implementation.common.SchemaMapper;
import io.ballerina.asyncapi.core.implementation.utils.JsonNodeUtils;
import io.ballerina.asyncapi.core.implementation.v2.channel.ChannelBindingsMapperV2;
import io.ballerina.asyncapi.core.implementation.v2.message.CorrelationIdMapperV2;
import io.ballerina.asyncapi.core.implementation.v2.message.MessageMapperV2;
import io.ballerina.asyncapi.core.implementation.v2.message.MessageTraitMapperV2;
import io.ballerina.asyncapi.core.implementation.v2.operation.OperationBindingsMapperV2;
import io.ballerina.asyncapi.core.implementation.v2.operation.OperationTraitMapperV2;
import io.ballerina.asyncapi.core.implementation.v2.server.SecuritySchemeMapperV2;
import io.ballerina.asyncapi.core.implementation.common.ServerBindingsMapper;
import io.ballerina.asyncapi.core.implementation.v2.server.ServerMapperV2;
import io.ballerina.asyncapi.core.model.channel.AsyncApiChannelParameter;
import io.ballerina.asyncapi.core.model.component.AsyncApiComponent;
import io.ballerina.asyncapi.core.model.security.AsyncApiSecurityScheme;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Maps Apicurio Components to {@link AsyncApiComponent} for AsyncAPI 2.x.
 */
public final class ComponentMapperV2 {

    private ComponentMapperV2() {

    }

    /**
     * Maps Apicurio {@link AsyncApiComponents} to {@link AsyncApiComponent}.
     *
     * @param components the Apicurio components object
     * @return the mapped AsyncApiComponent, or null if components is null
     */
    public static AsyncApiComponent map(AsyncApiComponents components) {
        if (components == null) {
            return null;
        }

        Map<String, JsonNode> extensions = switch (components) {
            case AsyncApi26Components typed -> typed.getExtensions();
            case AsyncApi25Components typed -> typed.getExtensions();
            case AsyncApi24Components typed -> typed.getExtensions();
            case AsyncApi23Components typed -> typed.getExtensions();
            case AsyncApi22Components typed -> typed.getExtensions();
            case AsyncApi21Components typed -> typed.getExtensions();
            case AsyncApi20Components typed -> typed.getExtensions();
            default -> null;
        };

        return new AsyncApiComponent(
                mapSchemas(components),
                mapComponentServers(components),
                mapComponentServerVariables(components),
                mapComponentChannels(components),
                null, // operations - not in AsyncAPI 2.x components
                mapValues(components.getMessages(),
                        MessageMapperV2::map),
                mapSecuritySchemes(
                        components.getSecuritySchemes()),
                mapParameters(components.getParameters()),
                mapValues(components.getCorrelationIds(),
                        CorrelationIdMapperV2::map),
                mapValues(components.getOperationTraits(),
                        OperationTraitMapperV2::map),
                mapValues(components.getMessageTraits(),
                        MessageTraitMapperV2::map),
                null, // replies - not in AsyncAPI 2.x
                null, // replyAddresses - not in AsyncAPI 2.x
                null, // externalDocs - not in AsyncAPI 2.x components
                null, // tags - not in AsyncAPI 2.x components
                mapValues(components.getServerBindings(),
                        binding -> ServerBindingsMapper.mapBindings(binding, null)),
                mapValues(components.getChannelBindings(),
                        binding -> ChannelBindingsMapperV2.map(binding, components)),
                mapValues(components.getOperationBindings(),
                        OperationBindingsMapperV2::map),
                mapValues(components.getMessageBindings(),
                        MessageMapperV2::mapBindings),
                extensions
        );
    }

    /**
     * Iterates a nullable map, applies a mapping function to each value,
     * and returns a new map with non-null results.
     *
     * @param <T>    the source value type
     * @param <R>    the result value type
     * @param source the source map (may be null)
     * @param mapper the mapping function
     * @return the mapped result map, or null if source is null/empty
     */
    private static <T, R> Map<String, R> mapValues(
            Map<String, ? extends T> source,
            Function<? super T, R> mapper) {
        if (source == null || source.isEmpty()) {
            return null;
        }
        Map<String, R> result = new LinkedHashMap<>();
        for (Map.Entry<String, ? extends T> entry : source.entrySet()) {
            R mapped = mapper.apply(entry.getValue());
            if (mapped != null) {
                result.put(entry.getKey(), mapped);
            }
        }
        return result.isEmpty() ? null : result;
    }

    /**
     * Maps component security schemes.
     *
     * @param schemes the Apicurio security schemes map
     * @return the mapped security schemes map, or null if empty
     */
    private static Map<String, AsyncApiSecurityScheme>
            mapSecuritySchemes(
            Map<String, ? extends SecurityScheme> schemes) {
        if (schemes == null || schemes.isEmpty()) {
            return null;
        }
        Map<String, AsyncApiSecurityScheme> result = new LinkedHashMap<>();
        for (Map.Entry<String, ? extends SecurityScheme> entry : schemes.entrySet()) {
            if (entry.getValue() instanceof AsyncApi20SecurityScheme typed) {
                result.put(entry.getKey(),
                        SecuritySchemeMapperV2.map(typed));
            }
        }
        return result.isEmpty() ? null : result;
    }

    /**
     * Maps component parameters.
     *
     * @param parameters the Apicurio parameters map
     * @return the mapped parameters map, or null if empty
     */
    private static Map<String, AsyncApiChannelParameter>
            mapParameters(
            Map<String, ? extends Parameter> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return null;
        }
        Map<String, AsyncApiChannelParameter> result = new LinkedHashMap<>();
        for (Map.Entry<String, ? extends Parameter> entry : parameters.entrySet()) {
            Parameter param = entry.getValue();
            Map<String, JsonNode> extensions = null;

            if (param instanceof AsyncApiExtensible extensible) {
                extensions = extensible.getExtensions();
            }

            // Extract schema fields (default, enum, examples)
            String defaultValue = null;
            List<String> enumValues = null;
            List<String> examples = null;

            AsyncApiSchema schema = getSchema(param);
            if (schema != null) {
                defaultValue = JsonNodeUtils.jsonNodeToString(schema.getDefault());
                enumValues = JsonNodeUtils.jsonNodeListToStringList(schema.getEnum());
                examples = JsonNodeUtils.jsonNodeListToStringList(schema.getExamples());
            }

            String location = null;
            if (param instanceof AsyncApiParameter asyncParam) {
                location = asyncParam.getLocation();
            }

            result.put(entry.getKey(),
                    new AsyncApiChannelParameter(
                            param.getDescription(),
                            defaultValue,
                            enumValues,
                            examples,
                            location,
                            extensions
                    ));
        }
        return result.isEmpty() ? null : result;
    }

    /**
     * Gets the schema from a parameter based on its version type.
     *
     * @param param the Apicurio parameter object
     * @return the schema object, or null if not available
     */
    private static AsyncApiSchema getSchema(Parameter param) {
        return switch (param) {
            case AsyncApi26Parameter typed -> typed.getSchema();
            case AsyncApi25Parameter typed -> typed.getSchema();
            case AsyncApi24Parameter typed -> typed.getSchema();
            case AsyncApi23Parameter typed -> typed.getSchema();
            case AsyncApi22Parameter typed -> typed.getSchema();
            case AsyncApi21Parameter typed -> typed.getSchema();
            case AsyncApi20Parameter typed -> typed.getSchema();
            default -> null;
        };
    }

    /**
     * Maps schemas from AsyncAPI components.
     *
     * @param components the Apicurio components object
     * @return the map of schema names to schema objects, or null if no schemas
     */
    private static Map<String, io.ballerina.asyncapi.core.model.component.AsyncApiSchema>
            mapSchemas(AsyncApiComponents components) {
        if (components == null) {
            return null;
        }

        Map<String, ? extends AsyncApiSchema> rawSchemas = switch (components) {
            case AsyncApi26Components typed -> typed.getSchemas();
            case AsyncApi25Components typed -> typed.getSchemas();
            case AsyncApi24Components typed -> typed.getSchemas();
            case AsyncApi23Components typed -> typed.getSchemas();
            case AsyncApi22Components typed -> typed.getSchemas();
            case AsyncApi21Components typed -> typed.getSchemas();
            case AsyncApi20Components typed -> typed.getSchemas();
            default -> null;
        };

        if (rawSchemas == null || rawSchemas.isEmpty()) {
            return null;
        }

        Map<String, io.ballerina.asyncapi.core.model.component.AsyncApiSchema> result =
                new LinkedHashMap<>();
        rawSchemas.forEach((key, schema) -> {
            io.ballerina.asyncapi.core.model.component.AsyncApiSchema mapped =
                    SchemaMapper.map(schema);
            if (mapped != null) {
                result.put(key, mapped);
            }
        });

        return result.isEmpty() ? null : result;
    }

    /**
     * Maps server variables from AsyncAPI components.
     *
     * @param components the Apicurio components object
     * @return the map of server variable names to server variable objects, or null if no server variables
     */
    private static Map<String, io.ballerina.asyncapi.core.model.server.AsyncApiServerVariable>
            mapComponentServerVariables(AsyncApiComponents components) {
        if (components == null) {
            return null;
        }

        Map<String, ? extends io.apicurio.datamodels.models.ServerVariable> rawVariables =
                switch (components) {
            case AsyncApi26Components typed -> typed.getServerVariables();
            case AsyncApi25Components typed -> typed.getServerVariables();
            case AsyncApi24Components typed -> typed.getServerVariables();
            case AsyncApi23Components typed -> null;  // Not available in v2.3
            case AsyncApi22Components typed -> null;  // Not available in v2.2
            case AsyncApi21Components typed -> null;  // Not available in v2.1
            case AsyncApi20Components typed -> null;  // Not available in v2.0
            default -> null;
        };

        // Delegate to existing ServerVariableMapperV2. Component server variables are
        // already resolved definitions, so no $ref resolution is needed (pass null).
        return io.ballerina.asyncapi.core.implementation.v2.server.ServerVariableMapperV2
                .mapVariables(rawVariables, null);
    }

    /**
     * Maps servers from AsyncAPI components.
     *
     * @param components the Apicurio components object
     * @return the map of server names to server objects, or null if no servers
     */
    private static Map<String, io.ballerina.asyncapi.core.model.server.AsyncApiServer>
            mapComponentServers(AsyncApiComponents components) {
        if (components == null) {
            return null;
        }

        Map<String, ? extends io.apicurio.datamodels.models.asyncapi.AsyncApiServer> rawServers =
                switch (components) {
            case AsyncApi26Components typed -> typed.getServers();
            case AsyncApi25Components typed -> typed.getServers();
            case AsyncApi24Components typed -> typed.getServers();
            case AsyncApi23Components typed -> typed.getServers();
            case AsyncApi22Components typed -> null;  // Not available in v2.2
            case AsyncApi21Components typed -> null;  // Not available in v2.1
            case AsyncApi20Components typed -> null;  // Not available in v2.0
            default -> null;
        };

        return ServerMapperV2.map(rawServers, components);
    }

    /**
     * Maps channels from AsyncAPI components.
     *
     * @param components the Apicurio components object
     * @return the map of channel names to channel objects, or null if no channels
     */
    private static Map<String, io.ballerina.asyncapi.core.model.channel.AsyncApiChannel>
            mapComponentChannels(AsyncApiComponents components) {
        if (components == null) {
            return null;
        }

        Map<String, ? extends io.apicurio.datamodels.models.asyncapi.AsyncApiChannelItem> rawChannels =
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

        Map<String, io.ballerina.asyncapi.core.model.channel.AsyncApiChannel> result =
                new LinkedHashMap<>();
        rawChannels.forEach((name, channelItem) -> {
            if (channelItem != null) {
                io.ballerina.asyncapi.core.model.channel.AsyncApiChannel mapped =
                        io.ballerina.asyncapi.core.implementation.v2.channel.ChannelMapperV2
                                .mapChannel(name, channelItem, components);
                if (mapped != null) {
                    result.put(name, mapped);
                }
            }
        });

        return result.isEmpty() ? null : result;
    }
}
