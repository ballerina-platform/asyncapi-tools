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
package io.ballerina.asyncapi.core.implementation.v3.message;

import com.fasterxml.jackson.databind.JsonNode;
import io.apicurio.datamodels.models.Tag;
import io.apicurio.datamodels.models.asyncapi.AsyncApiMessageTrait;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30MessageTrait;
import io.ballerina.asyncapi.core.implementation.v3.doc.ExternalDocMapperV3;
import io.ballerina.asyncapi.core.implementation.v3.tag.TagMapperV3;
import io.ballerina.asyncapi.core.model.message.AsyncApiMessageExample;
import io.ballerina.asyncapi.core.model.tag.AsyncApiTag;

import java.util.List;
import java.util.Map;

/**
 * Maps Apicurio {@link AsyncApiMessageTrait} to model
 * {@link io.ballerina.asyncapi.core.model.message.AsyncApiMessageTrait} for AsyncAPI 3.0.
 */
public final class MessageTraitMapperV3 {

    private MessageTraitMapperV3() {
    }

    /**
     * Maps an Apicurio {@link AsyncApiMessageTrait} to a model message trait.
     *
     * @param trait the Apicurio message trait object
     * @return the mapped AsyncApiMessageTrait, or null if trait is null
     */
    public static io.ballerina.asyncapi.core.model.message.AsyncApiMessageTrait map(AsyncApiMessageTrait trait) {
        if (trait == null) {
            return null;
        }

        Object headers = null;
        List<AsyncApiMessageExample> examples = null;
        Map<String, JsonNode> extensions = null;

        if (trait instanceof AsyncApi30MessageTrait typedTrait) {
            headers = typedTrait.getHeaders();
            extensions = typedTrait.getExtensions();
            examples = MessageExampleMapperV3.map(typedTrait);
        }

        List<AsyncApiTag> tags = null;
        List<? extends Tag> apicurioTags = trait.getTags();
        if (apicurioTags != null) {
            tags = apicurioTags.stream()
                    .map(tag -> TagMapperV3.map(tag, null))
                    .toList();
        }

        return new io.ballerina.asyncapi.core.model.message.AsyncApiMessageTrait(
                trait.getName(),
                trait.getTitle(),
                trait.getSummary(),
                trait.getDescription(),
                trait.getContentType(),
                headers,
                CorrelationIdMapperV3.map(trait.getCorrelationId()),
                tags,
                ExternalDocMapperV3.map(trait.getExternalDocs(), null),
                MessageBindingsMapperV3.map(trait.getBindings()),
                examples,
                extensions
        );
    }
}
