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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Performs pre-generation validation for the webhook verification DSL.
 */
public final class WebhookDslValidator {

    private static final Pattern TEMPLATE_VAR_PATTERN = Pattern.compile(
            "\\$\\{([A-Za-z_][A-Za-z0-9_]*)\\}|\\$([A-Za-z_][A-Za-z0-9_]*)|\\{([A-Za-z_][A-Za-z0-9_]*)\\}");
    private static final Pattern HEADER_FUNC_PATTERN = Pattern.compile("\\$header\\('([^']+)'\\)");
    private static final Pattern DOLLAR_TOKEN_PATTERN = Pattern.compile("\\$[A-Za-z_][A-Za-z0-9_]*");
    private static final Pattern BRACED_TOKEN_PATTERN =
            Pattern.compile("(?<!\\$)\\{([A-Za-z_][A-Za-z0-9_]*)\\}");

    private static final Set<String> BUILTIN_INPUT_TOKENS = Set.of("body", "method", "uri");

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

        HeaderTemplate template = parseHeaderTemplate(headerFormat);

        validateNoAdjacentPlaceholders(headerFormat, template);
        validateSignaturePresence(config.algorithm(), template.variables());
        validatePayloadInput(config.input(), template.variables());
    }

    private static HeaderTemplate parseHeaderTemplate(String headerFormat) {
        Matcher matcher = TEMPLATE_VAR_PATTERN.matcher(headerFormat);

        List<String> literals = new ArrayList<>();
        List<String> variables = new ArrayList<>();
        List<int[]> spans = new ArrayList<>();
        int currentIndex = 0;

        while (matcher.find()) {
            literals.add(headerFormat.substring(currentIndex, matcher.start()));
            String variable = matcher.group(1);
            if (variable == null) {
                variable = matcher.group(2);
            }
            if (variable == null) {
                variable = matcher.group(3);
            }
            variables.add(variable);
            spans.add(new int[]{matcher.start(), matcher.end()});
            currentIndex = matcher.end();
        }
        literals.add(headerFormat.substring(currentIndex));

        if (variables.isEmpty()) {
            literals = List.of("", "");
            variables = List.of("signature");
            spans = List.of(new int[]{0, 0});
        }

        return new HeaderTemplate(literals, variables, spans);
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

    private static void validatePayloadInput(String input, List<String> extractedVariables)
            throws GeneratorException {
        if (input == null || input.isBlank()) {
            return;
        }

        // Remove valid header lookups first so their '$header' token does not appear as invalid '$header'.
        Matcher headerMatcher = HEADER_FUNC_PATTERN.matcher(input);
        String withoutHeaderFuncs = headerMatcher.replaceAll(" ");

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
            if (allowedCustomVariables.contains(token)) {
                continue;
            }
            throw new GeneratorException("Invalid input token in webhook DSL: {" + token + "}");
        }
    }

    private record HeaderTemplate(List<String> literals, List<String> variables, List<int[]> spans) {
    }
}
