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
import io.ballerina.asyncapi.generator.http.extractor.EventIdentifierExtractor;
import io.ballerina.asyncapi.generator.http.generator.DataTypesGenerator;
import io.ballerina.asyncapi.generator.http.model.EventIdentifierConfig;
import io.ballerina.asyncapi.generator.http.model.HttpServiceType;
import io.ballerina.asyncapi.generator.http.utils.CodegenUtils;
import io.ballerina.compiler.syntax.tree.BlockStatementNode;
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionStatementNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.FunctionBodyBlockNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.MatchClauseNode;
import io.ballerina.compiler.syntax.tree.MatchStatementNode;
import io.ballerina.compiler.syntax.tree.MethodCallExpressionNode;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.StatementNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.ballerina.asyncapi.generator.http.node.GenerateAddServiceRefFuncNode.buildErrorReturnType;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyMinutiaeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createLiteralValueToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createSeparatedNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBasicLiteralNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBlockStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBuiltinSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createCheckExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createExpressionStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionBodyBlockNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionDefinitionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionSignatureNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMatchClauseNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMatchStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMethodCallExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createPositionalArgumentNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRequiredParameterNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CALL_STATEMENT;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CHECK_EXPRESSION;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CHECK_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.COMMA_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.DOT_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FUNCTION_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.MATCH_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OBJECT_METHOD_DEFINITION;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PRIVATE_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RIGHT_DOUBLE_ARROW_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SEMICOLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.STRING_LITERAL;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.STRING_LITERAL_TOKEN;

/**
 * Generates the {@code private function matchRemoteFunc(...) returns error?} method node
 * for the {@code DispatcherService} class in {@code dispatcher_service.bal}.
 *
 * <p>For {@code "composite"}, the signature includes both {@code string eventIdentifier} (the
 * compound header+body value) and {@code string eventType} (the header value used for dispatch).
 * For {@code "header"} and {@code "body"}, the signature includes only {@code string eventType}.
 * Generates a {@code match eventType} statement that dispatches each channel name to its
 * corresponding {@link GenerateMatchChunkFuncNode} function.
 */
public class GenerateMatchRemoteFuncNode implements Generator {

    public static final String DISPATCHER_MATCH_REMOTE_FUNC = "matchRemoteFunc";

    private final List<HttpServiceType> serviceTypes;
    private final EventIdentifierConfig identifierConfig;
    private final String eventIdentifierPath;
    private final List<GenerateMatchChunkFuncNode> chunkGenerators = new ArrayList<>();

    /**
     * Creates a generator for the {@code matchRemoteFunc} method.
     *
     * @param serviceTypes        the list of HTTP service type definitions
     * @param identifierConfig    the resolved event identifier type and path
     * @param eventIdentifierPath the expression matched against in the chunk match statements
     */
    public GenerateMatchRemoteFuncNode(List<HttpServiceType> serviceTypes,
                                       EventIdentifierConfig identifierConfig,
                                       String eventIdentifierPath) {
        this.serviceTypes = serviceTypes;
        this.identifierConfig = identifierConfig;
        this.eventIdentifierPath = eventIdentifierPath;
    }

    /**
     * Returns the chunk generators created during {@link #generate()}.
     *
     * @return an unmodifiable view of the chunk generators
     */
    public List<GenerateMatchChunkFuncNode> getChunkGenerators() {
        return Collections.unmodifiableList(chunkGenerators);
    }

    @Override
    public FunctionDefinitionNode generate() throws GeneratorException {
        String type = identifierConfig.type();
        boolean isBody = EventIdentifierExtractor.X_BALLERINA_EVENT_TYPE_BODY.equals(type);
        boolean isComposite = EventIdentifierExtractor.X_BALLERINA_EVENT_TYPE_COMPOSITE.equals(type);

        SeparatedNodeList<ParameterNode> params;
        if (isComposite) {
            params = createSeparatedNodeList(
                    createRequiredParameterNode(
                            createEmptyNodeList(),
                            createSimpleNameReferenceNode(createIdentifierToken(DataTypesGenerator.GENERIC_DATA_TYPE)),
                            createIdentifierToken(GenerateDispatcherServiceNode.CLONE_WITH_TYPE_VAR_NAME)),
                    createToken(COMMA_TOKEN),
                    createRequiredParameterNode(
                            createEmptyNodeList(),
                            createBuiltinSimpleNameReferenceNode(null, createIdentifierToken("string")),
                            createIdentifierToken("eventIdentifier")),
                    createToken(COMMA_TOKEN),
                    createRequiredParameterNode(
                            createEmptyNodeList(),
                            createBuiltinSimpleNameReferenceNode(null, createIdentifierToken("string")),
                            createIdentifierToken("eventType")));
        } else {
            params = createSeparatedNodeList(
                    createRequiredParameterNode(
                            createEmptyNodeList(),
                            createSimpleNameReferenceNode(createIdentifierToken(DataTypesGenerator.GENERIC_DATA_TYPE)),
                            createIdentifierToken(GenerateDispatcherServiceNode.CLONE_WITH_TYPE_VAR_NAME)),
                    createToken(COMMA_TOKEN),
                    createRequiredParameterNode(
                            createEmptyNodeList(),
                            createBuiltinSimpleNameReferenceNode(null, createIdentifierToken("string")),
                            createIdentifierToken("eventType")));
        }

        FunctionSignatureNode signature = createFunctionSignatureNode(
                createToken(OPEN_PAREN_TOKEN), params,
                createToken(CLOSE_PAREN_TOKEN), buildErrorReturnType());

        // Build one match arm per service type, dispatching on the snake_case channel name
        List<MatchClauseNode> clauses = new ArrayList<>();
        for (HttpServiceType serviceType : serviceTypes) {
            GenerateMatchChunkFuncNode chunkGen =
                    new GenerateMatchChunkFuncNode(serviceType, eventIdentifierPath, !isBody);
            chunkGenerators.add(chunkGen);

            SeparatedNodeList<FunctionArgumentNode> chunkArgs;
            if (isComposite) {
                chunkArgs = createSeparatedNodeList(
                        createPositionalArgumentNode(
                                createSimpleNameReferenceNode(createIdentifierToken(
                                        GenerateDispatcherServiceNode.CLONE_WITH_TYPE_VAR_NAME))),
                        createToken(COMMA_TOKEN),
                        createPositionalArgumentNode(
                                createSimpleNameReferenceNode(createIdentifierToken("eventIdentifier"))));
            } else if (!isBody) {
                chunkArgs = createSeparatedNodeList(
                        createPositionalArgumentNode(
                                createSimpleNameReferenceNode(createIdentifierToken(
                                        GenerateDispatcherServiceNode.CLONE_WITH_TYPE_VAR_NAME))),
                        createToken(COMMA_TOKEN),
                        createPositionalArgumentNode(
                                createSimpleNameReferenceNode(createIdentifierToken("eventType"))));
            } else {
                chunkArgs = createSeparatedNodeList(
                        createPositionalArgumentNode(
                                createSimpleNameReferenceNode(createIdentifierToken(
                                        GenerateDispatcherServiceNode.CLONE_WITH_TYPE_VAR_NAME))));
            }

            MethodCallExpressionNode call = createMethodCallExpressionNode(
                    createSimpleNameReferenceNode(createIdentifierToken("self")),
                    createToken(DOT_TOKEN),
                    createSimpleNameReferenceNode(createIdentifierToken(
                            GenerateMatchChunkFuncNode.chunkFuncName(serviceType.serviceTypeName()))),
                    createToken(OPEN_PAREN_TOKEN),
                    chunkArgs,
                    createToken(CLOSE_PAREN_TOKEN));

            CheckExpressionNode checkExpr = createCheckExpressionNode(
                    CHECK_EXPRESSION,
                    createToken(CHECK_KEYWORD), call);

            ExpressionStatementNode stmt = createExpressionStatementNode(
                    CALL_STATEMENT,
                    checkExpr,
                    createToken(SEMICOLON_TOKEN));

            BlockStatementNode block = createBlockStatementNode(
                    createToken(OPEN_BRACE_TOKEN),
                    createNodeList((StatementNode) stmt),
                    createToken(CLOSE_BRACE_TOKEN));

            MatchClauseNode clause = createMatchClauseNode(
                    createSeparatedNodeList(createBasicLiteralNode(STRING_LITERAL,
                            createLiteralValueToken(STRING_LITERAL_TOKEN,
                                    String.format("\"%s\"", CodegenUtils.toSnakeCase(serviceType.serviceTypeName())),
                                    createEmptyMinutiaeList(), createEmptyMinutiaeList()))),
                    null,
                    createLiteralValueToken(RIGHT_DOUBLE_ARROW_TOKEN, "=>",
                            createEmptyMinutiaeList(), createEmptyMinutiaeList()),
                    block);

            clauses.add(clause);
        }

        MatchStatementNode matchStatement = createMatchStatementNode(
                createToken(MATCH_KEYWORD),
                createSimpleNameReferenceNode(createIdentifierToken("eventType")),
                createToken(OPEN_BRACE_TOKEN),
                createNodeList(clauses),
                createToken(CLOSE_BRACE_TOKEN), null);

        FunctionBodyBlockNode body = createFunctionBodyBlockNode(
                createToken(OPEN_BRACE_TOKEN), null,
                createNodeList((StatementNode) matchStatement),
                createToken(CLOSE_BRACE_TOKEN), null);

        return createFunctionDefinitionNode(
                OBJECT_METHOD_DEFINITION, null,
                createNodeList(createToken(PRIVATE_KEYWORD)),
                createToken(FUNCTION_KEYWORD),
                createIdentifierToken(DISPATCHER_MATCH_REMOTE_FUNC),
                createEmptyNodeList(),
                signature, body);
    }
}
