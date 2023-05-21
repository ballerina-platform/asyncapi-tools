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
import io.apicurio.datamodels.models.ServerVariable;
import io.apicurio.datamodels.models.asyncapi.AsyncApiMessage;
import io.apicurio.datamodels.models.asyncapi.AsyncApiServer;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25DocumentImpl;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25InfoImpl;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25MessageImpl;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25ServerImpl;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25ServersImpl;
import io.ballerina.asyncapi.core.GeneratorConstants;
import io.ballerina.asyncapi.core.GeneratorUtils;
import io.ballerina.asyncapi.core.exception.BallerinaAsyncApiException;
import io.ballerina.asyncapi.core.generators.client.model.AASClientConfig;
import io.ballerina.asyncapi.core.generators.document.DocCommentsGenerator;
import io.ballerina.asyncapi.core.generators.schema.BallerinaTypesGenerator;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.AssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.BlockStatementNode;
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.ExpressionStatementNode;
import io.ballerina.compiler.syntax.tree.FieldAccessExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.FunctionBodyNode;
import io.ballerina.compiler.syntax.tree.FunctionCallExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.ImplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.IndexedExpressionNode;
import io.ballerina.compiler.syntax.tree.MapTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.MarkdownDocumentationLineNode;
import io.ballerina.compiler.syntax.tree.MarkdownDocumentationNode;
import io.ballerina.compiler.syntax.tree.MarkdownParameterDocumentationLineNode;
import io.ballerina.compiler.syntax.tree.MatchClauseNode;
import io.ballerina.compiler.syntax.tree.MatchStatementNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.MethodCallExpressionNode;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.ObjectFieldNode;
import io.ballerina.compiler.syntax.tree.OptionalTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.ParenthesizedArgList;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.ReturnStatementNode;
import io.ballerina.compiler.syntax.tree.ReturnTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.compiler.syntax.tree.TypeParameterNode;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocuments;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.ballerina.asyncapi.core.GeneratorConstants.CLIENT_EP;
import static io.ballerina.asyncapi.core.GeneratorConstants.CONSUME;
import static io.ballerina.asyncapi.core.GeneratorConstants.DEFAULT_API_KEY_DESC;
import static io.ballerina.asyncapi.core.GeneratorConstants.DEFAULT_TIME_OUT;
import static io.ballerina.asyncapi.core.GeneratorConstants.ENSURE_TYPE;
import static io.ballerina.asyncapi.core.GeneratorConstants.LANG_RUNTIME;
import static io.ballerina.asyncapi.core.GeneratorConstants.NUVINDU_PIPE;
import static io.ballerina.asyncapi.core.GeneratorConstants.PIPE;
import static io.ballerina.asyncapi.core.GeneratorConstants.PIPES;
import static io.ballerina.asyncapi.core.GeneratorConstants.PIPE_TRIGGER;
import static io.ballerina.asyncapi.core.GeneratorConstants.PRODUCE;
import static io.ballerina.asyncapi.core.GeneratorConstants.READ_MESSAGE;
import static io.ballerina.asyncapi.core.GeneratorConstants.READ_MESSAGE_QUEUE;
import static io.ballerina.asyncapi.core.GeneratorConstants.REMOTE_METHOD_NAME_PREFIX;
import static io.ballerina.asyncapi.core.GeneratorConstants.REQUEST_MESSAGE;
import static io.ballerina.asyncapi.core.GeneratorConstants.RESPONSE_MESSAGE;
import static io.ballerina.asyncapi.core.GeneratorConstants.RESPONSE_MESSAGE_VAR_NAME;
import static io.ballerina.asyncapi.core.GeneratorConstants.RUNTIME;
import static io.ballerina.asyncapi.core.GeneratorConstants.SELF;
import static io.ballerina.asyncapi.core.GeneratorConstants.SLEEP;
import static io.ballerina.asyncapi.core.GeneratorConstants.START_MESSAGE_READING;
import static io.ballerina.asyncapi.core.GeneratorConstants.START_MESSAGE_WRITING;
import static io.ballerina.asyncapi.core.GeneratorConstants.START_PIPE_TRIGGERING;
import static io.ballerina.asyncapi.core.GeneratorConstants.STRING;
import static io.ballerina.asyncapi.core.GeneratorConstants.WEBSOCKET;
import static io.ballerina.asyncapi.core.GeneratorConstants.WORKER_SLEEP_TIME_OUT;
import static io.ballerina.asyncapi.core.GeneratorConstants.WRITE_MESSAGE;
import static io.ballerina.asyncapi.core.GeneratorConstants.WRITE_MESSAGE_QUEUE;
import static io.ballerina.asyncapi.core.GeneratorConstants.X_BALLERINA_INIT_DESCRIPTION;
import static io.ballerina.asyncapi.core.GeneratorConstants.X_RESPONSE;
import static io.ballerina.asyncapi.core.GeneratorConstants.SIMPLE_PIPE;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createSeparatedNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createAssignmentStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBasicLiteralNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBlockStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBracedExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBuiltinSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createCheckExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createClassDefinitionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createExpressionStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFieldAccessExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFieldBindingPatternVarnameNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionBodyBlockNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionCallExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionDefinitionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionSignatureNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createImplicitNewExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createIndexedExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMapTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMarkdownDocumentationLineNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMarkdownDocumentationNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMatchClauseNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMatchStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMetadataNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMethodCallExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createModulePartNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createNamedWorkerDeclarationNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createObjectFieldNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createOptionalTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createParenthesizedArgList;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createPositionalArgumentNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createQualifiedNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRequiredExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createReturnStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createReturnTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTypeParameterNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTypedBindingPatternNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createVariableDeclarationNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createWhileStatementNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ACTION_STATEMENT;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ANYDATA_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CHECK_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLASS_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLIENT_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACKET_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.COLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.COMMA_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.DOT_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.EOF_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.EQUAL_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ERROR_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FINAL_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FUNCTION_CALL;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FUNCTION_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.GT_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ISOLATED_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.LT_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.MAP_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.MATCH_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.NEW_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACKET_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PRIVATE_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PUBLIC_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.QUESTION_MARK_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.REMOTE_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RETURNS_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RETURN_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RIGHT_ARROW_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RIGHT_DOUBLE_ARROW_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SEMICOLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.TRUE_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.WHILE_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.WORKER_KEYWORD;

/**
 * This class is used to generate ballerina client file according to given yaml file.
 *
 * @since 1.3.0
 */
public class BallerinaClientGenerator {

    private final AsyncApi25DocumentImpl asyncAPI;
    private final BallerinaTypesGenerator ballerinaSchemaGenerator;
    private final BallerinaUtilGenerator ballerinaUtilGenerator;
    private final List<String> remoteFunctionNameList;
    private final BallerinaAuthConfigGenerator ballerinaAuthConfigGenerator;
    private final boolean resourceMode;
    //    private final Filter filters;
    private List<ImportDeclarationNode> imports;
    private List<TypeDefinitionNode> typeDefinitionNodeList;
    private List<String> apiKeyNameList = new ArrayList<>();
    private String serverURL;

    public BallerinaClientGenerator(AASClientConfig asyncAPIClientConfig) {

//        this.filters = oasClientConfig.getFilters();
        this.imports = new ArrayList<>();
        this.typeDefinitionNodeList = new ArrayList<>();
        this.asyncAPI = asyncAPIClientConfig.getOpenAPI();
        this.ballerinaSchemaGenerator = new BallerinaTypesGenerator(asyncAPI,
                asyncAPIClientConfig.isNullable(), new LinkedList<>());
        this.ballerinaUtilGenerator = new BallerinaUtilGenerator();
        this.remoteFunctionNameList = new ArrayList<>();
        this.serverURL = "/";
        this.ballerinaAuthConfigGenerator = new BallerinaAuthConfigGenerator(false, false, asyncAPI,
                ballerinaUtilGenerator);
        this.resourceMode = asyncAPIClientConfig.isResourceMode();
    }

    /**
     * Returns a list of type definition nodes.
     */
    public List<TypeDefinitionNode> getTypeDefinitionNodeList() {

        return typeDefinitionNodeList;
    }

    /**
     * Set the typeDefinitionNodeList.
     */
    public void setTypeDefinitionNodeList(
            List<TypeDefinitionNode> typeDefinitionNodeList) {

        this.typeDefinitionNodeList = typeDefinitionNodeList;
    }

    /**
     * Returns ballerinaAuthConfigGenerator.
     */
    public BallerinaAuthConfigGenerator getBallerinaAuthConfigGenerator() {

        return ballerinaAuthConfigGenerator;
    }

    public List<String> getRemoteFunctionNameList() {

        return remoteFunctionNameList;
    }

    /**
     * Returns server URL.
     *
     * @return {@link String}
     */
    public String getServerUrl() {

        return serverURL;
    }

    /**
     * This method for generate the client syntax tree.
     *
     * @return return Syntax tree for the ballerina code.
     * @throws BallerinaAsyncApiException When function fail in process.
     */
    public SyntaxTree generateSyntaxTree() throws BallerinaAsyncApiException {

        // Create `ballerina/websocket` import declaration node
        ImportDeclarationNode importForWebsocket = GeneratorUtils.getImportDeclarationNode(GeneratorConstants.BALLERINA
                , WEBSOCKET);

        ImportDeclarationNode importForNuvinduPipe= GeneratorUtils.getImportDeclarationNode(GeneratorConstants.NUVINDU
                , NUVINDU_PIPE);
        ImportDeclarationNode importForRunTime= GeneratorUtils.getImportDeclarationNode(GeneratorConstants.BALLERINA
                , LANG_RUNTIME);

        imports.add(importForWebsocket);
        imports.add(importForNuvinduPipe);
        imports.add(importForRunTime);
        List<ModuleMemberDeclarationNode> nodes = new ArrayList<>();
        // Add authentication related records
        ballerinaAuthConfigGenerator.addAuthRelatedRecords(asyncAPI);

        // Add class definition node to module member nodes
        nodes.add(getClassDefinitionNode());

        NodeList<ImportDeclarationNode> importsList = createNodeList(imports);
        ModulePartNode modulePartNode =
                createModulePartNode(importsList, createNodeList(nodes), createToken(EOF_TOKEN));
        TextDocument textDocument = TextDocuments.from("");
        SyntaxTree syntaxTree = SyntaxTree.from(textDocument);
        return syntaxTree.modifyWith(modulePartNode);
    }

//    public BallerinaUtilGenerator getBallerinaUtilGenerator() {
//
//        return ballerinaUtilGenerator;
//    }

    /**
     * Generate Class definition Node with below code structure.
     * <pre>
     *     public isolated client class Client {
     *     final websocket:Client clientEp;
     *     public isolated function init(ConnectionConfig config =  {}, string serviceUrl = "/url")
     *     returns error? {
     *         http:Client httpEp = check new (serviceUrl, clientConfig);
     *         self.clientEp = httpEp;
     *     }
     *     // Remote functions
     *     remote isolated function pathParameter(int 'version, string name) returns string|error {
     *         string  path = string `/v1/${'version}/v2/${name}`;
     *         string response = check self.clientEp-> get(path);
     *         return response;
     *     }
     * }
     * </pre>
     */
    private ClassDefinitionNode getClassDefinitionNode() throws BallerinaAsyncApiException {
        // Collect members for class definition node
        List<Node> memberNodeList = new ArrayList<>();
        List<MatchClauseNode> matchStatementList=new ArrayList<>();
        // Add instance variable to class definition node
        memberNodeList.addAll(createClassInstanceVariables());
        // Add init function to class definition node
        memberNodeList.add(createInitFunction());
        // Add startInterMediator function
        memberNodeList.add(createStartMessageWriting());

        memberNodeList.add(createStartMessageReading());


        List<FunctionDefinitionNode> remoteFunctionNodes=createRemoteFunctions(asyncAPI.getComponents().getMessages(),matchStatementList);

        // Add callRelevantPipe function
        memberNodeList.add(createStartPipeTriggering(matchStatementList));

        // Add remoteFunctionNodes
        memberNodeList.addAll(remoteFunctionNodes);



        // Generate the class combining members
        MetadataNode metadataNode = getClassMetadataNode();

        String stringClassName = asyncAPI.getInfo().getTitle().trim() + GeneratorUtils.
                removeNonAlphanumeric(asyncAPI.getChannels().getItemNames().get(0).trim()) + "Client";
        IdentifierToken className = createIdentifierToken(stringClassName);
//        IdentifierToken className = createIdentifierToken(GeneratorConstants.CLIENT_CLASS);
//        NodeList<Token> classTypeQualifiers = createNodeList(
//                createToken(ISOLATED_KEYWORD), createToken(CLIENT_KEYWORD));
        NodeList<Token> classTypeQualifiers = createNodeList( createToken(CLIENT_KEYWORD));
        return createClassDefinitionNode(metadataNode, createToken(PUBLIC_KEYWORD), classTypeQualifiers,
                createToken(CLASS_KEYWORD), className, createToken(OPEN_BRACE_TOKEN),
                createNodeList(memberNodeList), createToken(CLOSE_BRACE_TOKEN), null);
    }

    private Node createStartMessageReading() {
        ArrayList initMetaDataDoc = new ArrayList();
        FunctionSignatureNode functionSignatureNode = getStartMessageReadingFunctionSignatureNode(initMetaDataDoc);
        FunctionBodyNode functionBodyNode = getStartMessageReadingFunctionBodyNode();
//        NodeList<Token> qualifierList = createNodeList(createToken(PRIVATE_KEYWORD), createToken(ISOLATED_KEYWORD));
        NodeList<Token> qualifierList = createNodeList(createToken(PRIVATE_KEYWORD));
        IdentifierToken functionName = createIdentifierToken(START_MESSAGE_READING);
        return createFunctionDefinitionNode(null, getInitDocComment(initMetaDataDoc), qualifierList,
                createToken(FUNCTION_KEYWORD),
                functionName, createEmptyNodeList(), functionSignatureNode, functionBodyNode);
    }

    private FunctionBodyNode getStartMessageReadingFunctionBodyNode() {
        NodeList<AnnotationNode> annotations = createEmptyNodeList();

        List<StatementNode> whileStatements =new ArrayList<>();
        Token openParanToken=createToken(OPEN_PAREN_TOKEN);
        Token closeParanToken=createToken(CLOSE_PAREN_TOKEN);
        Token openBraceToken=createToken(OPEN_BRACE_TOKEN);
        Token closeBraceToken=createToken(CLOSE_BRACE_TOKEN);
        Token semicolonToken = createToken(SEMICOLON_TOKEN);
        Token equalToken = createToken(EQUAL_TOKEN);
        Token dotToken = createToken(DOT_TOKEN);
        Token rightArrowToken = createToken(RIGHT_ARROW_TOKEN);




        //ResponseMessage responseMessage = check self.clientEp->readMessage();
        FieldAccessExpressionNode clientEp= createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(SELF)), createToken(DOT_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken(CLIENT_EP)));
        CheckExpressionNode responseMessageCheckExpressionNode=createCheckExpressionNode(null,createToken(CHECK_KEYWORD),
                createMethodCallExpressionNode(clientEp,rightArrowToken,
                        createSimpleNameReferenceNode(createIdentifierToken(READ_MESSAGE)),openParanToken,createSeparatedNodeList(new ArrayList<>()),closeParanToken));
        VariableDeclarationNode responseMessage= createVariableDeclarationNode(createEmptyNodeList(),null,createTypedBindingPatternNode(
                createBuiltinSimpleNameReferenceNode(null,createIdentifierToken(RESPONSE_MESSAGE)),
                createFieldBindingPatternVarnameNode(createSimpleNameReferenceNode(createIdentifierToken(RESPONSE_MESSAGE_VAR_NAME)))),
                equalToken,responseMessageCheckExpressionNode,semicolonToken);


        //check self.readMessageQueue.produce(responseMessage,5);
        FieldAccessExpressionNode readMessageQueue= createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(SELF)), createToken(DOT_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken(READ_MESSAGE_QUEUE)));
        CheckExpressionNode checkReadMessageQueueProduceNode=createCheckExpressionNode(null,createToken(CHECK_KEYWORD),
                createMethodCallExpressionNode(readMessageQueue,dotToken,
                        createSimpleNameReferenceNode(createIdentifierToken(PRODUCE)),openParanToken,createSeparatedNodeList(
                                createPositionalArgumentNode(createRequiredExpressionNode(createIdentifierToken(RESPONSE_MESSAGE_VAR_NAME))),
                                createToken(COMMA_TOKEN),
                                createPositionalArgumentNode(createRequiredExpressionNode(createIdentifierToken(DEFAULT_TIME_OUT)))
                        ),closeParanToken));
        ExpressionStatementNode readMessageQueueCheck=createExpressionStatementNode(null,
                checkReadMessageQueueProduceNode,createToken(SEMICOLON_TOKEN));



        //runtime:sleep(0.01);
        QualifiedNameReferenceNode qualifiedNameReferenceNode=createQualifiedNameReferenceNode(createIdentifierToken(
                RUNTIME),createToken(COLON_TOKEN),createIdentifierToken(SLEEP));
        FunctionCallExpressionNode sleep=createFunctionCallExpressionNode(qualifiedNameReferenceNode,openParanToken,
                createSeparatedNodeList(createPositionalArgumentNode(
                createRequiredExpressionNode(createIdentifierToken(WORKER_SLEEP_TIME_OUT)))),closeParanToken);
        ExpressionStatementNode runtimeSleep=createExpressionStatementNode(null,
                sleep,createToken(SEMICOLON_TOKEN));




//        whileStatements.add(writeMessage);
        whileStatements.add(responseMessage);
        whileStatements.add(readMessageQueueCheck);
        whileStatements.add(runtimeSleep);



        BlockStatementNode whileBody=createBlockStatementNode(openBraceToken,createNodeList(whileStatements),
                closeBraceToken);
        NodeList<StatementNode> workerStatements= createNodeList(createWhileStatementNode(createToken(WHILE_KEYWORD),
                createBasicLiteralNode(TRUE_KEYWORD,createToken(TRUE_KEYWORD)),whileBody,null));


        NodeList workerDeclarationNodes= createNodeList(createNamedWorkerDeclarationNode(annotations,null,createToken(WORKER_KEYWORD)
                ,createIdentifierToken(READ_MESSAGE),createReturnTypeDescriptorNode(
                        createToken(RETURNS_KEYWORD),createEmptyNodeList(),createToken(ERROR_KEYWORD)),
                createBlockStatementNode(openBraceToken,workerStatements,
                        closeBraceToken)));


        return createFunctionBodyBlockNode(openBraceToken,
                null, workerDeclarationNodes, closeBraceToken, null);




    }

    private FunctionSignatureNode getStartMessageReadingFunctionSignatureNode(ArrayList initMetaDataDoc) {
        //        serverURL = getServerURL((AsyncApi25ServersImpl) asyncAPI.getServers());
        SeparatedNodeList<ParameterNode> parameterList = createSeparatedNodeList(new ArrayList<>());
//
//        OptionalTypeDescriptorNode returnType = createOptionalTypeDescriptorNode(createToken(ERROR_KEYWORD),
//                createToken(QUESTION_MARK_TOKEN));
//        ReturnTypeDescriptorNode returnTypeDescriptorNode = createReturnTypeDescriptorNode(
//                createToken(RETURNS_KEYWORD), createEmptyNodeList(), returnType);
        return createFunctionSignatureNode(
                createToken(OPEN_PAREN_TOKEN), parameterList, createToken(CLOSE_PAREN_TOKEN),null);


    }

    private Node createStartPipeTriggering(List<MatchClauseNode> matchClauseNodes ) throws BallerinaAsyncApiException{
        ArrayList initMetaDataDoc = new ArrayList();
        FunctionSignatureNode functionSignatureNode = getStartPipeTriggeringFunctionSignatureNode(initMetaDataDoc);
        FunctionBodyNode functionBodyNode = getStartPipeTriggeringFunctionBodyNode("event","id",matchClauseNodes);
//        NodeList<Token> qualifierList = createNodeList(createToken(PRIVATE_KEYWORD), createToken(ISOLATED_KEYWORD));
        NodeList<Token> qualifierList = createNodeList(createToken(PRIVATE_KEYWORD));
        IdentifierToken functionName = createIdentifierToken(START_PIPE_TRIGGERING);
        return createFunctionDefinitionNode(null, getInitDocComment(initMetaDataDoc), qualifierList,
                createToken(FUNCTION_KEYWORD),
                functionName, createEmptyNodeList(), functionSignatureNode, functionBodyNode);
    }

    private FunctionBodyNode getStartPipeTriggeringFunctionBodyNode(String dispatcherKey, String dispatcherStreamId, List<MatchClauseNode> matchClauseNodes) {
        Token openParanToken=createToken(OPEN_PAREN_TOKEN);
        Token closeParanToken=createToken(CLOSE_PAREN_TOKEN);
        Token openBraceToken=createToken(OPEN_BRACE_TOKEN);
        Token closeBraceToken=createToken(CLOSE_BRACE_TOKEN);
        Token semicolonToken = createToken(SEMICOLON_TOKEN);
        Token equalToken = createToken(EQUAL_TOKEN);
        Token dotToken = createToken(DOT_TOKEN);
        NodeList<AnnotationNode> annotations = createEmptyNodeList();

        List<StatementNode> whileStatements =new ArrayList<>();





        List<StatementNode> callRelavantPipeStatements =new ArrayList<>();












        SimpleNameReferenceNode responseMessageNode=createSimpleNameReferenceNode(createIdentifierToken(RESPONSE_MESSAGE_VAR_NAME));


        //ResponseMessage responseMessage = check self.readMessageQueue.consume(5);
        FieldAccessExpressionNode globalQueue= createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(SELF)), createToken(DOT_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken(READ_MESSAGE_QUEUE)));
        CheckExpressionNode checkExpressionNode=createCheckExpressionNode(null,createToken(CHECK_KEYWORD),
                createMethodCallExpressionNode(globalQueue,dotToken,
                        createSimpleNameReferenceNode(createIdentifierToken(CONSUME)),openParanToken,createSeparatedNodeList(
                                createPositionalArgumentNode(createRequiredExpressionNode(createIdentifierToken(DEFAULT_TIME_OUT)))
                        ),closeParanToken));
        VariableDeclarationNode responseMessage= createVariableDeclarationNode(createEmptyNodeList(),null,createTypedBindingPatternNode(
                createBuiltinSimpleNameReferenceNode(null,createIdentifierToken(RESPONSE_MESSAGE)),
                createFieldBindingPatternVarnameNode(responseMessageNode)),equalToken,checkExpressionNode,semicolonToken);
        callRelavantPipeStatements.add(responseMessage);


        // string event=responseMessage.event;
       FieldAccessExpressionNode responseMessageEvent= createFieldAccessExpressionNode(responseMessageNode,dotToken,
               createSimpleNameReferenceNode(createIdentifierToken(dispatcherKey)));
       VariableDeclarationNode dispatcherKeyStatement= createVariableDeclarationNode(createEmptyNodeList(),
               null,createTypedBindingPatternNode(
                        createBuiltinSimpleNameReferenceNode(null,createIdentifierToken(STRING)),
                        createFieldBindingPatternVarnameNode(createSimpleNameReferenceNode(
                                createIdentifierToken(dispatcherKey)))),
                equalToken,responseMessageEvent,semicolonToken);
        callRelavantPipeStatements.add(dispatcherKeyStatement);

        // string id=responseMessage.id;
        FieldAccessExpressionNode responseMessageId= createFieldAccessExpressionNode(responseMessageNode,dotToken,
                createSimpleNameReferenceNode(createIdentifierToken(dispatcherStreamId)));
        VariableDeclarationNode dispatcherStreamStatement= createVariableDeclarationNode(createEmptyNodeList(),
                null,createTypedBindingPatternNode(
                        createBuiltinSimpleNameReferenceNode(null,createIdentifierToken(STRING)),
                        createFieldBindingPatternVarnameNode(createSimpleNameReferenceNode(
                                createIdentifierToken(dispatcherStreamId)))),
                equalToken,responseMessageId,semicolonToken);
        callRelavantPipeStatements.add(dispatcherStreamStatement);


       MatchStatementNode matchStatementNode=createMatchStatementNode(createToken(MATCH_KEYWORD),
               createBracedExpressionNode(null,openParanToken,
                       createSimpleNameReferenceNode(createIdentifierToken(dispatcherKey)),
                       closeParanToken),openBraceToken,createNodeList(matchClauseNodes),closeBraceToken,null);
       callRelavantPipeStatements.add(matchStatementNode);
       whileStatements.addAll(callRelavantPipeStatements);

        //statements
        BlockStatementNode whileBody=createBlockStatementNode(openBraceToken,createNodeList(whileStatements),
                closeBraceToken);
        NodeList<StatementNode> workerStatements= createNodeList(createWhileStatementNode(createToken(WHILE_KEYWORD),
                createBasicLiteralNode(TRUE_KEYWORD,createToken(TRUE_KEYWORD)),whileBody,null));


        NodeList workerDeclarationNodes= createNodeList(createNamedWorkerDeclarationNode(annotations,null,createToken(WORKER_KEYWORD)
                ,createIdentifierToken(PIPE_TRIGGER),createReturnTypeDescriptorNode(
                        createToken(RETURNS_KEYWORD),createEmptyNodeList(),createToken(ERROR_KEYWORD)),
                createBlockStatementNode(openBraceToken,workerStatements,
                        closeBraceToken)));

        return createFunctionBodyBlockNode(openBraceToken,
                null, workerDeclarationNodes, closeBraceToken, null);

    }

    private FunctionSignatureNode getStartPipeTriggeringFunctionSignatureNode(ArrayList initMetaDataNode) {
        //        serverURL = getServerURL((AsyncApi25ServersImpl) asyncAPI.getServers());


        SeparatedNodeList<ParameterNode> parameterList = createSeparatedNodeList();



        return createFunctionSignatureNode(
                createToken(OPEN_PAREN_TOKEN), parameterList, createToken(CLOSE_PAREN_TOKEN),null);
    }

    private Node createStartMessageWriting() throws BallerinaAsyncApiException {
        ArrayList initMetaDataDoc = new ArrayList();
        FunctionSignatureNode functionSignatureNode = getStartMessageWritingFunctionSignatureNode(initMetaDataDoc);
        FunctionBodyNode functionBodyNode = getStartMessageWritingFunctionBodyNode();
//        NodeList<Token> qualifierList = createNodeList(createToken(PRIVATE_KEYWORD), createToken(ISOLATED_KEYWORD));
        NodeList<Token> qualifierList = createNodeList(createToken(PRIVATE_KEYWORD));
        IdentifierToken functionName = createIdentifierToken(START_MESSAGE_WRITING);
        return createFunctionDefinitionNode(null, getInitDocComment(initMetaDataDoc), qualifierList,
                createToken(FUNCTION_KEYWORD),
                functionName, createEmptyNodeList(), functionSignatureNode, functionBodyNode);
    }

    private FunctionBodyNode getStartMessageWritingFunctionBodyNode() {
        NodeList<AnnotationNode> annotations = createEmptyNodeList();

        List<StatementNode> whileStatements =new ArrayList<>();
        Token openParanToken=createToken(OPEN_PAREN_TOKEN);
        Token closeParanToken=createToken(CLOSE_PAREN_TOKEN);
        Token openBraceToken=createToken(OPEN_BRACE_TOKEN);
        Token closeBraceToken=createToken(CLOSE_BRACE_TOKEN);
        Token semicolonToken = createToken(SEMICOLON_TOKEN);
        Token equalToken = createToken(EQUAL_TOKEN);
        Token dotToken = createToken(DOT_TOKEN);
        Token rightArrowToken = createToken(RIGHT_ARROW_TOKEN);

        //anydata requestMessage = check self.writeMessageQueue.consume(5);
       FieldAccessExpressionNode globalQueue= createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(SELF)), createToken(DOT_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken(WRITE_MESSAGE_QUEUE)));
        CheckExpressionNode checkExpressionNode=createCheckExpressionNode(null,createToken(CHECK_KEYWORD),
                createMethodCallExpressionNode(globalQueue,dotToken,
                        createSimpleNameReferenceNode(createIdentifierToken(CONSUME)),openParanToken,createSeparatedNodeList(
                                createPositionalArgumentNode(createRequiredExpressionNode(createIdentifierToken(DEFAULT_TIME_OUT)))
                        ),closeParanToken));
        VariableDeclarationNode queueData= createVariableDeclarationNode(createEmptyNodeList(),null,createTypedBindingPatternNode(
                createBuiltinSimpleNameReferenceNode(null,createToken(ANYDATA_KEYWORD)),
                createFieldBindingPatternVarnameNode(createSimpleNameReferenceNode(createIdentifierToken(REQUEST_MESSAGE)))),equalToken,checkExpressionNode,semicolonToken);


        //check self.clientEp->writeMessage(requestMessage);
        FieldAccessExpressionNode clientEp= createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(SELF)), createToken(DOT_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken(CLIENT_EP)));
        CheckExpressionNode checkWriteMessage=createCheckExpressionNode(ACTION_STATEMENT,createToken(CHECK_KEYWORD),
                createMethodCallExpressionNode(clientEp,rightArrowToken,
                        createSimpleNameReferenceNode(createIdentifierToken(WRITE_MESSAGE)),openParanToken,createSeparatedNodeList(
                                createPositionalArgumentNode(createRequiredExpressionNode(createIdentifierToken(REQUEST_MESSAGE)))
                        ),closeParanToken));
        ExpressionStatementNode writeMessage=createExpressionStatementNode(null,checkWriteMessage,createToken(SEMICOLON_TOKEN));



        //runtime:sleep(0.01);
        QualifiedNameReferenceNode qualifiedNameReferenceNode=createQualifiedNameReferenceNode(createIdentifierToken(
                "runtime"),createToken(COLON_TOKEN),createIdentifierToken("sleep"));
        FunctionCallExpressionNode sleep=createFunctionCallExpressionNode(qualifiedNameReferenceNode,openParanToken,
                createSeparatedNodeList(createPositionalArgumentNode(
                        createRequiredExpressionNode(createIdentifierToken("0.01")))),closeParanToken);
        ExpressionStatementNode runtimeSleep=createExpressionStatementNode(null,
                sleep,createToken(SEMICOLON_TOKEN));
//        //ResponseMessage responseMessage = check self.clientEp->readMessage();
//        CheckExpressionNode responseMessageCheckExpressionNode=createCheckExpressionNode(null,createToken(CHECK_KEYWORD),
//                createMethodCallExpressionNode(clientEp,rightArrowToken,
//                        createSimpleNameReferenceNode(createIdentifierToken(READ_MESSAGE)),openParanToken,createSeparatedNodeList(new ArrayList<>()),closeParanToken));
//        VariableDeclarationNode responseMessage= createVariableDeclarationNode(createEmptyNodeList(),null,createTypedBindingPatternNode(
//                createBuiltinSimpleNameReferenceNode(null,createIdentifierToken(RESPONSE_MESSAGE)),
//                createFieldBindingPatternVarnameNode(createSimpleNameReferenceNode(createIdentifierToken(RESPONSE_MESSAGE_VAR_NAME)))),
//                equalToken,responseMessageCheckExpressionNode,semicolonToken);
//
//
//        //check self.callRelevantPipe(responseMessage);
//        CheckExpressionNode callCheckRelavantPipeNode=createCheckExpressionNode(null,createToken(CHECK_KEYWORD),
//                createMethodCallExpressionNode( createSimpleNameReferenceNode(createIdentifierToken(SELF)),dotToken,
//                        createSimpleNameReferenceNode(createIdentifierToken("callRelevantPipe")),openParanToken,createSeparatedNodeList(
//                                createPositionalArgumentNode(createRequiredExpressionNode(createIdentifierToken("responseMessage")))
//                        ),closeParanToken));
//        ExpressionStatementNode callRelavantPipeNode =createExpressionStatementNode(null,callCheckRelavantPipeNode,createToken(SEMICOLON_TOKEN));


        whileStatements.add(queueData);
        whileStatements.add(writeMessage);
        whileStatements.add(runtimeSleep);
//        whileStatements.add(responseMessage);
//        whileStatements.add(callRelavantPipeNode);


        BlockStatementNode whileBody=createBlockStatementNode(openBraceToken,createNodeList(whileStatements),
                closeBraceToken);
        NodeList<StatementNode> workerStatements= createNodeList(createWhileStatementNode(createToken(WHILE_KEYWORD),
                createBasicLiteralNode(TRUE_KEYWORD,createToken(TRUE_KEYWORD)),whileBody,null));


       NodeList workerDeclarationNodes= createNodeList(createNamedWorkerDeclarationNode(annotations,null,createToken(WORKER_KEYWORD)
                ,createIdentifierToken(WRITE_MESSAGE),createReturnTypeDescriptorNode(
                        createToken(RETURNS_KEYWORD),createEmptyNodeList(),createToken(ERROR_KEYWORD)),
                createBlockStatementNode(openBraceToken,workerStatements,
                        closeBraceToken)));


        return createFunctionBodyBlockNode(openBraceToken,
                null, workerDeclarationNodes, closeBraceToken, null);


    }


    //TODO: Add metdata for the function
    private FunctionSignatureNode getStartMessageWritingFunctionSignatureNode(ArrayList initMetaDataNode)
            throws BallerinaAsyncApiException {

//        serverURL = getServerURL((AsyncApi25ServersImpl) asyncAPI.getServers());
        SeparatedNodeList<ParameterNode> parameterList = createSeparatedNodeList(new ArrayList<>());
//
//        OptionalTypeDescriptorNode returnType = createOptionalTypeDescriptorNode(createToken(ERROR_KEYWORD),
//                createToken(QUESTION_MARK_TOKEN));
//        ReturnTypeDescriptorNode returnTypeDescriptorNode = createReturnTypeDescriptorNode(
//                createToken(RETURNS_KEYWORD), createEmptyNodeList(), returnType);
        return createFunctionSignatureNode(
                createToken(OPEN_PAREN_TOKEN), parameterList, createToken(CLOSE_PAREN_TOKEN),null);
    }

    /**
     * Generate metadata node of the class including documentation. Content of the documentation
     * will be taken from the `description` section inside the `info` section in AsyncAPI definition.
     *
     * @return {@link MetadataNode}    Metadata node of the client class
     */
    private MetadataNode getClassMetadataNode() {

        List<AnnotationNode> classLevelAnnotationNodes = new ArrayList<>();
//        if (((AsyncApi25InfoImpl)asyncAPI.getInfo()).getExtensions() != null) {
////            Map<String, JsonNode> extensions =((AsyncApi25InfoImpl)asyncAPI.getInfo()).getExtensions();
//            DocCommentsGenerator.extractDisplayAnnotation(extensions, classLevelAnnotationNodes);
//        }
        // Generate api doc
        List<Node> documentationLines = new ArrayList<>();
        if (asyncAPI.getInfo().getDescription() != null && !asyncAPI.getInfo().getDescription().isBlank()) {
            documentationLines.addAll(DocCommentsGenerator.createAPIDescriptionDoc(
                    asyncAPI.getInfo().getDescription(), false));
        }
        MarkdownDocumentationNode apiDoc = createMarkdownDocumentationNode(createNodeList(documentationLines));
        return createMetadataNode(apiDoc, createNodeList(classLevelAnnotationNodes));
    }

    /**
     * Create client class init function
     * -- Scenario 1: init function when authentication mechanism is API key
     * <pre>
     *   public isolated function init(ApiKeysConfig apiKeyConfig, string serviceUrl,
     *     ConnectionConfig config =  {}) returns error? {
     *         http:Client httpEp = check new (serviceUrl, clientConfig);
     *         self.clientEp = httpEp;
     *         self.apiKeys = apiKeyConfig.apiKeys.cloneReadOnly();
     *   }
     * </pre>
     * -- Scenario 2: init function when authentication mechanism is OAuth2.0
     * <pre>
     *   public isolated function init(ConnectionConfig clientConfig, string serviceUrl = "base-url") returns error? {
     *         http:Client httpEp = check new (serviceUrl, clientConfig);
     *         self.clientEp = httpEp;
     *   }
     * </pre>
     * -- Scenario 3: init function when no authentication mechanism is provided
     * <pre>
     *   public isolated function init(ConnectionConfig config =  {},
     *      string serviceUrl = "base-url") returns error? {
     *         http:Client httpEp = check new (serviceUrl, clientConfig);
     *         self.clientEp = httpEp;
     *   }
     * </pre>
     *
     * @return {@link FunctionDefinitionNode}   Class init function
     * @throws BallerinaAsyncApiException When invalid server URL is provided
     */
    private FunctionDefinitionNode createInitFunction() throws BallerinaAsyncApiException {
        ArrayList initMetaDataDoc = new ArrayList();
        FunctionSignatureNode functionSignatureNode = getInitFunctionSignatureNode(initMetaDataDoc);
        FunctionBodyNode functionBodyNode = getInitFunctionBodyNode();
//        NodeList<Token> qualifierList = createNodeList(createToken(PUBLIC_KEYWORD), createToken(ISOLATED_KEYWORD));
        NodeList<Token> qualifierList = createNodeList(createToken(PUBLIC_KEYWORD));
        IdentifierToken functionName = createIdentifierToken("init");
        return createFunctionDefinitionNode(null, getInitDocComment(initMetaDataDoc), qualifierList,
                createToken(FUNCTION_KEYWORD),
                functionName, createEmptyNodeList(), functionSignatureNode, functionBodyNode);
    }

    /**
     * Create function body node of client init function.
     *
     * @return {@link FunctionBodyNode}
     */
    private FunctionBodyNode getInitFunctionBodyNode() {

        List<StatementNode> assignmentNodes = new ArrayList<>();

//        assignmentNodes.add(ballerinaAuthConfigGenerator.getWebsocketClientConfigVariableNode());
//        assignmentNodes.add(ballerinaAuthConfigGenerator.getClientConfigDoStatementNode());

        // If both apiKey and httpOrOAuth is supported
        // todo : After revamping
        if (ballerinaAuthConfigGenerator.isHttpApiKey() && ballerinaAuthConfigGenerator.isHttpOROAuth()) {
            assignmentNodes.add(ballerinaAuthConfigGenerator.handleInitForMixOfApiKeyAndHTTPOrOAuth());
        }
        // create initialization statement of websocket:Client class instance
        assignmentNodes.add(ballerinaAuthConfigGenerator.getClientInitializationNode());


        // create {@code self.clientEp = httpEp;} assignment node
        FieldAccessExpressionNode selfClientEp = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(SELF)), createToken(DOT_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken("clientEp")));
        SimpleNameReferenceNode selfClientEpValue = createSimpleNameReferenceNode(createIdentifierToken("websocketEp"));
        AssignmentStatementNode selfWebsocketClientAssignmentStatementNode = createAssignmentStatementNode(selfClientEp,
                createToken(EQUAL_TOKEN),selfClientEpValue, createToken(SEMICOLON_TOKEN));
        assignmentNodes.add(selfWebsocketClientAssignmentStatementNode);

        // create {@code self.pipes ={};} assignment node
        FieldAccessExpressionNode selfPipes = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(SELF)), createToken(DOT_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken("pipes")));
        SimpleNameReferenceNode selfPipesValue = createSimpleNameReferenceNode(createIdentifierToken("{}"));
        AssignmentStatementNode selfPipesAssignmentStatementNode = createAssignmentStatementNode(selfPipes,
                createToken(EQUAL_TOKEN),selfPipesValue, createToken(SEMICOLON_TOKEN));
        assignmentNodes.add(selfPipesAssignmentStatementNode);


        // create {@code self.writeMessageQueue = new (1000);} assignment node
        List<Node> argumentsList = new ArrayList<>();
        FieldAccessExpressionNode selfWriteMessageQueues = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(SELF)), createToken(DOT_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken(WRITE_MESSAGE_QUEUE)));
        argumentsList.add(createIdentifierToken("1000"));
        SeparatedNodeList<FunctionArgumentNode> arguments = createSeparatedNodeList(argumentsList);
        Token closeParenArg = createToken(CLOSE_PAREN_TOKEN);
        Token openParenArg=createToken(OPEN_PAREN_TOKEN);
        ParenthesizedArgList parenthesizedArgList = createParenthesizedArgList(openParenArg,
                arguments,
                closeParenArg);
        ImplicitNewExpressionNode expressionNode = createImplicitNewExpressionNode(createToken(NEW_KEYWORD),
                parenthesizedArgList);

        AssignmentStatementNode selfWriteQueueAssignmentStatementNode = createAssignmentStatementNode(selfWriteMessageQueues,
                createToken(EQUAL_TOKEN),expressionNode, createToken(SEMICOLON_TOKEN));
        assignmentNodes.add(selfWriteQueueAssignmentStatementNode);


        // create {@code self.readMessageQueue = new (1000);} assignment node
        FieldAccessExpressionNode selfReadMessageQueues = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(SELF)), createToken(DOT_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken(READ_MESSAGE_QUEUE)));
        AssignmentStatementNode selfReadQueuesAssignmentStatementNode = createAssignmentStatementNode(selfReadMessageQueues,
                createToken(EQUAL_TOKEN),expressionNode, createToken(SEMICOLON_TOKEN));
        assignmentNodes.add(selfReadQueuesAssignmentStatementNode);



        // create {@code self.startMessageWriting()} assignment node
        List workersArgumentsList=new ArrayList<>();
        SeparatedNodeList<FunctionArgumentNode> workersArguments =
                createSeparatedNodeList(workersArgumentsList);
        ExpressionStatementNode startMessageWriting=createExpressionStatementNode(FUNCTION_CALL,
                createMethodCallExpressionNode( createSimpleNameReferenceNode(createIdentifierToken(SELF)),
                        createToken(DOT_TOKEN),createSimpleNameReferenceNode(
                                createIdentifierToken(START_MESSAGE_WRITING)),openParenArg,
                workersArguments,closeParenArg),createToken(SEMICOLON_TOKEN));

        // create {@code self.startMessageReading()} assignment node

        ExpressionStatementNode startMessageReading=createExpressionStatementNode(FUNCTION_CALL,
                createMethodCallExpressionNode( createSimpleNameReferenceNode(createIdentifierToken(SELF)),
                        createToken(DOT_TOKEN),createSimpleNameReferenceNode(
                                createIdentifierToken(START_MESSAGE_READING)),openParenArg,
                        workersArguments,closeParenArg),createToken(SEMICOLON_TOKEN));

        // create {@code self.startPipeTriggering()} assignment node

        ExpressionStatementNode startPipeTriggering=createExpressionStatementNode(FUNCTION_CALL,
                createMethodCallExpressionNode( createSimpleNameReferenceNode(createIdentifierToken(SELF)),
                        createToken(DOT_TOKEN),createSimpleNameReferenceNode(
                                createIdentifierToken(START_PIPE_TRIGGERING)),openParenArg,
                      workersArguments,closeParenArg),createToken(SEMICOLON_TOKEN));


        assignmentNodes.add(startMessageWriting);
        assignmentNodes.add(startMessageReading);
        assignmentNodes.add(startPipeTriggering);





        // Get API key assignment node if authentication mechanism type is only `apiKey`
        if (ballerinaAuthConfigGenerator.isHttpApiKey() && !ballerinaAuthConfigGenerator.isHttpOROAuth()) {
            assignmentNodes.add(ballerinaAuthConfigGenerator.getApiKeyAssignmentNode());
        }
        if (ballerinaAuthConfigGenerator.isHttpApiKey()) {
            List<String> apiKeyNames = new ArrayList<>();
            apiKeyNames.addAll(ballerinaAuthConfigGenerator.getHeaderApiKeyNameList().values());
            apiKeyNames.addAll(ballerinaAuthConfigGenerator.getQueryApiKeyNameList().values());
            setApiKeyNameList(apiKeyNames);
        }
        ReturnStatementNode returnStatementNode = createReturnStatementNode(createToken(
                RETURN_KEYWORD), null, createToken(SEMICOLON_TOKEN));
        assignmentNodes.add(returnStatementNode);
        NodeList<StatementNode> statementList = createNodeList(assignmentNodes);
        return createFunctionBodyBlockNode(createToken(OPEN_BRACE_TOKEN),
                null, statementList, createToken(CLOSE_BRACE_TOKEN), null);
    }

    /**
     * Create function signature node of client init function.
     *
     * @return {@link FunctionSignatureNode}
     * @throws BallerinaAsyncApiException When invalid server URL is provided
     */
    private FunctionSignatureNode getInitFunctionSignatureNode(ArrayList initMetaDataNode)
            throws BallerinaAsyncApiException {
        //string serviceUrl = "ws://localhost:9090/payloadV"
        serverURL = getServerURL((AsyncApi25ServersImpl) asyncAPI.getServers());
        SeparatedNodeList<ParameterNode> parameterList = createSeparatedNodeList(
                ballerinaAuthConfigGenerator.getConfigParamForClassInit(serverURL, initMetaDataNode));

        //error?
        OptionalTypeDescriptorNode returnType = createOptionalTypeDescriptorNode(createToken(ERROR_KEYWORD),
                createToken(QUESTION_MARK_TOKEN));
        //returns
        ReturnTypeDescriptorNode returnTypeDescriptorNode = createReturnTypeDescriptorNode(
                createToken(RETURNS_KEYWORD), createEmptyNodeList(), returnType);
        return createFunctionSignatureNode(
                createToken(OPEN_PAREN_TOKEN), parameterList, createToken(CLOSE_PAREN_TOKEN), returnTypeDescriptorNode);
    }

    /**
     * Provide client class init function's documentation including function description and parameter descriptions.
     *
     * @return {@link MetadataNode}    Metadata node containing entire function documentation comment.
     */
    private MetadataNode getInitDocComment(ArrayList docs) {

//        List<Node> docs = new ArrayList<>();
        String clientInitDocComment = "Gets invoked to initialize the `connector`.\n";
        Map<String, JsonNode> extensions = ((AsyncApi25InfoImpl) asyncAPI.getInfo()).getExtensions();
        if (extensions != null && !extensions.isEmpty()) {
            for (Map.Entry<String, JsonNode> extension : extensions.entrySet()) {
                if (extension.getKey().trim().equals(X_BALLERINA_INIT_DESCRIPTION)) {
                    clientInitDocComment = clientInitDocComment.concat(extension.getValue().toString());
                    break;
                }
            }
        }
        //todo: setInitDocComment() pass the references
        docs.addAll(DocCommentsGenerator.createAPIDescriptionDoc(clientInitDocComment, true));
        if (ballerinaAuthConfigGenerator.isHttpApiKey() && !ballerinaAuthConfigGenerator.isHttpOROAuth()) {
            MarkdownParameterDocumentationLineNode apiKeyConfig = DocCommentsGenerator.createAPIParamDoc(
                    "apiKeyConfig", DEFAULT_API_KEY_DESC);
            docs.add(apiKeyConfig);
        }
        // Create method description
        MarkdownParameterDocumentationLineNode clientConfig = DocCommentsGenerator.createAPIParamDoc("config",
                "The configurations to be used when initializing the `connector`");
        docs.add(clientConfig);
        MarkdownParameterDocumentationLineNode serviceUrlAPI = DocCommentsGenerator.createAPIParamDoc("serviceUrl",
                "URL of the target service");
        docs.add(serviceUrlAPI);
        MarkdownParameterDocumentationLineNode returnDoc = DocCommentsGenerator.createAPIParamDoc("return",
                "An error if connector initialization failed");
        docs.add(returnDoc);
        MarkdownDocumentationNode clientInitDoc = createMarkdownDocumentationNode(createNodeList(docs));
        return createMetadataNode(clientInitDoc, createEmptyNodeList());
    }

    /**
     * Generate client class instance variables.
     *
     * @return {@link List<ObjectFieldNode>}    List of instance variables
     */
    private List<ObjectFieldNode> createClassInstanceVariables() {


        List<ObjectFieldNode> fieldNodeList = new ArrayList<>();
        Token privateKeywordToken = createToken(PRIVATE_KEYWORD);
        Token finalKeywordToken = createToken(FINAL_KEYWORD);
        ArrayList<Token> prefixTokens=new ArrayList<>();
        prefixTokens.add(privateKeywordToken);
        prefixTokens.add(finalKeywordToken);
        NodeList<Token> qualifierList = createNodeList(prefixTokens);

        //private final websocket:Client clientEp;
        QualifiedNameReferenceNode typeName = createQualifiedNameReferenceNode(createIdentifierToken(WEBSOCKET),
                createToken(COLON_TOKEN), createIdentifierToken(GeneratorConstants.CLIENT_CLASS));
        IdentifierToken fieldName = createIdentifierToken(GeneratorConstants.CLIENT_EP);
        MetadataNode metadataNode = createMetadataNode(null, createEmptyNodeList());
        ObjectFieldNode websocketClientField = createObjectFieldNode(metadataNode, null,
                qualifierList, typeName, fieldName, null, null, createToken(SEMICOLON_TOKEN));

        //private final pipe:Pipe writeMessageQueue;
        QualifiedNameReferenceNode pipeTypeName = createQualifiedNameReferenceNode(createIdentifierToken(SIMPLE_PIPE),
                createToken(COLON_TOKEN), createIdentifierToken(GeneratorConstants.CAPITAL_PIPE));
        IdentifierToken writeMessageQueueFieldName = createIdentifierToken(GeneratorConstants.WRITE_MESSAGE_QUEUE);
        MetadataNode writeMessageQueuemetadataNode = createMetadataNode(null, createEmptyNodeList());
        ObjectFieldNode writeMessageQueueClientField = createObjectFieldNode(writeMessageQueuemetadataNode, null,
                qualifierList, pipeTypeName, writeMessageQueueFieldName, null, null, createToken(SEMICOLON_TOKEN));

        //private final pipe:Pipe readMessageQueue;
        IdentifierToken readMessageQueueFieldName = createIdentifierToken(GeneratorConstants.READ_MESSAGE_QUEUE);
        MetadataNode readMessageQueuemetadataNode = createMetadataNode(null, createEmptyNodeList());
        ObjectFieldNode readMessageQueueClientField = createObjectFieldNode(readMessageQueuemetadataNode, null,
                qualifierList, pipeTypeName, readMessageQueueFieldName, null, null, createToken(SEMICOLON_TOKEN));


        //private final map<pipe:Pipe> pipes;
        TypeParameterNode pipesTypeParamsNode = createTypeParameterNode(createToken(LT_TOKEN),
                pipeTypeName, createToken(GT_TOKEN));
        MapTypeDescriptorNode pipesTypeName = createMapTypeDescriptorNode(createToken(MAP_KEYWORD),
                pipesTypeParamsNode);
//        MetadataNode customHeadersMetadata = getMetadataNode("Custom headers, " +
//                "which should be sent to the server");
        IdentifierToken pipesFieldName = createIdentifierToken(GeneratorConstants.PIPES);
        MetadataNode pipesMetadataNode = createMetadataNode(null, createEmptyNodeList());
        ObjectFieldNode pipesField = createObjectFieldNode(pipesMetadataNode, null,
                qualifierList, pipesTypeName, pipesFieldName, null, null, createToken(SEMICOLON_TOKEN));




        fieldNodeList.add(websocketClientField);
        fieldNodeList.add(writeMessageQueueClientField);
        fieldNodeList.add(readMessageQueueClientField);
        fieldNodeList.add(pipesField);



        // add apiKey instance variable when API key security schema is given
        ObjectFieldNode apiKeyFieldNode = ballerinaAuthConfigGenerator.getApiKeyMapClassVariable();
        if (apiKeyFieldNode != null) {
            fieldNodeList.add(apiKeyFieldNode);
        }
        return fieldNodeList;
    }

    /**
     * Generate remote functions for OpenAPI operations.
     * <p>
     * //     * @param paths  openAPI Paths
     * //     * @param filter user given tags and operations
     *
     * @return FunctionDefinitionNodes list
     * @throws BallerinaAsyncApiException - throws when creating remote functions fails
     */
    private List<FunctionDefinitionNode> createRemoteFunctions(Map<String, AsyncApiMessage> messages,List<MatchClauseNode> matchStatementList)
            throws BallerinaAsyncApiException {

        Token openParenToken=createToken(OPEN_PAREN_TOKEN);
        Token openBraceToken=createToken(OPEN_BRACE_TOKEN);
        Token closeBraceToken=createToken(CLOSE_BRACE_TOKEN);
        Token closeParenToken = createToken(CLOSE_PAREN_TOKEN);
        Token rightDoubleArrow =createToken(RIGHT_DOUBLE_ARROW_TOKEN);
        String dispatcherStreamId="id";

//        List<String> filterTags = filter.getTags();
//        List<String> filterOperations = filter.getOperations();
        List<FunctionDefinitionNode> functionDefinitionNodeList = new ArrayList<>();
//        Set<Map.Entry<String, PathItem>> pathsItems = paths.entrySet();
        Set<Map.Entry<String, AsyncApiMessage>> messageItems = messages.entrySet();
        LinkedHashMap<String,HashMap<String,String>> responseMap = new LinkedHashMap<>();
        int count=1;
        for (Map.Entry<String, AsyncApiMessage> messageItem : messageItems) {
            Map<String, JsonNode> extensions = ((AsyncApi25MessageImpl) messageItem.getValue()).getExtensions();
            if (extensions != null && extensions.get(X_RESPONSE) != null) {
//                JsonNode ref=extensions.get(X_RESPONSE).get(PAYLOAD);
                FunctionDefinitionNode functionDefinitionNode =
                        getClientMethodFunctionDefinitionNode(messageItem, extensions,Integer.toString(count),responseMap);
                functionDefinitionNodeList.add(functionDefinitionNode);
                count+=1;
            }
        }


        for(Map.Entry<String, HashMap<String,String>> key:responseMap.entrySet()){
            HashMap<String,String>requests= key.getValue();
            String response=key.getKey();

            if(requests.size()>1){
                List<MatchClauseNode> internalMatchStatementList=new ArrayList<>();

                for(String requestType:requests.keySet()){
                    createMatchStatements(internalMatchStatementList,response,requests.get(requestType),requestType);


                }

                MatchStatementNode matchStatementNode=createMatchStatementNode(createToken(MATCH_KEYWORD),
                        createBracedExpressionNode(null,openParenToken,
                                createSimpleNameReferenceNode(createIdentifierToken(dispatcherStreamId)),
                                closeParenToken),openBraceToken,createNodeList(internalMatchStatementList),
                        closeBraceToken,null);
                NodeList<StatementNode> statementNodes=createNodeList(matchStatementNode);
                MatchClauseNode matchClauseNode=createMatchClauseNode(createSeparatedNodeList(
                                createIdentifierToken("\""+response+"\"")),null,rightDoubleArrow,
                        createBlockStatementNode(openBraceToken,statementNodes,closeBraceToken));
                matchStatementList.add(matchClauseNode);

            }else{
                Map.Entry<String, String> entry = requests.entrySet().iterator().next();
                createMatchStatements(matchStatementList,response,null,entry.getKey());

            }





        }

//        for (Map.Entry<String, PathItem> path : pathsItems) {
//            if (!path.getValue().readOperationsMap().isEmpty()) {
//                for (Map.Entry<PathItem.HttpMethod, Operation> operation :
//                        path.getValue().readOperationsMap().entrySet()) {
//                    // create display annotation of the operation
//                    List<AnnotationNode> functionLevelAnnotationNodes = new ArrayList<>();
//                    if (operation.getValue().getExtensions() != null) {
//                        Map<String, Object> extensions = operation.getValue().getExtensions();
//                        DocCommentsGenerator.extractDisplayAnnotation(extensions, functionLevelAnnotationNodes);
//                    }
//                    List<String> operationTags = operation.getValue().getTags();
//                    String operationId = operation.getValue().getOperationId();
//                    if (!filterTags.isEmpty() || !filterOperations.isEmpty()) {
//                        // Generate remote function only if it is available in tag filter or operation filter or both
//                        if (operationTags != null || ((!filterOperations.isEmpty()) && (operationId != null))) {
//                            if (GeneratorUtils.hasTags(operationTags, filterTags) ||
//                                    ((operationId != null) && filterOperations.contains(operationId.trim()))) {
//                                // Generate remote function
//                                FunctionDefinitionNode functionDefinitionNode =
//                                        getClientMethodFunctionDefinitionNode(
//                                                functionLevelAnnotationNodes, path.getKey(), operation);
//                                functionDefinitionNodeList.add(functionDefinitionNode);
//                            }
//                        }
//                    } else {
//                        // Generate remote function
//                        FunctionDefinitionNode functionDefinitionNode = getClientMethodFunctionDefinitionNode(
//                                functionLevelAnnotationNodes, path.getKey(), operation);
//                        functionDefinitionNodeList.add(functionDefinitionNode);
//                    }
////                }
////            }
////        }
        return functionDefinitionNodeList;
    }


    public void createMatchStatements(List<MatchClauseNode> matchClauseNodes,String response,String count,String request){
        Token equalToken = createToken(EQUAL_TOKEN);
        Token semicolonToken = createToken(SEMICOLON_TOKEN);
        Token openBracketToken= createToken(OPEN_BRACKET_TOKEN);
        Token closeBracketToken=createToken(CLOSE_BRACKET_TOKEN);
        Token dotToken =createToken(DOT_TOKEN);
        Token closeParenToken = createToken(CLOSE_PAREN_TOKEN);
        Token openParenToken=createToken(OPEN_PAREN_TOKEN);
        Token openBraceToken=createToken(OPEN_BRACE_TOKEN);
        Token closeBraceToken=createToken(CLOSE_BRACE_TOKEN);
        Token rightDoubleArrow =createToken(RIGHT_DOUBLE_ARROW_TOKEN);



        String responseType=response;
        String requestType=request;
        String dispatcherStreamId="id";


        //requestType substring
        char requestTypeFirstChar = Character.toLowerCase(requestType.charAt(0)); // Lowercase the first character
        String requestRemainingString = requestType.substring(1);
        String requestTypeCamelCaseName=requestTypeFirstChar+requestRemainingString;
        String requestTypePipe=requestTypeCamelCaseName+"Pipe";
        String responseTypeCamelCaseName=null;
        if(responseType.contains(PIPE)){
            responseTypeCamelCaseName = "unionResult";

        }else {
            char responseTypeFirstChar = Character.toLowerCase(responseType.charAt(0)); // Lowercase the first character
            String responseRemainingString = responseType.substring(1);
            responseTypeCamelCaseName = responseTypeFirstChar + responseRemainingString;
        }


        List<StatementNode> matchStatementList =new ArrayList<>();
        SimpleNameReferenceNode requestTypePipeNode=createSimpleNameReferenceNode(createIdentifierToken(requestTypePipe));
        SimpleNameReferenceNode responseMessageNode=createSimpleNameReferenceNode(createIdentifierToken(RESPONSE_MESSAGE_VAR_NAME));
        SimpleNameReferenceNode responseTypeName =createSimpleNameReferenceNode(createIdentifierToken(responseType));
        SimpleNameReferenceNode responseNameNode=createSimpleNameReferenceNode(createIdentifierToken(responseTypeCamelCaseName));

        //matchStatements
        //pipe:Pipe tuplePipe = check self.pipes["tuplePipe"].ensureType();
        QualifiedNameReferenceNode pipeTypeName = createQualifiedNameReferenceNode(createIdentifierToken(SIMPLE_PIPE),
                createToken(COLON_TOKEN), createIdentifierToken(GeneratorConstants.CAPITAL_PIPE));
        IndexedExpressionNode selfPipes=createIndexedExpressionNode(createFieldAccessExpressionNode(
                        createSimpleNameReferenceNode(createIdentifierToken(SELF)), createToken(DOT_TOKEN),
                        createSimpleNameReferenceNode(createIdentifierToken(PIPES))),openBracketToken,
                createSeparatedNodeList(createSimpleNameReferenceNode(createIdentifierToken("\""+requestTypePipe+"\""))),
                closeBracketToken);
        MethodCallExpressionNode methodCallExpressionNode= createMethodCallExpressionNode(selfPipes,dotToken,
                createSimpleNameReferenceNode(createIdentifierToken(ENSURE_TYPE)),
                openParenToken,createSeparatedNodeList(),closeParenToken);

        CheckExpressionNode selfPipeCheck=createCheckExpressionNode(null,createToken(CHECK_KEYWORD),
                methodCallExpressionNode);
        SimpleNameReferenceNode responseTypePipeNode=createSimpleNameReferenceNode(createIdentifierToken(requestTypePipe));
        VariableDeclarationNode pipeTypeEnsureStatement= createVariableDeclarationNode(createEmptyNodeList(),null,
                createTypedBindingPatternNode(
                        pipeTypeName,
                        createFieldBindingPatternVarnameNode(responseTypePipeNode)),
                equalToken,selfPipeCheck,semicolonToken);
        matchStatementList.add(pipeTypeEnsureStatement);

        //User user = check responseMessage.cloneWithType();

        MethodCallExpressionNode cloneWithTypeMethodCallExpressionNode= createMethodCallExpressionNode(
                responseMessageNode,dotToken,
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
        matchStatementList.add(responseTypeCloneStatement);

        //check tuplePipe.produce(user, 5);
        List<Node> nodes=new ArrayList<>();
        nodes.add(responseNameNode);
        nodes.add(createToken(COMMA_TOKEN));
        nodes.add(createIdentifierToken("5"));
        MethodCallExpressionNode pipeProduceExpressionNode= createMethodCallExpressionNode(responseTypePipeNode,dotToken,
                createSimpleNameReferenceNode(createIdentifierToken("produce")),
                openParenToken,createSeparatedNodeList(nodes),closeParenToken);

        CheckExpressionNode pipeProduceCheck=createCheckExpressionNode(null,createToken(CHECK_KEYWORD),
                pipeProduceExpressionNode);
        ExpressionStatementNode pipeProduceExpression= createExpressionStatementNode(null,
                pipeProduceCheck,createToken(SEMICOLON_TOKEN));
        matchStatementList.add(pipeProduceExpression);

        NodeList<StatementNode> statementNodes=createNodeList(matchStatementList);
        if(count!=null){
            responseType=count;
        }
        MatchClauseNode matchClauseNode=createMatchClauseNode(createSeparatedNodeList(
                        createIdentifierToken("\""+responseType+"\"")),null,rightDoubleArrow,
                createBlockStatementNode(openBraceToken,statementNodes,closeBraceToken));


        matchClauseNodes.add(matchClauseNode);




    }

    //    /**
//     * Generate function definition node.
//     * <pre>
//     *     remote isolated function pathParameter(int 'version, string name) returns string|error {
//     *          string  path = string `/v1/${'version}/v2/${name}`;
//     *          string response = check self.clientEp-> get(path);
//     *          return response;
//     *    }
//     *    or
//     *     resource isolated function get v1/[string 'version]/v2/[sting name]() returns string|error {
//     *         string  path = string `/v1/${'version}/v2/${name}`;
//     *         string response = check self.clientEp-> get(path);
//     *         return response;
//     *     }
//     * </pre>
//     */
    private FunctionDefinitionNode getClientMethodFunctionDefinitionNode(Map.Entry<String, AsyncApiMessage> message,
                                                                         Map<String, JsonNode> extensions,String count,
                                                                         LinkedHashMap<String,
                                                                                 HashMap<String,String>> responseMap)
            throws BallerinaAsyncApiException {
        // Create api doc for function
        List<Node> remoteFunctionDocs = new ArrayList<>();
        AsyncApi25MessageImpl messageValue = (AsyncApi25MessageImpl) message.getValue();
        String messageName = message.getKey();

        if (messageValue.getSummary() != null) {
            remoteFunctionDocs.addAll(DocCommentsGenerator.createAPIDescriptionDoc(
                    messageValue.getSummary(), true));
        } else if (messageValue.getDescription() != null && !messageValue.getDescription().isBlank()) {
            remoteFunctionDocs.addAll(DocCommentsGenerator.createAPIDescriptionDoc(
                    messageValue.getDescription(), true));
        } else {
            MarkdownDocumentationLineNode newLine = createMarkdownDocumentationLineNode(null,
                    createToken(SyntaxKind.HASH_TOKEN), createEmptyNodeList());
            remoteFunctionDocs.add(newLine);
        }

        //Create qualifier list
        NodeList<Token> qualifierList = createNodeList(createToken(
                        REMOTE_KEYWORD),
                createToken(ISOLATED_KEYWORD));
        Token functionKeyWord = createToken(FUNCTION_KEYWORD);
        IdentifierToken functionName = createIdentifierToken(REMOTE_METHOD_NAME_PREFIX + messageName);

        remoteFunctionNameList.add(messageName);

        FunctionSignatureGenerator functionSignatureGenerator = new FunctionSignatureGenerator(asyncAPI,
                ballerinaSchemaGenerator, typeDefinitionNodeList, resourceMode);
        FunctionSignatureNode functionSignatureNode =
                functionSignatureGenerator.getFunctionSignatureNode(messageValue.getPayload(),
                        remoteFunctionDocs, extensions);
        typeDefinitionNodeList = functionSignatureGenerator.getTypeDefinitionNodeList();
//        // Create `Deprecated` annotation if an operation has mentioned as `deprecated:true`
//        if (operation.getValue().getDeprecated() != null && operation.getValue().getDeprecated()) {
//            DocCommentsGenerator.extractDeprecatedAnnotation(operation.getValue().getExtensions(),
//                    remoteFunctionDocs, annotationNodes);
//        }
        // Create metadataNode add documentation string

        List<AnnotationNode> annotationNodes = new ArrayList<>();
        MetadataNode metadataNode = createMetadataNode(createMarkdownDocumentationNode(
                createNodeList(remoteFunctionDocs)), createNodeList(annotationNodes));

        // Create Function Body
        FunctionBodyGenerator functionBodyGenerator = new FunctionBodyGenerator(imports, typeDefinitionNodeList,
                asyncAPI, ballerinaSchemaGenerator, ballerinaAuthConfigGenerator, ballerinaUtilGenerator, resourceMode);
        FunctionBodyNode functionBodyNode = functionBodyGenerator.getFunctionBodyNode(extensions,
                ((RequiredParameterNode) functionSignatureNode.parameters().get(0)).paramName().get().toString(),count,responseMap);
        imports = functionBodyGenerator.getImports();

        //Generate relative path
//        NodeList<Node> relativeResourcePath = resourceMode ?
//                createNodeList(GeneratorUtils.getRelativeResourcePath(path, operation.getValue(), null)) :
//                createEmptyNodeList();
        NodeList<Node> relativeResourcePath =
                createEmptyNodeList();
        return createFunctionDefinitionNode(null,
                metadataNode, qualifierList, functionKeyWord, functionName, relativeResourcePath,
                functionSignatureNode, functionBodyNode);
    }

    /**
     * Generate serverUrl for client default value.
     */
    private String getServerURL(AsyncApi25ServersImpl servers) throws BallerinaAsyncApiException {


        String serverURL;
        List<AsyncApiServer> serversList = servers.getItems();
        AsyncApi25ServerImpl selectedServer = (AsyncApi25ServerImpl) serversList.get(0);
        if (!selectedServer.getUrl().startsWith("https:") && servers.getItems().size() > 1) {
            for (AsyncApiServer server : serversList) {
                if (server.getUrl().startsWith("https:")) {
                    selectedServer = (AsyncApi25ServerImpl) server;
                    break;
                }
            }
        }
        if (selectedServer.getUrl() == null) {
            serverURL = "http://localhost:9090/v1";
        } else if (selectedServer.getVariables() != null) {
            Map<String, ServerVariable> variables = selectedServer.getVariables();
            URL url;
            String resolvedUrl = GeneratorUtils.buildUrl(selectedServer.getUrl(), variables);
            //                url = new URL(resolvedUrl);
//                serverURL = url.toString();
            serverURL = resolvedUrl;
        } else {
            serverURL = selectedServer.getUrl();
        }
        return serverURL;
    }

    /**
     * Return auth type to generate test file.
     *
     * @return {@link Set<String>}
     */
    public Set<String> getAuthType() {
        return ballerinaAuthConfigGenerator.getAuthType();
    }

    /**
     * Provide list of the field names in ApiKeysConfig record to generate the Config.toml file.
     *
     * @return {@link List<String>}
     */
    public List<String> getApiKeyNameList() {
        return apiKeyNameList;
    }

    /**
     * Set the `apiKeyNameList` by adding the Api Key names available under security schemas.
     *
     * @param apiKeyNameList {@link List<String>}
     */
    public void setApiKeyNameList(List<String> apiKeyNameList) {
        this.apiKeyNameList = apiKeyNameList;
    }
}
