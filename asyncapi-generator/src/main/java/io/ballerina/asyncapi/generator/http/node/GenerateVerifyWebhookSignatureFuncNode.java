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
import io.ballerina.asyncapi.generator.http.utils.SignaturePayloadTemplateBuilder;
import io.ballerina.asyncapi.generator.http.utils.WebhookCryptoMapper;
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
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ISOLATED_KEYWORD;
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
    private static final Pattern CUSTOM_VAR_PATTERN = Pattern.compile("\\$([A-Za-z_][A-Za-z0-9_]*)");
    private static final Pattern BRACED_VAR_PATTERN =
            Pattern.compile("(?<!\\$)\\{([A-Za-z_][A-Za-z0-9_]*)\\}");

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
        boolean useAlgorithm = hasAlgorithmConfigured(authConfig.algorithm());
        // Extracts every named component of the header template (e.g. Stripe's "t=$timestamp,v1=$signature"
        // yields both "timestamp" and "signature" as real variables) -- needed in both branches below,
        // since the algorithm branch's own `input` DSL can reference any of these components too, not
        // just the ones used for hashing. The signature-role component itself is never referenced by
        // the algorithm branch (it compares receivedHeader/expectedHeader instead), so its declaration
        // is skipped there to avoid an always-unused variable.
        addHeaderTemplateExtractionStatements(statements, headerTemplate,
                useAlgorithm ? resolveSignatureVariableName(headerTemplate) : null);

        // If algorithm is present, use HMAC verification. Otherwise do static token verification.
        if (useAlgorithm) {

            // 1. Translate the DSL input string to Ballerina string interpolation
            String inputDsl = authConfig.input() != null ? authConfig.input() : "$body";
            String balTemplate = buildPayloadTemplate(escapeBacktickTemplate(inputDsl));
                    
            statements.add(NodeParser.parseStatement(
                    "string payloadToHash = string `" + balTemplate + "`;"));

            // 2. Map the algorithm to the correct Ballerina crypto module function. "hmac" (default, or
            // absent) uses a secret-keyed HMAC; "hash" uses a plain, unkeyed digest — the secret must
            // then be folded into `input` explicitly via $secret (e.g. HubSpot v1/v2-style schemes).
            boolean isPlainHash = "hash".equalsIgnoreCase(authConfig.strategy());
            String computeStatement;
            if (isPlainHash) {
                String cryptoFunc = WebhookCryptoMapper.hashFunctionFor(authConfig.algorithm());
                computeStatement = String.format(
                        "byte[] computedDigest = crypto:%s(payloadToHash.toBytes());", cryptoFunc);
            } else {
                String cryptoFunc = WebhookCryptoMapper.hmacFunctionFor(authConfig.algorithm());
                computeStatement = String.format(
                        "byte[] computedDigest = check crypto:%s(payloadToHash.toBytes(), webhookSecret.toBytes());",
                        cryptoFunc);
            }
            statements.add(NodeParser.parseStatement(computeStatement));

            // 3. Apply the requested encoding (hex or base64). Note: Shopify/QuickBooks use base64,
            // Slack/GitHub use hex.
            String encoding = authConfig.encoding() != null ? authConfig.encoding() : "hex";
            String encodeFunc = WebhookCryptoMapper.encodeFunctionFor(encoding);
            statements.add(NodeParser.parseStatement(
                    String.format(
                            "string computedSignature = computedDigest.%s;",
                            encodeFunc)));

            // 4. Construct the final expected header string using the DSL format
            String expectedHeaderTemplate = escapeBacktickTemplate(headerFormat)
                    .replace("${signature}", "${computedSignature}")
                    .replace("{signature}", "${computedSignature}");
            expectedHeaderTemplate = replaceBareToken(expectedHeaderTemplate, "$signature", "${computedSignature}");
            expectedHeaderTemplate = normalizeCustomVariables(expectedHeaderTemplate);
            
            statements.add(NodeParser.parseStatement(
                    "string expectedHeader = string `" + expectedHeaderTemplate + "`;"));

            statements.add(NodeParser.parseStatement(
                    "if !crypto:equalConstantTime(receivedHeader.toBytes(), expectedHeader.toBytes()) {"
                            + " return error(\"Unauthorized: Signature Mismatch\"); }"));

        } else {
            // Static token mode: compare the signature-role component already declared above by
            // addHeaderTemplateExtractionStatements directly against the configured secret, with no
            // hashing involved.
            String signatureVariable = resolveSignatureVariableName(headerTemplate);
            statements.add(NodeParser.parseStatement(String.format(
                    "if !crypto:equalConstantTime(%s.toBytes(), webhookSecret.toBytes()) {"
                            + " return error(\"Unauthorized: Signature Mismatch\"); }",
                    signatureVariable)));
        }

        FunctionBodyBlockNode body = createFunctionBodyBlockNode(
                createToken(OPEN_BRACE_TOKEN), null, createNodeList(statements),
                createToken(CLOSE_BRACE_TOKEN), null);

        return createFunctionDefinitionNode(
                OBJECT_METHOD_DEFINITION, null,
                createNodeList(createToken(PRIVATE_KEYWORD), createToken(ISOLATED_KEYWORD)),
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
        statements.add(NodeParser.parseStatement(
                "decimal freshnessSkewMillis = freshnessNowMillis - freshnessTimestamp;"));
        statements.add(NodeParser.parseStatement(String.format(
                "if freshnessSkewMillis.abs() > %dd {"
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
        return SignaturePayloadTemplateBuilder.build(inputDsl, new SignaturePayloadTemplateBuilder.TokenResolver() {
            @Override
            public String body() {
                return "check request.getTextPayload()";
            }

            @Override
            public String uri() {
                return "request.rawPath";
            }

            @Override
            public String method() {
                return "request.method";
            }

            @Override
            public String secret() {
                return "webhookSecret";
            }

            @Override
            public String header(String headerName) {
                return getSafeHeaderExtraction(headerName);
            }

            @Override
            public String config(String configName) {
                return "self." + configName;
            }
        });
    }

    private void addHeaderTemplateExtractionStatements(List<StatementNode> statements, HeaderTemplate template,
            String skipDeclarationFor) {
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
                if (variableName.equals(skipDeclarationFor)) {
                    continue;
                }
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

    /**
     * Replaces a bare reserved token (e.g. {@code "$body"}) with the given replacement, but only
     * when the token is not immediately followed by another identifier character -- so a DSL
     * variable that merely starts with a reserved name (e.g. {@code $bodyHash}) is left untouched
     * instead of having its {@code $body} prefix corrupted by a plain substring replace.
     *
     * @param text        the text to search within
     * @param token       the bare token to replace, including its leading {@code $}
     * @param replacement the replacement text
     * @return {@code text} with every standalone occurrence of {@code token} replaced
     */
    private String replaceBareToken(String text, String token, String replacement) {
        Pattern tokenPattern = Pattern.compile(Pattern.quote(token) + "(?![A-Za-z0-9_])");
        return tokenPattern.matcher(text).replaceAll(Matcher.quoteReplacement(replacement));
    }

    private String escapeForBallerinaString(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String escapeBacktickTemplate(String value) {
        return value.replace("\\", "\\\\").replace("`", "\\`");
    }

    private String getSafeMapExtraction(String mapName, String key, String defaultValue) {
        String escapedKey = escapeForBallerinaString(key);
        String escapedDefaultValue = escapeForBallerinaString(defaultValue);
        return String.format("%s[\"%s\"] ?: \"%s\"", mapName, escapedKey, escapedDefaultValue);
    }

    private String getSafeHeaderExtraction(String headerName) {
        String escapedHeaderName = escapeForBallerinaString(headerName);
        return String.format("check request.getHeader(\"%s\")", escapedHeaderName);
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

}
