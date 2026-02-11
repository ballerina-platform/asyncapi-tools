package io.ballerina.asyncapi.core.model.info;

import com.fasterxml.jackson.databind.JsonNode;
import io.ballerina.asyncapi.core.model.doc.AsyncApiExternalDocs;
import io.ballerina.asyncapi.core.model.tag.AsyncApiTag;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Represents the Info Object in an AsyncAPI document.
 * Provides metadata about the API.
 *
 * @param title          The title of the application.
 * @param version        The version of the application API.
 * @param description    A description of the application.
 * @param termsOfService A URI to the Terms of Service for the API.
 * @param contact        Contact information for the API.
 * @param license        License information for the API.
 * @param tags           A list of tags for API documentation control.
 * @param externalDocs   Additional external documentation.
 * @param extensions     Specification extensions (fields prefixed with "x-").
 */
public record AsyncApiInfo(
        String title,
        String version,
        String description,
        URI termsOfService,
        AsyncApiContact contact,
        AsyncApiLicense license,
        List<AsyncApiTag> tags,
        AsyncApiExternalDocs externalDocs,
        Map<String, JsonNode> extensions
) {
}
