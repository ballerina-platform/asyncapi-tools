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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Extracts outbound (client-side) API authentication configuration from the standard
 * {@code components.securitySchemes} section of an AsyncAPI document.
 *
 * <p>Distinct from {@link WebhookAuthExtractor}, which reads the custom {@code x-ballerina-auth}
 * extension for inbound webhook verification. This extractor reads a standard AsyncAPI construct
 * with no custom syntax of its own: a spec that honestly documents how its own outbound API calls
 * are authenticated already has everything this extractor needs.
 *
 * <p>Only {@code oauth2} (with the {@code authorizationCode} or {@code clientCredentials} flow),
 * {@code userPassword}, {@code httpApiKey}, and {@code X509} scheme types are currently supported.
 * A recognized scheme type in an unsupported shape (e.g. {@code oauth2} declaring only the
 * {@code implicit} flow, or {@code httpApiKey} missing a required field) fails loudly with a
 * {@link GeneratorException} rather than being silently ignored, since a spec author declaring
 * such a scheme clearly intended it to be used.
 *
 * <p><b>Note:</b> {@code httpApiKey} (supported) and the generic AsyncAPI {@code apiKey} type
 * (out of scope, stays silently ignored like any other unrecognized type) are different
 * {@code type} strings - easy to conflate, but not the same scheme.
 */
public final class ConnectionAuthExtractor {

    private final AsyncApiSpec asyncApiSpec;

    public ConnectionAuthExtractor(AsyncApiSpec asyncApiSpec) {
        this.asyncApiSpec = asyncApiSpec;
    }

    /**
     * Extracts the outbound auth configuration from the spec's {@code components.securitySchemes}.
     *
     * <p>Exactly one {@code oauth2}/{@code userPassword} entry is expected. If more than one is
     * declared, which one the generated trigger should actually use is genuinely ambiguous -
     * rather than silently picking the first one encountered and leaving the rest to be
     * discovered missing at runtime, this fails loudly and names the conflicting entries so the
     * spec author can remove the ones that don't apply to this trigger's outbound calls.
     *
     * @return an {@link Optional} containing the resolved {@link ConnectionAuthConfig} if exactly
     *         one recognized scheme type ({@code oauth2}, {@code userPassword}, {@code httpApiKey},
     *         or {@code X509}) is present; {@link Optional#empty()} if no security schemes are
     *         declared at all, or only scheme types outside this extractor's scope (e.g.
     *         {@code apiKey}, {@code http})
     * @throws GeneratorException if more than one recognized scheme is declared, or if the single
     *                            recognized scheme is present but in an unsupported shape
     *                            (unsupported {@code oauth2} flow, or a required URL missing)
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

        List<Map.Entry<String, AsyncApiSecurityScheme>> candidates = securitySchemes.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().type() != null)
                .filter(entry -> ConnectionAuthConfig.TYPE_USER_PASSWORD.equals(entry.getValue().type())
                        || ConnectionAuthConfig.TYPE_OAUTH2.equals(entry.getValue().type())
                        || ConnectionAuthConfig.TYPE_HTTP_API_KEY.equals(entry.getValue().type())
                        || ConnectionAuthConfig.TYPE_X509.equals(entry.getValue().type()))
                .toList();
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        if (candidates.size() > 1) {
            String names = candidates.stream().map(Map.Entry::getKey).collect(Collectors.joining(", "));
            throw new GeneratorException(
                    "Multiple outbound auth schemes declared under components.securitySchemes (" + names + ") "
                            + "- only one is supported per spec, since it's ambiguous which one the generated "
                            + "trigger should use. Remove the entries that don't apply to this trigger's "
                            + "outbound calls.");
        }

        AsyncApiSecurityScheme scheme = candidates.get(0).getValue();
        if (ConnectionAuthConfig.TYPE_USER_PASSWORD.equals(scheme.type())) {
            return Optional.of(new ConnectionAuthConfig(
                    ConnectionAuthConfig.TYPE_USER_PASSWORD, null, null, null));
        }
        if (ConnectionAuthConfig.TYPE_X509.equals(scheme.type())) {
            // AsyncAPI intentionally carries no certificate material in the spec itself (same as
            // OpenAPI) - the scheme just declares that mutual TLS is required, nothing more to
            // extract.
            return Optional.of(new ConnectionAuthConfig(ConnectionAuthConfig.TYPE_X509, null, null, null));
        }
        if (ConnectionAuthConfig.TYPE_HTTP_API_KEY.equals(scheme.type())) {
            return Optional.of(extractHttpApiKey(scheme));
        }
        return Optional.of(extractOAuth2(scheme));
    }

    private ConnectionAuthConfig extractHttpApiKey(AsyncApiSecurityScheme scheme) throws GeneratorException {
        String name = scheme.name();
        if (name == null || name.isBlank()) {
            throw new GeneratorException(
                    "httpApiKey security scheme requires a 'name' - the header/query/cookie "
                            + "parameter name the API key is sent as");
        }
        String in = scheme.in();
        if (!ConnectionAuthConfig.API_KEY_IN_HEADER.equals(in)
                && !ConnectionAuthConfig.API_KEY_IN_QUERY.equals(in)
                && !ConnectionAuthConfig.API_KEY_IN_COOKIE.equals(in)) {
            throw new GeneratorException(
                    "httpApiKey security scheme requires 'in' to be one of 'header', 'query', "
                            + "or 'cookie', but was: " + in);
        }
        return new ConnectionAuthConfig(
                ConnectionAuthConfig.TYPE_HTTP_API_KEY, null, null, null, name, in);
    }

    private ConnectionAuthConfig extractOAuth2(AsyncApiSecurityScheme scheme) throws GeneratorException {
        AsyncApiOAuthFlows flows = scheme.flows();
        if (flows == null) {
            throw new GeneratorException(
                    "oauth2 security scheme requires a 'flows' object with a supported flow "
                            + "(authorizationCode or clientCredentials)");
        }

        AsyncApiOAuthFlow authorizationCode = flows.authorizationCode();
        AsyncApiOAuthFlow clientCredentials = flows.clientCredentials();
        if (authorizationCode != null && clientCredentials != null) {
            throw new GeneratorException(
                    "oauth2 security scheme declares both authorizationCode and clientCredentials flows "
                            + "- only one is supported per spec, since it's ambiguous which one the "
                            + "generated trigger should use for its outbound calls. Remove the flow "
                            + "that doesn't apply.");
        }

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
