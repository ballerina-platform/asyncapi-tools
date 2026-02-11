package io.ballerina.asyncapi.core.model.security;

import com.fasterxml.jackson.databind.JsonNode;

import java.net.URI;
import java.util.Map;

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
