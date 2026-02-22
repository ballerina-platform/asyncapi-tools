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

import com.fasterxml.jackson.databind.JsonNode;
import io.apicurio.datamodels.models.Extensible;
import io.apicurio.datamodels.models.SecurityScheme;
import io.apicurio.datamodels.models.asyncapi.AsyncApiComponents;
import io.apicurio.datamodels.models.asyncapi.AsyncApiSchema;
import io.apicurio.datamodels.models.asyncapi.v30.*;
import io.apicurio.datamodels.models.union.MultiFormatSchemaSchemaUnion;
import io.ballerina.asyncapi.core.implementation.common.SchemaMapper;
import io.ballerina.asyncapi.core.implementation.v3.channel.ChannelBindingsMapperV3;
import io.ballerina.asyncapi.core.implementation.v3.channel.ChannelMapperV3;
import io.ballerina.asyncapi.core.implementation.v3.channel.ChannelParameterMapperV3;
import io.ballerina.asyncapi.core.implementation.v3.doc.ExternalDocMapperV3;
import io.ballerina.asyncapi.core.implementation.v3.operation.OperationMapperV3;
import io.ballerina.asyncapi.core.implementation.v3.server.SecuritySchemeMapperV3;
import io.ballerina.asyncapi.core.implementation.common.ServerBindingsMapper;
import io.ballerina.asyncapi.core.implementation.v3.server.ServerMapperV3;
import io.ballerina.asyncapi.core.implementation.v3.tag.TagMapperV3;
import io.ballerina.asyncapi.core.model.component.AsyncApiComponent;
import io.ballerina.asyncapi.core.model.security.AsyncApiSecurityScheme;
import io.ballerina.asyncapi.core.model.server.AsyncApiServerVariable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Maps Apicurio Components to {@link AsyncApiComponent} for AsyncAPI 3.0.
 */
public final class ComponentMapperV3 {

    private ComponentMapperV3() {

    }

    /**
     * Maps Apicurio {@link AsyncApi30Components} to {@link AsyncApiComponent}.
     *
     * @param components the Apicurio components object
     * @return the mapped AsyncApiComponent, or null if components is null
     */
    public static AsyncApiComponent map(AsyncApiComponents components) {
        if (components == null) {
            return null;
        }

        // Add additional version checks here as new AsyncAPI 3.x versions are supported.
        if (components instanceof AsyncApi30Components typedComponents) {
            Map<String, JsonNode> extensions = null;
            if (typedComponents instanceof Extensible extensible) {
                extensions = extensible.getExtensions();
            }

            return new AsyncApiComponent(
                    mapSchemas(typedComponents),
                    mapValues(typedComponents.getServers(),
                            server -> ServerMapperV3.buildServer(server, typedComponents)),
                    mapServerVariables(typedComponents.getServerVariables()),
                    mapValues(typedComponents.getChannels(),
                            channel -> ChannelMapperV3.buildChannel(channel, typedComponents, null)),
                    mapValues(typedComponents.getOperations(),
                            operation -> OperationMapperV3.buildOperation(operation, null, null, typedComponents)),
                    mapValues(typedComponents.getMessages(),
                            MessageMapperV3::map),
                    mapSecuritySchemes(typedComponents.getSecuritySchemes()),
                    ChannelParameterMapperV3.mapParameters(typedComponents.getParameters()),
                    mapValues(typedComponents.getCorrelationIds(),
                            MessageMapperV3::mapCorrelationId),
                    mapValues(typedComponents.getOperationTraits(),
                            trait -> OperationMapperV3.mapTrait(trait, typedComponents)),
                    mapValues(typedComponents.getMessageTraits(),
                            MessageMapperV3::mapTrait),
                    mapValues(typedComponents.getReplies(),
                            reply -> OperationMapperV3.mapReply(reply, null, null, typedComponents)),
                    mapValues(typedComponents.getReplyAddresses(),
                            OperationMapperV3::mapReplyAddress),
                    mapValues(typedComponents.getExternalDocs(),
                            doc -> ExternalDocMapperV3.map(doc, typedComponents)),
                    mapValues(typedComponents.getTags(),
                            tag -> TagMapperV3.map(tag, typedComponents)),
                    mapValues(typedComponents.getServerBindings(),
                            binding -> ServerBindingsMapper.mapBindings(binding, null)),
                    mapValues(typedComponents.getChannelBindings(),
                            ChannelBindingsMapperV3::map),
                    mapValues(typedComponents.getOperationBindings(),
                            OperationMapperV3::mapBindings),
                    mapValues(typedComponents.getMessageBindings(),
                            MessageMapperV3::mapBindings),
                    extensions
            );
        }
        return null;
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
            Map<String, T> source, Function<? super T, R> mapper) {
        if (source == null || source.isEmpty()) {
            return null;
        }
        Map<String, R> result = new LinkedHashMap<>();
        for (Map.Entry<String, T> entry : source.entrySet()) {
            R mapped = mapper.apply(entry.getValue());
            if (mapped != null) {
                result.put(entry.getKey(), mapped);
            }
        }
        return result.isEmpty() ? null : result;
    }

    // ---- special-case map methods (need key or cast) ----

    /**
     * Maps component server variables.
     *
     * @param variables the Apicurio server variables map
     * @return the mapped variables map, or null if empty
     */
    private static Map<String, AsyncApiServerVariable>
    mapServerVariables(
            Map<String, AsyncApi30ServerVariable> variables) {
        if (variables == null || variables.isEmpty()) {
            return null;
        }
        Map<String, AsyncApiServerVariable> result =
                new LinkedHashMap<>();
        for (Map.Entry<String, AsyncApi30ServerVariable> entry
                : variables.entrySet()) {
            result.put(entry.getKey(),
                    ServerMapperV3.mapVariable(entry.getValue()));
        }
        return result.isEmpty() ? null : result;
    }

    /**
     * Maps component security schemes. Requires instanceof cast to
     * {@link AsyncApi30SecurityScheme}.
     *
     * @param schemes the Apicurio security schemes map
     * @return the mapped security schemes map, or null if empty
     */
    private static Map<String, AsyncApiSecurityScheme>
    mapSecuritySchemes(
            Map<String, SecurityScheme> schemes) {
        if (schemes == null || schemes.isEmpty()) {
            return null;
        }
        Map<String, AsyncApiSecurityScheme> result =
                new LinkedHashMap<>();
        for (Map.Entry<String, SecurityScheme> entry
                : schemes.entrySet()) {
            if (entry.getValue()
                    instanceof AsyncApi30SecurityScheme typed) {
                result.put(entry.getKey(),
                        SecuritySchemeMapperV3.map(typed, null));
            }
        }
        return result.isEmpty() ? null : result;
    }

    /**
     * Maps component schemas from AsyncAPI 3.0 components.
     * Schemas in V3 can be either MultiFormatSchema (for Avro, Protobuf, etc.)
     * or plain JSON Schema objects. Only JSON Schema entries (those where the union
     * holds an {@link AsyncApiSchema}) are mapped; non-JSON schema formats are skipped.
     *
     * @param components the AsyncAPI 3.0 components object
     * @return a map of schema names to mapped schema objects, or null if empty
     */
    private static Map<String, io.ballerina.asyncapi.core.model.component.AsyncApiSchema>
            mapSchemas(AsyncApi30Components components) {
        if (components == null) {
            return null;
        }

        Map<String, MultiFormatSchemaSchemaUnion> rawSchemas = components.getSchemas();
        if (rawSchemas == null || rawSchemas.isEmpty()) {
            return null;
        }

        Map<String, io.ballerina.asyncapi.core.model.component.AsyncApiSchema> result =
                new LinkedHashMap<>();
        rawSchemas.forEach((key, schemaUnion) -> {
            if (schemaUnion != null && schemaUnion.isSchema()
                    && schemaUnion.asSchema() instanceof AsyncApiSchema typedSchema) {
                io.ballerina.asyncapi.core.model.component.AsyncApiSchema mapped =
                        SchemaMapper.map(typedSchema);
                if (mapped != null) {
                    result.put(key, mapped);
                }
            }
        });

        return result.isEmpty() ? null : result;
    }
}
