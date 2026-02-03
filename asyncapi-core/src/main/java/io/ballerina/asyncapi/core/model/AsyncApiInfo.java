package io.ballerina.asyncapi.core.model;

import java.util.List;
import java.util.Map;

public record AsyncApiInfo(
        String title,
        String version,
        String description,
        String termsOfService,
        Contact contact,
        License license,
        List<AsyncApiTag> tags,
        AsyncApiExternalDocs externalDocs,
        Map<String, Object> extensions
) {

    public record Contact(
            String name,
            String url,
            String email,
            Map<String, Object> extensions
    ) {
    }

    public record License(
            String name,
            String url,
            Map<String, Object> extensions
    ) {
    }
}
