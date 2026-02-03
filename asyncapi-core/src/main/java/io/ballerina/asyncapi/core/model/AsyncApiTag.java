package io.ballerina.asyncapi.core.model;

import java.util.Map;

public record AsyncApiTag(
        String name,
        String description,
        AsyncApiExternalDocs externalDocs,
        Map<String, Object> extensions
) {
}
