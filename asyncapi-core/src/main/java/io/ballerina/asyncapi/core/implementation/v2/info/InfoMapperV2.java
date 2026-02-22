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
package io.ballerina.asyncapi.core.implementation.v2.info;

import com.fasterxml.jackson.databind.JsonNode;
import io.apicurio.datamodels.models.Info;
import io.apicurio.datamodels.models.Tag;
import io.apicurio.datamodels.models.asyncapi.AsyncApiDocument;
import io.apicurio.datamodels.models.asyncapi.AsyncApiExtensible;
import io.apicurio.datamodels.models.asyncapi.AsyncApiExternalDocumentation;
import io.apicurio.datamodels.models.asyncapi.v20.AsyncApi20Document;
import io.apicurio.datamodels.models.asyncapi.v21.AsyncApi21Document;
import io.apicurio.datamodels.models.asyncapi.v22.AsyncApi22Document;
import io.apicurio.datamodels.models.asyncapi.v23.AsyncApi23Document;
import io.apicurio.datamodels.models.asyncapi.v24.AsyncApi24Document;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25Document;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26Document;
import io.ballerina.asyncapi.core.model.info.AsyncApiInfo;
import io.ballerina.asyncapi.core.model.tag.AsyncApiTag;
import io.ballerina.asyncapi.core.implementation.common.ContactMapper;
import io.ballerina.asyncapi.core.implementation.common.LicenseMapper;
import io.ballerina.asyncapi.core.implementation.utils.URIUtils;
import io.ballerina.asyncapi.core.implementation.v2.doc.ExternalDocMapperV2;
import io.ballerina.asyncapi.core.implementation.v2.tag.TagMapperV2;

import java.util.List;
import java.util.Map;

/**
 * Maps Apicurio Info models {@link Info} to {@link AsyncApiInfo}.
 */
public final class InfoMapperV2 {

    private InfoMapperV2() {

    }

    /**
     * Maps an Apicurio {@link AsyncApiDocument} to an {@link AsyncApiInfo}.
     *
     * @param asyncApiDocument the Apicurio document
     * @return the mapped AsyncApiInfo.
     */
    public static AsyncApiInfo map(AsyncApiDocument asyncApiDocument) {
        Info info = asyncApiDocument.getInfo();
        AsyncApiExternalDocumentation documentExternalDocs = null;
        List<? extends Tag> documentTags = null;

        switch (asyncApiDocument) {
            case AsyncApi26Document doc -> {
                documentExternalDocs = doc.getExternalDocs();
                documentTags = doc.getTags();
            }
            case AsyncApi25Document doc -> {
                documentExternalDocs = doc.getExternalDocs();
                documentTags = doc.getTags();
            }
            case AsyncApi24Document doc -> {
                documentExternalDocs = doc.getExternalDocs();
                documentTags = doc.getTags();
            }
            case AsyncApi23Document doc -> {
                documentExternalDocs = doc.getExternalDocs();
                documentTags = doc.getTags();
            }
            case AsyncApi22Document doc -> {
                documentExternalDocs = doc.getExternalDocs();
                documentTags = doc.getTags();
            }
            case AsyncApi21Document doc -> {
                documentExternalDocs = doc.getExternalDocs();
                documentTags = doc.getTags();
            }
            case AsyncApi20Document doc -> {
                documentExternalDocs = doc.getExternalDocs();
                documentTags = doc.getTags();
            }
            default -> {
                return null;
            }
        }

        Map<String, JsonNode> extensions = null;
        if (info instanceof AsyncApiExtensible extensible) {
            extensions = extensible.getExtensions();
        }

        List<AsyncApiTag> tags = null;
        if (documentTags != null) {
            tags = documentTags.stream()
                    .map(TagMapperV2::map)
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
                ExternalDocMapperV2.map(documentExternalDocs),
                extensions
        );
    }

}
