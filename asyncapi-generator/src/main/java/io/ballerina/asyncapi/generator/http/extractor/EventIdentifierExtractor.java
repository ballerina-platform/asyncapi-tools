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
import io.ballerina.asyncapi.generator.http.Constants;
import io.ballerina.asyncapi.generator.http.model.EventIdentifierConfig;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Extracts the event identifier configuration from the {@code x-ballerina-event-identifier}
 * extension on an {@link AsyncApiSpec} document.
 */
public final class EventIdentifierExtractor {

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
        if (extensions == null || !extensions.containsKey(Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER)) {
            throw new GeneratorException(Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER
                    + " attribute is not found in the Async API Specification");
        }

        JsonNode identifierNode = extensions.get(Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER);
        if (identifierNode == null || !identifierNode.isObject()) {
            throw new GeneratorException(Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER
                    + " must be a JSON object in the Async API Specification");
        }
        Map<String, String> identifierFields = new HashMap<>();
        for (Map.Entry<String, JsonNode> entry : identifierNode.properties()) {
            identifierFields.put(entry.getKey(), entry.getValue().asText());
        }

        if (!identifierFields.containsKey(Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER_TYPE)) {
            throw new GeneratorException(Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER_TYPE
                    + " attribute is not found within the attribute "
                    + Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER
                    + " in the Async API Specification");
        }

        String type = identifierFields.get(Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER_TYPE);

        return switch (type) {
            case Constants.X_BALLERINA_EVENT_TYPE_HEADER ->
                    new EventIdentifierConfig(type, extractHeaderPath(identifierFields));
            case Constants.X_BALLERINA_EVENT_TYPE_BODY ->
                    new EventIdentifierConfig(type, extractBodyPath(identifierFields));
            default -> throw new GeneratorException(Constants.X_BALLERINA_EVENT_TYPE_HEADER + " or "
                    + Constants.X_BALLERINA_EVENT_TYPE_BODY + " is not provided as the value of "
                    + Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER_TYPE + " attribute within the attribute "
                    + Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER + " in the Async API Specification");
        };
    }

    /**
     * Extracts and escapes the header name for a {@code type=header} identifier.
     *
     * @param identifierFields the fields parsed from the extension node
     * @return the (possibly keyword-escaped) header name
     * @throws GeneratorException if the {@code name} field is absent
     */
    private String extractHeaderPath(Map<String, String> identifierFields) throws GeneratorException {
        if (!identifierFields.containsKey(Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER_NAME)) {
            throw new GeneratorException(Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER_NAME
                    + " attribute is not found within the attribute "
                    + Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER
                    + " in the Async API Specification");
        }
        String name = identifierFields.get(Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER_NAME);
        if (Constants.BAL_KEYWORDS.stream().anyMatch(name::equals)) {
            return "'" + name;
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
        if (!identifierFields.containsKey(Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER_PATH)) {
            throw new GeneratorException(Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER_PATH
                    + " attribute is not found within the attribute "
                    + Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER
                    + " in the Async API Specification");
        }
        String identifierPath = identifierFields.get(Constants.X_BALLERINA_EVENT_FIELD_IDENTIFIER_PATH);
        return Arrays.stream(identifierPath.split("\\."))
                .map(part -> Constants.BAL_KEYWORDS.stream().anyMatch(part::equals) ? "'" + part : part)
                .collect(Collectors.joining("."));
    }
}
