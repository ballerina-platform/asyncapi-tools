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
package io.ballerina.asyncapi.core.implementation.v2.operation;

import com.fasterxml.jackson.databind.JsonNode;
import io.apicurio.datamodels.models.asyncapi.AsyncApiExtensible;
import io.apicurio.datamodels.models.asyncapi.AsyncApiOperationTrait;
import io.ballerina.asyncapi.core.implementation.v2.doc.ExternalDocMapperV2;
import io.ballerina.asyncapi.core.implementation.v2.tag.TagMapperV2;
import io.ballerina.asyncapi.core.model.tag.AsyncApiTag;

import java.util.List;
import java.util.Map;

/**
 * Maps Apicurio {@link AsyncApiOperationTrait} to
 * {@link io.ballerina.asyncapi.core.model.operation.AsyncApiOperationTrait} for AsyncAPI 2.x.
 */
public final class OperationTraitMapperV2 {

    private OperationTraitMapperV2() {

    }

    /**
     * Maps a list of Apicurio operation traits to a list of model operation traits.
     *
     * @param traits the Apicurio operation traits list
     * @return the mapped traits list, or null if empty
     */
    static List<io.ballerina.asyncapi.core.model.operation.AsyncApiOperationTrait>
            mapTraits(List<? extends AsyncApiOperationTrait> traits) {
        if (traits == null || traits.isEmpty()) {
            return null;
        }
        return traits.stream()
                .map(OperationTraitMapperV2::map)
                .toList();
    }

    /**
     * Maps an Apicurio operation trait to a model operation trait.
     * In v2, traits have no title or security (those are v3-only).
     *
     * @param trait the Apicurio operation trait object
     * @return the mapped AsyncApiOperationTrait
     */
    public static io.ballerina.asyncapi.core.model.operation.AsyncApiOperationTrait
            map(AsyncApiOperationTrait trait) {
        Map<String, JsonNode> extensions = null;
        if (trait instanceof AsyncApiExtensible extensible) {
            extensions = extensible.getExtensions();
        }
        List<AsyncApiTag> tags = null;
        if (trait.getTags() != null) {
            tags = trait.getTags().stream()
                    .map(TagMapperV2::map)
                    .toList();
        }
        return new io.ballerina.asyncapi.core.model.operation.AsyncApiOperationTrait(
                null, // title (v2 has none)
                trait.getSummary(),
                trait.getDescription(),
                null, // security (v2 traits have none)
                tags,
                ExternalDocMapperV2.map(trait.getExternalDocs()),
                OperationBindingsMapperV2.map(trait.getBindings()),
                extensions
        );
    }
}
