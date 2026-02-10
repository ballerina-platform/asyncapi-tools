package io.ballerina.asyncapi.core.model;

import java.net.URI;
import java.util.List;
import java.util.Map;

public record AsyncApiInfo(
        String title,
        String version,
        String description,
        URI termsOfService,
        Contact contact,
        License license,
        List<AsyncApiTag> tags,
        AsyncApiExternalDocs externalDocs,
        Map<String, String> extensions
) {

    public record Contact(
            String name,
            URI url,
            String email,
            Map<String, String> extensions
    ) {
    }

    public record License(
            String name,
            URI url,
            Map<String, String> extensions
    ) {
    }
}
