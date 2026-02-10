package io.ballerina.asyncapi.core.model;

import java.net.URI;
import java.util.List;
import java.util.Map;

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
        Map<String, String> extensions
) {

    public record AsyncApiOAuthFlows(
            AsyncApiOAuthFlow implicit,
            AsyncApiOAuthFlow password,
            AsyncApiOAuthFlow clientCredentials,
            AsyncApiOAuthFlow authorizationCode,
            Map<String, String> extensions
    ) {
    }

    public record AsyncApiOAuthFlow(
            URI authorizationUrl,
            URI tokenUrl,
            URI refreshUrl,
            Map<String, String> availableScopes,
            Map<String, String> extensions
    ) {
    }
}
