package io.ballerina.asyncapi.core.model.info;

import com.fasterxml.jackson.databind.JsonNode;

import java.net.URI;
import java.util.Map;

/**
 * Represents license information for the API.
 *
 * @param name       The license name used for the API.
 * @param url        A URI to the license used for the API.
 * @param extensions Specification extensions (fields prefixed with "x-").
 */
public record AsyncApiLicense(
        String name,
        URI url,
        Map<String, JsonNode> extensions
) {
}
