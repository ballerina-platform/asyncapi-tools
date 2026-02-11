package io.ballerina.asyncapi.core.model.operation;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * Represents the address for an operation reply.
 *
 * @param location    A runtime expression that specifies the location of the reply address.
 * @param description A description of the reply address.
 * @param extensions  Specification extensions (fields prefixed with "x-").
 */
public record AsyncApiOperationReplyAddress(
        String location,
        String description,
        Map<String, JsonNode> extensions
) {
}
