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
package io.ballerina.asyncapi.generator.http.node;

import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.compiler.syntax.tree.FunctionBodyBlockNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.compiler.syntax.tree.ReturnTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.StatementNode;

import java.util.ArrayList;
import java.util.List;

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
 * <p>Validates the HMAC signature carried in the configured auth header, supporting
 * {@code sha256}, {@code sha1}, {@code sha384}, and {@code sha512} algorithms.
 * Returns an HTTP 401 response if the header is absent, malformed, uses an unsupported algorithm,
 * or the computed HMAC does not match the supplied signature.
 */
public class GenerateVerifyWebhookSignatureFuncNode implements Generator {

    public static final String VERIFY_WEBHOOK_SIGNATURE_FUNC = "verifyWebhookSignature";

    private final String headerName;

    /**
     * Creates a generator for the {@code verifyWebhookSignature} function.
     *
     * @param headerName the HTTP header name carrying the HMAC signature (from {@code x-ballerina-auth})
     */
    public GenerateVerifyWebhookSignatureFuncNode(String headerName) {
        this.headerName = headerName;
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
        statements.add(NodeParser.parseStatement(String.format(
                "if !request.hasHeader(\"%s\") { return error(\"Unauthorized\"); }",
                headerName)));
        statements.add(NodeParser.parseStatement(String.format(
                "string signature = check request.getHeader(\"%s\");",
                headerName)));
        statements.add(NodeParser.parseStatement(
                "byte[] binaryPay = check request.getBinaryPayload();"));
        statements.add(NodeParser.parseStatement(
                "string[] parts = re `=`.split(signature);"));
        statements.add(NodeParser.parseStatement(
                "if parts.length() < 2 { return error(\"Unauthorized\"); }"));
        statements.add(NodeParser.parseStatement(
                "string algorithm = parts[0];"));
        statements.add(NodeParser.parseStatement(
                "byte[] computedHmac;"));
        statements.add(NodeParser.parseStatement(
                "string expected;"));
        statements.add(NodeParser.parseStatement(
                "if algorithm == \"sha256\" {"
                        + " computedHmac = check crypto:hmacSha256(binaryPay, webhookSecret.toBytes());"
                        + " expected = \"sha256=\" + computedHmac.toBase16(); }"
                        + " else if algorithm == \"sha1\" {"
                        + " computedHmac = check crypto:hmacSha1(binaryPay, webhookSecret.toBytes());"
                        + " expected = \"sha1=\" + computedHmac.toBase16(); }"
                        + " else if algorithm == \"sha384\" {"
                        + " computedHmac = check crypto:hmacSha384(binaryPay, webhookSecret.toBytes());"
                        + " expected = \"sha384=\" + computedHmac.toBase16(); }"
                        + " else if algorithm == \"sha512\" {"
                        + " computedHmac = check crypto:hmacSha512(binaryPay, webhookSecret.toBytes());"
                        + " expected = \"sha512=\" + computedHmac.toBase16(); }"
                        + " else { return error(\"Unauthorized\"); }"));
        statements.add(NodeParser.parseStatement(
                "if !crypto:equalConstantTime(signature.toBytes(), expected.toBytes()) {"
                        + " return error(\"Unauthorized\"); }"));
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

    private ReturnTypeDescriptorNode buildOptionalErrorReturnType() {
        return createReturnTypeDescriptorNode(
                createToken(RETURNS_KEYWORD),
                createEmptyNodeList(),
                createOptionalTypeDescriptorNode(
                        createToken(ERROR_KEYWORD),
                        createToken(QUESTION_MARK_TOKEN)));
    }
}
