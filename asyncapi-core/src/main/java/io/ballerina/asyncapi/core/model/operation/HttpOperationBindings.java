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
package io.ballerina.asyncapi.core.model.operation;

import io.ballerina.asyncapi.core.model.component.AsyncApiSchema;

/**
 * HTTP Operation Binding Object.
 *
 * @param method         The HTTP method for the request.
 * @param query          A Schema Object for the query parameters.
 * @param bindingVersion The version of this binding.
 */
public record HttpOperationBindings(
        HttpMethod method,
        AsyncApiSchema query,
        String bindingVersion
) {

    /** The HTTP method for the request in an HTTP operation binding. */
    public enum HttpMethod {
        GET,
        POST,
        PUT,
        PATCH,
        DELETE,
        HEAD,
        OPTIONS,
        CONNECT,
        TRACE
    }
}
