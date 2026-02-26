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
package io.ballerina.asyncapi.core.implementation.v2.operation;

import io.apicurio.datamodels.models.asyncapi.AsyncApiBinding;
import io.ballerina.asyncapi.core.implementation.utils.BindingUtils;
import io.ballerina.asyncapi.core.model.component.AsyncApiSchema;
import io.ballerina.asyncapi.core.model.operation.HttpOperationBindings;
import io.ballerina.asyncapi.core.model.operation.HttpOperationBindings.HttpMethod;

/**
 * Maps Apicurio HTTP operation binding to {@link HttpOperationBindings} for AsyncAPI 2.x.
 */
final class HttpOperationBindingMapperV2 {

    private HttpOperationBindingMapperV2() {
    }

    /**
     * Maps an Apicurio HTTP operation binding to {@link HttpOperationBindings}.
     *
     * @param binding the Apicurio HTTP binding object
     * @return the mapped HttpOperationBindings, or null if binding is null
     */
    static HttpOperationBindings map(AsyncApiBinding binding) {
        if (binding == null) {
            return null;
        }
        HttpMethod method = parseHttpMethod(BindingUtils.getItemAsText(binding, "method"));
        Object query = binding.getItem("query");
        String bindingVersion = BindingUtils.getItemAsText(binding, "bindingVersion");
        return new HttpOperationBindings(method, (AsyncApiSchema) query, bindingVersion);
    }

    /**
     * Parses an HTTP method string to {@link HttpMethod} enum.
     *
     * @param methodStr the method string (e.g., "GET", "POST")
     * @return the HttpMethod enum, or null if invalid/null
     */
    private static HttpMethod parseHttpMethod(String methodStr) {
        if (methodStr == null || methodStr.isBlank()) {
            return null;
        }
        try {
            return HttpMethod.valueOf(methodStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
