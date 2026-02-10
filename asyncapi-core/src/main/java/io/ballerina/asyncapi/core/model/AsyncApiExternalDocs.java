package io.ballerina.asyncapi.core.model;

import java.net.URI;
import java.util.Map;

public record AsyncApiExternalDocs(
        String description,
        URI url,
        Map<String, String> extensions
) {
}
