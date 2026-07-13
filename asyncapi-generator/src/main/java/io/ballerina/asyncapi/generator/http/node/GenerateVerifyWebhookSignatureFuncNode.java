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
package io.ballerina.asyncapi.generator.http.node;

import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.http.model.WebhookAuthConfig;
import io.ballerina.asyncapi.generator.http.utils.HeaderTemplateParser;
import io.ballerina.asyncapi.generator.http.utils.HeaderTemplateParser.HeaderTemplate;
import io.ballerina.compiler.syntax.tree.FunctionBodyBlockNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.compiler.syntax.tree.ReturnTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.StatementNode;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createSeparatedNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBuiltinSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionBodyBlockNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionDefinitionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionSignatureNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createQualifiedNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRequiredParameterNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createOptionalTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createReturnTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ERROR_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.COLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.COMMA_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FUNCTION_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OBJECT_METHOD_DEFINITION;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PRIVATE_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.QUESTION_MARK_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RETURNS_KEYWORD;

/**
 * Generates the {@code private function verifyWebhookSignature(http:Request request, string webhookSecret)
 * returns http:Response|error} method node for the {@code DispatcherService} class in
 * {@code dispatcher_service.bal}.
 *
 * <p>Validates the HMAC signature carried in the configured auth header, supporting dynamic
 * payload extraction and formatting based on the AsyncAPI x-ballerina-auth DSL.
 */
public class GenerateVerifyWebhookSignatureFuncNode implements Generator {

    public static final String VERIFY_WEBHOOK_SIGNATURE_FUNC = "verifyWebhookSignature";
    private static final Pattern HEADER_FUNC_PATTERN = Pattern.compile("\\$header\\('([^']+)'\\)");
    private static final Pattern CONFIG_FUNC_PATTERN = Pattern.compile("\\$config\\('([^']+)'\\)");
    private static final Pattern CUSTOM_VAR_PATTERN = Pattern.compile("\\$([A-Za-z_][A-Za-z0-9_]*)");
    private static final Pattern BRACED_VAR_PATTERN =
            Pattern.compile("(?<!\\$)\\{([A-Za-z_][A-Za-z0-9_]*)\\}");
    private static final String SECRET_TOKEN = "$secret";

    private final WebhookAuthConfig authConfig;

    /**
     * Creates a generator for the {@code verifyWebhookSignature} function.
     *
     * @param authConfig the webhook authentication configuration extracted from the DSL
     */
    public GenerateVerifyWebhookSignatureFuncNode(WebhookAuthConfig authConfig) {
        this.authConfig = authConfig;
    }

    @Override
    public FunctionDefinitionNode generate() throws GeneratorException {
        FunctionSignatureNode signature = createFunctionSignatureNode(
                createToken(OPEN_PAREN_TOKEN),
                createSeparatedNodeList(
                        createRequiredParameterNode(
                                createEmptyNodeList(),
                                createQualifiedNameReferenceNode(
                                        createIdentifierToken(GenerateHttpImportNode.HTTP_MODULE),
                                        createToken(COLON_TOKEN),
                                        createIdentifierToken("Request")),
                                createIdentifierToken("request")),
                        createToken(COMMA_TOKEN),
                        createRequiredParameterNode(
                                createEmptyNodeList(),
                                createBuiltinSimpleNameReferenceNode(null, createIdentifierToken("string")),
                                createIdentifierToken("webhookSecret"))),
                createToken(CLOSE_PAREN_TOKEN),
                buildOptionalErrorReturnType());

        List<StatementNode> statements = new ArrayList<>();
        String headerName = authConfig.headerName();

        if (authConfig.freshnessHeader() != null) {
            addFreshnessCheckStatements(statements, authConfig.freshnessHeader(),
                    authConfig.freshnessToleranceMillis());
        }

        statements.add(NodeParser.parseStatement(String.format(
                "if !request.hasHeader(\"%s\") { return error(\"Unauthorized: Missing Signature Header\"); }",
                headerName)));
        String receivedHeaderExpr = getSafeHeaderExtraction(headerName);
        statements.add(NodeParser.parseStatement(String.format(
            "string receivedHeader = %s;",
            receivedHeaderExpr)));

        String headerFormat = authConfig.headerFormat() != null ? authConfig.headerFormat() : "$signature";
        HeaderTemplate headerTemplate = HeaderTemplateParser.parse(headerFormat);
        addHeaderTemplateExtractionStatements(statements, headerTemplate);

        String signatureVariable = resolveSignatureVariableName(headerTemplate);
        statements.add(NodeParser.parseStatement(String.format(
            "if !extractedHeaderValues.hasKey(\"%s\") {"
                + " return error(\"Unauthorized: Missing Signature Value\"); }",
            signatureVariable)));
        String extractedSignatureExpr = getSafeMapExtraction("extractedHeaderValues", signatureVariable, "");
        statements.add(NodeParser.parseStatement(String.format(
            "string extractedSignature = %s;",
            extractedSignatureExpr)));

        // If algorithm is present, use HMAC verification. Otherwise do static token verification.
        if (hasAlgorithmConfigured(authConfig.algorithm())) {

            // 1. Translate the DSL input string to Ballerina string interpolation
            String inputDsl = authConfig.input() != null ? authConfig.input() : "$body";
            String balTemplate = buildPayloadTemplate(inputDsl);
                    
            statements.add(NodeParser.parseStatement(
                    "string payloadToHash = string `" + balTemplate + "`;"));

            // 2. Map the algorithm to the correct Ballerina crypto module function. "hmac" (default, or
            // absent) uses a secret-keyed HMAC; "hash" uses a plain, unkeyed digest — the secret must
            // then be folded into `input` explicitly via $secret (e.g. HubSpot v1/v2-style schemes).
            boolean isPlainHash = "hash".equalsIgnoreCase(authConfig.strategy());
            String algo = authConfig.algorithm().toLowerCase();
            String cryptoFunc;
            String computeStatement;
            if (isPlainHash) {
                cryptoFunc = switch (algo) {
                    case "sha1" -> "hashSha1";
                    case "sha384" -> "hashSha384";
                    case "sha512" -> "hashSha512";
                    default -> "hashSha256";
                };
                computeStatement = String.format(
                        "byte[] computedDigest = crypto:%s(payloadToHash.toBytes());", cryptoFunc);
            } else {
                cryptoFunc = switch (algo) {
                    case "sha1" -> "hmacSha1";
                    case "sha384" -> "hmacSha384";
                    case "sha512" -> "hmacSha512";
                    default -> "hmacSha256";
                };
                computeStatement = String.format(
                        "byte[] computedDigest = check crypto:%s(payloadToHash.toBytes(), webhookSecret.toBytes());",
                        cryptoFunc);
            }
            statements.add(NodeParser.parseStatement(computeStatement));

            // 3. Apply the requested encoding (hex or base64)
            String encoding = authConfig.encoding() != null ? authConfig.encoding().toLowerCase() : "hex";
            String encodeFunc = encoding.equals("base64") ? "toBase64()" : "toBase16()";

            // Note: Shopify/QuickBooks use base64, Slack/GitHub use hex.
            statements.add(NodeParser.parseStatement(
                    String.format(
                            "string computedSignature = computedDigest.%s;",
                            encodeFunc)));

            // 4. Construct the final expected header string using the DSL format
            String expectedHeaderTemplate = headerFormat
                    .replace("${signature}", "${computedSignature}")
                    .replace("{signature}", "${computedSignature}")
                    .replace("$signature", "${computedSignature}");
                expectedHeaderTemplate = normalizeCustomVariables(expectedHeaderTemplate);
            
            statements.add(NodeParser.parseStatement(
                    "string expectedHeader = string `" + expectedHeaderTemplate + "`;"));

            statements.add(NodeParser.parseStatement(
                    "if !crypto:equalConstantTime(receivedHeader.toBytes(), expectedHeader.toBytes()) {"
                            + " return error(\"Unauthorized: Signature Mismatch\"); }"));

        } else {
            statements.add(NodeParser.parseStatement(
                    "if !crypto:equalConstantTime(extractedSignature.toBytes(), webhookSecret.toBytes()) {"
                            + " return error(\"Unauthorized: Signature Mismatch\"); }"));
        }
        
        statements.add(NodeParser.parseStatement("log:printInfo(\"SIGNATURE_VERIFIED\");"));
        statements.add(NodeParser.parseStatement("return;"));

        FunctionBodyBlockNode body = createFunctionBodyBlockNode(
                createToken(OPEN_BRACE_TOKEN), null, createNodeList(statements),
                createToken(CLOSE_BRACE_TOKEN), null);

        return createFunctionDefinitionNode(
                OBJECT_METHOD_DEFINITION, null,
                createNodeList(createToken(PRIVATE_KEYWORD)),
                createToken(FUNCTION_KEYWORD),
                createIdentifierToken(VERIFY_WEBHOOK_SIGNATURE_FUNC),
                createEmptyNodeList(),
                signature, body);
    }

    /**
     * Emits a request-timestamp staleness check, independent of and preceding signature verification.
     * Reads the freshness header, parses it as an epoch-millisecond {@code decimal}, and rejects the
     * request if it's older than the configured tolerance.
     *
     * @param statements       the statement list to append to
     * @param freshnessHeader  the header carrying the request timestamp (epoch milliseconds)
     * @param toleranceMillis  the maximum allowed request age, in milliseconds
     */
    private void addFreshnessCheckStatements(List<StatementNode> statements, String freshnessHeader,
            long toleranceMillis) {
        statements.add(NodeParser.parseStatement(String.format(
                "if !request.hasHeader(\"%s\") { return error(\"Unauthorized: Missing Freshness Header\"); }",
                freshnessHeader)));
        statements.add(NodeParser.parseStatement(String.format(
                "string freshnessHeaderValue = %s;",
                getSafeHeaderExtraction(freshnessHeader))));
        statements.add(NodeParser.parseStatement(
                "decimal freshnessTimestamp = check decimal:fromString(freshnessHeaderValue);"));
        statements.add(NodeParser.parseStatement(
                "decimal freshnessNowMillis = <decimal>time:utcNow()[0] * 1000;"));
        statements.add(NodeParser.parseStatement(String.format(
                "if (freshnessNowMillis - freshnessTimestamp) > <decimal>%d {"
                        + " return error(\"Unauthorized: Request Timestamp Expired\"); }",
                toleranceMillis)));
    }

    private ReturnTypeDescriptorNode buildOptionalErrorReturnType() {
        return createReturnTypeDescriptorNode(
                createToken(RETURNS_KEYWORD),
                createEmptyNodeList(),
                createOptionalTypeDescriptorNode(
                        createToken(ERROR_KEYWORD),
                        createToken(QUESTION_MARK_TOKEN)));
    }

    private String buildPayloadTemplate(String inputDsl) {
        String normalizedDsl = convertBracedVariables(inputDsl)
                .replace("${body}", "$body")
                .replace("${uri}", "$uri")
                .replace("${method}", "$method")
                .replaceAll("\\$\\{header\\('([^']+)'\\)\\}", "\\$header('$1')");

        if (normalizedDsl.contains(" . ")) {
            return buildTemplateFromDotExpression(normalizedDsl);
        }

        String template = normalizedDsl
                .replace("$body", "${check request.getTextPayload()}")
                .replace("$uri", "${request.rawPath}")
                .replace("$method", "${request.method}");

        Matcher headerMatcher = HEADER_FUNC_PATTERN.matcher(template);
        StringBuffer headerReplaced = new StringBuffer();
        while (headerMatcher.find()) {
            String replacement = "${" + getSafeHeaderExtraction(headerMatcher.group(1)) + "}";
            headerMatcher.appendReplacement(headerReplaced, Matcher.quoteReplacement(replacement));
        }
        headerMatcher.appendTail(headerReplaced);

        Matcher configMatcher = CONFIG_FUNC_PATTERN.matcher(headerReplaced.toString());
        StringBuffer configReplaced = new StringBuffer();
        while (configMatcher.find()) {
            String replacement = "${self." + configMatcher.group(1) + "}";
            configMatcher.appendReplacement(configReplaced, Matcher.quoteReplacement(replacement));
        }
        configMatcher.appendTail(configReplaced);

        String secretReplaced = configReplaced.toString()
                .replaceAll("\\$secret\\b", Matcher.quoteReplacement("${webhookSecret}"));

        Matcher customVarMatcher = CUSTOM_VAR_PATTERN.matcher(secretReplaced);
        StringBuffer customReplaced = new StringBuffer();
        while (customVarMatcher.find()) {
            String replacement = "${" + customVarMatcher.group(1) + "}";
            customVarMatcher.appendReplacement(customReplaced, Matcher.quoteReplacement(replacement));
        }
        customVarMatcher.appendTail(customReplaced);
        return customReplaced.toString();
    }

    private void addHeaderTemplateExtractionStatements(List<StatementNode> statements, HeaderTemplate template) {
        statements.add(NodeParser.parseStatement("map<string> extractedHeaderValues = {};"));
        statements.add(NodeParser.parseStatement("int headerCursor = 0;"));

        for (int i = 0; i < template.variables().size(); i++) {
            String variableName = template.variables().get(i);
            String prefix = template.literals().get(i);

            if (!prefix.isEmpty()) {
                String escapedPrefix = escapeForBallerinaString(prefix);
                statements.add(NodeParser.parseStatement(String.format(
                        "if !receivedHeader.substring(headerCursor).startsWith(\"%s\") {"
                                + " return error(\"Unauthorized: Malformed Signature Header\"); }",
                        escapedPrefix)));
                statements.add(NodeParser.parseStatement(String.format(
                        "headerCursor += %d;",
                        prefix.length())));
            }

            boolean isLastVariable = i == template.variables().size() - 1;
            String nextLiteral = template.literals().get(i + 1);
            if (!isLastVariable) {
                if (nextLiteral.isEmpty()) {
                    statements.add(NodeParser.parseStatement(
                            "return error(\"Unauthorized: Ambiguous Header Template\");"));
                    continue;
                }
                String markerVar = variableName + "Marker";
                String safeIndexExpr = getSafeIndexOf("receivedHeader", nextLiteral, "headerCursor");
                statements.add(NodeParser.parseStatement(String.format(
                    "int %s = %s;",
                        markerVar,
                        safeIndexExpr)));
                statements.add(NodeParser.parseStatement(String.format(
                        "if %s < 0 { return error(\"Unauthorized: Malformed Signature Header\"); }",
                        markerVar)));
                statements.add(NodeParser.parseStatement(String.format(
                        "extractedHeaderValues[\"%s\"] = receivedHeader.substring(headerCursor, %s);",
                        variableName,
                        markerVar)));
                statements.add(NodeParser.parseStatement(String.format(
                        "headerCursor = %s;",
                        markerVar)));
            } else {
                if (nextLiteral.isEmpty()) {
                    statements.add(NodeParser.parseStatement(String.format(
                            "extractedHeaderValues[\"%s\"] = receivedHeader.substring(headerCursor);",
                            variableName)));
                    statements.add(NodeParser.parseStatement("headerCursor = receivedHeader.length();"));
                } else {
                    String escapedSuffix = escapeForBallerinaString(nextLiteral);
                    statements.add(NodeParser.parseStatement(String.format(
                            "if !receivedHeader.substring(headerCursor).endsWith(\"%s\") {"
                                    + " return error(\"Unauthorized: Malformed Signature Header\"); }",
                            escapedSuffix)));
                    statements.add(NodeParser.parseStatement(String.format(
                            "int %sEnd = receivedHeader.length() - %d;",
                            variableName,
                            nextLiteral.length())));
                    statements.add(NodeParser.parseStatement(String.format(
                            "if %sEnd < headerCursor { return error(\"Unauthorized: Malformed Signature Header\"); }",
                            variableName)));
                    statements.add(NodeParser.parseStatement(String.format(
                            "extractedHeaderValues[\"%s\"] = receivedHeader.substring(headerCursor, %sEnd);",
                            variableName,
                            variableName)));
                    statements.add(NodeParser.parseStatement("headerCursor = receivedHeader.length();"));
                }
            }
        }

            List<String> declaredVariables = new ArrayList<>();
            for (String variableName : template.variables()) {
                if (declaredVariables.contains(variableName)) {
                continue;
                }
                declaredVariables.add(variableName);
                statements.add(NodeParser.parseStatement(String.format(
                    "if !extractedHeaderValues.hasKey(\"%s\") {"
                        + " return error(\"Unauthorized: Missing Header Component: %s\"); }",
                    variableName,
                    variableName)));
                String safeMapExtraction = getSafeMapExtraction("extractedHeaderValues", variableName, "");
                statements.add(NodeParser.parseStatement(String.format(
                    "string %s = %s;",
                    variableName,
                    safeMapExtraction)));
            }
    }

    private String resolveSignatureVariableName(HeaderTemplate template) {
        if (template.variables().contains("signature")) {
            return "signature";
        }
        return template.variables().isEmpty() ? "signature" : template.variables().get(0);
    }

    private boolean hasAlgorithmConfigured(String algorithm) {
        return algorithm != null && !algorithm.isBlank();
    }

    private String escapeForBallerinaString(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String getSafeMapExtraction(String mapName, String key, String defaultValue) {
        String escapedKey = escapeForBallerinaString(key);
        String escapedDefaultValue = escapeForBallerinaString(defaultValue);
        return String.format("%s[\"%s\"] ?: \"%s\"", mapName, escapedKey, escapedDefaultValue);
    }

    private String getSafeHeaderExtraction(String headerName) {
        String escapedHeaderName = escapeForBallerinaString(headerName);
        return String.format("let var headerValue = trap request.getHeader(\"%s\") in "
                + "(headerValue is string ? headerValue : \"\")", escapedHeaderName);
    }

    private String getSafeIndexOf(String targetVariable, String searchString, String startIndex) {
        String escapedSearchString = escapeForBallerinaString(searchString);
        return String.format("%s.indexOf(\"%s\", %s) ?: -1", targetVariable, escapedSearchString, startIndex);
    }

    private String normalizeCustomVariables(String text) {
        String bracedResult = convertBracedVariables(text);

        Matcher customVarMatcher = CUSTOM_VAR_PATTERN.matcher(bracedResult);
        StringBuffer customResult = new StringBuffer();
        while (customVarMatcher.find()) {
            String replacement = "${" + customVarMatcher.group(1) + "}";
            customVarMatcher.appendReplacement(customResult, Matcher.quoteReplacement(replacement));
        }
        customVarMatcher.appendTail(customResult);
        return customResult.toString();
    }

    private String convertBracedVariables(String text) {
        Matcher bracedVarMatcher = BRACED_VAR_PATTERN.matcher(text);
        StringBuffer bracedResult = new StringBuffer();
        while (bracedVarMatcher.find()) {
            String replacement = "${" + bracedVarMatcher.group(1) + "}";
            bracedVarMatcher.appendReplacement(bracedResult, Matcher.quoteReplacement(replacement));
        }
        bracedVarMatcher.appendTail(bracedResult);
        return bracedResult.toString();
    }

    private String buildTemplateFromDotExpression(String expression) {
        String[] segments = expression.split("\\s+\\.\\s+");
        StringBuilder templateBuilder = new StringBuilder();

        for (String segment : segments) {
            String token = segment.trim();
            if (token.length() >= 2 && token.startsWith("'") && token.endsWith("'")) {
                templateBuilder.append(token, 1, token.length() - 1);
            } else {
                templateBuilder.append(mapConcatTokenToInterpolation(token));
            }
        }
        return templateBuilder.toString();
    }

    private String mapConcatTokenToInterpolation(String token) {
        // $method and $uri are mapped explicitly to request context values.
        if ("$body".equals(token)) {
            return "${check request.getTextPayload()}";
        }
        if ("$uri".equals(token)) {
            return "${request.rawPath}";
        }
        if ("$method".equals(token)) {
            return "${request.method}";
        }
        if (SECRET_TOKEN.equals(token)) {
            return "${webhookSecret}";
        }

        Matcher configMatcher = CONFIG_FUNC_PATTERN.matcher(token);
        if (configMatcher.matches()) {
            return "${self." + configMatcher.group(1) + "}";
        }

        Matcher headerMatcher = HEADER_FUNC_PATTERN.matcher(token);
        if (headerMatcher.matches()) {
            return "${" + getSafeHeaderExtraction(headerMatcher.group(1)) + "}";
        }

        if (token.startsWith("${") && token.endsWith("}")) {
            return token;
        }
        if (token.startsWith("$") && token.length() > 1) {
            return "${" + token.substring(1) + "}";
        }
        return token;
    }
}
