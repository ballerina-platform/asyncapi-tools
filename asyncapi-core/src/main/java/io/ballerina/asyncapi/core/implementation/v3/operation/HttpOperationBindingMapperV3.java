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
import io.apicurio.datamodels.models.asyncapi.AsyncApiBinding;
import io.ballerina.asyncapi.core.model.operation.HttpOperationBindings;
import io.ballerina.asyncapi.core.model.operation.HttpOperationBindings.HttpMethod;
import io.ballerina.asyncapi.core.model.component.AsyncApiSchema;

/**
 * Maps Apicurio AsyncAPI 3.0 HTTP operation bindings to {@link HttpOperationBindings}.
 */
final class HttpOperationBindingMapperV3 {

    private HttpOperationBindingMapperV3() {
    }

    /**
     * Maps an Apicurio AsyncAPI binding to an HttpOperationBindings domain model.
     *
     * @param binding the Apicurio binding object (from AsyncApi30OperationBindings.getHttp())
     * @return the mapped HttpOperationBindings, or null if binding is null
     */
    static HttpOperationBindings map(AsyncApiBinding binding) {
        if (binding == null) {
            return null;
        }

        HttpMethod method = parseHttpMethod(getBindingItemAsText(binding, "method"));
        Object query = binding.getItem("query");  // Schema Object - store as raw Object
        String bindingVersion = getBindingItemAsText(binding, "bindingVersion");

        return new HttpOperationBindings(method, (AsyncApiSchema) query, bindingVersion);
    }

    /**
     * Extracts a binding field as text.
     *
     * @param binding the Apicurio binding object
     * @param key the field name
     * @return the field value as String, or null if not present or not textual
     */
    private static String getBindingItemAsText(AsyncApiBinding binding, String key) {
        JsonNode node = binding.getItem(key);
        if (node == null) {
            return null;
        }
        return node.isTextual() ? node.asText() : node.toString();
    }

    /**
     * Parses HTTP method string to HttpMethod enum.
     *
     * @param methodStr the method string (GET, POST, etc.)
     * @return the HttpMethod enum value, or null if invalid/null
     */
    private static HttpMethod parseHttpMethod(String methodStr) {
        if (methodStr == null || methodStr.isBlank()) {
            return null;
        }
        try {
            return HttpMethod.valueOf(methodStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;  // Invalid method - return null instead of throwing
        }
    }
}
