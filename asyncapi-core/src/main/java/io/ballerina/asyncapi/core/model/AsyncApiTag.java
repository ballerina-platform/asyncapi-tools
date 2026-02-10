package io.ballerina.asyncapi.core.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * Represents a Tag Object in an AsyncAPI document.
 *
 * @param name         The name of the tag.
 * @param description  A description for the tag.
 * @param externalDocs Additional external documentation for the tag.
 * @param extensions   Specification extensions (fields prefixed with "x-").
 */
public record AsyncApiTag(
        String name,
        String description,
        AsyncApiExternalDocs externalDocs,
        Map<String, JsonNode> extensions
) {
}
