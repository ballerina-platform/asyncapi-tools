package io.ballerina.asyncapi.core.model.server;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * Represents a Server Variable Object for URL template substitution.
 *
 * @param name         The name of the variable.
 * @param description  A description of the variable.
 * @param defaultValue The default value to use for substitution.
 * @param enumValues   An enumeration of allowed string values.
 * @param examples     Example values for the variable.
 * @param extensions   Specification extensions (fields prefixed with "x-").
 */
public record AsyncApiServerVariable(
        String name,
        String description,
        String defaultValue,
        List<String> enumValues,
        List<String> examples,
        Map<String, JsonNode> extensions
) {
}
