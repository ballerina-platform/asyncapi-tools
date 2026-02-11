package io.ballerina.asyncapi.core.model.info;

import com.fasterxml.jackson.databind.JsonNode;

import java.net.URI;
import java.util.Map;

/**
 * Represents contact information for the API.
 *
 * @param name       The identifying name of the contact person or organization.
 * @param url        The URI pointing to the contact information.
 * @param email      The email address of the contact person or organization.
 * @param extensions Specification extensions (fields prefixed with "x-").
 */
public record AsyncApiContact(
        String name,
        URI url,
        String email,
        Map<String, JsonNode> extensions
) {
}
