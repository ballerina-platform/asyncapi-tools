/*
 * Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.asyncapi.core.generators.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.ballerina.asyncapi.core.GeneratorConstants;
import io.ballerina.asyncapi.core.GeneratorUtils;
import io.ballerina.asyncapi.core.exception.BallerinaAsyncApiException;
import io.ballerina.compiler.syntax.tree.AssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionStatementNode;
import io.ballerina.compiler.syntax.tree.FieldAccessExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.FunctionBodyNode;
import io.ballerina.compiler.syntax.tree.IfElseStatementNode;
import io.ballerina.compiler.syntax.tree.ImplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.LockStatementNode;
import io.ballerina.compiler.syntax.tree.MatchClauseNode;
import io.ballerina.compiler.syntax.tree.MethodCallExpressionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.ParenthesizedArgList;
import io.ballerina.compiler.syntax.tree.PositionalArgumentNode;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.ReturnStatementNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.StreamTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.StreamTypeParamsNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.ballerina.asyncapi.core.GeneratorConstants.CONNECTION_CLOSED;
import static io.ballerina.asyncapi.core.GeneratorConstants.CONSUME;
import static io.ballerina.asyncapi.core.GeneratorConstants.CREATE_TYPE1_AS_STRING;
import static io.ballerina.asyncapi.core.GeneratorConstants.MESSAGE;
import static io.ballerina.asyncapi.core.GeneratorConstants.MESSAGE_VAR_NAME;
import static io.ballerina.asyncapi.core.GeneratorConstants.PIPE;
import static io.ballerina.asyncapi.core.GeneratorConstants.PIPES;
import static io.ballerina.asyncapi.core.GeneratorConstants.PRODUCE;
import static io.ballerina.asyncapi.core.GeneratorConstants.SELF;
import static io.ballerina.asyncapi.core.GeneratorConstants.SERVER_STREAMING;
import static io.ballerina.asyncapi.core.GeneratorConstants.SIMPLE_PIPE;
import static io.ballerina.asyncapi.core.GeneratorConstants.TIMEOUT;
import static io.ballerina.asyncapi.core.GeneratorConstants.UUID;
import static io.ballerina.asyncapi.core.GeneratorConstants.WRITE_MESSAGE_QUEUE;
import static io.ballerina.asyncapi.core.GeneratorConstants.X_RESPONSE_TYPE;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createSeparatedNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createAssignmentStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBlockStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createCaptureBindingPatternNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createCheckExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createErrorConstructorExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createExpressionStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFieldAccessExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFieldBindingPatternVarnameNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionBodyBlockNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createIfElseStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createImplicitNewExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createLockStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMatchClauseNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMethodCallExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createParenthesizedArgList;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createPositionalArgumentNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createQualifiedNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRequiredExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createReturnStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createStreamTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createStreamTypeParamsNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTypedBindingPatternNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createVariableDeclarationNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CHECK_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.COLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.COMMA_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.DOT_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.EQUAL_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ERROR_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.GT_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.IF_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.LOCK_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.LT_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.NEW_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RIGHT_DOUBLE_ARROW_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SEMICOLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.STREAM_KEYWORD;

/**
 * This Util class uses for generating remote function body  {@link FunctionBodyNode}.
 *
 * @since 1.3.0
 */
public class RemoteFunctionBodyGenerator {

    private final List<ImportDeclarationNode> imports;

    public RemoteFunctionBodyGenerator(List<ImportDeclarationNode> imports) {
        this.imports = Collections.unmodifiableList(imports);
    }

    private void createCommentStatementsForDispatcherId(List<StatementNode> statementsList,
                                                        String requestType,
                                                        String dispatcherStreamId,
                                                        String requestTypePipe,
                                                        boolean isSubscribe) {

        SimpleNameReferenceNode requestTypePipeNode = createSimpleNameReferenceNode(
                createIdentifierToken(requestTypePipe));
        Token equalToken = createToken(EQUAL_TOKEN);
        Token semicolonToken = createToken(SEMICOLON_TOKEN);
        Token dotToken = createToken(DOT_TOKEN);
        Token openBraceToken = createToken(OPEN_BRACE_TOKEN);
        Token closeBraceToken = createToken(CLOSE_BRACE_TOKEN);
        Token openParenToken = createToken(OPEN_PAREN_TOKEN);
        Token closeParenToken = createToken(CLOSE_PAREN_TOKEN);

        PositionalArgumentNode responseTypeTimeOut = createPositionalArgumentNode(createRequiredExpressionNode(
                createIdentifierToken(TIMEOUT)));


        ArrayList<StatementNode> lockStatements = new ArrayList<>();

        //Create remote function body when dispatcherStreamId is present
        if (dispatcherStreamId != null) {
            SimpleNameReferenceNode dispatcherStreamIdNode = createSimpleNameReferenceNode(
                    createIdentifierToken(dispatcherStreamId));
            VariableDeclarationNode dispatcherStreamIdDefineNode = createVariableDeclarationNode(
                    createEmptyNodeList(), null,
                    createTypedBindingPatternNode(
                            createSimpleNameReferenceNode(createIdentifierToken("string")),
                            createFieldBindingPatternVarnameNode(dispatcherStreamIdNode)),
                    null, null, semicolonToken);
            // string id;
            statementsList.add(dispatcherStreamIdDefineNode);

            QualifiedNameReferenceNode uuidNode = createQualifiedNameReferenceNode(createIdentifierToken(UUID),
                    createToken(COLON_TOKEN), createIdentifierToken(CREATE_TYPE1_AS_STRING));
            AssignmentStatementNode uuidAssignmentNode = createAssignmentStatementNode(dispatcherStreamIdNode,
                    equalToken, uuidNode, semicolonToken);


            FieldAccessExpressionNode requestTypeVarRef = createFieldAccessExpressionNode(
                    createSimpleNameReferenceNode(createIdentifierToken(requestType)), dotToken,
                    dispatcherStreamIdNode);
            AssignmentStatementNode idValueAssignmentStatementNode = createAssignmentStatementNode(requestTypeVarRef,
                    equalToken, dispatcherStreamIdNode, semicolonToken);

            //  id = uuid:createType1AsString();
            lockStatements.add(uuidAssignmentNode);

            // subscribeMessage.id = id;
            lockStatements.add(idValueAssignmentStatementNode);

            LockStatementNode functionLockStatementNode = createLockStatementNode(createToken(LOCK_KEYWORD),
                    createBlockStatementNode(openBraceToken, createNodeList(lockStatements), closeBraceToken),
                    null);


            statementsList.add(functionLockStatementNode);


            FieldAccessExpressionNode remoteSelfPipes = createFieldAccessExpressionNode(
                    createSimpleNameReferenceNode(createIdentifierToken(SELF)), dotToken,
                    createSimpleNameReferenceNode(createIdentifierToken("pipes")));

            MethodCallExpressionNode addPipeMethodCallExpressionNode = createMethodCallExpressionNode(remoteSelfPipes,
                    dotToken,
                    createSimpleNameReferenceNode(createIdentifierToken("addPipe")),
                    openParenToken, createSeparatedNodeList(dispatcherStreamIdNode, createToken(COMMA_TOKEN),
                            requestTypePipeNode), closeParenToken);
            ExpressionStatementNode addPipeByIdStatement = createExpressionStatementNode(null,
                    addPipeMethodCallExpressionNode, semicolonToken);

            // self.pipes.addPipe(id, subscribePipe);
            statementsList.add(addPipeByIdStatement);
        } else {


            SimpleNameReferenceNode selfNode = createSimpleNameReferenceNode(createIdentifierToken(SELF));

            FieldAccessExpressionNode remoteSelfPipes = createFieldAccessExpressionNode(
                    selfNode, dotToken,
                    createSimpleNameReferenceNode(createIdentifierToken("pipes")));

            MethodCallExpressionNode addPipeMethodCallExpressionNode = createMethodCallExpressionNode(remoteSelfPipes,
                    dotToken,
                    createSimpleNameReferenceNode(createIdentifierToken("getPipe")),
                    openParenToken, createSeparatedNodeList(createSimpleNameReferenceNode(
                            createIdentifierToken("\"" +
                                    requestType + "\""))), closeParenToken);
            FieldAccessExpressionNode remoteSelfPipe = createFieldAccessExpressionNode(
                    selfNode, dotToken,
                    requestTypePipeNode);
            AssignmentStatementNode assignmentStatementNode = createAssignmentStatementNode(remoteSelfPipe, equalToken,
                    addPipeMethodCallExpressionNode, semicolonToken);

            LockStatementNode lockStatementNode = createLockStatementNode(createToken(LOCK_KEYWORD),
                    createBlockStatementNode(openBraceToken, createNodeList(assignmentStatementNode),
                            closeBraceToken), null);

            //  lock {
            //     self.connectionInitMessagePipe = self.pipes.getPipe("connectionInitMessage");
            //  }
            statementsList.add(lockStatementNode);
        }

        if (!isSubscribe) {
            SimpleNameReferenceNode requestTypeNameNode =
                    createSimpleNameReferenceNode(createIdentifierToken(requestType));
            VariableDeclarationNode messageVariableDeclarationNode =
                    createVariableDeclarationNode(createEmptyNodeList(),
                            null, createTypedBindingPatternNode(
                                    createSimpleNameReferenceNode(createIdentifierToken(
                                            MESSAGE)),
                                    createCaptureBindingPatternNode(createIdentifierToken(MESSAGE_VAR_NAME))),
                            equalToken,
                            createCheckExpressionNode(null, createToken(CHECK_KEYWORD),
                                    createMethodCallExpressionNode(requestTypeNameNode, dotToken,
                                            createSimpleNameReferenceNode(
                                                    createIdentifierToken("cloneWithType")),
                                            openParenToken, createSeparatedNodeList(), closeParenToken)),
                            semicolonToken);

            // Message message = check subscribe.cloneWithType();
            statementsList.add(messageVariableDeclarationNode);

            List<Node> argumentArrays = new ArrayList<>();

            argumentArrays.add(createSimpleNameReferenceNode(createIdentifierToken(MESSAGE_VAR_NAME)));
            argumentArrays.add(createToken(COMMA_TOKEN));
            argumentArrays.add(responseTypeTimeOut);
            FieldAccessExpressionNode globalQueue = createFieldAccessExpressionNode(
                    createSimpleNameReferenceNode(createIdentifierToken(SELF)), dotToken,
                    createSimpleNameReferenceNode(createIdentifierToken(WRITE_MESSAGE_QUEUE)));
            CheckExpressionNode callGlobalQueueProduce = createCheckExpressionNode(null, createToken(
                            CHECK_KEYWORD),
                    createMethodCallExpressionNode(globalQueue, dotToken,
                            createSimpleNameReferenceNode(createIdentifierToken(PRODUCE)), openParenToken,
                            createSeparatedNodeList(
                                    argumentArrays
                            ), closeParenToken));
            ExpressionStatementNode callGlobalQueueProduceNode = createExpressionStatementNode(null,
                    callGlobalQueueProduce, semicolonToken);

            // check self.writeMessageQueue.produce(tuple, timeout);
            statementsList.add(callGlobalQueueProduceNode);
        }
    }

    public List<ImportDeclarationNode> getImports() {
        return imports;
    }


    /**
     * Generate function body node for the remote function.
     * <p>
     * //     * @param path      - remote function path
     * //     * @param operation - asyncapi operation
     *
     * @return - {@link FunctionBodyNode}
     * @throws BallerinaAsyncApiException - throws exception if generating FunctionBodyNode fails.
     */
    public FunctionBodyNode getFunctionBodyNode(Map<String, JsonNode> extensions, String requestType,
                                                String specDispatcherStreamId, List<MatchClauseNode> matchStatementList,
                                                boolean isSubscribe, String responseType, List<String> pipeNameMethods)
            throws BallerinaAsyncApiException {

        // Create statements
        List<StatementNode> statementsList = new ArrayList<>();

        // This return type for target data type binding.

        if (extensions != null) {
//            JsonNode xResponse = extensions.get(X_RESPONSE);
            JsonNode xResponseType = extensions.get(X_RESPONSE_TYPE);

            if (xResponseType != null && xResponseType.equals(new TextNode(SERVER_STREAMING))) {
                //TODO: Include an if condition to check this only one time
//                utilGenerator.setStreamFound(true);
                createStreamFunctionBodyStatements(
                        statementsList,
                        requestType,
                        responseType,
                        specDispatcherStreamId
                        , matchStatementList,
                        isSubscribe,
                        pipeNameMethods
                );


            } else {

                createSimpleRPCFunctionBodyStatements(statementsList, requestType, responseType, specDispatcherStreamId,
                        matchStatementList, isSubscribe, pipeNameMethods);
            }

        } else {
            createNoResponseFunctionBodyStatement(statementsList, requestType);

        }
        //Create statements
        NodeList<StatementNode> statements = createNodeList(statementsList);
        return createFunctionBodyBlockNode(createToken(OPEN_BRACE_TOKEN), null, statements,
                createToken(CLOSE_BRACE_TOKEN), null);
    }

    private void createStreamFunctionBodyStatements(List<StatementNode> statementsList,
                                                    String requestType, String responseType,

                                                    String dispatcherStreamId,
                                                    List<MatchClauseNode> matchStatementList, boolean isSubscribe,
                                                    List<String> pipeNameMethods) {

        String requestTypePipe = requestType + "Pipe";


        Token equalToken = createToken(EQUAL_TOKEN);
        Token semicolonToken = createToken(SEMICOLON_TOKEN);
        Token openBraceToken = createToken(OPEN_BRACE_TOKEN);
        Token closeBraceToken = createToken(CLOSE_BRACE_TOKEN);
        Token dotToken = createToken(DOT_TOKEN);
        Token closeParenToken = createToken(CLOSE_PAREN_TOKEN);
        Token openParenToken = createToken(OPEN_PAREN_TOKEN);
        Token rightDoubleArrow = createToken(RIGHT_DOUBLE_ARROW_TOKEN);

        SimpleNameReferenceNode requestTypePipeNode = createSimpleNameReferenceNode(createIdentifierToken(
                requestTypePipe));
        SimpleNameReferenceNode responseMessageNode = createSimpleNameReferenceNode(
                createIdentifierToken(MESSAGE_VAR_NAME));
        QualifiedNameReferenceNode pipeTypeCombined = createQualifiedNameReferenceNode(
                createIdentifierToken(SIMPLE_PIPE),
                createToken(COLON_TOKEN), createIdentifierToken(GeneratorConstants.CAPITAL_PIPE));

        //if (self.writeMessageQueue.isClosed()) {
        //       return error("");
        //}
        if (!isSubscribe) {
            addWriteMessageClosed(statementsList);
        }


        //pipe:Pipe tuplePipe = new (10000);
        if (dispatcherStreamId != null) {
            List<Node> argumentsList = new ArrayList<>();
            argumentsList.add(createIdentifierToken("10000"));
            SeparatedNodeList<FunctionArgumentNode> arguments = createSeparatedNodeList(argumentsList);
            ParenthesizedArgList parenthesizedArgList = createParenthesizedArgList(openParenToken,
                    arguments,
                    closeParenToken);
            ImplicitNewExpressionNode expressionNode = createImplicitNewExpressionNode(createToken(NEW_KEYWORD),
                    parenthesizedArgList);
            VariableDeclarationNode remotePipeTypeEnsureStatement = createVariableDeclarationNode(createEmptyNodeList(),
                    null,
                    createTypedBindingPatternNode(
                            pipeTypeCombined,
                            createFieldBindingPatternVarnameNode(requestTypePipeNode)),
                    equalToken, expressionNode, semicolonToken);
            statementsList.add(remotePipeTypeEnsureStatement);
        } else {
            //pipe:Pipe connectionInitMessagePipe;
            VariableDeclarationNode remotePipeTypeEnsureStatement = createVariableDeclarationNode(createEmptyNodeList(),
                    null,
                    createTypedBindingPatternNode(
                            pipeTypeCombined,
                            createFieldBindingPatternVarnameNode(requestTypePipeNode)),
                    null, null, semicolonToken);
            statementsList.add(remotePipeTypeEnsureStatement);


        }

        //lock {
        //            id = uuid:createType1AsString();
        //            self.pipes.addPipe(id, subscribeMessagePipe);
        //            subscribeMessage.id = id;
        //
        //        }
        //        self.pipes.addPipe(id, subscribeMessagePipe);
        //        Message message = check subscribeMessage.cloneWithType();
        //        check self.writeMessageQueue.produce(message, timeout);
        createCommentStatementsForDispatcherId(statementsList, requestType, dispatcherStreamId,
                requestTypePipe, isSubscribe);


        if (dispatcherStreamId == null) {
            pipeNameMethods.add(requestTypePipe);
            ArrayList<StatementNode> statementNodes = new ArrayList<>();
            QualifiedNameReferenceNode pipeTypeName = createQualifiedNameReferenceNode(
                    createIdentifierToken(SIMPLE_PIPE),
                    createToken(COLON_TOKEN), createIdentifierToken(GeneratorConstants.CAPITAL_PIPE));
            FieldAccessExpressionNode selfPipes = createFieldAccessExpressionNode(
                    createSimpleNameReferenceNode(createIdentifierToken(SELF)), createToken(DOT_TOKEN),
                    createSimpleNameReferenceNode(createIdentifierToken(PIPES)));
            MethodCallExpressionNode methodCallExpressionNode = createMethodCallExpressionNode(selfPipes, dotToken,
                    createSimpleNameReferenceNode(createIdentifierToken("getPipe")),
                    openParenToken, createSeparatedNodeList(createSimpleNameReferenceNode(
                            createIdentifierToken("\"" + requestType + "\""))), closeParenToken);


            VariableDeclarationNode pipeTypeEnsureStatement = createVariableDeclarationNode(
                    createEmptyNodeList(), null,
                    createTypedBindingPatternNode(
                            pipeTypeName,
                            createFieldBindingPatternVarnameNode(requestTypePipeNode)),
                    equalToken, methodCallExpressionNode, semicolonToken);
            statementNodes.add(pipeTypeEnsureStatement);

            List<Node> nodes = new ArrayList<>();
            nodes.add(responseMessageNode);
            nodes.add(createToken(COMMA_TOKEN));
            nodes.add(createIdentifierToken("5"));
            MethodCallExpressionNode pipeProduceExpressionNode = createMethodCallExpressionNode(
                    requestTypePipeNode, dotToken,
                    createSimpleNameReferenceNode(createIdentifierToken(PRODUCE)),
                    openParenToken, createSeparatedNodeList(nodes), closeParenToken);

            CheckExpressionNode pipeProduceCheck = createCheckExpressionNode(null, createToken(CHECK_KEYWORD),
                    pipeProduceExpressionNode);
            ExpressionStatementNode pipeProduceExpression = createExpressionStatementNode(null,
                    pipeProduceCheck, createToken(SEMICOLON_TOKEN));
            statementNodes.add(pipeProduceExpression);
            List<Node> responseNodeList = new ArrayList<>();
            if (responseType.contains("|")) {
                String[] responseArray = responseType.split("\\|");
                for (String response : responseArray) {
                    responseNodeList.add(createIdentifierToken("\"" + response.trim() + "\""));
                    responseNodeList.add(createIdentifierToken("|"));

                }
                responseNodeList.remove(responseNodeList.size() - 1);

            } else {
                responseNodeList.add(createIdentifierToken("\"" + responseType + "\""));


            }
            MatchClauseNode matchClauseNode = createMatchClauseNode(createSeparatedNodeList(responseNodeList),
                    null, rightDoubleArrow,
                    createBlockStatementNode(openBraceToken, createNodeList(statementNodes), closeBraceToken));
            matchStatementList.add(matchClauseNode);


            ArrayList lockStatements = new ArrayList();
            FieldAccessExpressionNode selfRequestTypePipeNodes = createFieldAccessExpressionNode(
                    createSimpleNameReferenceNode(createIdentifierToken(SELF)), createToken(DOT_TOKEN),
                    requestTypePipeNode);

            MethodCallExpressionNode ensureTypeMethodCallExpressionNode = createMethodCallExpressionNode(
                    selfRequestTypePipeNodes, dotToken,
                    createSimpleNameReferenceNode(createIdentifierToken("ensureType")),
                    openParenToken, createSeparatedNodeList(), closeParenToken);

            CheckExpressionNode checkExpressionNode = createCheckExpressionNode(null, createToken(CHECK_KEYWORD),
                    ensureTypeMethodCallExpressionNode);
            AssignmentStatementNode ensureTypeAssignmentNode = createAssignmentStatementNode(requestTypePipeNode,
                    equalToken, checkExpressionNode, semicolonToken);


            // connectionInitMessagePipe = check self.connectionInitMessagePipe.ensureType();
            lockStatements.add(ensureTypeAssignmentNode);

            LockStatementNode functionLockStatementNode = createLockStatementNode(createToken(LOCK_KEYWORD),
                    createBlockStatementNode(openBraceToken, createNodeList(lockStatements),
                            closeBraceToken), null);

            //      lock {
            //            connectionInitMessagePipe = check self.connectionInitMessagePipe.ensureType();
            //        }
            statementsList.add(functionLockStatementNode);
        }


        SimpleNameReferenceNode responseNameNode = createSimpleNameReferenceNode(createIdentifierToken(
                responseType + ",error?"));
        SimpleNameReferenceNode streamMessageNode = createSimpleNameReferenceNode(createIdentifierToken(
                "streamMessages"));


//         stream<NextMessage|CompleteMessage|ErrorMessage> streamMessages;
        StreamTypeParamsNode streamTypeParamsNode = createStreamTypeParamsNode(createToken(LT_TOKEN),
                responseNameNode, null, null, createToken(GT_TOKEN));
        StreamTypeDescriptorNode streamTypeDescriptorNode = createStreamTypeDescriptorNode(
                createToken(STREAM_KEYWORD), streamTypeParamsNode);

        VariableDeclarationNode streamMessages = createVariableDeclarationNode(createEmptyNodeList(), null,

                createTypedBindingPatternNode(
                        streamTypeDescriptorNode,
                        createFieldBindingPatternVarnameNode(streamMessageNode)),
                //TODO: Findout [] node
                null, null, semicolonToken);

        //
        statementsList.add(streamMessages);


        //  lock {
        //     StreamGenerator streamGenerator = check new (subscribeMessagePipe, timeout);
        //     self.streamGenerators.addStreamGenerator(streamGenerator);
        //     streamMessages = new (streamGenerator);
        //  }
        ArrayList<StatementNode> streamStatementList = new ArrayList<>();
        String streamGenName = GeneratorUtils.getStreamGeneratorName(responseType);

        ArrayList<Node> streamGeneratorArguments = new ArrayList<>();
        streamGeneratorArguments.add(createPositionalArgumentNode(requestTypePipeNode));
        streamGeneratorArguments.add(createToken(COMMA_TOKEN));
        streamGeneratorArguments.add(createPositionalArgumentNode(createSimpleNameReferenceNode(createIdentifierToken(
                "timeout"))));
        CheckExpressionNode checkExpressionNode = createCheckExpressionNode(null, createToken(CHECK_KEYWORD),
                createImplicitNewExpressionNode(createToken(NEW_KEYWORD),
                        createParenthesizedArgList(openParenToken,
                                createSeparatedNodeList(streamGeneratorArguments), closeParenToken)));
//       createSimpleNameReferenceNode(createIdentifierToken("graphqlOverWebsocketStreamGenerator"));
        SimpleNameReferenceNode streamGeneratorNode = createSimpleNameReferenceNode(createIdentifierToken(
                "streamGenerator"));
        VariableDeclarationNode streamGenerator = createVariableDeclarationNode(createEmptyNodeList(),
                null, createTypedBindingPatternNode(createSimpleNameReferenceNode(createIdentifierToken(
                                streamGenName + "StreamGenerator"))

                        , createFieldBindingPatternVarnameNode(streamGeneratorNode)), equalToken, checkExpressionNode,
                semicolonToken);
        streamStatementList.add(streamGenerator);

        MethodCallExpressionNode streamGeneratorMethodCallNode = createMethodCallExpressionNode(
                createFieldAccessExpressionNode(createSimpleNameReferenceNode(createIdentifierToken(SELF)),
                        createToken(DOT_TOKEN), createSimpleNameReferenceNode(createIdentifierToken(
                                "streamGenerators"))),
                createToken(DOT_TOKEN), createSimpleNameReferenceNode(createIdentifierToken("addStreamGenerator")),
                openParenToken, createSeparatedNodeList(streamGeneratorNode), closeParenToken);
        ExpressionStatementNode streamGeneratorExpressionNode = createExpressionStatementNode(
                null, streamGeneratorMethodCallNode, semicolonToken);

        streamStatementList.add(streamGeneratorExpressionNode);


        AssignmentStatementNode streamMessagesAssignmentStatementNode = createAssignmentStatementNode(streamMessageNode,
                equalToken, createImplicitNewExpressionNode(createToken(NEW_KEYWORD),
                        createParenthesizedArgList(openParenToken, createSeparatedNodeList(


                                createPositionalArgumentNode(streamGeneratorNode)
                        ), closeParenToken))
                , semicolonToken);

        streamStatementList.add(streamMessagesAssignmentStatementNode);


        LockStatementNode streamLockStatementNode = createLockStatementNode(createToken(LOCK_KEYWORD),
                createBlockStatementNode(openBraceToken, createNodeList(streamStatementList), closeBraceToken),
                null);

        statementsList.add(streamLockStatementNode);


        //    return streamMessage
        Token returnKeyWord = createIdentifierToken("return");

        ReturnStatementNode returnStatementNode = createReturnStatementNode(returnKeyWord, streamMessageNode,
                createToken(SEMICOLON_TOKEN));
        statementsList.add(returnStatementNode);
    }

    private void addWriteMessageClosed(List<StatementNode> statementsList) {
        Token semicolonToken = createToken(SEMICOLON_TOKEN);
        Token openBraceToken = createToken(OPEN_BRACE_TOKEN);
        Token closeParenToken = createToken(CLOSE_PAREN_TOKEN);
        Token openParenToken = createToken(OPEN_PAREN_TOKEN);
        Token closeBraceToken = createToken(CLOSE_BRACE_TOKEN);

        MethodCallExpressionNode isClosedMethodCall = createMethodCallExpressionNode(createFieldAccessExpressionNode(
                        createSimpleNameReferenceNode(createIdentifierToken(SELF)),
                        createToken(DOT_TOKEN), createSimpleNameReferenceNode(
                                createIdentifierToken(WRITE_MESSAGE_QUEUE))),
                createToken(DOT_TOKEN), createSimpleNameReferenceNode(createIdentifierToken("isClosed")),
                openParenToken, createSeparatedNodeList(), closeParenToken);
        ReturnStatementNode errorReturnStatementNode = createReturnStatementNode(
                createToken(SyntaxKind.RETURN_KEYWORD), createErrorConstructorExpressionNode(createToken(ERROR_KEYWORD),
                        null, openParenToken, createSeparatedNodeList(
                                createIdentifierToken("\"" + CONNECTION_CLOSED + "\"")), closeParenToken),
                semicolonToken);
        IfElseStatementNode writeMessageQueueCheckNode = createIfElseStatementNode(createToken(IF_KEYWORD),
                isClosedMethodCall, createBlockStatementNode(openBraceToken, createNodeList(errorReturnStatementNode),
                        closeBraceToken), null);
        statementsList.add(writeMessageQueueCheckNode);
    }

    private void createNoResponseFunctionBodyStatement(List<StatementNode> statementsList, String requestType) {
        // check self.writeMessageQueue.produce(tuple, timeout);

        Token semicolonToken = createToken(SEMICOLON_TOKEN);
        Token dotToken = createToken(DOT_TOKEN);
        Token closeParenToken = createToken(CLOSE_PAREN_TOKEN);
        Token openParenToken = createToken(OPEN_PAREN_TOKEN);
        Token equalToken = createToken(EQUAL_TOKEN);


        SimpleNameReferenceNode messageTypeNode = createSimpleNameReferenceNode(
                createIdentifierToken(MESSAGE));
        SimpleNameReferenceNode messageVarNode = createSimpleNameReferenceNode(
                createIdentifierToken(MESSAGE_VAR_NAME));
        SimpleNameReferenceNode requestTypePipeNode = createSimpleNameReferenceNode(
                createIdentifierToken(requestType));

        addWriteMessageClosed(statementsList);

        MethodCallExpressionNode cloneWithTypeMethodCallExpressionNode = createMethodCallExpressionNode(
                requestTypePipeNode, dotToken,
                createSimpleNameReferenceNode(createIdentifierToken("cloneWithType")),
                openParenToken, createSeparatedNodeList(), closeParenToken);

        CheckExpressionNode cloneWithTypeCheck = createCheckExpressionNode(null, createToken(CHECK_KEYWORD),
                cloneWithTypeMethodCallExpressionNode);

        VariableDeclarationNode responseTypeCloneStatement = createVariableDeclarationNode(createEmptyNodeList(),
                null,
                createTypedBindingPatternNode(
                        messageTypeNode,
                        createFieldBindingPatternVarnameNode(
                                messageVarNode)),
                equalToken, cloneWithTypeCheck, semicolonToken);
        statementsList.add(responseTypeCloneStatement);

        List<Node> argumentArrays = new ArrayList<>();
        PositionalArgumentNode responseTypeTimeOut = createPositionalArgumentNode(
                createRequiredExpressionNode(createIdentifierToken("timeout")));
        argumentArrays.add(messageVarNode);
        argumentArrays.add(createToken(COMMA_TOKEN));
        argumentArrays.add(responseTypeTimeOut);
        FieldAccessExpressionNode globalQueue = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(SELF)), dotToken,
                createSimpleNameReferenceNode(createIdentifierToken(WRITE_MESSAGE_QUEUE)));
        CheckExpressionNode callGlobalQueueProduce = createCheckExpressionNode(null, createToken(CHECK_KEYWORD),
                createMethodCallExpressionNode(globalQueue, dotToken,
                        createSimpleNameReferenceNode(createIdentifierToken(PRODUCE)),
                        openParenToken, createSeparatedNodeList(
                                argumentArrays
                        ), closeParenToken));
        ExpressionStatementNode callGlobalQueueProduceNode = createExpressionStatementNode(null,
                callGlobalQueueProduce, semicolonToken);
        statementsList.add(callGlobalQueueProduceNode);
    }

    /**
     * Generate common statements in function body.
     */
    private void createSimpleRPCFunctionBodyStatements(List<StatementNode> statementsList, String requestType,
                                                       String responseType, String dispatcherStreamId,
                                                       List<MatchClauseNode> matchStatementList, boolean isSubscribe,
                                                       List<String> pipeNameMethods) {


        String requestTypePipe = requestType + "Pipe";
        //responseType substring
        String responseTypeCamelCaseName = null;
        if (responseType.contains(PIPE)) {
            responseTypeCamelCaseName = "unionResult";

        } else {
            char responseTypeFirstChar = Character.toLowerCase(responseType.charAt(0)); //Lowercase the first character
            String responseRemainingString = responseType.substring(1);
            responseTypeCamelCaseName = responseTypeFirstChar + responseRemainingString;
        }

        Token equalToken = createToken(EQUAL_TOKEN);
        Token semicolonToken = createToken(SEMICOLON_TOKEN);
        Token dotToken = createToken(DOT_TOKEN);
        Token closeParenToken = createToken(CLOSE_PAREN_TOKEN);
        Token openParenToken = createToken(OPEN_PAREN_TOKEN);
        Token openBraceToken = createToken(OPEN_BRACE_TOKEN);
        Token closeBraceToken = createToken(CLOSE_BRACE_TOKEN);
        Token rightDoubleArrow = createToken(RIGHT_DOUBLE_ARROW_TOKEN);

        SimpleNameReferenceNode requestTypePipeNode = createSimpleNameReferenceNode(
                createIdentifierToken(requestTypePipe));
        SimpleNameReferenceNode responseMessageNode = createSimpleNameReferenceNode(
                createIdentifierToken(MESSAGE_VAR_NAME));
        QualifiedNameReferenceNode pipeTypeCombined = createQualifiedNameReferenceNode(
                createIdentifierToken(SIMPLE_PIPE),
                createToken(COLON_TOKEN), createIdentifierToken(GeneratorConstants.CAPITAL_PIPE));

        //if (self.writeMessageQueue.isClosed()) {
        //    return error("");
        //}
        if (!isSubscribe) {
            addWriteMessageClosed(statementsList);
        }


        if (dispatcherStreamId != null) {
            List<Node> argumentsList = new ArrayList<>();
            //pipe:Pipe tuplePipe = new (1);
            argumentsList.add(createIdentifierToken("1"));
            SeparatedNodeList<FunctionArgumentNode> arguments = createSeparatedNodeList(argumentsList);

            ParenthesizedArgList parenthesizedArgList = createParenthesizedArgList(openParenToken,
                    arguments,
                    closeParenToken);
            ImplicitNewExpressionNode expressionNode = createImplicitNewExpressionNode(createToken(NEW_KEYWORD),
                    parenthesizedArgList);
            VariableDeclarationNode remotePipeTypeEnsureStatement = createVariableDeclarationNode(
                    createEmptyNodeList(), null,
                    createTypedBindingPatternNode(
                            pipeTypeCombined,
                            createFieldBindingPatternVarnameNode(requestTypePipeNode)),
                    equalToken, expressionNode, semicolonToken);
            statementsList.add(remotePipeTypeEnsureStatement);
        } else {

            //pipe:Pipe tuplePipe;
            VariableDeclarationNode remotePipeTypeEnsureStatement = createVariableDeclarationNode(createEmptyNodeList(),
                    null,
                    createTypedBindingPatternNode(
                            pipeTypeCombined,
                            createFieldBindingPatternVarnameNode(requestTypePipeNode)),
                    null, null, semicolonToken);
            statementsList.add(remotePipeTypeEnsureStatement);

        }

        //lock {
        //            id = uuid:createType1AsString();
        //            pingMessage.id = id;
        //        }
        createCommentStatementsForDispatcherId(statementsList, requestType, dispatcherStreamId, requestTypePipe,
                isSubscribe
        );


        //Create pipes using request Type names when there is no dispatcherStreamId
        if (dispatcherStreamId == null) {
            pipeNameMethods.add(requestTypePipe);
            ArrayList<StatementNode> statementNodes = new ArrayList<>();
            QualifiedNameReferenceNode pipeTypeName = createQualifiedNameReferenceNode(
                    createIdentifierToken(SIMPLE_PIPE),
                    createToken(COLON_TOKEN), createIdentifierToken(GeneratorConstants.CAPITAL_PIPE));
            FieldAccessExpressionNode selfPipes = createFieldAccessExpressionNode(
                    createSimpleNameReferenceNode(createIdentifierToken(SELF)), createToken(DOT_TOKEN),
                    createSimpleNameReferenceNode(createIdentifierToken(PIPES)));
            MethodCallExpressionNode methodCallExpressionNode = createMethodCallExpressionNode(selfPipes, dotToken,
                    createSimpleNameReferenceNode(createIdentifierToken("getPipe")),
                    openParenToken, createSeparatedNodeList(createSimpleNameReferenceNode(
                            createIdentifierToken("\"" + requestType + "\""))), closeParenToken);


            VariableDeclarationNode pipeTypeEnsureStatement = createVariableDeclarationNode(
                    createEmptyNodeList(), null,
                    createTypedBindingPatternNode(
                            pipeTypeName,
                            createFieldBindingPatternVarnameNode(requestTypePipeNode)),
                    equalToken, methodCallExpressionNode, semicolonToken);


            //pipe:Pipe pingMessagePipe = self.pipes.getPipe("pingMessage");
            statementNodes.add(pipeTypeEnsureStatement);

            List<Node> nodes = new ArrayList<>();
            nodes.add(responseMessageNode);
            nodes.add(createToken(COMMA_TOKEN));
            nodes.add(createIdentifierToken("5"));
            MethodCallExpressionNode pipeProduceExpressionNode = createMethodCallExpressionNode(
                    requestTypePipeNode, dotToken,
                    createSimpleNameReferenceNode(createIdentifierToken(PRODUCE)),
                    openParenToken, createSeparatedNodeList(nodes), closeParenToken);

            CheckExpressionNode pipeProduceCheck = createCheckExpressionNode(null, createToken(CHECK_KEYWORD),
                    pipeProduceExpressionNode);
            ExpressionStatementNode pipeProduceExpression = createExpressionStatementNode(null,
                    pipeProduceCheck, createToken(SEMICOLON_TOKEN));


            //check pingMessagePipe.produce(message, 5);
            statementNodes.add(pipeProduceExpression);


            MatchClauseNode matchClauseNode = createMatchClauseNode(createSeparatedNodeList(
                            createIdentifierToken("\"" + responseType + "\"")), null, rightDoubleArrow,
                    createBlockStatementNode(openBraceToken, createNodeList(statementNodes), closeBraceToken));

            // "PongMessage" => {
            //      pipe:Pipe pingMessagePipe = self.pipes.getPipe("pingMessage");
            //      check pingMessagePipe.produce(message, 5);
            // }
            matchStatementList.add(matchClauseNode);


//        if (!isSubscribe) {
//
//
//            MethodCallExpressionNode cloneWithTypeMethodCallExpressionNode = createMethodCallExpressionNode(
//                    requestTypeNode, dotToken,
//                    createSimpleNameReferenceNode(createIdentifierToken("cloneWithType")),
//                    openParenToken, createSeparatedNodeList(), closeParenToken);
//
//            CheckExpressionNode cloneWithTypeCheck = createCheckExpressionNode(null, createToken(CHECK_KEYWORD),
//                    cloneWithTypeMethodCallExpressionNode);
//
//            VariableDeclarationNode responseTypeCloneStatement = createVariableDeclarationNode(createEmptyNodeList(),
//                    null,
//                    createTypedBindingPatternNode(
//                            responseMessageTypeNode,
//                            createFieldBindingPatternVarnameNode(
//                                    responseMessageNode)),
//                    equalToken, cloneWithTypeCheck, semicolonToken);
//            //Message message = check pingMessage.cloneWithType();
//            statementsList.add(responseTypeCloneStatement);
//
//
//
//            List<Node> argumentArrays = new ArrayList<>();
//
//            argumentArrays.add(responseMessageNode);
//            argumentArrays.add(createToken(COMMA_TOKEN));
//            argumentArrays.add(responseTypeTimeOut);
//            FieldAccessExpressionNode globalQueue = createFieldAccessExpressionNode(
//                    createSimpleNameReferenceNode(createIdentifierToken(SELF)), dotToken,
//                    createSimpleNameReferenceNode(createIdentifierToken(WRITE_MESSAGE_QUEUE)));
//            CheckExpressionNode callGlobalQueueProduce = createCheckExpressionNode(null,
//                    createToken(CHECK_KEYWORD),
//                    createMethodCallExpressionNode(globalQueue, dotToken,
//                            createSimpleNameReferenceNode(createIdentifierToken(PRODUCE)),
//                            openParenToken, createSeparatedNodeList(
//                                    argumentArrays
//                            ), closeParenToken));
//            ExpressionStatementNode callGlobalQueueProduceNode = createExpressionStatementNode(
//                    null, callGlobalQueueProduce, semicolonToken);
//
//            // check self.writeMessageQueue.produce(tuple, timeout);
//            statementsList.add(callGlobalQueueProduceNode);
//        }


            ArrayList lockStatements = new ArrayList();
            FieldAccessExpressionNode selfRequestTypePipeNodes = createFieldAccessExpressionNode(
                    createSimpleNameReferenceNode(createIdentifierToken(SELF)), createToken(DOT_TOKEN),
                    requestTypePipeNode);

            MethodCallExpressionNode ensureTypeMethodCallExpressionNode = createMethodCallExpressionNode(
                    selfRequestTypePipeNodes, dotToken,
                    createSimpleNameReferenceNode(createIdentifierToken("ensureType")),
                    openParenToken, createSeparatedNodeList(), closeParenToken);

            CheckExpressionNode checkExpressionNode = createCheckExpressionNode(null, createToken(CHECK_KEYWORD),
                    ensureTypeMethodCallExpressionNode);
            AssignmentStatementNode ensureTypeAssignmentNode = createAssignmentStatementNode(requestTypePipeNode,
                    equalToken, checkExpressionNode, semicolonToken);


            // connectionInitMessagePipe = check self.connectionInitMessagePipe.ensureType();
            lockStatements.add(ensureTypeAssignmentNode);

            LockStatementNode functionLockStatementNode = createLockStatementNode(createToken(LOCK_KEYWORD),
                    createBlockStatementNode(openBraceToken, createNodeList(lockStatements),
                            closeBraceToken), null);

            //      lock {
            //            connectionInitMessagePipe = check self.connectionInitMessagePipe.ensureType();
            //        }
            statementsList.add(functionLockStatementNode);
        }


        SimpleNameReferenceNode responseTypeName = createSimpleNameReferenceNode(createIdentifierToken(responseType));
        SimpleNameReferenceNode anydata = createSimpleNameReferenceNode(createIdentifierToken("anydata"));
        SimpleNameReferenceNode responseMessageVarNode = createSimpleNameReferenceNode(createIdentifierToken(
                "responseMessage"));
        SimpleNameReferenceNode responseNameNode;
        if (!requestType.equals("error")) {
            responseNameNode = createSimpleNameReferenceNode(createIdentifierToken(responseTypeCamelCaseName));
        } else {
            responseNameNode = createSimpleNameReferenceNode(createIdentifierToken(
                    responseTypeCamelCaseName + "Message"));

        }

        PositionalArgumentNode responseTypeTimeOut = createPositionalArgumentNode(
                createRequiredExpressionNode(createIdentifierToken("timeout")));

        CheckExpressionNode callRelevantPipeConsumeNode = createCheckExpressionNode(null,
                createToken(CHECK_KEYWORD),
                createMethodCallExpressionNode(requestTypePipeNode, dotToken,
                        createSimpleNameReferenceNode(createIdentifierToken(CONSUME)),
                        openParenToken, createSeparatedNodeList(
                                responseTypeTimeOut
                        ), closeParenToken));
        VariableDeclarationNode callRelevantPipeProduceVar = createVariableDeclarationNode(createEmptyNodeList(),
                null,
                createTypedBindingPatternNode(
                        anydata,
                        createFieldBindingPatternVarnameNode(responseMessageVarNode)),
                equalToken, callRelevantPipeConsumeNode, semicolonToken);

        // anydata responseMessage = check tuplePipe.consume(timeout);
        statementsList.add(callRelevantPipeProduceVar);

        if (dispatcherStreamId != null) {
            MethodCallExpressionNode gracefulMethodCallNode = createMethodCallExpressionNode(
                    requestTypePipeNode, dotToken,
                    createSimpleNameReferenceNode(createIdentifierToken("gracefulClose")),
                    openParenToken, createSeparatedNodeList(), closeParenToken);
            CheckExpressionNode graceFulCheckNode = createCheckExpressionNode(null,
                    createToken(CHECK_KEYWORD), gracefulMethodCallNode);
            ExpressionStatementNode graceFulCheckExpressionNode = createExpressionStatementNode(
                    null, graceFulCheckNode, semicolonToken);


            statementsList.add(graceFulCheckExpressionNode);
        }


        //PongMessage pongMessage = check responseMessage.cloneWithType();
        MethodCallExpressionNode cloneWithTypeMethodCallExpressionNode = createMethodCallExpressionNode(
                responseMessageVarNode, dotToken,
                createSimpleNameReferenceNode(createIdentifierToken("cloneWithType")),
                openParenToken, createSeparatedNodeList(), closeParenToken);

        CheckExpressionNode cloneWithTypeCheck = createCheckExpressionNode(null, createToken(CHECK_KEYWORD),
                cloneWithTypeMethodCallExpressionNode);

        VariableDeclarationNode responseTypeCloneStatement = createVariableDeclarationNode(createEmptyNodeList(),
                null,
                createTypedBindingPatternNode(
                        responseTypeName,
                        createFieldBindingPatternVarnameNode(
                                responseNameNode)),
                equalToken, cloneWithTypeCheck, semicolonToken);


        statementsList.add(responseTypeCloneStatement);

        Token returnKeyWord = createIdentifierToken("return");

        ReturnStatementNode returnStatementNode = createReturnStatementNode(returnKeyWord, responseNameNode,
                createToken(SEMICOLON_TOKEN));
        statementsList.add(returnStatementNode);
    }

}
