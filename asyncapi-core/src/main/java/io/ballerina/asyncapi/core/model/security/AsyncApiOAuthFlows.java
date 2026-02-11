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

import java.util.Map;

/**
 * Represents the OAuth Flows Object containing configuration for supported OAuth flows.
 *
 * @param implicit          Configuration for the OAuth Implicit flow.
 * @param password          Configuration for the OAuth Resource Owner Password flow.
 * @param clientCredentials Configuration for the OAuth Client Credentials flow.
 * @param authorizationCode Configuration for the OAuth Authorization Code flow.
 * @param extensions        Specification extensions (fields prefixed with "x-").
 */
public record AsyncApiOAuthFlows(
        AsyncApiOAuthFlow implicit,
        AsyncApiOAuthFlow password,
        AsyncApiOAuthFlow clientCredentials,
        AsyncApiOAuthFlow authorizationCode,
        Map<String, JsonNode> extensions
) {
}
