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

import io.ballerina.compiler.syntax.tree.FunctionBodyNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.LockStatementNode;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.ReturnTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.Token;

import java.util.ArrayList;
import java.util.List;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createSeparatedNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBlockStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionBodyBlockNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionDefinitionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionSignatureNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createIfElseStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createLockStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMetadataNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRequiredParameterNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createReturnTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FUNCTION_DEFINITION;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FUNCTION_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.IF_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ISOLATED_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.LOCK_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OBJECT_METHOD_DEFINITION;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PRIVATE_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.REMOTE_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RETURNS_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SEMICOLON_TOKEN;

/**
 * Builds the helper function nodes for a WebSocket client class:
 * {@code getRecordName}, {@code getPipeName}, {@code attemptToCloseConnection},
 * and {@code connectionClose}.
 */
public class ClientHelperGenerator {

    // getRecordName
    private static final String GET_RECORD_NAME = "getRecordName";
    private static final String STRING_TYPE = "string";
    private static final String DISPATCHING_VALUE = "dispatchingValue";
    private static final String WORDS_STMT =
            "string[] words = regexp:split(re `[\\W_]+`, dispatchingValue);";
    private static final String RESULT_INIT_STMT = "string result = \"\";";
    private static final String FOREACH_WORDS_STMT =
            "foreach string word in words { result +="
            + " word.substring(0, 1).toUpperAscii() + word.substring(1).toLowerAscii(); }";
    private static final String RETURN_RESULT_STMT = "return result;";

    // getPipeName
    private static final String GET_PIPE_NAME = "getPipeName";
    private static final String RESPONSE_TYPE = "responseType";
    private static final String RESPONSE_RECORD_TYPE_STMT =
            "string responseRecordType = self.getRecordName(responseType);";
    private static final String RESPONSE_MAP_HAS_KEY_EXPR = "self.responseMap.hasKey(responseRecordType)";
    private static final String RETURN_RESPONSE_MAP_GET_STMT =
            "return self.responseMap.get(responseRecordType);";
    private static final String RETURN_RESPONSE_TYPE_STMT = "return responseType;";

    // attemptToCloseConnection / connectionClose
    private static final String ATTEMPT_TO_CLOSE_CONNECTION = "attemptToCloseConnection";
    private static final String CONNECTION_CLOSE = "connectionClose";
    private static final String CONNECTION_ERR = "ConnectionError";
    private static final String IS_ACTIVE = "isActive";
    private static final String WRITE_MESSAGE_QUEUE = "writeMessageQueue";
    private static final String PIPES = "pipes";
    private static final String REMOVE_PIPES = "removePipes";
    private static final String STREAM_GENERATORS = "streamGenerators";
    private static final String REMOVE_STREAM_GENERATORS = "removeStreamGenerators";
    private static final String CLIENT_EP = "clientEp";
    private static final String OPTIONAL_ERROR = "error?";
    private static final String CONNECTION_CLOSE_STATEMENT = "error? connectionClose = self->connectionClose();";
    private static final String LOG_PRINT_ERR_TEMPLATE = "log:printError(\"%s\", connectionClose);";
    private static final String IS_ACTIVE_FALSE_STATEMENT = "self." + IS_ACTIVE + " = false;";
    private static final String WRITE_QUEUE_CLOSE_STATEMENT =
            "check self." + WRITE_MESSAGE_QUEUE + ".immediateClose();";
    private static final String PIPES_REMOVE_STATEMENT = "check self." + PIPES + "." + REMOVE_PIPES + "();";
    private static final String STREAM_GENERATORS_REMOVE_STATEMENT =
            "check self." + STREAM_GENERATORS + "." + REMOVE_STREAM_GENERATORS + "();";
    private static final String CLIENT_EP_CLOSE_STATEMENT = "check self." + CLIENT_EP + "->close();";

    private static final Token OPEN_PAREN = createToken(OPEN_PAREN_TOKEN);
    private static final Token CLOSE_PAREN = createToken(CLOSE_PAREN_TOKEN);
    private static final Token OPEN_BRACE = createToken(OPEN_BRACE_TOKEN);
    private static final Token CLOSE_BRACE = createToken(CLOSE_BRACE_TOKEN);

    private final boolean streamsPresent;

    /**
     * Creates a new client helper generator.
     *
     * @param streamsPresent whether stream generators are in use
     */
    public ClientHelperGenerator(boolean streamsPresent) {
        this.streamsPresent = streamsPresent;
    }

    /**
     * Builds the {@code getRecordName} private helper function node.
     *
     * @return the function definition node
     */
    public FunctionDefinitionNode buildGetRecordName() {
        RequiredParameterNode param = createRequiredParameterNode(createNodeList(),
                createSimpleNameReferenceNode(createIdentifierToken(STRING_TYPE)),
                createIdentifierToken(DISPATCHING_VALUE));
        SimpleNameReferenceNode returnTypeNode = createSimpleNameReferenceNode(createIdentifierToken(STRING_TYPE));
        ReturnTypeDescriptorNode returnTypeDesc = createReturnTypeDescriptorNode(
                createToken(RETURNS_KEYWORD), createEmptyNodeList(), returnTypeNode);
        FunctionSignatureNode signature = createFunctionSignatureNode(OPEN_PAREN,
                createSeparatedNodeList(param), CLOSE_PAREN, returnTypeDesc);
        List<StatementNode> statements = new ArrayList<>();
        statements.add(NodeParser.parseStatement(WORDS_STMT));
        statements.add(NodeParser.parseStatement(RESULT_INIT_STMT));
        statements.add(NodeParser.parseStatement(FOREACH_WORDS_STMT));
        statements.add(NodeParser.parseStatement(RETURN_RESULT_STMT));
        FunctionBodyNode body = createFunctionBodyBlockNode(OPEN_BRACE, null,
                createNodeList(statements), CLOSE_BRACE, null);
        return createFunctionDefinitionNode(OBJECT_METHOD_DEFINITION, null,
                createNodeList(createToken(PRIVATE_KEYWORD), createToken(ISOLATED_KEYWORD)),
                createToken(FUNCTION_KEYWORD), createIdentifierToken(GET_RECORD_NAME),
                createEmptyNodeList(), signature, body);
    }

    /**
     * Builds the {@code getPipeName} private helper function node.
     *
     * @return the function definition node
     */
    public FunctionDefinitionNode buildGetPipeName() {
        RequiredParameterNode param = createRequiredParameterNode(createNodeList(),
                createSimpleNameReferenceNode(createIdentifierToken(STRING_TYPE)),
                createIdentifierToken(RESPONSE_TYPE));
        SimpleNameReferenceNode returnTypeNode = createSimpleNameReferenceNode(createIdentifierToken(STRING_TYPE));
        ReturnTypeDescriptorNode returnTypeDesc = createReturnTypeDescriptorNode(
                createToken(RETURNS_KEYWORD), createEmptyNodeList(), returnTypeNode);
        FunctionSignatureNode signature = createFunctionSignatureNode(OPEN_PAREN,
                createSeparatedNodeList(param), CLOSE_PAREN, returnTypeDesc);
        List<StatementNode> statements = new ArrayList<>();
        statements.add(NodeParser.parseStatement(RESPONSE_RECORD_TYPE_STMT));
        statements.add(createIfElseStatementNode(createToken(IF_KEYWORD),
                NodeParser.parseExpression(RESPONSE_MAP_HAS_KEY_EXPR),
                createBlockStatementNode(OPEN_BRACE,
                        createNodeList(NodeParser.parseStatement(RETURN_RESPONSE_MAP_GET_STMT)),
                        CLOSE_BRACE), null));
        statements.add(NodeParser.parseStatement(RETURN_RESPONSE_TYPE_STMT));
        FunctionBodyNode body = createFunctionBodyBlockNode(OPEN_BRACE, null,
                createNodeList(statements), CLOSE_BRACE, null);
        return createFunctionDefinitionNode(OBJECT_METHOD_DEFINITION, null,
                createNodeList(createToken(PRIVATE_KEYWORD), createToken(ISOLATED_KEYWORD)),
                createToken(FUNCTION_KEYWORD), createIdentifierToken(GET_PIPE_NAME),
                createEmptyNodeList(), signature, body);
    }

    /**
     * Builds the {@code attemptToCloseConnection} function node using NodeFactory.
     *
     * @return the function definition node
     */
    public FunctionDefinitionNode buildAttemptToCloseConnection() {
        List<StatementNode> statements = new ArrayList<>();
        statements.add(NodeParser.parseStatement(CONNECTION_CLOSE_STATEMENT));
        statements.add(createIfElseStatementNode(createToken(IF_KEYWORD),
                NodeParser.parseExpression(CONNECTION_CLOSE + " is error"),
                createBlockStatementNode(OPEN_BRACE, createNodeList(NodeParser.parseStatement(
                        String.format(LOG_PRINT_ERR_TEMPLATE, CONNECTION_ERR))), CLOSE_BRACE), null));
        return createFunctionDefinitionNode(FUNCTION_DEFINITION, null,
                createNodeList(createToken(ISOLATED_KEYWORD)), createToken(FUNCTION_KEYWORD),
                createIdentifierToken(ATTEMPT_TO_CLOSE_CONNECTION), createEmptyNodeList(),
                createFunctionSignatureNode(OPEN_PAREN, createSeparatedNodeList(), CLOSE_PAREN, null),
                createFunctionBodyBlockNode(OPEN_BRACE, null, createNodeList(statements), CLOSE_BRACE, null));
    }

    /**
     * Builds the {@code connectionClose} remote function node using NodeFactory.
     *
     * @return the function definition node
     */
    public FunctionDefinitionNode buildConnectionClose() {
        List<StatementNode> lockStatements = new ArrayList<>();
        lockStatements.add(NodeParser.parseStatement(IS_ACTIVE_FALSE_STATEMENT));
        lockStatements.add(NodeParser.parseStatement(WRITE_QUEUE_CLOSE_STATEMENT));
        lockStatements.add(NodeParser.parseStatement(PIPES_REMOVE_STATEMENT));
        if (streamsPresent) {
            lockStatements.add(NodeParser.parseStatement(STREAM_GENERATORS_REMOVE_STATEMENT));
        }
        lockStatements.add(NodeParser.parseStatement(CLIENT_EP_CLOSE_STATEMENT));
        LockStatementNode lockStatement = createLockStatementNode(createToken(LOCK_KEYWORD),
                createBlockStatementNode(OPEN_BRACE, createNodeList(lockStatements), CLOSE_BRACE), null);
        SimpleNameReferenceNode returnTypeNode = createSimpleNameReferenceNode(
                createIdentifierToken(OPTIONAL_ERROR));
        ReturnTypeDescriptorNode returnTypeDesc = createReturnTypeDescriptorNode(
                createToken(RETURNS_KEYWORD), createEmptyNodeList(), returnTypeNode);
        FunctionSignatureNode signature = createFunctionSignatureNode(OPEN_PAREN,
                createSeparatedNodeList(), CLOSE_PAREN, returnTypeDesc);
        FunctionBodyNode body = createFunctionBodyBlockNode(OPEN_BRACE, null,
                createNodeList(lockStatement), CLOSE_BRACE, createToken(SEMICOLON_TOKEN));
        return createFunctionDefinitionNode(OBJECT_METHOD_DEFINITION,
                createMetadataNode(null, createEmptyNodeList()),
                createNodeList(createToken(REMOTE_KEYWORD), createToken(ISOLATED_KEYWORD)),
                createToken(FUNCTION_KEYWORD), createIdentifierToken(CONNECTION_CLOSE),
                createEmptyNodeList(), signature, body);
    }
}
