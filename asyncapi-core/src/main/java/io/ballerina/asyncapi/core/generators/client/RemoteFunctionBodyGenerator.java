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
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25DocumentImpl;
import io.ballerina.asyncapi.core.GeneratorConstants;
import io.ballerina.asyncapi.core.exception.BallerinaAsyncApiException;
import io.ballerina.compiler.syntax.tree.AssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionStatementNode;
import io.ballerina.compiler.syntax.tree.FieldAccessExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.FunctionBodyNode;
import io.ballerina.compiler.syntax.tree.ImplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.IndexedExpressionNode;
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
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.ballerina.asyncapi.core.GeneratorConstants.CONSUME;
import static io.ballerina.asyncapi.core.GeneratorConstants.CREATE_TYPE1_AS_STRING;
import static io.ballerina.asyncapi.core.GeneratorConstants.ENSURE_TYPE;
import static io.ballerina.asyncapi.core.GeneratorConstants.PIPE;
import static io.ballerina.asyncapi.core.GeneratorConstants.PIPES;
import static io.ballerina.asyncapi.core.GeneratorConstants.PRODUCE;
import static io.ballerina.asyncapi.core.GeneratorConstants.RESPONSE_MESSAGE_VAR_NAME;
import static io.ballerina.asyncapi.core.GeneratorConstants.SELF;
import static io.ballerina.asyncapi.core.GeneratorConstants.SERVER_STREAMING;
import static io.ballerina.asyncapi.core.GeneratorConstants.SIMPLE_PIPE;
import static io.ballerina.asyncapi.core.GeneratorConstants.TIMEOUT;
import static io.ballerina.asyncapi.core.GeneratorConstants.UUID;
import static io.ballerina.asyncapi.core.GeneratorConstants.WRITE_MESSAGE_QUEUE;
import static io.ballerina.asyncapi.core.GeneratorConstants.X_RESPONSE;
import static io.ballerina.asyncapi.core.GeneratorConstants.X_RESPONSE_TYPE;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createSeparatedNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createAssignmentStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBlockStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createCheckExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createExpressionStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFieldAccessExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFieldBindingPatternVarnameNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionBodyBlockNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createImplicitNewExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createIndexedExpressionNode;
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
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACKET_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.COLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.COMMA_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.DOT_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.EQUAL_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.GT_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.LOCK_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.LT_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.NEW_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACKET_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RIGHT_ARROW_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RIGHT_DOUBLE_ARROW_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SEMICOLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.STREAM_KEYWORD;

/**
 * This Util class uses for generating remote function body  {@link FunctionBodyNode}.
 *
 * @since 1.3.0
 */
public class RemoteFunctionBodyGenerator {

    private final AsyncApi25DocumentImpl asyncAPI;
    private final List<ImportDeclarationNode> imports;

    private final UtilGenerator utilGenerator;

    public RemoteFunctionBodyGenerator(List<ImportDeclarationNode> imports,
                                       AsyncApi25DocumentImpl asyncAPI,UtilGenerator utilGenerator) {

        this.imports = imports;
        this.asyncAPI = asyncAPI;
        this.utilGenerator =utilGenerator;
    }

    private static void createCommentStatementsForDispatcherId(List<StatementNode> statementsList,
                                                               String requestType,
                                                               String dispatcherStreamId,
                                                               String requestTypePipe) {

        SimpleNameReferenceNode requestypePipeNode = createSimpleNameReferenceNode(
                createIdentifierToken(requestTypePipe));
        Token equalToken = createToken(EQUAL_TOKEN);
        Token semicolonToken = createToken(SEMICOLON_TOKEN);
        Token openBracketToken = createToken(OPEN_BRACKET_TOKEN);
        Token closeBracketToken = createToken(CLOSE_BRACKET_TOKEN);
        Token dotToken = createToken(DOT_TOKEN);
        Token openBraceToken = createToken(OPEN_BRACE_TOKEN);
        Token closeBraceToken = createToken(CLOSE_BRACE_TOKEN);
        Token openParenToken=createToken(OPEN_PAREN_TOKEN);
        Token closeParenToken=createToken(CLOSE_PAREN_TOKEN);


        ArrayList<StatementNode> lockStatements = new ArrayList<>();
        //Create remote function body when dispatcherStreamId is present
        if (dispatcherStreamId != null) {
            SimpleNameReferenceNode dispatcherStreamIdNode = createSimpleNameReferenceNode(
                    createIdentifierToken(dispatcherStreamId));
            //string id;
            VariableDeclarationNode dispatcherStreamIdDefineNode = createVariableDeclarationNode(
                    createEmptyNodeList(), null,
                    createTypedBindingPatternNode(
                            createSimpleNameReferenceNode(createIdentifierToken("string")),
                            createFieldBindingPatternVarnameNode(dispatcherStreamIdNode)),
                    null, null, semicolonToken);
            statementsList.add(dispatcherStreamIdDefineNode);

            //lock{
            //      id = uuid:createType1AsString();
            //      self.pipes["Pongmessage"] = pongMessagePipe;
            // }
            QualifiedNameReferenceNode uuidNode = createQualifiedNameReferenceNode(createIdentifierToken(UUID),
                    createToken(COLON_TOKEN), createIdentifierToken(CREATE_TYPE1_AS_STRING));
            AssignmentStatementNode uuidAssignmentNode = createAssignmentStatementNode(dispatcherStreamIdNode,
                    equalToken, uuidNode, semicolonToken);
            lockStatements.add(uuidAssignmentNode);
            FieldAccessExpressionNode remoteSelfPipes =createFieldAccessExpressionNode(
                            createSimpleNameReferenceNode(createIdentifierToken(SELF)), dotToken,
                            createSimpleNameReferenceNode(createIdentifierToken("pipes")));

            MethodCallExpressionNode addPipeMethodCallExpressionNode = createMethodCallExpressionNode(remoteSelfPipes,
                    dotToken,
                    createSimpleNameReferenceNode(createIdentifierToken("addPipe")),
                    openParenToken, createSeparatedNodeList(dispatcherStreamIdNode,createToken(COMMA_TOKEN),
                            requestypePipeNode), closeParenToken);

//                    createSeparatedNodeList(createSimpleNameReferenceNode(createIdentifierToken(dispatcherStreamId)));
//            VariableDeclarationNode pipeTypeEnsureStatement = createVariableDeclarationNode(
//                    createEmptyNodeList(), null,
//                    createTypedBindingPatternNode(
//                            pipeTypeName,
//                            createFieldBindingPatternVarnameNode(requestTypePipeNode)),
//                    equalToken, addPipeMethodCallExpressionNode, semicolonToken);
           ExpressionStatementNode addPipeByIdStatement=createExpressionStatementNode(null,
                   addPipeMethodCallExpressionNode,semicolonToken);

//            lockStatements.add(selfPipesAssignmentStatementNode);
            LockStatementNode functionLockStatementNode = createLockStatementNode(createToken(LOCK_KEYWORD),
                    createBlockStatementNode(openBraceToken, createNodeList(lockStatements), closeBraceToken),
                    null);
            statementsList.add(functionLockStatementNode);
            statementsList.add(addPipeByIdStatement);


            // pingMessage["id"]=id;
            IndexedExpressionNode requestTypeVarRef = createIndexedExpressionNode(createSimpleNameReferenceNode(
                            createIdentifierToken(requestType)), openBracketToken,
                    createSeparatedNodeList(createSimpleNameReferenceNode(createIdentifierToken("\"" +
                            dispatcherStreamId + "\""))),
                    closeBracketToken);
            AssignmentStatementNode idValueAssignmentStatementNode = createAssignmentStatementNode(requestTypeVarRef,
                    equalToken, dispatcherStreamIdNode, semicolonToken);
            statementsList.add(idValueAssignmentStatementNode);





        } else {

            //self.pipes["tuplePipe"] = tuplePipe;
//            AssignmentStatementNode selfPipesAssignmentStatementNode = createAssignmentStatementNode(remoteSelfPipes,
//                    equalToken, requestypePipeNode, semicolonToken);
            FieldAccessExpressionNode remoteSelfPipes =createFieldAccessExpressionNode(
                    createSimpleNameReferenceNode(createIdentifierToken(SELF)), dotToken,
                    createSimpleNameReferenceNode(createIdentifierToken("pipes")));

            MethodCallExpressionNode addPipeMethodCallExpressionNode = createMethodCallExpressionNode(remoteSelfPipes,
                    dotToken,
                    createSimpleNameReferenceNode(createIdentifierToken("addPipe")),
                    openParenToken, createSeparatedNodeList(createSimpleNameReferenceNode(createIdentifierToken("\"" +
                            requestType + "\"")),createToken(COMMA_TOKEN),
                            requestypePipeNode), closeParenToken);

            ExpressionStatementNode addPipeByTypeStatement=createExpressionStatementNode(null,
                    addPipeMethodCallExpressionNode,semicolonToken);
//            lockStatements.add(selfPipesAssignmentStatementNode);
//
//            LockStatementNode functionLockStatementNode = createLockStatementNode(createToken(LOCK_KEYWORD),
//                    createBlockStatementNode(openBraceToken, createNodeList(lockStatements), closeBraceToken),
//                    null);
            statementsList.add(addPipeByTypeStatement);

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
                                                String dispatcherStreamId, List<MatchClauseNode> matchStatementList)
            throws BallerinaAsyncApiException {

        RemoteFunctionReturnTypeGenerator functionReturnType = new RemoteFunctionReturnTypeGenerator(
                asyncAPI);
        // Create statements
        List<StatementNode> statementsList = new ArrayList<>();

        // This return type for target data type binding.

        if (extensions != null) {
            JsonNode xResponse = extensions.get(X_RESPONSE);
            JsonNode xResponseType = extensions.get(X_RESPONSE_TYPE);

            String responseType = functionReturnType.getReturnType(xResponse, xResponseType);
            if (xResponseType!=null && xResponseType.equals(new TextNode(SERVER_STREAMING))) {
                //TODO: Include a if condition to check this only one time
                utilGenerator.setStreamFound(true);
                createStreamFunctionBodyStatements(statementsList, requestType, responseType, dispatcherStreamId
                        ,matchStatementList);


            } else {

                createSimpleRPCFunctionBodyStatements(statementsList, requestType, responseType, dispatcherStreamId,
                        matchStatementList);
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
                                                    List<MatchClauseNode> matchStatementList) {

        String requestTypePipe = requestType + "Pipe";


        Token equalToken = createToken(EQUAL_TOKEN);
        Token semicolonToken = createToken(SEMICOLON_TOKEN);
        Token openBraceToken = createToken(OPEN_BRACE_TOKEN);
        Token closeBraceToken = createToken(CLOSE_BRACE_TOKEN);
        Token dotToken = createToken(DOT_TOKEN);
        Token closeParenToken = createToken(CLOSE_PAREN_TOKEN);
        Token openParenToken = createToken(OPEN_PAREN_TOKEN);
        Token rightDoubleArrow =createToken(RIGHT_DOUBLE_ARROW_TOKEN);

        SimpleNameReferenceNode requestTypePipeNode = createSimpleNameReferenceNode(createIdentifierToken(
                requestTypePipe));
        SimpleNameReferenceNode responseMessageNode = createSimpleNameReferenceNode(
                createIdentifierToken(RESPONSE_MESSAGE_VAR_NAME));
        QualifiedNameReferenceNode pipeTypeCombined = createQualifiedNameReferenceNode(
                createIdentifierToken(SIMPLE_PIPE),
                createToken(COLON_TOKEN), createIdentifierToken(GeneratorConstants.CAPITAL_PIPE));

        //pipe:Pipe tuplePipe = new (10000);
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

        PositionalArgumentNode responseTypeTimeOut = createPositionalArgumentNode(createRequiredExpressionNode(
                createIdentifierToken(TIMEOUT)));
        createCommentStatementsForDispatcherId(statementsList, requestType, dispatcherStreamId, requestTypePipe);

        if (dispatcherStreamId == null) {
            ArrayList<StatementNode> statementNodes = new ArrayList<>();
            QualifiedNameReferenceNode pipeTypeName = createQualifiedNameReferenceNode(
                    createIdentifierToken(SIMPLE_PIPE),
                    createToken(COLON_TOKEN), createIdentifierToken(GeneratorConstants.CAPITAL_PIPE));
            FieldAccessExpressionNode selfPipes = createFieldAccessExpressionNode(
                    createSimpleNameReferenceNode(createIdentifierToken(SELF)), createToken(DOT_TOKEN),
                    createSimpleNameReferenceNode(createIdentifierToken(PIPES)));
//                    createSeparatedNodeList(createSimpleNameReferenceNode(
//                            createIdentifierToken("\"" + requestType + "\""))),
//                    closeBracketToken);
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
            List<Node> responseNodeList=new ArrayList<>();
            if(responseType.contains("|")){
               String[] responseArray= responseType.split("\\|");
               for(String response: responseArray){
                   responseNodeList.add(createIdentifierToken("\""+response.trim()+"\""));
                   responseNodeList.add(createIdentifierToken("|"));

               }
               responseNodeList.remove(responseNodeList.size()-1);

            }else{
              responseNodeList.add(createIdentifierToken("\"" + responseType + "\""));


            }
            MatchClauseNode matchClauseNode = createMatchClauseNode(createSeparatedNodeList(responseNodeList),
                    null, rightDoubleArrow,
                    createBlockStatementNode(openBraceToken, createNodeList(statementNodes), closeBraceToken));
            matchStatementList.add(matchClauseNode);
        }



        if (!requestType.equals("error")) {
            // check self.writeMessageQueue.produce(tuple, timeout);
            List<Node> argumentArrays = new ArrayList<>();
            PositionalArgumentNode requestTypeName = createPositionalArgumentNode(createRequiredExpressionNode(
                    createIdentifierToken(requestType)));
            argumentArrays.add(requestTypeName);
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
            statementsList.add(callGlobalQueueProduceNode);
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
        statementsList.add(streamMessages);


        //  lock {
        //     StreamGenerator streamGenerator = check new (subscribeMessagePipe, timeout);
        //     streamMessages = new (streamGenerator);
        //  }
        ArrayList<StatementNode> streamStatementList = new ArrayList<>();

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
                                "StreamGenerator"))

                        , createFieldBindingPatternVarnameNode(streamGeneratorNode)), equalToken, checkExpressionNode,
                semicolonToken);
        streamStatementList.add(streamGenerator);

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

    private void createNoResponseFunctionBodyStatement(List<StatementNode> statementsList, String requestType) {
        // check self.writeMessageQueue.produce(tuple, timeout);

        Token semicolonToken = createToken(SEMICOLON_TOKEN);
        Token dotToken = createToken(DOT_TOKEN);
        Token closeParenToken = createToken(CLOSE_PAREN_TOKEN);
        Token openParenToken = createToken(OPEN_PAREN_TOKEN);

        List<Node> argumentArrays = new ArrayList<>();
        PositionalArgumentNode requestTypeName = createPositionalArgumentNode(
                createRequiredExpressionNode(createIdentifierToken(requestType)));
        PositionalArgumentNode responseTypeTimeOut = createPositionalArgumentNode(
                createRequiredExpressionNode(createIdentifierToken("timeout")));
        argumentArrays.add(requestTypeName);
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
                                                       List<MatchClauseNode> matchStatementList) {


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
        Token openBracketToken = createToken(OPEN_BRACKET_TOKEN);
        Token closeBracketToken = createToken(CLOSE_BRACKET_TOKEN);
        Token dotToken = createToken(DOT_TOKEN);
        Token closeParenToken = createToken(CLOSE_PAREN_TOKEN);
        Token openParenToken = createToken(OPEN_PAREN_TOKEN);
        Token openBraceToken = createToken(OPEN_BRACE_TOKEN);
        Token closeBraceToken = createToken(CLOSE_BRACE_TOKEN);
        Token rightDoubleArrow = createToken(RIGHT_DOUBLE_ARROW_TOKEN);

        SimpleNameReferenceNode requestTypePipeNode = createSimpleNameReferenceNode(
                createIdentifierToken(requestTypePipe));
        SimpleNameReferenceNode responseMessageNode = createSimpleNameReferenceNode(
                createIdentifierToken(RESPONSE_MESSAGE_VAR_NAME));
        QualifiedNameReferenceNode pipeTypeCombined = createQualifiedNameReferenceNode(
                createIdentifierToken(SIMPLE_PIPE),
                createToken(COLON_TOKEN), createIdentifierToken(GeneratorConstants.CAPITAL_PIPE));


        //pipe:Pipe tuplePipe = new (1);
        List<Node> argumentsList = new ArrayList<>();
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

        //tuple["id"] = id;
        createCommentStatementsForDispatcherId(statementsList, requestType, dispatcherStreamId, requestTypePipe);


        //Create pipes using request Type names when there is no dispatcherStreamId
        if (dispatcherStreamId == null) {
            ArrayList<StatementNode> statementNodes = new ArrayList<>();
            QualifiedNameReferenceNode pipeTypeName = createQualifiedNameReferenceNode(
                    createIdentifierToken(SIMPLE_PIPE),
                    createToken(COLON_TOKEN), createIdentifierToken(GeneratorConstants.CAPITAL_PIPE));
            FieldAccessExpressionNode selfPipes = createFieldAccessExpressionNode(
                            createSimpleNameReferenceNode(createIdentifierToken(SELF)), createToken(DOT_TOKEN),
                            createSimpleNameReferenceNode(createIdentifierToken(PIPES)));
//                    createSeparatedNodeList(createSimpleNameReferenceNode(
//                            createIdentifierToken("\"" + requestType + "\""))),
//                    closeBracketToken);
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


            MatchClauseNode matchClauseNode = createMatchClauseNode(createSeparatedNodeList(
                            createIdentifierToken("\"" + responseType + "\"")), null, rightDoubleArrow,
                    createBlockStatementNode(openBraceToken, createNodeList(statementNodes), closeBraceToken));
            matchStatementList.add(matchClauseNode);
        }


        PositionalArgumentNode responseTypeTimeOut = createPositionalArgumentNode(
                createRequiredExpressionNode(createIdentifierToken("timeout")));

        if (!requestType.equals("error")) {
            // check self.writeMessageQueue.produce(tuple, timeout);
            List<Node> argumentArrays = new ArrayList<>();
            PositionalArgumentNode requestTypeName = createPositionalArgumentNode(
                    createRequiredExpressionNode(createIdentifierToken(requestType)));
            argumentArrays.add(requestTypeName);
            argumentArrays.add(createToken(COMMA_TOKEN));
            argumentArrays.add(responseTypeTimeOut);
            FieldAccessExpressionNode globalQueue = createFieldAccessExpressionNode(
                    createSimpleNameReferenceNode(createIdentifierToken(SELF)), dotToken,
                    createSimpleNameReferenceNode(createIdentifierToken(WRITE_MESSAGE_QUEUE)));
            CheckExpressionNode callGlobalQueueProduce = createCheckExpressionNode(null,
                    createToken(CHECK_KEYWORD),
                    createMethodCallExpressionNode(globalQueue, dotToken,
                            createSimpleNameReferenceNode(createIdentifierToken(PRODUCE)),
                            openParenToken, createSeparatedNodeList(
                                    argumentArrays
                            ), closeParenToken));
            ExpressionStatementNode callGlobalQueueProduceNode = createExpressionStatementNode(
                    null, callGlobalQueueProduce, semicolonToken);
            statementsList.add(callGlobalQueueProduceNode);
        }


        // anydata user = check tuplePipe.consume(timeout);
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
        statementsList.add(callRelevantPipeProduceVar);


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


        //check pongMessagePipe.immediateClose();
        CheckExpressionNode immediateCloseCheck = createCheckExpressionNode(
                null, createToken(CHECK_KEYWORD), createMethodCallExpressionNode(
                        requestTypePipeNode, dotToken, createSimpleNameReferenceNode(
                                createIdentifierToken("immediateClose")),
                        openParenToken, createSeparatedNodeList(), closeParenToken));

        ExpressionStatementNode immediateCloseExpressionNode = createExpressionStatementNode(
                null, immediateCloseCheck, createToken(SEMICOLON_TOKEN));
        statementsList.add(immediateCloseExpressionNode);
        Token returnKeyWord = createIdentifierToken("return");

        ReturnStatementNode returnStatementNode = createReturnStatementNode(returnKeyWord, responseNameNode,
                createToken(SEMICOLON_TOKEN));
        statementsList.add(returnStatementNode);
    }

}
