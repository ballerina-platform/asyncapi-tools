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
import com.fasterxml.jackson.databind.node.BooleanNode;
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
import io.ballerina.compiler.syntax.tree.LockStatementNode;
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
import io.ballerina.compiler.syntax.tree.TypedBindingPatternNode;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocuments;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.ballerina.asyncapi.core.GeneratorConstants.API_KEY_CONFIG;
import static io.ballerina.asyncapi.core.GeneratorConstants.BALLERINA_CLIENT_CANNOT_BE_GENERATED;
import static io.ballerina.asyncapi.core.GeneratorConstants.BALLERINA_WEBSOCKET_DOESNT_SUPPORT_FOR_MULTIPLE_CHANNELS;
import static io.ballerina.asyncapi.core.GeneratorConstants.CAPITAL_PIPE;
import static io.ballerina.asyncapi.core.GeneratorConstants.CHECK_PATH_FOR_QUERY_PARAM;
import static io.ballerina.asyncapi.core.GeneratorConstants.CLIENT_CLASS_NAME;
import static io.ballerina.asyncapi.core.GeneratorConstants.CLIENT_CONFIG_CUSTOM_HEADERS;
import static io.ballerina.asyncapi.core.GeneratorConstants.CLIENT_EP;
import static io.ballerina.asyncapi.core.GeneratorConstants.CLONE_WITH_TYPE;
import static io.ballerina.asyncapi.core.GeneratorConstants.CLOSE;
import static io.ballerina.asyncapi.core.GeneratorConstants.COLON;
import static io.ballerina.asyncapi.core.GeneratorConstants.CONFIG;
import static io.ballerina.asyncapi.core.GeneratorConstants.CONFIG_DESCRIPTION;
import static io.ballerina.asyncapi.core.GeneratorConstants.CONNECTION_CLOSE;
import static io.ballerina.asyncapi.core.GeneratorConstants.CONSUME;
import static io.ballerina.asyncapi.core.GeneratorConstants.CUSTOM_HEADERS;
import static io.ballerina.asyncapi.core.GeneratorConstants.DEFAULT_API_KEY_DESC;
import static io.ballerina.asyncapi.core.GeneratorConstants.DEFAULT_PIPE_TIME_OUT;
import static io.ballerina.asyncapi.core.GeneratorConstants.DEFAULT_URL;
import static io.ballerina.asyncapi.core.GeneratorConstants.DOT;
import static io.ballerina.asyncapi.core.GeneratorConstants.DOT_TO_STRING;
import static io.ballerina.asyncapi.core.GeneratorConstants.DOUBLE_QUOTE;
import static io.ballerina.asyncapi.core.GeneratorConstants.ENSURE_TYPE;
import static io.ballerina.asyncapi.core.GeneratorConstants.EQUAL_SPACE;
import static io.ballerina.asyncapi.core.GeneratorConstants.FAIL_TO_READ_ENDPOINT_DETAILS;
import static io.ballerina.asyncapi.core.GeneratorConstants.GET_COMBINE_HEADERS;
import static io.ballerina.asyncapi.core.GeneratorConstants.GET_PIPE;
import static io.ballerina.asyncapi.core.GeneratorConstants.GRACEFUL_CLOSE;
import static io.ballerina.asyncapi.core.GeneratorConstants.HAS_KEY;
import static io.ballerina.asyncapi.core.GeneratorConstants.HEADER_PARAM;
import static io.ballerina.asyncapi.core.GeneratorConstants.HEADER_PARAMETERS;
import static io.ballerina.asyncapi.core.GeneratorConstants.HEADER_PARAMS;
import static io.ballerina.asyncapi.core.GeneratorConstants.IMMEDIATE_CLOSE;
import static io.ballerina.asyncapi.core.GeneratorConstants.INIT;
import static io.ballerina.asyncapi.core.GeneratorConstants.IS_MESSAGE_READING;
import static io.ballerina.asyncapi.core.GeneratorConstants.IS_MESSAGE_WRITING;
import static io.ballerina.asyncapi.core.GeneratorConstants.IS_PIPE_TRIGGERING;
import static io.ballerina.asyncapi.core.GeneratorConstants.LANG_RUNTIME;
import static io.ballerina.asyncapi.core.GeneratorConstants.MAP_ANY_DATA;
import static io.ballerina.asyncapi.core.GeneratorConstants.MAP_STRING;
import static io.ballerina.asyncapi.core.GeneratorConstants.MESSAGE;
import static io.ballerina.asyncapi.core.GeneratorConstants.MESSAGE_VAR_NAME;
import static io.ballerina.asyncapi.core.GeneratorConstants.MESSAGE_WITH_ID;
import static io.ballerina.asyncapi.core.GeneratorConstants.MESSAGE_WITH_ID_VAR_NAME;
import static io.ballerina.asyncapi.core.GeneratorConstants.MODIFIED_URL;
import static io.ballerina.asyncapi.core.GeneratorConstants.NOT_IS;
import static io.ballerina.asyncapi.core.GeneratorConstants.NULL_VALUE;
import static io.ballerina.asyncapi.core.GeneratorConstants.OBJECT;
import static io.ballerina.asyncapi.core.GeneratorConstants.OPTIONAL_ERROR;
import static io.ballerina.asyncapi.core.GeneratorConstants.PATH_PARAMETERS;
import static io.ballerina.asyncapi.core.GeneratorConstants.PATH_PARAMS;
import static io.ballerina.asyncapi.core.GeneratorConstants.PIPES;
import static io.ballerina.asyncapi.core.GeneratorConstants.PIPE_TRIGGER;
import static io.ballerina.asyncapi.core.GeneratorConstants.PLUS_SPACE;
import static io.ballerina.asyncapi.core.GeneratorConstants.PRODUCE;
import static io.ballerina.asyncapi.core.GeneratorConstants.QUERY_PARAM;
import static io.ballerina.asyncapi.core.GeneratorConstants.QUERY_PARAMETERS;
import static io.ballerina.asyncapi.core.GeneratorConstants.QUERY_PARAMS;
import static io.ballerina.asyncapi.core.GeneratorConstants.QUEUE_DEFAULT_SIZE;
import static io.ballerina.asyncapi.core.GeneratorConstants.READ_MESSAGE;
import static io.ballerina.asyncapi.core.GeneratorConstants.READ_MESSAGE_QUEUE;
import static io.ballerina.asyncapi.core.GeneratorConstants.READ_ONLY;
import static io.ballerina.asyncapi.core.GeneratorConstants.REF;
import static io.ballerina.asyncapi.core.GeneratorConstants.REMOTE_METHOD_NAME_PREFIX;
import static io.ballerina.asyncapi.core.GeneratorConstants.REMOVE_PIPES;
import static io.ballerina.asyncapi.core.GeneratorConstants.REMOVE_STREAM_GENERATORS;
import static io.ballerina.asyncapi.core.GeneratorConstants.REQUEST_MESSAGE;
import static io.ballerina.asyncapi.core.GeneratorConstants.RETURN;
import static io.ballerina.asyncapi.core.GeneratorConstants.RETURN_DESCRIPTION;
import static io.ballerina.asyncapi.core.GeneratorConstants.RUNTIME;
import static io.ballerina.asyncapi.core.GeneratorConstants.SELF;
import static io.ballerina.asyncapi.core.GeneratorConstants.SERVICE_URL;
import static io.ballerina.asyncapi.core.GeneratorConstants.SERVICE_URL_DESCRIPTION;
import static io.ballerina.asyncapi.core.GeneratorConstants.SIMPLE_PIPE;
import static io.ballerina.asyncapi.core.GeneratorConstants.SIMPLE_RPC;
import static io.ballerina.asyncapi.core.GeneratorConstants.SLEEP;
import static io.ballerina.asyncapi.core.GeneratorConstants.SPACE;
import static io.ballerina.asyncapi.core.GeneratorConstants.START_MESSAGE_READING;
import static io.ballerina.asyncapi.core.GeneratorConstants.START_MESSAGE_READING_DESCRIPTION;
import static io.ballerina.asyncapi.core.GeneratorConstants.START_MESSAGE_WRITING;
import static io.ballerina.asyncapi.core.GeneratorConstants.START_MESSAGE_WRITING_DESCRIPTION;
import static io.ballerina.asyncapi.core.GeneratorConstants.START_PIPE_TRIGGERING;
import static io.ballerina.asyncapi.core.GeneratorConstants.START_PIPE_TRIGGERING_DESCRIPTION;
import static io.ballerina.asyncapi.core.GeneratorConstants.STREAM_GENERATORS;
import static io.ballerina.asyncapi.core.GeneratorConstants.STRING;
import static io.ballerina.asyncapi.core.GeneratorConstants.S_DOT;
import static io.ballerina.asyncapi.core.GeneratorConstants.UUID;
import static io.ballerina.asyncapi.core.GeneratorConstants.WEBSOCKET;
import static io.ballerina.asyncapi.core.GeneratorConstants.WEBSOCKET_EP;
import static io.ballerina.asyncapi.core.GeneratorConstants.WORKER_SLEEP_TIME_OUT;
import static io.ballerina.asyncapi.core.GeneratorConstants.WRITE_MESSAGE;
import static io.ballerina.asyncapi.core.GeneratorConstants.WRITE_MESSAGE_QUEUE;
import static io.ballerina.asyncapi.core.GeneratorConstants.WSS;
import static io.ballerina.asyncapi.core.GeneratorConstants.XLIBB_PIPE;
import static io.ballerina.asyncapi.core.GeneratorConstants.X_BALLERINA_INIT_DESCRIPTION;
import static io.ballerina.asyncapi.core.GeneratorConstants.X_BALLERINA_MESSAGE_READ_DESCRIPTION;
import static io.ballerina.asyncapi.core.GeneratorConstants.X_BALLERINA_MESSAGE_WRITE_DESCRIPTION;
import static io.ballerina.asyncapi.core.GeneratorConstants.X_BALLERINA_PIPE_TRIGGER_DESCRIPTION;
import static io.ballerina.asyncapi.core.GeneratorConstants.X_DISPATCHER_KEY;
import static io.ballerina.asyncapi.core.GeneratorConstants.X_DISPATCHER_KEY_CANNOT_BE_EMPTY;
import static io.ballerina.asyncapi.core.GeneratorConstants.X_DISPATCHER_KEY_MUST_INCLUDE_IN_THE_SPECIFICATION;
import static io.ballerina.asyncapi.core.GeneratorConstants.X_DISPATCHER_STREAM_ID;
import static io.ballerina.asyncapi.core.GeneratorConstants.X_DISPATCHER_STREAM_ID_CANNOT_BE_EMPTY;
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
import static io.ballerina.compiler.syntax.tree.NodeFactory.createLockStatementNode;
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
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.COLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.COMMA_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.DOT_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ELSE_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.EOF_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.EQUAL_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ERROR_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FALSE_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FINAL_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FUNCTION_CALL;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FUNCTION_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.IF_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ISOLATED_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.LOCK_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.MATCH_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.NEW_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_TOKEN;
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
 */
public class IntermediateClientGenerator {

    private final AsyncApi25DocumentImpl asyncAPI;
    private final List<String> remoteFunctionNameList;
    private final BallerinaAuthConfigGenerator ballerinaAuthConfigGenerator;
    private final List<ImportDeclarationNode> imports;
    private final BallerinaTypesGenerator ballerinaSchemaGenerator;
    private final RemoteFunctionReturnTypeGenerator functionReturnType;
    private UtilGenerator utilGenerator;
    private List<TypeDefinitionNode> typeDefinitionNodeList;
    private List<String> apiKeyNameList = new ArrayList<>();
    private String serverURL;
    private String clientName = null;

    public IntermediateClientGenerator(AASClientConfig asyncAPIClientConfig) {

        this.imports = new ArrayList<>();
        this.typeDefinitionNodeList = new ArrayList<>();
        this.asyncAPI = asyncAPIClientConfig.getAsyncAPI();
        this.utilGenerator = null;
        this.remoteFunctionNameList = new ArrayList<>();
        this.ballerinaSchemaGenerator = new BallerinaTypesGenerator(asyncAPI, new LinkedList<>());
        this.serverURL = "/";
        this.ballerinaAuthConfigGenerator = new BallerinaAuthConfigGenerator(false, false,
                ballerinaSchemaGenerator);
        this.functionReturnType = new RemoteFunctionReturnTypeGenerator(this.asyncAPI);

    }

    private static ObjectFieldNode getObjectFieldNode(NodeList<Token> qualifiers,
                                                      Node typeNode,
                                                      String fieldIdentifier) {

        IdentifierToken fieldName = createIdentifierToken(fieldIdentifier);
        MetadataNode metadataNode = createMetadataNode(null, createEmptyNodeList());
        ObjectFieldNode field = createObjectFieldNode(metadataNode, null,
                qualifiers, typeNode, fieldName,
                null, null, createToken(SEMICOLON_TOKEN));
        return field;
    }

    public void setUtilGenerator(UtilGenerator utilGenerator) {
        this.utilGenerator = utilGenerator;
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

    //Get remote function name list to generate testing functions
    public List<String> getRemoteFunctionNameList() {

        return remoteFunctionNameList;
    }

    //Get clientName to generate testings
    public String getClientName() {
        return this.clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
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
        ImportDeclarationNode importForXlibbPipe = GeneratorUtils.getImportDeclarationNode(GeneratorConstants.XLIBB
                , XLIBB_PIPE);
        ImportDeclarationNode importForRunTime = GeneratorUtils.getImportDeclarationNode(GeneratorConstants.BALLERINA
                , LANG_RUNTIME);

        imports.add(importForWebsocket);
        imports.add(importForXlibbPipe);
        imports.add(importForRunTime);


        //TODO: This has to improve
        // Add authentication related records
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


    /**
     * Generate Class definition Node with below code structure.
     * <pre>
     *
        public client isolated class PayloadVClient {
            private final websocket:Client clientEp;
            private final pipe:Pipe writeMessageQueue;
            private final pipe:Pipe readMessageQueue;
            private final PipesMap pipes;
            private final StreamGeneratorsMap streamGenerators;
            private boolean isMessageWriting;
            private boolean isMessageReading;
            private boolean isPipeTriggering;
            private pipe:Pipe? pingMessagePipe;
            private pipe:Pipe? connectionInitMessagePipe;
            # Gets invoked to initialize the `connector`.
            #
            # + config - The configurations to be used when initializing the `connector`
            # + serviceUrl - URL of the target service
            # + return - An error if connector initialization failed
            public isolated function init(websocket:ClientConfiguration clientConfig =  {}, string serviceUrl = "ws://localhost:9090/payloadV") returns error? {
                self.pipes = new ();
                self.streamGenerators = new ();
                self.writeMessageQueue = new (1000);
                self.readMessageQueue = new (1000);
                websocket:Client websocketEp = check new (serviceUrl, clientConfig);
                self.clientEp = websocketEp;
                self.pingMessagePipe = ();
                self.connectionInitMessagePipe = ();
                self.isMessageWriting = true;
                self.isMessageReading = true;
                self.isPipeTriggering = true;
                self.startMessageWriting();
                self.startMessageReading();
                self.startPipeTriggering();
                return;
            }
            # Use to write messages to the websocket.
            #
            private isolated function startMessageWriting() {
                worker writeMessage returns error? {
                    while self.isMessageWriting {
                        anydata requestMessage = check self.writeMessageQueue.consume(5);
                        check self.clientEp->writeMessage(requestMessage);
                        runtime:sleep(0.01);
                    }
                }
            }
            # Use to read messages from the websocket.
            #
            private isolated function startMessageReading() {
                worker readMessage returns error? {
                    while self.isMessageReading {
                        Message message = check self.clientEp->readMessage();
                        check self.readMessageQueue.produce(message, 5);
                        runtime:sleep(0.01);
                    }
                }
            }
            # Use to map received message responses into relevant requests.
            #
            private isolated function startPipeTriggering() {
                worker pipeTrigger returns error? {
                    while self.isPipeTriggering {
                        Message message = check self.readMessageQueue.consume(5);
                        if message.hasKey("id") {
                            MessageWithId messageWithId = check message.cloneWithType();
                            string id = messageWithId.id;
                            pipe:Pipe idPipe = self.pipes.getPipe(id);
                            check idPipe.produce(messageWithId, 5);
                        } else {
                            string 'type = message.'type;
                            match ('type) {
                                "PongMessage" => {
                                    pipe:Pipe pingMessagePipe = self.pipes.getPipe("pingMessage");
                                    check pingMessagePipe.produce(message, 5);
                                }
                                "ConnectionAckMessage" => {
                                    pipe:Pipe connectionInitMessagePipe = self.pipes.getPipe("connectionInitMessage");
                                    check connectionInitMessagePipe.produce(message, 5);
                                }
                            }
                        }
                    }
                }
            }
            #
            remote isolated function doSubscribeMessage(SubscribeMessage subscribeMessage, decimal timeout) returns stream<NextMessage|CompleteMessage,error?>|error {
                if self.writeMessageQueue.isClosed() {
                    return error("connection closed");
                }
                pipe:Pipe subscribeMessagePipe = new (10000);
                string id;
                lock {
                    id = uuid:createType1AsString();
                    subscribeMessage.id = id;
                }
                self.pipes.addPipe(id, subscribeMessagePipe);
                Message message = check subscribeMessage.cloneWithType();
                check self.writeMessageQueue.produce(message, timeout);
                stream<NextMessage|CompleteMessage,error?> streamMessages;
                lock {
                    NextMessageCompleteMessageStreamGenerator streamGenerator = check new (subscribeMessagePipe, timeout);
                    self.streamGenerators.addStreamGenerator(streamGenerator);
                    streamMessages = new (streamGenerator);
                }
                return streamMessages;
            }
            #
            remote isolated function doPingMessage(PingMessage pingMessage, decimal timeout) returns PongMessage|error {
                if self.writeMessageQueue.isClosed() {
                    return error("connection closed");
                }
                pipe:Pipe pingMessagePipe;
                lock {
                    self.pingMessagePipe = self.pipes.getPipe("pingMessage");
                }
                Message message = check pingMessage.cloneWithType();
                check self.writeMessageQueue.produce(message, timeout);
                lock {
                    pingMessagePipe = check self.pingMessagePipe.ensureType();
                }
                anydata responseMessage = check pingMessagePipe.consume(timeout);
                PongMessage pongMessage = check responseMessage.cloneWithType();
                return pongMessage;
            }
            #
            remote isolated function doPongMessage(PongMessage pongMessage, decimal timeout) returns error? {
                if self.writeMessageQueue.isClosed() {
                    return error("connection closed");
                }
                Message message = check pongMessage.cloneWithType();
                check self.writeMessageQueue.produce(message, timeout);
            }
            #
            remote isolated function doConnectionInitMessage(ConnectionInitMessage connectionInitMessage, decimal timeout) returns ConnectionAckMessage|error {
                if self.writeMessageQueue.isClosed() {
                    return error("connection closed");
                }
                pipe:Pipe connectionInitMessagePipe;
                lock {
                    self.connectionInitMessagePipe = self.pipes.getPipe("connectionInitMessage");
                }
                Message message = check connectionInitMessage.cloneWithType();
                check self.writeMessageQueue.produce(message, timeout);
                lock {
                    connectionInitMessagePipe = check self.connectionInitMessagePipe.ensureType();
                }
                anydata responseMessage = check connectionInitMessagePipe.consume(timeout);
                ConnectionAckMessage connectionAckMessage = check responseMessage.cloneWithType();
                return connectionAckMessage;
            }
            #
            remote isolated function doCompleteMessage(CompleteMessage completeMessage, decimal timeout) returns error? {
                if self.writeMessageQueue.isClosed() {
                    return error("connection closed");
                }
                Message message = check completeMessage.cloneWithType();
                check self.writeMessageQueue.produce(message, timeout);
            }
            remote isolated function closePingMessagePipe() returns error? {
                lock {
                    if self.pingMessagePipe !is() {
                        pipe:Pipe pingMessagePipe = check self.pingMessagePipe.ensureType();
                        check pingMessagePipe.gracefulClose();
                    }
                }
            };
            remote isolated function closeConnectionInitMessagePipe() returns error? {
                lock {
                    if self.connectionInitMessagePipe !is() {
                        pipe:Pipe connectionInitMessagePipe = check self.connectionInitMessagePipe.ensureType();
                        check connectionInitMessagePipe.gracefulClose();
                    }
                }
            };
            remote isolated function connectionClose() returns error? {
                lock {
                    self.isMessageReading = false;
                    self.isMessageWriting = false;
                    self.isPipeTriggering = false;
                    check self.writeMessageQueue.immediateClose();
                    check self.readMessageQueue.immediateClose();
                    check self.pipes.removePipes();
                    check self.streamGenerators.removeStreamGenerators();
                    check self.clientEp->close();
                }
            };
        }
     * </pre>
     */
    private ClassDefinitionNode getClassDefinitionNode() throws BallerinaAsyncApiException {

        //Get dispatcherKey
        Map<String, JsonNode> extensions = asyncAPI.getExtensions();

        if (extensions == null || extensions.get(X_DISPATCHER_KEY) == null) {
            throw new BallerinaAsyncApiException(X_DISPATCHER_KEY_MUST_INCLUDE_IN_THE_SPECIFICATION);
        }
        TextNode dispatcherKeyNode = (TextNode) extensions.get(X_DISPATCHER_KEY);

        String dispatcherKey = dispatcherKeyNode.asText();
        if (dispatcherKey.equals("")) {
            throw new BallerinaAsyncApiException(X_DISPATCHER_KEY_CANNOT_BE_EMPTY);
        }

        //Get dispatcherStreamId
        String dispatcherStreamId = null;
        if (extensions.get(X_DISPATCHER_STREAM_ID) != null) {
            TextNode dispatcherStreamIdNode = (TextNode) extensions.get(X_DISPATCHER_STREAM_ID);
            if (dispatcherStreamIdNode != null) {
                dispatcherStreamId = extensions.get(X_DISPATCHER_STREAM_ID).asText();
                if (dispatcherStreamId.equals("")) {
                    throw new BallerinaAsyncApiException(X_DISPATCHER_STREAM_ID_CANNOT_BE_EMPTY);
                }
            }
        }


        //Create a list to collect match statements when dispatcherStreamId is absent in that schema
        List<MatchClauseNode> matchStatementList = new ArrayList<>();

        // Adding remote functions which is using id pipes
        List<String> pipeIdMethods = new ArrayList<>();

        // Adding remote functions which is using name pipes
        ArrayList<String> pipeNameMethods = new ArrayList<>();

        ArrayList<String> streamReturns = new ArrayList<>();

        List<FunctionDefinitionNode> remoteFunctionNodes = createRemoteFunctions(streamReturns, matchStatementList,
                pipeIdMethods, pipeNameMethods);


        if (pipeIdMethods.size() == 0 && pipeNameMethods.size() == 0) {
            throw new BallerinaAsyncApiException(BALLERINA_CLIENT_CANNOT_BE_GENERATED);
        }

        // Collect members for class definition node
        List<Node> memberNodeList = new ArrayList<>();

        boolean isStreamPresent = streamReturns.size() > 0;

        // Add instance variable to class definition node
        memberNodeList.addAll(createClassInstanceVariables(pipeNameMethods, isStreamPresent));

        // Add init function to class definition node
        memberNodeList.add(createInitFunction(pipeNameMethods, isStreamPresent));

        // Add startInterMediator function
        memberNodeList.add(createStartMessageWriting());

        // Add startMessageReading function
        memberNodeList.add(createStartMessageReading());

        // Add startPipeTriggering function
        memberNodeList.add(createStartPipeTriggering(matchStatementList, pipeIdMethods));

        // Add remoteFunctionNodes
        memberNodeList.addAll(remoteFunctionNodes);

        // Generate the class combining members
        MetadataNode metadataNode = getClassMetadataNode();

        //Get title name from the specification
        String titleName = asyncAPI.getInfo().getTitle().trim().replaceAll("\\s", "");

        //Get channel name from the specification
        String channelName = GeneratorUtils.
                removeNonAlphanumeric(asyncAPI.getChannels().getItemNames().get(0).trim());

        //Combine class name as titleName+channelName+Client
        String stringClassName = titleName + channelName + CLIENT_CLASS_NAME;


        setClientName(stringClassName);
        IdentifierToken className = createIdentifierToken(stringClassName);
        NodeList<Token> classTypeQualifiers = createNodeList(createToken(CLIENT_KEYWORD),
                createToken(ISOLATED_KEYWORD));

        return createClassDefinitionNode(metadataNode, createToken(PUBLIC_KEYWORD), classTypeQualifiers,
                createToken(CLASS_KEYWORD), className, createToken(OPEN_BRACE_TOKEN),
                createNodeList(memberNodeList), createToken(CLOSE_BRACE_TOKEN), null);
    }

    /**
     * Create startMessageReading function.
     * <pre>
      private isolated function startMessageReading() {
        worker readMessage returns error? {
            while self.isMessageReading {
                Message message = check self.clientEp->readMessage();
                check self.readMessageQueue.produce(message, 5);
                runtime:sleep(0.01);
            }
        }
      }
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
                        START_MESSAGE_READING_DESCRIPTION), qualifierList,
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
                        createBuiltinSimpleNameReferenceNode(null, createIdentifierToken(MESSAGE)),
                        createFieldBindingPatternVarnameNode(createSimpleNameReferenceNode(createIdentifierToken(
                                MESSAGE_VAR_NAME)))),
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
                                        createIdentifierToken(MESSAGE_VAR_NAME))),
                                createToken(COMMA_TOKEN),
                                createPositionalArgumentNode(createRequiredExpressionNode(
                                        createIdentifierToken(DEFAULT_PIPE_TIME_OUT)))
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
        FieldAccessExpressionNode selfDotIsMessageReading = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(SELF)), createToken(DOT_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken(GeneratorConstants.IS_MESSAGE_READING)));
        NodeList<StatementNode> workerStatements = createNodeList(createWhileStatementNode(createToken(WHILE_KEYWORD),
                selfDotIsMessageReading, whileBody, null));


        NodeList<AnnotationNode> annotations = createEmptyNodeList();
        NodeList workerDeclarationNodes = createNodeList(createNamedWorkerDeclarationNode(annotations,
                null, createToken(WORKER_KEYWORD)
                , createIdentifierToken(READ_MESSAGE), createReturnTypeDescriptorNode(
                        createToken(RETURNS_KEYWORD), createEmptyNodeList(), createIdentifierToken(OPTIONAL_ERROR)),
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

    private Node createStartPipeTriggering(List<MatchClauseNode> matchClauseNodes, List<String> idMethods)
            throws BallerinaAsyncApiException {

        //List to store metadata of the function
        ArrayList initMetaDataDoc = new ArrayList();

        //Create function signature node with metadata documentation
        FunctionSignatureNode functionSignatureNode = getStartPipeTriggeringFunctionSignatureNode(initMetaDataDoc);

        //Create function body node
        FunctionBodyNode functionBodyNode = getStartPipeTriggeringFunctionBodyNode(matchClauseNodes, idMethods);

        //Create function name
        NodeList<Token> qualifierList = createNodeList(createToken(PRIVATE_KEYWORD), createToken(ISOLATED_KEYWORD));
        IdentifierToken functionName = createIdentifierToken(START_PIPE_TRIGGERING);


        //Return function
        return createFunctionDefinitionNode(null, getDocCommentsForWorker(X_BALLERINA_PIPE_TRIGGER_DESCRIPTION,
                        START_PIPE_TRIGGERING_DESCRIPTION), qualifierList,
                createToken(FUNCTION_KEYWORD),
                functionName, createEmptyNodeList(), functionSignatureNode, functionBodyNode);
    }

    private FunctionBodyNode getStartPipeTriggeringFunctionBodyNode(
            List<MatchClauseNode> matchClauseNodes,
            List<String> idMethods)
            throws BallerinaAsyncApiException {

        String dispatcherKey = asyncAPI.getExtensions().get(X_DISPATCHER_KEY).asText();
        String dispatcherStreamId = null;
        if (asyncAPI.getExtensions().get(X_DISPATCHER_STREAM_ID) != null) {
            dispatcherStreamId = asyncAPI.getExtensions().get(X_DISPATCHER_STREAM_ID).asText();
        }
        //Define variables
        Token openParanToken = createToken(OPEN_PAREN_TOKEN);
        Token closeParanToken = createToken(CLOSE_PAREN_TOKEN);
        Token openBraceToken = createToken(OPEN_BRACE_TOKEN);
        Token closeBraceToken = createToken(CLOSE_BRACE_TOKEN);
        Token semicolonToken = createToken(SEMICOLON_TOKEN);
        Token equalToken = createToken(EQUAL_TOKEN);
        Token dotToken = createToken(DOT_TOKEN);


        SimpleNameReferenceNode responseMessageTypeNode = createSimpleNameReferenceNode(
                createIdentifierToken(MESSAGE));
        SimpleNameReferenceNode responseMessageVarNode = createSimpleNameReferenceNode(
                createIdentifierToken(MESSAGE_VAR_NAME));

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
                                        createIdentifierToken(DEFAULT_PIPE_TIME_OUT)))
                        ), closeParanToken));

        VariableDeclarationNode responseMessageNode = createVariableDeclarationNode(createEmptyNodeList(),
                null, createTypedBindingPatternNode(
                        responseMessageTypeNode,
                        createFieldBindingPatternVarnameNode(responseMessageVarNode)), equalToken, checkExpressionNode,
                semicolonToken);
        AsyncApi25SchemaImpl responseMessageSchema = createResponseMessage(dispatcherKey);
        TypeDefinitionNode responseMessageTypeDefinitionNode = ballerinaSchemaGenerator.getTypeDefinitionNode
                (responseMessageSchema, MESSAGE, new ArrayList<>());
        GeneratorUtils.updateTypeDefNodeList(MESSAGE, responseMessageTypeDefinitionNode,
                typeDefinitionNodeList);


        //////////////////////////////////////////////////////////////////////////////////////////////////////////////
        whileStatements.add(responseMessageNode);

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

        IfElseStatementNode ifElseStatementNode;
        if (matchClauseNodes.size() != 0 && idMethods.size() != 0) {
            ArrayList<StatementNode> ifStatementNodes = getIfStatementNodes(dispatcherStreamId);
            AsyncApi25SchemaImpl responseMessageWithIdSchema = createResponseMessageWithIDRecord(
                    dispatcherKey, dispatcherStreamId);
            TypeDefinitionNode typeDefinitionNode = ballerinaSchemaGenerator.getTypeDefinitionNode
                    (responseMessageWithIdSchema, MESSAGE_WITH_ID, new ArrayList<>());
            GeneratorUtils.updateTypeDefNodeList(MESSAGE_WITH_ID, typeDefinitionNode, typeDefinitionNodeList);
//            ballerinaSchemaGenerator.setIdMethodsPresent(true);

            if (dispatcherKey.equals("type")) {
                dispatcherKey = "'type";
            }
            ArrayList<StatementNode> elseStatementNodes = getElseStatementNodes(dispatcherKey,
                    matchClauseNodes);


            //Create if else statement node
            ifElseStatementNode = createIfElseStatementNode(createToken(IF_KEYWORD),
                    createMethodCallExpressionNode(responseMessageVarNode, dotToken, createSimpleNameReferenceNode(
                                    createIdentifierToken(HAS_KEY)),
                            openParanToken, createSeparatedNodeList(createSimpleNameReferenceNode(
                                    createIdentifierToken("\"" + dispatcherStreamId + "\""))),
                            closeParanToken), createBlockStatementNode(openBraceToken, createNodeList(ifStatementNodes),
                            closeBraceToken), createElseBlockNode(createToken(ELSE_KEYWORD),
                            createBlockStatementNode(openBraceToken, createNodeList(elseStatementNodes),
                                    closeBraceToken)));
            whileStatements.add(ifElseStatementNode);

        } else if (matchClauseNodes.size() == 0 && idMethods.size() != 0) {
            //Create if else statement node
            ArrayList<StatementNode> ifStatementNodes = getIfStatementNodes(dispatcherStreamId);
            AsyncApi25SchemaImpl responseMessageWithIdSchema = createResponseMessageWithIDRecord(dispatcherKey,
                    dispatcherStreamId);
            TypeDefinitionNode typeDefinitionNode = ballerinaSchemaGenerator.getTypeDefinitionNode
                    (responseMessageWithIdSchema, MESSAGE_WITH_ID, new ArrayList<>());
            GeneratorUtils.updateTypeDefNodeList(MESSAGE_WITH_ID, typeDefinitionNode, typeDefinitionNodeList);

            ifElseStatementNode = createIfElseStatementNode(createToken(IF_KEYWORD),
                    createMethodCallExpressionNode(responseMessageVarNode, dotToken, createSimpleNameReferenceNode(
                                    createIdentifierToken(HAS_KEY)),
                            openParanToken, createSeparatedNodeList(createSimpleNameReferenceNode(
                                    createIdentifierToken("\"" + dispatcherStreamId + "\""))),
                            closeParanToken), createBlockStatementNode(openBraceToken, createNodeList(ifStatementNodes),
                            closeBraceToken), null);
            whileStatements.add(ifElseStatementNode);


        } else {
            if (dispatcherKey.equals("type")) {
                dispatcherKey = "'type";
            }
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
        }


        //Create while body node
        BlockStatementNode whileBody = createBlockStatementNode(openBraceToken, createNodeList(whileStatements),
                closeBraceToken);

        //Add worker statements (whileBody is added)
        FieldAccessExpressionNode selfDotIsPipeTriggering = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(SELF)), createToken(DOT_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken(GeneratorConstants.IS_PIPE_TRIGGERING)));
        NodeList<StatementNode> workerStatements = createNodeList(createWhileStatementNode(createToken(WHILE_KEYWORD),
                selfDotIsPipeTriggering, whileBody, null));

        //Create worker
        NodeList workerDeclarationNodes = createNodeList(createNamedWorkerDeclarationNode(createEmptyNodeList(),
                null, createToken(WORKER_KEYWORD)
                , createIdentifierToken(PIPE_TRIGGER), createReturnTypeDescriptorNode(
                        createToken(RETURNS_KEYWORD), createEmptyNodeList(), createIdentifierToken(OPTIONAL_ERROR)),
                createBlockStatementNode(openBraceToken, workerStatements,
                        closeBraceToken)));

        //Return worker
        return createFunctionBodyBlockNode(openBraceToken,
                null, workerDeclarationNodes, closeBraceToken, null);

    }

    private AsyncApi25SchemaImpl createResponseMessageWithIDRecord(String dispatcherKey, String dispatcherStreamId) {
        //create MessageWithID record
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
        responseMessageWithId.addExtension(READ_ONLY, BooleanNode.TRUE);
        return responseMessageWithId;
    }

    private AsyncApi25SchemaImpl createResponseMessage(String dispatcherKey) {
        //create Message record
        AsyncApi25SchemaImpl message = new AsyncApi25SchemaImpl();
        message.setType(OBJECT);
        AsyncApi25SchemaImpl stringEventSchema = new AsyncApi25SchemaImpl();
        AsyncApi25SchemaImpl stringIdSchema = new AsyncApi25SchemaImpl();

        stringEventSchema.setType(STRING);
        stringIdSchema.setType(STRING);
        List requiredFields = new ArrayList();
        requiredFields.add(dispatcherKey);

        message.setRequired(requiredFields);
        message.addProperty(dispatcherKey, stringEventSchema);
        message.addExtension(READ_ONLY, BooleanNode.TRUE);
        return message;
    }

    private ArrayList<StatementNode> getElseStatementNodes(String dispatcherKey,
                                                           List<MatchClauseNode> matchClauseNodes) {
        SimpleNameReferenceNode responseMessageVarNode = createSimpleNameReferenceNode(
                createIdentifierToken(MESSAGE_VAR_NAME));
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
                createIdentifierToken(MESSAGE_WITH_ID));
        SimpleNameReferenceNode responseMessageVarNode = createSimpleNameReferenceNode(
                createIdentifierToken(MESSAGE_VAR_NAME));
        SimpleNameReferenceNode responseMessageWithIdVarNode = createSimpleNameReferenceNode(
                createIdentifierToken(MESSAGE_WITH_ID_VAR_NAME));

        //If statements
        ArrayList<StatementNode> ifStatementNodes = new ArrayList<>();


        //ResponseMessageWithId responseMessageWithId = check responseMessage.cloneWithType();
        MethodCallExpressionNode cloneWithTypeMethodCallExpressionNode = createMethodCallExpressionNode(
                responseMessageVarNode, dotToken,
                createSimpleNameReferenceNode(createIdentifierToken(CLONE_WITH_TYPE)),
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
                createSimpleNameReferenceNode(createIdentifierToken(GET_PIPE)),
                openParanToken, createSeparatedNodeList(createSimpleNameReferenceNode(
                        createIdentifierToken(dispatcherStreamId))), closeParanToken);

        SimpleNameReferenceNode responseTypePipeNode = createSimpleNameReferenceNode(createIdentifierToken(
                dispatcherStreamId + CAPITAL_PIPE));
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
        nodes.add(createIdentifierToken(DEFAULT_PIPE_TIME_OUT));
        MethodCallExpressionNode pipeProduceExpressionNode = createMethodCallExpressionNode(responseTypePipeNode,
                dotToken,
                createSimpleNameReferenceNode(createIdentifierToken(PRODUCE)),
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
                        START_MESSAGE_WRITING_DESCRIPTION), qualifierList,
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
                                        createIdentifierToken(DEFAULT_PIPE_TIME_OUT)))
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
                RUNTIME), createToken(COLON_TOKEN), createIdentifierToken(SLEEP));
        FunctionCallExpressionNode sleep = createFunctionCallExpressionNode(qualifiedNameReferenceNode, openParanToken,
                createSeparatedNodeList(createPositionalArgumentNode(
                        createRequiredExpressionNode(createIdentifierToken(WORKER_SLEEP_TIME_OUT)))), closeParanToken);
        ExpressionStatementNode runtimeSleep = createExpressionStatementNode(null,
                sleep, createToken(SEMICOLON_TOKEN));


        whileStatements.add(queueData);
        whileStatements.add(writeMessage);
        whileStatements.add(runtimeSleep);


        BlockStatementNode whileBody = createBlockStatementNode(openBraceToken, createNodeList(whileStatements),
                closeBraceToken);
        FieldAccessExpressionNode selfDotIsMessageWriting = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(SELF)), createToken(DOT_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken(GeneratorConstants.IS_MESSAGE_WRITING)));
        NodeList<StatementNode> workerStatements = createNodeList(createWhileStatementNode(createToken(WHILE_KEYWORD),
                selfDotIsMessageWriting, whileBody, null));


        NodeList workerDeclarationNodes = createNodeList(createNamedWorkerDeclarationNode(annotations,
                null, createToken(WORKER_KEYWORD)
                , createIdentifierToken(WRITE_MESSAGE), createReturnTypeDescriptorNode(
                        createToken(RETURNS_KEYWORD), createEmptyNodeList(), createIdentifierToken(OPTIONAL_ERROR)),
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


    private FunctionDefinitionNode createInitFunction(List<String> pipeNameMethods, boolean isStreamPresent)
            throws BallerinaAsyncApiException {
        AsyncApi25SchemaImpl headerSchema = new AsyncApi25SchemaImpl();
        AsyncApi25SchemaImpl querySchema = new AsyncApi25SchemaImpl();
        FunctionSignatureNode functionSignatureNode = getInitFunctionSignatureNode(querySchema,
                headerSchema);
        FunctionBodyNode functionBodyNode = getInitFunctionBodyNode(querySchema,
                headerSchema,
                pipeNameMethods,
                isStreamPresent);
        NodeList<Token> qualifierList = createNodeList(createToken(PUBLIC_KEYWORD), createToken(ISOLATED_KEYWORD));
        IdentifierToken functionName = createIdentifierToken(INIT);
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
                                                     AsyncApi25SchemaImpl headerSchema,
                                                     List<String> pipeNameMethods,
                                                     boolean isStreamPresent)
            throws BallerinaAsyncApiException {

        List<StatementNode> assignmentNodes = new ArrayList<>();


        //TODO: Attempt to map auth configurations
        // If both apiKey and httpOrOAuth is supported
        // todo : After revamping
        if (ballerinaAuthConfigGenerator.isHttpApiKey() && ballerinaAuthConfigGenerator.isHttpOROAuth()) {
            assignmentNodes.add(ballerinaAuthConfigGenerator.handleInitForMixOfApiKeyAndHTTPOrOAuth());
        }

        // self.pipes =new ();
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
                createSimpleNameReferenceNode(createIdentifierToken(GeneratorConstants.PIPES)));
        AssignmentStatementNode selfPipesAssignmentStatementNode = createAssignmentStatementNode(selfPipes,
                createToken(EQUAL_TOKEN), pipesExpressionNode, createToken(SEMICOLON_TOKEN));
        assignmentNodes.add(selfPipesAssignmentStatementNode);


        //TODO: use this as conditionally checking
        // create {@code self.streamGenerators =new ();} assignment node
        if (isStreamPresent) {
            List<Node> streamGeneratorsArgumentsList = new ArrayList<>();
            SeparatedNodeList<FunctionArgumentNode> streamGeneratorsArguments =
                    createSeparatedNodeList(streamGeneratorsArgumentsList);
            ParenthesizedArgList streamGeneratorsParenthesizedArgList = createParenthesizedArgList(openParenArg,
                    streamGeneratorsArguments,
                    closeParenArg);
            ImplicitNewExpressionNode streamGeneratorsExpressionNode =
                    createImplicitNewExpressionNode(createToken(NEW_KEYWORD),
                            streamGeneratorsParenthesizedArgList);

            FieldAccessExpressionNode selfStreamGenerators = createFieldAccessExpressionNode(
                    createSimpleNameReferenceNode(createIdentifierToken(SELF)), createToken(DOT_TOKEN),
                    createSimpleNameReferenceNode(createIdentifierToken(GeneratorConstants.STREAM_GENERATORS)));
            AssignmentStatementNode selfStreamGeneratorsAssignmentStatementNode = createAssignmentStatementNode(
                    selfStreamGenerators,
                    createToken(EQUAL_TOKEN), streamGeneratorsExpressionNode, createToken(SEMICOLON_TOKEN));
            assignmentNodes.add(selfStreamGeneratorsAssignmentStatementNode);
        }


        // self.writeMessageQueue = new (1000);
        List<Node> argumentsList = new ArrayList<>();
        FieldAccessExpressionNode selfWriteMessageQueues = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(SELF)), createToken(DOT_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken(WRITE_MESSAGE_QUEUE)));
        argumentsList.add(createIdentifierToken(QUEUE_DEFAULT_SIZE));
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
            throw new BallerinaAsyncApiException(BALLERINA_WEBSOCKET_DOESNT_SUPPORT_FOR_MULTIPLE_CHANNELS);
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


        // create initialization statement of websocket:Client class instance

        // self.clientEp = websocketEp
        FieldAccessExpressionNode selfClientEp = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(SELF)), createToken(DOT_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken(CLIENT_EP)));
        SimpleNameReferenceNode selfClientEpValue = createSimpleNameReferenceNode(createIdentifierToken(
                WEBSOCKET_EP));
        AssignmentStatementNode selfWebsocketClientAssignmentStatementNode = createAssignmentStatementNode(
                selfClientEp,
                createToken(EQUAL_TOKEN), selfClientEpValue, createToken(SEMICOLON_TOKEN));
        assignmentNodes.add(selfWebsocketClientAssignmentStatementNode);


        //self.pingMessagePipe= ()
        //self.connectionInitMessagePipe =()
        for (String pipeName : pipeNameMethods) {
            FieldAccessExpressionNode requestTypeVarRef = createFieldAccessExpressionNode(
                    createSimpleNameReferenceNode(createIdentifierToken(SELF)), createToken(DOT_TOKEN),
                    createSimpleNameReferenceNode(createIdentifierToken(pipeName)));
            AssignmentStatementNode namePipeDeclareNode = createAssignmentStatementNode(requestTypeVarRef,
                    createToken(EQUAL_TOKEN), createSimpleNameReferenceNode(createIdentifierToken(NULL_VALUE)),
                    createToken(SEMICOLON_TOKEN));
            assignmentNodes.add(namePipeDeclareNode);
        }


        //self.isMessageWriting = true;
        //self.isMessageReading =  true;
        //self.isPipeTriggering = true;
        ArrayList<String> whileLoopBreakVariables = new ArrayList<>();
        whileLoopBreakVariables.add(IS_MESSAGE_WRITING);
        whileLoopBreakVariables.add(IS_MESSAGE_READING);
        whileLoopBreakVariables.add(IS_PIPE_TRIGGERING);

        for (String whileLoopBreakVariable : whileLoopBreakVariables) {
            addInitsOFWhileLoopBreaksNodes(whileLoopBreakVariable, assignmentNodes, TRUE_KEYWORD.stringValue());
        }

        ArrayList<String> workers = new ArrayList<>();
        workers.add(START_MESSAGE_WRITING);
        workers.add(START_MESSAGE_READING);
        workers.add(START_PIPE_TRIGGERING);

        List workersArgumentsList = new ArrayList<>();
        SeparatedNodeList<FunctionArgumentNode> workersArguments =
                createSeparatedNodeList(workersArgumentsList);
        for (String worker : workers) {
            ExpressionStatementNode workerNode = createExpressionStatementNode(FUNCTION_CALL,
                    createMethodCallExpressionNode(createSimpleNameReferenceNode(createIdentifierToken(SELF)),
                            createToken(DOT_TOKEN), createSimpleNameReferenceNode(
                                    createIdentifierToken(worker)), openParenArg,
                            workersArguments, closeParenArg), createToken(SEMICOLON_TOKEN));
            assignmentNodes.add(workerNode);

        }


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

    private void addInitsOFWhileLoopBreaksNodes(String initName,
                                                List<StatementNode> assignmentNodes,
                                                String booleanValue) {
        SimpleNameReferenceNode selfIsMessageWritingValue = createSimpleNameReferenceNode(createIdentifierToken(
                booleanValue));
        FieldAccessExpressionNode selfIsMessageWriting = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(SELF)), createToken(DOT_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken(initName)));
        AssignmentStatementNode selfIsMessageWritingAssignmentStatementNode =
                createAssignmentStatementNode(selfIsMessageWriting, createToken(EQUAL_TOKEN), selfIsMessageWritingValue,
                        createToken(SEMICOLON_TOKEN));

        assignmentNodes.add(selfIsMessageWritingAssignmentStatementNode);
    }

    /**
     * Generate statements for query parameters and headers.
     */
    private void handleParameterSchemaInOperation(AsyncApi25SchemaImpl querySchema, AsyncApi25SchemaImpl headerSchema,
                                                  List<StatementNode> statementsList, boolean initalized) {

        handleQueryParamsAndHeaders(querySchema, headerSchema, statementsList, initalized);
    }

    /**
     * Handle query parameters and headers within a remote function.
     */
    public void handleQueryParamsAndHeaders(AsyncApi25SchemaImpl querySchema, AsyncApi25SchemaImpl headerSchema,
                                            List<StatementNode> statementsList, boolean initalized) {

        if (querySchema.getProperties() != null) {
            utilGenerator.setQueryParamsFound(true);
            statementsList.add(getMapForParameters(querySchema, MAP_ANY_DATA,
                    QUERY_PARAM));
            ExpressionStatementNode updatedPath;
            if (initalized) {
                updatedPath = GeneratorUtils.getSimpleExpressionStatementNode(
                        STRING + SPACE + MODIFIED_URL + EQUAL_SPACE + SERVICE_URL +
                                PLUS_SPACE + CHECK_PATH_FOR_QUERY_PARAM);

            } else {
                updatedPath = GeneratorUtils.getSimpleExpressionStatementNode(
                        MODIFIED_URL + EQUAL_SPACE + MODIFIED_URL + PLUS_SPACE + CHECK_PATH_FOR_QUERY_PARAM);

            }
            statementsList.add(updatedPath);

        }
        if (headerSchema.getProperties() != null) {
            statementsList.add(getMapForParameters(headerSchema, MAP_STRING,
                    HEADER_PARAM));
            statementsList.add(GeneratorUtils.getSimpleExpressionStatementNode(
                    MAP_STRING + SPACE + CUSTOM_HEADERS + EQUAL_SPACE + GET_COMBINE_HEADERS));
            statementsList.add(GeneratorUtils.getSimpleExpressionStatementNode(
                    CLIENT_CONFIG_CUSTOM_HEADERS + EQUAL_SPACE + CUSTOM_HEADERS
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
            IdentifierToken fieldName = createIdentifierToken(DOUBLE_QUOTE + (entry.getKey().trim()) +
                    DOUBLE_QUOTE);
            Token colon = createToken(COLON_TOKEN);
            SimpleNameReferenceNode valueExpr = null;
            if (entry.getValue().getType().equals(STRING) || mapName.equals(QUERY_PARAM)) {
                valueExpr = createSimpleNameReferenceNode(
                        createIdentifierToken(mapName + S_DOT + getValidName(entry.getKey().trim(),
                                false)));
            } else if (!entry.getValue().getType().equals(STRING) && mapName.equals(HEADER_PARAM)) {
                valueExpr = createSimpleNameReferenceNode(
                        createIdentifierToken(mapName + S_DOT +
                                getValidName(entry.getKey().trim(), false) + DOT_TO_STRING));
            }

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
                    String replaceVariable = "{getEncodedUri(" + "pathParams." +
                            GeneratorUtils.escapeIdentifier(d.trim()) + ")}";
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
                    API_KEY_CONFIG, DEFAULT_API_KEY_DESC);
            docs.add(apiKeyConfig);
        }
        // Create method description
        MarkdownParameterDocumentationLineNode clientConfig = DocCommentsGenerator.createAPIParamDoc(
                CONFIG,
                CONFIG_DESCRIPTION);
        docs.add(clientConfig);
        MarkdownParameterDocumentationLineNode serviceUrlAPI = DocCommentsGenerator.createAPIParamDoc(
                SERVICE_URL,
                SERVICE_URL_DESCRIPTION);
        docs.add(serviceUrlAPI);
        MarkdownParameterDocumentationLineNode returnDoc = DocCommentsGenerator.createAPIParamDoc(RETURN,
                RETURN_DESCRIPTION);
        docs.add(returnDoc);

        if (ballerinaAuthConfigGenerator.isPathParam()) {
            MarkdownParameterDocumentationLineNode pathParamDocNode = DocCommentsGenerator.createAPIParamDoc(
                    PATH_PARAMS, PATH_PARAMETERS);
            docs.add(pathParamDocNode);
        }
        if (ballerinaAuthConfigGenerator.isQueryParam()) {
            MarkdownParameterDocumentationLineNode queryParamDocNode = DocCommentsGenerator.createAPIParamDoc(
                    QUERY_PARAMS, QUERY_PARAMETERS);
            docs.add(queryParamDocNode);
        }
        if (ballerinaAuthConfigGenerator.isHeaderParam()) {
            MarkdownParameterDocumentationLineNode headerParamDocNode = DocCommentsGenerator.createAPIParamDoc(
                    HEADER_PARAMS, HEADER_PARAMETERS);
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
    private List<ObjectFieldNode> createClassInstanceVariables(List<String> pipeNameMethods, boolean isStreamPresent) {

        List<ObjectFieldNode> fieldNodeList = new ArrayList<>();
        Token privateKeywordToken = createToken(PRIVATE_KEYWORD);
        Token finalKeywordToken = createToken(FINAL_KEYWORD);
        ArrayList<Token> prefixTokensForPrivateAndFinal = new ArrayList<>();
        ArrayList<Token> prefixTokensForOnlyPrivate = new ArrayList<>();
        prefixTokensForPrivateAndFinal.add(privateKeywordToken);
        prefixTokensForPrivateAndFinal.add(finalKeywordToken);
        prefixTokensForOnlyPrivate.add(privateKeywordToken);
        NodeList<Token> qualifiersWithPrivateAndFinal = createNodeList(prefixTokensForPrivateAndFinal);
        NodeList<Token> qualifiersWithOnlyPrivate = createNodeList(prefixTokensForOnlyPrivate);

        //private final websocket:Client clientEp;
        QualifiedNameReferenceNode websocketType = createQualifiedNameReferenceNode(createIdentifierToken(WEBSOCKET),
                createToken(COLON_TOKEN), createIdentifierToken(GeneratorConstants.CLIENT_CLASS_NAME));

        ObjectFieldNode websocketClientField = getObjectFieldNode(
                qualifiersWithPrivateAndFinal,
                websocketType,
                GeneratorConstants.CLIENT_EP);

        fieldNodeList.add(websocketClientField);

        QualifiedNameReferenceNode pipeType = createQualifiedNameReferenceNode(createIdentifierToken(SIMPLE_PIPE),
                createToken(COLON_TOKEN), createIdentifierToken(GeneratorConstants.CAPITAL_PIPE));

        //private final pipe:Pipe writeMessageQueue;
        ObjectFieldNode writeMessageQueueClientField = getObjectFieldNode(
                qualifiersWithPrivateAndFinal,
                pipeType,
                GeneratorConstants.WRITE_MESSAGE_QUEUE);
        fieldNodeList.add(writeMessageQueueClientField);

        //private final pipe:Pipe readMessageQueue;
        ObjectFieldNode readMessageQueueClientField = getObjectFieldNode(
                qualifiersWithPrivateAndFinal,
                pipeType,
                GeneratorConstants.READ_MESSAGE_QUEUE);
        fieldNodeList.add(readMessageQueueClientField);


        //private final PipesMap pipes;
        SimpleNameReferenceNode pipesType = createSimpleNameReferenceNode(createIdentifierToken(
                GeneratorConstants.PIPES_MAP));
        ObjectFieldNode pipesField = getObjectFieldNode(
                qualifiersWithPrivateAndFinal,
                pipesType,
                GeneratorConstants.PIPES);
        fieldNodeList.add(pipesField);


        if (isStreamPresent) {
            //private final StreamGeneratorsMap streamGenerators;
            SimpleNameReferenceNode streamGeneratorsType =
                    createSimpleNameReferenceNode(createIdentifierToken(GeneratorConstants.STREAM_GENERATORS_MAP));
            ObjectFieldNode streamGeneratorsField = getObjectFieldNode(
                    qualifiersWithPrivateAndFinal,
                    streamGeneratorsType,
                    GeneratorConstants.STREAM_GENERATORS);
            fieldNodeList.add(streamGeneratorsField);
        }

        SimpleNameReferenceNode booleanType =
                createSimpleNameReferenceNode(createIdentifierToken(GeneratorConstants.BOOLEAN));
        //private boolean isMessageWriting;
        ObjectFieldNode isMessageWritingField = getObjectFieldNode(
                qualifiersWithOnlyPrivate,
                booleanType,
                GeneratorConstants.IS_MESSAGE_WRITING);
        fieldNodeList.add(isMessageWritingField);

        //private boolean isMessageReading;
        ObjectFieldNode isMessageReadingField = getObjectFieldNode(
                qualifiersWithOnlyPrivate,
                booleanType,
                GeneratorConstants.IS_MESSAGE_READING
        );
        fieldNodeList.add(isMessageReadingField);

        //private boolean isPipeTriggering;
        ObjectFieldNode isPipeTriggeringField = getObjectFieldNode(
                qualifiersWithOnlyPrivate,
                booleanType,
                GeneratorConstants.IS_PIPE_TRIGGERING
        );
        fieldNodeList.add(isPipeTriggeringField);

        for (String pipeName : pipeNameMethods) {
            OptionalTypeDescriptorNode pipeOptionalNode = createOptionalTypeDescriptorNode(pipeType,
                    createToken(QUESTION_MARK_TOKEN));
            //private pipe:Pipe? pingMessagePipe;
            ObjectFieldNode nameMessagePipeField = getObjectFieldNode(
                    qualifiersWithOnlyPrivate,
                    pipeOptionalNode,
                    pipeName
            );
            fieldNodeList.add(nameMessagePipeField);

        }


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
    private List<FunctionDefinitionNode> createRemoteFunctions(ArrayList streamReturns,
                                                               List<MatchClauseNode> matchStatementList,
                                                               List<String> pipeIdMethods,
                                                               List<String> pipeNameMethods)
            throws BallerinaAsyncApiException {

        Map<String, AsyncApiMessage> messages = asyncAPI.getComponents().
                getMessages();
        List<FunctionDefinitionNode> functionDefinitionNodeList = new ArrayList<>();


        // Create an array to store all request messages
        ArrayList remainingResponseMessages = new ArrayList();

        if (asyncAPI.getChannels().getItems().get(0).getPublish() != null) {
            List<AsyncApiMessage> publishMessages = null;
            if (asyncAPI.getChannels().getItems().get(0).getPublish().
                    getMessage().getOneOf() != null) {
                publishMessages = asyncAPI.getChannels().getItems().get(0).getPublish().
                        getMessage().getOneOf();
            } else if (asyncAPI.getChannels().getItems().get(0).getPublish().
                    getMessage() != null) {
                AsyncApiMessage asyncApiMessage = asyncAPI.getChannels().getItems().get(0).getPublish().
                        getMessage();
                List<AsyncApiMessage> onePublishMessage = new ArrayList<>();
                onePublishMessage.add(asyncApiMessage);
                publishMessages = onePublishMessage;

            }
            if (publishMessages != null) {
                ListIterator<AsyncApiMessage> requestMessages = publishMessages.listIterator();
                for (ListIterator<AsyncApiMessage> it = requestMessages; it.hasNext(); ) {

                    AsyncApi25MessageImpl message = (AsyncApi25MessageImpl) it.next();
                    String reference = message.get$ref();
                    String messageName = GeneratorUtils.extractReferenceType(reference);
                    AsyncApi25MessageImpl componentMessage = (AsyncApi25MessageImpl) messages.get(messageName);
                    Map<String, JsonNode> extensions = componentMessage.getExtensions();
                    if (extensions != null && extensions.get(X_RESPONSE) != null) {
                        FunctionDefinitionNode functionDefinitionNode =
                                getRemoteFunctionDefinitionNode(messageName, componentMessage, extensions,
                                        matchStatementList, pipeIdMethods,
                                        remainingResponseMessages, false, streamReturns, pipeNameMethods);
                        functionDefinitionNodeList.add(functionDefinitionNode);

                    } else {
                        FunctionDefinitionNode functionDefinitionNode =
                                getRemoteFunctionDefinitionNode(messageName,
                                        componentMessage,
                                        null,
                                        null, pipeIdMethods,
                                        null, false, null, pipeNameMethods);
                        functionDefinitionNodeList.add(functionDefinitionNode);
                    }

                }
            }
        }
        //Set util generator with stream return classes
        setUtilGenerator(new UtilGenerator(streamReturns));
        if (asyncAPI.getChannels().getItems().get(0).getSubscribe() != null) {
            List<AsyncApiMessage> subscribeMessages = null;
            if (asyncAPI.getChannels().getItems().get(0).getSubscribe().
                    getMessage().getOneOf() != null) {
                subscribeMessages = asyncAPI.getChannels().getItems().get(0).getSubscribe().
                        getMessage().getOneOf();
            } else if (asyncAPI.getChannels().getItems().get(0).getSubscribe().
                    getMessage() != null) {
                AsyncApiMessage asyncApiMessage = asyncAPI.getChannels().getItems().get(0).getSubscribe().
                        getMessage();
                List<AsyncApiMessage> oneSubscribeMessage = new ArrayList<>();
                oneSubscribeMessage.add(asyncApiMessage);
                subscribeMessages = oneSubscribeMessage;

            }
            if (subscribeMessages != null) {

                ListIterator<AsyncApiMessage> responseMessages = subscribeMessages.listIterator();
                for (ListIterator<AsyncApiMessage> it = responseMessages; it.hasNext(); ) {
                    AsyncApi25MessageImpl message = (AsyncApi25MessageImpl) it.next();
                    String reference = message.get$ref();
                    String messageName = GeneratorUtils.extractReferenceType(reference);
                    if (!remainingResponseMessages.contains(messageName)) {
                        Map<String, JsonNode> extensions = new LinkedHashMap<>();
                        AsyncApi25MessageImpl newMessage = new AsyncApi25MessageImpl();
                        ObjectNode objectNode = new ObjectNode(JsonNodeFactory.instance);
                        objectNode.put(REF, reference);
                        extensions.put(X_RESPONSE, objectNode);
                        newMessage.addExtension(X_RESPONSE, objectNode);
                        extensions.put(X_RESPONSE_TYPE, new TextNode(SIMPLE_RPC));
                        newMessage.addExtension(X_RESPONSE_TYPE, new TextNode(SIMPLE_RPC));


                        FunctionDefinitionNode functionDefinitionNode =
                                getRemoteFunctionDefinitionNode(messageName,
                                        newMessage,
                                        extensions,
                                        matchStatementList, pipeIdMethods,
                                        null, true, streamReturns, pipeNameMethods);
                        functionDefinitionNodeList.add(functionDefinitionNode);

                    }
                }

            }

        }

        for (String pipeNameMethod : pipeNameMethods) {
            functionDefinitionNodeList.add(createNamePipeCloseFunction(pipeNameMethod));
        }
        boolean streamsPresent = streamReturns.size() > 0;
        functionDefinitionNodeList.add(createConnectionCloseFunction(streamsPresent));


        return functionDefinitionNodeList;
    }

    private FunctionDefinitionNode createConnectionCloseFunction(boolean streamsPresent) {
        Token dotToken = createToken(DOT_TOKEN);
        Token rightArrowToken = createToken(RIGHT_ARROW_TOKEN);
        Token openBraceToken = createToken(OPEN_BRACE_TOKEN);
        Token closeBraceToken = createToken(CLOSE_BRACE_TOKEN);
        ArrayList<StatementNode> lockStatements = new ArrayList<>();
        addInitsOFWhileLoopBreaksNodes(IS_MESSAGE_READING, lockStatements, FALSE_KEYWORD.stringValue());
        addInitsOFWhileLoopBreaksNodes(IS_MESSAGE_WRITING, lockStatements, FALSE_KEYWORD.stringValue());
        addInitsOFWhileLoopBreaksNodes(IS_PIPE_TRIGGERING, lockStatements, FALSE_KEYWORD.stringValue());

        //check self.writeMessageQueue.immediateClose();
        ExpressionStatementNode writeMessageStatementNode = getCloseLockStatementNode(
                WRITE_MESSAGE_QUEUE, IMMEDIATE_CLOSE, dotToken);
        lockStatements.add(writeMessageStatementNode);

        //check self.readMessageQueue.immediateClose();
        ExpressionStatementNode readMessageStatementNode = getCloseLockStatementNode(
                READ_MESSAGE_QUEUE, IMMEDIATE_CLOSE, dotToken);
        lockStatements.add(readMessageStatementNode);

        //check self.pipes.removePipes();
        ExpressionStatementNode removePipesNode = getCloseLockStatementNode(PIPES, REMOVE_PIPES, dotToken);
        lockStatements.add(removePipesNode);

        //TODO: conditionally check this one
        if (streamsPresent) {
            ExpressionStatementNode removeStreamGeneratorsNode = getCloseLockStatementNode(STREAM_GENERATORS,
                    REMOVE_STREAM_GENERATORS, dotToken);
            lockStatements.add(removeStreamGeneratorsNode);
        }


        ExpressionStatementNode clientCloseNode = getCloseLockStatementNode(CLIENT_EP, CLOSE, rightArrowToken);
        lockStatements.add(clientCloseNode);

        LockStatementNode lockStatementNode = createLockStatementNode(createToken(LOCK_KEYWORD),
                createBlockStatementNode(openBraceToken,
                        createNodeList(lockStatements), closeBraceToken), null);

        IdentifierToken functionName = createIdentifierToken(CONNECTION_CLOSE);
        return getAdditionalFunctionDefinitionNode(functionName, lockStatementNode);

    }

    private FunctionDefinitionNode createNamePipeCloseFunction(String pipeNameMethod) {
        // pipe:Pipe pingMessagePipe = check self.pingMessagePipe.ensureType();
        // check pingMessagePipe.gracefulClose();
        Token dotToken = createToken(DOT_TOKEN);
        Token closeParenToken = createToken(CLOSE_PAREN_TOKEN);
        Token openParenToken = createToken(OPEN_PAREN_TOKEN);
        Token equalToken = createToken(EQUAL_TOKEN);
        Token semicolonToken = createToken(SEMICOLON_TOKEN);
        Token openBraceToken = createToken(OPEN_BRACE_TOKEN);
        Token closeBraceToken = createToken(CLOSE_BRACE_TOKEN);


        ArrayList<StatementNode> ifStatements = new ArrayList<>();
        SimpleNameReferenceNode requestTypePipeNode =
                createSimpleNameReferenceNode(createIdentifierToken(pipeNameMethod));
        FieldAccessExpressionNode selfRequestTypePipeNodes = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(SELF)), createToken(DOT_TOKEN),
                requestTypePipeNode);
        MethodCallExpressionNode methodCallExpressionNode = createMethodCallExpressionNode(
                selfRequestTypePipeNodes, dotToken,
                createSimpleNameReferenceNode(createIdentifierToken(ENSURE_TYPE)),
                openParenToken, createSeparatedNodeList(), closeParenToken);
        CheckExpressionNode checkExpressionNode = createCheckExpressionNode(null, createToken(CHECK_KEYWORD),
                methodCallExpressionNode);
        VariableDeclarationNode messagePipeEnsureType = createVariableDeclarationNode(createEmptyNodeList(),
                null,
                createTypedBindingPatternNode(
                        createQualifiedNameReferenceNode(createIdentifierToken(SIMPLE_PIPE), createToken
                                (COLON_TOKEN), createIdentifierToken(CAPITAL_PIPE)),
                        createFieldBindingPatternVarnameNode(
                                requestTypePipeNode)),
                equalToken, checkExpressionNode, semicolonToken);


        ifStatements.add(messagePipeEnsureType);


        MethodCallExpressionNode gracefulMethodCallNode = createMethodCallExpressionNode(
                requestTypePipeNode, dotToken,
                createSimpleNameReferenceNode(createIdentifierToken(GRACEFUL_CLOSE)),
                openParenToken, createSeparatedNodeList(), closeParenToken);
        CheckExpressionNode graceFulCheckNode = createCheckExpressionNode(null,
                createToken(CHECK_KEYWORD), gracefulMethodCallNode);
        ExpressionStatementNode graceFulCheckExpressionNode = createExpressionStatementNode(
                null, graceFulCheckNode, semicolonToken);
        ifStatements.add(graceFulCheckExpressionNode);


        IfElseStatementNode pipeNullCheckNode = createIfElseStatementNode(createToken(IF_KEYWORD),
                createSimpleNameReferenceNode(createIdentifierToken(SELF + DOT + pipeNameMethod +
                        SPACE + NOT_IS + NULL_VALUE)),
                createBlockStatementNode(openBraceToken, createNodeList(ifStatements), closeBraceToken), null);


        LockStatementNode lockStatementNode = createLockStatementNode(createToken(LOCK_KEYWORD),
                createBlockStatementNode(openBraceToken,
                        createNodeList(pipeNullCheckNode), closeBraceToken), null);
        char responseTypeFirstChar = Character.toUpperCase(pipeNameMethod.charAt(0)); //Lowercase the first character
        String responseRemainingString = pipeNameMethod.substring(1);
        String responseTypeCamelCaseName = responseTypeFirstChar + responseRemainingString;
        IdentifierToken functionName = createIdentifierToken(CLOSE + responseTypeCamelCaseName);
        return getAdditionalFunctionDefinitionNode(functionName, lockStatementNode);
    }


    private FunctionDefinitionNode getAdditionalFunctionDefinitionNode(IdentifierToken functionName,
                                                                       LockStatementNode lockStatementNode) {
        Token openParenToken = createToken(OPEN_PAREN_TOKEN);
        Token closeParenToken = createToken(CLOSE_PAREN_TOKEN);
        Token openBraceToken = createToken(OPEN_BRACE_TOKEN);
        Token closeBraceToken = createToken(CLOSE_BRACE_TOKEN);
        Token semicolonToken = createToken(SEMICOLON_TOKEN);
        ReturnTypeDescriptorNode returnTypeDescriptorNode = createReturnTypeDescriptorNode(createToken(RETURNS_KEYWORD)
                , createNodeList(),
                createSimpleNameReferenceNode(createIdentifierToken(OPTIONAL_ERROR)));
        FunctionSignatureNode functionSignatureNode = createFunctionSignatureNode(openParenToken,
                createSeparatedNodeList(), closeParenToken, returnTypeDescriptorNode);

        FunctionBodyNode functionBodyNode = createFunctionBodyBlockNode(openBraceToken, null,
                createNodeList(lockStatementNode), closeBraceToken, semicolonToken);

        MetadataNode metadataNode = createMetadataNode(null, createNodeList());
        NodeList<Token> qualifierList = createNodeList(createToken(
                        REMOTE_KEYWORD),
                createToken(ISOLATED_KEYWORD));
        Token functionKeyWord = createToken(FUNCTION_KEYWORD);
        return createFunctionDefinitionNode(null,
                metadataNode, qualifierList, functionKeyWord, functionName, createEmptyNodeList(),
                functionSignatureNode, functionBodyNode);

    }

    private ExpressionStatementNode getCloseLockStatementNode(String messageQueue,

                                                              String closeType, Token divideToken) {

        Token openParenToken = createToken(OPEN_PAREN_TOKEN);
        Token semicolonToken = createToken(SEMICOLON_TOKEN);
        Token dotToken = createToken(DOT_TOKEN);
        Token closeParenToken = createToken(CLOSE_PAREN_TOKEN);

        FieldAccessExpressionNode writeMessageQueue = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(SELF)), dotToken,
                createSimpleNameReferenceNode(createIdentifierToken(messageQueue)));
        CheckExpressionNode checkExpressionNode = createCheckExpressionNode(null, createToken(CHECK_KEYWORD),
                createMethodCallExpressionNode(writeMessageQueue, divideToken,
                        createSimpleNameReferenceNode(createIdentifierToken(closeType)), openParenToken,
                        createSeparatedNodeList(), closeParenToken));
        ExpressionStatementNode expressionStatementNode = createExpressionStatementNode(
                null, checkExpressionNode, semicolonToken);
        return expressionStatementNode;
    }


    /**
     * Generate remote function definition node.
     */
    private FunctionDefinitionNode getRemoteFunctionDefinitionNode(String messageName,
                                                                   AsyncApi25MessageImpl messageValue,
                                                                   Map<String, JsonNode> extensions,
                                                                   List<MatchClauseNode> matchStatementList,
                                                                   List<String> pipeIdMethods,
                                                                   ArrayList responseMessages,
                                                                   boolean isSubscribe,
                                                                   List<String> streamReturns,
                                                                   List<String> pipeNameMethods)
            throws BallerinaAsyncApiException {
        String specDispatcherStreamId = null;
        if (asyncAPI.getExtensions().get(X_DISPATCHER_STREAM_ID) != null) {
            specDispatcherStreamId = asyncAPI.getExtensions().get(X_DISPATCHER_STREAM_ID).asText();

        }

        Map<String, Schema> schemas = asyncAPI.getComponents().getSchemas();

        // Create api doc for remote function
        List<Node> remoteFunctionDocs = new ArrayList<>();

        //Add documentation for remote function
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


        //Add remote function for test files
        remoteFunctionNameList.add(messageName);
        String responseType = null;
        if (extensions != null) {
            JsonNode xResponse = extensions.get(X_RESPONSE);
            JsonNode xResponseType = extensions.get(X_RESPONSE_TYPE);
            responseType = functionReturnType.getReturnType(xResponse, xResponseType, responseMessages);
        }


        //Create remote function signature
        RemoteFunctionSignatureGenerator remoteFunctionSignatureGenerator = new
                RemoteFunctionSignatureGenerator(
                asyncAPI,
                ballerinaSchemaGenerator,
                typeDefinitionNodeList);

        FunctionSignatureNode functionSignatureNode =
                remoteFunctionSignatureGenerator.getFunctionSignatureNode(
                        messageValue.getPayload(),
                        remoteFunctionDocs,
                        extensions,
                        responseType,
                        streamReturns);
        typeDefinitionNodeList = remoteFunctionSignatureGenerator.getTypeDefinitionNodeList();


        // Create metadataNode add documentation string
        MetadataNode metadataNode = createMetadataNode(createMarkdownDocumentationNode(
                createNodeList(remoteFunctionDocs)), createNodeList());


        // Create remote Function Body
        RemoteFunctionBodyGenerator remoteFunctionBodyGenerator = new RemoteFunctionBodyGenerator(imports);
        boolean schemaDispatcherStreamIdContains = false;
        if (messageValue.getPayload() != null) {
            JsonNode jsonNode = messageValue.getPayload();
            TextNode textNode = (TextNode) jsonNode.get(REF);
            String schemaName = GeneratorUtils.extractReferenceType(textNode.asText());
            AsyncApi25SchemaImpl schema = (AsyncApi25SchemaImpl) schemas.get(schemaName);
            CommonFunctionUtils commonFunctionUtils = new CommonFunctionUtils(asyncAPI);
            schemaDispatcherStreamIdContains = commonFunctionUtils.isDispatcherPresent
                    (schemaName, schema, specDispatcherStreamId, true);
        }

        //Check if the schema has dispatcherStreamId
        if ((specDispatcherStreamId != null && !schemaDispatcherStreamIdContains)) {
            //If no dispatcherStreamId found from the schema
            specDispatcherStreamId = null;

            //Check if the schema has dispatcherStreamId
        } else if (specDispatcherStreamId != null) {
            //If found at least one dispatcherStreamId then don't add uuid again and again
            pipeIdMethods.add(messageName);
            ImportDeclarationNode importForUUID = GeneratorUtils.
                    getImportDeclarationNode(GeneratorConstants.BALLERINA
                            , UUID);

            if (pipeIdMethods.size() == 1) {
                imports.add(importForUUID);
            }

        }
        char requestTypeFirstChar = Character.toLowerCase(messageName.charAt(0)); // Lowercase the first character
        String requestRemainingString = messageName.substring(1);
        String requestTypeCamelCaseName = requestTypeFirstChar + requestRemainingString;
        FunctionBodyNode functionBodyNode = remoteFunctionBodyGenerator.getFunctionBodyNode(
                extensions,
                requestTypeCamelCaseName,
                specDispatcherStreamId,
                matchStatementList,
                isSubscribe, responseType, pipeNameMethods);


        //Create remote function details
        NodeList<Token> qualifierList = createNodeList(createToken(
                        REMOTE_KEYWORD),
                createToken(ISOLATED_KEYWORD));
        Token functionKeyWord = createToken(FUNCTION_KEYWORD);
        IdentifierToken functionName = createIdentifierToken(REMOTE_METHOD_NAME_PREFIX +
                getValidName(messageName, true));

        //Create whole remote function
        return createFunctionDefinitionNode(null,
                metadataNode, qualifierList, functionKeyWord, functionName, createEmptyNodeList(),
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
            if (!selectedServer.getUrl().startsWith(WSS + COLON) && servers.getItems().size() > 1) {
                for (AsyncApiServer server : serversList) {
                    if (server.getUrl().startsWith(WSS + COLON)) {
                        selectedServer = (AsyncApi25ServerImpl) server;
                        break;
                    }
                }
            }
            if (selectedServer.getUrl() == null) {
                serverURL = DEFAULT_URL;
            } else if (selectedServer.getVariables() != null) {
                Map<String, ServerVariable> variables = selectedServer.getVariables();

                String resolvedUrl = GeneratorUtils.buildUrl(selectedServer.getUrl(), variables);

                try {
                    new URI(resolvedUrl);
                } catch (URISyntaxException e) {
                    throw new BallerinaAsyncApiException(FAIL_TO_READ_ENDPOINT_DETAILS +
                            selectedServer.getUrl(), e);
                }

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
