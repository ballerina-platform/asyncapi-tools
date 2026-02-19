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

import com.fasterxml.jackson.databind.JsonNode;
import io.apicurio.datamodels.models.asyncapi.AsyncApiBinding;
import io.ballerina.asyncapi.core.model.message.HttpMessageBindings;

/**
 * Maps Apicurio AsyncAPI 3.0 HTTP message bindings to {@link HttpMessageBindings}.
 */
final class HttpMessageBindingMapperV3 {

    private HttpMessageBindingMapperV3() {
    }

    /**
     * Maps an Apicurio AsyncAPI binding to an HttpMessageBindings domain model.
     *
     * @param binding the Apicurio binding object (from AsyncApi30MessageBindings.getHttp())
     * @return the mapped HttpMessageBindings, or null if binding is null
     */
    static HttpMessageBindings map(AsyncApiBinding binding) {
        if (binding == null) {
            return null;
        }

        Object headers = binding.getItem("headers");  // Schema Object - store as raw Object
        Integer statusCode = getBindingItemAsInteger(binding, "statusCode");
        String bindingVersion = getBindingItemAsText(binding, "bindingVersion");

        return new HttpMessageBindings(headers, statusCode, bindingVersion);
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
     * Extracts a binding field as integer.
     *
     * @param binding the Apicurio binding object
     * @param key the field name
     * @return the field value as Integer, or null if not present or not numeric
     */
    private static Integer getBindingItemAsInteger(AsyncApiBinding binding, String key) {
        JsonNode node = binding.getItem(key);
        if (node == null) {
            return null;
        }
        return node.isNumber() ? node.asInt() : null;
    }
}
