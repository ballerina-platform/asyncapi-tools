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
package io.ballerina.asyncapi.core.implementation.v3.info;

import com.fasterxml.jackson.databind.JsonNode;
import io.apicurio.datamodels.models.Info;
import io.apicurio.datamodels.models.Tag;
import io.apicurio.datamodels.models.asyncapi.AsyncApiComponents;
import io.apicurio.datamodels.models.asyncapi.AsyncApiDocument;
import io.apicurio.datamodels.models.asyncapi.AsyncApiExtensible;
import io.apicurio.datamodels.models.asyncapi.AsyncApiExternalDocumentation;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Info;
import io.ballerina.asyncapi.core.model.info.AsyncApiInfo;
import io.ballerina.asyncapi.core.model.tag.AsyncApiTag;
import io.ballerina.asyncapi.core.implementation.common.ContactMapper;
import io.ballerina.asyncapi.core.implementation.common.LicenseMapper;
import io.ballerina.asyncapi.core.implementation.utils.URIUtils;
import io.ballerina.asyncapi.core.implementation.v3.tag.TagMapperV3;
import io.ballerina.asyncapi.core.implementation.v3.doc.ExternalDocMapperV3;

import java.util.List;
import java.util.Map;

/**
 * Maps Apicurio Info models to {@link AsyncApiInfo} for AsyncAPI 3.0.
 * Delegates sub-object mapping to {@link ContactMapper}, {@link LicenseMapper},
 * {@link TagMapperV3}, and {@link ExternalDocMapperV3}.
 */
public final class InfoMapperV3 {

    private InfoMapperV3() {

    }

    /**
     * Maps an Apicurio {@link AsyncApiDocument} to an {@link AsyncApiInfo}.
     *
     * @param info the Apicurio Info object
     * @param components the Apicurio components object
     * @return the mapped AsyncApiInfo
     */
    public static AsyncApiInfo map(Info info, AsyncApiComponents components) {
        AsyncApiExternalDocumentation documentExternalDocs = null;
        List<? extends Tag> documentTags = null;
        Map<String, JsonNode> extensions = null;

        if (info instanceof AsyncApi30Info typedInfo) {
            documentExternalDocs = typedInfo.getExternalDocs();
            documentTags = typedInfo.getTags();
        }

        if (info instanceof AsyncApiExtensible extensible) {
            extensions = extensible.getExtensions();
        }

        List<AsyncApiTag> tags = null;
        if (documentTags != null) {
            tags = documentTags.stream()
                    .map(tag -> TagMapperV3.map(tag, components))
                    .toList();
        }

        return new AsyncApiInfo(
                info.getTitle(),
                info.getVersion(),
                info.getDescription(),
                URIUtils.toUri(info.getTermsOfService()),
                ContactMapper.map(info.getContact()),
                LicenseMapper.map(info.getLicense()),
                tags,
                ExternalDocMapperV3.map(documentExternalDocs, components),
                extensions
        );
    }

}
