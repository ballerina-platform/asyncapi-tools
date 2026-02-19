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
package io.ballerina.asyncapi.core.implementation.v2.server;

import com.fasterxml.jackson.databind.JsonNode;
import io.apicurio.datamodels.models.ServerVariable;
import io.apicurio.datamodels.models.asyncapi.v20.AsyncApi20ServerVariable;
import io.apicurio.datamodels.models.asyncapi.v21.AsyncApi21ServerVariable;
import io.apicurio.datamodels.models.asyncapi.v22.AsyncApi22ServerVariable;
import io.apicurio.datamodels.models.asyncapi.v23.AsyncApi23ServerVariable;
import io.apicurio.datamodels.models.asyncapi.v24.AsyncApi24ServerVariable;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25ServerVariable;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26ServerVariable;
import io.ballerina.asyncapi.core.model.server.AsyncApiServerVariable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps Apicurio {@link ServerVariable} to {@link AsyncApiServerVariable}
 * for AsyncAPI 2.x.
 */
public final class ServerVariableMapperV2 {

    private ServerVariableMapperV2() {

    }

    /**
     * Maps a map of Apicurio server variables to a map of {@link AsyncApiServerVariable}.
     *
     * @param variables the Apicurio server variables map
     * @return the mapped variables map, or null if variables is null or empty
     */
    public static Map<String, AsyncApiServerVariable> mapVariables(
            Map<String, ? extends ServerVariable> variables) {
        if (variables == null || variables.isEmpty()) {
            return null;
        }
        Map<String, AsyncApiServerVariable> result = new LinkedHashMap<>();
        for (Map.Entry<String, ? extends ServerVariable> entry : variables.entrySet()) {
            result.put(entry.getKey(), mapVariable(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    /**
     * Maps an Apicurio {@link ServerVariable} to an {@link AsyncApiServerVariable}.
     *
     * @param name     the variable name (from the map key)
     * @param variable the Apicurio server variable object
     * @return the mapped AsyncApiServerVariable
     */
    static AsyncApiServerVariable mapVariable(String name, ServerVariable variable) {
        Map<String, JsonNode> extensions = null;
        List<String> examples = null;
        switch (variable) {
            case AsyncApi26ServerVariable typed -> {
                extensions = typed.getExtensions();
                examples = typed.getExamples();
            }
            case AsyncApi25ServerVariable typed -> {
                extensions = typed.getExtensions();
                examples = typed.getExamples();
            }
            case AsyncApi24ServerVariable typed -> {
                extensions = typed.getExtensions();
                examples = typed.getExamples();
            }
            case AsyncApi23ServerVariable typed -> {
                extensions = typed.getExtensions();
                examples = typed.getExamples();
            }
            case AsyncApi22ServerVariable typed -> {
                extensions = typed.getExtensions();
                examples = typed.getExamples();
            }
            case AsyncApi21ServerVariable typed -> {
                extensions = typed.getExtensions();
                examples = typed.getExamples();
            }
            case AsyncApi20ServerVariable typed -> {
                extensions = typed.getExtensions();
                examples = typed.getExamples();
            }
            default -> { }
        }
        return new AsyncApiServerVariable(
                name,
                variable.getDescription(),
                variable.getDefault(),
                variable.getEnum(),
                examples,
                extensions
        );
    }
}
