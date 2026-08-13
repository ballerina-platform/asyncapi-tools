/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com)
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.ballerina.asyncapi.generator.http.model;

/**
 * Holds the resolved outbound (client-side) API authentication configuration, extracted from a
 * recognized entry in the AsyncAPI document's standard {@code components.securitySchemes} section.
 *
 * <p>Distinct from {@link WebhookAuthConfig}: this describes credentials the generated trigger
 * needs to call the provider's own API (e.g. to register a webhook subscription or fetch a
 * changed resource), not credentials used to verify an inbound webhook request.
 *
 * @param type       the security scheme type: {@code "oauth2"}, {@code "userPassword"},
 *                   {@code "httpApiKey"}, or {@code "X509"}
 * @param flow       for {@code type == "oauth2"}, the OAuth flow: {@code "authorizationCode"} or
 *                   {@code "clientCredentials"}; {@code null} for every other type
 * @param tokenUrl   the resolved token endpoint URL for the {@code clientCredentials} flow;
 *                   {@code null} otherwise. Generated as a {@code ListenerConfig} field defaulted
 *                   to this value (not hardcoded), since a spec cannot distinguish a URL that's
 *                   fixed by the provider from one that varies per deployment (e.g. a per-tenant
 *                   identity provider) - the default covers the common case while staying
 *                   overridable for the other.
 * @param refreshUrl the resolved refresh endpoint URL for the {@code authorizationCode} flow;
 *                   {@code null} otherwise. Generated the same way, for the same reason.
 * @param apiKeyName for {@code type == "httpApiKey"}, the header/query/cookie parameter name the
 *                   API key is sent as (the spec's {@code name}); {@code null} otherwise.
 * @param apiKeyIn   for {@code type == "httpApiKey"}, where the key is sent - one of
 *                   {@code "header"}, {@code "query"}, {@code "cookie"} (the spec's {@code in});
 *                   {@code null} otherwise.
 */
public record ConnectionAuthConfig(
        String type,
        String flow,
        String tokenUrl,
        String refreshUrl,
        String apiKeyName,
        String apiKeyIn
) {

    public static final String TYPE_OAUTH2 = "oauth2";
    public static final String TYPE_USER_PASSWORD = "userPassword";
    public static final String TYPE_HTTP_API_KEY = "httpApiKey";
    public static final String TYPE_X509 = "X509";
    public static final String FLOW_AUTHORIZATION_CODE = "authorizationCode";
    public static final String FLOW_CLIENT_CREDENTIALS = "clientCredentials";
    public static final String API_KEY_IN_HEADER = "header";
    public static final String API_KEY_IN_QUERY = "query";
    public static final String API_KEY_IN_COOKIE = "cookie";

    /**
     * Convenience constructor for scheme types with no API-key location info
     * ({@code oauth2}, {@code userPassword}), defaulting {@code apiKeyName}/{@code apiKeyIn} to
     * {@code null}.
     *
     * @param type       the security scheme type
     * @param flow       the OAuth flow, or {@code null}
     * @param tokenUrl   the resolved token endpoint URL, or {@code null}
     * @param refreshUrl the resolved refresh endpoint URL, or {@code null}
     */
    public ConnectionAuthConfig(String type, String flow, String tokenUrl, String refreshUrl) {
        this(type, flow, tokenUrl, refreshUrl, null, null);
    }
}
