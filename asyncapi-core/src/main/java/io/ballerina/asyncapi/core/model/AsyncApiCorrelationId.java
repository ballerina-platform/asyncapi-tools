package io.ballerina.asyncapi.core.model;

import java.util.Map;

public record AsyncApiCorrelationId(
        String description,
        String location,
        Map<String, String> extensions
) {
}
