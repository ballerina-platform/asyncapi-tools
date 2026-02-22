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
import io.apicurio.datamodels.models.asyncapi.AsyncApiMessageTrait;
import io.apicurio.datamodels.models.asyncapi.v20.AsyncApi20MessageTrait;
import io.apicurio.datamodels.models.asyncapi.v21.AsyncApi21MessageTrait;
import io.apicurio.datamodels.models.asyncapi.v22.AsyncApi22MessageTrait;
import io.apicurio.datamodels.models.asyncapi.v23.AsyncApi23MessageTrait;
import io.apicurio.datamodels.models.asyncapi.v24.AsyncApi24MessageTrait;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25MessageTrait;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26MessageTrait;
import io.ballerina.asyncapi.core.implementation.v2.message.CorrelationIdMapperV2;
import io.ballerina.asyncapi.core.implementation.v2.doc.ExternalDocMapperV2;
import io.ballerina.asyncapi.core.implementation.v2.tag.TagMapperV2;
import io.ballerina.asyncapi.core.model.tag.AsyncApiTag;

import java.util.List;
import java.util.Map;

/**
 * Maps Apicurio {@link AsyncApiMessageTrait} to model
 * {@link io.ballerina.asyncapi.core.model.message.AsyncApiMessageTrait} for AsyncAPI 2.x.
 */
public final class MessageTraitMapperV2 {

    private MessageTraitMapperV2() {

    }

    /**
     * Maps an Apicurio {@link AsyncApiMessageTrait} to a model message trait.
     *
     * @param trait the Apicurio message trait object
     * @return the mapped AsyncApiMessageTrait, or null if null
     */
    public static io.ballerina.asyncapi.core.model.message.AsyncApiMessageTrait
            map(AsyncApiMessageTrait trait) {
        if (trait == null) {
            return null;
        }

        Object headers = null;
        Map<String, JsonNode> extensions = null;

        switch (trait) {
            case AsyncApi26MessageTrait typed -> {
                headers = typed.getHeaders();
                extensions = typed.getExtensions();
            }
            case AsyncApi25MessageTrait typed -> {
                headers = typed.getHeaders();
                extensions = typed.getExtensions();
            }
            case AsyncApi24MessageTrait typed -> {
                headers = typed.getHeaders();
                extensions = typed.getExtensions();
            }
            case AsyncApi23MessageTrait typed -> {
                headers = typed.getHeaders();
                extensions = typed.getExtensions();
            }
            case AsyncApi22MessageTrait typed -> {
                headers = typed.getHeaders();
                extensions = typed.getExtensions();
            }
            case AsyncApi21MessageTrait typed -> {
                headers = typed.getHeaders();
                extensions = typed.getExtensions();
            }
            case AsyncApi20MessageTrait typed -> {
                headers = typed.getHeaders();
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
                MessageMapperV2.mapBindings(trait.getBindings()),
                MessageMapperV2.mapMessageTraitExample(trait),
                extensions
        );
    }
}
