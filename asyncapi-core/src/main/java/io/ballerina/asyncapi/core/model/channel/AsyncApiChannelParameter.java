package io.ballerina.asyncapi.core.model.channel;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * Represents a Channel Parameter Object for address template substitution.
 *
 * @param description  A description of the parameter.
 * @param defaultValue The default value to use for substitution.
 * @param enumValues   An enumeration of allowed string values.
 * @param examples     Example values for the parameter.
 * @param extensions   Specification extensions (fields prefixed with "x-").
 */
public record AsyncApiChannelParameter(
        String description,
        String defaultValue,
        List<String> enumValues,
        List<String> examples,
        Map<String, JsonNode> extensions
) {
}
