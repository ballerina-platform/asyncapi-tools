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
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.apicurio.datamodels.models.Schema;
import io.apicurio.datamodels.models.ServerVariable;
import io.apicurio.datamodels.models.asyncapi.AsyncApiChannelItem;
import io.apicurio.datamodels.models.asyncapi.AsyncApiMessage;
import io.apicurio.datamodels.models.asyncapi.AsyncApiServer;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25DocumentImpl;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25InfoImpl;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25MessageImpl;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25SchemaImpl;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25ServerImpl;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25ServersImpl;
import io.ballerina.asyncapi.core.GeneratorConstants;
import io.ballerina.asyncapi.core.GeneratorUtils;
import io.ballerina.asyncapi.core.exception.BallerinaAsyncApiException;
import io.ballerina.asyncapi.core.generators.asyncspec.model.BalAsyncApi25SchemaImpl;
import io.ballerina.asyncapi.core.generators.client.model.AASClientConfig;
import io.ballerina.asyncapi.core.generators.document.DocCommentsGenerator;
import io.ballerina.asyncapi.core.generators.schema.BallerinaTypesGenerator;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.AssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.BinaryExpressionNode;
import io.ballerina.compiler.syntax.tree.BlockStatementNode;
import io.ballerina.compiler.syntax.tree.BuiltinSimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.CaptureBindingPatternNode;
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
import io.ballerina.compiler.syntax.tree.IfElseStatementNode;
import io.ballerina.compiler.syntax.tree.ImplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
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
import io.ballerina.compiler.syntax.tree.ReturnStatementNode;
import io.ballerina.compiler.syntax.tree.ReturnTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.TemplateExpressionNode;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.compiler.syntax.tree.TypeParameterNode;
import io.ballerina.compiler.syntax.tree.TypedBindingPatternNode;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocuments;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.ballerina.asyncapi.core.GeneratorConstants.CLIENT_CLASS_NAME;
import static io.ballerina.asyncapi.core.GeneratorConstants.CLIENT_EP;
import static io.ballerina.asyncapi.core.GeneratorConstants.CONSUME;
import static io.ballerina.asyncapi.core.GeneratorConstants.CUSTOM_HEADERS;
import static io.ballerina.asyncapi.core.GeneratorConstants.DEFAULT_API_KEY_DESC;
import static io.ballerina.asyncapi.core.GeneratorConstants.DEFAULT_TIME_OUT;
import static io.ballerina.asyncapi.core.GeneratorConstants.HEADER_PARAM;
import static io.ballerina.asyncapi.core.GeneratorConstants.LANG_RUNTIME;
import static io.ballerina.asyncapi.core.GeneratorConstants.MODIFIED_URL;
import static io.ballerina.asyncapi.core.GeneratorConstants.NUVINDU_PIPE;
import static io.ballerina.asyncapi.core.GeneratorConstants.OBJECT;
import static io.ballerina.asyncapi.core.GeneratorConstants.PIPES;
import static io.ballerina.asyncapi.core.GeneratorConstants.PIPE_TRIGGER;
import static io.ballerina.asyncapi.core.GeneratorConstants.PRODUCE;
import static io.ballerina.asyncapi.core.GeneratorConstants.QUERY_PARAM;
import static io.ballerina.asyncapi.core.GeneratorConstants.READ_MESSAGE;
import static io.ballerina.asyncapi.core.GeneratorConstants.READ_MESSAGE_QUEUE;
import static io.ballerina.asyncapi.core.GeneratorConstants.REMOTE_METHOD_NAME_PREFIX;
import static io.ballerina.asyncapi.core.GeneratorConstants.REQUEST_MESSAGE;
import static io.ballerina.asyncapi.core.GeneratorConstants.RESPONSE_MESSAGE;
import static io.ballerina.asyncapi.core.GeneratorConstants.RESPONSE_MESSAGE_VAR_NAME;
import static io.ballerina.asyncapi.core.GeneratorConstants.RESPONSE_MESSAGE_WITH_ID;
import static io.ballerina.asyncapi.core.GeneratorConstants.RESPONSE_MESSAGE_WITH_ID_VAR_NAME;
import static io.ballerina.asyncapi.core.GeneratorConstants.RUNTIME;
import static io.ballerina.asyncapi.core.GeneratorConstants.SELF;
import static io.ballerina.asyncapi.core.GeneratorConstants.SERVICE_URL;
import static io.ballerina.asyncapi.core.GeneratorConstants.SIMPLE_PIPE;
import static io.ballerina.asyncapi.core.GeneratorConstants.SIMPLE_RPC;
import static io.ballerina.asyncapi.core.GeneratorConstants.SLEEP;
import static io.ballerina.asyncapi.core.GeneratorConstants.START_MESSAGE_READING;
import static io.ballerina.asyncapi.core.GeneratorConstants.START_MESSAGE_WRITING;
import static io.ballerina.asyncapi.core.GeneratorConstants.START_PIPE_TRIGGERING;
import static io.ballerina.asyncapi.core.GeneratorConstants.STRING;
import static io.ballerina.asyncapi.core.GeneratorConstants.UUID;
import static io.ballerina.asyncapi.core.GeneratorConstants.WEBSOCKET;
import static io.ballerina.asyncapi.core.GeneratorConstants.WORKER_SLEEP_TIME_OUT;
import static io.ballerina.asyncapi.core.GeneratorConstants.WRITE_MESSAGE;
import static io.ballerina.asyncapi.core.GeneratorConstants.WRITE_MESSAGE_QUEUE;
import static io.ballerina.asyncapi.core.GeneratorConstants.X_BALLERINA_INIT_DESCRIPTION;
import static io.ballerina.asyncapi.core.GeneratorConstants.X_BALLERINA_MESSAGE_READ_DESCRIPTION;
import static io.ballerina.asyncapi.core.GeneratorConstants.X_BALLERINA_MESSAGE_WRITE_DESCRIPTION;
import static io.ballerina.asyncapi.core.GeneratorConstants.X_BALLERINA_PIPE_TRIGGER_DESCRIPTION;
import static io.ballerina.asyncapi.core.GeneratorConstants.X_DISPATCHER_KEY;
import static io.ballerina.asyncapi.core.GeneratorConstants.X_DISPATCHER_STREAM_ID;
import static io.ballerina.asyncapi.core.GeneratorConstants.X_RESPONSE;
import static io.ballerina.asyncapi.core.GeneratorConstants.X_RESPONSE_TYPE;
import static io.ballerina.asyncapi.core.GeneratorUtils.getValidName;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyMinutiaeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createLiteralValueToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createSeparatedNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createAssignmentStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBasicLiteralNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBinaryExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBlockStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBracedExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBuiltinSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createCaptureBindingPatternNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createCheckExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createClassDefinitionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createElseBlockNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createExpressionStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFieldAccessExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFieldBindingPatternVarnameNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionBodyBlockNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionCallExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionDefinitionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionSignatureNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createIfElseStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createImplicitNewExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMappingConstructorExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMarkdownDocumentationLineNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMarkdownDocumentationNode;
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
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSpecificFieldNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTemplateExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTypeParameterNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTypedBindingPatternNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createVariableDeclarationNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createWhileStatementNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ACTION_STATEMENT;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ANYDATA_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.BACKTICK_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CHECK_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLASS_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLIENT_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACKET_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.COLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.COMMA_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.DOT_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ELSE_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.EOF_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.EQUAL_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ERROR_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FINAL_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FUNCTION_CALL;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FUNCTION_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.GT_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.IF_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ISOLATED_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.LT_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.MATCH_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.NEW_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACKET_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PLUS_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PRIVATE_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PUBLIC_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.QUESTION_MARK_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.REMOTE_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RETURNS_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RETURN_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RIGHT_ARROW_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SEMICOLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.STRING_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.TRUE_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.WHILE_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.WORKER_KEYWORD;

/**
 * This class is used to generate ballerina client file according to given yaml file.
 *
 * @since 1.3.0
 */
public class IntermediateClientGenerator {

    private final AsyncApi25DocumentImpl asyncAPI;
    private final UtilGenerator utilGenerator;
    private final List<String> remoteFunctionNameList;
    private final BallerinaAuthConfigGenerator ballerinaAuthConfigGenerator;
    //    private final Filter filters;
    private final List<ImportDeclarationNode> imports;
    private final BallerinaTypesGenerator ballerinaSchemaGenerator;
    private List<TypeDefinitionNode> typeDefinitionNodeList;
    private List<String> apiKeyNameList = new ArrayList<>();
    private String serverURL;

    public IntermediateClientGenerator(AASClientConfig asyncAPIClientConfig) {

        this.imports = new ArrayList<>();
        this.typeDefinitionNodeList = new ArrayList<>();
        this.asyncAPI = asyncAPIClientConfig.getOpenAPI();
        this.utilGenerator = new UtilGenerator();
        this.remoteFunctionNameList = new ArrayList<>();
        this.ballerinaSchemaGenerator = new BallerinaTypesGenerator(asyncAPI, new LinkedList<>());
        this.serverURL = "/";
        this.ballerinaAuthConfigGenerator = new BallerinaAuthConfigGenerator(false, false,
                ballerinaSchemaGenerator, utilGenerator);

    }


    /**
     * Returns a list of type definition nodes.
     */
    public List<TypeDefinitionNode> getTypeDefinitionNodeList() {

        return typeDefinitionNodeList;
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

        // Create `ballerina/websocket` import declaration node, "ballerina/uuid will be imported only for streaming"
        ImportDeclarationNode importForWebsocket = GeneratorUtils.getImportDeclarationNode(GeneratorConstants.BALLERINA
                , WEBSOCKET);
        ImportDeclarationNode importForNuvinduPipe = GeneratorUtils.getImportDeclarationNode(GeneratorConstants.NUVINDU
                , NUVINDU_PIPE);
        ImportDeclarationNode importForRunTime = GeneratorUtils.getImportDeclarationNode(GeneratorConstants.BALLERINA
                , LANG_RUNTIME);

        imports.add(importForWebsocket);
        imports.add(importForNuvinduPipe);
        imports.add(importForRunTime);


        // Add authentication related records TODO: This has to improve
        ballerinaAuthConfigGenerator.addAuthRelatedRecords(asyncAPI);

        List<ModuleMemberDeclarationNode> nodes = new ArrayList<>();
        // Add class definition node to module member nodes
        nodes.add(getClassDefinitionNode());

        NodeList<ImportDeclarationNode> importsList = createNodeList(imports);


        ModulePartNode modulePartNode =
                createModulePartNode(importsList, createNodeList(nodes), createToken(EOF_TOKEN));
        TextDocument textDocument = TextDocuments.from("");
        SyntaxTree syntaxTree = SyntaxTree.from(textDocument);
        return syntaxTree.modifyWith(modulePartNode);
    }

    public UtilGenerator getBallerinaUtilGenerator() {

        return utilGenerator;
    }
    public BallerinaTypesGenerator getBallerinaTypeGenerator() {

        return ballerinaSchemaGenerator;
    }


    /**
     * Generate Class definition Node with below code structure.
     * <pre>
     *     public client class ChatClient {
     *     private final websocket:Client clientEp;
     *     private final pipe:Pipe writeMessageQueue;
     *     private final pipe:Pipe readMessageQueue;
     *     private final map<pipe:Pipe> pipes;
     *     # Gets invoked to initialize the `connector`.
     *     #
     *     # + config - The configurations to be used when initializing the `connector`
     *     # + serviceUrl - URL of the target service
     *     # + return - An error if connector initialization failed
     *     public function init(websocket:ClientConfiguration clientConfig =  {},
     *     string serviceUrl = "ws://localhost:9090/chat") returns error? {
     *         self.pipes = {};
     *         self.writeMessageQueue = new (1000);
     *         self.readMessageQueue = new (1000);
     *         websocket:Client websocketEp = check new (serviceUrl, clientConfig);
     *         self.clientEp = websocketEp;
     *         self.startMessageWriting();
     *         self.startMessageReading();
     *         self.startPipeTriggering();
     *         return;
     *     }
     *     # Gets invoked to initialize the `connector`.
     *     #
     *     # + config - The configurations to be used when initializing the `connector`
     *     # + serviceUrl - URL of the target service
     *     # + return - An error if connector initialization failed
     *     private function startMessageWriting() {
     *         worker writeMessage returns error {
     *             while true {
     *                 anydata requestMessage = check self.writeMessageQueue.consume(5);
     *                 check self.clientEp->writeMessage(requestMessage);
     *                 runtime:sleep(0.01);
     *             }
     *         }
     *     }
     *     # Gets invoked to initialize the `connector`.
     *     #
     *     # + config - The configurations to be used when initializing the `connector`
     *     # + serviceUrl - URL of the target service
     *     # + return - An error if connector initialization failed
     *     private function startMessageReading() {
     *         worker readMessage returns error {
     *             while true {
     *                 ResponseMessage responseMessage = check self.clientEp->readMessage();
     *                 check self.readMessageQueue.produce(responseMessage, 5);
     *                 runtime:sleep(0.01);
     *             }
     *         }
     *     }
     *     # Gets invoked to initialize the `connector`.
     *     #
     *     # + config - The configurations to be used when initializing the `connector`
     *     # + serviceUrl - URL of the target service
     *     # + return - An error if connector initialization failed
     *     private function startPipeTriggering() {
     *         worker pipeTrigger returns error {
     *             while true {
     *                 ResponseMessage responseMessage = check self.readMessageQueue.consume(5);
     *                 if responseMessage.hasKey("id") {
     *                     ResponseMessageWithId responseMessagWithId = check responseMessage.cloneWithType();
     *                     string id = responseMessagWithId.id;
     *                     pipe:Pipe idPipe = check self.pipes[id].ensureType();
     *                     check idPipe.produce(responseMessagWithId, 5);
     *                 } else {
     *                     string 'type = responseMessage.'type;
     *                     match ('type) {
     *                         "pong_message" => {
     *                             pipe:Pipe pingMessagePipe = check self.pipes["pingMessage"].ensureType();
     *                             check pingMessagePipe.produce(responseMessage, 5);
     *                         }
     *                         "connection_ack_message" => {
     *                             pipe:Pipe connectionInitMessagePipe =
     *                             check self.pipes["connectionInitMessage"].ensureType();
     *                             check connectionInitMessagePipe.produce(responseMessage, 5);
     *                         }
     *                         "error" => {
     *                             pipe:Pipe errorPipe = check self.pipes["error"].ensureType();
     *                             check errorPipe.produce(responseMessage, 5);
     *                         }
     *                     }
     *                 }
     *             }
     *         }
     *     }
     *     #
     *     remote isolated function doSubscribeMessage(SubscribeMessage subscribeMessage, decimal timeout)
     *     returns stream<NextMessage|CompleteMessage|ErrorMessage,error?>|error {
     *         pipe:Pipe subscribeMessagePipe = new (10000);
     *         string id;
     *         lock {
     *             id = uuid:createType1AsString();
     *             self.pipes[id] = subscribeMessagePipe;
     *         }
     *         subscribeMessage["id"] = id;
     *         check self.writeMessageQueue.produce(subscribeMessage, timeout);
     *         stream<NextMessage|CompleteMessage|ErrorMessage,error?> streamMessages;
     *         lock {
     *             StreamGenerator streamGenerator = check new (subscribeMessagePipe, timeout);
     *             streamMessages = new (streamGenerator);
     *         }
     *         return streamMessages;
     *     }
     *     #
     *     remote isolated function doPingMessage(PingMessage pingMessage, decimal timeout) returns PongMessage|error {
     *         pipe:Pipe pingMessagePipe = new (1);
     *         lock {
     *             self.pipes["pingMessage"] = pingMessagePipe;
     *         }
     *         check self.writeMessageQueue.produce(pingMessage, timeout);
     *         anydata responseMessage = check pingMessagePipe.consume(timeout);
     *         PongMessage pongMessage = check responseMessage.cloneWithType();
     *         check pingMessagePipe.immediateClose();
     *         return pongMessage;
     *     }
     *     #
     *     remote isolated function doConnectionInitMessage(ConnectionInitMessage connectionInitMessage,
     *     decimal timeout) returns ConnectionAckMessage|error {
     *         pipe:Pipe connectionInitMessagePipe = new (1);
     *         lock {
     *             self.pipes["connectionInitMessage"] = connectionInitMessagePipe;
     *         }
     *         check self.writeMessageQueue.produce(connectionInitMessage, timeout);
     *         anydata responseMessage = check connectionInitMessagePipe.consume(timeout);
     *         ConnectionAckMessage connectionAckMessage = check responseMessage.cloneWithType();
     *         check connectionInitMessagePipe.immediateClose();
     *         return connectionAckMessage;
     *     }
     *     #
     *     remote isolated function doError(decimal timeout) returns Error|error {
     *         pipe:Pipe errorPipe = new (1);
     *         lock {
     *             self.pipes["error"] = errorPipe;
     *         }
     *         anydata responseMessage = check errorPipe.consume(timeout);
     *         Error errorMessage = check responseMessage.cloneWithType();
     *         check errorPipe.immediateClose();
     *         return errorMessage;
     *     }
     *     #
     *     remote isolated function doPongMessage(PongMessage pongMessage, decimal timeout) returns error? {
     *         check self.writeMessageQueue.produce(pongMessage, timeout);
     *     }
     *     #
     *     remote isolated function doCompleteMessage(CompleteMessage completeMessage, decimal timeout)
     *     returns error? {
     *         check self.writeMessageQueue.produce(completeMessage, timeout);
     *     }
     * }
     * </pre>
     */
    private ClassDefinitionNode getClassDefinitionNode() throws BallerinaAsyncApiException {

        //Get dispatcherKey
        Map<String, JsonNode> extensions= asyncAPI.getExtensions();
//        if(extensions==null){
//            throw new BallerinaAsyncApiException("x-dispatcherKey must include in the specification");
//        }
        if(extensions==null || extensions.get(X_DISPATCHER_KEY)==null){
            throw new BallerinaAsyncApiException("x-dispatcherKey must include in the specification");
        }
        TextNode dispatcherKeyNode= (TextNode) extensions.get(X_DISPATCHER_KEY);

//        if()
        String dispatcherKey = dispatcherKeyNode.asText();
        if(dispatcherKey.equals("")) {
            throw new BallerinaAsyncApiException("x-dispatcherKey cannot be empty");
        }
//        } else if (dispatcherKey.equals("type")) {
//            dispatcherKey="'type";
//        }

        //Get dispatcherStreamId
        String dispatcherStreamId=null;
        if(extensions.get(X_DISPATCHER_STREAM_ID)!=null){
            TextNode dispatcherStreamIdNode=(TextNode)extensions.get(X_DISPATCHER_STREAM_ID);
            if(dispatcherStreamIdNode!=null) {
                dispatcherStreamId = extensions.get(X_DISPATCHER_STREAM_ID).asText();
                if(dispatcherStreamId.equals("")){
                    throw new BallerinaAsyncApiException("x-dispatcherStreamId cannot be empty");
                }
            }
        }

//        ballerinaSchemaGenerator.setDispatcherKey(dispatcherKey);
//        ballerinaSchemaGenerator.setDispatcherStreamId(dispatcherStreamId);

        //Create a list to collect match statements when dispatcherStreamId is absent in that schema
        List<MatchClauseNode> matchStatementList = new ArrayList<>();

        // Adding remote functions
        List<String> idMethods = new ArrayList<>();

        List<FunctionDefinitionNode> remoteFunctionNodes = createRemoteFunctions(asyncAPI.getComponents().
                getMessages(), matchStatementList, dispatcherStreamId, dispatcherKey,idMethods);


        if(idMethods.size()==0 && matchStatementList.size()==0){
            throw new BallerinaAsyncApiException("Ballerina client cannot be generated enter correct specification");
        }

        // Collect members for class definition node
        List<Node> memberNodeList = new ArrayList<>();

        // Add instance variable to class definition node
        memberNodeList.addAll(createClassInstanceVariables());

        // Add init function to class definition node
        memberNodeList.add(createInitFunction());

        // Add startInterMediator function
        memberNodeList.add(createStartMessageWriting());

        // Add startMessageReading function
        memberNodeList.add(createStartMessageReading());

        // Add startPipeTriggering function
        memberNodeList.add(createStartPipeTriggering(matchStatementList, idMethods,dispatcherKey, dispatcherStreamId));

        // Add remoteFunctionNodes
        memberNodeList.addAll(remoteFunctionNodes);

        // Generate the class combining members
        MetadataNode metadataNode = getClassMetadataNode();

        //Get title name from the specification
        String titleName = asyncAPI.getInfo().getTitle().trim();

        //Get channel name from the specification
        String channelName = GeneratorUtils.
                removeNonAlphanumeric(asyncAPI.getChannels().getItemNames().get(0).trim());

        //Combine class name as titleName+channelName+Client
        String stringClassName = titleName + channelName + CLIENT_CLASS_NAME;

        IdentifierToken className = createIdentifierToken(stringClassName);
        NodeList<Token> classTypeQualifiers = createNodeList(createToken(CLIENT_KEYWORD), createToken(ISOLATED_KEYWORD));

        return createClassDefinitionNode(metadataNode, createToken(PUBLIC_KEYWORD), classTypeQualifiers,
                createToken(CLASS_KEYWORD), className, createToken(OPEN_BRACE_TOKEN),
                createNodeList(memberNodeList), createToken(CLOSE_BRACE_TOKEN), null);
    }


    /**
     * Create startMessageReading function.
     * <pre>
     *     private function startMessageReading() {
     *         worker readMessage returns error {
     *             while true {
     *                 ResponseMessage responseMessage = check self.clientEp->readMessage();
     *                 check self.readMessageQueue.produce(responseMessage, 5);
     *                 runtime:sleep(0.01);
     *             }
     *         }
     *     }
     * </pre>
     */
    private Node createStartMessageReading() {

        //Create function signature node with metadata documentation
        FunctionSignatureNode functionSignatureNode = getStartMessageReadingFunctionSignatureNode();

        //Create function body node
        FunctionBodyNode functionBodyNode = getStartMessageReadingFunctionBodyNode();

        //Create function name
        NodeList<Token> qualifierList = createNodeList(createToken(PRIVATE_KEYWORD), createToken(ISOLATED_KEYWORD));
        IdentifierToken functionName = createIdentifierToken(START_MESSAGE_READING);

        //Return function
        return createFunctionDefinitionNode(null, getDocCommentsForWorker(X_BALLERINA_MESSAGE_READ_DESCRIPTION,
                        "Use to read messages from the websocket."), qualifierList,
                createToken(FUNCTION_KEYWORD),
                functionName, createEmptyNodeList(), functionSignatureNode, functionBodyNode);
    }


    //
    private FunctionBodyNode getStartMessageReadingFunctionBodyNode() {


        List<StatementNode> whileStatements = new ArrayList<>();
        Token openParanToken = createToken(OPEN_PAREN_TOKEN);
        Token closeParanToken = createToken(CLOSE_PAREN_TOKEN);
        Token openBraceToken = createToken(OPEN_BRACE_TOKEN);
        Token closeBraceToken = createToken(CLOSE_BRACE_TOKEN);
        Token semicolonToken = createToken(SEMICOLON_TOKEN);
        Token equalToken = createToken(EQUAL_TOKEN);
        Token dotToken = createToken(DOT_TOKEN);
        Token rightArrowToken = createToken(RIGHT_ARROW_TOKEN);


        //ResponseMessage responseMessage = check self.clientEp->readMessage();
        FieldAccessExpressionNode clientEp = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(SELF)), createToken(DOT_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken(CLIENT_EP)));
        CheckExpressionNode responseMessageCheckExpressionNode = createCheckExpressionNode(null,
                createToken(CHECK_KEYWORD),
                createMethodCallExpressionNode(clientEp, rightArrowToken,
                        createSimpleNameReferenceNode(createIdentifierToken(READ_MESSAGE)), openParanToken,
                        createSeparatedNodeList(new ArrayList<>()), closeParanToken));
        VariableDeclarationNode responseMessage = createVariableDeclarationNode(createEmptyNodeList(),
                null, createTypedBindingPatternNode(
                        createBuiltinSimpleNameReferenceNode(null, createIdentifierToken(RESPONSE_MESSAGE)),
                        createFieldBindingPatternVarnameNode(createSimpleNameReferenceNode(createIdentifierToken(
                                RESPONSE_MESSAGE_VAR_NAME)))),
                equalToken, responseMessageCheckExpressionNode, semicolonToken);


        //check self.readMessageQueue.produce(responseMessage,5);
        FieldAccessExpressionNode readMessageQueue = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(SELF)), createToken(DOT_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken(READ_MESSAGE_QUEUE)));
        CheckExpressionNode checkReadMessageQueueProduceNode = createCheckExpressionNode(null,
                createToken(CHECK_KEYWORD),
                createMethodCallExpressionNode(readMessageQueue, dotToken,
                        createSimpleNameReferenceNode(createIdentifierToken(PRODUCE)), openParanToken,
                        createSeparatedNodeList(
                                createPositionalArgumentNode(createRequiredExpressionNode(
                                        createIdentifierToken(RESPONSE_MESSAGE_VAR_NAME))),
                                createToken(COMMA_TOKEN),
                                createPositionalArgumentNode(createRequiredExpressionNode(
                                        createIdentifierToken(DEFAULT_TIME_OUT)))
                        ), closeParanToken));
        ExpressionStatementNode readMessageQueueCheck = createExpressionStatementNode(null,
                checkReadMessageQueueProduceNode, createToken(SEMICOLON_TOKEN));


        //runtime:sleep(0.01);
        QualifiedNameReferenceNode qualifiedNameReferenceNode = createQualifiedNameReferenceNode(createIdentifierToken(
                RUNTIME), createToken(COLON_TOKEN), createIdentifierToken(SLEEP));
        FunctionCallExpressionNode sleep = createFunctionCallExpressionNode(qualifiedNameReferenceNode, openParanToken,
                createSeparatedNodeList(createPositionalArgumentNode(
                        createRequiredExpressionNode(createIdentifierToken(WORKER_SLEEP_TIME_OUT)))), closeParanToken);
        ExpressionStatementNode runtimeSleep = createExpressionStatementNode(null,
                sleep, createToken(SEMICOLON_TOKEN));


        //whileStatements.add(writeMessage);
        whileStatements.add(responseMessage);
        whileStatements.add(readMessageQueueCheck);
        whileStatements.add(runtimeSleep);


        BlockStatementNode whileBody = createBlockStatementNode(openBraceToken, createNodeList(whileStatements),
                closeBraceToken);
        NodeList<StatementNode> workerStatements = createNodeList(createWhileStatementNode(createToken(WHILE_KEYWORD),
                createBasicLiteralNode(TRUE_KEYWORD, createToken(TRUE_KEYWORD)), whileBody, null));


        NodeList<AnnotationNode> annotations = createEmptyNodeList();
        NodeList workerDeclarationNodes = createNodeList(createNamedWorkerDeclarationNode(annotations,
                null, createToken(WORKER_KEYWORD)
                , createIdentifierToken(READ_MESSAGE), createReturnTypeDescriptorNode(
                        createToken(RETURNS_KEYWORD), createEmptyNodeList(), createToken(ERROR_KEYWORD)),
                createBlockStatementNode(openBraceToken, workerStatements,
                        closeBraceToken)));


        return createFunctionBodyBlockNode(openBraceToken,
                null, workerDeclarationNodes, closeBraceToken, null);


    }

    private FunctionSignatureNode getStartMessageReadingFunctionSignatureNode() {

        SeparatedNodeList<ParameterNode> parameterList = createSeparatedNodeList(new ArrayList<>());

        return createFunctionSignatureNode(
                createToken(OPEN_PAREN_TOKEN), parameterList, createToken(CLOSE_PAREN_TOKEN), null);

    }

    private Node createStartPipeTriggering(List<MatchClauseNode> matchClauseNodes, List<String> idMethods,
                                           String dispatcherKey, String dispatcherStreamId) throws BallerinaAsyncApiException {

        //List to store metadata of the function
        ArrayList initMetaDataDoc = new ArrayList();

        //Create function signature node with metadata documentation
        FunctionSignatureNode functionSignatureNode = getStartPipeTriggeringFunctionSignatureNode(initMetaDataDoc);

        //Create function body node
        FunctionBodyNode functionBodyNode = getStartPipeTriggeringFunctionBodyNode(dispatcherKey,
                dispatcherStreamId, matchClauseNodes,idMethods);

        //Create function name
        NodeList<Token> qualifierList = createNodeList(createToken(PRIVATE_KEYWORD), createToken(ISOLATED_KEYWORD));
        IdentifierToken functionName = createIdentifierToken(START_PIPE_TRIGGERING);


        //Return function
        return createFunctionDefinitionNode(null, getDocCommentsForWorker(X_BALLERINA_PIPE_TRIGGER_DESCRIPTION,
                        "Use to map received message responses into relevant requests.\n"), qualifierList,
                createToken(FUNCTION_KEYWORD),
                functionName, createEmptyNodeList(), functionSignatureNode, functionBodyNode);
    }

    private FunctionBodyNode getStartPipeTriggeringFunctionBodyNode(String dispatcherKey, String dispatcherStreamId,
                                                                    List<MatchClauseNode> matchClauseNodes,
                                                                    List<String> idMethods) throws BallerinaAsyncApiException {

        //Define variables
        Token openParanToken = createToken(OPEN_PAREN_TOKEN);
        Token closeParanToken = createToken(CLOSE_PAREN_TOKEN);
        Token openBraceToken = createToken(OPEN_BRACE_TOKEN);
        Token closeBraceToken = createToken(CLOSE_BRACE_TOKEN);
        Token semicolonToken = createToken(SEMICOLON_TOKEN);
        Token equalToken = createToken(EQUAL_TOKEN);
        Token dotToken = createToken(DOT_TOKEN);


        SimpleNameReferenceNode responseMessageTypeNode = createSimpleNameReferenceNode(
                createIdentifierToken(RESPONSE_MESSAGE));
        SimpleNameReferenceNode responseMessageVarNode = createSimpleNameReferenceNode(
                createIdentifierToken(RESPONSE_MESSAGE_VAR_NAME));

        //Create a list to add while statements
        List<StatementNode> whileStatements = new ArrayList<>();

        //ResponseMessage responseMessage = check self.readMessageQueue.consume(5);
        FieldAccessExpressionNode globalQueue = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(SELF)), createToken(DOT_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken(READ_MESSAGE_QUEUE)));
        CheckExpressionNode checkExpressionNode = createCheckExpressionNode(null, createToken(CHECK_KEYWORD),
                createMethodCallExpressionNode(globalQueue, dotToken,
                        createSimpleNameReferenceNode(createIdentifierToken(CONSUME)), openParanToken,
                        createSeparatedNodeList(
                                createPositionalArgumentNode(createRequiredExpressionNode(
                                        createIdentifierToken(DEFAULT_TIME_OUT)))
                        ), closeParanToken));

        VariableDeclarationNode responseMessageNode = createVariableDeclarationNode(createEmptyNodeList(),
                null, createTypedBindingPatternNode(
                        responseMessageTypeNode,
                        createFieldBindingPatternVarnameNode(responseMessageVarNode)), equalToken, checkExpressionNode,
                semicolonToken);
        AsyncApi25SchemaImpl responseMessageSchema=createResponseMessage(dispatcherKey);
        TypeDefinitionNode responseMessageTypeDefinitionNode = ballerinaSchemaGenerator.getTypeDefinitionNode
                ( responseMessageSchema, RESPONSE_MESSAGE, new ArrayList<>());
        GeneratorUtils.updateTypeDefNodeList(RESPONSE_MESSAGE, responseMessageTypeDefinitionNode,
                typeDefinitionNodeList);




        //////////////////////////////////////////////////////////////////////////////////////////////////////////////
        whileStatements.add(responseMessageNode);

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
        if(dispatcherKey.equals("type")){
            dispatcherKey="'type";
        }
        IfElseStatementNode ifElseStatementNode;
        if (matchClauseNodes.size() != 0 && idMethods.size() != 0) {
            ArrayList<StatementNode> ifStatementNodes = getIfStatementNodes(dispatcherStreamId);
            AsyncApi25SchemaImpl responseMessageWithIdSchema=createResponseMessageWithIDRecord(
                    dispatcherKey,dispatcherStreamId);
            TypeDefinitionNode typeDefinitionNode = ballerinaSchemaGenerator.getTypeDefinitionNode
                    ( responseMessageWithIdSchema, RESPONSE_MESSAGE_WITH_ID, new ArrayList<>());
            GeneratorUtils.updateTypeDefNodeList(RESPONSE_MESSAGE_WITH_ID, typeDefinitionNode, typeDefinitionNodeList);
//            ballerinaSchemaGenerator.setIdMethodsPresent(true);


            ArrayList<StatementNode> elseStatementNodes = getElseStatementNodes(dispatcherKey,
                    matchClauseNodes);


            //Create if else statement node
            ifElseStatementNode = createIfElseStatementNode(createToken(IF_KEYWORD),
                    createMethodCallExpressionNode(responseMessageVarNode, dotToken, createSimpleNameReferenceNode(
                                    createIdentifierToken("hasKey")),
                            openParanToken, createSeparatedNodeList(createSimpleNameReferenceNode(
                                    createIdentifierToken("\"" + dispatcherStreamId + "\""))),
                            closeParanToken), createBlockStatementNode(openBraceToken, createNodeList(ifStatementNodes),
                            closeBraceToken), createElseBlockNode(createToken(ELSE_KEYWORD),
                            createBlockStatementNode(openBraceToken, createNodeList(elseStatementNodes),
                                    closeBraceToken)));
            whileStatements.add(ifElseStatementNode);

        } else if(matchClauseNodes.size() == 0 && idMethods.size() != 0){
            //Create if else statement node
            ArrayList<StatementNode> ifStatementNodes = getIfStatementNodes(dispatcherStreamId);
            AsyncApi25SchemaImpl responseMessageWithIdSchema=createResponseMessageWithIDRecord(dispatcherKey,dispatcherStreamId);
            TypeDefinitionNode typeDefinitionNode = ballerinaSchemaGenerator.getTypeDefinitionNode
                    (responseMessageWithIdSchema, RESPONSE_MESSAGE_WITH_ID, new ArrayList<>());
            GeneratorUtils.updateTypeDefNodeList(RESPONSE_MESSAGE_WITH_ID, typeDefinitionNode, typeDefinitionNodeList);

            ifElseStatementNode = createIfElseStatementNode(createToken(IF_KEYWORD),
                    createMethodCallExpressionNode(responseMessageVarNode, dotToken, createSimpleNameReferenceNode(
                                    createIdentifierToken("hasKey")),
                            openParanToken, createSeparatedNodeList(createSimpleNameReferenceNode(
                                    createIdentifierToken("\"" + dispatcherStreamId + "\""))),
                            closeParanToken), createBlockStatementNode(openBraceToken, createNodeList(ifStatementNodes),
                            closeBraceToken), null);
            whileStatements.add(ifElseStatementNode);


        }else{
            // string type=responseMessage.type;
            FieldAccessExpressionNode responseDispatcherKey = createFieldAccessExpressionNode(responseMessageVarNode,
                    dotToken,
                    createSimpleNameReferenceNode(createIdentifierToken(dispatcherKey)));
            VariableDeclarationNode dispatcherKeyStatement = createVariableDeclarationNode(createEmptyNodeList(),
                    null, createTypedBindingPatternNode(
                            createBuiltinSimpleNameReferenceNode(null, createIdentifierToken(STRING)),
                            createFieldBindingPatternVarnameNode(createSimpleNameReferenceNode(
                                    createIdentifierToken(dispatcherKey)))),
                    equalToken, responseDispatcherKey, semicolonToken);
            //combine whole match statements generated using previous createRemoteFunctions method
            MatchStatementNode matchStatementNode = createMatchStatementNode(createToken(MATCH_KEYWORD),
                    createBracedExpressionNode(null, openParanToken,
                            createSimpleNameReferenceNode(createIdentifierToken(dispatcherKey)),
                            closeParanToken), openBraceToken, createNodeList(matchClauseNodes), closeBraceToken,
                    null);
            whileStatements.add(dispatcherKeyStatement);
            whileStatements.add(matchStatementNode);
            //Create if else statement node
//            ifElseStatementNode = createIfElseStatementNode(createToken(IF_KEYWORD),
//                    createMethodCallExpressionNode(responseMessageVarNode, dotToken, createSimpleNameReferenceNode(
//                                    createIdentifierToken("hasKey")),
//                            openParanToken, createSeparatedNodeList(createSimpleNameReferenceNode(
//                                    createIdentifierToken("\"" + dispatcherStreamId + "\""))),
//                            closeParanToken), createBlockStatementNode(openBraceToken, createNodeList(ifStatementNodes),
//                            closeBraceToken), null);

        }



        //Create while body node
        BlockStatementNode whileBody = createBlockStatementNode(openBraceToken, createNodeList(whileStatements),
                closeBraceToken);

        //Add worker statements (whileBody is added)
        NodeList<StatementNode> workerStatements = createNodeList(createWhileStatementNode(createToken(WHILE_KEYWORD),
                createBasicLiteralNode(TRUE_KEYWORD, createToken(TRUE_KEYWORD)), whileBody, null));

        //Create worker
        NodeList workerDeclarationNodes = createNodeList(createNamedWorkerDeclarationNode(createEmptyNodeList(),
                null, createToken(WORKER_KEYWORD)
                , createIdentifierToken(PIPE_TRIGGER), createReturnTypeDescriptorNode(
                        createToken(RETURNS_KEYWORD), createEmptyNodeList(), createToken(ERROR_KEYWORD)),
                createBlockStatementNode(openBraceToken, workerStatements,
                        closeBraceToken)));

        //Return worker
        return createFunctionBodyBlockNode(openBraceToken,
                null, workerDeclarationNodes, closeBraceToken, null);

    }

        private AsyncApi25SchemaImpl createResponseMessageWithIDRecord(String dispatcherKey,String dispatcherStreamId) {
        //create ResponseMessage record
        AsyncApi25SchemaImpl responseMessageWithId = new AsyncApi25SchemaImpl();
        responseMessageWithId.setType(OBJECT);
        AsyncApi25SchemaImpl stringEventSchema = new AsyncApi25SchemaImpl();
        AsyncApi25SchemaImpl stringIdSchema = new AsyncApi25SchemaImpl();

        stringEventSchema.setType(STRING);
        stringIdSchema.setType(STRING);
        List requiredFields = new ArrayList();
        requiredFields.add(dispatcherKey);
        requiredFields.add(dispatcherStreamId);

        responseMessageWithId.setRequired(requiredFields);
        responseMessageWithId.addProperty(dispatcherKey, stringEventSchema);
        responseMessageWithId.addProperty(dispatcherStreamId, stringIdSchema);
        return responseMessageWithId;
//        schemas.put(RESPONSE_MESSAGE_WITH_ID_VAR_NAME, responseMessageWithId);
    }

    private AsyncApi25SchemaImpl createResponseMessage(String dispatcherKey) {
        //create ResponseMessage record
        AsyncApi25SchemaImpl responseMessage = new AsyncApi25SchemaImpl();
        responseMessage.setType(OBJECT);
        AsyncApi25SchemaImpl stringEventSchema = new AsyncApi25SchemaImpl();
        AsyncApi25SchemaImpl stringIdSchema = new AsyncApi25SchemaImpl();

        stringEventSchema.setType(STRING);
        stringIdSchema.setType(STRING);
        List requiredFields = new ArrayList();
        requiredFields.add(dispatcherKey);

        responseMessage.setRequired(requiredFields);
        responseMessage.addProperty(dispatcherKey, stringEventSchema);
        return responseMessage;
//        schemas.put(RESPONSE_MESSAGE_WITH_ID_VAR_NAME, responseMessage);
    }

    private ArrayList<StatementNode> getElseStatementNodes(String dispatcherKey, List<MatchClauseNode> matchClauseNodes) {
        SimpleNameReferenceNode responseMessageVarNode = createSimpleNameReferenceNode(
                createIdentifierToken(RESPONSE_MESSAGE_VAR_NAME));
        Token openParanToken = createToken(OPEN_PAREN_TOKEN);
        Token closeParanToken = createToken(CLOSE_PAREN_TOKEN);
        Token openBraceToken = createToken(OPEN_BRACE_TOKEN);
        Token closeBraceToken = createToken(CLOSE_BRACE_TOKEN);
        Token semicolonToken = createToken(SEMICOLON_TOKEN);
        Token equalToken = createToken(EQUAL_TOKEN);
        Token dotToken = createToken(DOT_TOKEN);

        //Else statements
        ArrayList<StatementNode> elseStatementNodes = new ArrayList<>();
        // string type=responseMessage.type;
        FieldAccessExpressionNode responseDispatcherKey = createFieldAccessExpressionNode(responseMessageVarNode,
                dotToken,
                createSimpleNameReferenceNode(createIdentifierToken(dispatcherKey)));
        VariableDeclarationNode dispatcherKeyStatement = createVariableDeclarationNode(createEmptyNodeList(),
                null, createTypedBindingPatternNode(
                        createBuiltinSimpleNameReferenceNode(null, createIdentifierToken(STRING)),
                        createFieldBindingPatternVarnameNode(createSimpleNameReferenceNode(
                                createIdentifierToken(dispatcherKey)))),
                equalToken, responseDispatcherKey, semicolonToken);
        //combine whole match statements generated using previous createRemoteFunctions method
        MatchStatementNode matchStatementNode = createMatchStatementNode(createToken(MATCH_KEYWORD),
                createBracedExpressionNode(null, openParanToken,
                        createSimpleNameReferenceNode(createIdentifierToken(dispatcherKey)),
                        closeParanToken), openBraceToken, createNodeList(matchClauseNodes), closeBraceToken,
                null);

        //Add all else statements
        elseStatementNodes.add(dispatcherKeyStatement);
        elseStatementNodes.add(matchStatementNode);
        return elseStatementNodes;
    }

    private ArrayList<StatementNode> getIfStatementNodes(String dispatcherStreamId) {
        Token openParanToken = createToken(OPEN_PAREN_TOKEN);
        Token closeParanToken = createToken(CLOSE_PAREN_TOKEN);
        Token semicolonToken = createToken(SEMICOLON_TOKEN);
        Token equalToken = createToken(EQUAL_TOKEN);
        Token dotToken = createToken(DOT_TOKEN);

        SimpleNameReferenceNode responseMessageWithIdNode = createSimpleNameReferenceNode(
                createIdentifierToken(RESPONSE_MESSAGE_WITH_ID));
        SimpleNameReferenceNode responseMessageVarNode = createSimpleNameReferenceNode(
                createIdentifierToken(RESPONSE_MESSAGE_VAR_NAME));
        SimpleNameReferenceNode responseMessageWithIdVarNode = createSimpleNameReferenceNode(
                createIdentifierToken(RESPONSE_MESSAGE_WITH_ID_VAR_NAME));

        //If statements
        ArrayList<StatementNode> ifStatementNodes = new ArrayList<>();


        //ResponseMessageWithId responseMessageWithId = check responseMessage.cloneWithType();
        MethodCallExpressionNode cloneWithTypeMethodCallExpressionNode = createMethodCallExpressionNode(
                responseMessageVarNode, dotToken,
                createSimpleNameReferenceNode(createIdentifierToken("cloneWithType")),
                openParanToken, createSeparatedNodeList(), closeParanToken);

        CheckExpressionNode cloneWithTypeCheck = createCheckExpressionNode(null, createToken(CHECK_KEYWORD),
                cloneWithTypeMethodCallExpressionNode);


        VariableDeclarationNode responseTypeCloneStatement = createVariableDeclarationNode(createEmptyNodeList(),
                null,
                createTypedBindingPatternNode(
                        responseMessageWithIdNode,
                        createFieldBindingPatternVarnameNode(
                                responseMessageWithIdVarNode)),
                equalToken, cloneWithTypeCheck, semicolonToken);


        // string id=responseMessageWithId.id;
        FieldAccessExpressionNode responseMessageId = createFieldAccessExpressionNode(responseMessageWithIdVarNode,
                dotToken,
                createSimpleNameReferenceNode(createIdentifierToken(dispatcherStreamId)));
        VariableDeclarationNode dispatcherStreamStatement = createVariableDeclarationNode(createEmptyNodeList(),
                null, createTypedBindingPatternNode(
                        createBuiltinSimpleNameReferenceNode(null, createIdentifierToken(STRING)),
                        createFieldBindingPatternVarnameNode(createSimpleNameReferenceNode(
                                createIdentifierToken(dispatcherStreamId)))),
                equalToken, responseMessageId, semicolonToken);


        //pipe:Pipe idPipe = self.pipes.getPipe(id);
        QualifiedNameReferenceNode pipeTypeName = createQualifiedNameReferenceNode(createIdentifierToken(SIMPLE_PIPE),
                createToken(COLON_TOKEN), createIdentifierToken(GeneratorConstants.CAPITAL_PIPE));
        FieldAccessExpressionNode selfPipes = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(SELF)), createToken(DOT_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken(PIPES)));

        MethodCallExpressionNode methodCallExpressionNode = createMethodCallExpressionNode(selfPipes, dotToken,
                createSimpleNameReferenceNode(createIdentifierToken("getPipe")),
                openParanToken, createSeparatedNodeList(createSimpleNameReferenceNode(
                        createIdentifierToken(dispatcherStreamId))), closeParanToken);

//        CheckExpressionNode selfPipeCheck = createCheckExpressionNode(null, createToken(CHECK_KEYWORD),
//                methodCallExpressionNode);
        SimpleNameReferenceNode responseTypePipeNode = createSimpleNameReferenceNode(createIdentifierToken(
                dispatcherStreamId + "Pipe"));
        VariableDeclarationNode pipeTypeEnsureStatement = createVariableDeclarationNode(createEmptyNodeList(),
                null,
                createTypedBindingPatternNode(
                        pipeTypeName,
                        createFieldBindingPatternVarnameNode(responseTypePipeNode)),
                equalToken, methodCallExpressionNode, semicolonToken);

        //check idPipe.produce(user, 5);
        List<Node> nodes = new ArrayList<>();
        nodes.add(responseMessageWithIdVarNode);
        nodes.add(createToken(COMMA_TOKEN));
        nodes.add(createIdentifierToken("5"));
        MethodCallExpressionNode pipeProduceExpressionNode = createMethodCallExpressionNode(responseTypePipeNode,
                dotToken,
                createSimpleNameReferenceNode(createIdentifierToken("produce")),
                openParanToken, createSeparatedNodeList(nodes), closeParanToken);

        CheckExpressionNode pipeProduceCheck = createCheckExpressionNode(null, createToken(CHECK_KEYWORD),
                pipeProduceExpressionNode);
        ExpressionStatementNode pipeProduceExpression = createExpressionStatementNode(null,
                pipeProduceCheck, createToken(SEMICOLON_TOKEN));

        //Add all if statements
        ifStatementNodes.add(responseTypeCloneStatement);
        ifStatementNodes.add(dispatcherStreamStatement);
        ifStatementNodes.add(pipeTypeEnsureStatement);
        ifStatementNodes.add(pipeProduceExpression);
        return ifStatementNodes;
    }

    private FunctionSignatureNode getStartPipeTriggeringFunctionSignatureNode(ArrayList initMetaDataNode) {

        SeparatedNodeList<ParameterNode> parameterList = createSeparatedNodeList();
        return createFunctionSignatureNode(
                createToken(OPEN_PAREN_TOKEN), parameterList, createToken(CLOSE_PAREN_TOKEN), null);
    }

    private Node createStartMessageWriting() {

        FunctionSignatureNode functionSignatureNode = getStartMessageWritingFunctionSignatureNode();
        FunctionBodyNode functionBodyNode = getStartMessageWritingFunctionBodyNode();
        NodeList<Token> qualifierList = createNodeList(createToken(PRIVATE_KEYWORD), createToken(ISOLATED_KEYWORD));
        IdentifierToken functionName = createIdentifierToken(START_MESSAGE_WRITING);
        return createFunctionDefinitionNode(null, getDocCommentsForWorker(X_BALLERINA_MESSAGE_WRITE_DESCRIPTION,
                        "Use to write messages to the websocket."), qualifierList,
                createToken(FUNCTION_KEYWORD),
                functionName, createEmptyNodeList(), functionSignatureNode, functionBodyNode);
    }

    private FunctionBodyNode getStartMessageWritingFunctionBodyNode() {
        NodeList<AnnotationNode> annotations = createEmptyNodeList();

        List<StatementNode> whileStatements = new ArrayList<>();
        Token openParanToken = createToken(OPEN_PAREN_TOKEN);
        Token closeParanToken = createToken(CLOSE_PAREN_TOKEN);
        Token openBraceToken = createToken(OPEN_BRACE_TOKEN);
        Token closeBraceToken = createToken(CLOSE_BRACE_TOKEN);
        Token semicolonToken = createToken(SEMICOLON_TOKEN);
        Token equalToken = createToken(EQUAL_TOKEN);
        Token dotToken = createToken(DOT_TOKEN);
        Token rightArrowToken = createToken(RIGHT_ARROW_TOKEN);

        //anydata requestMessage = check self.writeMessageQueue.consume(5);
        FieldAccessExpressionNode globalQueue = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(SELF)), createToken(DOT_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken(WRITE_MESSAGE_QUEUE)));
        CheckExpressionNode checkExpressionNode = createCheckExpressionNode(null, createToken(CHECK_KEYWORD),
                createMethodCallExpressionNode(globalQueue, dotToken,
                        createSimpleNameReferenceNode(createIdentifierToken(CONSUME)), openParanToken,
                        createSeparatedNodeList(
                                createPositionalArgumentNode(createRequiredExpressionNode(
                                        createIdentifierToken(DEFAULT_TIME_OUT)))
                        ), closeParanToken));
        VariableDeclarationNode queueData = createVariableDeclarationNode(createEmptyNodeList(),
                null, createTypedBindingPatternNode(
                        createBuiltinSimpleNameReferenceNode(null, createToken(ANYDATA_KEYWORD)),
                        createFieldBindingPatternVarnameNode(createSimpleNameReferenceNode(
                                createIdentifierToken(REQUEST_MESSAGE)))), equalToken, checkExpressionNode,
                semicolonToken);


        //check self.clientEp->writeMessage(requestMessage);
        FieldAccessExpressionNode clientEp = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(SELF)), createToken(DOT_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken(CLIENT_EP)));
        CheckExpressionNode checkWriteMessage = createCheckExpressionNode(ACTION_STATEMENT, createToken(CHECK_KEYWORD),
                createMethodCallExpressionNode(clientEp, rightArrowToken,
                        createSimpleNameReferenceNode(createIdentifierToken(WRITE_MESSAGE)), openParanToken,
                        createSeparatedNodeList(
                                createPositionalArgumentNode(createRequiredExpressionNode(
                                        createIdentifierToken(REQUEST_MESSAGE)))
                        ), closeParanToken));
        ExpressionStatementNode writeMessage = createExpressionStatementNode(null, checkWriteMessage,
                createToken(SEMICOLON_TOKEN));


        //runtime:sleep(0.01);
        QualifiedNameReferenceNode qualifiedNameReferenceNode = createQualifiedNameReferenceNode(createIdentifierToken(
                "runtime"), createToken(COLON_TOKEN), createIdentifierToken("sleep"));
        FunctionCallExpressionNode sleep = createFunctionCallExpressionNode(qualifiedNameReferenceNode, openParanToken,
                createSeparatedNodeList(createPositionalArgumentNode(
                        createRequiredExpressionNode(createIdentifierToken("0.01")))), closeParanToken);
        ExpressionStatementNode runtimeSleep = createExpressionStatementNode(null,
                sleep, createToken(SEMICOLON_TOKEN));


        whileStatements.add(queueData);
        whileStatements.add(writeMessage);
        whileStatements.add(runtimeSleep);


        BlockStatementNode whileBody = createBlockStatementNode(openBraceToken, createNodeList(whileStatements),
                closeBraceToken);
        NodeList<StatementNode> workerStatements = createNodeList(createWhileStatementNode(createToken(WHILE_KEYWORD),
                createBasicLiteralNode(TRUE_KEYWORD, createToken(TRUE_KEYWORD)), whileBody, null));


        NodeList workerDeclarationNodes = createNodeList(createNamedWorkerDeclarationNode(annotations,
                null, createToken(WORKER_KEYWORD)
                , createIdentifierToken(WRITE_MESSAGE), createReturnTypeDescriptorNode(
                        createToken(RETURNS_KEYWORD), createEmptyNodeList(), createToken(ERROR_KEYWORD)),
                createBlockStatementNode(openBraceToken, workerStatements,
                        closeBraceToken)));


        return createFunctionBodyBlockNode(openBraceToken,
                null, workerDeclarationNodes, closeBraceToken, null);


    }


    //TODO: Add metdata for the function
    private FunctionSignatureNode getStartMessageWritingFunctionSignatureNode() {

        SeparatedNodeList<ParameterNode> parameterList = createSeparatedNodeList(new ArrayList<>());

        return createFunctionSignatureNode(
                createToken(OPEN_PAREN_TOKEN), parameterList, createToken(CLOSE_PAREN_TOKEN), null);
    }

    /**
     * Generate metadata node of the class including documentation. Content of the documentation
     * will be taken from the `description` section inside the `info` section in AsyncAPI definition.
     *
     * @return {@link MetadataNode}    Metadata node of the client class
     */
    private MetadataNode getClassMetadataNode() {

        List<AnnotationNode> classLevelAnnotationNodes = new ArrayList<>();

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
        AsyncApi25SchemaImpl headerSchema = new AsyncApi25SchemaImpl();
        AsyncApi25SchemaImpl querySchema = new AsyncApi25SchemaImpl();
        FunctionSignatureNode functionSignatureNode = getInitFunctionSignatureNode(querySchema,
                headerSchema);
        FunctionBodyNode functionBodyNode = getInitFunctionBodyNode(querySchema, headerSchema);
        NodeList<Token> qualifierList = createNodeList(createToken(PUBLIC_KEYWORD), createToken(ISOLATED_KEYWORD));
        IdentifierToken functionName = createIdentifierToken("init");
        return createFunctionDefinitionNode(null, getInitDocComment(), qualifierList,
                createToken(FUNCTION_KEYWORD),
                functionName, createEmptyNodeList(), functionSignatureNode, functionBodyNode);
    }

    /**
     * Create function body node of client init function.
     *
     * @return {@link FunctionBodyNode}
     */
    private FunctionBodyNode getInitFunctionBodyNode(AsyncApi25SchemaImpl querySchema,
                                                     AsyncApi25SchemaImpl headerSchema)
            throws BallerinaAsyncApiException {

        List<StatementNode> assignmentNodes = new ArrayList<>();


        // If both apiKey and httpOrOAuth is supported
        // todo : After revamping
        if (ballerinaAuthConfigGenerator.isHttpApiKey() && ballerinaAuthConfigGenerator.isHttpOROAuth()) {
            assignmentNodes.add(ballerinaAuthConfigGenerator.handleInitForMixOfApiKeyAndHTTPOrOAuth());
        }

        // create {@code self.pipes =new ();} assignment node
        List<Node> pipesArgumentsList = new ArrayList<>();
        SeparatedNodeList<FunctionArgumentNode> pipesArguments = createSeparatedNodeList(pipesArgumentsList);
        Token closeParenArg = createToken(CLOSE_PAREN_TOKEN);
        Token openParenArg = createToken(OPEN_PAREN_TOKEN);
        ParenthesizedArgList pipesParenthesizedArgList = createParenthesizedArgList(openParenArg,
                pipesArguments,
                closeParenArg);
        ImplicitNewExpressionNode pipesExpressionNode = createImplicitNewExpressionNode(createToken(NEW_KEYWORD),
                pipesParenthesizedArgList);

        FieldAccessExpressionNode selfPipes = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(SELF)), createToken(DOT_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken("pipes")));
//        SimpleNameReferenceNode selfPipesValue = createSimpleNameReferenceNode(createIdentifierToken("{}"));
        AssignmentStatementNode selfPipesAssignmentStatementNode = createAssignmentStatementNode(selfPipes,
                createToken(EQUAL_TOKEN), pipesExpressionNode, createToken(SEMICOLON_TOKEN));
        assignmentNodes.add(selfPipesAssignmentStatementNode);


        // create {@code self.writeMessageQueue = new (1000);} assignment node
        List<Node> argumentsList = new ArrayList<>();
        FieldAccessExpressionNode selfWriteMessageQueues = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(SELF)), createToken(DOT_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken(WRITE_MESSAGE_QUEUE)));
        argumentsList.add(createIdentifierToken("1000"));
        SeparatedNodeList<FunctionArgumentNode> arguments = createSeparatedNodeList(argumentsList);
        ParenthesizedArgList parenthesizedArgList = createParenthesizedArgList(openParenArg,
                arguments,
                closeParenArg);
        ImplicitNewExpressionNode expressionNode = createImplicitNewExpressionNode(createToken(NEW_KEYWORD),
                parenthesizedArgList);

        AssignmentStatementNode selfWriteQueueAssignmentStatementNode = createAssignmentStatementNode(
                selfWriteMessageQueues,
                createToken(EQUAL_TOKEN), expressionNode, createToken(SEMICOLON_TOKEN));
        assignmentNodes.add(selfWriteQueueAssignmentStatementNode);


        // create {@code self.readMessageQueue = new (1000);} assignment node
        FieldAccessExpressionNode selfReadMessageQueues = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(SELF)), createToken(DOT_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken(READ_MESSAGE_QUEUE)));
        AssignmentStatementNode selfReadQueuesAssignmentStatementNode = createAssignmentStatementNode(
                selfReadMessageQueues,
                createToken(EQUAL_TOKEN), expressionNode, createToken(SEMICOLON_TOKEN));
        assignmentNodes.add(selfReadQueuesAssignmentStatementNode);


        List<String> channelList = asyncAPI.getChannels().getItemNames();
        if (channelList.size() != 1) {
            throw new BallerinaAsyncApiException("Ballerina websocket doesn't support for multiple channels");
        }
        String path = channelList.get(0);

        if (!path.equals("/") || querySchema.getProperties() != null) {
            if (!path.equals("/")) {
                VariableDeclarationNode pathInt = getPathStatement(path);
                assignmentNodes.add(pathInt);
                handleParameterSchemaInOperation(querySchema, headerSchema, assignmentNodes, false);

            } else {
                handleParameterSchemaInOperation(querySchema, headerSchema, assignmentNodes, true);

            }

            assignmentNodes.add(ballerinaAuthConfigGenerator.getClientInitializationNode(MODIFIED_URL));


        } else {
            assignmentNodes.add(ballerinaAuthConfigGenerator.getClientInitializationNode(SERVICE_URL));

        }

        //TODO: move this
        //Handle query parameter map

        // create {@websocket:Client websocketEp=check new(modifiedUrl,clientConfig)}
        // create initialization statement of websocket:Client class instance

        // create {@code self.clientEp = websocketEp;} assignment node
        FieldAccessExpressionNode selfClientEp = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(SELF)), createToken(DOT_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken("clientEp")));
        SimpleNameReferenceNode selfClientEpValue = createSimpleNameReferenceNode(createIdentifierToken(
                "websocketEp"));
        AssignmentStatementNode selfWebsocketClientAssignmentStatementNode = createAssignmentStatementNode(
                selfClientEp,
                createToken(EQUAL_TOKEN), selfClientEpValue, createToken(SEMICOLON_TOKEN));
        assignmentNodes.add(selfWebsocketClientAssignmentStatementNode);

        // create {@code self.startMessageWriting()} assignment node
        List workersArgumentsList = new ArrayList<>();
        SeparatedNodeList<FunctionArgumentNode> workersArguments =
                createSeparatedNodeList(workersArgumentsList);
        ExpressionStatementNode startMessageWriting = createExpressionStatementNode(FUNCTION_CALL,
                createMethodCallExpressionNode(createSimpleNameReferenceNode(createIdentifierToken(SELF)),
                        createToken(DOT_TOKEN), createSimpleNameReferenceNode(
                                createIdentifierToken(START_MESSAGE_WRITING)), openParenArg,
                        workersArguments, closeParenArg), createToken(SEMICOLON_TOKEN));

        // create {@code self.startMessageReading()} assignment node

        ExpressionStatementNode startMessageReading = createExpressionStatementNode(FUNCTION_CALL,
                createMethodCallExpressionNode(createSimpleNameReferenceNode(createIdentifierToken(SELF)),
                        createToken(DOT_TOKEN), createSimpleNameReferenceNode(
                                createIdentifierToken(START_MESSAGE_READING)), openParenArg,
                        workersArguments, closeParenArg), createToken(SEMICOLON_TOKEN));

        // create {@code self.startPipeTriggering()} assignment node

        ExpressionStatementNode startPipeTriggering = createExpressionStatementNode(FUNCTION_CALL,
                createMethodCallExpressionNode(createSimpleNameReferenceNode(createIdentifierToken(SELF)),
                        createToken(DOT_TOKEN), createSimpleNameReferenceNode(
                                createIdentifierToken(START_PIPE_TRIGGERING)), openParenArg,
                        workersArguments, closeParenArg), createToken(SEMICOLON_TOKEN));


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
     * Generate statements for query parameters and headers.
     */
    private void handleParameterSchemaInOperation(AsyncApi25SchemaImpl querySchema, AsyncApi25SchemaImpl headerSchema,
                                                  List<StatementNode> statementsList, boolean initalized)
            throws BallerinaAsyncApiException {

        handleQueryParamsAndHeaders(querySchema, headerSchema, statementsList, initalized);
    }

    /**
     * Handle query parameters and headers within a remote function.
     */
    public void handleQueryParamsAndHeaders(AsyncApi25SchemaImpl querySchema, AsyncApi25SchemaImpl headerSchema,
                                            List<StatementNode> statementsList, boolean initalized) {

        if (querySchema.getProperties() != null) {
            utilGenerator.setQueryParamsFound(true);
            statementsList.add(getMapForParameters(querySchema, "map<anydata>",
                    QUERY_PARAM));
            ExpressionStatementNode updatedPath;
            if (initalized) {
                updatedPath = GeneratorUtils.getSimpleExpressionStatementNode(
                        "string " + MODIFIED_URL + " = " + SERVICE_URL + " + check getPathForQueryParam(queryParam)");

            } else {
                updatedPath = GeneratorUtils.getSimpleExpressionStatementNode(
                        MODIFIED_URL + " = " + MODIFIED_URL + " + check getPathForQueryParam(queryParam)");

            }
            statementsList.add(updatedPath);

        }
        if (headerSchema.getProperties() != null) {
            statementsList.add(getMapForParameters(headerSchema, "map<string>",
                    HEADER_PARAM));
            statementsList.add(GeneratorUtils.getSimpleExpressionStatementNode(
                    "map<string> " + CUSTOM_HEADERS + " = getCombineHeaders(clientConfig.customHeaders,headerParam)"));
            statementsList.add(GeneratorUtils.getSimpleExpressionStatementNode(
                    "clientConfig.customHeaders=customHeaders"
            ));
            utilGenerator.setHeadersFound(true);
        }
    }


    /**
     * Generate map variable for query parameters and headers.
     */
    private VariableDeclarationNode getMapForParameters(AsyncApi25SchemaImpl parameters, String mapDataType,
                                                        String mapName) {
        List<Node> filedOfMap = new ArrayList<>();
        BuiltinSimpleNameReferenceNode mapType = createBuiltinSimpleNameReferenceNode(null,
                createIdentifierToken(mapDataType));
        CaptureBindingPatternNode bindingPattern = createCaptureBindingPatternNode(
                createIdentifierToken(mapName));
        TypedBindingPatternNode bindingPatternNode = createTypedBindingPatternNode(mapType, bindingPattern);
        Map<String, Schema> properties = parameters.getProperties();
        for (Map.Entry<String, Schema> entry : properties.entrySet()) {
            // Initializer
            IdentifierToken fieldName = createIdentifierToken('"' + (entry.getKey().trim()) + '"');
            Token colon = createToken(COLON_TOKEN);
            SimpleNameReferenceNode valueExpr = createSimpleNameReferenceNode(
                    createIdentifierToken(mapName+"s." + getValidName(entry.getKey().trim(), false)));
            SpecificFieldNode specificFieldNode = createSpecificFieldNode(null,
                    fieldName, colon, valueExpr);
            filedOfMap.add(specificFieldNode);
            filedOfMap.add(createToken(COMMA_TOKEN));
        }


        filedOfMap.remove(filedOfMap.size() - 1);
        MappingConstructorExpressionNode initialize = createMappingConstructorExpressionNode(
                createToken(OPEN_BRACE_TOKEN), createSeparatedNodeList(filedOfMap),
                createToken(CLOSE_BRACE_TOKEN));
        return createVariableDeclarationNode(createEmptyNodeList(),
                null, bindingPatternNode, createToken(EQUAL_TOKEN), initialize,
                createToken(SEMICOLON_TOKEN));
    }

    /**
     * This method use to generate Path statement inside the function body node.
     * <p>
     * ex:
     * <pre> string  path = string `/weather`; </pre>
     *
     * @param path - Given path
     * @return - VariableDeclarationNode for path statement.
     */
    private VariableDeclarationNode getPathStatement(String path) {

        TypedBindingPatternNode typedBindingPatternNode = createTypedBindingPatternNode(createSimpleNameReferenceNode(
                createToken(STRING_KEYWORD)), createCaptureBindingPatternNode(
                createIdentifierToken(MODIFIED_URL)));
        // Create initializer
        // Content  should decide with /pet and /pet/{pet} by adding getEncodedUri
        path = generatePathWithPathParameter(path);
        //String path generator
        NodeList<Node> content = createNodeList(createLiteralValueToken(null, path, createEmptyMinutiaeList(),
                createEmptyMinutiaeList()));
        SimpleNameReferenceNode lhsExpr = createSimpleNameReferenceNode(createIdentifierToken(SERVICE_URL));
        TemplateExpressionNode rhsExpr = createTemplateExpressionNode(null, createToken(STRING_KEYWORD),
                createToken(BACKTICK_TOKEN), content, createToken(BACKTICK_TOKEN));
        BinaryExpressionNode initializer = createBinaryExpressionNode(null, lhsExpr, createToken(PLUS_TOKEN),
                rhsExpr);

        return createVariableDeclarationNode(createEmptyNodeList(), null,
                typedBindingPatternNode, createToken(EQUAL_TOKEN), initializer, createToken(SEMICOLON_TOKEN));
    }


    /**
     * This method is to used for generating path when it has path parameters.
     *
     * @param path - yaml contract path
     * @return string of path
     */
    public String generatePathWithPathParameter(String path) {

        if (path.contains("{")) {
            String refinedPath = path;
            Pattern p = Pattern.compile("\\{[^}]*}");
            Matcher m = p.matcher(path);
            while (m.find()) {
                String pathVariable = path.substring(m.start(), m.end());
                if (pathVariable.startsWith("{") && pathVariable.endsWith("}")) {
                    String d = pathVariable.replace("{", "").replace("}", "");
                    String replaceVariable = "{getEncodedUri(" + "pathParams." + getValidName(d, false) + ")}";
                    refinedPath = refinedPath.replace(pathVariable, replaceVariable);
                }
            }
            path = refinedPath.replaceAll("[{]", "\\${");
            utilGenerator.setPathParametersFound(true);

        }
        return path;
    }


    /**
     * Create function signature node of client init function.
     *
     * @return {@link FunctionSignatureNode}
     * @throws BallerinaAsyncApiException When invalid server URL is provided
     */
    private FunctionSignatureNode getInitFunctionSignatureNode(
            AsyncApi25SchemaImpl querySchema,
            AsyncApi25SchemaImpl headerSchema)
            throws BallerinaAsyncApiException {
        //string serviceUrl = "ws://localhost:9090/payloadV"
        serverURL = getServerURL((AsyncApi25ServersImpl) asyncAPI.getServers());

        List<Node> parameters = new ArrayList<>();

        AsyncApiChannelItem channelItem = asyncAPI.getChannels().getItems().get(0);


        //set pathParams,queryParams,headerParams
        ballerinaAuthConfigGenerator.setFunctionParameters(channelItem, parameters, createToken(COMMA_TOKEN),
                querySchema, headerSchema);
        ballerinaAuthConfigGenerator.getConfigParamForClassInit(serverURL, parameters);
        SeparatedNodeList<ParameterNode> parameterList = createSeparatedNodeList(parameters);
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
     * @return {@link MetadataNode} Metadata node containing entire function documentation comment.
     */
    private MetadataNode getInitDocComment() {

        List<Node> docs = new ArrayList<>();
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
        MarkdownParameterDocumentationLineNode clientConfig = DocCommentsGenerator.createAPIParamDoc(
                "config",
                "The configurations to be used when initializing the `connector`");
        docs.add(clientConfig);
        MarkdownParameterDocumentationLineNode serviceUrlAPI = DocCommentsGenerator.createAPIParamDoc(
                "serviceUrl",
                "URL of the target service");
        docs.add(serviceUrlAPI);
        MarkdownParameterDocumentationLineNode returnDoc = DocCommentsGenerator.createAPIParamDoc("return",
                "An error if connector initialization failed");
        docs.add(returnDoc);

        if (ballerinaAuthConfigGenerator.isPathParam()) {
            MarkdownParameterDocumentationLineNode pathParamDocNode = DocCommentsGenerator.createAPIParamDoc(
                    "pathParams", "path parameters");
            docs.add(pathParamDocNode);
        }
        if (ballerinaAuthConfigGenerator.isQueryParam()) {
            MarkdownParameterDocumentationLineNode queryParamDocNode = DocCommentsGenerator.createAPIParamDoc(
                    "queryParams", "query parameters");
            docs.add(queryParamDocNode);
        }
        if (ballerinaAuthConfigGenerator.isHeaderParam()) {
            MarkdownParameterDocumentationLineNode headerParamDocNode = DocCommentsGenerator.createAPIParamDoc(
                    "headerParams", "header parameters");
            docs.add(headerParamDocNode);
        }
        MarkdownDocumentationNode clientInitDoc = createMarkdownDocumentationNode(createNodeList(docs));
        return createMetadataNode(clientInitDoc, createEmptyNodeList());
    }


    /**
     * Provide client class init function's documentation including function description and parameter descriptions.
     *
     * @return {@link MetadataNode} Metadata node containing entire function documentation comment.
     */
    private MetadataNode getDocCommentsForWorker(String functionType, String comment) {

        List<Node> docs = new ArrayList<>();
//        String clientInitDocComment = "Gets invoked to initialize the `connector`.\n";
        Map<String, JsonNode> extensions = ((AsyncApi25InfoImpl) asyncAPI.getInfo()).getExtensions();
        if (extensions != null && !extensions.isEmpty()) {
            for (Map.Entry<String, JsonNode> extension : extensions.entrySet()) {
                if (extension.getKey().trim().equals(functionType)) {
                    comment = comment.concat(extension.getValue().toString());
                    break;
                }
            }
        }
        //todo: setInitDocComment() pass the references
        docs.addAll(DocCommentsGenerator.createAPIDescriptionDoc(comment, true));
        MarkdownDocumentationNode workerDoc = createMarkdownDocumentationNode(createNodeList(docs));
        return createMetadataNode(workerDoc, createEmptyNodeList());
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
        ArrayList<Token> prefixTokens = new ArrayList<>();
        prefixTokens.add(privateKeywordToken);
        prefixTokens.add(finalKeywordToken);
        NodeList<Token> qualifierList = createNodeList(prefixTokens);

        //private final websocket:Client clientEp;
        QualifiedNameReferenceNode typeName = createQualifiedNameReferenceNode(createIdentifierToken(WEBSOCKET),
                createToken(COLON_TOKEN), createIdentifierToken(GeneratorConstants.CLIENT_CLASS_NAME));
        IdentifierToken fieldName = createIdentifierToken(GeneratorConstants.CLIENT_EP);
        MetadataNode metadataNode = createMetadataNode(null, createEmptyNodeList());
        ObjectFieldNode websocketClientField = createObjectFieldNode(metadataNode, null,
                qualifierList, typeName, fieldName, null, null, createToken(SEMICOLON_TOKEN));

        //private final pipe:Pipe writeMessageQueue;
        QualifiedNameReferenceNode pipeTypeName = createQualifiedNameReferenceNode(createIdentifierToken(SIMPLE_PIPE),
                createToken(COLON_TOKEN), createIdentifierToken(GeneratorConstants.CAPITAL_PIPE));
        IdentifierToken writeMessageQueueFieldName = createIdentifierToken(GeneratorConstants.WRITE_MESSAGE_QUEUE);
        MetadataNode writeMessageQueuemetadataNode = createMetadataNode(null, createEmptyNodeList());
        ObjectFieldNode writeMessageQueueClientField = createObjectFieldNode(writeMessageQueuemetadataNode,
                null,
                qualifierList, pipeTypeName, writeMessageQueueFieldName, null, null,
                createToken(SEMICOLON_TOKEN));

        //private final pipe:Pipe readMessageQueue;
        IdentifierToken readMessageQueueFieldName = createIdentifierToken(GeneratorConstants.READ_MESSAGE_QUEUE);
        MetadataNode readMessageQueuemetadataNode = createMetadataNode(null, createEmptyNodeList());
        ObjectFieldNode readMessageQueueClientField = createObjectFieldNode(readMessageQueuemetadataNode,
                null,
                qualifierList, pipeTypeName, readMessageQueueFieldName, null, null,
                createToken(SEMICOLON_TOKEN));


        //private final map<pipe:Pipe> pipes;
        TypeParameterNode pipesTypeParamsNode = createTypeParameterNode(createToken(LT_TOKEN),
                pipeTypeName, createToken(GT_TOKEN));
//        MapTypeDescriptorNode pipesTypeName = createMapTypeDescriptorNode(createToken(MAP_KEYWORD),
//                pipesTypeParamsNode);
        SimpleNameReferenceNode pipesTypeName = createSimpleNameReferenceNode(createIdentifierToken("PipesMap"));
//        MetadataNode customHeadersMetadata = getMetadataNode("Custom headers, " +
//                "which should be sent to the server");
        IdentifierToken pipesFieldName = createIdentifierToken(GeneratorConstants.PIPES);
        MetadataNode pipesMetadataNode = createMetadataNode(null, createEmptyNodeList());
        ObjectFieldNode pipesField = createObjectFieldNode(pipesMetadataNode, null,
                qualifierList, pipesTypeName, pipesFieldName, null, null,
                createToken(SEMICOLON_TOKEN));


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
     * Generate remote functions for AsyncAPI messages.
     * <p>
     * //     * @param paths  asyncAPI Paths
     *
     * @return FunctionDefinitionNodes list
     * @throws BallerinaAsyncApiException - throws when creating remote functions fails
     */
    private List<FunctionDefinitionNode> createRemoteFunctions(Map<String, AsyncApiMessage> messages,
                                                               List<MatchClauseNode> matchStatementList,
                                                               String dispatcherStreamId, String dispatcherKey,
                                                               List<String> idMethods)
            throws BallerinaAsyncApiException {


        List<FunctionDefinitionNode> functionDefinitionNodeList = new ArrayList<>();
        Set<Map.Entry<String, AsyncApiMessage>> messageItems = messages.entrySet();

        //Take all schemas
        Map<String, Schema> schemas = asyncAPI.getComponents().getSchemas();

        // Create an array to store all request messages
        ArrayList requestMessages = new ArrayList();
        if (schemas.containsKey("Error")) {
            AsyncApi25MessageImpl errorMessage = new AsyncApi25MessageImpl();
            ObjectNode objectNode = new ObjectNode(JsonNodeFactory.instance);
            objectNode.put("$ref", "#/components/messages/Error");
            errorMessage.addExtension(X_RESPONSE, objectNode);
            errorMessage.addExtension(X_RESPONSE_TYPE, new TextNode(SIMPLE_RPC));
            messages.put("Error", errorMessage);
        }


        for (Map.Entry<String, AsyncApiMessage> messageItem : messageItems) {
            Map<String, JsonNode> extensions = ((AsyncApi25MessageImpl) messageItem.getValue()).getExtensions();
            if (extensions != null && extensions.get(X_RESPONSE) != null) {
                AsyncApi25MessageImpl messageValue = (AsyncApi25MessageImpl) messageItem.getValue();
                String messageName = messageItem.getKey();
                AsyncApi25SchemaImpl schema = (AsyncApi25SchemaImpl) schemas.get(messageName);
                Map<String, Schema> properties = schema.getProperties();
                if (properties.containsKey(dispatcherKey)) {
                    if (properties.get(dispatcherKey).getType().equals("string")) {
                        FunctionDefinitionNode functionDefinitionNode =
                                getClientMethodFunctionDefinitionNode(messageName, messageValue, extensions, schemas,
                                        matchStatementList, dispatcherStreamId,idMethods);
                        functionDefinitionNodeList.add(functionDefinitionNode);
                        requestMessages.add(messageItem.getKey());
                    } else {
                        throw new BallerinaAsyncApiException(
                                String.format("dispatcherKey type must be string in %s schema",messageName));
                    }
                } else {
                    throw new BallerinaAsyncApiException(String.format("%s schema must contain dispatcherKey \"%s\"",
                            messageName,dispatcherKey));
                }
            }
        }

        List<AsyncApiMessage> publishMessages = asyncAPI.getChannels().getItems().get(0).getPublish().
                getMessage().getOneOf();
        if (publishMessages != null) {
            ListIterator<AsyncApiMessage> remainingPublishMessages = publishMessages.listIterator();
            for (ListIterator<AsyncApiMessage> it = remainingPublishMessages; it.hasNext(); ) {
                AsyncApi25MessageImpl message = (AsyncApi25MessageImpl) it.next();
                String reference = message.get$ref();
                String type = GeneratorUtils.extractReferenceType(reference);
                if (!requestMessages.contains(type)) {
                    AsyncApi25SchemaImpl schema = (AsyncApi25SchemaImpl) schemas.get(type);
                    Map<String, Schema> properties = schema.getProperties();
                    if (properties.containsKey(dispatcherKey)) {
                        if (properties.get(dispatcherKey).getType().equals("string")) {
                            FunctionDefinitionNode functionDefinitionNode =
                                    getClientMethodFunctionDefinitionNode(type,
                                            (AsyncApi25MessageImpl) messages.get(type),
                                            null,
                                            schemas, null, dispatcherStreamId,idMethods);
                            functionDefinitionNodeList.add(functionDefinitionNode);
                        } else {
                            throw new BallerinaAsyncApiException("dispatcherKey type must be string");
                        }
                    } else {
                        throw new BallerinaAsyncApiException(String.format("Schema %s must contain dispatcherKey",
                                type));
                    }
                }

            }
        }
        return functionDefinitionNodeList;
    }


    /**
     * Generate function definition node.
     * <pre>
     *     remote isolated function pathParameter(int 'version, string name) returns string|error {
     *          string  path = string `/v1/${'version}/v2/${name}`;
     *          string response = check self.clientEp-> get(path);
     *          return response;
     *    }
     *    or
     *     resource isolated function get v1/[string 'version]/v2/[sting name]() returns string|error {
     *         string  path = string `/v1/${'version}/v2/${name}`;
     *         string response = check self.clientEp-> get(path);
     *         return response;
     *     }
     * </pre>
     */
    private FunctionDefinitionNode getClientMethodFunctionDefinitionNode(String messageName,
                                                                         AsyncApi25MessageImpl messageValue,
                                                                         Map<String, JsonNode> extensions,
                                                                         Map<String, Schema> schemas,
                                                                         List<MatchClauseNode> matchStatementList,
                                                                         String dispatcherStreamId,
                                                                         List<String> idMethods)
            throws BallerinaAsyncApiException {
        // Create api doc for function

        List<Node> remoteFunctionDocs = new ArrayList<>();


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

        RemoteFunctionSignatureGenerator remoteFunctionSignatureGenerator = new
                RemoteFunctionSignatureGenerator(asyncAPI, ballerinaSchemaGenerator, typeDefinitionNodeList);
        FunctionSignatureNode functionSignatureNode =
                remoteFunctionSignatureGenerator.getFunctionSignatureNode(messageValue.getPayload(),
                        remoteFunctionDocs, extensions);
        typeDefinitionNodeList = remoteFunctionSignatureGenerator.getTypeDefinitionNodeList();
        // Create metadataNode add documentation string

        MetadataNode metadataNode = createMetadataNode(createMarkdownDocumentationNode(
                createNodeList(remoteFunctionDocs)), createNodeList());

        // Create Function Body
        RemoteFunctionBodyGenerator remoteFunctionBodyGenerator = new RemoteFunctionBodyGenerator(imports,
                asyncAPI, utilGenerator);

        AsyncApi25SchemaImpl schema = (AsyncApi25SchemaImpl) schemas.get(messageName);
        Map<String, Schema> properties=  schema.getProperties();

        //Check if the schema has dispatcherStreamId
        if (dispatcherStreamId==null|| (dispatcherStreamId!=null && !properties.containsKey(dispatcherStreamId))) {
            //If no dispatcherStreamId found
            dispatcherStreamId = null;

        } else {
            if(properties.get(dispatcherStreamId).getType().equals("string")) {
                //If found at least one dispatcherStreamId
                idMethods.add(messageName);
                ImportDeclarationNode importForUUID = GeneratorUtils.getImportDeclarationNode(GeneratorConstants.BALLERINA
                        , UUID);
//                if (!imports.get(imports.size() - 1).moduleName().get(0).text().equals(UUID)) {
//                    imports.add(importForUUID);
//                }
                if (idMethods.size()==1) {
                    imports.add(importForUUID);
                }
            }else{
                throw new BallerinaAsyncApiException("dispatcherStreamId must be a string");
            }


        }
//       boolean isDispatcherStreamId=schema.getProperties().containsKey(dispatcherStreamId);
        char requestTypeFirstChar = Character.toLowerCase(messageName.charAt(0)); // Lowercase the first character
        String requestRemainingString = messageName.substring(1);
        String requestTypeCamelCaseName = requestTypeFirstChar + requestRemainingString;
        FunctionBodyNode functionBodyNode = remoteFunctionBodyGenerator.getFunctionBodyNode(extensions,
                requestTypeCamelCaseName, dispatcherStreamId, matchStatementList);

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
        if (servers != null) {
            List<AsyncApiServer> serversList = servers.getItems();
            AsyncApi25ServerImpl selectedServer = (AsyncApi25ServerImpl) serversList.get(0);
            if (!selectedServer.getUrl().startsWith("wss:") && servers.getItems().size() > 1) {
                for (AsyncApiServer server : serversList) {
                    if (server.getUrl().startsWith("wss:")) {
                        selectedServer = (AsyncApi25ServerImpl) server;
                        break;
                    }
                }
            }
            if (selectedServer.getUrl() == null) {
                serverURL = "ws://localhost:9090/v1";
            } else if (selectedServer.getVariables() != null) {
                Map<String, ServerVariable> variables = selectedServer.getVariables();
                URI url;
                String resolvedUrl = GeneratorUtils.buildUrl(selectedServer.getUrl(), variables);

                try {
                    url = new URI(resolvedUrl);
                } catch (URISyntaxException e) {
                    throw new BallerinaAsyncApiException("Failed to read endpoint details of the server: " +
                            selectedServer.getUrl(), e);
                }
                serverURL = url.toString();


//                serverURL = url.toString();
                serverURL = resolvedUrl;
            } else {
                serverURL = selectedServer.getUrl();
            }
            return serverURL;
        } else {
            serverURL = "/";
            return serverURL;
        }
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
