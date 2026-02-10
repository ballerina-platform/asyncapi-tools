package io.ballerina.asyncapi.core.model;

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
public record AsyncApiSecurityRequirement(
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

    /**
     * Represents a single OAuth Flow Object with token endpoint configuration.
     *
     * @param authorizationUrl The authorization URL for this flow.
     * @param tokenUrl         The token URL for this flow.
     * @param refreshUrl       The URL to be used for obtaining refresh tokens.
     * @param availableScopes  A map of available scope names to their descriptions.
     * @param extensions       Specification extensions (fields prefixed with "x-").
     */
    public record AsyncApiOAuthFlow(
            URI authorizationUrl,
            URI tokenUrl,
            URI refreshUrl,
            Map<String, String> availableScopes,
            Map<String, JsonNode> extensions
    ) {
    }
}
