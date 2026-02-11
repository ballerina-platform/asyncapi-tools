package io.ballerina.asyncapi.core.model.security;

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
public record AsyncApiSecurityScheme(
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
}
