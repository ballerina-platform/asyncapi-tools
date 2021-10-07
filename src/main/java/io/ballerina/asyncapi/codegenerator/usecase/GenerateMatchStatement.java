/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.asyncapi.codegenerator.usecase;

import io.ballerina.asyncapi.codegenerator.configuration.BallerinaAsyncApiException;
import io.ballerina.asyncapi.codegenerator.configuration.Constants;
import io.ballerina.asyncapi.codegenerator.entity.RemoteFunction;
import io.ballerina.asyncapi.codegenerator.entity.ServiceType;
import io.ballerina.asyncapi.codegenerator.usecase.utils.CodegenUtils;
import io.ballerina.compiler.syntax.tree.*;

import java.util.ArrayList;
import java.util.List;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.*;
import static io.ballerina.compiler.syntax.tree.NodeFactory.*;


public class GenerateMatchStatement implements GenerateUseCase {
    private final CodegenUtils codegenUtils = new CodegenUtils();
    private final List<ServiceType> serviceTypes;
    private final String eventIdentifierPath;

    public GenerateMatchStatement(List<ServiceType> serviceTypes, String eventIdentifierPath) {
        this.serviceTypes = serviceTypes;
        this.eventIdentifierPath = eventIdentifierPath;
    }

    @Override
    public MatchStatementNode generate() throws BallerinaAsyncApiException {
        List<MatchClauseNode> matchClauseNodes = new ArrayList<>();
        for (ServiceType service : serviceTypes) {
            String serviceName = service.getServiceTypeName();
            for (RemoteFunction remoteFunction : service.getRemoteFunctions()) {
                String eventName = remoteFunction.getEventName();
                String formattedEventName = codegenUtils.getFunctionNameByEventName(eventName);
                MatchClauseNode matchClause = generateMatchClause(serviceName, eventName, formattedEventName);
                matchClauseNodes.add(matchClause);
            }
        }

        return createMatchStatementNode(
                createToken(SyntaxKind.MATCH_KEYWORD),
                createSimpleNameReferenceNode(createIdentifierToken(eventIdentifierPath)),
                createToken(SyntaxKind.OPEN_BRACE_TOKEN),
                createNodeList(matchClauseNodes),
                createToken(SyntaxKind.CLOSE_BRACE_TOKEN), null);
    }

    /**
     * Generates each match clause which filters event types in dispatcher_service.bal
     */
    private MatchClauseNode generateMatchClause(String serviceTypeName, String eventName, String formattedEventName) {
        SeparatedNodeList<FunctionArgumentNode> argumentsList = createSeparatedNodeList(
                createPositionalArgumentNode(createSimpleNameReferenceNode(
                        createIdentifierToken(Constants.CLONE_WITH_TYPE_VAR_NAME))),
                createToken(SyntaxKind.COMMA_TOKEN),
                createPositionalArgumentNode(createSimpleNameReferenceNode(
                        createIdentifierToken("\"" + eventName + "\""))),
                createToken(SyntaxKind.COMMA_TOKEN),
                createPositionalArgumentNode(createSimpleNameReferenceNode(
                        createIdentifierToken("\"" +
                                codegenUtils.getServiceTypeNameByServiceName(serviceTypeName) + "\""))),
                createToken(SyntaxKind.COMMA_TOKEN),
                createPositionalArgumentNode(createSimpleNameReferenceNode(
                        createIdentifierToken("\"" + formattedEventName + "\""))));

        MethodCallExpressionNode methodCallExpressionNode = createMethodCallExpressionNode(
                createSimpleNameReferenceNode(
                        createIdentifierToken(Constants.SELF_KEYWORD)),
                createToken(SyntaxKind.DOT_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken(
                        Constants.INTEROP_INVOKE_FUNCTION_NAME)),
                createToken(SyntaxKind.OPEN_PAREN_TOKEN), argumentsList,
                createToken(SyntaxKind.CLOSE_PAREN_TOKEN));

        CheckExpressionNode lineNode = createCheckExpressionNode(SyntaxKind.CHECK_EXPRESSION,
                createToken(SyntaxKind.CHECK_KEYWORD), methodCallExpressionNode);
        BlockStatementNode blockStatement = createBlockStatementNode(
                createToken(SyntaxKind.OPEN_BRACE_TOKEN),
                createNodeList(createExpressionStatementNode(SyntaxKind.CALL_STATEMENT,
                        lineNode,
                        createToken(SyntaxKind.SEMICOLON_TOKEN))),
                createToken(SyntaxKind.CLOSE_BRACE_TOKEN));

        return createMatchClauseNode(createSeparatedNodeList(
                createBasicLiteralNode(SyntaxKind.STRING_LITERAL,
                        createLiteralValueToken(
                                SyntaxKind.STRING_LITERAL_TOKEN, "\"" + eventName + "\"",
                                createEmptyMinutiaeList(), createEmptyMinutiaeList())
                )), null,
                createLiteralValueToken(SyntaxKind.RIGHT_DOUBLE_ARROW_TOKEN, "=>",
                        createEmptyMinutiaeList(), createEmptyMinutiaeList()),
                blockStatement);
    }
}
