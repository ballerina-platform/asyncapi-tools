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
package io.ballerina.asyncapi.core.model.message;

import io.ballerina.asyncapi.core.model.component.AsyncApiSchema;

/**
 * HTTP Message Binding Object.
 *
 * @param headers        A Schema Object containing the definitions for the HTTP headers.
 * @param statusCode     The HTTP response status code.
 * @param bindingVersion The version of this binding.
 */
public record HttpMessageBindings(
        AsyncApiSchema headers,
        Integer statusCode,
        String bindingVersion
) {

    /**
     * Creates an {@link HttpMessageBindings} without a {@code headers} schema,
     * equivalent to passing {@code null} for {@code headers}.
     *
     * @param statusCode     the HTTP response status code
     * @param bindingVersion the version of this binding
     */
    public HttpMessageBindings(Integer statusCode, String bindingVersion) {
        this(null, statusCode, bindingVersion);
    }
}
