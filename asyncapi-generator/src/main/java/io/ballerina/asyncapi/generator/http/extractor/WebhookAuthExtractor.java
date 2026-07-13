/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com)
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.ballerina.asyncapi.generator.http.extractor;

import com.fasterxml.jackson.databind.JsonNode;
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.http.model.WebhookAuthConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts the webhook authentication configuration from the {@code x-ballerina-auth}
 * extension on an {@link AsyncApiSpec} document.
 */
public final class WebhookAuthExtractor {

    private static final String X_BALLERINA_AUTH = "x-ballerina-auth";
    private static final String X_BALLERINA_AUTH_HEADER = "header";
    private static final String X_BALLERINA_AUTH_SIGNATURE = "signature";
    private static final String X_BALLERINA_AUTH_FRESHNESS = "freshness";

    // New DSL Keys
    private static final String SIGNATURE_ALGORITHM = "algorithm";
    private static final String SIGNATURE_ENCODING = "encoding";
    private static final String SIGNATURE_HEADER_FORMAT = "headerFormat";
    private static final String SIGNATURE_INPUT = "input";
    private static final String SIGNATURE_STRATEGY = "strategy";

    // Freshness DSL keys
    private static final String FRESHNESS_HEADER = "header";
    private static final String FRESHNESS_TOLERANCE_MILLIS = "toleranceMillis";

    private static final Pattern CONFIG_FUNC_PATTERN = Pattern.compile("\\$config\\('([^']+)'\\)");

    private final AsyncApiSpec asyncApiSpec;

    public WebhookAuthExtractor(AsyncApiSpec asyncApiSpec) {
        this.asyncApiSpec = asyncApiSpec;
    }

    /**
     * Extracts the webhook auth configuration from the spec.
     *
     * @return an {@link Optional} containing the {@link WebhookAuthConfig} if the
     * {@code x-ballerina-auth} extension is present and has a {@code header} field;
     * {@link Optional#empty()} otherwise
     * @throws GeneratorException if a {@code signature} block is present but is not a JSON object
     */
    public Optional<WebhookAuthConfig> extract() throws GeneratorException {
        Map<String, JsonNode> extensions = asyncApiSpec.getAsyncApiExtensions().orElse(null);
        if (extensions == null || !extensions.containsKey(X_BALLERINA_AUTH)) {
            return Optional.empty();
        }

        JsonNode authNode = extensions.get(X_BALLERINA_AUTH);
        if (authNode == null || !authNode.isObject()) {
            return Optional.empty();
        }

        JsonNode headerNode = authNode.get(X_BALLERINA_AUTH_HEADER);
        if (headerNode == null || !headerNode.isTextual()) {
            return Optional.empty();
        }

        String header = headerNode.asText();
        WebhookFreshnessConfig freshness = extractFreshness(authNode);

        // Extract the new nested signature properties
        JsonNode signatureNode = authNode.get(X_BALLERINA_AUTH_SIGNATURE);
        if (signatureNode != null) {
            if (!signatureNode.isObject()) {
                throw new GeneratorException(String.format(
                        "Invalid %s.%s: expected an object with algorithm/encoding/headerFormat/input "
                                + "fields, but found a %s",
                        X_BALLERINA_AUTH, X_BALLERINA_AUTH_SIGNATURE, signatureNode.getNodeType()));
            }
            String algorithm = extractTextNode(signatureNode, SIGNATURE_ALGORITHM);
            String encoding = extractTextNode(signatureNode, SIGNATURE_ENCODING);
            String headerFormat = extractTextNode(signatureNode, SIGNATURE_HEADER_FORMAT);
            String input = extractTextNode(signatureNode, SIGNATURE_INPUT);
            String strategy = extractTextNode(signatureNode, SIGNATURE_STRATEGY);
            List<String> configFields = extractConfigFields(input);

            return Optional.of(new WebhookAuthConfig(header, algorithm, encoding, headerFormat, input,
                    strategy, configFields, freshness.header(), freshness.toleranceMillis()));
        }

        // No signature block at all (as opposed to a malformed one, handled above): fallback for
        // simple/legacy configs that only define a header. Per the DSL backwards-compatibility
        // guarantee, default to GitHub's HMAC-SHA256 configuration so existing pipelines keep
        // working unchanged.
        return Optional.of(new WebhookAuthConfig(header, "sha256", "hex", "{signature}", "$body",
                null, List.of(), freshness.header(), freshness.toleranceMillis()));
    }

    /**
     * Extracts the optional {@code x-ballerina-auth.freshness} block, which declares a request-timestamp
     * staleness check independent of signature verification.
     *
     * @param authNode the parsed {@code x-ballerina-auth} extension node
     * @return the resolved freshness config (both fields {@code null} if the block is absent)
     * @throws GeneratorException if the block is present but malformed or incomplete
     */
    private WebhookFreshnessConfig extractFreshness(JsonNode authNode) throws GeneratorException {
        JsonNode freshnessNode = authNode.get(X_BALLERINA_AUTH_FRESHNESS);
        if (freshnessNode == null) {
            return new WebhookFreshnessConfig(null, null);
        }
        if (!freshnessNode.isObject()) {
            throw new GeneratorException(String.format(
                    "Invalid %s.%s: expected an object with header/toleranceMillis fields, but found a %s",
                    X_BALLERINA_AUTH, X_BALLERINA_AUTH_FRESHNESS, freshnessNode.getNodeType()));
        }
        JsonNode headerNode = freshnessNode.get(FRESHNESS_HEADER);
        JsonNode toleranceNode = freshnessNode.get(FRESHNESS_TOLERANCE_MILLIS);
        if (headerNode == null || !headerNode.isTextual() || toleranceNode == null
                || !toleranceNode.isNumber()) {
            throw new GeneratorException(String.format(
                    "Invalid %s.%s: both '%s' (string) and '%s' (number) are required",
                    X_BALLERINA_AUTH, X_BALLERINA_AUTH_FRESHNESS, FRESHNESS_HEADER, FRESHNESS_TOLERANCE_MILLIS));
        }
        return new WebhookFreshnessConfig(headerNode.asText(), toleranceNode.asLong());
    }

    /**
     * Scans a DSL {@code input} expression for distinct {@code $config('name')} references, in order of
     * first appearance. Each one becomes a configurable field threaded through the generated listener.
     *
     * @param input the DSL payload-composition expression, may be {@code null}
     * @return the distinct config field names referenced, or an empty list if none/{@code input} is null
     */
    private List<String> extractConfigFields(String input) {
        if (input == null) {
            return List.of();
        }
        List<String> configFields = new ArrayList<>();
        Matcher matcher = CONFIG_FUNC_PATTERN.matcher(input);
        while (matcher.find()) {
            String name = matcher.group(1);
            if (!configFields.contains(name)) {
                configFields.add(name);
            }
        }
        return configFields;
    }

    /**
     * Holds the resolved {@code freshness} block, or a pair of {@code null}s if not configured.
     *
     * @param header          the header carrying the request timestamp
     * @param toleranceMillis the maximum allowed request age, in milliseconds
     */
    private record WebhookFreshnessConfig(String header, Long toleranceMillis) {
    }

    private String extractTextNode(JsonNode parentNode, String fieldName) {
        JsonNode childNode = parentNode.get(fieldName);
        return (childNode != null && childNode.isTextual()) ? childNode.asText() : null;
    }
}
