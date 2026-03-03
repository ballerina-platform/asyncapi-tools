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

import io.apicurio.datamodels.models.ServerVariable;
import io.apicurio.datamodels.models.asyncapi.AsyncApiComponents;
import io.apicurio.datamodels.models.asyncapi.AsyncApiServer;
import io.apicurio.datamodels.models.asyncapi.v20.AsyncApi20Components;
import io.apicurio.datamodels.models.asyncapi.v21.AsyncApi21Components;
import io.apicurio.datamodels.models.asyncapi.v22.AsyncApi22Components;
import io.apicurio.datamodels.models.asyncapi.v23.AsyncApi23Components;
import io.apicurio.datamodels.models.asyncapi.v24.AsyncApi24Components;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25Components;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26Components;
import io.ballerina.asyncapi.core.implementation.common.ServerVariableMapper;
import io.ballerina.asyncapi.core.implementation.v2.server.ServerMapperV2;
import io.ballerina.asyncapi.core.model.server.AsyncApiServerVariable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps server and server variable definitions from AsyncAPI 2.x components.
 */
final class ComponentServerMapperV2 {

    private ComponentServerMapperV2() {

    }

    /**
     * Maps servers from AsyncAPI components.
     * Servers in components are available from AsyncAPI 2.3 onwards.
     *
     * @param components the Apicurio components object
     * @return the map of server names to server objects, or null if not available
     */
    static Map<String, io.ballerina.asyncapi.core.model.server.AsyncApiServer> map(AsyncApiComponents components) {
        if (components == null) {
            return null;
        }

        Map<String, ? extends AsyncApiServer> rawServers = switch (components) {
                    case AsyncApi26Components typed -> typed.getServers();
                    case AsyncApi25Components typed -> typed.getServers();
                    case AsyncApi24Components typed -> typed.getServers();
                    case AsyncApi23Components typed -> typed.getServers();
                    case AsyncApi22Components typed -> null;  // Not available in v2.2
                    case AsyncApi21Components typed -> null;  // Not available in v2.1
                    case AsyncApi20Components typed -> null;  // Not available in v2.0
                    default -> null;
                };

        if (rawServers == null || rawServers.isEmpty()) {
            return null;
        }
        Map<String, io.ballerina.asyncapi.core.model.server.AsyncApiServer> result = new LinkedHashMap<>();
        rawServers.forEach((name, server) -> {
            if (server != null) {
                io.ballerina.asyncapi.core.model.server.AsyncApiServer mapped =
                        ServerMapperV2.mapServerItem(server, components);
                if (mapped != null) {
                    result.put(name, mapped);
                }
            }
        });
        return result.isEmpty() ? null : result;
    }

    /**
     * Maps server variables from AsyncAPI components.
     * Server variables in components are available from AsyncAPI 2.4 onwards.
     *
     * @param components the Apicurio components object
     * @return the map of server variable names to server variable objects, or null if not available
     */
    static Map<String, io.ballerina.asyncapi.core.model.server.AsyncApiServerVariable> mapComponentServerVariables(
            AsyncApiComponents components) {
        if (components == null) {
            return null;
        }

        Map<String, ? extends ServerVariable> rawVariables = switch (components) {
                    case AsyncApi26Components typed -> typed.getServerVariables();
                    case AsyncApi25Components typed -> typed.getServerVariables();
                    case AsyncApi24Components typed -> typed.getServerVariables();
                    case AsyncApi23Components typed -> null;  // Not available in v2.3
                    case AsyncApi22Components typed -> null;  // Not available in v2.2
                    case AsyncApi21Components typed -> null;  // Not available in v2.1
                    case AsyncApi20Components typed -> null;  // Not available in v2.0
                    default -> null;
                };

        if (rawVariables == null || rawVariables.isEmpty()) {
            return null;
        }
        // Component server variables are already resolved definitions — no $ref resolution needed.
        Map<String, AsyncApiServerVariable> result = new HashMap<>();
        for (Map.Entry<String, ? extends ServerVariable> entry : rawVariables.entrySet()) {
            AsyncApiServerVariable mapped = ServerVariableMapper.mapVariable(entry.getValue());
            if (mapped != null) {
                result.put(entry.getKey(), mapped);
            }
        }
        return result.isEmpty() ? null : result;
    }
}
