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
package io.ballerina.asyncapi.core.implementation.common;

import com.fasterxml.jackson.databind.JsonNode;
import io.apicurio.datamodels.models.ServerVariable;
import io.apicurio.datamodels.models.asyncapi.AsyncApiExtensible;
import io.ballerina.asyncapi.core.model.server.AsyncApiServerVariable;

import java.util.List;
import java.util.Map;

/**
 * Shared utility for mapping an Apicurio {@link ServerVariable} to the common
 * {@link AsyncApiServerVariable} model, version-independently.
 */
public final class ServerVariableMapper {

    private ServerVariableMapper() {
    }

    /**
     * Maps an Apicurio {@link ServerVariable} to an {@link AsyncApiServerVariable}.
     *
     * @param variable the Apicurio server variable object
     * @return the mapped AsyncApiServerVariable
     */
    public static AsyncApiServerVariable mapVariable(ServerVariable variable) {
        if (variable == null) {
            return null;
        }
        Map<String, JsonNode> extensions = null;
        if (variable instanceof AsyncApiExtensible extensible) {
            extensions = extensible.getExtensions();
        }
        List<String> examples = null;
        if (variable instanceof io.apicurio.datamodels.models.asyncapi.AsyncApiServerVariable asyncVar) {
            examples = asyncVar.getExamples();
        }
        return new AsyncApiServerVariable(
                variable.getDescription(),
                variable.getDefault(),
                variable.getEnum(),
                examples,
                extensions
        );
    }
}
