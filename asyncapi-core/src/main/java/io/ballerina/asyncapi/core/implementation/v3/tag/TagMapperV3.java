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
package io.ballerina.asyncapi.core.implementation.v3.tag;

import com.fasterxml.jackson.databind.JsonNode;
import io.apicurio.datamodels.models.Tag;
import io.apicurio.datamodels.models.asyncapi.AsyncApiComponents;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Tag;
import io.ballerina.asyncapi.core.model.tag.AsyncApiTag;
import io.ballerina.asyncapi.core.implementation.v3.doc.ExternalDocMapperV3;

import java.util.Map;

/**
 * Maps Apicurio {@link Tag} to {@link AsyncApiTag} for AsyncAPI 3.0.
 */
public final class TagMapperV3 {

    private TagMapperV3() {

    }

    /**
     * Maps an Apicurio {@link Tag} to an {@link AsyncApiTag}.
     *
     * @param tag the Apicurio tag object
     * @return the mapped AsyncApiTag, or null if tag is null
     */
    public static AsyncApiTag map(Tag tag, AsyncApiComponents components) {
        if (tag == null) {
            return null;
        }
        Map<String, JsonNode> extensions = null;
        if (tag instanceof AsyncApi30Tag typedTag) {
            extensions = typedTag.getExtensions();
        }
        return new AsyncApiTag(
                tag.getName(),
                tag.getDescription(),
                ExternalDocMapperV3.map(tag.getExternalDocs(), components),
                extensions
        );
    }
}
