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
package io.ballerina.asyncapi.generator.ws.client.node.client;

import com.fasterxml.jackson.databind.JsonNode;
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.generator.ws.client.generator.DocCommentsGenerator;
import io.ballerina.asyncapi.generator.ws.client.generator.RemoteFunctionReturnTypeGenerator;
import io.ballerina.asyncapi.generator.ws.client.utils.CodegenUtils;
import io.ballerina.compiler.syntax.tree.FunctionBodyNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.IfElseStatementNode;
import io.ballerina.compiler.syntax.tree.MarkdownDocumentationNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createSeparatedNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBasicLiteralNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBlockStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBreakStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createElseBlockNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFieldAccessExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFieldBindingPatternVarnameNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionBodyBlockNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionDefinitionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionSignatureNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createIfElseStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createLockStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMarkdownDocumentationNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMetadataNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMethodCallExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createNamedWorkerDeclarationNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createOptionalTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createPositionalArgumentNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRequiredExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createReturnStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTypedBindingPatternNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createUnionTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createVariableDeclarationNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createWhileStatementNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.BOOLEAN_LITERAL;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.BREAK_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.COMMA_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.DOT_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ELSE_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.EQUAL_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FUNCTION_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.IF_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ISOLATED_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.LOCK_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OBJECT_METHOD_DEFINITION;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PIPE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PRIVATE_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.QUESTION_MARK_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RETURN_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RIGHT_ARROW_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SEMICOLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.TRUE_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.WHILE_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.WORKER_KEYWORD;

/**
 * Builds the {@code startMessageWriting} and {@code startMessageReading} worker function nodes
 * for a WebSocket client class.
 */
public class ClientWorkerGenerator {

    private static final String X_BALLERINA_MESSAGE_WRITE_DESCRIPTION = "x-ballerina-write-message-description";
    private static final String X_BALLERINA_MESSAGE_READ_DESCRIPTION = "x-ballerina-read-message-description";
    private static final String START_MESSAGE_WRITING = "startMessageWriting";
    private static final String START_MESSAGE_READING = "startMessageReading";
    private static final String START_MESSAGE_WRITING_DESCRIPTION = "Used to write messages to the websocket.";
    private static final String START_MESSAGE_READING_DESCRIPTION = "Used to read messages from the websocket.";
    private static final String IS_ACTIVE = "isActive";
    private static final String MESSAGE_TYPE = "Message";
    private static final String PIPE_ERROR_TYPE = "pipe:Error";
    private static final String MESSAGE_VAR = "message";
    private static final String WRITE_MESSAGE_QUEUE = "writeMessageQueue";
    private static final String ATTEMPT_TO_CLOSE_CONNECTION = "attemptToCloseConnection";
    private static final String WS_ERROR_TYPE = "websocket:Error";
    private static final String CLIENT_EP = "clientEp";
    private static final String WS_ERR_VAR = "wsErr";
    private static final String PIPE_VAR = "pipe";
    private static final String MESSAGE_WITH_ID = "MessageWithId";
    private static final String PIPES = "pipes";
    private static final String PIPE_NAME_VAR = "pipeName";
    private static final String PIPE_ERR_VAR = "pipeErr";

    // Token constants
    private static final Token OPEN_PAREN = createToken(OPEN_PAREN_TOKEN);
    private static final Token CLOSE_PAREN = createToken(CLOSE_PAREN_TOKEN);
    private static final Token OPEN_BRACE = createToken(OPEN_BRACE_TOKEN);
    private static final Token CLOSE_BRACE = createToken(CLOSE_BRACE_TOKEN);
    private static final Token SEMICOLON = createToken(SEMICOLON_TOKEN);
    private static final Token EQUAL = createToken(EQUAL_TOKEN);
    private static final Token DOT = createToken(DOT_TOKEN);
    private static final Token RIGHT_ARROW = createToken(RIGHT_ARROW_TOKEN);

    // Worker/method names
    private static final String WRITE_MESSAGE = "writeMessage";
    private static final String READ_MESSAGE = "readMessage";
    private static final String CONSUME = "consume";
    private static final String PRODUCE = "produce";
    private static final String GET_PIPE_NAME = "getPipeName";
    private static final String GET_PIPE = "getPipe";
    private static final String DEFAULT_TIMEOUT = "5";

    // Error messages for log:printError calls
    private static final String LOG_PRINT_ERR = "log:printError(%s);";
    private static final String WRITE_PIPE_ERR_MSG = "\"PipeError: Failed to consume message from the pipe\", %s";
    private static final String WRITE_WS_ERR_MSG = "\"WsError: Failed to write message to the client\", %s";
    private static final String READ_WS_ERR_MSG = "\"WsError: Failed to read message from the client\", %s";

    // Misc string fragments
    private static final String SELF = "self";
    private static final String NOT = "!";
    private static final String IS = " is ";
    private static final String MESSAGE_WITH_ID_VAR = "messageWithId";
    private static final String FRAME_VAR = "frame";
    private static final String PARSED_MESSAGE_VAR = "parsedMessage";
    private static final String FRAME_PIPE_ERR_VAR = "framePipeErr";
    private static final String PIPE_COLON_PIPE = "pipe:Pipe";
    private static final String PIPE_COLON_ERROR = "pipe:Error";
    private static final String OP_TIMEOUT = "\"Operation has timed out\"";
    private static final String CONTINUE_STMT = "continue;";

    private final AsyncApiSpec asyncApiSpec;
    private final String dispatcherKey;
    private final String dispatcherStreamId;
    private final boolean routeFramesWithoutDispatcher;
    private final boolean keepFramesForReceiveOnly;

    /**
     * Creates a new client worker generator.
     *
     * @param asyncApiSpec       the parsed AsyncAPI spec
     * @param dispatcherKey      the dispatcher key field name
     * @param dispatcherStreamId optional stream ID field name (may be {@code null})
     */
    public ClientWorkerGenerator(AsyncApiSpec asyncApiSpec, String dispatcherKey, String dispatcherStreamId) {
        this(asyncApiSpec, dispatcherKey, dispatcherStreamId, false);
    }

    /**
     * Creates a new client worker generator.
     *
     * @param asyncApiSpec                  the parsed AsyncAPI spec
     * @param dispatcherKey                 the dispatcher key field name
     * @param dispatcherStreamId            optional stream ID field name (may be {@code null})
     * @param routeFramesWithoutDispatcher  whether some reply has no dispatcher key, so the reader must keep
     *                                      such frames for it instead of closing the connection
     */
    public ClientWorkerGenerator(AsyncApiSpec asyncApiSpec, String dispatcherKey, String dispatcherStreamId,
                                 boolean routeFramesWithoutDispatcher) {
        this(asyncApiSpec, dispatcherKey, dispatcherStreamId, routeFramesWithoutDispatcher, false);
    }

    /**
     * Creates a new client worker generator.
     *
     * @param asyncApiSpec                  the parsed AsyncAPI spec
     * @param dispatcherKey                 the dispatcher key field name
     * @param dispatcherStreamId            optional stream ID field name (may be {@code null})
     * @param routeFramesWithoutDispatcher  whether some reply has no dispatcher key, so the reader must keep
     *                                      such frames for it instead of closing the connection
     * @param keepFramesForReceiveOnly      whether a receive-only function reads such frames, so one that
     *                                      arrives while no call waits for a reply is kept for it
     */
    public ClientWorkerGenerator(AsyncApiSpec asyncApiSpec, String dispatcherKey, String dispatcherStreamId,
                                 boolean routeFramesWithoutDispatcher, boolean keepFramesForReceiveOnly) {
        this.asyncApiSpec = asyncApiSpec;
        this.dispatcherKey = dispatcherKey;
        this.dispatcherStreamId = dispatcherStreamId;
        this.routeFramesWithoutDispatcher = routeFramesWithoutDispatcher;
        this.keepFramesForReceiveOnly = routeFramesWithoutDispatcher && keepFramesForReceiveOnly;
    }

    /**
     * Builds the {@code startMessageWriting} private worker function node.
     *
     * @return the function definition node
     */
    public FunctionDefinitionNode buildStartMessageWriting() {
        List<StatementNode> whileStatements = new ArrayList<>();
        whileStatements.add(getIsActiveCheck());

        // Message|pipe:Error message = self.writeMessageQueue.consume(5);
        VariableDeclarationNode queueData = createVariableDeclarationNode(createEmptyNodeList(), null,
                createTypedBindingPatternNode(
                        createUnionTypeDescriptorNode(
                                createSimpleNameReferenceNode(createIdentifierToken(MESSAGE_TYPE)),
                                createToken(PIPE_TOKEN),
                                NodeParser.parseTypeDescriptor(PIPE_COLON_ERROR)),
                        createFieldBindingPatternVarnameNode(
                                createSimpleNameReferenceNode(createIdentifierToken(MESSAGE_VAR)))),
                EQUAL,
                createMethodCallExpressionNode(
                        createFieldAccessExpressionNode(
                                createSimpleNameReferenceNode(createIdentifierToken(SELF)), DOT,
                                createSimpleNameReferenceNode(createIdentifierToken(WRITE_MESSAGE_QUEUE))),
                        DOT,
                        createSimpleNameReferenceNode(createIdentifierToken(CONSUME)),
                        OPEN_PAREN,
                        createSeparatedNodeList(createPositionalArgumentNode(
                                createRequiredExpressionNode(createIdentifierToken(DEFAULT_TIMEOUT)))),
                        CLOSE_PAREN),
                SEMICOLON);
        whileStatements.add(queueData);
        whileStatements.add(getIsPipeError(MESSAGE_VAR, WRITE_PIPE_ERR_MSG, true));

        // websocket:Error? wsErr = self.clientEp->writeMessage(message);
        VariableDeclarationNode wsWrite = createVariableDeclarationNode(createEmptyNodeList(), null,
                createTypedBindingPatternNode(
                        createOptionalTypeDescriptorNode(
                                NodeParser.parseTypeDescriptor(WS_ERROR_TYPE),
                                createToken(QUESTION_MARK_TOKEN)),
                        createFieldBindingPatternVarnameNode(
                                createSimpleNameReferenceNode(createIdentifierToken(WS_ERR_VAR)))),
                EQUAL,
                createMethodCallExpressionNode(
                        createFieldAccessExpressionNode(
                                createSimpleNameReferenceNode(createIdentifierToken(SELF)), DOT,
                                createSimpleNameReferenceNode(createIdentifierToken(CLIENT_EP))),
                        RIGHT_ARROW,
                        createSimpleNameReferenceNode(createIdentifierToken(WRITE_MESSAGE)),
                        OPEN_PAREN,
                        createSeparatedNodeList(createPositionalArgumentNode(
                                createRequiredExpressionNode(createIdentifierToken(MESSAGE_VAR)))),
                        CLOSE_PAREN),
                SEMICOLON);
        whileStatements.add(wsWrite);
        whileStatements.add(getIsWsError(WRITE_WS_ERR_MSG, WS_ERR_VAR));

        return buildWorkerFunction(START_MESSAGE_WRITING, WRITE_MESSAGE,
                getDocCommentsForWorker(X_BALLERINA_MESSAGE_WRITE_DESCRIPTION, START_MESSAGE_WRITING_DESCRIPTION),
                whileStatements);
    }

    /**
     * Builds the {@code startMessageReading} private worker function node.
     *
     * @return the function definition node
     */
    public FunctionDefinitionNode buildStartMessageReading() {
        List<StatementNode> whileStatements = new ArrayList<>();
        whileStatements.add(getIsActiveCheck());
        if (routeFramesWithoutDispatcher) {
            addFrameWithoutDispatcherRoute(whileStatements);
        } else {
            addMessageRead(whileStatements);
        }

        String escapedDispKey = CodegenUtils.escapeIdentifier(dispatcherKey);
        if (dispatcherStreamId != null) {
            addStreamIdBranch(whileStatements, escapedDispKey);
        } else {
            // string? pipeName = self.getPipeName(message.<dispKey>);
            whileStatements.add(NodeParser.parseStatement(
                    "string? " + PIPE_NAME_VAR + " = self." + GET_PIPE_NAME
                    + "(" + MESSAGE_VAR + "." + escapedDispKey + ");"));
            whileStatements.add(getDropUnroutedStatement(escapedDispKey));
            // pipe:Pipe pipe = self.pipes.getPipe(pipeName);
            VariableDeclarationNode pipesVar = createVariableDeclarationNode(createEmptyNodeList(), null,
                    createTypedBindingPatternNode(
                            NodeParser.parseTypeDescriptor(PIPE_COLON_PIPE),
                            createFieldBindingPatternVarnameNode(
                                    createSimpleNameReferenceNode(createIdentifierToken(PIPE_VAR)))),
                    EQUAL,
                    NodeParser.parseExpression("self." + PIPES + "." + GET_PIPE + "(" + PIPE_NAME_VAR + ")"),
                    SEMICOLON);
            whileStatements.add(pipesVar);
        }

        // pipe:Error? pipeErr = pipe.produce(message, 5);
        VariableDeclarationNode pipeErrVar = createVariableDeclarationNode(createEmptyNodeList(), null,
                createTypedBindingPatternNode(
                        createOptionalTypeDescriptorNode(
                                NodeParser.parseTypeDescriptor(PIPE_COLON_ERROR),
                                createToken(QUESTION_MARK_TOKEN)),
                        createFieldBindingPatternVarnameNode(
                                createSimpleNameReferenceNode(createIdentifierToken(PIPE_ERR_VAR)))),
                EQUAL,
                createMethodCallExpressionNode(
                        createSimpleNameReferenceNode(createIdentifierToken(PIPE_VAR)),
                        DOT,
                        createSimpleNameReferenceNode(createIdentifierToken(PRODUCE)),
                        OPEN_PAREN,
                        createSeparatedNodeList(
                                createPositionalArgumentNode(createRequiredExpressionNode(
                                        createIdentifierToken(MESSAGE_VAR))),
                                createToken(COMMA_TOKEN),
                                createPositionalArgumentNode(createRequiredExpressionNode(
                                        createIdentifierToken(DEFAULT_TIMEOUT)))),
                        CLOSE_PAREN),
                SEMICOLON);
        whileStatements.add(pipeErrVar);
        whileStatements.add(getDropFrameStatement(PIPE_ERR_VAR));

        return buildWorkerFunction(START_MESSAGE_READING, READ_MESSAGE,
                getDocCommentsForWorker(X_BALLERINA_MESSAGE_READ_DESCRIPTION, START_MESSAGE_READING_DESCRIPTION),
                whileStatements);
    }

    /**
     * Reads each frame as text and parses it into {@code Message}. The websocket client reports a frame it can't
     * bind as a plain {@code websocket:Error}, like a broken connection, so parsing here keeps the two apart: a frame
     * that isn't a {@code Message} is logged and dropped, and only a read error closes the connection.
     */
    private static void addMessageRead(List<StatementNode> whileStatements) {
        whileStatements.add(NodeParser.parseStatement(String.format("string|%s %s = self.%s->%s(string);",
                WS_ERROR_TYPE, FRAME_VAR, CLIENT_EP, READ_MESSAGE)));
        whileStatements.add(getIsWsError(READ_WS_ERR_MSG, FRAME_VAR));
        whileStatements.add(NodeParser.parseStatement(String.format("%s|error %s = %s.fromJsonStringWithType();",
                MESSAGE_TYPE, PARSED_MESSAGE_VAR, FRAME_VAR)));
        whileStatements.add(NodeParser.parseStatement(String.format("if %s is error { log:printWarn(\"Dropped a "
                + "message that doesn't match the spec\", 'error = %s); continue; }",
                PARSED_MESSAGE_VAR, PARSED_MESSAGE_VAR)));
        whileStatements.add(NodeParser.parseStatement(String.format("%s %s = %s;",
                MESSAGE_TYPE, MESSAGE_VAR, PARSED_MESSAGE_VAR)));
    }

    /**
     * Reads the frame as text. One that isn't a JSON message with the dispatcher key goes, as is, to the reply pipe
     * while a call waits for a reply there, so a frame from before the request is never taken as its reply. At any
     * other time it goes to the receive-only functions' pipe, or is dropped if there are none. The first such frame
     * while a call waits can't be told apart from the reply, so it is taken as the reply; claiming it ends the wait,
     * so a second frame can't queue behind it for a later call.
     */
    private void addFrameWithoutDispatcherRoute(List<StatementNode> whileStatements) {
        whileStatements.add(NodeParser.parseStatement(String.format("string|%s %s = self.%s->%s(string);",
                WS_ERROR_TYPE, FRAME_VAR, CLIENT_EP, READ_MESSAGE)));
        whileStatements.add(getIsWsError(READ_WS_ERR_MSG, FRAME_VAR));
        whileStatements.add(NodeParser.parseStatement(String.format("%s|error %s = %s.fromJsonStringWithType();",
                MESSAGE_TYPE, PARSED_MESSAGE_VAR, FRAME_VAR)));
        String replyPipe = CodegenUtils.toStringLiteral(RemoteFunctionReturnTypeGenerator.NO_DISPATCHER_KEY_PIPE);
        String awaited = String.format("self.%s.claimReply(%s)", PIPES, replyPipe);
        List<StatementNode> ifStmts = new ArrayList<>();
        String framePipe;
        if (keepFramesForReceiveOnly) {
            framePipe = String.format("%s ? %s : %s", awaited, replyPipe,
                    CodegenUtils.toStringLiteral(RemoteFunctionReturnTypeGenerator.NO_DISPATCHER_KEY_EVENTS_PIPE));
        } else {
            ifStmts.add(NodeParser.parseStatement(String.format("if !%s { log:printWarn(\"Dropped a message that "
                    + "no call is waiting for\"); continue; }", awaited)));
            framePipe = replyPipe;
        }
        ifStmts.add(NodeParser.parseStatement(String.format("%s? %s = self.%s.%s(%s).%s(%s, %s);",
                PIPE_COLON_ERROR, FRAME_PIPE_ERR_VAR, PIPES, GET_PIPE, framePipe, PRODUCE, FRAME_VAR,
                DEFAULT_TIMEOUT)));
        ifStmts.add(getDropFrameStatement(FRAME_PIPE_ERR_VAR));
        ifStmts.add(NodeParser.parseStatement(CONTINUE_STMT));
        whileStatements.add(createIfElseStatementNode(createToken(IF_KEYWORD),
                NodeParser.parseExpression(PARSED_MESSAGE_VAR + " is error"),
                createBlockStatementNode(OPEN_BRACE, createNodeList(ifStmts), CLOSE_BRACE),
                null));
        whileStatements.add(NodeParser.parseStatement(String.format("%s %s = %s;",
                MESSAGE_TYPE, MESSAGE_VAR, PARSED_MESSAGE_VAR)));
    }

    /**
     * A frame its pipe can't take within the wait is logged and dropped. The pipe is full because no call is reading
     * it, and closing the connection for that would end every other call too.
     */
    private static StatementNode getDropFrameStatement(String errVar) {
        return NodeParser.parseStatement(String.format("if %s is %s { log:printWarn(\"Dropped a message because no "
                + "call is reading its pipe\", 'error = %s); continue; }", errVar, PIPE_COLON_ERROR, errVar));
    }

    /**
     * A frame whose dispatcher value has no pipe is logged and dropped, since no call waits for it. Queuing it on a
     * pipe nobody reads would fill that pipe and end with the reader closing the connection.
     */
    private static StatementNode getDropUnroutedStatement(String escapedDispKey) {
        return NodeParser.parseStatement(String.format(
                "if %s is () { log:printWarn(\"Dropped a message that no call is waiting for\", "
                        + "dispatcherValue = %s.%s); continue; }", PIPE_NAME_VAR, MESSAGE_VAR, escapedDispKey));
    }

    private void addStreamIdBranch(List<StatementNode> whileStatements, String escapedDispKey) {
        String escapedStreamId = CodegenUtils.escapeIdentifier(dispatcherStreamId);
        // pipe:Pipe pipe;
        whileStatements.add(NodeParser.parseStatement(PIPE_COLON_PIPE + " " + PIPE_VAR + ";"));
        // MessageWithId|error messageWithId = message.cloneWithType(MessageWithId);
        whileStatements.add(NodeParser.parseStatement(
                MESSAGE_WITH_ID + "|error " + MESSAGE_WITH_ID_VAR + " = "
                + MESSAGE_VAR + ".cloneWithType(" + MESSAGE_WITH_ID + ");"));
        StatementNode getPipeNameStmt = NodeParser.parseStatement(
                "string? " + PIPE_NAME_VAR + " = self." + GET_PIPE_NAME
                + "(" + MESSAGE_VAR + "." + escapedDispKey + ");");
        IfElseStatementNode pipeConditional = createIfElseStatementNode(createToken(IF_KEYWORD),
                NodeParser.parseExpression(MESSAGE_WITH_ID_VAR + IS + MESSAGE_WITH_ID),
                createBlockStatementNode(OPEN_BRACE,
                        createNodeList(NodeParser.parseStatement(
                                PIPE_VAR + " = self." + PIPES + "." + GET_PIPE
                                + "(" + MESSAGE_WITH_ID_VAR + "." + escapedStreamId + ");")),
                        CLOSE_BRACE),
                createElseBlockNode(createToken(ELSE_KEYWORD),
                        createBlockStatementNode(OPEN_BRACE,
                                createNodeList(getPipeNameStmt, getDropUnroutedStatement(escapedDispKey),
                                        NodeParser.parseStatement(PIPE_VAR + " = self." + PIPES
                                                + "." + GET_PIPE + "(" + PIPE_NAME_VAR + ");")),
                                CLOSE_BRACE)));
        whileStatements.add(pipeConditional);
    }

    private static FunctionDefinitionNode buildWorkerFunction(String funcName, String workerName,
                                                              MetadataNode metadata,
                                                              List<StatementNode> whileStatements) {
        FunctionSignatureNode signature = createFunctionSignatureNode(
                OPEN_PAREN, createSeparatedNodeList(), CLOSE_PAREN, null);
        @SuppressWarnings("unchecked")
        FunctionBodyNode body = createFunctionBodyBlockNode(OPEN_BRACE, null,
                (io.ballerina.compiler.syntax.tree.NodeList<StatementNode>) (Object) createNodeList(
                        createNamedWorkerDeclarationNode(
                                createEmptyNodeList(), null,
                                createToken(WORKER_KEYWORD), createIdentifierToken(workerName), null,
                                createBlockStatementNode(OPEN_BRACE,
                                        createNodeList(createWhileStatementNode(
                                                createToken(WHILE_KEYWORD),
                                                createBasicLiteralNode(BOOLEAN_LITERAL, createToken(TRUE_KEYWORD)),
                                                createBlockStatementNode(OPEN_BRACE,
                                                        createNodeList(whileStatements), CLOSE_BRACE),
                                                null)),
                                        CLOSE_BRACE),
                                null)),
                CLOSE_BRACE, null);
        return createFunctionDefinitionNode(OBJECT_METHOD_DEFINITION, metadata,
                createNodeList(createToken(PRIVATE_KEYWORD), createToken(ISOLATED_KEYWORD)),
                createToken(FUNCTION_KEYWORD), createIdentifierToken(funcName),
                createEmptyNodeList(), signature, body);
    }

    private static StatementNode getIsActiveCheck() {
        return createLockStatementNode(createToken(LOCK_KEYWORD),
                createBlockStatementNode(OPEN_BRACE,
                        createNodeList(createIfElseStatementNode(createToken(IF_KEYWORD),
                                createSimpleNameReferenceNode(createIdentifierToken(
                                        NOT + SELF + "." + IS_ACTIVE)),
                                createBlockStatementNode(OPEN_BRACE,
                                        createNodeList(createBreakStatementNode(
                                                createToken(BREAK_KEYWORD), SEMICOLON)),
                                        CLOSE_BRACE),
                                null)),
                        CLOSE_BRACE),
                null);
    }

    private static IfElseStatementNode getIsWsError(String errMsg, String errVar) {
        List<StatementNode> ifStmts = new ArrayList<>();
        ifStmts.add(getStopIfClosedStatement());
        ifStmts.add(NodeParser.parseStatement(String.format(LOG_PRINT_ERR, String.format(errMsg, errVar))));
        ifStmts.add(NodeParser.parseStatement("self." + ATTEMPT_TO_CLOSE_CONNECTION + "();"));
        ifStmts.add(createReturnStatementNode(createToken(RETURN_KEYWORD), null, SEMICOLON));
        return createIfElseStatementNode(createToken(IF_KEYWORD),
                createSimpleNameReferenceNode(createIdentifierToken(errVar + IS + WS_ERROR_TYPE)),
                createBlockStatementNode(OPEN_BRACE, createNodeList(ifStmts), CLOSE_BRACE),
                null);
    }

    private static IfElseStatementNode getIsPipeError(String errVar, String errMsg, boolean checkTimeout) {
        List<StatementNode> ifStmts = new ArrayList<>();
        if (checkTimeout) {
            ifStmts.add(createIfElseStatementNode(createToken(IF_KEYWORD),
                    NodeParser.parseExpression(errVar + ".message() == " + OP_TIMEOUT),
                    createBlockStatementNode(OPEN_BRACE,
                            createNodeList(NodeParser.parseStatement(CONTINUE_STMT)),
                            CLOSE_BRACE),
                    null));
        }
        ifStmts.add(getStopIfClosedStatement());
        ifStmts.add(NodeParser.parseStatement(String.format(LOG_PRINT_ERR, String.format(errMsg, errVar))));
        ifStmts.add(NodeParser.parseStatement("self." + ATTEMPT_TO_CLOSE_CONNECTION + "();"));
        ifStmts.add(createReturnStatementNode(createToken(RETURN_KEYWORD), null, SEMICOLON));
        return createIfElseStatementNode(createToken(IF_KEYWORD),
                createSimpleNameReferenceNode(createIdentifierToken(errVar + IS + PIPE_ERROR_TYPE)),
                createBlockStatementNode(OPEN_BRACE, createNodeList(ifStmts), CLOSE_BRACE),
                null);
    }

    /**
     * After a deliberate close the socket and pipes report errors that are expected, so a worker stops quietly
     * instead of logging them and closing again.
     */
    private static StatementNode getStopIfClosedStatement() {
        return NodeParser.parseStatement("lock { if !self." + IS_ACTIVE + " { return; } }");
    }

    private MetadataNode getDocCommentsForWorker(String extensionKey, String defaultDesc) {
        String description = defaultDesc;
        Map<String, JsonNode> extensions = asyncApiSpec.getAsyncApiExtensions().orElse(null);
        if (extensions != null && extensions.containsKey(extensionKey)) {
            description = defaultDesc.concat(extensions.get(extensionKey).asText());
        }
        List<Node> docs = new ArrayList<>(DocCommentsGenerator.createAPIDescriptionDoc(description, true));
        MarkdownDocumentationNode docNode = createMarkdownDocumentationNode(createNodeList(docs));
        return createMetadataNode(docNode, createEmptyNodeList());
    }
}
