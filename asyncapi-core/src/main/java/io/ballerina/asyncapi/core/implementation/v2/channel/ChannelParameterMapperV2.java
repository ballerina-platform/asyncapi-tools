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
package io.ballerina.asyncapi.core.implementation.v2.channel;

import com.fasterxml.jackson.databind.JsonNode;
import io.apicurio.datamodels.models.asyncapi.AsyncApiExtensible;
import io.apicurio.datamodels.models.asyncapi.AsyncApiParameter;
import io.apicurio.datamodels.models.asyncapi.AsyncApiParameters;
import io.apicurio.datamodels.models.asyncapi.AsyncApiSchema;
import io.apicurio.datamodels.models.asyncapi.v20.AsyncApi20Parameter;
import io.apicurio.datamodels.models.asyncapi.v21.AsyncApi21Parameter;
import io.apicurio.datamodels.models.asyncapi.v22.AsyncApi22Parameter;
import io.apicurio.datamodels.models.asyncapi.v23.AsyncApi23Parameter;
import io.apicurio.datamodels.models.asyncapi.v24.AsyncApi24Parameter;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25Parameter;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26Parameter;
import io.ballerina.asyncapi.core.implementation.utils.JsonNodeUtils;
import io.ballerina.asyncapi.core.model.channel.AsyncApiChannelParameter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps Apicurio channel parameters to {@link AsyncApiChannelParameter} for AsyncAPI 2.x.
 */
final class ChannelParameterMapperV2 {

    private ChannelParameterMapperV2() {

    }

    /**
     * Maps Apicurio {@link AsyncApiParameters} to a map of parameter names to {@link AsyncApiChannelParameter}.
     *
     * @param parameters the Apicurio parameters object
     * @return the mapped parameters map, or null if parameters is null or empty
     */
    static Map<String, AsyncApiChannelParameter> mapParameters(
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

            Map<String, JsonNode> extensions = null;
            if (param instanceof AsyncApiExtensible extensible) {
                extensions = extensible.getExtensions();
            }

            String defaultValue = null;
            List<String> enumValues = null;
            List<String> examples = null;

            AsyncApiSchema schema = getSchema(param);
            if (schema != null) {
                defaultValue = JsonNodeUtils.jsonNodeToString(schema.getDefault());
                enumValues = JsonNodeUtils.jsonNodeListToStringList(schema.getEnum());
                examples = JsonNodeUtils.jsonNodeListToStringList(schema.getExamples());
            }

            result.put(name, new AsyncApiChannelParameter(
                    param.getDescription(),
                    defaultValue,
                    enumValues,
                    examples,
                    extensions
            ));
        }
        return result.isEmpty() ? null : result;
    }

    /**
     * Gets the schema from a parameter based on its version type.
     *
     * @param param the Apicurio parameter object
     * @return the schema object, or null if not available
     */
    private static AsyncApiSchema getSchema(AsyncApiParameter param) {
        return switch (param) {
            case AsyncApi26Parameter typed -> typed.getSchema();
            case AsyncApi25Parameter typed -> typed.getSchema();
            case AsyncApi24Parameter typed -> typed.getSchema();
            case AsyncApi23Parameter typed -> typed.getSchema();
            case AsyncApi22Parameter typed -> typed.getSchema();
            case AsyncApi21Parameter typed -> typed.getSchema();
            case AsyncApi20Parameter typed -> typed.getSchema();
            default -> null;
        };
    }
}
