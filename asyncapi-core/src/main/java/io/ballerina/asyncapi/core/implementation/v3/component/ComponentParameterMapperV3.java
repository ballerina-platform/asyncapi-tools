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

import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Parameter;
import io.ballerina.asyncapi.core.implementation.v3.channel.ChannelParameterMapperV3;
import io.ballerina.asyncapi.core.model.channel.AsyncApiChannelParameter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps Apicurio component parameter models to {@link AsyncApiChannelParameter}
 * for AsyncAPI 3.0.
 */
public final class ComponentParameterMapperV3 {

    private ComponentParameterMapperV3() {
    }

    /**
     * Maps a map of Apicurio {@link io.apicurio.datamodels.models.Parameter} to a map of
     * {@link AsyncApiChannelParameter}.
     *
     * @param parametersMap the Apicurio parameters map
     * @return the mapped parameters map, or null if parametersMap is null or empty
     */
    public static Map<String, AsyncApiChannelParameter> mapParameters(
            Map<String, io.apicurio.datamodels.models.Parameter> parametersMap) {
        if (parametersMap == null || parametersMap.isEmpty()) {
            return null;
        }
        Map<String, AsyncApiChannelParameter> result = new LinkedHashMap<>();
        for (Map.Entry<String, io.apicurio.datamodels.models.Parameter> entry
                : parametersMap.entrySet()) {
            if (entry.getValue() instanceof AsyncApi30Parameter typedParam) {
                result.put(entry.getKey(), ChannelParameterMapperV3.mapParameter(typedParam));
            }
        }
        return result.isEmpty() ? null : result;
    }
}
