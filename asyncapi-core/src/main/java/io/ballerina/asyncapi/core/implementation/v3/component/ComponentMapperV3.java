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
import io.apicurio.datamodels.models.asyncapi.AsyncApiComponents;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Components;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Message;
import io.ballerina.asyncapi.core.implementation.common.ServerBindingsMapper;
import io.ballerina.asyncapi.core.implementation.v3.channel.ChannelBindingsMapperV3;
import io.ballerina.asyncapi.core.implementation.v3.doc.ExternalDocMapperV3;
import io.ballerina.asyncapi.core.implementation.v3.message.CorrelationIdMapperV3;
import io.ballerina.asyncapi.core.implementation.v3.message.MessageBindingsMapperV3;
import io.ballerina.asyncapi.core.implementation.v3.message.MessageMapperV3;
import io.ballerina.asyncapi.core.implementation.v3.message.MessageTraitMapperV3;
import io.ballerina.asyncapi.core.implementation.v3.operation.OperationBindingsMapperV3;
import io.ballerina.asyncapi.core.implementation.v3.operation.OperationMapperV3;
import io.ballerina.asyncapi.core.implementation.v3.operation.OperationReplyMapperV3;
import io.ballerina.asyncapi.core.implementation.v3.operation.OperationTraitMapperV3;
import io.ballerina.asyncapi.core.implementation.v3.tag.TagMapperV3;
import io.ballerina.asyncapi.core.model.component.AsyncApiComponent;

import java.util.HashMap;
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
                    ComponentSchemaMapperV3.map(typedComponents),
                    ComponentServerMapperV3.map(typedComponents),
                    ComponentServerMapperV3.mapComponentServerVariables(typedComponents),
                    ComponentChannelMapperV3.map(typedComponents),
                    mapValues(typedComponents.getOperations(), operation ->
                            OperationMapperV3.mapOperationItem(operation, null, null, typedComponents)),
                    mapValues(typedComponents.getMessages(),
                            msg -> MessageMapperV3.mapMessageItem((AsyncApi30Message) msg, typedComponents)),
                    ComponentSecuritySchemeMapperV3.map(typedComponents.getSecuritySchemes()),
                    ComponentParameterMapperV3.mapParameters(typedComponents.getParameters()),
                    mapValues(typedComponents.getCorrelationIds(), CorrelationIdMapperV3::map),
                    mapValues(typedComponents.getOperationTraits(), trait ->
                            OperationTraitMapperV3.map(trait, typedComponents)),
                    mapValues(typedComponents.getMessageTraits(), MessageTraitMapperV3::map),
                    mapValues(typedComponents.getReplies(), reply ->
                            OperationReplyMapperV3.mapReply(reply, null, null, typedComponents)),
                    mapValues(typedComponents.getReplyAddresses(), OperationReplyMapperV3::mapReplyAddress),
                    mapValues(typedComponents.getExternalDocs(), doc ->
                            ExternalDocMapperV3.map(doc, typedComponents)),
                    mapValues(typedComponents.getTags(), tag -> TagMapperV3.map(tag, typedComponents)),
                    mapValues(typedComponents.getServerBindings(), binding ->
                            ServerBindingsMapper.mapBindings(binding, typedComponents)),
                    mapValues(typedComponents.getChannelBindings(), binding ->
                            ChannelBindingsMapperV3.map(binding, typedComponents)),
                    mapValues(typedComponents.getOperationBindings(), OperationBindingsMapperV3::map),
                    mapValues(typedComponents.getMessageBindings(), MessageBindingsMapperV3::map),
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
    private static <T, R> Map<String, R> mapValues(Map<String, T> source, Function<? super T, R> mapper) {
        if (source == null || source.isEmpty()) {
            return null;
        }
        Map<String, R> result = new HashMap<>();
        for (Map.Entry<String, T> entry : source.entrySet()) {
            R mapped = mapper.apply(entry.getValue());
            if (mapped != null) {
                result.put(entry.getKey(), mapped);
            }
        }
        return result.isEmpty() ? null : result;
    }

}
