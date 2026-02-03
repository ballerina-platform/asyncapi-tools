package io.ballerina.asyncapi.core.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

public record AsyncApiServer(
        String host,
        String protocol,
        String protocolVersion,
        String pathname,
        String description,
        String title,
        String summary,
        Map<String, AsyncApiServerVariable> variables,
        List<AsyncApiSecurityRequirement> security,
        List<AsyncApiTag> tags,
        AsyncApiExternalDocs externalDocs,
        AsyncApiBindings bindings,
        Map<String, Object> extensions
) {

    public record AsyncApiServerVariable(
            String name,
            String description,
            String defaultValue,
            List<String> enumValues,
            List<String> examples,
            Map<String, Object> extensions
    ) {
    }

    public record AsyncApiSecurityRequirement(
            String type,
            String description,
            String name,
            String in,
            String scheme,
            String bearerFormat,
            AsyncApiOAuthFlows flows,
            String openIdConnectUrl,
            List<String> scopes,
            Map<String, Object> extensions
    ) {
    }

    public record AsyncApiOAuthFlows(
            AsyncApiOAuthFlow implicit,
            AsyncApiOAuthFlow password,
            AsyncApiOAuthFlow clientCredentials,
            AsyncApiOAuthFlow authorizationCode,
            Map<String, JsonNode> extensions
    ) {

    }

    public record AsyncApiOAuthFlow(
            String authorizationUrl,
            String tokenUrl,
            String refreshUrl,
            Map<String, String> availableScopes,
            Map<String, JsonNode> extensions
    ) {

    }

}
