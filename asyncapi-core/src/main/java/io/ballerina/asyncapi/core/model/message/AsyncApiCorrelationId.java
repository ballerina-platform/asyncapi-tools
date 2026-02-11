package io.ballerina.asyncapi.core.model.message;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * Represents a Correlation ID Object in an AsyncAPI document.
 *
 * @param description A description of the identifier.
 * @param location    A runtime expression that specifies the location of the correlation ID.
 * @param extensions  Specification extensions (fields prefixed with "x-").
 */
public record AsyncApiCorrelationId(
        String description,
        String location,
        Map<String, JsonNode> extensions
) {
}
