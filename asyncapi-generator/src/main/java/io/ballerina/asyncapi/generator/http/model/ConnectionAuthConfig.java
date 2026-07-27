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
 * @param type       the security scheme type: {@code "oauth2"} or {@code "userPassword"}
 * @param flow       for {@code type == "oauth2"}, the OAuth flow: {@code "authorizationCode"} or
 *                   {@code "clientCredentials"}; {@code null} for {@code "userPassword"}
 * @param tokenUrl   the resolved token endpoint URL for the {@code clientCredentials} flow;
 *                   {@code null} otherwise. Generated as a constant, not a user-configurable field,
 *                   since it is fixed by the provider rather than varying per deployment.
 * @param refreshUrl the resolved refresh endpoint URL for the {@code authorizationCode} flow;
 *                   {@code null} otherwise. Generated as a constant for the same reason.
 */
public record ConnectionAuthConfig(
        String type,
        String flow,
        String tokenUrl,
        String refreshUrl
) {

    public static final String TYPE_OAUTH2 = "oauth2";
    public static final String TYPE_USER_PASSWORD = "userPassword";
    public static final String FLOW_AUTHORIZATION_CODE = "authorizationCode";
    public static final String FLOW_CLIENT_CREDENTIALS = "clientCredentials";
}
