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
package io.ballerina.asyncapi.core.implementation.v3.operation;

import com.fasterxml.jackson.databind.JsonNode;
import io.apicurio.datamodels.models.asyncapi.AsyncApiOperationTrait;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30OperationTrait;
import io.ballerina.asyncapi.core.implementation.v3.doc.ExternalDocMapperV3;
import io.ballerina.asyncapi.core.implementation.v3.server.ServerMapperV3;
import io.ballerina.asyncapi.core.implementation.v3.tag.TagMapperV3;
import io.ballerina.asyncapi.core.model.security.AsyncApiSecurityScheme;
import io.ballerina.asyncapi.core.model.tag.AsyncApiTag;

import java.util.List;
import java.util.Map;

/**
 * Maps Apicurio OperationTrait models to
 * {@link io.ballerina.asyncapi.core.model.operation.AsyncApiOperationTrait}
 * for AsyncAPI 3.0.
 */
final class OperationTraitMapperV3 {

    private OperationTraitMapperV3() {

    }

    /**
     * Maps a list of Apicurio operation traits to a list of
     * {@link io.ballerina.asyncapi.core.model.operation.AsyncApiOperationTrait}.
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
                .map(OperationTraitMapperV3::map)
                .toList();
    }

    /**
     * Maps an Apicurio operation trait to an
     * {@link io.ballerina.asyncapi.core.model.operation.AsyncApiOperationTrait}.
     *
     * @param trait the Apicurio operation trait object
     * @return the mapped AsyncApiOperationTrait
     */
    static io.ballerina.asyncapi.core.model.operation.AsyncApiOperationTrait
            map(AsyncApiOperationTrait trait) {
        String title = null;
        List<AsyncApiSecurityScheme> security = null;
        Map<String, JsonNode> extensions = null;
        if (trait instanceof AsyncApi30OperationTrait typedTrait) {
            title = typedTrait.getTitle();
            security = ServerMapperV3.mapSecurityList(typedTrait.getSecurity());
            extensions = typedTrait.getExtensions();
        }
        List<AsyncApiTag> tags = null;
        if (trait.getTags() != null) {
            tags = trait.getTags().stream()
                    .map(tag -> TagMapperV3.map(tag, null))
                    .toList();
        }
        return new io.ballerina.asyncapi.core.model.operation.AsyncApiOperationTrait(
                title,
                trait.getSummary(),
                trait.getDescription(),
                security,
                tags,
                ExternalDocMapperV3.map(trait.getExternalDocs(), null),
                OperationMapperV3.mapBindings(trait.getBindings()),
                extensions
        );
    }
}
