package io.ballerina.asyncapi.core.model;

import java.util.Map;

public record AsyncApiExternalDocs(
        String description,
        String url,
        Map<String, Object> extensions
) {
}
