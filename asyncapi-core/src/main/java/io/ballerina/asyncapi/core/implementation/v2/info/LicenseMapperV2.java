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
import io.apicurio.datamodels.models.License;
import io.apicurio.datamodels.models.asyncapi.AsyncApiExtensible;
import io.ballerina.asyncapi.core.model.info.AsyncApiLicense;
import io.ballerina.asyncapi.core.implementation.utils.URIUtils;

import java.util.Map;

/**
 * Maps Apicurio {@link License} to {@link AsyncApiLicense} for AsyncAPI 2.x.
 */
final class LicenseMapperV2 {

    private LicenseMapperV2() {

    }

    /**
     * Maps an Apicurio {@link License} to an {@link AsyncApiLicense}.
     *
     * @param license the Apicurio license object
     * @return the mapped AsyncApiLicense, or null if license is null
     */
    static AsyncApiLicense map(License license) {
        if (license == null) {
            return null;
        }
        Map<String, JsonNode> extensions = null;
        if (license instanceof AsyncApiExtensible extensible) {
            extensions = extensible.getExtensions();
        }

        return new AsyncApiLicense(
                license.getName(),
                URIUtils.toUri(license.getUrl()),
                extensions
        );
    }

}
