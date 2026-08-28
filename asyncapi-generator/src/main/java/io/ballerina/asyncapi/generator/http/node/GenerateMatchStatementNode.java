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
import io.ballerina.asyncapi.generator.http.model.HttpRemoteFunction;
import io.ballerina.asyncapi.generator.http.model.HttpServiceType;
import io.ballerina.asyncapi.generator.http.utils.CodegenUtils;
import io.ballerina.compiler.syntax.tree.BlockStatementNode;
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.MatchClauseNode;
import io.ballerina.compiler.syntax.tree.MatchStatementNode;
import io.ballerina.compiler.syntax.tree.MethodCallExpressionNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;

import java.util.ArrayList;
import java.util.List;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyMinutiaeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createLiteralValueToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createSeparatedNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBasicLiteralNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBlockStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createCheckExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createExpressionStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMatchClauseNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMatchStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMethodCallExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createPositionalArgumentNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSimpleNameReferenceNode;

/**
 * Generates the {@code match} statement(s) body for {@code matchRemoteFunc} in
 * {@code dispatcher_service.bal}.
 *
 * <p>Most events match on the composite {@code eventIdentifierPath} (e.g. {@code eventIdentifier},
 * holding {@code "meta_deleted"}). Events whose action field has no enumerable set of possible
 * values ({@link HttpRemoteFunction#matchOnEventType()}) can never have a safe composite literal
 * baked in at generation time, so they are matched on the bare {@code eventTypePath} instead (e.g.
 * {@code eventType}, holding {@code "repository_dispatch"}). When both kinds of events are present
 * in the same service type, two separate match statements are generated -- one per subject
 * expression -- since a single Ballerina {@code match} can only test one subject.
 */
public class GenerateMatchStatementNode implements Generator {

    private final List<HttpServiceType> serviceTypes;
    private final String eventIdentifierPath;
    private final String eventTypePath;

    /**
     * Creates a generator for the match statement(s).
     *
     * @param serviceTypes        the list of HTTP service type definitions
     * @param eventIdentifierPath the expression to match against for events with an enumerable
     *                            action field (e.g. {@code genericDataType.event.'type})
     * @param eventTypePath       the expression to match against for events whose action field has
     *                            no enumerable set of values, or {@code null} if the identifier
     *                            type never distinguishes the two (in which case every remote
     *                            function is matched against {@code eventIdentifierPath})
     */
    public GenerateMatchStatementNode(List<HttpServiceType> serviceTypes, String eventIdentifierPath,
            String eventTypePath) {
        this.serviceTypes = serviceTypes;
        this.eventIdentifierPath = eventIdentifierPath;
        this.eventTypePath = eventTypePath;
    }

    @Override
    public List<StatementNode> generate() throws GeneratorException {
        if (eventIdentifierPath.isEmpty()) {
            throw new GeneratorException("Event identifier path is empty");
        }
        if (serviceTypes.isEmpty()) {
            throw new GeneratorException(
                    "No service types found, probably there are no channels defined in the async api spec");
        }
        List<MatchClauseNode> identifierClauses = new ArrayList<>();
        List<MatchClauseNode> eventTypeClauses = new ArrayList<>();
        for (HttpServiceType service : serviceTypes) {
            for (HttpRemoteFunction fn : service.remoteFunctions()) {
                MatchClauseNode clause = generateMatchClause(service.serviceTypeName(), fn);
                if (eventTypePath != null && fn.matchOnEventType()) {
                    eventTypeClauses.add(clause);
                } else {
                    identifierClauses.add(clause);
                }
            }
        }

        List<StatementNode> statements = new ArrayList<>();
        if (!identifierClauses.isEmpty()) {
            statements.add(buildMatchStatement(eventIdentifierPath, identifierClauses));
        }
        if (!eventTypeClauses.isEmpty()) {
            statements.add(buildMatchStatement(eventTypePath, eventTypeClauses));
        }
        return statements;
    }

    private MatchStatementNode buildMatchStatement(String subjectPath, List<MatchClauseNode> clauses) {
        return createMatchStatementNode(
                createToken(SyntaxKind.MATCH_KEYWORD),
                createSimpleNameReferenceNode(createIdentifierToken(subjectPath)),
                createToken(SyntaxKind.OPEN_BRACE_TOKEN),
                createNodeList(clauses),
                createToken(SyntaxKind.CLOSE_BRACE_TOKEN), null);
    }

    private MatchClauseNode generateMatchClause(String serviceTypeName, HttpRemoteFunction fn) {
        String eventName = fn.functionName();
        String namingBasis = fn.displayLabel() != null ? fn.displayLabel() : fn.functionName();
        String funcName = CodegenUtils.getFunctionNameByEventName(namingBasis);
        String resolvedServiceTypeName = CodegenUtils.getServiceTypeNameByServiceName(serviceTypeName);

        SeparatedNodeList<FunctionArgumentNode> args = createSeparatedNodeList(
                createPositionalArgumentNode(createSimpleNameReferenceNode(
                        createIdentifierToken(GenerateDispatcherServiceNode.CLONE_WITH_TYPE_VAR_NAME))),
                createToken(SyntaxKind.COMMA_TOKEN),
                createPositionalArgumentNode(createSimpleNameReferenceNode(
                        createIdentifierToken(String.format("\"%s\"", eventName)))),
                createToken(SyntaxKind.COMMA_TOKEN),
                createPositionalArgumentNode(createSimpleNameReferenceNode(
                        createIdentifierToken(String.format("\"%s\"", resolvedServiceTypeName)))),
                createToken(SyntaxKind.COMMA_TOKEN),
                createPositionalArgumentNode(createSimpleNameReferenceNode(
                        createIdentifierToken(String.format("\"%s\"", funcName)))));

        MethodCallExpressionNode methodCall = createMethodCallExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken("self")),
                createToken(SyntaxKind.DOT_TOKEN),
                createSimpleNameReferenceNode(
                        createIdentifierToken(GenerateExecuteRemoteFuncNode.EXECUTE_REMOTE_FUNC_NAME)),
                createToken(SyntaxKind.OPEN_PAREN_TOKEN), args,
                createToken(SyntaxKind.CLOSE_PAREN_TOKEN));

        CheckExpressionNode checkExpr = createCheckExpressionNode(SyntaxKind.CHECK_EXPRESSION,
                createToken(SyntaxKind.CHECK_KEYWORD), methodCall);

        BlockStatementNode block = createBlockStatementNode(
                createToken(SyntaxKind.OPEN_BRACE_TOKEN),
                createNodeList(createExpressionStatementNode(SyntaxKind.CALL_STATEMENT,
                        checkExpr, createToken(SyntaxKind.SEMICOLON_TOKEN))),
                createToken(SyntaxKind.CLOSE_BRACE_TOKEN));

        return createMatchClauseNode(
                createSeparatedNodeList(createBasicLiteralNode(SyntaxKind.STRING_LITERAL,
                        createLiteralValueToken(SyntaxKind.STRING_LITERAL_TOKEN,
                                String.format("\"%s\"", eventName),
                                createEmptyMinutiaeList(), createEmptyMinutiaeList()))),
                null,
                createLiteralValueToken(SyntaxKind.RIGHT_DOUBLE_ARROW_TOKEN, "=>",
                        createEmptyMinutiaeList(), createEmptyMinutiaeList()),
                block);
    }
}
