/*
 *  Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
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
package io.ballerina.asyncapi.websocketscore.generators.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.apicurio.datamodels.models.Schema;
import io.apicurio.datamodels.models.ServerVariable;
import io.apicurio.datamodels.models.asyncapi.AsyncApiChannelItem;
import io.apicurio.datamodels.models.asyncapi.AsyncApiMessage;
import io.apicurio.datamodels.models.asyncapi.AsyncApiOperation;
import io.apicurio.datamodels.models.asyncapi.AsyncApiSchema;
import io.apicurio.datamodels.models.asyncapi.AsyncApiServer;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25DocumentImpl;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25InfoImpl;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25MessageImpl;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25Schema;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25SchemaImpl;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25ServerImpl;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25ServersImpl;
import io.ballerina.asyncapi.websocketscore.GeneratorConstants;
import io.ballerina.asyncapi.websocketscore.GeneratorUtils;
import io.ballerina.asyncapi.websocketscore.exception.BallerinaAsyncApiExceptionWs;
import io.ballerina.asyncapi.websocketscore.generators.client.model.AasClientConfig;
import io.ballerina.asyncapi.websocketscore.generators.document.DocCommentsGenerator;
import io.ballerina.asyncapi.websocketscore.generators.schema.BallerinaTypesGenerator;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.AssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.BinaryExpressionNode;
import io.ballerina.compiler.syntax.tree.BlockStatementNode;
import io.ballerina.compiler.syntax.tree.BuiltinSimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.CaptureBindingPatternNode;
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionStatementNode;
import io.ballerina.compiler.syntax.tree.FieldAccessExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.FunctionBodyNode;
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
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.MethodCallExpressionNode;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NodeParser;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.AND_SPACE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.API_KEY_CONFIG;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.ATTEMPT_CON_CLOSE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.ATTEMPT_TO_CLOSE_CONNECTION;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.BALLERINA_WEBSOCKET_DOESNT_SUPPORT_FOR_MULTIPLE_CHANNELS;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.CAPITAL_PIPE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.CHECK_PATH_FOR_QUERY_PARAM;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.CLIENT_CLASS_NAME;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.CLIENT_CONFIG_CUSTOM_HEADERS;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.CLIENT_EP;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.CLOSE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.COLON;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.CONFIG;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.CONFIG_DESCRIPTION;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.CONNECTION_CLOSE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.CONNECTION_CLOSE_STATEMENT;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.CONNECTION_ERR;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.CONSUME;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.CUSTOM_HEADERS;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.DEFAULT_API_KEY_DESC;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.DEFAULT_PIPE_TIME_OUT;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.DEFAULT_URL;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.DOT;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.DOT_TO_STRING;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.DOUBLE_QUOTE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.EQUAL_SPACE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.ERROR;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.FAIL_TO_READ_ENDPOINT_DETAILS;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.GET_COMBINE_HEADERS;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.GET_PIPE_NAME_STATEMENT;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.GET_PIPE_NAME_FUNCTION_TEMPLATE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.GET_RECORD_NAME_FUNCTION_TEMPLATE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.HEADER_PARAM;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.HEADER_PARAMETERS;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.HEADER_PARAMS;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.IMMEDIATE_CLOSE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.INIT;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.IS;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.IS_ACTIVE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.LANG_REGEXP;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.LOG;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.LOG_PRINT_ERR;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.MAP_ANY_DATA;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.MAP_STRING;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.MESSAGE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.MESSAGE_VAR_NAME;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.MESSAGE_WITH_ID;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.MESSAGE_WITH_ID_VAR_CLONE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.MESSAGE_WITH_ID_VAR_NAME;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.MODIFIED_URL;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.NOT;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.OBJECT;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.OPTIONAL_ERROR;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.OP_TIMEOUT_EXPR;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.PATH_PARAMETERS;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.PATH_PARAMS;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.PIPES;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.PIPE_COLON_PIPE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.PIPE_ERR;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.PIPE_ERROR_NODE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.PIPE_NAME;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.PIPE_VAR;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.PLUS_SPACE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.PRODUCE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.QUERY_PARAM;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.QUERY_PARAMETERS;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.QUERY_PARAMS;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.QUEUE_DEFAULT_SIZE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.READONLY;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.READ_MESSAGE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.READ_MESSAGE_CLIENT_READ_ERROR;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.READ_MESSAGE_PIPE_PRODUCE_ERROR;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.READ_ONLY;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.REF;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.REMOTE_METHOD_NAME_PREFIX;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.REMOVE_PIPES;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.REMOVE_STREAM_GENERATORS;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.RESPONSE_MAP;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.RETURN;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.RETURN_DESCRIPTION;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.SELF;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.SELF_PIPES_GET_PIPE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.SEMICOLON;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.SERVICE_URL;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.SERVICE_URL_DESCRIPTION;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.SIMPLE_PIPE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.SIMPLE_RPC;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.SPACE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.START_MESSAGE_READING;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.START_MESSAGE_READING_DESCRIPTION;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.START_MESSAGE_WRITING;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.START_MESSAGE_WRITING_DESCRIPTION;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.STREAM_GENERATORS;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.STRING;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.S_DOT;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.UUID;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.WEBSOCKET;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.WEBSOCKET_EP;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.WRITE_MESSAGE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.WRITE_MESSAGE_CLIENT_WRITE_ERROR;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.WRITE_MESSAGE_PIPE_CONSUME_ERROR;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.WRITE_MESSAGE_QUEUE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.WSS;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.WS_ERR;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.WS_ERROR;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.WS_ERROR_OPTIONAL;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.XLIBB_PIPE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.X_BALLERINA_INIT_DESCRIPTION;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.X_BALLERINA_MESSAGE_READ_DESCRIPTION;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.X_BALLERINA_MESSAGE_WRITE_DESCRIPTION;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.X_DISPATCHER_KEY;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.X_DISPATCHER_KEY_CANNOT_BE_EMPTY;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.X_DISPATCHER_KEY_MUST_INCLUDE_IN_THE_SPECIFICATION;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.X_DISPATCHER_STREAM_ID;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.X_DISPATCHER_STREAM_ID_CANNOT_BE_EMPTY;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.X_RESPONSE;
import static io.ballerina.asyncapi.websocketscore.GeneratorConstants.X_RESPONSE_TYPE;
import static io.ballerina.asyncapi.websocketscore.GeneratorUtils.escapeIdentifier;
import static io.ballerina.asyncapi.websocketscore.GeneratorUtils.extractReferenceType;
import static io.ballerina.asyncapi.websocketscore.GeneratorUtils.getValidName;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.ONEOF;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.service.AsyncApiRemoteMapper.isCloseFrameSchema;
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
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBreakStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBuiltinSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createCaptureBindingPatternNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createCheckExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createClassDefinitionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createContinueStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createElseBlockNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createExpressionStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFieldAccessExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFieldBindingPatternVarnameNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionBodyBlockNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionDefinitionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionSignatureNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createIfElseStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createImplicitNewExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createLockStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMappingConstructorExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMarkdownDocumentationLineNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMarkdownDocumentationNode;
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
import static io.ballerina.compiler.syntax.tree.NodeFactory.createUnionTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createVariableDeclarationNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createWhileStatementNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.BACKTICK_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.BINARY_EXPRESSION;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.BREAK_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CHECK_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLASS_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLIENT_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.COLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.COMMA_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CONTINUE_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.DOT_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.DOUBLE_EQUAL_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ELSE_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.EOF_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.EQUAL_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ERROR_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FALSE_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FINAL_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FUNCTION_CALL;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FUNCTION_DEFINITION;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FUNCTION_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.IF_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ISOLATED_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.LOCK_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.NEW_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PIPE_TOKEN;
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
import static io.ballerina.compiler.syntax.tree.SyntaxKind.VAR_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.WHILE_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.WORKER_KEYWORD;

/**
 * This class is used to generate ballerina client file according to given yaml file.
 */
public class IntermediateClientGenerator {

    private final AsyncApi25DocumentImpl asyncApi;
    private final List<String> remoteFunctionNameList;
    private final BallerinaAuthConfigGenerator ballerinaAuthConfigGenerator;
    private final List<ImportDeclarationNode> imports;
    private final BallerinaTypesGenerator ballerinaSchemaGenerator;
    private final RemoteFunctionReturnTypeGenerator functionReturnType;
    private final Map<String, String> responseMap = new HashMap<>();
    private UtilGenerator utilGenerator;
    private List<TypeDefinitionNode> typeDefinitionNodeList;
    private List<String> apiKeyNameList = new ArrayList<>();
    private String serverURL;
    private String clientName = null;
    private String dispatcherKey;
    private String dispatcherStreamId;

    private static final Token openParenToken = createToken(OPEN_PAREN_TOKEN);
    private static final Token closeParenToken = createToken(CLOSE_PAREN_TOKEN);
    private static final Token openBraceToken = createToken(OPEN_BRACE_TOKEN);
    private static final Token closeBraceToken = createToken(CLOSE_BRACE_TOKEN);
    private static final Token semicolonToken = createToken(SEMICOLON_TOKEN);
    private static final Token equalToken = createToken(EQUAL_TOKEN);
    private static final Token dotToken = createToken(DOT_TOKEN);
    private static final Token rightArrowToken = createToken(RIGHT_ARROW_TOKEN);
    private static final Token colonToken = createToken(COLON_TOKEN);
    private static final Token eofToken = createToken(EOF_TOKEN);

    public IntermediateClientGenerator(AasClientConfig asyncAPIClientConfig) {

        this.imports = new ArrayList<>();
        this.typeDefinitionNodeList = new ArrayList<>();
        this.asyncApi = asyncAPIClientConfig.getAsyncAPI();
        this.utilGenerator = null;
        this.remoteFunctionNameList = new ArrayList<>();
        this.ballerinaSchemaGenerator = new BallerinaTypesGenerator(asyncApi, new LinkedList<>());
        this.serverURL = "/";
        this.ballerinaAuthConfigGenerator = new BallerinaAuthConfigGenerator(false, false, ballerinaSchemaGenerator);
        this.functionReturnType = new RemoteFunctionReturnTypeGenerator(this.asyncApi);
    }

    private static ObjectFieldNode getObjectFieldNode(NodeList<Token> qualifiers, Node typeNode,
                                                      String fieldIdentifier) {
        IdentifierToken fieldName = createIdentifierToken(fieldIdentifier);
        MetadataNode metadataNode = createMetadataNode(null, createEmptyNodeList());
        return createObjectFieldNode(metadataNode, null, qualifiers, typeNode, fieldName, null, null, semicolonToken);
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
     * @throws BallerinaAsyncApiExceptionWs When function fail in process.
     */
    public SyntaxTree generateSyntaxTree() throws BallerinaAsyncApiExceptionWs {

        // Create `ballerina/websocket` import declaration node, "ballerina/uuid will be imported only for streaming"
        ImportDeclarationNode importForWebsocket = GeneratorUtils.getImportDeclarationNode(GeneratorConstants.BALLERINA
                , WEBSOCKET);
        ImportDeclarationNode importForXlibbPipe = GeneratorUtils.getImportDeclarationNode(GeneratorConstants.XLIBB
                , XLIBB_PIPE);
        ImportDeclarationNode importForLog = GeneratorUtils.getImportDeclarationNode(GeneratorConstants.BALLERINA
                , LOG);
        ImportDeclarationNode importForRegex = GeneratorUtils.getImportDeclarationNode(GeneratorConstants.BALLERINA
                , LANG_REGEXP);

        imports.add(importForRegex);

        imports.add(importForLog);
        imports.add(importForWebsocket);
        imports.add(importForXlibbPipe);

        //TODO: This has to improve
        // Add authentication related records
        ballerinaAuthConfigGenerator.addAuthRelatedRecords(asyncApi);

        List<ModuleMemberDeclarationNode> nodes = new ArrayList<>();
        // Add class definition node to module member nodes
        nodes.add(getClassDefinitionNode());

        NodeList<ImportDeclarationNode> importsList = createNodeList(imports);


        ModulePartNode modulePartNode = createModulePartNode(importsList, createNodeList(nodes), eofToken);
        TextDocument textDocument = TextDocuments.from("");
        SyntaxTree syntaxTree = SyntaxTree.from(textDocument);
        return syntaxTree.modifyWith(modulePartNode);
    }

    public UtilGenerator getBallerinaUtilGenerator() {
        return utilGenerator;
    }

    /**
     * Generate Class definition Nodes.
     */
    private ClassDefinitionNode getClassDefinitionNode() throws BallerinaAsyncApiExceptionWs {

        //Get dispatcherKey
        Map<String, JsonNode> extensions = asyncApi.getExtensions();

        if (extensions == null || extensions.get(X_DISPATCHER_KEY) == null) {
            throw new BallerinaAsyncApiExceptionWs(X_DISPATCHER_KEY_MUST_INCLUDE_IN_THE_SPECIFICATION);
        }
        TextNode dispatcherKeyNode = (TextNode) extensions.get(X_DISPATCHER_KEY);

        this.dispatcherKey = dispatcherKeyNode.asText();
        if (this.dispatcherKey.isEmpty()) {
            throw new BallerinaAsyncApiExceptionWs(X_DISPATCHER_KEY_CANNOT_BE_EMPTY);
        }

        //Get dispatcherStreamId
        if (extensions.get(X_DISPATCHER_STREAM_ID) != null) {
            TextNode dispatcherStreamIdNode = (TextNode) extensions.get(X_DISPATCHER_STREAM_ID);
            if (dispatcherStreamIdNode != null) {
                this.dispatcherStreamId = extensions.get(X_DISPATCHER_STREAM_ID).asText();
                if (this.dispatcherStreamId.isEmpty()) {
                    throw new BallerinaAsyncApiExceptionWs(X_DISPATCHER_STREAM_ID_CANNOT_BE_EMPTY);
                }
            }
        }

        // Add Message and MessageWithId types to typeDefinitionNodeList
        AsyncApi25SchemaImpl responseMessageSchema = createResponseMessage(this.dispatcherKey);
        TypeDefinitionNode responseMessageTypeDefinitionNode = ballerinaSchemaGenerator.getTypeDefinitionNode
                (responseMessageSchema, MESSAGE, new ArrayList<>());
        GeneratorUtils.updateTypeDefNodeList(MESSAGE, responseMessageTypeDefinitionNode, typeDefinitionNodeList);

        if (!Objects.isNull(this.dispatcherStreamId)) {
            AsyncApi25SchemaImpl responseMessageWithIdSchema = createResponseMessageWithIDRecord(
                    this.dispatcherKey, this.dispatcherStreamId);
            TypeDefinitionNode typeDefinitionNode = ballerinaSchemaGenerator.getTypeDefinitionNode
                    (responseMessageWithIdSchema, MESSAGE_WITH_ID, new ArrayList<>());
            GeneratorUtils.updateTypeDefNodeList(MESSAGE_WITH_ID, typeDefinitionNode, typeDefinitionNodeList);
        }

        List<String> pipeIdMethods = new ArrayList<>();
        ArrayList<String> streamReturns = new ArrayList<>();
        List<FunctionDefinitionNode> remoteFunctionNodes = createRemoteFunctions(streamReturns, pipeIdMethods);

        boolean isStreamPresent = !streamReturns.isEmpty();

        List<Node> memberNodeList = new ArrayList<>(createClassInstanceVariables(isStreamPresent));

        memberNodeList.add(createInitFunction(isStreamPresent));
        memberNodeList.add(createGetRecordNameFunction());
        memberNodeList.add(createGetPipeNameFunction());
        memberNodeList.add(createStartMessageWriting());
        memberNodeList.add(createStartMessageReading());
        memberNodeList.addAll(remoteFunctionNodes);
        MetadataNode metadataNode = getClassMetadataNode();
        String titleName = asyncApi.getInfo().getTitle().trim().replaceAll("\\s", "");
        String channelName = GeneratorUtils.removeNonAlphanumeric(asyncApi.getChannels().getItemNames().get(0).trim());
        String stringClassName = titleName + channelName + CLIENT_CLASS_NAME;

        setClientName(stringClassName);
        IdentifierToken className = createIdentifierToken(stringClassName);
        NodeList<Token> classTypeQualifiers = createNodeList(createToken(CLIENT_KEYWORD),
                createToken(ISOLATED_KEYWORD));

        return createClassDefinitionNode(metadataNode, createToken(PUBLIC_KEYWORD), classTypeQualifiers,
                createToken(CLASS_KEYWORD), className, openBraceToken, createNodeList(memberNodeList),
                closeBraceToken, null);
    }

    private Node createStartMessageReading() {

        //Create function signature node with metadata documentation
        FunctionSignatureNode functionSignatureNode = getStartMessageReadingFunctionSignatureNode();

        //Create function body node
        FunctionBodyNode functionBodyNode = getStartMessageReadingFunctionBodyNode();

        //Create function name
        NodeList<Token> qualifierList = createNodeList(createToken(PRIVATE_KEYWORD), createToken(ISOLATED_KEYWORD));
        IdentifierToken functionName = createIdentifierToken(START_MESSAGE_READING);

        //Return function
        return createFunctionDefinitionNode(SyntaxKind.OBJECT_METHOD_DEFINITION,
                getDocCommentsForWorker(X_BALLERINA_MESSAGE_READ_DESCRIPTION, START_MESSAGE_READING_DESCRIPTION),
                qualifierList, createToken(FUNCTION_KEYWORD), functionName, createEmptyNodeList(),
                functionSignatureNode, functionBodyNode);
    }

    private FunctionBodyNode getStartMessageReadingFunctionBodyNode() {

        List<StatementNode> whileStatements = new ArrayList<>();
        whileStatements.add(getIsActiveCheck());

        // Expected ballerina statement:
        // Message|websocket:Error message = self.clientEp->readMessage();
        FieldAccessExpressionNode clientEp = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(SELF)), dotToken,
                createSimpleNameReferenceNode(createIdentifierToken(CLIENT_EP)));
        MethodCallExpressionNode responseMessageExpressionNode = createMethodCallExpressionNode(clientEp,
                rightArrowToken, createSimpleNameReferenceNode(createIdentifierToken(READ_MESSAGE)), openParenToken,
                        createSeparatedNodeList(createIdentifierToken(MESSAGE)), closeParenToken);
        VariableDeclarationNode responseMessage = createVariableDeclarationNode(createEmptyNodeList(),
                null, createTypedBindingPatternNode(
                        createUnionTypeDescriptorNode(createBuiltinSimpleNameReferenceNode(null,
                                        createIdentifierToken(MESSAGE)), createToken(PIPE_TOKEN), WS_ERROR),
                        createFieldBindingPatternVarnameNode(createSimpleNameReferenceNode(createIdentifierToken(
                                MESSAGE_VAR_NAME)))), equalToken, responseMessageExpressionNode, semicolonToken);

        whileStatements.add(responseMessage);
        whileStatements.add(getIsWsError(READ_MESSAGE_CLIENT_READ_ERROR, MESSAGE_VAR_NAME));

        // Expected ballerina statement:
        // string pipeName = self.getPipeName(message.event);
        StatementNode getPipeNameStatement = NodeParser.parseStatement(String.format(GET_PIPE_NAME_STATEMENT,
                escapeIdentifier(dispatcherKey)));

        // Expected ballerina statement:
        // pipe:Pipe pipe = self.pipes.getPipe(pipeName);
        String messageEventAccessor = String.format(SELF_PIPES_GET_PIPE, PIPE_NAME);
        if (Objects.isNull(this.dispatcherStreamId)) {
            VariableDeclarationNode pipesVar = createVariableDeclarationNode(createEmptyNodeList(), null,
                    createTypedBindingPatternNode(NodeParser.parseTypeDescriptor(PIPE_COLON_PIPE),
                            createFieldBindingPatternVarnameNode(createSimpleNameReferenceNode(
                                    createIdentifierToken(SIMPLE_PIPE)))), equalToken,
                    NodeParser.parseExpression(messageEventAccessor), semicolonToken);
            whileStatements.add(getPipeNameStatement);
            whileStatements.add(pipesVar);
        } else {
            whileStatements.add(NodeParser.parseStatement(PIPE_VAR + SEMICOLON));
            whileStatements.add(NodeParser.parseStatement(MESSAGE_WITH_ID_VAR_CLONE));
            IfElseStatementNode pipeConditional = createIfElseStatementNode(createToken(IF_KEYWORD),
                    NodeParser.parseExpression(MESSAGE_WITH_ID_VAR_NAME + IS + MESSAGE_WITH_ID),
                    createBlockStatementNode(openBraceToken, createNodeList(NodeParser.parseStatement(
                            SIMPLE_PIPE + EQUAL_SPACE + String.format(SELF_PIPES_GET_PIPE,
                                    MESSAGE_WITH_ID_VAR_NAME + DOT + escapeIdentifier(this.dispatcherStreamId)) +
                                    SEMICOLON)), closeBraceToken),
                    createElseBlockNode(createToken(ELSE_KEYWORD), createBlockStatementNode(openBraceToken,
                            createNodeList(getPipeNameStatement, NodeParser.parseStatement(SIMPLE_PIPE + EQUAL_SPACE +
                                    messageEventAccessor + SEMICOLON)), closeBraceToken)));
            whileStatements.add(pipeConditional);
        }

        // Expected ballerina statement:
        // pipe:Error? pipeErr = pipe.produce(message, 5);
        MethodCallExpressionNode produceExpression = createMethodCallExpressionNode(createBasicLiteralNode(VAR_KEYWORD,
                        createIdentifierToken(SIMPLE_PIPE)), dotToken, createSimpleNameReferenceNode(
                                createIdentifierToken(PRODUCE)), openParenToken, createSeparatedNodeList(
                                createPositionalArgumentNode(createRequiredExpressionNode(createIdentifierToken(
                                        MESSAGE_VAR_NAME))), createToken(COMMA_TOKEN),
                                        createPositionalArgumentNode(createRequiredExpressionNode(createIdentifierToken(
                                                DEFAULT_PIPE_TIME_OUT)))), closeParenToken);
        VariableDeclarationNode pipeErrVar = createVariableDeclarationNode(createEmptyNodeList(), null,
                createTypedBindingPatternNode(createOptionalTypeDescriptorNode(PIPE_ERROR_NODE,
                                createToken(QUESTION_MARK_TOKEN)), createFieldBindingPatternVarnameNode(
                                        createSimpleNameReferenceNode(createIdentifierToken(PIPE_ERR)))),
                equalToken, produceExpression, semicolonToken);

        whileStatements.add(pipeErrVar);
        whileStatements.add(getIsPipeError(PIPE_ERR, READ_MESSAGE_PIPE_PRODUCE_ERROR, false));

        BlockStatementNode whileBody = createBlockStatementNode(openBraceToken, createNodeList(whileStatements),
                closeBraceToken);
        NodeList<StatementNode> workerStatements = createNodeList(createWhileStatementNode(createToken(WHILE_KEYWORD),
                createBasicLiteralNode(SyntaxKind.BOOLEAN_LITERAL, createToken(SyntaxKind.TRUE_KEYWORD)), whileBody,
                null));

        NodeList workerDeclarationNodes = createNodeList(createNamedWorkerDeclarationNode(createEmptyNodeList(),
                null, createToken(WORKER_KEYWORD), createIdentifierToken(READ_MESSAGE),
                null, createBlockStatementNode(openBraceToken, workerStatements, closeBraceToken), null));

        return createFunctionBodyBlockNode(openBraceToken, null, workerDeclarationNodes, closeBraceToken,
                null);
    }

    private FunctionSignatureNode getStartMessageReadingFunctionSignatureNode() {
        SeparatedNodeList<ParameterNode> parameterList = createSeparatedNodeList(new ArrayList<>());
        return createFunctionSignatureNode(openParenToken, parameterList, closeParenToken, null);
    }

    private AsyncApi25SchemaImpl createResponseMessageWithIDRecord(String dispatcherKey, String dispatcherStreamId) {
        //create MessageWithID record
        AsyncApi25SchemaImpl responseMessageWithId = new AsyncApi25SchemaImpl();
        responseMessageWithId.setType(OBJECT);
        AsyncApi25SchemaImpl stringEventSchema = new AsyncApi25SchemaImpl();
        AsyncApi25SchemaImpl stringIdSchema = new AsyncApi25SchemaImpl();
        stringEventSchema.setType(STRING);
        stringIdSchema.setType(STRING);
        List<String> requiredFields = new ArrayList<>();
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
        List<String> requiredFields = new ArrayList<>();
        requiredFields.add(dispatcherKey);
        message.setRequired(requiredFields);
        message.addProperty(dispatcherKey, stringEventSchema);
        message.addExtension(READ_ONLY, BooleanNode.TRUE);
        return message;
    }

    private Node createGetPipeNameFunction() {
        return NodeParser.parseObjectMember(GET_PIPE_NAME_FUNCTION_TEMPLATE);
    }

    private Node createGetRecordNameFunction() {
        return NodeParser.parseObjectMember(GET_RECORD_NAME_FUNCTION_TEMPLATE);
    }

    private Node createStartMessageWriting() {
        FunctionSignatureNode functionSignatureNode = getStartMessageWritingFunctionSignatureNode();
        FunctionBodyNode functionBodyNode = getStartMessageWritingFunctionBodyNode();
        NodeList<Token> qualifierList = createNodeList(createToken(PRIVATE_KEYWORD), createToken(ISOLATED_KEYWORD));
        IdentifierToken functionName = createIdentifierToken(START_MESSAGE_WRITING);
        return createFunctionDefinitionNode(SyntaxKind.OBJECT_METHOD_DEFINITION,
                getDocCommentsForWorker(X_BALLERINA_MESSAGE_WRITE_DESCRIPTION, START_MESSAGE_WRITING_DESCRIPTION),
                qualifierList, createToken(FUNCTION_KEYWORD), functionName, createEmptyNodeList(),
                functionSignatureNode, functionBodyNode);
    }

    private FunctionBodyNode getStartMessageWritingFunctionBodyNode() {
        NodeList<AnnotationNode> annotations = createEmptyNodeList();

        // anydata|pipe:Error requestMessage = self.writeMessageQueue.consume(5);
        FieldAccessExpressionNode globalQueue = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(SELF)), dotToken,
                createSimpleNameReferenceNode(createIdentifierToken(WRITE_MESSAGE_QUEUE)));
        MethodCallExpressionNode consumeExpression = createMethodCallExpressionNode(globalQueue, dotToken,
                        createSimpleNameReferenceNode(createIdentifierToken(CONSUME)), openParenToken,
                        createSeparatedNodeList(createPositionalArgumentNode(createRequiredExpressionNode(
                                        createIdentifierToken(DEFAULT_PIPE_TIME_OUT)))), closeParenToken);
        VariableDeclarationNode queueData = createVariableDeclarationNode(createEmptyNodeList(), null,
                createTypedBindingPatternNode(createUnionTypeDescriptorNode(createSimpleNameReferenceNode(
                        createIdentifierToken(MESSAGE)), createToken(PIPE_TOKEN), PIPE_ERROR_NODE),
                        createFieldBindingPatternVarnameNode(createSimpleNameReferenceNode(createIdentifierToken(
                                MESSAGE_VAR_NAME)))), equalToken, consumeExpression, semicolonToken);

        // self.clientEp->writeMessage(requestMessage);
        FieldAccessExpressionNode clientEp = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(SELF)), dotToken,
                createSimpleNameReferenceNode(createIdentifierToken(CLIENT_EP)));
        MethodCallExpressionNode writeMessageExpression = createMethodCallExpressionNode(clientEp, rightArrowToken,
                        createSimpleNameReferenceNode(createIdentifierToken(WRITE_MESSAGE)), openParenToken,
                        createSeparatedNodeList(
                                createPositionalArgumentNode(createRequiredExpressionNode(
                                        createIdentifierToken(MESSAGE_VAR_NAME)))), closeParenToken);
        VariableDeclarationNode writeMessage = createVariableDeclarationNode(createEmptyNodeList(), null,
                createTypedBindingPatternNode(WS_ERROR_OPTIONAL,
                        createFieldBindingPatternVarnameNode(createSimpleNameReferenceNode(
                                createIdentifierToken(WS_ERR)))), equalToken, writeMessageExpression, semicolonToken);

        List<StatementNode> whileStatements = new ArrayList<>();
        whileStatements.add(getIsActiveCheck());
        whileStatements.add(queueData);
        whileStatements.add(getIsPipeError(MESSAGE_VAR_NAME, WRITE_MESSAGE_PIPE_CONSUME_ERROR, true));
        whileStatements.add(writeMessage);
        whileStatements.add(getIsWsError(WRITE_MESSAGE_CLIENT_WRITE_ERROR, WS_ERR));

        BlockStatementNode whileBody = createBlockStatementNode(openBraceToken, createNodeList(whileStatements),
                closeBraceToken);

        NodeList<StatementNode> workerStatements = createNodeList(createWhileStatementNode(createToken(WHILE_KEYWORD),
                createBasicLiteralNode(SyntaxKind.BOOLEAN_LITERAL, createToken(SyntaxKind.TRUE_KEYWORD)), whileBody,
                null));

        NodeList workerDeclarationNodes = createNodeList(createNamedWorkerDeclarationNode(
                annotations, null, createToken(WORKER_KEYWORD), createIdentifierToken(WRITE_MESSAGE), null,
                createBlockStatementNode(openBraceToken, workerStatements, closeBraceToken), null));

        return createFunctionBodyBlockNode(openBraceToken, null, workerDeclarationNodes, closeBraceToken, null);
    }

    private static StatementNode getIsActiveCheck() {
        //        lock {
        //            if !self.isActive {
        //                break;
        //            }
        //        }
        NodeList<StatementNode> ifIsActiveNode = createNodeList(createIfElseStatementNode(createToken(IF_KEYWORD),
                createSimpleNameReferenceNode(createIdentifierToken(NOT + SELF + DOT + IS_ACTIVE)),
                createBlockStatementNode(openBraceToken, createNodeList(createBreakStatementNode(
                        createToken(BREAK_KEYWORD), semicolonToken)), closeBraceToken), null));
        return createLockStatementNode(createToken(LOCK_KEYWORD),
                createBlockStatementNode(openBraceToken, ifIsActiveNode, closeBraceToken), null);
    }

    private static IfElseStatementNode getIsPipeError(String errVar, String errorMessageTemplate,
                                                      boolean checkTimeout) {
        //        if requestMessage is pipe:Error {
        //            if (requestMessage.message() == "Operation has timed out") {
        //                continue;
        //            }
        //            log:printError("[writeMessage]PipeError: " + requestMessage.message());
        //            self.attemptToCloseConnection();
        //            return;
        //        }
        ArrayList<StatementNode> ifStatements = new ArrayList<>();
        if (checkTimeout) {
            StatementNode continueStatementNode = createContinueStatementNode(createToken(CONTINUE_KEYWORD),
                    semicolonToken);
            IfElseStatementNode ifTimeOutErrorNode = createIfElseStatementNode(createToken(IF_KEYWORD),
                    createBinaryExpressionNode(BINARY_EXPRESSION, createMethodCallExpressionNode(
                                    createSimpleNameReferenceNode(createIdentifierToken(errVar)), dotToken,
                                    createSimpleNameReferenceNode(createIdentifierToken(MESSAGE_VAR_NAME)),
                                    openParenToken, createSeparatedNodeList(), closeParenToken),
                            createToken(DOUBLE_EQUAL_TOKEN), OP_TIMEOUT_EXPR),
                    createBlockStatementNode(openBraceToken, createNodeList(continueStatementNode), closeBraceToken),
                    null);
            ifStatements.add(ifTimeOutErrorNode);
        }

        StatementNode logPrintError = NodeParser.parseStatement(String.format(LOG_PRINT_ERR,
                String.format(errorMessageTemplate, errVar)));

        ifStatements.add(logPrintError);
        ifStatements.add(ATTEMPT_CON_CLOSE);

        ReturnStatementNode returnStatement = createReturnStatementNode(createToken(RETURN_KEYWORD), null,
                semicolonToken);

        ifStatements.add(returnStatement);
        return createIfElseStatementNode(createToken(IF_KEYWORD),
                createSimpleNameReferenceNode(createIdentifierToken(errVar + IS + PIPE_ERROR_NODE)),
                createBlockStatementNode(openBraceToken, createNodeList(ifStatements), closeBraceToken), null);
    }

    private static IfElseStatementNode getIsWsError(String errMessageTemplate, String errVar) {
        //        if err is websocket:Error {
        //            log:printError("[writeMessage]WsError: " + err.message());
        //            self.attemptToCloseConnection();
        //            return;
        //        }
        ArrayList<StatementNode> ifStatements = new ArrayList<>();

        StatementNode logPrintError = NodeParser.parseStatement(String.format(LOG_PRINT_ERR,
                String.format(errMessageTemplate, errVar)));

        ifStatements.add(logPrintError);

        ifStatements.add(ATTEMPT_CON_CLOSE);

        ReturnStatementNode returnStatement = createReturnStatementNode(createToken(RETURN_KEYWORD), null,
                semicolonToken);

        ifStatements.add(returnStatement);

        return createIfElseStatementNode(createToken(IF_KEYWORD),
                createSimpleNameReferenceNode(createIdentifierToken(errVar + IS + WS_ERROR)),
                createBlockStatementNode(openBraceToken, createNodeList(ifStatements), closeBraceToken), null);
    }

    //TODO: Add metdata for the function
    private FunctionSignatureNode getStartMessageWritingFunctionSignatureNode() {
        SeparatedNodeList<ParameterNode> parameterList = createSeparatedNodeList(new ArrayList<>());
        return createFunctionSignatureNode(openParenToken, parameterList, closeParenToken, null);
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
        if (asyncApi.getInfo().getDescription() != null && !asyncApi.getInfo().getDescription().isBlank()) {
            documentationLines.addAll(DocCommentsGenerator.createAPIDescriptionDoc(
                    asyncApi.getInfo().getDescription(), false));
        }
        MarkdownDocumentationNode apiDoc = createMarkdownDocumentationNode(createNodeList(documentationLines));
        return createMetadataNode(apiDoc, createNodeList(classLevelAnnotationNodes));
    }


    private FunctionDefinitionNode createInitFunction(boolean isStreamPresent)
            throws BallerinaAsyncApiExceptionWs {
        AsyncApi25SchemaImpl headerSchema = new AsyncApi25SchemaImpl();
        AsyncApi25SchemaImpl querySchema = new AsyncApi25SchemaImpl();
        FunctionSignatureNode functionSignatureNode = getInitFunctionSignatureNode(querySchema, headerSchema);
        FunctionBodyNode functionBodyNode = getInitFunctionBodyNode(querySchema, headerSchema, isStreamPresent);
        NodeList<Token> qualifierList = createNodeList(createToken(PUBLIC_KEYWORD), createToken(ISOLATED_KEYWORD));
        IdentifierToken functionName = createIdentifierToken(INIT);
        return createFunctionDefinitionNode(SyntaxKind.OBJECT_METHOD_DEFINITION, getInitDocComment(), qualifierList,
                createToken(FUNCTION_KEYWORD), functionName, createEmptyNodeList(), functionSignatureNode,
                functionBodyNode);
    }

    /**
     * Create function body node of client init function.
     *
     * @return {@link FunctionBodyNode}
     */
    private FunctionBodyNode getInitFunctionBodyNode(AsyncApi25SchemaImpl querySchema,
                                                     AsyncApi25SchemaImpl headerSchema,
                                                     boolean isStreamPresent) throws BallerinaAsyncApiExceptionWs {
        List<StatementNode> assignmentNodes = new ArrayList<>();

        //TODO: Attempt to map auth configurations
        // If both apiKey and httpOrOAuth is supported
        // todo : After revamping
        if (ballerinaAuthConfigGenerator.isHttpApiKey() && ballerinaAuthConfigGenerator.isHttpOrOAuth()) {
            assignmentNodes.add(ballerinaAuthConfigGenerator.handleInitForMixOfApiKeyAndHTTPOrOAuth());
        }

        // self.pipes =new ();
        List<Node> pipesArgumentsList = new ArrayList<>();
        SeparatedNodeList<FunctionArgumentNode> pipesArguments = createSeparatedNodeList(pipesArgumentsList);
        ParenthesizedArgList pipesParenthesizedArgList = createParenthesizedArgList(openParenToken, pipesArguments,
                closeParenToken);
        ImplicitNewExpressionNode pipesExpressionNode = createImplicitNewExpressionNode(createToken(NEW_KEYWORD),
                pipesParenthesizedArgList);

        FieldAccessExpressionNode selfPipes = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(SELF)), dotToken, createSimpleNameReferenceNode(
                        createIdentifierToken(PIPES)));
        AssignmentStatementNode selfPipesAssignmentStatementNode = createAssignmentStatementNode(selfPipes, equalToken,
                pipesExpressionNode, semicolonToken);
        assignmentNodes.add(selfPipesAssignmentStatementNode);

        //TODO: use this as conditionally checking
        // create {@code self.streamGenerators =new ();} assignment node
        if (isStreamPresent) {
            List<Node> streamGeneratorsArgumentsList = new ArrayList<>();
            SeparatedNodeList<FunctionArgumentNode> streamGeneratorsArguments =
                    createSeparatedNodeList(streamGeneratorsArgumentsList);
            ParenthesizedArgList streamGeneratorsParenthesizedArgList = createParenthesizedArgList(openParenToken,
                    streamGeneratorsArguments, closeParenToken);
            ImplicitNewExpressionNode streamGeneratorsExpressionNode =
                    createImplicitNewExpressionNode(createToken(NEW_KEYWORD), streamGeneratorsParenthesizedArgList);

            FieldAccessExpressionNode selfStreamGenerators = createFieldAccessExpressionNode(
                    createSimpleNameReferenceNode(createIdentifierToken(SELF)), dotToken,
                    createSimpleNameReferenceNode(createIdentifierToken(STREAM_GENERATORS)));
            AssignmentStatementNode selfStreamGeneratorsAssignmentStatementNode = createAssignmentStatementNode(
                    selfStreamGenerators, equalToken, streamGeneratorsExpressionNode, semicolonToken);
            assignmentNodes.add(selfStreamGeneratorsAssignmentStatementNode);
        }

        // self.writeMessageQueue = new (1000);
        List<Node> argumentsList = new ArrayList<>();
        FieldAccessExpressionNode selfWriteMessageQueues = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(SELF)), dotToken,
                createSimpleNameReferenceNode(createIdentifierToken(WRITE_MESSAGE_QUEUE)));
        argumentsList.add(createIdentifierToken(QUEUE_DEFAULT_SIZE));
        SeparatedNodeList<FunctionArgumentNode> arguments = createSeparatedNodeList(argumentsList);
        ParenthesizedArgList parenthesizedArgList = createParenthesizedArgList(openParenToken, arguments,
                closeParenToken);
        ImplicitNewExpressionNode expressionNode = createImplicitNewExpressionNode(createToken(NEW_KEYWORD),
                parenthesizedArgList);

        AssignmentStatementNode selfWriteQueueAssignmentStatementNode = createAssignmentStatementNode(
                selfWriteMessageQueues, equalToken, expressionNode, semicolonToken);
        assignmentNodes.add(selfWriteQueueAssignmentStatementNode);

        List<String> channelList = asyncApi.getChannels().getItemNames();
        if (channelList.size() != 1) {
            throw new BallerinaAsyncApiExceptionWs(BALLERINA_WEBSOCKET_DOESNT_SUPPORT_FOR_MULTIPLE_CHANNELS);
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

        // self.clientEp = websocketEp
        FieldAccessExpressionNode selfClientEp = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(SELF)), dotToken,
                createSimpleNameReferenceNode(createIdentifierToken(CLIENT_EP)));
        SimpleNameReferenceNode selfClientEpValue = createSimpleNameReferenceNode(createIdentifierToken(WEBSOCKET_EP));
        AssignmentStatementNode selfWebsocketClientAssignmentStatementNode = createAssignmentStatementNode(
                selfClientEp, equalToken, selfClientEpValue, semicolonToken);
        assignmentNodes.add(selfWebsocketClientAssignmentStatementNode);

        //self.isActive = true;
        ArrayList<String> whileLoopBreakVariables = new ArrayList<>();
        whileLoopBreakVariables.add(IS_ACTIVE);

        for (String whileLoopBreakVariable : whileLoopBreakVariables) {
            addInitsOfWhileLoopBreaksNodes(whileLoopBreakVariable, assignmentNodes, TRUE_KEYWORD.stringValue());
        }

        ArrayList<String> workers = new ArrayList<>();
        workers.add(START_MESSAGE_WRITING);
        workers.add(START_MESSAGE_READING);

        List<Node> workersArgumentsList = new ArrayList<>();
        SeparatedNodeList<FunctionArgumentNode> workersArguments = createSeparatedNodeList(workersArgumentsList);
        for (String worker : workers) {
            ExpressionStatementNode workerNode = createExpressionStatementNode(FUNCTION_CALL,
                    createMethodCallExpressionNode(createSimpleNameReferenceNode(createIdentifierToken(SELF)),
                            dotToken, createSimpleNameReferenceNode(createIdentifierToken(worker)), openParenToken,
                            workersArguments, closeParenToken), semicolonToken);
            assignmentNodes.add(workerNode);

        }

        // Get API key assignment node if authentication mechanism type is only `apiKey`
        if (ballerinaAuthConfigGenerator.isHttpApiKey() && !ballerinaAuthConfigGenerator.isHttpOrOAuth()) {
            assignmentNodes.add(ballerinaAuthConfigGenerator.getApiKeyAssignmentNode());
        }
        if (ballerinaAuthConfigGenerator.isHttpApiKey()) {
            List<String> apiKeyNames = new ArrayList<>();
            apiKeyNames.addAll(ballerinaAuthConfigGenerator.getHeaderApiKeyNameList().values());
            apiKeyNames.addAll(ballerinaAuthConfigGenerator.getQueryApiKeyNameList().values());
            setApiKeyNameList(apiKeyNames);
        }
        ReturnStatementNode returnStatementNode = createReturnStatementNode(createToken(RETURN_KEYWORD), null,
                semicolonToken);
        assignmentNodes.add(returnStatementNode);
        NodeList<StatementNode> statementList = createNodeList(assignmentNodes);
        return createFunctionBodyBlockNode(openBraceToken, null, statementList, closeBraceToken, null);
    }

    private void addInitsOfWhileLoopBreaksNodes(String initName, List<StatementNode> assignmentNodes,
                                                String booleanValue) {
        SimpleNameReferenceNode selfIsMessageWritingValue = createSimpleNameReferenceNode(createIdentifierToken(
                booleanValue));
        FieldAccessExpressionNode selfIsMessageWriting = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(SELF)), dotToken,
                createSimpleNameReferenceNode(createIdentifierToken(initName)));
        AssignmentStatementNode selfIsMessageWritingAssignmentStatementNode =
                createAssignmentStatementNode(selfIsMessageWriting, equalToken, selfIsMessageWritingValue,
                        semicolonToken);
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
            statementsList.add(getMapForParameters(querySchema, MAP_ANY_DATA, QUERY_PARAM));
            ExpressionStatementNode updatedPath;
            if (initalized) {
                updatedPath = GeneratorUtils.getSimpleExpressionStatementNode(STRING + SPACE + MODIFIED_URL +
                        EQUAL_SPACE + SERVICE_URL + PLUS_SPACE + CHECK_PATH_FOR_QUERY_PARAM);
            } else {
                updatedPath = GeneratorUtils.getSimpleExpressionStatementNode(
                        MODIFIED_URL + EQUAL_SPACE + MODIFIED_URL + PLUS_SPACE + CHECK_PATH_FOR_QUERY_PARAM);
            }
            statementsList.add(updatedPath);
        }
        if (headerSchema.getProperties() != null) {
            statementsList.add(getMapForParameters(headerSchema, MAP_STRING, HEADER_PARAM));
            statementsList.add(GeneratorUtils.getSimpleExpressionStatementNode(
                    MAP_STRING + SPACE + CUSTOM_HEADERS + EQUAL_SPACE + GET_COMBINE_HEADERS));
            statementsList.add(GeneratorUtils.getSimpleExpressionStatementNode(CLIENT_CONFIG_CUSTOM_HEADERS +
                    EQUAL_SPACE + CUSTOM_HEADERS));
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
        CaptureBindingPatternNode bindingPattern = createCaptureBindingPatternNode(createIdentifierToken(mapName));
        TypedBindingPatternNode bindingPatternNode = createTypedBindingPatternNode(mapType, bindingPattern);
        Map<String, Schema> properties = parameters.getProperties();
        for (Map.Entry<String, Schema> entry : properties.entrySet()) {
            // Initializer
            IdentifierToken fieldName = createIdentifierToken(DOUBLE_QUOTE + (entry.getKey().trim()) + DOUBLE_QUOTE);
            SimpleNameReferenceNode valueExpr = null;
            if (((AsyncApi25SchemaImpl) entry.getValue()).getType().equals(STRING) || mapName.equals(QUERY_PARAM)) {
                valueExpr = createSimpleNameReferenceNode(
                        createIdentifierToken(mapName + S_DOT + getValidName(entry.getKey().trim(), false)));
            } else if (!((AsyncApi25SchemaImpl) entry.getValue()).getType().equals(STRING) &&
                    mapName.equals(HEADER_PARAM)) {
                valueExpr = createSimpleNameReferenceNode(
                        createIdentifierToken(mapName + S_DOT + getValidName(entry.getKey().trim(), false)
                                + DOT_TO_STRING));
            }

            SpecificFieldNode specificFieldNode = createSpecificFieldNode(null,
                    fieldName, colonToken, valueExpr);
            filedOfMap.add(specificFieldNode);
            filedOfMap.add(createToken(COMMA_TOKEN));
        }

        filedOfMap.remove(filedOfMap.size() - 1);
        MappingConstructorExpressionNode initialize = createMappingConstructorExpressionNode(openBraceToken,
                createSeparatedNodeList(filedOfMap), closeBraceToken);
        return createVariableDeclarationNode(createEmptyNodeList(), null, bindingPatternNode, equalToken,
                initialize, semicolonToken);
    }

    /**
     * This method use to generate Path statement inside the function body node.
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
        return createVariableDeclarationNode(createEmptyNodeList(), null, typedBindingPatternNode, equalToken,
                initializer, semicolonToken);
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
     * @throws BallerinaAsyncApiExceptionWs When invalid server URL is provided
     */
    private FunctionSignatureNode getInitFunctionSignatureNode(
            AsyncApi25SchemaImpl querySchema,
            AsyncApi25SchemaImpl headerSchema)
            throws BallerinaAsyncApiExceptionWs {
        //string serviceUrl = "ws://localhost:9090/payloadV"
        serverURL = getServerURL((AsyncApi25ServersImpl) asyncApi.getServers());
        List<Node> parameters = new ArrayList<>();
        AsyncApiChannelItem channelItem = asyncApi.getChannels().getItems().get(0);

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
        return createFunctionSignatureNode(openParenToken, parameterList, closeParenToken, returnTypeDescriptorNode);
    }

    /**
     * Provide client class init function's documentation including function description and parameter descriptions.
     *
     * @return {@link MetadataNode} Metadata node containing entire function documentation comment.
     */
    private MetadataNode getInitDocComment() {
        List<Node> docs = new ArrayList<>();
        String clientInitDocComment = "Gets invoked to initialize the `connector`.\n";
        Map<String, JsonNode> extensions = ((AsyncApi25InfoImpl) asyncApi.getInfo()).getExtensions();
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
        if (ballerinaAuthConfigGenerator.isHttpApiKey() && !ballerinaAuthConfigGenerator.isHttpOrOAuth()) {
            MarkdownParameterDocumentationLineNode apiKeyConfig = DocCommentsGenerator.createAPIParamDoc(
                    API_KEY_CONFIG, DEFAULT_API_KEY_DESC);
            docs.add(apiKeyConfig);
        }
        // Create method description
        MarkdownParameterDocumentationLineNode clientConfig = DocCommentsGenerator.createAPIParamDoc(CONFIG,
                CONFIG_DESCRIPTION);
        docs.add(clientConfig);
        MarkdownParameterDocumentationLineNode serviceUrlAPI = DocCommentsGenerator.createAPIParamDoc(SERVICE_URL,
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
        Map<String, JsonNode> extensions = ((AsyncApi25InfoImpl) asyncApi.getInfo()).getExtensions();
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
    private List<ObjectFieldNode> createClassInstanceVariables(boolean isStreamPresent) {

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
                colonToken, createIdentifierToken(CLIENT_CLASS_NAME));

        ObjectFieldNode websocketClientField = getObjectFieldNode(qualifiersWithPrivateAndFinal, websocketType,
                CLIENT_EP);

        fieldNodeList.add(websocketClientField);

        QualifiedNameReferenceNode pipeType = createQualifiedNameReferenceNode(createIdentifierToken(SIMPLE_PIPE),
                colonToken, createIdentifierToken(CAPITAL_PIPE));

        //private final pipe:Pipe writeMessageQueue;
        ObjectFieldNode writeMessageQueueClientField = getObjectFieldNode(qualifiersWithPrivateAndFinal, pipeType,
                WRITE_MESSAGE_QUEUE);
        fieldNodeList.add(writeMessageQueueClientField);

        //private final PipesMap pipes;
        SimpleNameReferenceNode pipesType = createSimpleNameReferenceNode(createIdentifierToken(
                GeneratorConstants.PIPES_MAP));
        ObjectFieldNode pipesField = getObjectFieldNode(qualifiersWithPrivateAndFinal, pipesType, PIPES);
        fieldNodeList.add(pipesField);

        if (isStreamPresent) {
            //private final StreamGeneratorsMap streamGenerators;
            SimpleNameReferenceNode streamGeneratorsType =
                    createSimpleNameReferenceNode(createIdentifierToken(GeneratorConstants.STREAM_GENERATORS_MAP));
            ObjectFieldNode streamGeneratorsField = getObjectFieldNode(qualifiersWithPrivateAndFinal,
                    streamGeneratorsType, STREAM_GENERATORS);
            fieldNodeList.add(streamGeneratorsField);
        }

        SimpleNameReferenceNode booleanType =
                createSimpleNameReferenceNode(createIdentifierToken(GeneratorConstants.BOOLEAN));
        //private boolean isActive;
        ObjectFieldNode isActiveField = getObjectFieldNode(qualifiersWithOnlyPrivate, booleanType, IS_ACTIVE);
        fieldNodeList.add(isActiveField);

        // add apiKey instance variable when API key security schema is given
        ObjectFieldNode apiKeyFieldNode = ballerinaAuthConfigGenerator.getApiKeyMapClassVariable();
        if (apiKeyFieldNode != null) {
            fieldNodeList.add(apiKeyFieldNode);
        }

        // Add response map
        String jsonString = responseMap.entrySet().stream()
                .map(entry -> String.format("\"%s\": \"%s\"", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(",\n\t\t", "{\n\t\t", "\n\t}"));

        Node typeDescriptor = NodeParser.parseTypeDescriptor(READONLY + AND_SPACE + MAP_STRING);
        ExpressionNode expression = NodeParser.parseExpression(jsonString);
        ObjectFieldNode responseMapField =
                createObjectFieldNode(null, null, qualifiersWithPrivateAndFinal, typeDescriptor,
                        createIdentifierToken(RESPONSE_MAP), createToken(EQUAL_TOKEN), expression,
                        semicolonToken);
        fieldNodeList.add(responseMapField);
        return fieldNodeList;
    }

    /**
     * Generate remote functions for AsyncAPI messages.
     *
     * @return FunctionDefinitionNodes list
     * @throws BallerinaAsyncApiExceptionWs - throws when creating remote functions fails
     */
    private List<FunctionDefinitionNode> createRemoteFunctions(ArrayList<String> streamReturns,
                                                               List<String> pipeIdMethods)
            throws BallerinaAsyncApiExceptionWs {

        Map<String, AsyncApiMessage> messages = asyncApi.getComponents().getMessages();
        List<FunctionDefinitionNode> functionDefinitionNodeList = new ArrayList<>();

        // Create an array to store all request messages
        ArrayList<String> remainingResponseMessages = new ArrayList<>();

        AsyncApiOperation publishOperation = asyncApi.getChannels().getItems().get(0).getPublish();

        if (!Objects.isNull(publishOperation)) {
            List<AsyncApiMessage> publishMessages = getAsyncApiMessages();
            if (!publishMessages.isEmpty()) {
                ListIterator<AsyncApiMessage> requestMessages = publishMessages.listIterator();
                for (ListIterator<AsyncApiMessage> it = requestMessages; it.hasNext();) {
                    AsyncApi25MessageImpl message = (AsyncApi25MessageImpl) it.next();
                    String reference = message.get$ref();
                    String messageName = GeneratorUtils.extractReferenceType(reference);
                    AsyncApi25MessageImpl componentMessage = (AsyncApi25MessageImpl) messages.get(messageName);
                    Map<String, JsonNode> extensions = componentMessage.getExtensions();
                    extensions = removeCloseFrameFromResponse(extensions);
                    FunctionDefinitionNode functionDefinitionNode;
                    if (extensions != null && extensions.get(X_RESPONSE) != null) {
                         functionDefinitionNode = getRemoteFunctionDefinitionNode(messageName, componentMessage,
                                 extensions, pipeIdMethods, remainingResponseMessages, false, streamReturns);
                    } else {
                        functionDefinitionNode = getRemoteFunctionDefinitionNode(messageName, componentMessage, null,
                                pipeIdMethods, null, false, null);
                    }
                    functionDefinitionNodeList.add(functionDefinitionNode);
                }
            }
        }
        //Set util generator with stream return classes
        setUtilGenerator(new UtilGenerator(streamReturns));
        if (asyncApi.getChannels().getItems().get(0).getSubscribe() != null) {
            List<AsyncApiMessage> subscribeMessages = null;
            if (asyncApi.getChannels().getItems().get(0).getSubscribe().getMessage().getOneOf() != null) {
                subscribeMessages = asyncApi.getChannels().getItems().get(0).getSubscribe().getMessage().getOneOf();
            } else if (asyncApi.getChannels().getItems().get(0).getSubscribe().getMessage() != null) {
                AsyncApiMessage asyncApiMessage = asyncApi.getChannels().getItems().get(0).getSubscribe().getMessage();
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
                        AsyncApi25Schema refSchema = (AsyncApi25Schema) asyncApi.getComponents()
                                .getSchemas().get(messageName);
                        if (isCloseFrameSchema(refSchema)) {
                            continue; // Skip close frame schema
                        }
                        Map<String, JsonNode> extensions = new LinkedHashMap<>();
                        AsyncApi25MessageImpl newMessage = new AsyncApi25MessageImpl();
                        ObjectNode objectNode = new ObjectNode(JsonNodeFactory.instance);
                        objectNode.put(REF, reference);
                        extensions.put(X_RESPONSE, objectNode);
                        newMessage.addExtension(X_RESPONSE, objectNode);
                        extensions.put(X_RESPONSE_TYPE, new TextNode(SIMPLE_RPC));
                        newMessage.addExtension(X_RESPONSE_TYPE, new TextNode(SIMPLE_RPC));
                        FunctionDefinitionNode functionDefinitionNode = getRemoteFunctionDefinitionNode(messageName,
                                newMessage, extensions, pipeIdMethods, null, true, streamReturns);
                        functionDefinitionNodeList.add(functionDefinitionNode);
                    }
                }
            }
        }
        functionDefinitionNodeList.add(createAttemptToCloseConnectionFunction());
        functionDefinitionNodeList.add(createConnectionCloseFunction(!streamReturns.isEmpty()));
        return functionDefinitionNodeList;
    }

    private Map<String, JsonNode> removeCloseFrameFromResponse(Map<String, JsonNode> extensions)
            throws BallerinaAsyncApiExceptionWs {
        if (Objects.isNull(extensions) || Objects.isNull(extensions.get(X_RESPONSE))) {
            return extensions;
        }
        JsonNode xResponse = extensions.get(X_RESPONSE);
        if (Objects.nonNull(xResponse.get(ONEOF)) && xResponse.get(ONEOF) instanceof ArrayNode nodes) {
            ObjectNode newNode = (ObjectNode) extensions.get(X_RESPONSE);
            for (Iterator<JsonNode> it = nodes.iterator(); it.hasNext(); ) {
                JsonNode jsonNode = it.next();
                if (Objects.nonNull(jsonNode.get(REF)) && isCloseFrameRef(jsonNode.get(REF))) {
                    it.remove();
                }
            }
            if (nodes.isEmpty()) {
                return Collections.emptyMap();
            }
            if (nodes.size() == 1) {
                newNode.remove(ONEOF);
                newNode.set(REF, nodes.get(0).get(REF));
            } else {
                newNode.set(ONEOF, nodes);
            }
            extensions.put(X_RESPONSE, newNode);
        } else if (Objects.nonNull(xResponse.get(REF))) {
            if (isCloseFrameRef(xResponse.get(REF))) {
                return Collections.emptyMap();
            }
        }
        return extensions;
    }

    private boolean isCloseFrameRef(JsonNode refNode) throws BallerinaAsyncApiExceptionWs {
        if (Objects.isNull(refNode)) {
            return false;
        }
        String reference = refNode.asText();
        String messageName = extractReferenceType(reference);
        AsyncApiMessage message = asyncApi.getComponents().getMessages().get(messageName);
        TextNode schemaReference = (TextNode) message.getPayload().get(REF);
        String schemaName = extractReferenceType(schemaReference.asText());
        AsyncApiSchema refSchema = (AsyncApiSchema) asyncApi.getComponents().getSchemas().get(schemaName);
        return isCloseFrameSchema(refSchema);
    }

    private List<AsyncApiMessage> getAsyncApiMessages() {
        List<AsyncApiMessage> publishMessages = new ArrayList<>();
        if (asyncApi.getChannels().getItems().get(0).getPublish().getMessage().getOneOf() != null) {
            publishMessages = asyncApi.getChannels().getItems().get(0).getPublish().getMessage().getOneOf();
        } else if (asyncApi.getChannels().getItems().get(0).getPublish().getMessage() != null) {
            AsyncApiMessage asyncApiMessage = asyncApi.getChannels().getItems().get(0).getPublish().getMessage();
            List<AsyncApiMessage> onePublishMessage = new ArrayList<>();
            onePublishMessage.add(asyncApiMessage);
            publishMessages = onePublishMessage;
        }
        return publishMessages;
    }

    private FunctionDefinitionNode createAttemptToCloseConnectionFunction() {
        ArrayList<StatementNode> statements = new ArrayList<>();

        statements.add(NodeParser.parseStatement(CONNECTION_CLOSE_STATEMENT));
        statements.add(createIfElseStatementNode(createToken(IF_KEYWORD),
                createSimpleNameReferenceNode(createIdentifierToken(CONNECTION_CLOSE + IS + ERROR)),
                createBlockStatementNode(openBraceToken, createNodeList(NodeParser.parseStatement(
                        String.format(LOG_PRINT_ERR, CONNECTION_ERR))), closeBraceToken), null));

        return createFunctionDefinitionNode(FUNCTION_DEFINITION, null,
                createNodeList(createToken(ISOLATED_KEYWORD)), createToken(FUNCTION_KEYWORD),
                createIdentifierToken(ATTEMPT_TO_CLOSE_CONNECTION), createEmptyNodeList(),
                createFunctionSignatureNode(openParenToken, createSeparatedNodeList(), closeParenToken,
                        null), createFunctionBodyBlockNode(openBraceToken, null,
                        createNodeList(statements), closeBraceToken, null));
    }

    private FunctionDefinitionNode createConnectionCloseFunction(boolean streamsPresent) {
        ArrayList<StatementNode> lockStatements = new ArrayList<>();
        addInitsOfWhileLoopBreaksNodes(IS_ACTIVE, lockStatements, FALSE_KEYWORD.stringValue());

        //check self.writeMessageQueue.immediateClose();
        ExpressionStatementNode writeMessageStatementNode = getCloseLockStatementNode(WRITE_MESSAGE_QUEUE,
                IMMEDIATE_CLOSE, dotToken);
        lockStatements.add(writeMessageStatementNode);

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
                createBlockStatementNode(openBraceToken, createNodeList(lockStatements), closeBraceToken), null);

        IdentifierToken functionName = createIdentifierToken(CONNECTION_CLOSE);
        return getAdditionalFunctionDefinitionNode(functionName, lockStatementNode);

    }

    private FunctionDefinitionNode getAdditionalFunctionDefinitionNode(IdentifierToken functionName,
                                                                       LockStatementNode lockStatementNode) {
        ReturnTypeDescriptorNode returnTypeDescriptorNode = createReturnTypeDescriptorNode(createToken(RETURNS_KEYWORD),
                createNodeList(), createSimpleNameReferenceNode(createIdentifierToken(OPTIONAL_ERROR)));
        FunctionSignatureNode functionSignatureNode = createFunctionSignatureNode(openParenToken,
                createSeparatedNodeList(), closeParenToken, returnTypeDescriptorNode);

        FunctionBodyNode functionBodyNode = createFunctionBodyBlockNode(openBraceToken, null,
                createNodeList(lockStatementNode), closeBraceToken, semicolonToken);

        MetadataNode metadataNode = createMetadataNode(null, createNodeList());
        NodeList<Token> qualifierList = createNodeList(createToken(REMOTE_KEYWORD), createToken(ISOLATED_KEYWORD));
        Token functionKeyWord = createToken(FUNCTION_KEYWORD);
        return createFunctionDefinitionNode(SyntaxKind.OBJECT_METHOD_DEFINITION,
                metadataNode, qualifierList, functionKeyWord, functionName, createEmptyNodeList(),
                functionSignatureNode, functionBodyNode);
    }

    private ExpressionStatementNode getCloseLockStatementNode(String messageQueue, String closeType,
                                                              Token divideToken) {
        FieldAccessExpressionNode writeMessageQueue = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(SELF)), dotToken,
                createSimpleNameReferenceNode(createIdentifierToken(messageQueue)));
        CheckExpressionNode checkExpressionNode = createCheckExpressionNode(null, createToken(CHECK_KEYWORD),
                createMethodCallExpressionNode(writeMessageQueue, divideToken,
                        createSimpleNameReferenceNode(createIdentifierToken(closeType)), openParenToken,
                        createSeparatedNodeList(), closeParenToken));
        return createExpressionStatementNode(null, checkExpressionNode, semicolonToken);
    }

    /**
     * Generate remote function definition node.
     */
    private FunctionDefinitionNode getRemoteFunctionDefinitionNode(String messageName,
                                                                   AsyncApi25MessageImpl messageValue,
                                                                   Map<String, JsonNode> extensions,
                                                                   List<String> pipeIdMethods,
                                                                   ArrayList<String> responseMessages,
                                                                   boolean isSubscribe, List<String> streamReturns)
            throws BallerinaAsyncApiExceptionWs {

        String specDispatcherStreamId = this.dispatcherStreamId;

        Map<String, Schema> schemas = asyncApi.getComponents().getSchemas();

        // Create api doc for remote function
        List<Node> remoteFunctionDocs = new ArrayList<>();

        //Add documentation for remote function
        if (messageValue.getSummary() != null) {
            remoteFunctionDocs.addAll(DocCommentsGenerator.createAPIDescriptionDoc(messageValue.getSummary(), true));
        } else if (messageValue.getDescription() != null && !messageValue.getDescription().isBlank()) {
            remoteFunctionDocs.addAll(DocCommentsGenerator.createAPIDescriptionDoc(
                    messageValue.getDescription(), true));
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
                RemoteFunctionSignatureGenerator(asyncApi, ballerinaSchemaGenerator, typeDefinitionNodeList);

        List<Node> remoteFunctionDocParameters = new ArrayList<>();
        FunctionSignatureNode functionSignatureNode =
                remoteFunctionSignatureGenerator.getFunctionSignatureNode(messageValue.getPayload(),
                        remoteFunctionDocParameters, extensions, responseType, streamReturns);
        if (!remoteFunctionDocParameters.isEmpty()) {
            if (remoteFunctionDocs.isEmpty()) {
                MarkdownDocumentationLineNode newLine = createMarkdownDocumentationLineNode(null,
                        createToken(SyntaxKind.HASH_TOKEN), createEmptyNodeList());
                remoteFunctionDocs.add(newLine);
            }
            remoteFunctionDocs.addAll(remoteFunctionDocParameters);
        }
        typeDefinitionNodeList = remoteFunctionSignatureGenerator.getTypeDefinitionNodeList();

        // Create metadataNode add documentation string
        MetadataNode metadataNode = createMetadataNode(createMarkdownDocumentationNode(
                createNodeList(remoteFunctionDocs)), createNodeList());

        // Create remote Function Body
        String functionNameString = REMOTE_METHOD_NAME_PREFIX + getValidName(messageName, true);
        RemoteFunctionBodyGenerator remoteFunctionBodyGenerator = new RemoteFunctionBodyGenerator(imports,
                functionNameString);
        boolean schemaDispatcherStreamIdContains = false;
        if (messageValue.getPayload() != null) {
            JsonNode jsonNode = messageValue.getPayload();
            TextNode textNode = (TextNode) jsonNode.get(REF);
            String schemaName = GeneratorUtils.extractReferenceType(textNode.asText());
            AsyncApi25SchemaImpl schema = (AsyncApi25SchemaImpl) schemas.get(schemaName);
            CommonFunctionUtils commonFunctionUtils = new CommonFunctionUtils(asyncApi);
            schemaDispatcherStreamIdContains = commonFunctionUtils.isDispatcherPresent(schemaName, schema,
                    specDispatcherStreamId, true);
        }

        //Check if the schema has dispatcherStreamId
        if ((!Objects.isNull(specDispatcherStreamId) && !schemaDispatcherStreamIdContains)) {
            //If no dispatcherStreamId found from the schema
            specDispatcherStreamId = null;
            //Check if the schema has dispatcherStreamId
        } else if (!Objects.isNull(specDispatcherStreamId)) {
            //If found at least one dispatcherStreamId then don't add uuid again and again
            pipeIdMethods.add(messageName);
            ImportDeclarationNode importForUUID = GeneratorUtils.
                    getImportDeclarationNode(GeneratorConstants.BALLERINA, UUID);
            if (pipeIdMethods.size() == 1) {
                // TODO: Add this after generated-stream-id flag implementation
//                imports.add(importForUUID);
            }
        }

        String requestTypeCamelCaseName = Character.toLowerCase(messageName.charAt(0)) + messageName.substring(1);
        FunctionBodyNode functionBodyNode = remoteFunctionBodyGenerator.getFunctionBodyNode(extensions,
                requestTypeCamelCaseName, specDispatcherStreamId, isSubscribe, responseType);

        // Add types to response map
        if (responseType != null) {
            if (responseType.contains("|")) {
                String[] responseTypes = responseType.split("\\|");
                for (String type : responseTypes) {
                    this.responseMap.put(type, requestTypeCamelCaseName);
                }
            } else {
                this.responseMap.put(responseType, requestTypeCamelCaseName);
            }
        }

        //Create remote function details
        NodeList<Token> qualifierList = createNodeList(createToken(REMOTE_KEYWORD), createToken(ISOLATED_KEYWORD));
        Token functionKeyWord = createToken(FUNCTION_KEYWORD);
        IdentifierToken functionName = createIdentifierToken(functionNameString);

        //Create whole remote function
        return createFunctionDefinitionNode(SyntaxKind.OBJECT_METHOD_DEFINITION, metadataNode, qualifierList,
                functionKeyWord, functionName, createEmptyNodeList(), functionSignatureNode, functionBodyNode);
    }

    /**
     * Generate serverUrl for client default value.
     */
    private String getServerURL(AsyncApi25ServersImpl servers) throws BallerinaAsyncApiExceptionWs {
        String serverURL;
        if (!Objects.isNull(servers)) {
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
                    throw new BallerinaAsyncApiExceptionWs(FAIL_TO_READ_ENDPOINT_DETAILS + selectedServer.getUrl(), e);
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
        this.apiKeyNameList = Collections.unmodifiableList(apiKeyNameList);
    }
}
