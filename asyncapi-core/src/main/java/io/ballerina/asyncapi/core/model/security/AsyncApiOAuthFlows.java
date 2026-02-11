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
