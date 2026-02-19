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
import io.apicurio.datamodels.models.asyncapi.AsyncApiOAuthFlows;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30OAuthFlow;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30OAuthFlows;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30SecurityScheme;
import io.ballerina.asyncapi.core.implementation.utils.URIUtils;
import io.ballerina.asyncapi.core.model.security.AsyncApiOAuthFlow;
import io.ballerina.asyncapi.core.model.security.AsyncApiSecurityScheme;

import java.util.Map;

/**
 * Maps Apicurio {@link AsyncApi30SecurityScheme} to {@link AsyncApiSecurityScheme}
 * for AsyncAPI 3.0.
 */
final class SecuritySchemeMapperV3 {

    private SecuritySchemeMapperV3() {

    }

    /**
     * Maps an Apicurio {@link AsyncApi30SecurityScheme} to an {@link AsyncApiSecurityScheme}.
     *
     * @param scheme the Apicurio security scheme object
     * @return the mapped AsyncApiSecurityScheme
     */
    static AsyncApiSecurityScheme map(AsyncApi30SecurityScheme scheme) {
        return new AsyncApiSecurityScheme(
                scheme.getType(),
                scheme.getDescription(),
                scheme.getName(),
                scheme.getIn(),
                scheme.getScheme(),
                scheme.getBearerFormat(),
                mapOAuthFlows(scheme.getFlows()),
                URIUtils.toUri(scheme.getOpenIdConnectUrl()),
                scheme.getScopes(),
                scheme.getExtensions()
        );
    }

    /**
     * Maps Apicurio {@link AsyncApiOAuthFlows} to
     * {@link io.ballerina.asyncapi.core.model.security.AsyncApiOAuthFlows}.
     *
     * @param flows the Apicurio OAuth flows object
     * @return the mapped AsyncApiOAuthFlows, or null if flows is null
     */
    private static io.ballerina.asyncapi.core.model.security.AsyncApiOAuthFlows
            mapOAuthFlows(AsyncApiOAuthFlows flows) {
        if (flows == null) {
            return null;
        }
        Map<String, JsonNode> extensions = null;
        if (flows instanceof AsyncApi30OAuthFlows typedFlows) {
            extensions = typedFlows.getExtensions();
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
