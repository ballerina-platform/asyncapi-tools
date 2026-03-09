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
import io.apicurio.datamodels.models.asyncapi.AsyncApiComponents;
import io.apicurio.datamodels.models.asyncapi.AsyncApiExtensible;
import io.ballerina.asyncapi.core.implementation.common.ServerBindingsMapper;
import io.ballerina.asyncapi.core.implementation.v2.channel.ChannelBindingsMapperV2;
import io.ballerina.asyncapi.core.implementation.v2.message.CorrelationIdMapperV2;
import io.ballerina.asyncapi.core.implementation.v2.message.MessageBindingsMapperV2;
import io.ballerina.asyncapi.core.implementation.v2.message.MessageMapperV2;
import io.ballerina.asyncapi.core.implementation.v2.message.MessageTraitMapperV2;
import io.ballerina.asyncapi.core.implementation.v2.operation.OperationBindingsMapperV2;
import io.ballerina.asyncapi.core.implementation.v2.operation.OperationTraitMapperV2;
import io.ballerina.asyncapi.core.model.component.AsyncApiComponent;

import java.util.LinkedHashMap;
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

        Map<String, JsonNode> extensions = null;

        if (components instanceof AsyncApiExtensible extensible) {
            extensions = extensible.getExtensions();
        }

        return new AsyncApiComponent(
                ComponentSchemaMapperV2.map(components),
                ComponentServerMapperV2.map(components),
                ComponentServerMapperV2.mapComponentServerVariables(components),
                ComponentChannelMapperV2.map(components),
                null, // operations - not in AsyncAPI 2.x components
                mapValues(components.getMessages(), msg -> MessageMapperV2.mapMessageItem(msg, components)),
                ComponentSecuritySchemeMapperV2.map(components.getSecuritySchemes()),
                ComponentParameterMapperV2.map(components.getParameters()),
                mapValues(components.getCorrelationIds(), CorrelationIdMapperV2::map),
                mapValues(components.getOperationTraits(), OperationTraitMapperV2::map),
                mapValues(components.getMessageTraits(), trait -> MessageTraitMapperV2.map(trait, components)),
                null, // replies - not in AsyncAPI 2.x
                null, // replyAddresses - not in AsyncAPI 2.x
                null, // externalDocs - not in AsyncAPI 2.x components
                null, // tags - not in AsyncAPI 2.x components
                mapValues(components.getServerBindings(), binding ->
                        ServerBindingsMapper.mapBindings(binding, null)),
                mapValues(components.getChannelBindings(), binding ->
                        ChannelBindingsMapperV2.map(binding, components)),
                mapValues(components.getOperationBindings(), OperationBindingsMapperV2::map),
                mapValues(components.getMessageBindings(), MessageBindingsMapperV2::map),
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
    private static <T, R> Map<String, R> mapValues(Map<String, ? extends T> source, Function<? super T, R> mapper) {
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

}
