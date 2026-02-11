package io.ballerina.asyncapi.core.model.message;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * Represents an example of a message.
 *
 * @param name       A machine-friendly name for the example.
 * @param summary    A short summary of the example.
 * @param headers    Example of the message headers as a map.
 * @param payload    Example of the message payload.
 * @param extensions Specification extensions (fields prefixed with "x-").
 */
public record AsyncApiMessageExample(
        String name,
        String summary,
        Map<String, Object> headers,
        Object payload,
        Map<String, JsonNode> extensions
) {
}
