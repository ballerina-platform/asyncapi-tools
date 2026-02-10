package io.ballerina.asyncapi.core.model;

import com.fasterxml.jackson.databind.JsonNode;

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
        Contact contact,
        License license,
        List<AsyncApiTag> tags,
        AsyncApiExternalDocs externalDocs,
        Map<String, JsonNode> extensions
) {

    /**
     * Represents contact information for the API.
     *
     * @param name       The identifying name of the contact person or organization.
     * @param url        The URI pointing to the contact information.
     * @param email      The email address of the contact person or organization.
     * @param extensions Specification extensions (fields prefixed with "x-").
     */
    public record Contact(
            String name,
            URI url,
            String email,
            Map<String, JsonNode> extensions
    ) {
    }

    /**
     * Represents license information for the API.
     *
     * @param name       The license name used for the API.
     * @param url        A URI to the license used for the API.
     * @param extensions Specification extensions (fields prefixed with "x-").
     */
    public record License(
            String name,
            URI url,
            Map<String, JsonNode> extensions
    ) {
    }
}
