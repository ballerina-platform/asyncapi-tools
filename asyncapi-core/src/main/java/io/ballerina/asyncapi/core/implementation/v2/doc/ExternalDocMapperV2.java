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
package io.ballerina.asyncapi.core.implementation.v2.doc;

import com.fasterxml.jackson.databind.JsonNode;
import io.apicurio.datamodels.models.asyncapi.AsyncApiExtensible;
import io.apicurio.datamodels.models.asyncapi.AsyncApiExternalDocumentation;
import io.ballerina.asyncapi.core.implementation.utils.URIUtils;
import io.ballerina.asyncapi.core.model.doc.AsyncApiExternalDocs;

import java.util.Map;

/**
 * Maps Apicurio {@link AsyncApiExternalDocumentation} to {@link AsyncApiExternalDocs} for AsyncAPI 2.x.
 */
public final class ExternalDocMapperV2 {

    private ExternalDocMapperV2() {
    }

    /**
     * Maps an Apicurio {@link AsyncApiExternalDocumentation} to an {@link AsyncApiExternalDocs}.
     *
     * @param externalDocs the Apicurio external documentation object
     * @return the mapped AsyncApiExternalDocs, or null if externalDocs is null
     */
    public static AsyncApiExternalDocs map(AsyncApiExternalDocumentation externalDocs) {
        if (externalDocs == null) {
            return null;
        }

        Map<String, JsonNode> extensions = externalDocs instanceof AsyncApiExtensible extensible
                ? extensible.getExtensions()
                : null;

        return new AsyncApiExternalDocs(
                externalDocs.getDescription(),
                URIUtils.toUri(externalDocs.getUrl()),
                extensions
        );
    }

}
