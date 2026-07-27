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
package io.ballerina.asyncapi.generator.http.extractor;

import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.core.model.component.AsyncApiComponent;
import io.ballerina.asyncapi.core.model.security.AsyncApiOAuthFlow;
import io.ballerina.asyncapi.core.model.security.AsyncApiOAuthFlows;
import io.ballerina.asyncapi.core.model.security.AsyncApiSecurityScheme;
import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.http.model.ConnectionAuthConfig;

import java.util.Map;
import java.util.Optional;

/**
 * Extracts outbound (client-side) API authentication configuration from the standard
 * {@code components.securitySchemes} section of an AsyncAPI document.
 *
 * <p>Distinct from {@link WebhookAuthExtractor}, which reads the custom {@code x-ballerina-auth}
 * extension for inbound webhook verification. This extractor reads a standard AsyncAPI construct
 * with no custom syntax of its own: a spec that honestly documents how its own outbound API calls
 * are authenticated already has everything this extractor needs.
 *
 * <p>Only {@code oauth2} (with the {@code authorizationCode} or {@code clientCredentials} flow)
 * and {@code userPassword} scheme types are currently supported. A recognized scheme type in an
 * unsupported shape (e.g. {@code oauth2} declaring only the {@code implicit} flow) fails loudly
 * with a {@link GeneratorException} rather than being silently ignored, since a spec author
 * declaring such a scheme clearly intended it to be used.
 */
public final class ConnectionAuthExtractor {

    private final AsyncApiSpec asyncApiSpec;

    public ConnectionAuthExtractor(AsyncApiSpec asyncApiSpec) {
        this.asyncApiSpec = asyncApiSpec;
    }

    /**
     * Extracts the outbound auth configuration from the spec's {@code components.securitySchemes}.
     *
     * @return an {@link Optional} containing the resolved {@link ConnectionAuthConfig} if a
     *         recognized scheme type ({@code oauth2} or {@code userPassword}) is present;
     *         {@link Optional#empty()} if no security schemes are declared at all, or only
     *         scheme types outside this extractor's scope (e.g. {@code apiKey}, {@code http})
     * @throws GeneratorException if a recognized scheme type is present but in an unsupported
     *                            shape (unsupported {@code oauth2} flow, or a required URL missing)
     */
    public Optional<ConnectionAuthConfig> extract() throws GeneratorException {
        Optional<AsyncApiComponent> components = asyncApiSpec.getAsyncApiComponents();
        if (components.isEmpty()) {
            return Optional.empty();
        }
        Map<String, AsyncApiSecurityScheme> securitySchemes = components.get().securitySchemes();
        if (securitySchemes == null || securitySchemes.isEmpty()) {
            return Optional.empty();
        }

        for (AsyncApiSecurityScheme scheme : securitySchemes.values()) {
            if (scheme == null || scheme.type() == null) {
                continue;
            }
            if (ConnectionAuthConfig.TYPE_USER_PASSWORD.equals(scheme.type())) {
                return Optional.of(new ConnectionAuthConfig(
                        ConnectionAuthConfig.TYPE_USER_PASSWORD, null, null, null));
            }
            if (ConnectionAuthConfig.TYPE_OAUTH2.equals(scheme.type())) {
                return Optional.of(extractOAuth2(scheme));
            }
        }
        return Optional.empty();
    }

    private ConnectionAuthConfig extractOAuth2(AsyncApiSecurityScheme scheme) throws GeneratorException {
        AsyncApiOAuthFlows flows = scheme.flows();
        if (flows == null) {
            throw new GeneratorException(
                    "oauth2 security scheme requires a 'flows' object with a supported flow "
                            + "(authorizationCode or clientCredentials)");
        }

        AsyncApiOAuthFlow authorizationCode = flows.authorizationCode();
        if (authorizationCode != null) {
            if (authorizationCode.refreshUrl() == null) {
                throw new GeneratorException(
                        "oauth2 authorizationCode flow requires a refreshUrl - the generated trigger "
                                + "uses it to refresh access tokens at runtime");
            }
            return new ConnectionAuthConfig(ConnectionAuthConfig.TYPE_OAUTH2,
                    ConnectionAuthConfig.FLOW_AUTHORIZATION_CODE, null,
                    authorizationCode.refreshUrl().toString());
        }

        AsyncApiOAuthFlow clientCredentials = flows.clientCredentials();
        if (clientCredentials != null) {
            if (clientCredentials.tokenUrl() == null) {
                throw new GeneratorException(
                        "oauth2 clientCredentials flow requires a tokenUrl");
            }
            return new ConnectionAuthConfig(ConnectionAuthConfig.TYPE_OAUTH2,
                    ConnectionAuthConfig.FLOW_CLIENT_CREDENTIALS,
                    clientCredentials.tokenUrl().toString(), null);
        }

        throw new GeneratorException(
                "Unsupported oauth2 flow - only authorizationCode and clientCredentials are "
                        + "currently supported for generated trigger outbound authentication");
    }
}
