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
package io.ballerina.asyncapi.core.implementation.v2.message;

import com.fasterxml.jackson.databind.JsonNode;
import io.apicurio.datamodels.models.Tag;
import io.apicurio.datamodels.models.asyncapi.AsyncApiComponents;
import io.apicurio.datamodels.models.asyncapi.AsyncApiMessageTrait;
import io.apicurio.datamodels.models.asyncapi.AsyncApiReferenceable;
import io.apicurio.datamodels.models.asyncapi.AsyncApiSchema;
import io.apicurio.datamodels.models.asyncapi.v20.AsyncApi20MessageTrait;
import io.apicurio.datamodels.models.asyncapi.v21.AsyncApi21MessageTrait;
import io.apicurio.datamodels.models.asyncapi.v22.AsyncApi22MessageTrait;
import io.apicurio.datamodels.models.asyncapi.v23.AsyncApi23MessageTrait;
import io.apicurio.datamodels.models.asyncapi.v24.AsyncApi24MessageTrait;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25MessageTrait;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26MessageTrait;
import io.ballerina.asyncapi.core.Constants;
import io.ballerina.asyncapi.core.implementation.common.SchemaMapper;
import io.ballerina.asyncapi.core.implementation.v2.doc.ExternalDocMapperV2;
import io.ballerina.asyncapi.core.implementation.v2.tag.TagMapperV2;
import io.ballerina.asyncapi.core.model.tag.AsyncApiTag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Maps Apicurio {@link AsyncApiMessageTrait} to model
 * {@link io.ballerina.asyncapi.core.model.message.AsyncApiMessageTrait} for AsyncAPI 2.x.
 */
public final class MessageTraitMapperV2 {

    private static final Logger LOG = LogManager.getLogger(MessageTraitMapperV2.class);

    private MessageTraitMapperV2() {
    }

    /**
     * Maps an Apicurio {@link AsyncApiMessageTrait} to a model message trait,
     * resolving any {@code $ref} references before building the model.
     *
     * @param trait      the Apicurio message trait object (may be a reference)
     * @param components the AsyncAPI components (for $ref resolution)
     * @return the mapped AsyncApiMessageTrait, or null if null
     */
    public static io.ballerina.asyncapi.core.model.message.AsyncApiMessageTrait map(
            AsyncApiMessageTrait trait, AsyncApiComponents components) {
        if (trait == null) {
            return null;
        }

        String ref = trait instanceof AsyncApiReferenceable referenceable ? referenceable.get$ref() : null;
        if (ref != null) {
            AsyncApiMessageTrait resolved = resolveRef(ref, components);
            if (resolved == null) {
                LOG.warn("Could not resolve $ref: {}. Skipping message trait.", ref);
                return null;
            }
            return map(resolved, components);
        }

        Object headers = null;
        Map<String, JsonNode> extensions = null;

        switch (trait) {
            case AsyncApi26MessageTrait typed -> {
                if (typed.getHeaders() instanceof AsyncApiSchema typedSchema) {
                    headers = SchemaMapper.map(typedSchema);
                }
                extensions = typed.getExtensions();
            }
            case AsyncApi25MessageTrait typed -> {
                if (typed.getHeaders() instanceof AsyncApiSchema typedSchema) {
                    headers = SchemaMapper.map(typedSchema);
                }
                extensions = typed.getExtensions();
            }
            case AsyncApi24MessageTrait typed -> {
                if (typed.getHeaders() instanceof AsyncApiSchema typedSchema) {
                    headers = SchemaMapper.map(typedSchema);
                }
                extensions = typed.getExtensions();
            }
            case AsyncApi23MessageTrait typed -> {
                if (typed.getHeaders() instanceof AsyncApiSchema typedSchema) {
                    headers = SchemaMapper.map(typedSchema);
                }
                extensions = typed.getExtensions();
            }
            case AsyncApi22MessageTrait typed -> {
                if (typed.getHeaders() instanceof AsyncApiSchema typedSchema) {
                    headers = SchemaMapper.map(typedSchema);
                }
                extensions = typed.getExtensions();
            }
            case AsyncApi21MessageTrait typed -> {
                if (typed.getHeaders() instanceof AsyncApiSchema typedSchema) {
                    headers = SchemaMapper.map(typedSchema);
                }
                extensions = typed.getExtensions();
            }
            case AsyncApi20MessageTrait typed -> {
                if (typed.getHeaders() instanceof AsyncApiSchema typedSchema) {
                    headers = SchemaMapper.map(typedSchema);
                }
                extensions = typed.getExtensions();
            }
            default -> { }
        }

        List<AsyncApiTag> tags = null;
        List<? extends Tag> apicurioTags = trait.getTags();
        if (apicurioTags != null) {
            tags = apicurioTags.stream()
                    .map(TagMapperV2::map)
                    .toList();
        }

        return new io.ballerina.asyncapi.core.model.message.AsyncApiMessageTrait(
                trait.getName(),
                trait.getTitle(),
                trait.getSummary(),
                trait.getDescription(),
                trait.getContentType(),
                headers,
                CorrelationIdMapperV2.map(trait.getCorrelationId()),
                tags,
                ExternalDocMapperV2.map(trait.getExternalDocs()),
                MessageBindingsMapperV2.map(trait.getBindings()),
                MessageExampleMapperV2.map(trait),
                extensions
        );
    }

    /**
     * Resolves a {@code $ref} to a component message trait by extracting the name from the reference
     * string and looking it up in the components message traits map.
     *
     * @param ref        the reference string (e.g. {@code #/components/messageTraits/myTrait})
     * @param components the Apicurio components object
     * @return the resolved message trait, or null if not found or invalid
     */
    private static AsyncApiMessageTrait resolveRef(String ref, AsyncApiComponents components) {
        if (components == null) {
            LOG.warn("Cannot resolve $ref: {}. Components is null.", ref);
            return null;
        }
        Set<String> visited = new HashSet<>();
        String current = ref;
        while (current != null) {
            if (!current.startsWith(Constants.MESSAGE_TRAITS_REF_PREFIX)) {
                LOG.warn("Unsupported $ref format: {}. Skipping message trait.", current);
                return null;
            }
            if (!visited.add(current)) {
                LOG.warn("Cyclic $ref detected: {}. Skipping message trait.", current);
                return null;
            }
            String name = current.substring(Constants.MESSAGE_TRAITS_REF_PREFIX.length());
            Map<String, ?> traitsMap = components.getMessageTraits();
            Object entry = traitsMap != null ? traitsMap.get(name) : null;
            AsyncApiMessageTrait resolved = entry instanceof AsyncApiMessageTrait t ? t : null;
            if (resolved == null) {
                LOG.warn("Could not resolve $ref: '{}'. No matching message trait found.", current);
                return null;
            }
            if (resolved instanceof AsyncApiReferenceable r && r.get$ref() != null) {
                current = r.get$ref();
            } else {
                return resolved;
            }
        }
        return null;
    }
}
