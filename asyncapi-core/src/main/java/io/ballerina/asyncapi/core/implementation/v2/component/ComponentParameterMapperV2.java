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

import com.fasterxml.jackson.databind.JsonNode;
import io.apicurio.datamodels.models.Parameter;
import io.apicurio.datamodels.models.asyncapi.AsyncApiExtensible;
import io.apicurio.datamodels.models.asyncapi.AsyncApiParameter;
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
 * Maps parameter definitions from AsyncAPI 2.x components.
 */
final class ComponentParameterMapperV2 {

    private ComponentParameterMapperV2() {

    }

    /**
     * Maps component parameters.
     *
     * @param parameters the Apicurio parameters map
     * @return the mapped parameters map, or null if empty
     */
    static Map<String, AsyncApiChannelParameter> map(
            Map<String, ? extends Parameter> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return null;
        }

        Map<String, AsyncApiChannelParameter> result = new LinkedHashMap<>();
        for (Map.Entry<String, ? extends Parameter> entry : parameters.entrySet()) {
            Parameter param = entry.getValue();
            Map<String, JsonNode> extensions = null;

            if (param instanceof AsyncApiExtensible extensible) {
                extensions = extensible.getExtensions();
            }

            // Extract schema fields (default, enum, examples)
            String defaultValue = null;
            List<String> enumValues = null;
            List<String> examples = null;

            AsyncApiSchema schema = resolveSchema(param);
            if (schema != null) {
                defaultValue = JsonNodeUtils.jsonNodeToString(schema.getDefault());
                enumValues = JsonNodeUtils.jsonNodeListToStringList(schema.getEnum());
                examples = JsonNodeUtils.jsonNodeListToStringList(schema.getExamples());
            }

            String location = null;
            if (param instanceof AsyncApiParameter asyncParam) {
                location = asyncParam.getLocation();
            }

            result.put(entry.getKey(),
                    new AsyncApiChannelParameter(
                            param.getDescription(),
                            defaultValue,
                            enumValues,
                            examples,
                            location,
                            extensions
                    ));
        }
        return result.isEmpty() ? null : result;
    }

    /**
     * Resolves the schema from a versioned parameter type.
     *
     * @param param the Apicurio parameter object
     * @return the schema object, or null if not available
     */
    private static AsyncApiSchema resolveSchema(Parameter param) {
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
