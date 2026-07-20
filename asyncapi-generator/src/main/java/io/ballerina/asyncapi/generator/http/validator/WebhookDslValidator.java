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
package io.ballerina.asyncapi.generator.http.validator;

import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.http.model.WebhookAuthConfig;
import io.ballerina.asyncapi.generator.http.utils.HeaderTemplateParser;
import io.ballerina.asyncapi.generator.http.utils.HeaderTemplateParser.HeaderTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Performs pre-generation validation for the webhook verification DSL.
 */
public final class WebhookDslValidator {

    private static final Set<String> SUPPORTED_ALGORITHMS = Set.of("sha1", "sha256", "sha384", "sha512");
    private static final Set<String> SUPPORTED_ENCODINGS = Set.of("hex", "base64");

    private static final Pattern HEADER_FUNC_PATTERN = Pattern.compile("\\$header\\('([^']+)'\\)");
    private static final Pattern CONFIG_FUNC_PATTERN = Pattern.compile("\\$config\\('([^']+)'\\)");
    private static final Pattern DOLLAR_TOKEN_PATTERN = Pattern.compile("\\$[A-Za-z_][A-Za-z0-9_]*");
    private static final Pattern BRACED_TOKEN_PATTERN =
            Pattern.compile("(?<!\\$)\\{([A-Za-z_][A-Za-z0-9_]*)\\}");
    private static final Pattern DOLLAR_BRACED_TOKEN_PATTERN =
            Pattern.compile("\\$\\{([A-Za-z_][A-Za-z0-9_]*)\\}");

    // "secret" allows $secret to fold the webhook secret directly into the hashed payload for
    // strategy: "hash" (plain-digest) schemes, instead of using it as an HMAC key.
    private static final Set<String> BUILTIN_INPUT_TOKENS = Set.of("body", "method", "uri", "secret");

    private WebhookDslValidator() {
    }

    /**
     * Validates the webhook DSL and throws on invalid configurations.
     *
     * @param config webhook authentication config extracted from x-ballerina-auth
     * @throws GeneratorException when the DSL violates validation rules
     */
    public static void validate(WebhookAuthConfig config) throws GeneratorException {
        if (config == null) {
            return;
        }

        String headerFormat = config.headerFormat() == null || config.headerFormat().isBlank()
                ? "$signature"
                : config.headerFormat();

        HeaderTemplate template = HeaderTemplateParser.parse(headerFormat);

        validateNoAdjacentPlaceholders(headerFormat, template);
        validateSignaturePresence(config.algorithm(), template.variables());
        validateAlgorithmAndEncoding(config.algorithm(), config.encoding());
        validatePayloadInput(config.input(), template.variables());
    }

    private static void validateNoAdjacentPlaceholders(String headerFormat, HeaderTemplate template)
            throws GeneratorException {
        List<int[]> spans = template.spans();
        for (int i = 1; i < spans.size(); i++) {
            int[] previous = spans.get(i - 1);
            int[] current = spans.get(i);
            if (previous[1] == current[0]) {
                throw new GeneratorException("Invalid headerFormat: adjacent placeholders are not allowed: "
                        + headerFormat);
            }
        }
    }

    private static void validateSignaturePresence(String algorithm, List<String> extractedVariables)
            throws GeneratorException {
        if (algorithm == null || algorithm.isBlank()) {
            return;
        }

        if (!extractedVariables.contains("signature")) {
            throw new GeneratorException("Invalid headerFormat: when algorithm is configured, "
                    + "headerFormat must include $signature/{signature}.");
        }
    }

    /**
     * Validates that {@code algorithm} and {@code encoding} are values the generator actually
     * supports. {@link io.ballerina.asyncapi.generator.http.node.GenerateVerifyWebhookSignatureFuncNode}
     * silently falls back to {@code hmacSha256}/hex for any unrecognized value, so a typo here
     * (e.g. {@code "sha-256"}) would otherwise compile fine but produce a verifier that never
     * matches the real provider's signature, with no diagnostic pointing at the misconfiguration.
     *
     * @param algorithm the configured signature algorithm, or {@code null}/blank for static-token
     *                   verification (not validated in that case)
     * @param encoding   the configured signature encoding, or {@code null}/blank to default to hex
     * @throws GeneratorException when either value is set but not one of the supported values
     */
    private static void validateAlgorithmAndEncoding(String algorithm, String encoding)
            throws GeneratorException {
        if (algorithm == null || algorithm.isBlank()) {
            return;
        }

        String normalizedAlgorithm = algorithm.toLowerCase(Locale.ROOT);
        if (!SUPPORTED_ALGORITHMS.contains(normalizedAlgorithm)) {
            throw new GeneratorException(String.format(
                    "Unsupported x-ballerina-auth signature algorithm: '%s'. Supported values: %s.",
                    algorithm, String.join(", ", SUPPORTED_ALGORITHMS)));
        }

        if (encoding == null || encoding.isBlank()) {
            return;
        }

        String normalizedEncoding = encoding.toLowerCase(Locale.ROOT);
        if (!SUPPORTED_ENCODINGS.contains(normalizedEncoding)) {
            throw new GeneratorException(String.format(
                    "Unsupported x-ballerina-auth signature encoding: '%s'. Supported values: %s.",
                    encoding, String.join(", ", SUPPORTED_ENCODINGS)));
        }
    }

    private static void validatePayloadInput(String input, List<String> extractedVariables)
            throws GeneratorException {
        if (input == null || input.isBlank()) {
            return;
        }

        // Remove valid header/config lookups first so their '$header'/'$config' token doesn't
        // appear as an invalid bare '$header'/'$config'.
        Matcher headerMatcher = HEADER_FUNC_PATTERN.matcher(input);
        String withoutHeaderFuncs = headerMatcher.replaceAll(" ");
        Matcher configMatcher = CONFIG_FUNC_PATTERN.matcher(withoutHeaderFuncs);
        withoutHeaderFuncs = configMatcher.replaceAll(" ");

        Set<String> allowedCustomVariables = new HashSet<>(extractedVariables);

        Matcher tokenMatcher = DOLLAR_TOKEN_PATTERN.matcher(withoutHeaderFuncs);
        while (tokenMatcher.find()) {
            String token = tokenMatcher.group().substring(1);
            if (BUILTIN_INPUT_TOKENS.contains(token)) {
                continue;
            }
            if (allowedCustomVariables.contains(token)) {
                continue;
            }
            throw new GeneratorException("Invalid input token in webhook DSL: $" + token);
        }

        Matcher bracedTokenMatcher = BRACED_TOKEN_PATTERN.matcher(withoutHeaderFuncs);
        while (bracedTokenMatcher.find()) {
            String token = bracedTokenMatcher.group(1);
            if (BUILTIN_INPUT_TOKENS.contains(token)) {
                continue;
            }
            if (allowedCustomVariables.contains(token)) {
                continue;
            }
            throw new GeneratorException("Invalid input token in webhook DSL: {" + token + "}");
        }

        Matcher dollarBracedTokenMatcher = DOLLAR_BRACED_TOKEN_PATTERN.matcher(withoutHeaderFuncs);
        while (dollarBracedTokenMatcher.find()) {
            String token = dollarBracedTokenMatcher.group(1);
            if (BUILTIN_INPUT_TOKENS.contains(token)) {
                continue;
            }
            if (allowedCustomVariables.contains(token)) {
                continue;
            }
            throw new GeneratorException("Invalid input token in webhook DSL: ${" + token + "}");
        }
    }
}
