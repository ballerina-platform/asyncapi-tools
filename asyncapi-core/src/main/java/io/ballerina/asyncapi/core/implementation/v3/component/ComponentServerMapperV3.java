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

import io.apicurio.datamodels.models.asyncapi.AsyncApiComponents;
import io.apicurio.datamodels.models.asyncapi.AsyncApiServers;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Components;
import io.ballerina.asyncapi.core.implementation.v3.server.ServerMapperV3;
import io.ballerina.asyncapi.core.model.server.AsyncApiServer;
import io.ballerina.asyncapi.core.model.server.AsyncApiServerVariable;

import java.util.Map;

/**
 * Maps server and server variable definitions from AsyncAPI 3.x components.
 */
final class ComponentServerMapperV3 {

    private ComponentServerMapperV3() {

    }

    /**
     * Maps servers from AsyncAPI components.
     *
     * @param components the Apicurio components object
     * @return the map of server names to server objects, or null if not available
     */
    static Map<String, AsyncApiServer> map(AsyncApiComponents components) {
        if (components == null) {
            return null;
        }

        // Add additional version cases here as new AsyncAPI 3.x versions are supported.
        return switch (components) {
            case AsyncApi30Components typed -> ServerMapperV3.map((AsyncApiServers) typed.getServers(), typed);
            default -> null;
        };
    }

    /**
     * Maps server variables from AsyncAPI components.
     *
     * @param components the Apicurio components object
     * @return the map of server variable names to server variable objects, or null if not available
     */
    static Map<String, AsyncApiServerVariable> mapComponentServerVariables(AsyncApiComponents components) {
        if (components == null) {
            return null;
        }

        // Add additional version cases here as new AsyncAPI 3.x versions are supported.
        return switch (components) {
            case AsyncApi30Components typed -> ComponentServerVariableMapperV3.map(typed.getServerVariables());
            default -> null;
        };
    }

}
