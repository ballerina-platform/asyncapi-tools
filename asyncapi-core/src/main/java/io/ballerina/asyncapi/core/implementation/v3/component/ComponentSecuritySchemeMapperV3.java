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
package io.ballerina.asyncapi.core.implementation.v3.component;

import io.apicurio.datamodels.models.SecurityScheme;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30SecurityScheme;
import io.ballerina.asyncapi.core.implementation.v3.server.SecuritySchemeMapperV3;
import io.ballerina.asyncapi.core.model.security.AsyncApiSecurityScheme;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps security scheme definitions from AsyncAPI 3.0 components.
 * Filters entries to only those that are {@link AsyncApi30SecurityScheme} instances
 * before delegating to {@link SecuritySchemeMapperV3}.
 */
final class ComponentSecuritySchemeMapperV3 {

    private ComponentSecuritySchemeMapperV3() {

    }

    /**
     * Maps component security schemes.
     *
     * @param schemes the Apicurio security schemes map
     * @return the mapped security schemes map, or null if empty
     */
    static Map<String, AsyncApiSecurityScheme> map(
            Map<String, SecurityScheme> schemes) {
        if (schemes == null || schemes.isEmpty()) {
            return null;
        }

        Map<String, AsyncApiSecurityScheme> result = new LinkedHashMap<>();
        for (Map.Entry<String, SecurityScheme> entry : schemes.entrySet()) {
            if (entry.getValue() instanceof AsyncApi30SecurityScheme typed) {
                result.put(entry.getKey(), SecuritySchemeMapperV3.map(typed, null));
            }
        }
        return result.isEmpty() ? null : result;
    }
}
