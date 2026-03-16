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

import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30ServerVariable;
import io.ballerina.asyncapi.core.implementation.common.ServerVariableMapper;
import io.ballerina.asyncapi.core.model.server.AsyncApiServerVariable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps server variable definitions from AsyncAPI 3.0 components.
 */
final class ComponentServerVariableMapperV3 {

    private ComponentServerVariableMapperV3() {
    }

    /**
     * Maps component server variables.
     *
     * @param variables the Apicurio server variables map
     * @return the mapped variables map, or null if empty
     */
    static Map<String, AsyncApiServerVariable> map(Map<String, AsyncApi30ServerVariable> variables) {
        if (variables == null || variables.isEmpty()) {
            return null;
        }

        Map<String, AsyncApiServerVariable> result = new LinkedHashMap<>();
        for (Map.Entry<String, AsyncApi30ServerVariable> entry : variables.entrySet()) {
            result.put(entry.getKey(), ServerVariableMapper.mapVariable(entry.getValue()));
        }
        return result.isEmpty() ? null : result;
    }
}
