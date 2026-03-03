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
package io.ballerina.asyncapi.core.implementation.v3.server;

import com.fasterxml.jackson.databind.JsonNode;
import io.apicurio.datamodels.models.OAuthFlow;
import io.apicurio.datamodels.models.asyncapi.AsyncApiComponents;
import io.apicurio.datamodels.models.asyncapi.AsyncApiExtensible;
import io.apicurio.datamodels.models.asyncapi.AsyncApiOAuthFlows;
import io.apicurio.datamodels.models.asyncapi.AsyncApiReferenceable;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Components;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30OAuthFlow;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30SecurityScheme;
import io.ballerina.asyncapi.core.Constants;
import io.ballerina.asyncapi.core.implementation.utils.URIUtils;
import io.ballerina.asyncapi.core.model.security.AsyncApiOAuthFlow;
import io.ballerina.asyncapi.core.model.security.AsyncApiSecurityScheme;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Maps Apicurio security scheme to {@link AsyncApiSecurityScheme}
 * for AsyncAPI 3.0.
 */
public final class SecuritySchemeMapperV3 {

    private static final Logger LOG = LogManager.getLogger(SecuritySchemeMapperV3.class);

    private SecuritySchemeMapperV3() {
    }

    /**
     * Maps a list of Apicurio security schemes to a list of {@link AsyncApiSecurityScheme},
     * resolving any {@code $ref} entries via the provided components object.
     *
     * @param securitySchemes the Apicurio security schemes list
     * @param components      the Apicurio components object used for {@code $ref} resolution
     * @return the mapped security schemes list, or null if empty
     */
    public static List<AsyncApiSecurityScheme> map(
            List<? extends io.apicurio.datamodels.models.asyncapi.AsyncApiSecurityScheme> securitySchemes,
            AsyncApiComponents components) {
        if (securitySchemes == null || securitySchemes.isEmpty()) {
            return null;
        }
        List<AsyncApiSecurityScheme> result = securitySchemes.stream()
                .map(scheme -> mapSecurityItem(scheme, components))
                .filter(Objects::nonNull)
                .toList();
        return result.isEmpty() ? null : result;
    }

    /**
     * Maps an Apicurio {@link io.apicurio.datamodels.models.asyncapi.AsyncApiSecurityScheme}
     * to an {@link AsyncApiSecurityScheme}, resolving any {@code $ref} via the provided
     * components object.
     *
     * @param scheme     the Apicurio security scheme object (may be a reference)
     * @param components the Apicurio components object used for {@code $ref} resolution
     *                   (may be null)
     * @return the mapped AsyncApiSecurityScheme, or null for unresolvable refs
     */
    public static AsyncApiSecurityScheme mapSecurityItem(
            io.apicurio.datamodels.models.asyncapi.AsyncApiSecurityScheme scheme,
            AsyncApiComponents components) {
        if (scheme == null) {
            return null;
        }
        String $ref = null;
        if (scheme instanceof AsyncApiReferenceable referenceable) {
            $ref = referenceable.get$ref();
        }
        if ($ref != null) {
            io.apicurio.datamodels.models.asyncapi.AsyncApiSecurityScheme resolved = resolveRef($ref, components);
            if (resolved == null) {
                LOG.warn("Could not resolve $ref: {}. Skipping security scheme.", $ref);
                return null;
            }
            return mapSecurityItem(resolved, components);
        }
        Map<String, JsonNode> extensions = null;
        if (scheme instanceof AsyncApiExtensible extensible) {
            extensions = extensible.getExtensions();
        }
        List<String> scopes = null;
        if (scheme instanceof AsyncApi30SecurityScheme typedScheme) {
            scopes = typedScheme.getScopes();
        }
        return new AsyncApiSecurityScheme(
                scheme.getType(),
                scheme.getDescription(),
                scheme.getName(),
                scheme.getIn(),
                scheme.getScheme(),
                scheme.getBearerFormat(),
                mapOAuthFlows(scheme.getFlows()),
                URIUtils.toUri(scheme.getOpenIdConnectUrl()),
                scopes,
                extensions
        );
    }

    /**
     * Resolves a {@code $ref} to a component security scheme by extracting the name from the
     * reference string and looking it up in the AsyncAPI 3.0 components map.
     *
     * @param $ref       the reference string (e.g. {@code #/components/securitySchemes/MyScheme})
     * @param components the Apicurio components object
     * @return the resolved security scheme, or null if not found
     */
    private static io.apicurio.datamodels.models.asyncapi.AsyncApiSecurityScheme resolveRef(String $ref,
                                                                                            AsyncApiComponents components) {
        if (components == null) {
            LOG.warn("Cannot resolve $ref: {}. Components is null.", $ref);
            return null;
        }
        Set<String> visited = new HashSet<>();
        String current = $ref;
        while (current != null) {
            if (!current.startsWith(Constants.SECURITY_SCHEMES_REF_PREFIX)) {
                LOG.warn("Unsupported $ref format: {}. Skipping security scheme.", current);
                return null;
            }
            if (!visited.add(current)) {
                LOG.warn("Cyclic $ref detected: {}. Skipping security scheme.", current);
                return null;
            }
            String name = current.substring(Constants.SECURITY_SCHEMES_REF_PREFIX.length());
            // Add additional version checks here as new AsyncAPI 3.x versions are supported.
            io.apicurio.datamodels.models.asyncapi.AsyncApiSecurityScheme resolved = null;
            if (components instanceof AsyncApi30Components typed) {
                io.apicurio.datamodels.models.SecurityScheme found = typed.getSecuritySchemes() != null
                        ? typed.getSecuritySchemes().get(name) : null;
                if (found instanceof io.apicurio.datamodels.models.asyncapi.AsyncApiSecurityScheme typedScheme) {
                    resolved = typedScheme;
                }
            }
            if (resolved == null) {
                LOG.warn("Could not resolve $ref: '{}'. No matching security scheme found.", current);
                return null;
            }
            if (resolved instanceof AsyncApiReferenceable resolvedTyped && resolvedTyped.get$ref() != null) {
                current = resolvedTyped.get$ref();
            } else {
                return resolved;
            }
        }
        return null;
    }

    /**
     * Maps Apicurio {@link AsyncApiOAuthFlows} to
     * {@link io.ballerina.asyncapi.core.model.security.AsyncApiOAuthFlows}.
     *
     * @param flows the Apicurio OAuth flows object
     * @return the mapped AsyncApiOAuthFlows, or null if flows is null
     */
    private static io.ballerina.asyncapi.core.model.security.AsyncApiOAuthFlows mapOAuthFlows(
            AsyncApiOAuthFlows flows) {
        if (flows == null) {
            return null;
        }
        Map<String, JsonNode> extensions = null;
        if (flows instanceof AsyncApiExtensible extensible) {
            extensions = extensible.getExtensions();
        }
        return new io.ballerina.asyncapi.core.model.security.AsyncApiOAuthFlows(
                mapOAuthFlow(flows.getImplicit()),
                mapOAuthFlow(flows.getPassword()),
                mapOAuthFlow(flows.getClientCredentials()),
                mapOAuthFlow(flows.getAuthorizationCode()),
                extensions
        );
    }

    /**
     * Maps an Apicurio {@link OAuthFlow} to an {@link AsyncApiOAuthFlow}.
     *
     * @param flow the Apicurio OAuth flow object
     * @return the mapped AsyncApiOAuthFlow, or null if flow is null
     */
    private static AsyncApiOAuthFlow mapOAuthFlow(OAuthFlow flow) {
        if (flow == null) {
            return null;
        }
        Map<String, String> availableScopes = null;
        Map<String, JsonNode> extensions = null;
        if (flow instanceof AsyncApi30OAuthFlow typedFlow) {
            availableScopes = typedFlow.getAvailableScopes();
            extensions = typedFlow.getExtensions();
        }
        return new AsyncApiOAuthFlow(
                URIUtils.toUri(flow.getAuthorizationUrl()),
                URIUtils.toUri(flow.getTokenUrl()),
                URIUtils.toUri(flow.getRefreshUrl()),
                availableScopes,
                extensions
        );
    }
}
