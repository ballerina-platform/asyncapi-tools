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
package io.ballerina.asyncapi.core.implementation.v2.server;

import com.fasterxml.jackson.databind.JsonNode;
import io.apicurio.datamodels.models.OAuthFlow;
import io.apicurio.datamodels.models.SecurityRequirement;
import io.apicurio.datamodels.models.SecurityScheme;
import io.apicurio.datamodels.models.asyncapi.AsyncApiExtensible;
import io.apicurio.datamodels.models.asyncapi.AsyncApiOAuthFlows;
import io.apicurio.datamodels.models.asyncapi.v20.AsyncApi20OAuthFlow;
import io.apicurio.datamodels.models.asyncapi.v21.AsyncApi21OAuthFlow;
import io.apicurio.datamodels.models.asyncapi.v22.AsyncApi22OAuthFlow;
import io.apicurio.datamodels.models.asyncapi.v23.AsyncApi23OAuthFlow;
import io.apicurio.datamodels.models.asyncapi.v24.AsyncApi24OAuthFlow;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25OAuthFlow;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26OAuthFlow;
import io.ballerina.asyncapi.core.implementation.utils.URIUtils;
import io.ballerina.asyncapi.core.model.security.AsyncApiOAuthFlow;
import io.ballerina.asyncapi.core.model.security.AsyncApiSecurityScheme;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Maps Apicurio security scheme to {@link AsyncApiSecurityScheme}
 * for AsyncAPI 2.x.
 */
public final class SecuritySchemeMapperV2 {

    private SecuritySchemeMapperV2() {
    }

    /**
     * Resolves security requirements against the component security scheme definitions.
     *
     * @param requirements the security requirements from a server
     * @param schemeLookup the security scheme definitions from components
     * @return the mapped security schemes list, or null if empty
     */
    static List<AsyncApiSecurityScheme> map(List<SecurityRequirement> requirements,
                                            Map<String, SecurityScheme> schemeLookup) {
        if (requirements == null || requirements.isEmpty() || schemeLookup == null) {
            return null;
        }
        List<AsyncApiSecurityScheme> result = new ArrayList<>();
        for (SecurityRequirement requirement : requirements) {
            List<String> names = requirement.getItemNames();
            if (names == null) {
                continue;
            }
            for (String name : names) {
                SecurityScheme scheme = schemeLookup.get(name);
                if (scheme instanceof io.apicurio.datamodels.models.asyncapi.AsyncApiSecurityScheme typedScheme) {
                    result.add(mapSecurityItem(typedScheme));
                }
            }
        }
        return result.isEmpty() ? null : result;
    }

    /**
     * Maps an Apicurio {@link io.apicurio.datamodels.models.asyncapi.AsyncApiSecurityScheme}
     * to {@link AsyncApiSecurityScheme}.
     *
     * @param scheme the Apicurio security scheme object
     * @return the mapped AsyncApiSecurityScheme
     */
    public static AsyncApiSecurityScheme mapSecurityItem(
            io.apicurio.datamodels.models.asyncapi.AsyncApiSecurityScheme scheme) {
        Map<String, JsonNode> extensions = null;
        if (scheme instanceof AsyncApiExtensible extensible) {
            extensions = extensible.getExtensions();
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
                extensions
        );
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

        Map<String, JsonNode> extensions = null;
        if (flow instanceof AsyncApiExtensible extensible) {
            extensions = extensible.getExtensions();
        }
        Map<String, String> availableScopes = switch (flow) {
            case AsyncApi26OAuthFlow typed -> typed.getScopes();
            case AsyncApi25OAuthFlow typed -> typed.getScopes();
            case AsyncApi24OAuthFlow typed -> typed.getScopes();
            case AsyncApi23OAuthFlow typed -> typed.getScopes();
            case AsyncApi22OAuthFlow typed -> typed.getScopes();
            case AsyncApi21OAuthFlow typed -> typed.getScopes();
            case AsyncApi20OAuthFlow typed -> typed.getScopes();
            default -> throw new IllegalArgumentException("Unsupported OAuthFlow type: " + flow.getClass().getName());
        };

        return new AsyncApiOAuthFlow(
                URIUtils.toUri(flow.getAuthorizationUrl()),
                URIUtils.toUri(flow.getTokenUrl()),
                URIUtils.toUri(flow.getRefreshUrl()),
                availableScopes,
                extensions
        );
    }
}
