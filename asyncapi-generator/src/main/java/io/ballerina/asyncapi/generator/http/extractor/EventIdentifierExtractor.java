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
import io.ballerina.asyncapi.generator.http.model.HandshakeConfig;
import io.ballerina.asyncapi.generator.http.utils.CodegenUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
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
    public static final String X_BALLERINA_EVENT_TYPE_BODY_KEY = "body-key";
    private static final String X_BALLERINA_EVENT_FIELD_IDENTIFIER = "x-ballerina-event-identifier";
    private static final String X_BALLERINA_EVENT_FIELD_IDENTIFIER_TYPE = "type";
    private static final String X_BALLERINA_EVENT_FIELD_IDENTIFIER_PATH = "path";
    private static final String X_BALLERINA_EVENT_FIELD_IDENTIFIER_NAME = "name";
    private static final String X_BALLERINA_EVENT_FIELD_IDENTIFIER_HANDSHAKE = "handshake";
    private static final String HANDSHAKE_METHOD = "method";
    private static final String HANDSHAKE_WHEN = "when";
    private static final String HANDSHAKE_WHEN_EQUALS = "equals";
    private static final String HANDSHAKE_CHALLENGE = "challenge";
    private static final String HANDSHAKE_CHALLENGE_IN = "in";
    private static final String HANDSHAKE_IN_BODY = "body";
    private static final String HANDSHAKE_IN_QUERY = "query";
    private static final String HANDSHAKE_FIELD =
            X_BALLERINA_EVENT_FIELD_IDENTIFIER + "." + X_BALLERINA_EVENT_FIELD_IDENTIFIER_HANDSHAKE;

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
        HandshakeConfig handshake = extractHandshake(identifierNode.get(X_BALLERINA_EVENT_FIELD_IDENTIFIER_HANDSHAKE));

        return switch (type) {
            case X_BALLERINA_EVENT_TYPE_HEADER ->
                    new EventIdentifierConfig(type, extractHeaderName(identifierFields), null, handshake);
            case X_BALLERINA_EVENT_TYPE_BODY ->
                    new EventIdentifierConfig(type, null, extractBodyPath(identifierFields), handshake);
            case X_BALLERINA_EVENT_TYPE_BODY_KEY ->
                    new EventIdentifierConfig(type, null, extractBodyPath(identifierFields), handshake);
            case X_BALLERINA_EVENT_TYPE_COMPOSITE ->
                    new EventIdentifierConfig(type, extractHeaderName(identifierFields),
                            extractBodyPath(identifierFields), handshake);
            default -> throw new GeneratorException(String.format(
                    "%s, %s, %s or %s is not provided as the value of %s attribute within the attribute %s"
                            + " in the Async API Specification",
                    X_BALLERINA_EVENT_TYPE_HEADER, X_BALLERINA_EVENT_TYPE_BODY,
                    X_BALLERINA_EVENT_TYPE_BODY_KEY, X_BALLERINA_EVENT_TYPE_COMPOSITE,
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
     * Extracts and escapes the dot-notation path for a {@code type=body}, {@code body-key} or {@code composite}
     * identifier.
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
        return escapeBodyPath(identifierFields.get(X_BALLERINA_EVENT_FIELD_IDENTIFIER_PATH));
    }

    /**
     * Escapes a dot-notation body path for use as a Ballerina field access: in each segment, a character that
     * cannot appear in an identifier is escaped with {@code \}, and a segment that is a keyword or starts with a
     * digit is prefixed with {@code '}.
     *
     * @param path the dot-notation path, e.g. {@code event.type} or {@code event-type}
     * @return the escaped path, e.g. {@code event.'type} or {@code event\-type}
     */
    public static String escapeBodyPath(String path) {
        return Arrays.stream(path.split("\\."))
                .map(EventIdentifierExtractor::escapePathSegment)
                .collect(Collectors.joining("."));
    }

    private static String escapePathSegment(String segment) {
        String escaped = segment.replaceAll("([^A-Za-z0-9_])", "\\\\$1");
        boolean quoted = CodegenUtils.BAL_KEYWORDS.contains(segment)
                || (!segment.isEmpty() && Character.isDigit(segment.charAt(0)));
        return quoted ? "'" + escaped : escaped;
    }

    /**
     * Reads the optional {@code handshake} block: {@code POST} needs {@code when.path}, {@code when.equals} and
     * {@code challenge: {in: body, path}}; {@code GET} needs {@code challenge: {in: query, name}}.
     *
     * @param node the {@code handshake} node, or {@code null} if absent
     * @return the handshake with its paths unescaped, or {@code null} if there is none
     * @throws GeneratorException if the block is incomplete or mixes the two shapes
     */
    private static HandshakeConfig extractHandshake(JsonNode node) throws GeneratorException {
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.isObject()) {
            throw new GeneratorException(String.format("%s must be an object", HANDSHAKE_FIELD));
        }
        String method = requiredText(node, HANDSHAKE_METHOD, HANDSHAKE_FIELD).toUpperCase(Locale.ROOT);
        if (!HandshakeConfig.METHOD_GET.equals(method) && !HandshakeConfig.METHOD_POST.equals(method)) {
            throw new GeneratorException(String.format(
                    "%s.%s must be GET or POST, but found '%s'", HANDSHAKE_FIELD, HANDSHAKE_METHOD, method));
        }
        JsonNode challenge = requiredObject(node, HANDSHAKE_CHALLENGE, HANDSHAKE_FIELD);
        String challengeField = HANDSHAKE_FIELD + "." + HANDSHAKE_CHALLENGE;
        String in = requiredText(challenge, HANDSHAKE_CHALLENGE_IN, challengeField);
        JsonNode when = node.get(HANDSHAKE_WHEN);

        if (HandshakeConfig.METHOD_GET.equals(method)) {
            if (!HANDSHAKE_IN_QUERY.equals(in)) {
                throw new GeneratorException(String.format(
                        "%s.%s must be '%s' for a GET handshake", challengeField, HANDSHAKE_CHALLENGE_IN,
                        HANDSHAKE_IN_QUERY));
            }
            if (when != null) {
                throw new GeneratorException(String.format(
                        "%s.%s is only used with a POST handshake", HANDSHAKE_FIELD, HANDSHAKE_WHEN));
            }
            return new HandshakeConfig(method, null, null,
                    requiredText(challenge, X_BALLERINA_EVENT_FIELD_IDENTIFIER_NAME, challengeField));
        }
        if (!HANDSHAKE_IN_BODY.equals(in)) {
            throw new GeneratorException(String.format(
                    "%s.%s must be '%s' for a POST handshake", challengeField, HANDSHAKE_CHALLENGE_IN,
                    HANDSHAKE_IN_BODY));
        }
        if (when == null) {
            throw new GeneratorException(String.format(
                    "A POST handshake needs %s.%s to tell it apart from events", HANDSHAKE_FIELD, HANDSHAKE_WHEN));
        }
        String whenField = HANDSHAKE_FIELD + "." + HANDSHAKE_WHEN;
        if (!when.isObject()) {
            throw new GeneratorException(String.format("%s must be an object", whenField));
        }
        return new HandshakeConfig(method,
                requiredText(when, X_BALLERINA_EVENT_FIELD_IDENTIFIER_PATH, whenField),
                requiredText(when, HANDSHAKE_WHEN_EQUALS, whenField),
                requiredText(challenge, X_BALLERINA_EVENT_FIELD_IDENTIFIER_PATH, challengeField));
    }

    private static String requiredText(JsonNode node, String key, String parent) throws GeneratorException {
        JsonNode value = node.get(key);
        if (value == null || !value.isValueNode() || value.asText().isBlank()) {
            throw new GeneratorException(String.format("%s.%s is required", parent, key));
        }
        return value.asText().trim();
    }

    private static JsonNode requiredObject(JsonNode node, String key, String parent) throws GeneratorException {
        JsonNode value = node.get(key);
        if (value == null || !value.isObject()) {
            throw new GeneratorException(String.format("%s.%s is required and must be an object", parent, key));
        }
        return value;
    }
}
