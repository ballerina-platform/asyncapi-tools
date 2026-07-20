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
package io.ballerina.asyncapi.generator.http.utils;

import io.ballerina.asyncapi.generator.GeneratorException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tokenizes an {@code x-ballerina-auth} {@code headerFormat} string into its literal and
 * placeholder ({@code $name}, {@code ${name}}, or {@code {name}}) segments.
 *
 * <p>Shared by {@link io.ballerina.asyncapi.generator.http.validator.WebhookDslValidator} and
 * {@link io.ballerina.asyncapi.generator.http.node.GenerateVerifyWebhookSignatureFuncNode} so both
 * validation and code generation agree on exactly the same tokenization -- a config that passes
 * validation is guaranteed to be parsed identically at generation time.
 */
public final class HeaderTemplateParser {

    private static final Pattern TEMPLATE_VAR_PATTERN = Pattern.compile(
            "\\$\\{([A-Za-z_][A-Za-z0-9_]*)\\}|\\$([A-Za-z_][A-Za-z0-9_]*)|\\{([A-Za-z_][A-Za-z0-9_]*)\\}");

    private HeaderTemplateParser() {
    }

    /**
     * Parses a {@code headerFormat} string into its literal/placeholder segments.
     *
     * @param headerFormat the raw {@code headerFormat} value from the DSL, or {@code null}/blank
     *                      to use the default of a single bare {@code signature} placeholder
     * @return the parsed {@link HeaderTemplate}
     * @throws GeneratorException if {@code headerFormat} is non-blank but contains no recognized
     *                             placeholder syntax -- almost always a typo (e.g. a missing
     *                             {@code $}/{@code {}}), since a plain literal header value can
     *                             never actually match a real HMAC signature
     */
    public static HeaderTemplate parse(String headerFormat) throws GeneratorException {
        String template = headerFormat == null || headerFormat.isBlank() ? "$signature" : headerFormat;
        Matcher matcher = TEMPLATE_VAR_PATTERN.matcher(template);

        List<String> literals = new ArrayList<>();
        List<String> variables = new ArrayList<>();
        List<int[]> spans = new ArrayList<>();
        int currentIndex = 0;

        while (matcher.find()) {
            literals.add(template.substring(currentIndex, matcher.start()));
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
        literals.add(template.substring(currentIndex));

        if (variables.isEmpty()) {
            throw new GeneratorException(String.format(
                    "Invalid headerFormat: '%s' does not contain a recognized placeholder "
                            + "($signature, ${signature}, or {signature}).",
                    headerFormat));
        }

        return new HeaderTemplate(literals, variables, spans);
    }

    /**
     * A parsed {@code headerFormat}: the literal text between placeholders, the placeholder
     * variable names in order, and each placeholder's {@code [start, end)} character span in the
     * original string (used to detect adjacent placeholders with no literal separator).
     *
     * @param literals  the literal text segments, always one more than {@code variables}
     * @param variables the placeholder variable names, in order of appearance
     * @param spans     each placeholder's {@code [start, end)} span in the original string
     */
    public record HeaderTemplate(List<String> literals, List<String> variables, List<int[]> spans) {
    }
}
