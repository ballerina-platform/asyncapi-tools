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
import io.ballerina.asyncapi.core.generators.schema.BallerinaTypesGenerator;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.AssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionStatementNode;
import io.ballerina.compiler.syntax.tree.FieldAccessExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.FunctionBodyNode;
import io.ballerina.compiler.syntax.tree.ImplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.IndexedExpressionNode;
import io.ballerina.compiler.syntax.tree.LockStatementNode;
import io.ballerina.compiler.syntax.tree.MatchClauseNode;
import io.ballerina.compiler.syntax.tree.MatchStatementNode;
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
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.ballerina.asyncapi.core.GeneratorConstants.CONSUME;
import static io.ballerina.asyncapi.core.GeneratorConstants.CREATE_TYPE1_AS_STRING;
import static io.ballerina.asyncapi.core.GeneratorConstants.ENSURE_TYPE;
import static io.ballerina.asyncapi.core.GeneratorConstants.NILLABLE;
import static io.ballerina.asyncapi.core.GeneratorConstants.PIPE;
import static io.ballerina.asyncapi.core.GeneratorConstants.PIPES;
import static io.ballerina.asyncapi.core.GeneratorConstants.PRODUCE;
import static io.ballerina.asyncapi.core.GeneratorConstants.RESPONSE_MESSAGE;
import static io.ballerina.asyncapi.core.GeneratorConstants.RESPONSE_MESSAGE_VAR_NAME;
import static io.ballerina.asyncapi.core.GeneratorConstants.SELF;
import static io.ballerina.asyncapi.core.GeneratorConstants.SIMPLE_PIPE;
import static io.ballerina.asyncapi.core.GeneratorConstants.SERVER_STREAMING;
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
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMatchStatementNode;
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
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RIGHT_DOUBLE_ARROW_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SEMICOLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.STREAM_KEYWORD;

/**
 * This Util class uses for generating remote function body  {@link FunctionBodyNode}.
 *
 * @since 1.3.0
 */
public class FunctionBodyGenerator {

    private final List<TypeDefinitionNode> typeDefinitionNodeList;
    private final AsyncApi25DocumentImpl asyncAPI;
    private final BallerinaTypesGenerator ballerinaSchemaGenerator;
    private final BallerinaUtilGenerator ballerinaUtilGenerator;
    private final BallerinaAuthConfigGenerator ballerinaAuthConfigGenerator;
    private final boolean resourceMode;
    private List<ImportDeclarationNode> imports;
    private boolean isHeader;

    public FunctionBodyGenerator(List<ImportDeclarationNode> imports, List<TypeDefinitionNode> typeDefinitionNodeList,
                                 AsyncApi25DocumentImpl asyncAPI, BallerinaTypesGenerator ballerinaSchemaGenerator,
                                 BallerinaAuthConfigGenerator ballerinaAuthConfigGenerator,
                                 BallerinaUtilGenerator ballerinaUtilGenerator, boolean resourceMode) {

        this.imports = imports;
        this.isHeader = false;
        this.typeDefinitionNodeList = typeDefinitionNodeList;
        this.asyncAPI = asyncAPI;
        this.ballerinaSchemaGenerator = ballerinaSchemaGenerator;
        this.ballerinaUtilGenerator = ballerinaUtilGenerator;
        this.ballerinaAuthConfigGenerator = ballerinaAuthConfigGenerator;
        this.resourceMode = resourceMode;
    }

    public List<ImportDeclarationNode> getImports() {
        return imports;
    }

    public void setImports(List<ImportDeclarationNode> imports) {
        this.imports = imports;
    }

    /**
     * Generate function body node for the remote function.
     * <p>
     * //     * @param path      - remote function path
     * //     * @param operation - opneapi operation
     *
     * @return - {@link FunctionBodyNode}
     * @throws BallerinaAsyncApiException - throws exception if generating FunctionBodyNode fails.
     */
    public FunctionBodyNode getFunctionBodyNode(Map<String, JsonNode> extensions, String requestType, String dispatcherStreamId, List<MatchClauseNode> matchStatementList)
            throws BallerinaAsyncApiException {

        NodeList<AnnotationNode> annotationNodes = createEmptyNodeList();
        FunctionReturnTypeGenerator functionReturnType = new FunctionReturnTypeGenerator(
                asyncAPI, ballerinaSchemaGenerator, typeDefinitionNodeList);
        isHeader = false;
        // Create statements
        List<StatementNode> statementsList = new ArrayList<>();
        // Check whether given path is complex path , if complex it will handle adding these two statement
//        if (resourceMode && isComplexURL(path)) {
//            List<StatementNode> bodyStatements = generateBodyStatementForComplexUrl(path);
//            statementsList.addAll(bodyStatements);
//        }
//        string path - common for every remote functions


//        String method = operation.getKey().name().trim().toLowerCase(Locale.ENGLISH);
        // This return type for target data type binding.

        if(extensions!=null) {
            JsonNode x_response=extensions.get(X_RESPONSE);
            JsonNode x_response_type=extensions.get(X_RESPONSE_TYPE);

            String responseType = functionReturnType.getReturnType(x_response,x_response_type);
            if (x_response_type.equals(new TextNode(SERVER_STREAMING))){
                createStreamFunctionBodyStatements(statementsList, requestType, responseType, dispatcherStreamId);



            } else {
//            String returnType = returnTypeForTargetTypeField(rType);
                createSimpleRPCFunctionBodyStatements(statementsList, requestType, responseType, dispatcherStreamId,matchStatementList);
            }

        }else{
            createNoResponseFunctionBodyStatement(statementsList,requestType);

        }
        // Statement Generator for requestBody
//        if (operation.getValue().getRequestBody() != null) {
//            RequestBody requestBody = operation.getValue().getRequestBody();
//            handleRequestBodyInOperation(statementsList, method, returnType, requestBody);
//        } else {

//        }
        //Create statements
        NodeList<StatementNode> statements = createNodeList(statementsList);
        return createFunctionBodyBlockNode(createToken(OPEN_BRACE_TOKEN), null, statements,
                createToken(CLOSE_BRACE_TOKEN), null);
    }

    private void createStreamFunctionBodyStatements(List<StatementNode> statementsList,
                                                    String requestType, String responseType,

                                                    String dispatcherStreamId) {

        String dispatcherKey="'type";


        //requestType substring
//        char requestTypeFirstChar = Character.toLowerCase(requestType.charAt(0)); // Lowercase the first character
//        String requestRemainingString = requestType.substring(1);
//        String requestTypeCamelCaseName=requestTypeFirstChar+requestRemainingString;
        String requestTypePipe=requestType+"Pipe";
        //responseType substring
//        String responseTypeCamelCaseName=null;
//        if(responseType.contains(PIPE)){
//            responseTypeCamelCaseName = "unionResult";
//
//        }else {
//            char responseTypeFirstChar = Character.toLowerCase(responseType.charAt(0)); // Lowercase the first character
//            String responseRemainingString = responseType.substring(1);
//            responseTypeCamelCaseName = responseTypeFirstChar + responseRemainingString;
//        }

        Token equalToken = createToken(EQUAL_TOKEN);
        Token semicolonToken = createToken(SEMICOLON_TOKEN);
        Token openBracketToken= createToken(OPEN_BRACKET_TOKEN);
        Token closeBracketToken=createToken(CLOSE_BRACKET_TOKEN);
        Token openBraceToken=createToken(OPEN_BRACE_TOKEN);
        Token closeBraceToken=createToken(CLOSE_BRACE_TOKEN);
        Token dotToken =createToken(DOT_TOKEN);
        Token closeParenToken = createToken(CLOSE_PAREN_TOKEN);
        Token openParenToken=createToken(OPEN_PAREN_TOKEN);
        Token rightDoubleArrow =createToken(RIGHT_DOUBLE_ARROW_TOKEN);



//
        SimpleNameReferenceNode requestTypePipeNode=createSimpleNameReferenceNode(createIdentifierToken(requestTypePipe));
        SimpleNameReferenceNode responseMessageNode=createSimpleNameReferenceNode(createIdentifierToken(RESPONSE_MESSAGE_VAR_NAME));
        QualifiedNameReferenceNode pipeTypeCombined = createQualifiedNameReferenceNode(createIdentifierToken(SIMPLE_PIPE),
                createToken(COLON_TOKEN), createIdentifierToken(GeneratorConstants.CAPITAL_PIPE));

//        //tuple["id"] = "1";
//        if(isDispatcherStreamId) {
//            IndexedExpressionNode requestTypeVarRef = createIndexedExpressionNode(createSimpleNameReferenceNode(createIdentifierToken(requestType)), openBracketToken,
//                    createSeparatedNodeList(createSimpleNameReferenceNode(createIdentifierToken("\"" + dispatcherStreamId + "\""))),
//                    closeBracketToken);
//            AssignmentStatementNode idValueAssignmentStatementNode = createAssignmentStatementNode(requestTypeVarRef,
//                    equalToken, createSimpleNameReferenceNode(createIdentifierToken("\"" + count + "\"")), semicolonToken);
//            statementsList.add(idValueAssignmentStatementNode);
//
//        }

        //pipe:Pipe tuplePipe = new (10000);
        List<Node> argumentsList = new ArrayList<>();
        argumentsList.add(createIdentifierToken("10000"));
        SeparatedNodeList<FunctionArgumentNode> arguments = createSeparatedNodeList(argumentsList);

        ParenthesizedArgList parenthesizedArgList = createParenthesizedArgList(openParenToken,
                arguments,
                closeParenToken);
        ImplicitNewExpressionNode expressionNode = createImplicitNewExpressionNode(createToken(NEW_KEYWORD),
                parenthesizedArgList);
        VariableDeclarationNode remotePipeTypeEnsureStatement= createVariableDeclarationNode(createEmptyNodeList(),null,
                createTypedBindingPatternNode(
                        pipeTypeCombined,
                        createFieldBindingPatternVarnameNode(requestTypePipeNode)),
                equalToken,expressionNode,semicolonToken);
        statementsList.add(remotePipeTypeEnsureStatement);


//        //self.pipes["tuplePipe"] = tuplePipe;
//        IndexedExpressionNode remoteSelfPipes=createIndexedExpressionNode(createFieldAccessExpressionNode(
//                        createSimpleNameReferenceNode(createIdentifierToken(SELF)), dotToken,
//                        createSimpleNameReferenceNode(createIdentifierToken("pipes"))),openBracketToken,
//                createSeparatedNodeList(createSimpleNameReferenceNode(createIdentifierToken("\""+requestTypePipe+"\""))),
//                closeBracketToken);
//        AssignmentStatementNode selfPipesAssignmentStatementNode = createAssignmentStatementNode(remoteSelfPipes,
//                equalToken,requestTypePipeNode, semicolonToken);
//        statementsList.add(selfPipesAssignmentStatementNode);

        PositionalArgumentNode responseTypeTimeOut = createPositionalArgumentNode(createRequiredExpressionNode(createIdentifierToken("timeout")));
        createCommentStatementsForDispatcherId(statementsList, requestType,dispatcherStreamId, requestTypePipe);


        if(!requestType.equals("error")) {
            // check self.writeMessageQueue.produce(tuple, timeout);
            List<Node> argumentArrays = new ArrayList<>();
            PositionalArgumentNode requestTypeName = createPositionalArgumentNode(createRequiredExpressionNode(createIdentifierToken(requestType)));
            argumentArrays.add(requestTypeName);
            argumentArrays.add(createToken(COMMA_TOKEN));
            argumentArrays.add(responseTypeTimeOut);
            FieldAccessExpressionNode globalQueue = createFieldAccessExpressionNode(
                    createSimpleNameReferenceNode(createIdentifierToken(SELF)), dotToken,
                    createSimpleNameReferenceNode(createIdentifierToken(WRITE_MESSAGE_QUEUE)));
            CheckExpressionNode callGlobalQueueProduce = createCheckExpressionNode(null, createToken(CHECK_KEYWORD),
                    createMethodCallExpressionNode(globalQueue, dotToken,
                            createSimpleNameReferenceNode(createIdentifierToken(PRODUCE)), openParenToken, createSeparatedNodeList(
                                    argumentArrays
                            ), closeParenToken));
            ExpressionStatementNode callGlobalQueueProduceNode = createExpressionStatementNode(null, callGlobalQueueProduce, semicolonToken);
            statementsList.add(callGlobalQueueProduceNode);
        }
        SimpleNameReferenceNode responseMessageTypeName =createSimpleNameReferenceNode(createIdentifierToken(RESPONSE_MESSAGE));


//
//        // ResponseMessage[] responseMessageArray = [];
//        SimpleNameReferenceNode responseMessageArrayNode = createSimpleNameReferenceNode(createIdentifierToken("responseMessageArray"));
//
//        ArrayTypeDescriptorNode arrayTypeDescriptorNode= createArrayTypeDescriptorNode( responseMessageTypeName,createNodeList(createArrayDimensionNode(openBracketToken,null,closeBracketToken)));
//        VariableDeclarationNode responseMessageArray= createVariableDeclarationNode(createEmptyNodeList(),null,
//
//                createTypedBindingPatternNode(
//                        arrayTypeDescriptorNode,
//                        createFieldBindingPatternVarnameNode(responseMessageArrayNode)),
//                //TODO: Findout [] node
//                equalToken,createSimpleNameReferenceNode(createIdentifierToken("[]")),semicolonToken);
//
//        statementsList.add(responseMessageArray);
//
//
        //  ResponseMessage responseMessage = check subscribeMessagePipe.consume(timeout);
//        responseNameNode;
////        if(!requestType.equals("error")) {
        SimpleNameReferenceNode responseNameNode = createSimpleNameReferenceNode(createIdentifierToken(responseType+",error?"));
       SimpleNameReferenceNode streamMessageNode= createSimpleNameReferenceNode(createIdentifierToken("streamMessages"));
////        }else{
////            responseNameNode = createSimpleNameReferenceNode(createIdentifierToken(responseTypeCamelCaseName+"Message"));
//
////        }
//
//        CheckExpressionNode callRelevantPipeConsumeNode=createCheckExpressionNode(null,createToken(CHECK_KEYWORD),
//                createMethodCallExpressionNode(requestTypePipeNode,dotToken,
//                        createSimpleNameReferenceNode(createIdentifierToken(CONSUME)),openParenToken,createSeparatedNodeList(
//                                responseTypeTimeOut
//                        ),closeParenToken));
//        VariableDeclarationNode callRelevantPipeProduceVar= createVariableDeclarationNode(createEmptyNodeList(),null,
//                createTypedBindingPatternNode(
//                        responseMessageTypeName,
//                        createFieldBindingPatternVarnameNode(createSimpleNameReferenceNode(
//                                createIdentifierToken(RESPONSE_MESSAGE_VAR_NAME)))),
//                equalToken,callRelevantPipeConsumeNode,semicolonToken);
//        statementsList.add(callRelevantPipeProduceVar);
//
//
//        //while
//        List<StatementNode> whileStatements =new ArrayList<>();
//        FieldAccessExpressionNode responseMessageEvent= createFieldAccessExpressionNode(responseMessageNode,dotToken,
//                createSimpleNameReferenceNode(createIdentifierToken(dispatcherKey)));
//        VariableDeclarationNode responseMessageTypenode= createVariableDeclarationNode(createEmptyNodeList(),
//                null,createTypedBindingPatternNode(
//                        createBuiltinSimpleNameReferenceNode(null,createIdentifierToken(STRING)),
//                        createFieldBindingPatternVarnameNode(createSimpleNameReferenceNode(
//                                createIdentifierToken(dispatcherKey)))),
//                equalToken,responseMessageEvent,semicolonToken);
//        whileStatements.add(responseMessageTypenode);
//        MethodCallExpressionNode callPushResponseMessageArray= createMethodCallExpressionNode(responseMessageArrayNode,dotToken,createSimpleNameReferenceNode(
//                createIdentifierToken("push")),openParenToken,createSeparatedNodeList(),closeParenToken);
//       ExpressionStatementNode responseMessageArrayPushNode= createExpressionStatementNode(null,callPushResponseMessageArray,semicolonToken);
//        whileStatements.add(responseMessageArrayPushNode);
//
//       String[] completeAndErrorReturns=responseType.split("\\|");
//        NodeList<StatementNode> statementNodes=createNodeList(createBreakStatementNode(createToken(BREAK_KEYWORD),createToken(SEMICOLON_TOKEN)));
//        MatchClauseNode matchClauseNode=createMatchClauseNode(createSeparatedNodeList(
//                        createIdentifierToken("\""+completeAndErrorReturns[1]+"\"|\""+completeAndErrorReturns[2]+"\"")),null,rightDoubleArrow,
//                createBlockStatementNode(openBraceToken,statementNodes,closeBraceToken));
//
//        MatchStatementNode matchStatementNode=createMatchStatementNode(createToken(MATCH_KEYWORD),
//                createBracedExpressionNode(null,openParenToken,
//                        createSimpleNameReferenceNode(createIdentifierToken(dispatcherKey)),
//                        closeParenToken),openBraceToken,createNodeList(matchClauseNode),closeBraceToken,null);
//
//        whileStatements.add(matchStatementNode);
//
//
//
//        AssignmentStatementNode responseMessageAssignmentStatementNode = createAssignmentStatementNode(responseMessageNode,
//                createToken(EQUAL_TOKEN),callRelevantPipeConsumeNode, createToken(SEMICOLON_TOKEN));
//        whileStatements.add(responseMessageAssignmentStatementNode);
//
//
//
//
//        BlockStatementNode whileBody=createBlockStatementNode(openBraceToken,createNodeList(whileStatements),
//                closeBraceToken);
//       WhileStatementNode streamWhileStatements= createWhileStatementNode(createToken(WHILE_KEYWORD),
//                createBasicLiteralNode(TRUE_KEYWORD,createToken(TRUE_KEYWORD)),whileBody,null);
//        statementsList.add(streamWhileStatements);






//         stream<NextMessage|CompleteMessage|ErrorMessage> streamMessages;
        StreamTypeParamsNode streamTypeParamsNode= createStreamTypeParamsNode(createToken(LT_TOKEN),
                responseNameNode,null,null,createToken(GT_TOKEN));
        StreamTypeDescriptorNode streamTypeDescriptorNode=createStreamTypeDescriptorNode(
                createToken(STREAM_KEYWORD),streamTypeParamsNode);
//        ArrayTypeDescriptorNode arrayTypeDescriptorNode= createArrayTypeDescriptorNode( responseMessageTypeName,
//                createNodeList(createArrayDimensionNode(openBracketToken,null,closeBracketToken)));
//       MethodCallExpressionNode toStreamMethodCall= createMethodCallExpressionNode(
//               responseMessageArrayNode,dotToken,createSimpleNameReferenceNode(
//               createIdentifierToken("toStream")),openParenToken,createSeparatedNodeList(),closeParenToken);

        VariableDeclarationNode streamMessages= createVariableDeclarationNode(createEmptyNodeList(),null,

                createTypedBindingPatternNode(
                        streamTypeDescriptorNode,
                        createFieldBindingPatternVarnameNode(streamMessageNode)),
                //TODO: Findout [] node
                null,null,semicolonToken);
        statementsList.add(streamMessages);


        //  lock {
        //            GraphQLOverWebSocketStream graphqlOverWebsocketStreamGenerator = check new (subscribeMessagePipe, timeout);
        //            streamMessages = new (graphqlOverWebsocketStreamGenerator);
        //        }
       ArrayList<StatementNode> streamStatementList= new ArrayList<>();

       ArrayList<Node> streamGeneratorArguments= new ArrayList<>();
       streamGeneratorArguments.add(createPositionalArgumentNode(createSimpleNameReferenceNode(createIdentifierToken("subscribeMessagePipe"))));
       streamGeneratorArguments.add(createToken(COMMA_TOKEN));
        streamGeneratorArguments.add(createPositionalArgumentNode(createSimpleNameReferenceNode(createIdentifierToken("timeout"))));
       CheckExpressionNode checkExpressionNode=createCheckExpressionNode(null, createToken(CHECK_KEYWORD),
               createImplicitNewExpressionNode(createToken(NEW_KEYWORD),
               createParenthesizedArgList(openParenToken,
                       createSeparatedNodeList(streamGeneratorArguments),closeParenToken)));
//       createSimpleNameReferenceNode(createIdentifierToken("graphqlOverWebsocketStreamGenerator"));
       SimpleNameReferenceNode graphqlOverWebSocketStreamGeneratorNode= createSimpleNameReferenceNode(createIdentifierToken("graphqlOverWebSocketStreamGenerator"));
       VariableDeclarationNode graphqlOverWebsocketStreamGenerator= createVariableDeclarationNode(createEmptyNodeList(),
               null,createTypedBindingPatternNode(createSimpleNameReferenceNode(createIdentifierToken("GraphQLOverWebSocketStream"))

                    ,createFieldBindingPatternVarnameNode(graphqlOverWebSocketStreamGeneratorNode)),equalToken,checkExpressionNode,semicolonToken);
       streamStatementList.add(graphqlOverWebsocketStreamGenerator);

        AssignmentStatementNode streamMessagesAssignmentStatementNode = createAssignmentStatementNode(streamMessageNode,
                equalToken,createImplicitNewExpressionNode(createToken(NEW_KEYWORD),
                        createParenthesizedArgList(openParenToken,createSeparatedNodeList(


                                createPositionalArgumentNode(graphqlOverWebSocketStreamGeneratorNode)
                        ),closeParenToken))
                , semicolonToken);

        streamStatementList.add(streamMessagesAssignmentStatementNode);





       LockStatementNode streamLockStatementNode= createLockStatementNode(createToken(LOCK_KEYWORD),
               createBlockStatementNode(openBraceToken,createNodeList(streamStatementList),closeBraceToken),null);

       statementsList.add(streamLockStatementNode);





        //    return streamMessages;













//        HashMap requestTypes;
//        if(responseType.contains(PIPE)){
//            String[] unionResponses=responseType.split("\\"+PIPE);
//            for(String response:unionResponses){
//                if(responseMap.get(response)!=null){
//                    requestTypes=responseMap.get(response);
//                    requestTypes.put(requestType,count);
//                }else{
//                    requestTypes=new LinkedHashMap<>();
//                    requestTypes.put(requestType,count);
//                }
//                responseMap.put(response,requestTypes);
//            }
//        }
//        else {
//            if (responseMap.get(responseType) != null) {
//                requestTypes = responseMap.get(responseType);
//                requestTypes.put(requestType,count);
//            } else {
//                requestTypes = new LinkedHashMap();
//                requestTypes.put(requestType,count);
//            }
//            responseMap.put(responseType, requestTypes);
////        }









//        String clientCallStatement = "check self.clientEp->" + "writeMessage" + "(" + paramName + ")";
//        ExpressionStatementNode clientCall = createExpressionStatementNode(SyntaxKind.CHECK_EXPRESSION,
//                createSimpleNameReferenceNode(createIdentifierToken(clientCallStatement)),
//                createToken(SEMICOLON_TOKEN));
//        String clientReadStatement = "check self.clientEp->" + "readMessage" + "(" + ")";
//        }
        //Return Variable
//        VariableDeclarationNode clientRead = GeneratorUtils.getSimpleStatement(returnType, RESPONSE,
//                clientReadStatement);
//        statementsList.add(clientCall);
//        statementsList.add(clientRead);
        Token returnKeyWord = createIdentifierToken("return");

        ReturnStatementNode returnStatementNode = createReturnStatementNode(returnKeyWord, streamMessageNode,
                createToken(SEMICOLON_TOKEN));
        statementsList.add(returnStatementNode);
    }

    private void createNoResponseFunctionBodyStatement( List<StatementNode> statementsList,String requestType) {
        // check self.writeMessageQueue.produce(tuple, timeout);

        Token semicolonToken = createToken(SEMICOLON_TOKEN);
        Token dotToken =createToken(DOT_TOKEN);
        Token closeParenToken = createToken(CLOSE_PAREN_TOKEN);
        Token openParenToken=createToken(OPEN_PAREN_TOKEN);

        List<Node> argumentArrays=new ArrayList<>();
        PositionalArgumentNode requestTypeName=createPositionalArgumentNode(createRequiredExpressionNode(createIdentifierToken(requestType)));
        PositionalArgumentNode responseTypeTimeOut=createPositionalArgumentNode(createRequiredExpressionNode(createIdentifierToken("timeout")));
        argumentArrays.add(requestTypeName);
        argumentArrays.add(createToken(COMMA_TOKEN));
        argumentArrays.add(responseTypeTimeOut);
        FieldAccessExpressionNode globalQueue = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(SELF)),dotToken,
                createSimpleNameReferenceNode(createIdentifierToken(WRITE_MESSAGE_QUEUE)));
        CheckExpressionNode callGlobalQueueProduce=createCheckExpressionNode(null,createToken(CHECK_KEYWORD),
                createMethodCallExpressionNode(globalQueue,dotToken,
                        createSimpleNameReferenceNode(createIdentifierToken(PRODUCE)),openParenToken,createSeparatedNodeList(
                                argumentArrays
                        ),closeParenToken));
        ExpressionStatementNode callGlobalQueueProduceNode =createExpressionStatementNode(null,callGlobalQueueProduce,semicolonToken);
        statementsList.add(callGlobalQueueProduceNode);
    }


//    /**
//     * This function for creating requestBody statements.
//     * -- ex: Request body with json payload.
//     * <pre>
//     *    http:Request request = new;
//     *    json jsonBody = payload.toJson();
//     *    request.setPayload(jsonBody, "application/json");
//     *    json response = check self.clientEp->put(path, request);
//     * </pre>
//     *
//     * @param isHeader       - Boolean value for header availability.
//     * @param statementsList - StatementNode list in body node
//     * @param method         - Operation method name.
//     * @param returnType     - Response type
//     * @param iterator       - RequestBody media type
//     */
//    private void createRequestBodyStatements(boolean isHeader, List<StatementNode> statementsList,
//                                             String method, String returnType, Iterator<Map.Entry<String,
//            MediaType>> iterator)
//            throws BallerinaAsyncApiException {
//
//        //Create Request statement
//        Map.Entry<String, MediaType> mediaTypeEntry = iterator.next();
//        if (GeneratorUtils.isSupportedMediaType(mediaTypeEntry)) {
//            VariableDeclarationNode requestVariable = GeneratorUtils.getSimpleStatement(HTTP_REQUEST,
//                    REQUEST, NEW);
//            statementsList.add(requestVariable);
//        }
//        if (mediaTypeEntry.getValue() != null && GeneratorUtils.isSupportedMediaType(mediaTypeEntry)) {
//            genStatementsForRequestMediaType(statementsList, mediaTypeEntry);
//            // TODO:Fill with other mime type
//        } else {
//            // Add default value comment
//            ExpressionStatementNode expressionStatementNode = GeneratorUtils.getSimpleExpressionStatementNode(
//                    "// TODO: Update the request as needed");
//            statementsList.add(expressionStatementNode);
//        }
//        // POST, PUT, PATCH, DELETE, EXECUTE
//        VariableDeclarationNode requestStatement =
//                GeneratorUtils.getSimpleStatement(returnType, RESPONSE, "check self.clientEp->"
//                        + method + "(" + RESOURCE_PATH + ", request)");
//        if (isHeader) {
//            if (method.equals(POST) || method.equals(PUT) || method.equals(PATCH) || method.equals(DELETE)
//                    || method.equals(EXECUTE)) {
//                requestStatement = GeneratorUtils.getSimpleStatement(returnType, RESPONSE,
//                        "check self.clientEp->" + method + "(" + RESOURCE_PATH + ", request, " +
//                                HTTP_HEADERS + ")");
//                statementsList.add(requestStatement);
//                Token returnKeyWord = createIdentifierToken("return");
//                SimpleNameReferenceNode returns = createSimpleNameReferenceNode(createIdentifierToken(RESPONSE));
//                ReturnStatementNode returnStatementNode = createReturnStatementNode(returnKeyWord, returns,
//                        createToken(SEMICOLON_TOKEN));
//                statementsList.add(returnStatementNode);
//            }
//        } else {
//            statementsList.add(requestStatement);
//            Token returnKeyWord = createIdentifierToken("return");
//            SimpleNameReferenceNode returnVariable = createSimpleNameReferenceNode(createIdentifierToken(RESPONSE));
//            ReturnStatementNode returnStatementNode = createReturnStatementNode(returnKeyWord, returnVariable,
//                    createToken(SEMICOLON_TOKEN));
//            statementsList.add(returnStatementNode);
//        }
//    }



//    /**
//     * Generate statements for query parameters and headers when a client supports both ApiKey and HTTPOrOAuth
//     * authentication.
//     */
//    private void addUpdatedPathAndHeaders(List<StatementNode> statementsList, List<String> queryApiKeyNameList,
//                                          List<Parameter> queryParameters, List<String> headerApiKeyNameList,
//                                          List<Parameter> headerParameters) throws BallerinaAsyncApiException{
//
//        List<StatementNode> ifBodyStatementsList = new ArrayList<>();
//
//        if (!headerParameters.isEmpty() || !headerApiKeyNameList.isEmpty()) {
//            if (!headerParameters.isEmpty()) {
//                statementsList.add(getMapForParameters(headerParameters, "map<any>",
//                        HEADER_VALUES, new ArrayList<>()));
//            } else {
//                ExpressionStatementNode headerMapCreation = GeneratorUtils.getSimpleExpressionStatementNode(
//                        "map<any> " + HEADER_VALUES + " = {}");
//                statementsList.add(headerMapCreation);
//            }
//
//            if (!headerApiKeyNameList.isEmpty()) {
//                // update headerValues Map within the if block
//                // `headerValues["api-key"] = self.apiKeyConfig?.apiKey;`
//                addApiKeysToMap(HEADER_VALUES, headerApiKeyNameList, ifBodyStatementsList);
//            }
//            isHeader = true;
//            ballerinaUtilGenerator.setHeadersFound(true);
//        }
//
//        if (!queryParameters.isEmpty() || !queryApiKeyNameList.isEmpty()) {
//            ballerinaUtilGenerator.setQueryParamsFound(true);
//            if (!queryParameters.isEmpty()) {
//                statementsList.add(getMapForParameters(queryParameters, "map<anydata>",
//                        QUERY_PARAM, new ArrayList<>()));
//            } else {
//                ExpressionStatementNode queryParamMapCreation = GeneratorUtils.getSimpleExpressionStatementNode(
//                        "map<anydata> " + QUERY_PARAM + " = {}");
//                statementsList.add(queryParamMapCreation);
//            }
//
//            if (!queryApiKeyNameList.isEmpty()) {
//                // update queryParam Map within the if block
//                // `queryParam["api-key"] = self.apiKeyConfig?.apiKey;`
//                addApiKeysToMap(QUERY_PARAM, queryApiKeyNameList, ifBodyStatementsList);
//            }
//        }
//
//        generateIfBlockToAddApiKeysToMaps(statementsList, ifBodyStatementsList);
//
//        if (!queryParameters.isEmpty() || !queryApiKeyNameList.isEmpty()) {
//            getUpdatedPathHandlingQueryParamEncoding(statementsList, queryParameters);
//        }
//        if (!headerParameters.isEmpty() || !headerApiKeyNameList.isEmpty()) {
//            statementsList.add(GeneratorUtils.getSimpleExpressionStatementNode(
//                    "map<string|string[]> " + HTTP_HEADERS + " = getMapForHeaders(headerValues)"));
//        }
//    }

//    /**
//     * Add apiKeys to a given map (queryParam or headerValues).
//     * <p>
//     * `queryParam["api-key"] = self.apiKeyConfig?.apiKey;`
//     * `headerValues["api-key"] = self.apiKeyConfig?.apiKey;`
//     */
//    private void addApiKeysToMap(String mapName, List<String> apiKeyNames, List<StatementNode> statementNodeList) {
//
//        if (!apiKeyNames.isEmpty()) {
//            for (String apiKey : apiKeyNames) {
//                IdentifierToken fieldName = createIdentifierToken(mapName + "[" + '"' + apiKey.trim() + '"' + "]");
//                Token equal = createToken(EQUAL_TOKEN);
//                FieldAccessExpressionNode fieldExpr = createFieldAccessExpressionNode(
//                        createSimpleNameReferenceNode(createIdentifierToken(SELF)), createToken(DOT_TOKEN),
//                        createSimpleNameReferenceNode(createIdentifierToken(API_KEY_CONFIG_PARAM +
//                                QUESTION_MARK_TOKEN.stringValue())));
//                SimpleNameReferenceNode valueExpr = createSimpleNameReferenceNode(createIdentifierToken(
//                        getValidName(getValidName(apiKey, false), false)));
//                ExpressionNode apiKeyExpr = createFieldAccessExpressionNode(
//                        fieldExpr, createToken(DOT_TOKEN), valueExpr);
//                statementNodeList.add(createAssignmentStatementNode(fieldName, equal, apiKeyExpr, createToken(
//                        SEMICOLON_TOKEN)));
//            }
//        }
//    }
//
//    /**
//     * Get updated path considering queryParamEncodingMap.
//     */
//    private void getUpdatedPathHandlingQueryParamEncoding(List<StatementNode> statementsList, List<Parameter>
//            queryParameters) throws BallerinaAsyncApiException {
//
//        VariableDeclarationNode queryParamEncodingMap = getQueryParameterEncodingMap(queryParameters);
//        if (queryParamEncodingMap != null) {
//            statementsList.add(queryParamEncodingMap);
//            ExpressionStatementNode updatedPath = GeneratorUtils.getSimpleExpressionStatementNode(
//                    RESOURCE_PATH + " = " + RESOURCE_PATH + " + check getPathForQueryParam(queryParam, " +
//                            "queryParamEncoding)");
//            statementsList.add(updatedPath);
//        } else {
//            ExpressionStatementNode updatedPath = GeneratorUtils.getSimpleExpressionStatementNode(
//                    RESOURCE_PATH + " = " + RESOURCE_PATH + " + check getPathForQueryParam(queryParam)");
//            statementsList.add(updatedPath);
//        }
//    }

//    /**
//     * Generate if block when a client supports both ApiKey and HTTPOrOAuth authentication.
//     *
//     * <pre>
//     * if self.apiKeyConfig is ApiKeysConfig {
//     *      --- given statements ---
//     * }
//     * </pre>
//     */
//    private void generateIfBlockToAddApiKeysToMaps(List<StatementNode> statementsList,
//                                                   List<StatementNode> ifBodyStatementsList) {
//
//        if (!ifBodyStatementsList.isEmpty()) {
//            NodeList<StatementNode> ifBodyStatementsNodeList = createNodeList(ifBodyStatementsList);
//            BlockStatementNode ifBody = createBlockStatementNode(createToken(OPEN_BRACE_TOKEN),
//                    ifBodyStatementsNodeList, createToken(CLOSE_BRACE_TOKEN));
//
//            // Create expression `self.apiKeyConfig is ApiKeysConfig`
//            ExpressionNode condition = createBinaryExpressionNode(null, createIdentifierToken(SELF +
//                            DOT_TOKEN.stringValue() + API_KEY_CONFIG_PARAM),
//                    createToken(IS_KEYWORD),
//                    createIdentifierToken(API_KEYS_CONFIG));
//            IfElseStatementNode ifBlock = createIfElseStatementNode(createToken(IF_KEYWORD), condition, ifBody, null);
//            statementsList.add(ifBlock);
//        }
//    }

//    /**
//     * Generate VariableDeclarationNode for query parameter encoding map which includes the data related serialization
//     * mechanism that needs to be used with object or array type parameters. Parameters in primitive types will not be
//     * included to the map even when the serialization mechanisms are specified. These data is given in the `style`
//     and
//     * `explode` sections of the OpenAPI definition. Style defines how multiple values are delimited and explode
//     * specifies whether arrays and objects should generate separate parameters
//     * <p>
//     * --ex: {@code map<Encoding> queryParamEncoding = {"expand": ["deepObject", true]};}
//     *
//     * @param queryParameters List of query parameters defined in a particular function
//     * @return {@link VariableDeclarationNode}
//     * @throws BallerinaAsyncApiException When invalid referenced schema is given.
//     */
//    private VariableDeclarationNode getQueryParameterEncodingMap(List<Parameter> queryParameters)
//            throws BallerinaAsyncApiException {
//
//        List<Node> filedOfMap = new ArrayList<>();
//        BuiltinSimpleNameReferenceNode mapType = createBuiltinSimpleNameReferenceNode(null,
//                createIdentifierToken("map<" + ENCODING + ">"));
//        CaptureBindingPatternNode bindingPattern = createCaptureBindingPatternNode(
//                createIdentifierToken("queryParamEncoding"));
//        TypedBindingPatternNode bindingPatternNode = createTypedBindingPatternNode(mapType, bindingPattern);
//
//        for (Parameter parameter : queryParameters) {
//            Schema paramSchema = parameter.getSchema();
//            if (paramSchema.get$ref() != null) {
//                paramSchema = openAPI.getComponents().getSchemas().get(
//                        getValidName(extractReferenceType(paramSchema.get$ref()), true));
//            }
//            if (paramSchema != null && (paramSchema.getProperties() != null ||
//                    (paramSchema.getType() != null && paramSchema.getType().equals("array")) ||
//                    (paramSchema instanceof ComposedSchema))) {
//                if (parameter.getStyle() != null || parameter.getExplode() != null) {
//                    GeneratorUtils.createEncodingMap(filedOfMap, parameter.getStyle().toString(),
//                            parameter.getExplode(), parameter.getName().trim());
//                }
//            }
//        }
//        if (!filedOfMap.isEmpty()) {
//            filedOfMap.remove(filedOfMap.size() - 1);
//            MappingConstructorExpressionNode initialize = createMappingConstructorExpressionNode(
//                    createToken(OPEN_BRACE_TOKEN), createSeparatedNodeList(filedOfMap),
//                    createToken(CLOSE_BRACE_TOKEN));
//            return createVariableDeclarationNode(createEmptyNodeList(),
//                    null, bindingPatternNode, createToken(EQUAL_TOKEN), initialize,
//                    createToken(SEMICOLON_TOKEN));
//        }
//        return null;
//
//    }

//    /**
//     * Provides the list of security schemes available for the given operation.
//     *
//     * @param operation Current operation
//     * @return Security schemes that can be used to authorize the given operation
//     */
//    private Set<String> getSecurityRequirementForOperation(Operation operation) {
//
//        Set<String> securitySchemasAvailable = new LinkedHashSet<>();
//        List<SecurityRequirement> securityRequirements = new ArrayList<>();
//        if (operation.getSecurity() != null) {
//            securityRequirements = operation.getSecurity();
//        } else if (openAPI.getSecurity() != null) {
//            securityRequirements = openAPI.getSecurity();
//        }
//
//        if (securityRequirements.size() > 0) {
//            for (SecurityRequirement requirement : securityRequirements) {
//                securitySchemasAvailable.addAll(requirement.keySet());
//            }
//        }
//        return securitySchemasAvailable;
//    }
//
//    /**
//     * Handle request body in operation.
//     */
//    private void handleRequestBodyInOperation(List<StatementNode> statementsList, String method, String returnType,
//                                              RequestBody requestBody)
//            throws BallerinaOpenApiException {
//
//        if (requestBody.getContent() != null) {
//            Content rbContent = requestBody.getContent();
//            Set<Map.Entry<String, MediaType>> entries = rbContent.entrySet();
//            Iterator<Map.Entry<String, MediaType>> iterator = entries.iterator();
//            //Currently align with first content of the requestBody
//            while (iterator.hasNext()) {
//                createRequestBodyStatements(isHeader, statementsList, method, returnType, iterator);
//                break;
//            }
//        } else if (requestBody.get$ref() != null) {
//            RequestBody requestBodySchema =
//                    openAPI.getComponents().getRequestBodies().get(extractReferenceType(requestBody.get$ref()));
//            Content rbContent = requestBodySchema.getContent();
//            Set<Map.Entry<String, MediaType>> entries = rbContent.entrySet();
//            Iterator<Map.Entry<String, MediaType>> iterator = entries.iterator();
//            //Currently align with first content of the requestBody
//            while (iterator.hasNext()) {
//                createRequestBodyStatements(isHeader, statementsList, method, returnType, iterator);
//                break;
//            }
//        }
//    }

    /**
     * Generate common statements in function body.
     */
    private void createSimpleRPCFunctionBodyStatements(List<StatementNode> statementsList, String requestType,
                                                       String responseType, String dispatcherStreamId, List<MatchClauseNode> matchStatementList) {



        //requestType substring
//        char requestTypeFirstChar = Character.toLowerCase(requestType.charAt(0)); // Lowercase the first character
//        String requestRemainingString = requestType.substring(1);
//        String requestTypeCamelCaseName=requestTypeFirstChar+requestRemainingString;
        String requestTypePipe=requestType+"Pipe";
        //responseType substring
        String responseTypeCamelCaseName=null;
        if(responseType.contains(PIPE)){
            responseTypeCamelCaseName = "unionResult";

        }else {
            char responseTypeFirstChar = Character.toLowerCase(responseType.charAt(0)); // Lowercase the first character
            String responseRemainingString = responseType.substring(1);
            responseTypeCamelCaseName = responseTypeFirstChar + responseRemainingString;
        }

        Token equalToken = createToken(EQUAL_TOKEN);
        Token semicolonToken = createToken(SEMICOLON_TOKEN);
        Token openBracketToken= createToken(OPEN_BRACKET_TOKEN);
        Token closeBracketToken=createToken(CLOSE_BRACKET_TOKEN);
        Token dotToken =createToken(DOT_TOKEN);
        Token closeParenToken = createToken(CLOSE_PAREN_TOKEN);
        Token openParenToken=createToken(OPEN_PAREN_TOKEN);
        Token openBraceToken=createToken(OPEN_BRACE_TOKEN);
        Token closeBraceToken =createToken(CLOSE_BRACE_TOKEN);
        Token rightDoubleArrow=createToken(RIGHT_DOUBLE_ARROW_TOKEN);



//

        SimpleNameReferenceNode requestTypePipeNode=createSimpleNameReferenceNode(createIdentifierToken(requestTypePipe));
        SimpleNameReferenceNode responseMessageNode=createSimpleNameReferenceNode(createIdentifierToken(RESPONSE_MESSAGE_VAR_NAME));
        QualifiedNameReferenceNode pipeTypeCombined = createQualifiedNameReferenceNode(createIdentifierToken(SIMPLE_PIPE),
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
        VariableDeclarationNode remotePipeTypeEnsureStatement= createVariableDeclarationNode(createEmptyNodeList(),null,
                createTypedBindingPatternNode(
                        pipeTypeCombined,
                        createFieldBindingPatternVarnameNode(requestTypePipeNode)),
                equalToken,expressionNode,semicolonToken);
        statementsList.add(remotePipeTypeEnsureStatement);

        //tuple["id"] = id;

        createCommentStatementsForDispatcherId(statementsList, requestType,dispatcherStreamId, requestTypePipe);


        //Create pipes using request Type names when there is no dispatcherStreamId
        if(dispatcherStreamId==null){
            ArrayList<StatementNode> statementNodes=new ArrayList<>();
//                NodeList<StatementNode> statementNodes=createNodeList(matchStatementNode);
            //pipe:Pipe idPipe = check self.pipes[id].ensureType();
            QualifiedNameReferenceNode pipeTypeName = createQualifiedNameReferenceNode(createIdentifierToken(SIMPLE_PIPE),
                    createToken(COLON_TOKEN), createIdentifierToken(GeneratorConstants.CAPITAL_PIPE));
            IndexedExpressionNode selfPipes=createIndexedExpressionNode(createFieldAccessExpressionNode(
                            createSimpleNameReferenceNode(createIdentifierToken(SELF)), createToken(DOT_TOKEN),
                            createSimpleNameReferenceNode(createIdentifierToken(PIPES))), openBracketToken,
                    createSeparatedNodeList(createSimpleNameReferenceNode(createIdentifierToken("\""+requestType+"\""))),
                    closeBracketToken);
            MethodCallExpressionNode methodCallExpressionNode= createMethodCallExpressionNode(selfPipes, dotToken,
                    createSimpleNameReferenceNode(createIdentifierToken(ENSURE_TYPE)),
                    openParenToken,createSeparatedNodeList(), closeParenToken);

            CheckExpressionNode selfPipeCheck=createCheckExpressionNode(null,createToken(CHECK_KEYWORD),
                    methodCallExpressionNode);
//            SimpleNameReferenceNode responseTypePipeNode=createSimpleNameReferenceNode(createIdentifierToken(requestTypePipeNode));
            VariableDeclarationNode pipeTypeEnsureStatement= createVariableDeclarationNode(createEmptyNodeList(),null,
                    createTypedBindingPatternNode(
                            pipeTypeName,
                            createFieldBindingPatternVarnameNode(requestTypePipeNode)),
                    equalToken,selfPipeCheck, semicolonToken);
            statementNodes.add(pipeTypeEnsureStatement);

            List<Node> nodes=new ArrayList<>();
            nodes.add(responseMessageNode);
            nodes.add(createToken(COMMA_TOKEN));
            nodes.add(createIdentifierToken("5"));
            MethodCallExpressionNode pipeProduceExpressionNode= createMethodCallExpressionNode(requestTypePipeNode, dotToken,
                    createSimpleNameReferenceNode(createIdentifierToken(PRODUCE)),
                    openParenToken,createSeparatedNodeList(nodes), closeParenToken);

            CheckExpressionNode pipeProduceCheck=createCheckExpressionNode(null,createToken(CHECK_KEYWORD),
                    pipeProduceExpressionNode);
            ExpressionStatementNode pipeProduceExpression= createExpressionStatementNode(null,
                    pipeProduceCheck,createToken(SEMICOLON_TOKEN));
            statementNodes.add(pipeProduceExpression);


            MatchClauseNode matchClauseNode=createMatchClauseNode(createSeparatedNodeList(
                            createIdentifierToken("\""+responseType+"\"")),null,rightDoubleArrow,
                    createBlockStatementNode(openBraceToken,createNodeList(statementNodes),closeBraceToken));
            matchStatementList.add(matchClauseNode);
        }


        PositionalArgumentNode responseTypeTimeOut = createPositionalArgumentNode(createRequiredExpressionNode(createIdentifierToken("timeout")));

        if(!requestType.equals("error")) {
            // check self.writeMessageQueue.produce(tuple, timeout);
            List<Node> argumentArrays = new ArrayList<>();
            PositionalArgumentNode requestTypeName = createPositionalArgumentNode(createRequiredExpressionNode(createIdentifierToken(requestType)));
            argumentArrays.add(requestTypeName);
            argumentArrays.add(createToken(COMMA_TOKEN));
            argumentArrays.add(responseTypeTimeOut);
            FieldAccessExpressionNode globalQueue = createFieldAccessExpressionNode(
                    createSimpleNameReferenceNode(createIdentifierToken(SELF)), dotToken,
                    createSimpleNameReferenceNode(createIdentifierToken(WRITE_MESSAGE_QUEUE)));
            CheckExpressionNode callGlobalQueueProduce = createCheckExpressionNode(null, createToken(CHECK_KEYWORD),
                    createMethodCallExpressionNode(globalQueue, dotToken,
                            createSimpleNameReferenceNode(createIdentifierToken(PRODUCE)), openParenToken, createSeparatedNodeList(
                                    argumentArrays
                            ), closeParenToken));
            ExpressionStatementNode callGlobalQueueProduceNode = createExpressionStatementNode(null, callGlobalQueueProduce, semicolonToken);
            statementsList.add(callGlobalQueueProduceNode);
        }


        // anydata user = check tuplePipe.consume(timeout);
        SimpleNameReferenceNode responseTypeName =createSimpleNameReferenceNode(createIdentifierToken(responseType));
        SimpleNameReferenceNode anydata =createSimpleNameReferenceNode(createIdentifierToken("anydata"));
        SimpleNameReferenceNode responseMessageVarNode =createSimpleNameReferenceNode(createIdentifierToken("responseMessage"));
        SimpleNameReferenceNode responseNameNode;
        if(!requestType.equals("error")) {
           responseNameNode = createSimpleNameReferenceNode(createIdentifierToken(responseTypeCamelCaseName));
        }else{
            responseNameNode = createSimpleNameReferenceNode(createIdentifierToken(responseTypeCamelCaseName+"Message"));

        }

        CheckExpressionNode callRelevantPipeConsumeNode=createCheckExpressionNode(null,createToken(CHECK_KEYWORD),
                createMethodCallExpressionNode(requestTypePipeNode,dotToken,
                        createSimpleNameReferenceNode(createIdentifierToken(CONSUME)),openParenToken,createSeparatedNodeList(
                                responseTypeTimeOut
                        ),closeParenToken));
        VariableDeclarationNode callRelevantPipeProduceVar= createVariableDeclarationNode(createEmptyNodeList(),null,
                createTypedBindingPatternNode(
                        anydata,
                        createFieldBindingPatternVarnameNode(responseMessageVarNode)),
                equalToken,callRelevantPipeConsumeNode,semicolonToken);
        statementsList.add(callRelevantPipeProduceVar);


        //PongMessage pongMessage = check responseMessage.cloneWithType();
        MethodCallExpressionNode cloneWithTypeMethodCallExpressionNode= createMethodCallExpressionNode(
                responseMessageVarNode,dotToken,
                createSimpleNameReferenceNode(createIdentifierToken("cloneWithType")),
                openParenToken,createSeparatedNodeList(),closeParenToken);

        CheckExpressionNode cloneWithTypeCheck=createCheckExpressionNode(null,createToken(CHECK_KEYWORD),
                cloneWithTypeMethodCallExpressionNode);



        VariableDeclarationNode responseTypeCloneStatement= createVariableDeclarationNode(createEmptyNodeList(),null,
                createTypedBindingPatternNode(
                        responseTypeName,
                        createFieldBindingPatternVarnameNode(
                                responseNameNode)),
                equalToken,cloneWithTypeCheck,semicolonToken);
        statementsList.add(responseTypeCloneStatement);



        //check pongMessagePipe.immediateClose();
        CheckExpressionNode immediateCloseCheck=createCheckExpressionNode(null,createToken(CHECK_KEYWORD),createMethodCallExpressionNode(requestTypePipeNode,dotToken,createSimpleNameReferenceNode(createIdentifierToken("immediateClose")),openParenToken,createSeparatedNodeList(),closeParenToken));

        ExpressionStatementNode immediateCloseExpressionNode = createExpressionStatementNode(null,immediateCloseCheck,createToken(SEMICOLON_TOKEN));
        statementsList.add(immediateCloseExpressionNode);




//        HashMap requestTypes;
//        if(responseType.contains(PIPE)){
//            String[] unionResponses=responseType.split("\\"+PIPE);
//            for(String response:unionResponses){
//                if(responseMap.get(response)!=null){
//                    requestTypes=responseMap.get(response);
//                    requestTypes.put(requestType,count);
//                }else{
//                    requestTypes=new LinkedHashMap<>();
//                    requestTypes.put(requestType,count);
//                }
//                responseMap.put(response,requestTypes);
//            }
//        }
//        else {
//            if (responseMap.get(responseType) != null) {
//                requestTypes = responseMap.get(responseType);
//                requestTypes.put(requestType,count);
//            } else {
//                requestTypes = new LinkedHashMap();
//                requestTypes.put(requestType,count);
//            }
//            responseMap.put(responseType, requestTypes);
//        }









//        String clientCallStatement = "check self.clientEp->" + "writeMessage" + "(" + paramName + ")";
//        ExpressionStatementNode clientCall = createExpressionStatementNode(SyntaxKind.CHECK_EXPRESSION,
//                createSimpleNameReferenceNode(createIdentifierToken(clientCallStatement)),
//                createToken(SEMICOLON_TOKEN));
//        String clientReadStatement = "check self.clientEp->" + "readMessage" + "(" + ")";
//        }
        //Return Variable
//        VariableDeclarationNode clientRead = GeneratorUtils.getSimpleStatement(returnType, RESPONSE,
//                clientReadStatement);
//        statementsList.add(clientCall);
//        statementsList.add(clientRead);
        Token returnKeyWord = createIdentifierToken("return");

        ReturnStatementNode returnStatementNode = createReturnStatementNode(returnKeyWord, responseNameNode,
                createToken(SEMICOLON_TOKEN));
        statementsList.add(returnStatementNode);
    }

    private static void createCommentStatementsForDispatcherId(List<StatementNode> statementsList, String requestType,  String dispatcherStreamId, String requestTypePipe) {

        SimpleNameReferenceNode requestypePipeNode=createSimpleNameReferenceNode(createIdentifierToken(requestTypePipe));
        Token equalToken = createToken(EQUAL_TOKEN);
        Token semicolonToken = createToken(SEMICOLON_TOKEN);
        Token openBracketToken= createToken(OPEN_BRACKET_TOKEN);
        Token closeBracketToken=createToken(CLOSE_BRACKET_TOKEN);
        Token dotToken =createToken(DOT_TOKEN);
        Token openBraceToken=createToken(OPEN_BRACE_TOKEN);
        Token closeBraceToken =createToken(CLOSE_BRACE_TOKEN);


        ArrayList<StatementNode> lockStatements=new ArrayList<>();
        //Create remote function body when dispatcherStreamId is present
        if(dispatcherStreamId!=null) {
            SimpleNameReferenceNode dispatcherStreamIdNode= createSimpleNameReferenceNode(createIdentifierToken(dispatcherStreamId));
            //string id;
            VariableDeclarationNode dispatcherStreamIdDefineNode= createVariableDeclarationNode(createEmptyNodeList(),null,
                    createTypedBindingPatternNode(
                            createSimpleNameReferenceNode(createIdentifierToken("string")),
                            createFieldBindingPatternVarnameNode(dispatcherStreamIdNode)),
                    null,null, semicolonToken);
            statementsList.add(dispatcherStreamIdDefineNode);

            //lock{
            //      id = uuid:createType1AsString();
            //      self.pipes["Pongmessage"] = pongMessagePipe;
            // }

            QualifiedNameReferenceNode uuidNode = createQualifiedNameReferenceNode(createIdentifierToken(UUID),
                    createToken(COLON_TOKEN), createIdentifierToken(CREATE_TYPE1_AS_STRING));
            AssignmentStatementNode uuidAssignmentNode= createAssignmentStatementNode(dispatcherStreamIdNode,
                    equalToken,uuidNode, semicolonToken);
            lockStatements.add(uuidAssignmentNode);
            IndexedExpressionNode remoteSelfPipes=createIndexedExpressionNode(createFieldAccessExpressionNode(
                            createSimpleNameReferenceNode(createIdentifierToken(SELF)), dotToken,
                            createSimpleNameReferenceNode(createIdentifierToken("pipes"))), openBracketToken,
                    createSeparatedNodeList(createSimpleNameReferenceNode(createIdentifierToken(dispatcherStreamId))),
                    closeBracketToken);
            AssignmentStatementNode selfPipesAssignmentStatementNode = createAssignmentStatementNode(remoteSelfPipes,
                    equalToken, requestypePipeNode, semicolonToken);

            lockStatements.add(selfPipesAssignmentStatementNode);
            LockStatementNode functionLockStatementNode= createLockStatementNode(createToken(LOCK_KEYWORD),createBlockStatementNode(openBraceToken,createNodeList(lockStatements), closeBraceToken),null);
            statementsList.add(functionLockStatementNode);

            // pingMessage["id"]=id;
            IndexedExpressionNode requestTypeVarRef = createIndexedExpressionNode(createSimpleNameReferenceNode(createIdentifierToken(requestType)), openBracketToken,
                    createSeparatedNodeList(createSimpleNameReferenceNode(createIdentifierToken("\"" + dispatcherStreamId + "\""))),
                    closeBracketToken);
            AssignmentStatementNode idValueAssignmentStatementNode = createAssignmentStatementNode(requestTypeVarRef,
                    equalToken, dispatcherStreamIdNode, semicolonToken);
            statementsList.add(idValueAssignmentStatementNode);


        }else{

//            MatchStatementNode matchStatementNode=createMatchStatementNode(createToken(MATCH_KEYWORD),
////                        createBracedExpressionNode(null,openParenToken,
////                                createSimpleNameReferenceNode(createIdentxifierToken(dispatcherStreamId)),
////                                closeParenToken),openBraceToken,createNodeList(internalMatchStatementList),
////                        closeBraceToken,null);


            //self.pipes["tuplePipe"] = tuplePipe;
            IndexedExpressionNode remoteSelfPipes=createIndexedExpressionNode(createFieldAccessExpressionNode(
                            createSimpleNameReferenceNode(createIdentifierToken(SELF)), dotToken,
                            createSimpleNameReferenceNode(createIdentifierToken("pipes"))), openBracketToken,
                    createSeparatedNodeList(createSimpleNameReferenceNode(createIdentifierToken("\""+ requestType +"\""))),
                    closeBracketToken);
            AssignmentStatementNode selfPipesAssignmentStatementNode = createAssignmentStatementNode(remoteSelfPipes,
                    equalToken, requestypePipeNode, semicolonToken);
            lockStatements.add(selfPipesAssignmentStatementNode);

            LockStatementNode functionLockStatementNode= createLockStatementNode(createToken(LOCK_KEYWORD),createBlockStatementNode(openBraceToken,createNodeList(lockStatements), closeBraceToken),null);
            statementsList.add(functionLockStatementNode);

        }
    }


//    /**
//     * This function for creating requestBody statements.
//     * -- ex: Request body with json payload.
//     * <pre>
//     *    http:Request request = new;
//     *    json jsonBody = payload.toJson();
//     *    request.setPayload(jsonBody, "application/json");
//     *    json response = check self.clientEp->put(path, request);
//     * </pre>
//     *
//     * @param isHeader       - Boolean value for header availability.
//     * @param statementsList - StatementNode list in body node
//     * @param method         - Operation method name.
//     * @param returnType     - Response type
//     * @param iterator       - RequestBody media type
//     */
//    private void createRequestBodyStatements(boolean isHeader, List<StatementNode> statementsList,
//                                             String method, String returnType, Iterator<Map.Entry<String,
//            MediaType>> iterator)
//            throws BallerinaAsyncApiException {
//
//        //Create Request statement
//        Map.Entry<String, MediaType> mediaTypeEntry = iterator.next();
//        if (GeneratorUtils.isSupportedMediaType(mediaTypeEntry)) {
//            VariableDeclarationNode requestVariable = GeneratorUtils.getSimpleStatement(HTTP_REQUEST,
//                    REQUEST, NEW);
//            statementsList.add(requestVariable);
//        }
//        if (mediaTypeEntry.getValue() != null && GeneratorUtils.isSupportedMediaType(mediaTypeEntry)) {
//            genStatementsForRequestMediaType(statementsList, mediaTypeEntry);
//            // TODO:Fill with other mime type
//        } else {
//            // Add default value comment
//            ExpressionStatementNode expressionStatementNode = GeneratorUtils.getSimpleExpressionStatementNode(
//                    "// TODO: Update the request as needed");
//            statementsList.add(expressionStatementNode);
//        }
//        // POST, PUT, PATCH, DELETE, EXECUTE
//        VariableDeclarationNode requestStatement =
//                GeneratorUtils.getSimpleStatement(returnType, RESPONSE, "check self.clientEp->"
//                        + method + "(" + RESOURCE_PATH + ", request)");
//        if (isHeader) {
//            if (method.equals(POST) || method.equals(PUT) || method.equals(PATCH) || method.equals(DELETE)
//                    || method.equals(EXECUTE)) {
//                requestStatement = GeneratorUtils.getSimpleStatement(returnType, RESPONSE,
//                        "check self.clientEp->" + method + "(" + RESOURCE_PATH + ", request, " +
//                                HTTP_HEADERS + ")");
//                statementsList.add(requestStatement);
//                Token returnKeyWord = createIdentifierToken("return");
//                SimpleNameReferenceNode returns = createSimpleNameReferenceNode(createIdentifierToken(RESPONSE));
//                ReturnStatementNode returnStatementNode = createReturnStatementNode(returnKeyWord, returns,
//                        createToken(SEMICOLON_TOKEN));
//                statementsList.add(returnStatementNode);
//            }
//        } else {
//            statementsList.add(requestStatement);
//            Token returnKeyWord = createIdentifierToken("return");
//            SimpleNameReferenceNode returnVariable = createSimpleNameReferenceNode(createIdentifierToken(RESPONSE));
//            ReturnStatementNode returnStatementNode = createReturnStatementNode(returnKeyWord, returnVariable,
//                    createToken(SEMICOLON_TOKEN));
//            statementsList.add(returnStatementNode);
//        }
//    }
//
//    /**
//     * This function is used for generating function body statements according to the given request body media type.
//     *
//     * @param statementsList - Previous statements list
//     * @param mediaTypeEntry - Media type entry
//     */
//    private void genStatementsForRequestMediaType(List<StatementNode> statementsList,
//                                                  Map.Entry<String, MediaType> mediaTypeEntry)
//            throws BallerinaOpenApiException {
//        MimeFactory factory = new MimeFactory();
//        MimeType mimeType = factory.getMimeType(mediaTypeEntry, ballerinaUtilGenerator, imports);
//        mimeType.setPayload(statementsList, mediaTypeEntry);
//    }

    /**
     * This util function for getting type to the targetType data binding.
     *
     * @param rType - Given Data type
     * @return - return type
     */
    private String returnTypeForTargetTypeField(String rType) {

        String returnType;
        int index = rType.lastIndexOf("|");
        returnType = rType.substring(0, index);
        return (rType.contains(NILLABLE) ? returnType + NILLABLE : returnType);
    }

//    /**
//     * Generate map variable for query parameters and headers.
//     */
//    private VariableDeclarationNode getMapForParameters(List<Parameter> parameters, String mapDataType,
//                                                        String mapName, List<String> apiKeyNames) {
//        List<Node> filedOfMap = new ArrayList<>();
//        BuiltinSimpleNameReferenceNode mapType = createBuiltinSimpleNameReferenceNode(null,
//                createIdentifierToken(mapDataType));
//        CaptureBindingPatternNode bindingPattern = createCaptureBindingPatternNode(
//                createIdentifierToken(mapName));
//        TypedBindingPatternNode bindingPatternNode = createTypedBindingPatternNode(mapType, bindingPattern);
//
//        for (Parameter parameter : parameters) {
//            // Initializer
//            IdentifierToken fieldName = createIdentifierToken('"' + (parameter.getName().trim()) + '"');
//            Token colon = createToken(COLON_TOKEN);
//            SimpleNameReferenceNode valueExpr = createSimpleNameReferenceNode(
//                    createIdentifierToken(getValidName(parameter.getName().trim(), false)));
//            SpecificFieldNode specificFieldNode = createSpecificFieldNode(null,
//                    fieldName, colon, valueExpr);
//            filedOfMap.add(specificFieldNode);
//            filedOfMap.add(createToken(COMMA_TOKEN));
//        }
//
//        if (!apiKeyNames.isEmpty()) {
//            for (String apiKey : apiKeyNames) {
//                IdentifierToken fieldName = createIdentifierToken('"' + apiKey.trim() + '"');
//                Token colon = createToken(COLON_TOKEN);
//                IdentifierToken apiKeyConfigIdentifierToken = createIdentifierToken(API_KEY_CONFIG_PARAM);
//                if (ballerinaAuthConfigGenerator.isHttpOROAuth() && ballerinaAuthConfigGenerator.isHttpApiKey()) {
//                    apiKeyConfigIdentifierToken = createIdentifierToken(API_KEY_CONFIG_PARAM +
//                            QUESTION_MARK_TOKEN.stringValue());
//                }
//                SimpleNameReferenceNode apiKeyConfigParamNode = createSimpleNameReferenceNode(
//                        apiKeyConfigIdentifierToken);
//                FieldAccessExpressionNode fieldExpr = createFieldAccessExpressionNode(
//                        createSimpleNameReferenceNode(createIdentifierToken(SELF)), createToken(DOT_TOKEN),
//                        apiKeyConfigParamNode);
//                SimpleNameReferenceNode valueExpr = createSimpleNameReferenceNode(createIdentifierToken(
//                        getValidName(apiKey, false)));
//                SpecificFieldNode specificFieldNode;
//                ExpressionNode apiKeyExpr = createFieldAccessExpressionNode(
//                        fieldExpr, createToken(DOT_TOKEN), valueExpr);
//                specificFieldNode = createSpecificFieldNode(null, fieldName, colon, apiKeyExpr);
//                filedOfMap.add(specificFieldNode);
//                filedOfMap.add(createToken(COMMA_TOKEN));
//            }
//        }
//
//        filedOfMap.remove(filedOfMap.size() - 1);
//        MappingConstructorExpressionNode initialize = createMappingConstructorExpressionNode(
//                createToken(OPEN_BRACE_TOKEN), createSeparatedNodeList(filedOfMap),
//                createToken(CLOSE_BRACE_TOKEN));
//        return createVariableDeclarationNode(createEmptyNodeList(),
//                null, bindingPatternNode, createToken(EQUAL_TOKEN), initialize,
//                createToken(SEMICOLON_TOKEN));
//    }
}
