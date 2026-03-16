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
package io.ballerina.asyncapi.core.model.security;

import com.fasterxml.jackson.databind.JsonNode;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Represents a Security Scheme Object in an AsyncAPI document.
 *
 * @param type             The type of the security scheme (e.g., "userPassword", "apiKey", "http", "oauth2",
 *                         "openIdConnect").
 * @param description      A description for the security scheme.
 * @param name             The name of the header, query, or cookie parameter to be used.
 * @param in               The location of the API key (e.g., "user", "password", "query", "header", "cookie").
 * @param scheme           The name of the HTTP Authorization scheme (e.g., "bearer").
 * @param bearerFormat     A hint to the client to identify how the bearer token is formatted.
 * @param flows            The OAuth Flows configuration.
 * @param openIdConnectUrl The URI to the OpenID Connect discovery document.
 * @param scopes           A list of required scope names for the security scheme.
 * @param extensions       Specification extensions (fields prefixed with "x-").
 */
public record AsyncApiSecurityScheme(
        String type,
        String description,
        String name,
        String in,
        String scheme,
        String bearerFormat,
        AsyncApiOAuthFlows flows,
        URI openIdConnectUrl,
        List<String> scopes,
        Map<String, JsonNode> extensions
) {

    /**
     * Creates an {@link AsyncApiSecurityScheme} without {@code scopes},
     * defaulting it to {@code null}. Used for AsyncAPI 2.x where the
     * {@code scopes} field does not exist on security scheme objects.
     *
     * @param type             the type of the security scheme
     * @param description      a description for the security scheme
     * @param name             the name of the header, query, or cookie parameter
     * @param in               the location of the API key
     * @param scheme           the HTTP Authorization scheme name
     * @param bearerFormat     hint for the bearer token format
     * @param flows            the OAuth Flows configuration
     * @param openIdConnectUrl the URI to the OpenID Connect discovery document
     * @param extensions       specification extensions prefixed with {@code "x-"}
     */
    public AsyncApiSecurityScheme(String type, String description, String name, String in,
            String scheme, String bearerFormat, AsyncApiOAuthFlows flows,
            URI openIdConnectUrl, Map<String, JsonNode> extensions) {
        this(type, description, name, in, scheme, bearerFormat, flows,
                openIdConnectUrl, null, extensions);
    }
}
