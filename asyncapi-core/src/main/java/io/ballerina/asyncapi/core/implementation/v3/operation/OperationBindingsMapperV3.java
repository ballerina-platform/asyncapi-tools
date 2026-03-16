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
package io.ballerina.asyncapi.core.implementation.v3.operation;

import com.fasterxml.jackson.databind.JsonNode;
import io.apicurio.datamodels.models.asyncapi.AsyncApiExtensible;
import io.apicurio.datamodels.models.asyncapi.AsyncApiOperationBindings;
import io.ballerina.asyncapi.core.model.operation.HttpOperationBindings;
import io.ballerina.asyncapi.core.model.operation.WsOperationBindings;

import java.util.Map;

/**
 * Maps Apicurio {@link AsyncApiOperationBindings} to
 * {@link io.ballerina.asyncapi.core.model.operation.AsyncApiOperationBindings} for AsyncAPI 3.0.
 */
public final class OperationBindingsMapperV3 {

    private OperationBindingsMapperV3() {
    }

    /**
     * Maps Apicurio {@link AsyncApiOperationBindings} to
     * {@link io.ballerina.asyncapi.core.model.operation.AsyncApiOperationBindings}.
     *
     * @param bindings the Apicurio operation bindings object
     * @return the mapped AsyncApiOperationBindings, or null if bindings is null or empty
     */
    public static io.ballerina.asyncapi.core.model.operation.AsyncApiOperationBindings map(
            AsyncApiOperationBindings bindings) {
        if (bindings == null) {
            return null;
        }
        HttpOperationBindings http = HttpOperationBindingMapperV3.map(bindings.getHttp());
        WsOperationBindings ws = bindings.getWs() != null ? new WsOperationBindings() : null;
        Map<String, JsonNode> extensions = null;
        if (bindings instanceof AsyncApiExtensible extensible) {
            extensions = extensible.getExtensions();
        }
        if (http == null && ws == null && extensions == null) {
            return null;
        }
        return new io.ballerina.asyncapi.core.model.operation.AsyncApiOperationBindings(http, ws, extensions);
    }
}
