/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com)
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package io.ballerina.asyncapi.generator.http.extractor;

import com.fasterxml.jackson.databind.JsonNode;
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.http.model.EventIdentifierConfig;
import io.ballerina.asyncapi.generator.http.utils.CodegenUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Extracts the event identifier configuration from the {@code x-ballerina-event-identifier}
 * extension on an {@link AsyncApiSpec} document.
 */
public final class EventIdentifierExtractor {

    public static final String X_BALLERINA_EVENT_TYPE_HEADER = "header";
    public static final String X_BALLERINA_EVENT_TYPE_BODY = "body";
    public static final String X_BALLERINA_EVENT_TYPE_COMPOSITE = "composite";
    private static final String X_BALLERINA_EVENT_FIELD_IDENTIFIER = "x-ballerina-event-identifier";
    private static final String X_BALLERINA_EVENT_FIELD_IDENTIFIER_TYPE = "type";
    private static final String X_BALLERINA_EVENT_FIELD_IDENTIFIER_PATH = "path";
    private static final String X_BALLERINA_EVENT_FIELD_IDENTIFIER_NAME = "name";
    private static final String X_BALLERINA_EVENT_FIELD_IDENTIFIER_BATCHED = "batched";

    private final AsyncApiSpec asyncApiSpec;

    public EventIdentifierExtractor(AsyncApiSpec asyncApiSpec) {
        this.asyncApiSpec = asyncApiSpec;
    }

    /**
     * Extracts the event identifier configuration from the spec.
     *
     * @return the resolved {@link EventIdentifierConfig}
     * @throws GeneratorException if {@code x-ballerina-event-identifier} is absent, has an
     *                            invalid type, or is missing required fields
     */
    public EventIdentifierConfig extract() throws GeneratorException {
        Map<String, JsonNode> extensions = asyncApiSpec.getAsyncApiExtensions().orElse(null);
        if (extensions == null || !extensions.containsKey(X_BALLERINA_EVENT_FIELD_IDENTIFIER)) {
            throw new GeneratorException(String.format(
                    "%s attribute is not found in the Async API Specification",
                    X_BALLERINA_EVENT_FIELD_IDENTIFIER));
        }

        JsonNode identifierNode = extensions.get(X_BALLERINA_EVENT_FIELD_IDENTIFIER);
        if (identifierNode == null || !identifierNode.isObject()) {
            throw new GeneratorException(String.format(
                    "%s must be a JSON object in the Async API Specification",
                    X_BALLERINA_EVENT_FIELD_IDENTIFIER));
        }
        Map<String, String> identifierFields = new HashMap<>();
        for (Map.Entry<String, JsonNode> entry : identifierNode.properties()) {
            identifierFields.put(entry.getKey(), entry.getValue().asText());
        }

        if (!identifierFields.containsKey(X_BALLERINA_EVENT_FIELD_IDENTIFIER_TYPE)) {
            throw new GeneratorException(String.format(
                    "%s attribute is not found within the attribute %s in the Async API Specification",
                    X_BALLERINA_EVENT_FIELD_IDENTIFIER_TYPE,
                    X_BALLERINA_EVENT_FIELD_IDENTIFIER));
        }

        String type = identifierFields.get(X_BALLERINA_EVENT_FIELD_IDENTIFIER_TYPE);
        boolean batched = Boolean.parseBoolean(
                identifierFields.getOrDefault(X_BALLERINA_EVENT_FIELD_IDENTIFIER_BATCHED, "false"));

        return switch (type) {
            case X_BALLERINA_EVENT_TYPE_HEADER ->
                    new EventIdentifierConfig(type, extractHeaderName(identifierFields), null, batched);
            case X_BALLERINA_EVENT_TYPE_BODY ->
                    new EventIdentifierConfig(type, null, extractBodyPath(identifierFields), batched);
            case X_BALLERINA_EVENT_TYPE_COMPOSITE ->
                    new EventIdentifierConfig(type, extractHeaderName(identifierFields),
                            extractBodyPath(identifierFields), batched);
            default -> throw new GeneratorException(String.format(
                    "%s, %s or %s is not provided as the value of %s attribute within the attribute %s"
                            + " in the Async API Specification",
                    X_BALLERINA_EVENT_TYPE_HEADER, X_BALLERINA_EVENT_TYPE_BODY,
                    X_BALLERINA_EVENT_TYPE_COMPOSITE,
                    X_BALLERINA_EVENT_FIELD_IDENTIFIER_TYPE,
                    X_BALLERINA_EVENT_FIELD_IDENTIFIER));
        };
    }

    /**
     * Extracts and escapes the header name for a {@code type=header} or {@code type=composite} identifier.
     *
     * @param identifierFields the fields parsed from the extension node
     * @return the (possibly keyword-escaped) header name
     * @throws GeneratorException if the {@code name} field is absent
     */
    private String extractHeaderName(Map<String, String> identifierFields) throws GeneratorException {
        if (!identifierFields.containsKey(X_BALLERINA_EVENT_FIELD_IDENTIFIER_NAME)) {
            throw new GeneratorException(String.format(
                    "%s attribute is not found within the attribute %s in the Async API Specification",
                    X_BALLERINA_EVENT_FIELD_IDENTIFIER_NAME,
                    X_BALLERINA_EVENT_FIELD_IDENTIFIER));
        }
        String name = identifierFields.get(X_BALLERINA_EVENT_FIELD_IDENTIFIER_NAME);
        if (CodegenUtils.BAL_KEYWORDS.stream().anyMatch(name::equals)) {
            return String.format("'%s", name);
        }
        return name;
    }

    /**
     * Extracts and escapes the dot-notation path for a {@code type=body} identifier.
     * Each path segment that matches a Ballerina keyword is prefixed with {@code '}.
     *
     * @param identifierFields the fields parsed from the extension node
     * @return the (possibly keyword-escaped) dot-notation path
     * @throws GeneratorException if the {@code path} field is absent
     */
    private String extractBodyPath(Map<String, String> identifierFields) throws GeneratorException {
        if (!identifierFields.containsKey(X_BALLERINA_EVENT_FIELD_IDENTIFIER_PATH)) {
            throw new GeneratorException(String.format(
                    "%s attribute is not found within the attribute %s in the Async API Specification",
                    X_BALLERINA_EVENT_FIELD_IDENTIFIER_PATH,
                    X_BALLERINA_EVENT_FIELD_IDENTIFIER));
        }
        String identifierPath = identifierFields.get(X_BALLERINA_EVENT_FIELD_IDENTIFIER_PATH);
        return Arrays.stream(identifierPath.split("\\."))
                .map(part -> CodegenUtils.BAL_KEYWORDS.stream().anyMatch(part::equals)
                        ? String.format("'%s", part) : part)
                .collect(Collectors.joining("."));
    }
}
