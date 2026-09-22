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
package io.ballerina.asyncapi.generator.http.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds a Ballerina string-template expression from an {@code x-ballerina-auth} {@code input} DSL
 * expression (e.g. {@code "$method . $config('callbackUrl') . $body"}), given a {@link TokenResolver}
 * that supplies the Ballerina expression text each token category should evaluate to.
 *
 * <p>Shared by server-side signature verification codegen and client-side test-signing codegen,
 * which parse the exact same DSL but differ only in what each token resolves to -- e.g. {@code $body}
 * is {@code check request.getTextPayload()} when verifying an incoming request, but a local fixture
 * variable when a generated test signs an outgoing one.
 */
public final class SignaturePayloadTemplateBuilder {

    private static final Pattern HEADER_FUNC_PATTERN = Pattern.compile("\\$header\\('([^']+)'\\)");
    private static final Pattern CONFIG_FUNC_PATTERN = Pattern.compile("\\$config\\('([^']+)'\\)");
    private static final Pattern CUSTOM_VAR_PATTERN = Pattern.compile("\\$([A-Za-z_][A-Za-z0-9_]*)");
    private static final Pattern BRACED_VAR_PATTERN = Pattern.compile("(?<!\\$)\\{([A-Za-z_][A-Za-z0-9_]*)\\}");
    private static final String SECRET_TOKEN = "$secret";

    private SignaturePayloadTemplateBuilder() {
    }

    /**
     * Resolves each DSL token category to the Ballerina expression text it should evaluate to in the
     * caller's context.
     */
    public interface TokenResolver {
        String body();

        String uri();

        String method();

        String secret();

        String header(String headerName);

        String config(String configName);
    }

    /**
     * Builds the interior of a Ballerina backtick string template (without the surrounding
     * backticks) from an {@code input} DSL expression.
     *
     * @param inputDsl the raw {@code x-ballerina-auth.signature.input} DSL expression
     * @param resolver supplies the Ballerina expression for each token category
     * @return the template body, ready to be wrapped as {@code string `<result>`}
     */
    public static String build(String inputDsl, TokenResolver resolver) {
        String normalizedDsl = convertBracedVariables(inputDsl)
                .replace("${body}", "$body")
                .replace("${uri}", "$uri")
                .replace("${method}", "$method")
                .replaceAll("\\$\\{header\\('([^']+)'\\)\\}", "\\$header('$1')");

        if (normalizedDsl.contains(" . ")) {
            return buildFromDotExpression(normalizedDsl, resolver);
        }
        return buildFromInterpolatedExpression(normalizedDsl, resolver);
    }

    private static String buildFromInterpolatedExpression(String normalizedDsl, TokenResolver resolver) {
        String template = normalizedDsl;
        template = replaceBareToken(template, "$body", "${" + resolver.body() + "}");
        template = replaceBareToken(template, "$uri", "${" + resolver.uri() + "}");
        template = replaceBareToken(template, "$method", "${" + resolver.method() + "}");

        Matcher headerMatcher = HEADER_FUNC_PATTERN.matcher(template);
        StringBuffer headerReplaced = new StringBuffer();
        while (headerMatcher.find()) {
            String replacement = "${" + resolver.header(headerMatcher.group(1)) + "}";
            headerMatcher.appendReplacement(headerReplaced, Matcher.quoteReplacement(replacement));
        }
        headerMatcher.appendTail(headerReplaced);

        Matcher configMatcher = CONFIG_FUNC_PATTERN.matcher(headerReplaced.toString());
        StringBuffer configReplaced = new StringBuffer();
        while (configMatcher.find()) {
            String replacement = "${" + resolver.config(configMatcher.group(1)) + "}";
            configMatcher.appendReplacement(configReplaced, Matcher.quoteReplacement(replacement));
        }
        configMatcher.appendTail(configReplaced);

        String secretReplaced = configReplaced.toString()
                .replaceAll("\\$secret\\b", Matcher.quoteReplacement("${" + resolver.secret() + "}"));

        Matcher customVarMatcher = CUSTOM_VAR_PATTERN.matcher(secretReplaced);
        StringBuffer customReplaced = new StringBuffer();
        while (customVarMatcher.find()) {
            String replacement = "${" + customVarMatcher.group(1) + "}";
            customVarMatcher.appendReplacement(customReplaced, Matcher.quoteReplacement(replacement));
        }
        customVarMatcher.appendTail(customReplaced);
        return customReplaced.toString();
    }

    private static String buildFromDotExpression(String expression, TokenResolver resolver) {
        String[] segments = expression.split("\\s+\\.\\s+");
        StringBuilder templateBuilder = new StringBuilder();

        for (String segment : segments) {
            String token = segment.trim();
            if (token.length() >= 2 && token.startsWith("'") && token.endsWith("'")) {
                templateBuilder.append(token, 1, token.length() - 1);
            } else {
                templateBuilder.append(mapConcatTokenToInterpolation(token, resolver));
            }
        }
        return templateBuilder.toString();
    }

    private static String mapConcatTokenToInterpolation(String token, TokenResolver resolver) {
        if ("$body".equals(token)) {
            return "${" + resolver.body() + "}";
        }
        if ("$uri".equals(token)) {
            return "${" + resolver.uri() + "}";
        }
        if ("$method".equals(token)) {
            return "${" + resolver.method() + "}";
        }
        if (SECRET_TOKEN.equals(token)) {
            return "${" + resolver.secret() + "}";
        }

        Matcher configMatcher = CONFIG_FUNC_PATTERN.matcher(token);
        if (configMatcher.matches()) {
            return "${" + resolver.config(configMatcher.group(1)) + "}";
        }

        Matcher headerMatcher = HEADER_FUNC_PATTERN.matcher(token);
        if (headerMatcher.matches()) {
            return "${" + resolver.header(headerMatcher.group(1)) + "}";
        }

        if (token.startsWith("${") && token.endsWith("}")) {
            return token;
        }
        if (token.startsWith("$") && token.length() > 1) {
            return "${" + token.substring(1) + "}";
        }
        return token;
    }

    /**
     * Extracts the distinct {@code $header('Name')} references from an {@code input} DSL expression,
     * in first-occurrence order.
     */
    public static java.util.List<String> extractHeaderReferences(String inputDsl) {
        java.util.List<String> headers = new java.util.ArrayList<>();
        Matcher matcher = HEADER_FUNC_PATTERN.matcher(inputDsl);
        while (matcher.find()) {
            String name = matcher.group(1);
            if (!headers.contains(name)) {
                headers.add(name);
            }
        }
        return headers;
    }

    private static String convertBracedVariables(String text) {
        Matcher bracedVarMatcher = BRACED_VAR_PATTERN.matcher(text);
        StringBuffer bracedResult = new StringBuffer();
        while (bracedVarMatcher.find()) {
            String replacement = "${" + bracedVarMatcher.group(1) + "}";
            bracedVarMatcher.appendReplacement(bracedResult, Matcher.quoteReplacement(replacement));
        }
        bracedVarMatcher.appendTail(bracedResult);
        return bracedResult.toString();
    }

    /**
     * Replaces a bare reserved token (e.g. {@code "$body"}) with the given replacement, but only when
     * the token is not immediately followed by another identifier character -- so a DSL variable that
     * merely starts with a reserved name (e.g. {@code $bodyHash}) is left untouched instead of having
     * its {@code $body} prefix corrupted by a plain substring replace.
     */
    private static String replaceBareToken(String text, String token, String replacement) {
        Pattern tokenPattern = Pattern.compile(Pattern.quote(token) + "(?![A-Za-z0-9_])");
        return tokenPattern.matcher(text).replaceAll(Matcher.quoteReplacement(replacement));
    }
}
