package io.ballerina.asyncapi.core.model;

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
        AsyncApiServerBindings bindings,
        Map<String, String> extensions
) {

    public record AsyncApiServerVariable(
            String name,
            String description,
            String defaultValue,
            List<String> enumValues,
            List<String> examples,
            Map<String, String> extensions
    ) {
    }

    public record AsyncApiServerBindings(
            Map<String, String> httpServerBindings,
            Map<String, String> wsServerBindings
    ) {
    }

}
