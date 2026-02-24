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
package io.ballerina.asyncapi.core.implementation.v3.channel;

import io.apicurio.datamodels.models.asyncapi.AsyncApiParameter;
import io.apicurio.datamodels.models.asyncapi.AsyncApiParameters;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Parameter;
import io.ballerina.asyncapi.core.model.channel.AsyncApiChannelParameter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps Apicurio Parameter models to {@link AsyncApiChannelParameter}
 * for AsyncAPI 3.0.
 */
public final class ChannelParameterMapperV3 {

    private ChannelParameterMapperV3() {

    }

    /**
     * Maps Apicurio {@link AsyncApiParameters} to a map of parameter names to
     * {@link AsyncApiChannelParameter}.
     *
     * @param parameters the Apicurio parameters object
     * @return the mapped parameters map, or null if parameters is null or empty
     */
    public static Map<String, AsyncApiChannelParameter> mapParameters(
            AsyncApiParameters parameters) {
        if (parameters == null) {
            return null;
        }
        List<String> names = parameters.getItemNames();
        if (names == null || names.isEmpty()) {
            return null;
        }
        Map<String, AsyncApiChannelParameter> result = new LinkedHashMap<>();
        for (String name : names) {
            AsyncApiParameter param = parameters.getItem(name);
            AsyncApiChannelParameter mapped = mapParameter(param);
            if (mapped != null) {
                result.put(name, mapped);
            }
        }
        return result.isEmpty() ? null : result;
    }

    /**
     * Maps a single Apicurio {@link AsyncApiParameter} to {@link AsyncApiChannelParameter}.
     *
     * @param param the Apicurio parameter object
     * @return the mapped AsyncApiChannelParameter
     */
    public static AsyncApiChannelParameter mapParameter(AsyncApiParameter param) {
        return switch (param) {
            case AsyncApi30Parameter typed -> new AsyncApiChannelParameter(
                    typed.getDescription(),
                    typed.getDefault(),
                    typed.getEnum(),
                    typed.getExamples(),
                    typed.getLocation(),
                    typed.getExtensions()
            );
            default -> null;
        };
    }
}
