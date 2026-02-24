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
package io.ballerina.asyncapi.core.implementation.v2.component;

import io.apicurio.datamodels.models.SecurityScheme;
import io.ballerina.asyncapi.core.implementation.v2.server.SecuritySchemeMapperV2;
import io.ballerina.asyncapi.core.model.security.AsyncApiSecurityScheme;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps security scheme definitions from AsyncAPI 2.x components.
 */
final class ComponentSecuritySchemeMapperV2 {

    private ComponentSecuritySchemeMapperV2() {

    }

    /**
     * Maps component security schemes.
     * Filters entries to only those that are AsyncAPI-typed security schemes
     * before delegating to {@link SecuritySchemeMapperV2}.
     *
     * @param schemes the Apicurio security schemes map
     * @return the mapped security schemes map, or null if empty
     */
    static Map<String, AsyncApiSecurityScheme> map(
            Map<String, ? extends SecurityScheme> schemes) {
        if (schemes == null || schemes.isEmpty()) {
            return null;
        }

        Map<String, AsyncApiSecurityScheme> result = new LinkedHashMap<>();
        for (Map.Entry<String, ? extends SecurityScheme> entry : schemes.entrySet()) {
            if (entry.getValue() instanceof
                    io.apicurio.datamodels.models.asyncapi.AsyncApiSecurityScheme typed) {
                result.put(entry.getKey(), SecuritySchemeMapperV2.map(typed));
            }
        }
        return result.isEmpty() ? null : result;
    }
}
