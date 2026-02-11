package io.ballerina.asyncapi.core.model.doc;

import com.fasterxml.jackson.databind.JsonNode;

import java.net.URI;
import java.util.Map;

/**
 * Represents an External Documentation Object in an AsyncAPI document.
 *
 * @param description A description of the target documentation.
 * @param url         The URI for the target documentation.
 * @param extensions  Specification extensions (fields prefixed with "x-").
 */
public record AsyncApiExternalDocs(
        String description,
        URI url,
        Map<String, JsonNode> extensions
) {
}
