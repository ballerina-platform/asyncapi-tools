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

package io.ballerina.asyncapi.websocketscore.generators.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.ballerina.asyncapi.websocketscore.GeneratorUtils;
import io.ballerina.compiler.syntax.tree.AssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionStatementNode;
import io.ballerina.compiler.syntax.tree.FieldAccessExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.FunctionBodyNode;
import io.ballerina.compiler.syntax.tree.ImplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.LockStatementNode;
import io.ballerina.compiler.syntax.tree.MethodCallExpressionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.compiler.syntax.tree.ParenthesizedArgList;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.ReturnStatementNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.StreamTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.StreamTypeParamsNode;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.ADD_PIPE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.ATTEMPT_CON_CLOSE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.CAPITAL_PIPE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.CLONE_WITH_TYPE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.CONNECTION_CLOSED_TEMPLATE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.CONSUME;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.CONSUMING;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.CREATE_TYPE1_AS_STRING;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.DATABINDING_ERR_TEMPLATE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.DOT;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.ERROR;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.GET_PIPE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.GRACEFUL_CLOSE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.IS;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.IS_ACTIVE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.MESSAGE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.MESSAGE_VAR_NAME;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.NOT;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.PIPE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.PIPES;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.PIPE_ERR;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.PIPE_ERROR;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.PIPE_PRODUCE_ERR_TEMPLATE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.PRODUCE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.PRODUCING;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.RESPONSE_MESSAGE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.RETURN;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.SELF;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.SERVER_STREAMING;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.SIMPLE_PIPE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.SPACE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.STREAM_GENERATOR;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.STRING;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.TIMEOUT;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.UUID;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.WITHIN_PAREN_TEMPLATE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.WRITE_MESSAGE_QUEUE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.X_RESPONSE_TYPE;
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
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMethodCallExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createOptionalTypeDescriptorNode;
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
import static io.ballerina.compiler.syntax.tree.SyntaxKind.QUESTION_MARK_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RETURN_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SEMICOLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.STREAM_KEYWORD;

/**
 * This Util class uses for generating remote function body  {@link FunctionBodyNode}.
 *
 * @since 1.3.0
 */
public class RemoteFunctionBodyGenerator {

    private final List<ImportDeclarationNode> imports;
    private final String functionName;

    private static final Token openParenToken = createToken(OPEN_PAREN_TOKEN);
    private static final Token closeParenToken = createToken(CLOSE_PAREN_TOKEN);
    private static final Token openBraceToken = createToken(OPEN_BRACE_TOKEN);
    private static final Token closeBraceToken = createToken(CLOSE_BRACE_TOKEN);
    private static final Token semicolonToken = createToken(SEMICOLON_TOKEN);
    private static final Token equalToken = createToken(EQUAL_TOKEN);
    private static final Token dotToken = createToken(DOT_TOKEN);
    private static final Token colonToken = createToken(COLON_TOKEN);

    public RemoteFunctionBodyGenerator(List<ImportDeclarationNode> imports, String functionName) {
        this.imports = Collections.unmodifiableList(imports);
        this.functionName = functionName;
    }

    private void addProduceStatementForWriteMessageQueue(List<StatementNode> statementsList, String requestType,
                                                         String dispatcherStreamId, String requestTypePipe,
                                                         boolean isSubscribe) {

        SimpleNameReferenceNode requestTypePipeNode = createSimpleNameReferenceNode(
                createIdentifierToken(requestTypePipe));

        ArrayList<StatementNode> lockStatements = new ArrayList<>();

        //Create remote function body when dispatcherStreamId is present
        if (dispatcherStreamId != null) {
            SimpleNameReferenceNode dispatcherStreamIdNode = createSimpleNameReferenceNode(
                    createIdentifierToken(dispatcherStreamId));
            VariableDeclarationNode dispatcherStreamIdDefineNode = createVariableDeclarationNode(
                    createEmptyNodeList(), null,
                    createTypedBindingPatternNode(createSimpleNameReferenceNode(createIdentifierToken(STRING)),
                            createFieldBindingPatternVarnameNode(dispatcherStreamIdNode)), null, null,
                    semicolonToken);
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
                    createBlockStatementNode(openBraceToken, createNodeList(lockStatements), closeBraceToken), null);

            statementsList.add(functionLockStatementNode);

            FieldAccessExpressionNode remoteSelfPipes = createFieldAccessExpressionNode(
                    createSimpleNameReferenceNode(createIdentifierToken(SELF)), dotToken,
                    createSimpleNameReferenceNode(createIdentifierToken(PIPES)));

            MethodCallExpressionNode addPipeMethodCallExpressionNode = createMethodCallExpressionNode(remoteSelfPipes,
                    dotToken, createSimpleNameReferenceNode(createIdentifierToken(ADD_PIPE)),
                    openParenToken, createSeparatedNodeList(dispatcherStreamIdNode, createToken(COMMA_TOKEN),
                            requestTypePipeNode), closeParenToken);
            ExpressionStatementNode addPipeByIdStatement = createExpressionStatementNode(null,
                    addPipeMethodCallExpressionNode, semicolonToken);

            // self.pipes.addPipe(id, subscribePipe);
            statementsList.add(addPipeByIdStatement);
        }

        if (!isSubscribe) {
            SimpleNameReferenceNode requestTypeNameNode =
                    createSimpleNameReferenceNode(createIdentifierToken(requestType));
            VariableDeclarationNode messageVariableDeclarationNode =
                    createVariableDeclarationNode(createEmptyNodeList(), null,
                            createTypedBindingPatternNode(NodeParser.parseTypeDescriptor(MESSAGE + PIPE + ERROR),
                                    createCaptureBindingPatternNode(createIdentifierToken(MESSAGE_VAR_NAME))),
                            equalToken, createMethodCallExpressionNode(requestTypeNameNode, dotToken,
                                    createSimpleNameReferenceNode(createIdentifierToken(CLONE_WITH_TYPE)),
                                    openParenToken, createSeparatedNodeList(), closeParenToken), semicolonToken);

            // Message|error message = subscribe.cloneWithType();
            statementsList.add(messageVariableDeclarationNode);
            statementsList.add(getCloningMessageError(MESSAGE_VAR_NAME));
            statementsList.addAll(getProduceToWriteMessageQueueVar());
        }
    }

    private List<StatementNode> getProduceToWriteMessageQueueVar() {
        // pipe:Error? pipeErr = self.writeMessageQueue.produce(message, timeout);
        List<StatementNode> statements = new ArrayList<>();
        List<Node> arguments = new ArrayList<>();
        arguments.add(createSimpleNameReferenceNode(createIdentifierToken(MESSAGE_VAR_NAME)));
        arguments.add(createToken(COMMA_TOKEN));
        arguments.add(createPositionalArgumentNode(createRequiredExpressionNode(createIdentifierToken(TIMEOUT))));
        FieldAccessExpressionNode globalQueue = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(SELF)), dotToken,
                createSimpleNameReferenceNode(createIdentifierToken(WRITE_MESSAGE_QUEUE)));
        MethodCallExpressionNode callGlobalQueueProduce =
                createMethodCallExpressionNode(globalQueue, dotToken,
                        createSimpleNameReferenceNode(createIdentifierToken(PRODUCE)), openParenToken,
                        createSeparatedNodeList(arguments), closeParenToken);
        statements.add(createVariableDeclarationNode(createEmptyNodeList(),
                null, createTypedBindingPatternNode(createOptionalTypeDescriptorNode(PIPE_ERROR,
                        createToken(QUESTION_MARK_TOKEN)), createFieldBindingPatternVarnameNode(
                                createSimpleNameReferenceNode(createIdentifierToken(PIPE_ERR)))),
                        equalToken, callGlobalQueueProduce, semicolonToken));
        statements.add(getPipeError(PIPE_ERR, PRODUCING));
        return statements;
    }

    private StatementNode getCloningMessageError(String varName) {
        return createIfElseStatementNode(createToken(IF_KEYWORD),
                NodeParser.parseExpression(varName + SPACE + IS + SPACE + ERROR),
                createBlockStatementNode(openBraceToken,
                        createNodeList(ATTEMPT_CON_CLOSE, createReturnStatementNode(
                                        createToken(RETURN_KEYWORD), createErrorConstructorExpressionNode(
                                                createToken(ERROR_KEYWORD), null, openParenToken,
                                        createSeparatedNodeList(createIdentifierToken(String.format(
                                                DATABINDING_ERR_TEMPLATE, this.functionName))), closeParenToken),
                                        semicolonToken)), closeBraceToken), null);
    }

    private StatementNode getPipeError(String errVar, String activity) {
        return createIfElseStatementNode(createToken(IF_KEYWORD),
                NodeParser.parseExpression(errVar + SPACE + IS + SPACE + PIPE_ERROR),
                createBlockStatementNode(openBraceToken,
                        createNodeList(ATTEMPT_CON_CLOSE, createReturnStatementNode(createToken(RETURN_KEYWORD),
                                createErrorConstructorExpressionNode(createToken(ERROR_KEYWORD), null,
                                        openParenToken, createSeparatedNodeList(createIdentifierToken(String.format(
                                                PIPE_PRODUCE_ERR_TEMPLATE, this.functionName, activity))),
                                        closeParenToken), semicolonToken)), closeBraceToken), null);
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
     */
    public FunctionBodyNode getFunctionBodyNode(Map<String, JsonNode> extensions, String requestType,
                                                String specDispatcherStreamId, boolean isSubscribe,
                                                String responseType) {

        // Create statements
        List<StatementNode> statementsList = new ArrayList<>();

        // This return type for target data type binding.

        if (extensions != null) {
//            JsonNode xResponse = extensions.get(X_RESPONSE);
            JsonNode xResponseType = extensions.get(X_RESPONSE_TYPE);

            if (xResponseType != null && xResponseType.equals(new TextNode(SERVER_STREAMING))) {
                //TODO: Include an if condition to check this only one time
//                utilGenerator.setStreamFound(true);
                createStreamFunctionBodyStatements(statementsList, requestType, responseType, specDispatcherStreamId,
                        isSubscribe);
            } else {
                createSimpleRPCFunctionBodyStatements(statementsList, requestType, responseType, specDispatcherStreamId,
                        isSubscribe);
            }
        } else {
            createNoResponseFunctionBodyStatement(statementsList, requestType);
        }
        //Create statements
        NodeList<StatementNode> statements = createNodeList(statementsList);
        return createFunctionBodyBlockNode(openBraceToken, null, statements, closeBraceToken, null);
    }

    private void createStreamFunctionBodyStatements(List<StatementNode> statementsList,
                                                    String requestType, String responseType, String dispatcherStreamId,
                                                    boolean isSubscribe) {

        String requestTypePipe = requestType + "Pipe";

        String requestTypePipeNode = SELF + DOT + PIPES + DOT + GET_PIPE + String.format(WITHIN_PAREN_TEMPLATE,
                String.format("\"%s\"", requestType));
        QualifiedNameReferenceNode pipeTypeCombined = createQualifiedNameReferenceNode(
                createIdentifierToken(SIMPLE_PIPE), colonToken, createIdentifierToken(CAPITAL_PIPE));

        if (!isSubscribe) {
            statementsList.add(getConnectionActiveCheck());
        }

        //pipe:Pipe tuplePipe = new (10000);
        if (dispatcherStreamId != null) {
            List<Node> argumentsList = new ArrayList<>();
            argumentsList.add(createIdentifierToken("10000"));
            SeparatedNodeList<FunctionArgumentNode> arguments = createSeparatedNodeList(argumentsList);
            ParenthesizedArgList parenthesizedArgList = createParenthesizedArgList(openParenToken, arguments,
                    closeParenToken);
            ImplicitNewExpressionNode expressionNode = createImplicitNewExpressionNode(createToken(NEW_KEYWORD),
                    parenthesizedArgList);
            VariableDeclarationNode remotePipeTypeEnsureStatement = createVariableDeclarationNode(createEmptyNodeList(),
                    null, createTypedBindingPatternNode(pipeTypeCombined,
                            NodeParser.parseBindingPattern(requestTypePipeNode)), equalToken, expressionNode,
                    semicolonToken);
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
        addProduceStatementForWriteMessageQueue(statementsList, requestType, dispatcherStreamId, requestTypePipe,
                isSubscribe);

        SimpleNameReferenceNode responseNameNode = createSimpleNameReferenceNode(createIdentifierToken(
                responseType + ",error?"));
        SimpleNameReferenceNode streamMessageNode = createSimpleNameReferenceNode(createIdentifierToken(
                "streamMessages"));


//         stream<NextMessage|CompleteMessage|ErrorMessage> streamMessages;
        StreamTypeParamsNode streamTypeParamsNode = createStreamTypeParamsNode(createToken(LT_TOKEN),
                responseNameNode, null, null, createToken(GT_TOKEN));
        StreamTypeDescriptorNode streamTypeDescriptorNode = createStreamTypeDescriptorNode(createToken(STREAM_KEYWORD),
                streamTypeParamsNode);

        VariableDeclarationNode streamMessages = createVariableDeclarationNode(createEmptyNodeList(), null,
                createTypedBindingPatternNode(streamTypeDescriptorNode, createFieldBindingPatternVarnameNode(
                        streamMessageNode)), //TODO: Findout [] node
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
        streamGeneratorArguments.add(createPositionalArgumentNode(NodeParser.parseExpression(requestTypePipeNode)));
        streamGeneratorArguments.add(createToken(COMMA_TOKEN));
        streamGeneratorArguments.add(createPositionalArgumentNode(createSimpleNameReferenceNode(createIdentifierToken(
                TIMEOUT))));
        ImplicitNewExpressionNode newExpressionNode = createImplicitNewExpressionNode(createToken(NEW_KEYWORD),
                        createParenthesizedArgList(openParenToken, createSeparatedNodeList(streamGeneratorArguments),
                                closeParenToken));
        SimpleNameReferenceNode streamGeneratorNode = createSimpleNameReferenceNode(createIdentifierToken(
                STREAM_GENERATOR));
        VariableDeclarationNode streamGenerator = createVariableDeclarationNode(createEmptyNodeList(),
                null, createTypedBindingPatternNode(createSimpleNameReferenceNode(createIdentifierToken(
                                streamGenName + "StreamGenerator")),
                        createFieldBindingPatternVarnameNode(streamGeneratorNode)), equalToken, newExpressionNode,
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
                                createPositionalArgumentNode(streamGeneratorNode)), closeParenToken)), semicolonToken);

        streamStatementList.add(streamMessagesAssignmentStatementNode);


        LockStatementNode streamLockStatementNode = createLockStatementNode(createToken(LOCK_KEYWORD),
                createBlockStatementNode(openBraceToken, createNodeList(streamStatementList), closeBraceToken), null);

        statementsList.add(streamLockStatementNode);

        ReturnStatementNode returnStatementNode = createReturnStatementNode(createToken(RETURN_KEYWORD),
                streamMessageNode, semicolonToken);
        statementsList.add(returnStatementNode);
    }

    private LockStatementNode getConnectionActiveCheck() {
        //        lock {
        //            if !self.isActive {
        //                return error ("[functionName]ConnectionError: Connection has been closed");
        //            }
        //        }
        NodeList<StatementNode> ifIsActiveNode = createNodeList(createIfElseStatementNode(createToken(IF_KEYWORD),
                createSimpleNameReferenceNode(createIdentifierToken(NOT + SELF + DOT + IS_ACTIVE)),
                createBlockStatementNode(openBraceToken, createNodeList(createReturnStatementNode(
                        createToken(RETURN_KEYWORD), createErrorConstructorExpressionNode(createToken(ERROR_KEYWORD),
                                null, openParenToken, createSeparatedNodeList(createIdentifierToken(
                                        String.format(CONNECTION_CLOSED_TEMPLATE, this.functionName))),
                                closeParenToken),
                        semicolonToken)), closeBraceToken), null));
        return createLockStatementNode(createToken(LOCK_KEYWORD),
                createBlockStatementNode(openBraceToken, ifIsActiveNode, closeBraceToken), null);
    }

    private void createNoResponseFunctionBodyStatement(List<StatementNode> statementsList, String requestType) {
        // check self.writeMessageQueue.produce(tuple, timeout);

        TypeDescriptorNode messageTypeNode = NodeParser.parseTypeDescriptor(MESSAGE + PIPE + ERROR);
        SimpleNameReferenceNode messageVarNode = createSimpleNameReferenceNode(
                createIdentifierToken(MESSAGE_VAR_NAME));
        SimpleNameReferenceNode requestTypePipeNode = createSimpleNameReferenceNode(
                createIdentifierToken(requestType));

        statementsList.add(getConnectionActiveCheck());

        MethodCallExpressionNode cloneWithTypeMethodCallExpressionNode = createMethodCallExpressionNode(
                requestTypePipeNode, dotToken, createSimpleNameReferenceNode(createIdentifierToken(CLONE_WITH_TYPE)),
                openParenToken, createSeparatedNodeList(), closeParenToken);

        VariableDeclarationNode responseTypeCloneStatement = createVariableDeclarationNode(createEmptyNodeList(),
                null,
                createTypedBindingPatternNode(messageTypeNode, createFieldBindingPatternVarnameNode(messageVarNode)),
                equalToken, cloneWithTypeMethodCallExpressionNode, semicolonToken);

        statementsList.add(responseTypeCloneStatement);
        statementsList.add(getCloningMessageError(MESSAGE_VAR_NAME));
        statementsList.addAll(getProduceToWriteMessageQueueVar());
    }

    /**
     * Generate common statements in function body.
     */
    private void createSimpleRPCFunctionBodyStatements(List<StatementNode> statementsList, String requestType,
                                                       String responseType, String dispatcherStreamId,
                                                       boolean isSubscribe) {


        String requestTypePipe = requestType + "Pipe";
        //responseType substring
        String responseTypeCamelCaseName;
        if (responseType.contains(PIPE)) {
            responseTypeCamelCaseName = "unionResult";
        } else {
            char responseTypeFirstChar = Character.toLowerCase(responseType.charAt(0)); //Lowercase the first character
            String responseRemainingString = responseType.substring(1);
            responseTypeCamelCaseName = responseTypeFirstChar + responseRemainingString;
        }

        SimpleNameReferenceNode requestTypePipeNode = createSimpleNameReferenceNode(
                createIdentifierToken(requestTypePipe));
        QualifiedNameReferenceNode pipeTypeCombined = createQualifiedNameReferenceNode(
                createIdentifierToken(SIMPLE_PIPE), createToken(COLON_TOKEN), createIdentifierToken(CAPITAL_PIPE));

        if (!isSubscribe) {
            statementsList.add(getConnectionActiveCheck());
        }

        if (dispatcherStreamId != null) {
            List<Node> argumentsList = new ArrayList<>();
            //pipe:Pipe tuplePipe = new (1);
            argumentsList.add(createIdentifierToken("1"));
            SeparatedNodeList<FunctionArgumentNode> arguments = createSeparatedNodeList(argumentsList);

            ParenthesizedArgList parenthesizedArgList = createParenthesizedArgList(openParenToken, arguments,
                    closeParenToken);
            ImplicitNewExpressionNode expressionNode = createImplicitNewExpressionNode(createToken(NEW_KEYWORD),
                    parenthesizedArgList);
            VariableDeclarationNode remotePipeTypeEnsureStatement = createVariableDeclarationNode(
                    createEmptyNodeList(), null,
                    createTypedBindingPatternNode(pipeTypeCombined, createFieldBindingPatternVarnameNode(
                            requestTypePipeNode)), equalToken, expressionNode, semicolonToken);
            statementsList.add(remotePipeTypeEnsureStatement);
        }

        addProduceStatementForWriteMessageQueue(statementsList, requestType, dispatcherStreamId, requestTypePipe,
                isSubscribe);

        TypeDescriptorNode responseTypeName = NodeParser.parseTypeDescriptor(responseType + PIPE + ERROR);
        TypeDescriptorNode consumeResponseType = NodeParser.parseTypeDescriptor(MESSAGE + PIPE + PIPE_ERROR);
        SimpleNameReferenceNode responseMessageVarNode = createSimpleNameReferenceNode(createIdentifierToken(
                RESPONSE_MESSAGE));
        SimpleNameReferenceNode responseNameNode;
        if (!requestType.equals(ERROR)) {
            responseNameNode = createSimpleNameReferenceNode(createIdentifierToken(responseTypeCamelCaseName));
        } else {
            responseNameNode = createSimpleNameReferenceNode(createIdentifierToken(
                    responseTypeCamelCaseName + MESSAGE));
        }

        VariableDeclarationNode callRelevantPipeConsumeVar = createVariableDeclarationNode(createEmptyNodeList(),
                null, createTypedBindingPatternNode(consumeResponseType,
                        createFieldBindingPatternVarnameNode(responseMessageVarNode)), equalToken,
                NodeParser.parseExpression(SELF + DOT + PIPES + DOT + GET_PIPE +
                        String.format(WITHIN_PAREN_TEMPLATE, String.format("\"%s\"", requestType)) + DOT + CONSUME +
                        String.format(WITHIN_PAREN_TEMPLATE, TIMEOUT)), semicolonToken);
        // Message|pipe:Error responseMessage = tuplePipe.consume(timeout);
        statementsList.add(callRelevantPipeConsumeVar);
        statementsList.add(getPipeError(RESPONSE_MESSAGE, CONSUMING));

        if (dispatcherStreamId != null) {
            MethodCallExpressionNode gracefulMethodCallNode = createMethodCallExpressionNode(
                    requestTypePipeNode, dotToken,
                    createSimpleNameReferenceNode(createIdentifierToken(GRACEFUL_CLOSE)),
                    openParenToken, createSeparatedNodeList(), closeParenToken);
            CheckExpressionNode graceFulCheckNode = createCheckExpressionNode(null,
                    createToken(CHECK_KEYWORD), gracefulMethodCallNode);
            ExpressionStatementNode graceFulCheckExpressionNode = createExpressionStatementNode(
                    null, graceFulCheckNode, semicolonToken);
            statementsList.add(graceFulCheckExpressionNode);
        }

        //PongMessage pongMessage = responseMessage.cloneWithType();
        MethodCallExpressionNode cloneWithTypeMethodCallExpressionNode = createMethodCallExpressionNode(
                responseMessageVarNode, dotToken,
                createSimpleNameReferenceNode(createIdentifierToken(CLONE_WITH_TYPE)),
                openParenToken, createSeparatedNodeList(), closeParenToken);

        VariableDeclarationNode responseTypeCloneStatement = createVariableDeclarationNode(createEmptyNodeList(),
                null, createTypedBindingPatternNode(responseTypeName, createFieldBindingPatternVarnameNode(
                        responseNameNode)), equalToken, cloneWithTypeMethodCallExpressionNode, semicolonToken);

        statementsList.add(responseTypeCloneStatement);
        statementsList.add(getCloningMessageError(responseTypeCamelCaseName));

        ReturnStatementNode returnStatementNode = createReturnStatementNode(createIdentifierToken(RETURN),
                responseNameNode, semicolonToken);
        statementsList.add(returnStatementNode);
    }

}
