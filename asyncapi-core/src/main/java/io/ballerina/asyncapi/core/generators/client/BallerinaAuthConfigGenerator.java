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

import io.apicurio.datamodels.models.SecurityScheme;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25DocumentImpl;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25SecuritySchemeImpl;
import io.ballerina.asyncapi.core.GeneratorConstants;
import io.ballerina.asyncapi.core.exception.BallerinaAsyncApiException;
import io.ballerina.compiler.syntax.tree.*;
import io.ballerina.asyncapi.core.generators.document.DocCommentsGenerator;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static io.ballerina.asyncapi.core.GeneratorConstants.*;
import static io.ballerina.asyncapi.core.GeneratorUtils.escapeIdentifier;
import static io.ballerina.asyncapi.core.GeneratorUtils.getValidName;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.SECURE_SOCKET;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.STRING;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyMinutiaeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createLiteralValueToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.*;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.*;

/**
 * This class is used to generate authentication related nodes of the ballerina connector client syntax tree.
 *
 * @since 1.3.0
 */
public class BallerinaAuthConfigGenerator {

    private final Map<String, String> headerApiKeyNameList = new HashMap<>();
    private final Map<String, String> queryApiKeyNameList = new HashMap<>();
    private final List<Node> apiKeysConfigRecordFields = new ArrayList<>();
    private final Set<String> authTypes = new LinkedHashSet<>();
    private boolean httpApiKey;
    private boolean httpOROAuth;
    private String clientCredGrantTokenUrl;
    private String passwordGrantTokenUrl;
    private String refreshTokenUrl;
    private List<TypeDefinitionNode> authRelatedTypeDefinitionNodes = new ArrayList<>();

    public BallerinaAuthConfigGenerator(boolean isAPIKey, boolean isHttpOROAuth) {
        this.httpApiKey = isAPIKey;
        this.httpOROAuth = isHttpOROAuth;
    }

    /**
     * Returns `true` if authentication mechanism is API key.
     *
     * @return {@link boolean}    values of the flag isAPIKey
     */
    public boolean isHttpApiKey() {
        return httpApiKey;
    }

    /**
     * Returns `true` if HTTP or OAuth authentication is supported.
     *
     * @return {@link boolean}
     */
    public boolean isHttpOROAuth() {

        return httpOROAuth;
    }

    /**
     * Returns API key names which need to send in the query string.
     *
     * @return {@link List<String>}    API key name list
     */
    public Map<String, String> getQueryApiKeyNameList() {
        return queryApiKeyNameList;
    }

    /**
     * Returns API key names which need to send as request headers.
     *
     * @return {@link List<String>}    API key name list
     */
    public Map<String, String> getHeaderApiKeyNameList() {
        return headerApiKeyNameList;
    }

    /**
     * Returns auth type to generate test file.
     *
     * @return {@link Set<String>}
     */
    public Set<String> getAuthType() {

        return authTypes;
    }

    public List<TypeDefinitionNode> getAuthRelatedTypeDefinitionNodes() {
        return authRelatedTypeDefinitionNodes;
    }

    /**
     * Add authentication related records.
     *
     * @param asyncAPI AsyncAPI object received from apicurio parser
     * @throws BallerinaAsyncApiException When function fails
     */
    public void addAuthRelatedRecords(AsyncApi25DocumentImpl asyncAPI) throws
            BallerinaAsyncApiException {
        List<TypeDefinitionNode> nodes = new ArrayList<>();
        if (asyncAPI.getComponents() != null) {
            // set auth types
            if (asyncAPI.getComponents().getSecuritySchemes() != null) {
                Map<String, SecurityScheme> securitySchemeMap = asyncAPI.getComponents().getSecuritySchemes();
                setAuthTypes(securitySchemeMap);
            }

            // generate related records
            TypeDefinitionNode connectionConfigRecord = generateConnectionConfigRecord();
//            TypeDefinitionNode clientHttp1SettingsRecord = getClientHttp1SettingsRecord();
//            TypeDefinitionNode customProxyConfigRecord = getCustomProxyRecord();
//            nodes.addAll(Arrays.asList(connectionConfigRecord, clientHttp1SettingsRecord, customProxyConfigRecord));
            nodes.add(connectionConfigRecord);


            if (isHttpApiKey()) {
                nodes.add(generateApiKeysConfig());
            }

            // Add custom `OAuth2ClientCredentialsGrantConfig` record with default tokenUrl if `tokenUrl` is available
            if (clientCredGrantTokenUrl != null) {
                nodes.add(getOAuth2ClientCredsGrantConfigRecord());
            }

            // Add custom `OAuth2PasswordGrantConfig` record with default tokenUrl if `tokenUrl` is available
            if (passwordGrantTokenUrl != null) {
                nodes.add(getOAuth2PasswordGrantConfigRecord());
            }

            // Add custom `OAuth2RefreshTokenGrantConfig` record with default refreshUrl if `refreshUrl` is available
            if (refreshTokenUrl != null) {
                nodes.add(getOAuth2RefreshTokenGrantConfigRecord());
            }
        }
        this.authRelatedTypeDefinitionNodes = nodes;
    }

    /**
     * Generate the Config record for the relevant authentication type.
     * -- ex: Config record for Http and OAuth 2.0 Authentication mechanisms.
     * <pre>
     * # Provides a set of configurations for controlling the behaviours when communicating with a remote WEBSOCKET
     * service endpoint.
     * public type ConnectionConfig record {|
     *          # Configurations related to client authentication
     *          websocket:BearerTokenConfig|websocket:OAuth2RefreshTokenGrantConfig auth;
     *          # Negotiable sub protocols of the client
     *          string[] subProtocols = [];
     *          # Custom headers, which should be sent to the server
     *          map<string> customHeaders = {};
     *          # Read timeout (in seconds) of the client
     *          decimal readTimeout = -1;
     *          # Write timeout (in seconds) of the client
     *          decimal writeTimeout = -1;
     *          # SSL/TLS-related options
     *          websocket:ClientSecureSocket? secureSocket = ();
     *          # The maximum payload size of a WebSocket frame in bytes
     *          int maxFrameSize = 65536;
     *          # Enable support for compression in the WebSocket
     *          boolean webSocketCompressionEnabled = true;
     *          # Time (in seconds) that a connection waits to get the response of the WebSocket handshake.
     *          decimal handShakeTimeout = 300;
     *          # An Array of http:Cookie
     *          http:Cookie[] cookies?;
     *          # A service to handle the ping/pong frames.
     *          PingPongService pingPongHandler?;
     *          # Configurations associated with retrying
     *          websocket:WebSocketRetryConfig retryConfig? = ();
     *          Enable/disable constraint validation
     *          boolean validation = true;Z
     * |};
     * </pre>
     * Scenario 1 : For asyncapi contracts with no authentication mechanism given, auth field will not be generated
     * Scenario 2 : For asyncapi contracts with authentication mechanism, auth field in relevant types will be generated
     * Scenario 3 : For asyncapi contracts with only apikey authentication mechanism, auth field will not be generated
     * Scenario 4 : For asyncapi contracts with both http and apikey authentication mechanisms given,
     * auth field in relevant types will be generated
     *
     * @return {@link TypeDefinitionNode}   Syntax tree node of config record
     */
    public TypeDefinitionNode generateConnectionConfigRecord() {

//        AnnotationNode annotationNode = getDisplayAnnotationForRecord("Connection Config");
//        MetadataNode configRecordMetadataNode = getMetadataNode(
//                "Provides a set of configurations for controlling the behaviours when communicating " +
//                        "with a remote HTTP endpoint.", Collections.singletonList(annotationNode));
        Token typeName = AbstractNodeFactory.createIdentifierToken(CONNECTION_CONFIG);
        NodeList<Node> recordFieldList = createNodeList(getClientConfigRecordFields());

        RecordTypeDescriptorNode recordTypeDescriptorNode =
                NodeFactory.createRecordTypeDescriptorNode(createToken(RECORD_KEYWORD),
                        createToken(OPEN_BRACE_PIPE_TOKEN), recordFieldList, null,
                        createToken(CLOSE_BRACE_PIPE_TOKEN));
//        TypeDefinitionNode node = NodeFactory.createTypeDefinitionNode(configRecordMetadataNode,
//                createToken(PUBLIC_KEYWORD), createToken(TYPE_KEYWORD), typeName,
//                recordTypeDescriptorNode, createToken(SEMICOLON_TOKEN));
        TypeDefinitionNode node = NodeFactory.createTypeDefinitionNode(null,
                createToken(PUBLIC_KEYWORD), createToken(TYPE_KEYWORD), typeName,
                recordTypeDescriptorNode, createToken(SEMICOLON_TOKEN));
        return node;

    }

    /**
     * Generate the ApiKeysConfig record when the http-api-key auth type is given.
     * <pre>
     *  # Provides API key configurations needed when communicating with a remote WEBSOCKET service endpoint.
     *  public type ApiKeysConfig record {|
     *     # Represents API Key `Authorization`
     *     string authorization;
     *     # Represents API Key `apikey`
     *     string apikey;
     *  |};
     * </pre>
     *
     * @return {@link TypeDefinitionNode}   Syntax tree node of config record
     */
    public TypeDefinitionNode generateApiKeysConfig() {
        MetadataNode configRecordMetadataNode = getMetadataNode(
                "Provides API key configurations needed when communicating " +
                        "with a remote WEBSOCKET service endpoint.");
        Token typeName = AbstractNodeFactory.createIdentifierToken(API_KEYS_CONFIG);
        NodeList<Node> recordFieldList = createNodeList(apiKeysConfigRecordFields);
        RecordTypeDescriptorNode recordTypeDescriptorNode =
                NodeFactory.createRecordTypeDescriptorNode(createToken(RECORD_KEYWORD),
                        createToken(OPEN_BRACE_PIPE_TOKEN), recordFieldList, null,
                        createToken(CLOSE_BRACE_PIPE_TOKEN));
        return NodeFactory.createTypeDefinitionNode(configRecordMetadataNode,
                createToken(PUBLIC_KEYWORD), createToken(TYPE_KEYWORD), typeName,
                recordTypeDescriptorNode, createToken(SEMICOLON_TOKEN));
    }

    /**
     * Create `OAuth2ClientCredentialsGrantConfig` record with default tokenUrl.
     *
     * <pre>
     *      # OAuth2 Client Credentials Grant Configs
     *      public type OAuth2ClientCredentialsGrantConfig record {|
     *          *websocket:OAuth2ClientCredentialsGrantConfig;
     *          # Token URL
     *          string tokenUrl = "https://zoom.us/oauth/token";
     *      |};
     * </pre>
     *
     * @return {@link TypeDefinitionNode}   Custom `OAuth2ClientCredentialsGrantConfig` record with default tokenUrl
     */
    private TypeDefinitionNode getOAuth2ClientCredsGrantConfigRecord() {
        Token typeName = AbstractNodeFactory.createIdentifierToken(AuthConfigTypes.CUSTOM_CLIENT_CREDENTIAL.getValue());
        NodeList<Node> recordFieldList = createNodeList(getClientCredsGrantConfigFields());
        MetadataNode configRecordMetadataNode = getMetadataNode("OAuth2 Client Credentials Grant Configs");
        RecordTypeDescriptorNode recordTypeDescriptorNode =
                NodeFactory.createRecordTypeDescriptorNode(createToken(RECORD_KEYWORD),
                        createToken(OPEN_BRACE_PIPE_TOKEN), recordFieldList, null,
                        createToken(CLOSE_BRACE_PIPE_TOKEN));
        return NodeFactory.createTypeDefinitionNode(configRecordMetadataNode,
                createToken(PUBLIC_KEYWORD), createToken(TYPE_KEYWORD), typeName,
                recordTypeDescriptorNode, createToken(SEMICOLON_TOKEN));
    }

    /**
     * Generates fields of `OAuth2ClientCredentialsGrantConfig` record with default tokenUrl.
     *
     * <pre>
     *      *websocket:OAuth2ClientCredentialsGrantConfig;
     *      # Token URL
     *      string tokenUrl = "https://zoom.us/oauth/token";
     * </pre>
     *
     * @return {@link List<Node>}
     */
    private List<Node> getClientCredsGrantConfigFields() {
        List<Node> recordFieldNodes = new ArrayList<>();
        Token semicolonToken = createToken(SEMICOLON_TOKEN);
        Token equalToken = createToken(EQUAL_TOKEN);

        recordFieldNodes.add(createIncludedRecordParameterNode(createEmptyNodeList(),
                createToken(ASTERISK_TOKEN),
                createIdentifierToken("websocket:OAuth2ClientCredentialsGrantConfig;"), null));

        MetadataNode metadataNode = getMetadataNode("Token URL");
        TypeDescriptorNode stringType = createSimpleNameReferenceNode(createToken(STRING_KEYWORD));
        IdentifierToken fieldNameTokenUrl = createIdentifierToken("tokenUrl");
        ExpressionNode defaultValue = createRequiredExpressionNode(createIdentifierToken("\"" +
                clientCredGrantTokenUrl + "\""));
        RecordFieldWithDefaultValueNode fieldNode = NodeFactory.createRecordFieldWithDefaultValueNode(
                metadataNode, null, stringType, fieldNameTokenUrl, equalToken, defaultValue,
                semicolonToken);
        recordFieldNodes.add(fieldNode);
        return recordFieldNodes;
    }

    /**
     * Create `OAuth2PasswordGrantConfig` record with default tokenUrl.
     *
     * <pre>
     *      # OAuth2 Password Grant Configs
     *      public type OAuth2PasswordGrantConfig record {|
     *          *websocket:OAuth2PasswordGrantConfig;
     *          # Token URL
     *          string tokenUrl = "https://zoom.us/oauth/token";
     *      |};
     * </pre>
     *
     * @return {@link TypeDefinitionNode}   Custom `OAuth2PasswordGrantConfig` record with default tokenUrl
     */
    private TypeDefinitionNode getOAuth2PasswordGrantConfigRecord() {
        Token typeName = AbstractNodeFactory.createIdentifierToken(AuthConfigTypes.CUSTOM_PASSWORD.getValue());
        NodeList<Node> recordFieldList = createNodeList(getPasswordGrantConfigFields());
        MetadataNode configRecordMetadataNode = getMetadataNode("OAuth2 Password Grant Configs");
        RecordTypeDescriptorNode recordTypeDescriptorNode =
                NodeFactory.createRecordTypeDescriptorNode(createToken(RECORD_KEYWORD),
                        createToken(OPEN_BRACE_PIPE_TOKEN), recordFieldList, null,
                        createToken(CLOSE_BRACE_PIPE_TOKEN));
        return NodeFactory.createTypeDefinitionNode(configRecordMetadataNode,
                createToken(PUBLIC_KEYWORD), createToken(TYPE_KEYWORD), typeName,
                recordTypeDescriptorNode, createToken(SEMICOLON_TOKEN));
    }

    /**
     * Generates fields of `OAuth2PasswordGrantConfig` record with default tokenUrl.
     *
     * <pre>
     *      *websocket:OAuth2PasswordGrantConfig;
     *      # Token URL
     *      string tokenUrl = "https://zoom.us/oauth/token";
     * </pre>
     *
     * @return {@link List<Node>}
     */
    private List<Node> getPasswordGrantConfigFields() {

        List<Node> recordFieldNodes = new ArrayList<>();
        Token semicolonToken = createToken(SEMICOLON_TOKEN);
        Token equalToken = createToken(EQUAL_TOKEN);

        recordFieldNodes.add(createIncludedRecordParameterNode(createEmptyNodeList(),
                createToken(ASTERISK_TOKEN),
                createIdentifierToken("websocket:OAuth2PasswordGrantConfig;"), null));

        MetadataNode metadataNode = getMetadataNode("Token URL");
        TypeDescriptorNode stringType = createSimpleNameReferenceNode(createToken(STRING_KEYWORD));
        IdentifierToken fieldNameTokenUrl = createIdentifierToken("tokenUrl");
        ExpressionNode defaultValue = createRequiredExpressionNode(createIdentifierToken("\"" +
                passwordGrantTokenUrl + "\""));
        RecordFieldWithDefaultValueNode fieldNode = NodeFactory.createRecordFieldWithDefaultValueNode(
                metadataNode, null, stringType, fieldNameTokenUrl, equalToken, defaultValue,
                semicolonToken);
        recordFieldNodes.add(fieldNode);
        return recordFieldNodes;
    }

    /**
     * Create `OAuth2RefreshTokenGrantConfig` record with default refreshUrl.
     *
     * <pre>
     *      # OAuth2 Refresh Token Grant Configs
     *      public type OAuth2RefreshTokenGrantConfig record {|
     *          *websocket:OAuth2RefreshTokenGrantConfig;
     *          # Refresh URL
     *          string refreshUrl = "https://zoom.us/oauth/token";
     *      |};
     * </pre>
     *
     * @return {@link TypeDefinitionNode}   Custom `OAuth2RefreshTokenGrantConfig` record with default refreshUrl
     */
    private TypeDefinitionNode getOAuth2RefreshTokenGrantConfigRecord() {
        Token typeName = AbstractNodeFactory.createIdentifierToken(AuthConfigTypes.CUSTOM_REFRESH_TOKEN.getValue());
        NodeList<Node> recordFieldList = createNodeList(getRefreshTokenGrantConfigFields());
        MetadataNode configRecordMetadataNode = getMetadataNode("OAuth2 Refresh Token Grant Configs");
        RecordTypeDescriptorNode recordTypeDescriptorNode =
                NodeFactory.createRecordTypeDescriptorNode(createToken(RECORD_KEYWORD),
                        createToken(OPEN_BRACE_PIPE_TOKEN), recordFieldList, null,
                        createToken(CLOSE_BRACE_PIPE_TOKEN));
        return NodeFactory.createTypeDefinitionNode(configRecordMetadataNode,
                createToken(PUBLIC_KEYWORD), createToken(TYPE_KEYWORD), typeName,
                recordTypeDescriptorNode, createToken(SEMICOLON_TOKEN));
    }

    /**
     * Generates fields of `OAuth2RefreshTokenGrantConfig` record with default refreshUrl.
     *
     * <pre>
     *      *websocket:OAuth2RefreshTokenGrantConfig;
     *      # Refresh URL
     *      string refreshUrl = "https://zoom.us/oauth/token";
     * </pre>
     *
     * @return {@link List<Node>}
     */
    private List<Node> getRefreshTokenGrantConfigFields() {

        List<Node> recordFieldNodes = new ArrayList<>();
        Token semicolonToken = createToken(SEMICOLON_TOKEN);
        Token equalToken = createToken(EQUAL_TOKEN);

        recordFieldNodes.add(createIncludedRecordParameterNode(createEmptyNodeList(),
                createToken(ASTERISK_TOKEN),
                createIdentifierToken("websocket:OAuth2RefreshTokenGrantConfig;"), null));

        MetadataNode metadataNode = getMetadataNode("Refresh URL");
        TypeDescriptorNode stringType = createSimpleNameReferenceNode(createToken(STRING_KEYWORD));
        IdentifierToken fieldNameTokenUrl = createIdentifierToken("refreshUrl");
        ExpressionNode defaultValue = createRequiredExpressionNode(createIdentifierToken("\"" +
                refreshTokenUrl + "\""));
        RecordFieldWithDefaultValueNode fieldNode = NodeFactory.createRecordFieldWithDefaultValueNode(
                metadataNode, null, stringType, fieldNameTokenUrl, equalToken, defaultValue,
                semicolonToken);
        recordFieldNodes.add(fieldNode);
        return recordFieldNodes;
    }

    /**
     * Generates`ClientHttp1Settings` record.
     *
     * <pre>
     *  # Provides settings related to HTTP/1.x protocol.
     *  public type ClientHttp1Settings record {|
     *     # Specifies whether to reuse a connection for multiple requests
     *     http:KeepAlive keepAlive = http:KEEPALIVE_AUTO;
     *     # The chunking behaviour of the request
     *     http:Chunking chunking = http:CHUNKING_AUTO;
     *     # Proxy server related options
     *     ProxyConfig proxy?;
     *  |};
     * </pre>
     *
     * @return {@link TypeDefinitionNode}
     */
    private TypeDefinitionNode getClientHttp1SettingsRecord() {
        Token recordTypeName = createIdentifierToken(CLIENT_HTTP1_SETTINGS);
        NodeList<Node> recordFieldList = createNodeList(getClientHttp1SettingsRecordFields());
        MetadataNode recordMetadataNode = getMetadataNode(
                "Provides settings related to HTTP/1.x protocol.");
        RecordTypeDescriptorNode recordTypeDescriptorNode = createRecordTypeDescriptorNode(createToken(RECORD_KEYWORD),
                createToken(OPEN_BRACE_PIPE_TOKEN), recordFieldList, null,
                createToken(CLOSE_BRACE_PIPE_TOKEN));
        return createTypeDefinitionNode(recordMetadataNode, createToken(PUBLIC_KEYWORD), createToken(TYPE_KEYWORD),
                recordTypeName, recordTypeDescriptorNode, createToken(SEMICOLON_TOKEN));

    }

    /**
     * Generates`ProxyConfig` record.
     *
     * <pre>
     *  # Proxy server configurations to be used with the HTTP client endpoint.
     *  public type ProxyConfig record {|
     *     # Host name of the proxy server
     *     string host = "";
     *     # Proxy server port
     *     int port = 0;
     *     # Proxy server username
     *     string userName = "";
     *     # Proxy server password
     *     @display {label: "", kind: "password"}
     *     string password = "";
     *  |};
     * </pre>
     *
     * @return {@link TypeDefinitionNode}
     */
    private TypeDefinitionNode getCustomProxyRecord() {
        Token recordTypeName = createIdentifierToken("ProxyConfig");
        NodeList<Node> recordFieldList = createNodeList(getCustomProxyRecordFields());
        MetadataNode recordMetadataNode = getMetadataNode(
                "Proxy server configurations to be used with the HTTP client endpoint.");
        RecordTypeDescriptorNode recordTypeDescriptorNode = createRecordTypeDescriptorNode(createToken(RECORD_KEYWORD),
                createToken(OPEN_BRACE_PIPE_TOKEN), recordFieldList, null,
                createToken(CLOSE_BRACE_PIPE_TOKEN));
        return createTypeDefinitionNode(recordMetadataNode, createToken(PUBLIC_KEYWORD), createToken(TYPE_KEYWORD),
                recordTypeName, recordTypeDescriptorNode, createToken(SEMICOLON_TOKEN));

    }

    /**
     * Generate Class variable for api key map {@code final readonly & ApiKeysConfig apiKeyConfig;}.
     *
     * @return {@link List<ObjectFieldNode>}    syntax tree object field node list
     */
    public ObjectFieldNode getApiKeyMapClassVariable() { // return ObjectFieldNode
        if (httpApiKey) {
            NodeList<Token> qualifierList = createNodeList(createToken(FINAL_KEYWORD));
            TypeDescriptorNode readOnlyNode = createTypeReferenceTypeDescNode(createSimpleNameReferenceNode
                    (createToken(READONLY_KEYWORD)));
            TypeDescriptorNode apiKeyMapNode = createSimpleNameReferenceNode(createIdentifierToken(API_KEYS_CONFIG));
            if (httpOROAuth) {
                apiKeyMapNode = createOptionalTypeDescriptorNode(apiKeyMapNode, createToken(QUESTION_MARK_TOKEN));
            }
            TypeDescriptorNode intersectionTypeDescriptorNode = createIntersectionTypeDescriptorNode(readOnlyNode,
                    createToken(BITWISE_AND_TOKEN), apiKeyMapNode);
            IdentifierToken fieldName = createIdentifierToken(API_KEY_CONFIG_PARAM);
            MetadataNode metadataNode = createMetadataNode(null, createEmptyNodeList());
            return createObjectFieldNode(metadataNode, null,
                    qualifierList, intersectionTypeDescriptorNode, fieldName, null, null,
                    createToken(SEMICOLON_TOKEN));
        }
        return null;
    }

    /**
     * Generate the config parameters of the client class init method.
     * -- ex: Config param for Http and OAuth 2.0 Authentication mechanisms.
     * {@code ClientConfig clientConfig, string serviceUrl = "https://petstore.swagger.io:443/v2" }
     * -- ex: Config param for API Key Authentication mechanism.
     * {@code ApiKeysConfig apiKeyConfig, http:ClientConfiguration clientConfig = {},
     * string serviceUrl = "https://petstore.swagger.io:443/v2" }
     * Config param for API Key Authentication mechanism with no server URL given
     * {@code ApiKeysConfig apiKeyConfig, string serviceUrl; http:ClientConfiguration clientConfig = {}}
     * -- ex: Config param when no authentication mechanism given.
     * {@code http:ClientConfiguration clientConfig = {},
     * string serviceUrl = "https://petstore.swagger.io:443/v2" }
     * Config param when no authentication mechanism given with no server URL
     * {@code string serviceUrl, http:ClientConfiguration clientConfig = {}}
     * -- ex: Config param for combination of API Key and Http or OAuth 2.0 Authentication mechanisms.
     * {@code AuthConfig authConfig,ConnectionConfig config =  {},
     * string serviceUrl = "https://petstore.swagger.io:443/v2" }
     *
     * @return {@link List<Node>}  syntax tree node list of config parameters
     */
    public List<Node> getConfigParamForClassInit(String serviceUrl) {

        NodeList<AnnotationNode> annotationNodes = createEmptyNodeList();
        Node serviceURLNode = getServiceURLNode(serviceUrl);
        List<Node> parameters = new ArrayList<>();
        IdentifierToken equalToken = createIdentifierToken(GeneratorConstants.EQUAL);
        if (httpOROAuth) {
            BuiltinSimpleNameReferenceNode typeName = createBuiltinSimpleNameReferenceNode(null,
                    createIdentifierToken(CONNECTION_CONFIG));
            IdentifierToken paramName = createIdentifierToken(CONFIG);
            RequiredParameterNode authConfig = createRequiredParameterNode(annotationNodes, typeName, paramName);
            parameters.add(authConfig);
            parameters.add(createToken(COMMA_TOKEN));
            parameters.add(serviceURLNode);
        } else {
            if (httpApiKey) {
                BuiltinSimpleNameReferenceNode apiKeyConfigTypeName = createBuiltinSimpleNameReferenceNode(null,
                        createIdentifierToken(API_KEYS_CONFIG));
                IdentifierToken apiKeyConfigParamName = createIdentifierToken(API_KEY_CONFIG_PARAM);
                RequiredParameterNode apiKeyConfigParamNode = createRequiredParameterNode(annotationNodes,
                        apiKeyConfigTypeName, apiKeyConfigParamName);
                parameters.add(apiKeyConfigParamNode);
                parameters.add(createToken(COMMA_TOKEN));
            }

            BuiltinSimpleNameReferenceNode httpClientConfigTypeName = createBuiltinSimpleNameReferenceNode(null,
                    createIdentifierToken(CONNECTION_CONFIG));
            IdentifierToken httpClientConfig = createIdentifierToken(CONFIG);
            BasicLiteralNode emptyexpression = createBasicLiteralNode(null, createIdentifierToken(" {}"));
            DefaultableParameterNode defaultConnectionConfig = createDefaultableParameterNode(annotationNodes,
                    httpClientConfigTypeName,
                    httpClientConfig, equalToken, emptyexpression);
            if (serviceURLNode instanceof RequiredParameterNode) {
                parameters.add(serviceURLNode);
                parameters.add(createToken(COMMA_TOKEN));
                parameters.add(defaultConnectionConfig);
            } else {
                parameters.add(defaultConnectionConfig);
                parameters.add(createToken(COMMA_TOKEN));
                parameters.add(serviceURLNode);
            }
        }
        return parameters;
    }

    /**
     * Generate the serviceUrl parameters of the client class init method.
     *
     * @param serviceUrl service Url given in the OpenAPI file
     * @return {@link DefaultableParameterNode} when server URl is given in the OpenAPI file
     * {@link RequiredParameterNode} when server URL is not given in the OpenAPI file
     */
    private Node getServiceURLNode(String serviceUrl) {
        Node serviceURLNode;
        NodeList<AnnotationNode> annotationNodes = createEmptyNodeList();
        BuiltinSimpleNameReferenceNode serviceURLType = createBuiltinSimpleNameReferenceNode(null,
                createIdentifierToken("string"));
        IdentifierToken serviceURLVarName = createIdentifierToken(GeneratorConstants.SERVICE_URL);

        if (serviceUrl.equals("/")) {
            serviceURLNode = createRequiredParameterNode(annotationNodes, serviceURLType, serviceURLVarName);
        } else {
            BasicLiteralNode expression = createBasicLiteralNode(STRING_LITERAL,
                    createIdentifierToken('"' + serviceUrl + '"'));
            serviceURLNode = createDefaultableParameterNode(annotationNodes, serviceURLType,
                    serviceURLVarName, createIdentifierToken("="), expression);
        }
        return serviceURLNode;
    }

    /**
     * Generate if-else statements for the do block in client init function.
     * <pre>
     *     if config.http1Settings is ClientHttp1Settings {
     *         ClientHttp1Settings settings = check config.http1Settings.ensureType(ClientHttp1Settings);
     *         httpClientConfig.http1Settings = {...settings};
     *     }
     * </pre>
     *
     * @param fieldName name of the field
     * @param fieldType type of the field
     * @return
     */
    private IfElseStatementNode getDoBlockIfElseStatementNodes(String fieldName, String fieldType) {
        ExpressionNode expressionNode = createFieldAccessExpressionNode(
                createRequiredExpressionNode(createIdentifierToken(CONFIG)),
                createToken(DOT_TOKEN), createSimpleNameReferenceNode(createIdentifierToken(fieldName)));

        ExpressionNode condition = createBinaryExpressionNode(null,
                expressionNode,
                createToken(IS_KEYWORD),
                createIdentifierToken(fieldType)
        );

        List<StatementNode> statementNodes = new ArrayList<>();

        // httpClientConfig.http2Settings = check config.http2Settings.ensureType(http:ClientHttp2Settings);
        FieldAccessExpressionNode fieldAccessExpressionNode = createFieldAccessExpressionNode(
                createRequiredExpressionNode(createIdentifierToken(HTTP_CLIENT_CONFIG)),
                createToken(DOT_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken(fieldName)));

        MethodCallExpressionNode methodCallExpressionNode = createMethodCallExpressionNode(
                createFieldAccessExpressionNode(createRequiredExpressionNode(createIdentifierToken(CONFIG)),
                        createToken(DOT_TOKEN),
                        createSimpleNameReferenceNode(createIdentifierToken(fieldName))),
                createToken(DOT_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken(ENSURE_TYPE)),
                createToken(OPEN_PAREN_TOKEN),
                createSeparatedNodeList(createPositionalArgumentNode(
                        createRequiredExpressionNode(createIdentifierToken(fieldType)))),
                createToken(CLOSE_PAREN_TOKEN));
        CheckExpressionNode checkExpressionNode = createCheckExpressionNode(null, createToken(CHECK_KEYWORD),
                methodCallExpressionNode);
        AssignmentStatementNode varAssignmentNode = createAssignmentStatementNode(fieldAccessExpressionNode,
                createToken(EQUAL_TOKEN), checkExpressionNode, createToken(SEMICOLON_TOKEN));
        statementNodes.add(varAssignmentNode);

        NodeList<StatementNode> statementList = createNodeList(statementNodes);
        BlockStatementNode ifBody = createBlockStatementNode(createToken(OPEN_BRACE_TOKEN), statementList,
                createToken(CLOSE_BRACE_TOKEN));
        return createIfElseStatementNode(createToken(IF_KEYWORD), condition,
                ifBody, null);
    }

    /**
     * Generate do block in client init function.
     *
     * @return {@link DoStatementNode}
     */
    public DoStatementNode getClientConfigDoStatementNode() {
        List<StatementNode> doStatementNodeList = new ArrayList<>();
        // ClientHttp1Settings if statement
        {
            ExpressionNode expressionNode = createFieldAccessExpressionNode(
                    createRequiredExpressionNode(createIdentifierToken(CONFIG)),
                    createToken(DOT_TOKEN), createSimpleNameReferenceNode(
                            createIdentifierToken(CLIENT_HTTP1_SETTINGS_FIELD)));

            ExpressionNode condition = createBinaryExpressionNode(null,
                    expressionNode,
                    createToken(IS_KEYWORD),
                    createIdentifierToken(CLIENT_HTTP1_SETTINGS)
            );

            List<StatementNode> statementNodes = new ArrayList<>();

            // ClientHttp1Settings settings = check config.http1Settings.ensureType(ClientHttp1Settings);
            SimpleNameReferenceNode typeBindingPattern = createSimpleNameReferenceNode(
                    createIdentifierToken(CLIENT_HTTP1_SETTINGS));
            CaptureBindingPatternNode bindingPattern = createCaptureBindingPatternNode(
                    createIdentifierToken(SETTINGS));
            TypedBindingPatternNode typedBindingPatternNode = createTypedBindingPatternNode(typeBindingPattern,
                    bindingPattern);
            MethodCallExpressionNode methodCallExpressionNode = createMethodCallExpressionNode(
                    createFieldAccessExpressionNode(createRequiredExpressionNode(createIdentifierToken(CONFIG)),
                            createToken(DOT_TOKEN),
                            createSimpleNameReferenceNode(createIdentifierToken(CLIENT_HTTP1_SETTINGS_FIELD))),
                    createToken(DOT_TOKEN),
                    createSimpleNameReferenceNode(createIdentifierToken(ENSURE_TYPE)),
                    createToken(OPEN_PAREN_TOKEN),
                    createSeparatedNodeList(createPositionalArgumentNode(
                            createRequiredExpressionNode(createIdentifierToken(CLIENT_HTTP1_SETTINGS)))),
                    createToken(CLOSE_PAREN_TOKEN));
            CheckExpressionNode checkExpressionNode = createCheckExpressionNode(null, createToken(CHECK_KEYWORD),
                    methodCallExpressionNode);
            AssignmentStatementNode varAssignmentNode = createAssignmentStatementNode(typedBindingPatternNode,
                    createToken(EQUAL_TOKEN), checkExpressionNode, createToken(SEMICOLON_TOKEN));
            statementNodes.add(varAssignmentNode);

            // httpClientConfig.http1Settings = {...settings};
            FieldAccessExpressionNode fieldAccessExpressionNode = createFieldAccessExpressionNode(
                    createRequiredExpressionNode(createIdentifierToken(HTTP_CLIENT_CONFIG)),
                    createToken(DOT_TOKEN),
                    createSimpleNameReferenceNode(createIdentifierToken(CLIENT_HTTP1_SETTINGS_FIELD)));
            MappingConstructorExpressionNode mappingConstructorExpressionNode = createMappingConstructorExpressionNode(
                    createToken(OPEN_BRACE_TOKEN),
                    createSeparatedNodeList(
                            createRestArgumentNode(createToken(ELLIPSIS_TOKEN),
                                    createRequiredExpressionNode(createIdentifierToken(SETTINGS)))),
                    createToken(CLOSE_BRACE_TOKEN));

            AssignmentStatementNode fieldAssignmentNode = createAssignmentStatementNode(fieldAccessExpressionNode,
                    createToken(EQUAL_TOKEN), mappingConstructorExpressionNode, createToken(SEMICOLON_TOKEN));

            statementNodes.add(fieldAssignmentNode);

            NodeList<StatementNode> statementList = createNodeList(statementNodes);
            BlockStatementNode ifBody = createBlockStatementNode(createToken(OPEN_BRACE_TOKEN), statementList,
                    createToken(CLOSE_BRACE_TOKEN));

            IfElseStatementNode ifElseStatementNode = createIfElseStatementNode(createToken(IF_KEYWORD), condition,
                    ifBody, null);
            doStatementNodeList.add(ifElseStatementNode);
        }
        // http:ClientHttp2Settings if statement
        doStatementNodeList.addAll(Arrays.asList(
                getDoBlockIfElseStatementNodes(HTTP2_SETTINGS_FIELD, HTTP2_SETTINGS),
                getDoBlockIfElseStatementNodes(CACHE_CONFIG_FIELD, CACHE_CONFIG),
                getDoBlockIfElseStatementNodes(RESPONSE_LIMIT_FIELD, RESPONSE_LIMIT),
                getDoBlockIfElseStatementNodes(SECURE_SOCKET_FIELD, SECURE_SOCKET),
                getDoBlockIfElseStatementNodes(PROXY_CONFIG, PROXY)));

        BlockStatementNode blockStatementNode = createBlockStatementNode(createToken(OPEN_BRACE_TOKEN),
                createNodeList(doStatementNodeList), createToken(CLOSE_BRACE_TOKEN));

        return createDoStatementNode(createToken(DO_KEYWORD),
                blockStatementNode, null);
    }

    /**
     * Generate `httpClientConfig` variable.
     * <pre>
     *        http:ClientConfiguration httpClientConfig = {
     *             httpVersion: config.httpVersion,
     *             timeout: config.timeout,
     *             forwarded: config.forwarded,
     *             poolConfig: config.poolConfig,
     *             compression: config.compression,
     *             circuitBreaker: config.circuitBreaker,
     *             retryConfig: config.retryConfig,
     *             validation: config.validation
     *         };
     * </pre>
     *
     * @return
     */
    public VariableDeclarationNode getHttpClientConfigVariableNode() {
        Token comma = createToken(COMMA_TOKEN);
        NodeList<AnnotationNode> annotationNodes = createEmptyNodeList();
        // http:ClientConfiguration variable declaration
        SimpleNameReferenceNode typeBindingPattern = createSimpleNameReferenceNode(
                createIdentifierToken("http:ClientConfiguration"));
        CaptureBindingPatternNode bindingPattern = createCaptureBindingPatternNode(
                createIdentifierToken(HTTP_CLIENT_CONFIG));
        TypedBindingPatternNode typedBindingPatternNode = createTypedBindingPatternNode(typeBindingPattern,
                bindingPattern);

        List<Node> argumentsList = new ArrayList<>();

        if (isHttpOROAuth() && !isHttpApiKey()) {
            ExpressionNode authValExp = createFieldAccessExpressionNode(
                    createSimpleNameReferenceNode(createIdentifierToken(CONFIG)),
                    createToken(DOT_TOKEN), createSimpleNameReferenceNode(createIdentifierToken(AUTH)));
            SpecificFieldNode authField = createSpecificFieldNode(null,
                    createIdentifierToken(AUTH),
                    createToken(COLON_TOKEN), authValExp);
            argumentsList.add(authField);
            argumentsList.add(comma);
        }

        // create httpVersion field
        ExpressionNode httpVersionValExp = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(CONFIG)),
                createToken(DOT_TOKEN), createSimpleNameReferenceNode(createIdentifierToken("httpVersion")));
        SpecificFieldNode httpVersionField = createSpecificFieldNode(null,
                createIdentifierToken("httpVersion"),
                createToken(COLON_TOKEN), httpVersionValExp);
        argumentsList.add(httpVersionField);
        argumentsList.add(comma);
        // create timeout field
        ExpressionNode timeoutValExp = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(CONFIG)),
                createToken(DOT_TOKEN), createSimpleNameReferenceNode(createIdentifierToken("timeout")));
        SpecificFieldNode timeoutField = createSpecificFieldNode(null,
                createIdentifierToken("timeout"),
                createToken(COLON_TOKEN), timeoutValExp);
        argumentsList.add(timeoutField);
        argumentsList.add(comma);

        // create forwarded field
        ExpressionNode forwardedValExp = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(CONFIG)),
                createToken(DOT_TOKEN), createSimpleNameReferenceNode(createIdentifierToken("forwarded")));
        SpecificFieldNode forwardedField = createSpecificFieldNode(null,
                createIdentifierToken("forwarded"),
                createToken(COLON_TOKEN), forwardedValExp);
        argumentsList.add(forwardedField);
        argumentsList.add(comma);

        // create poolConfig field
        ExpressionNode poolConfigValExp = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(CONFIG)),
                createToken(DOT_TOKEN), createSimpleNameReferenceNode(createIdentifierToken("poolConfig")));
        SpecificFieldNode poolConfigField = createSpecificFieldNode(null,
                createIdentifierToken("poolConfig"),
                createToken(COLON_TOKEN), poolConfigValExp);
        argumentsList.add(poolConfigField);
        argumentsList.add(comma);

        // create compression field
        ExpressionNode compressionValExp = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(CONFIG)),
                createToken(DOT_TOKEN), createSimpleNameReferenceNode(createIdentifierToken("compression")));
        SpecificFieldNode compressionField = createSpecificFieldNode(null,
                createIdentifierToken("compression"),
                createToken(COLON_TOKEN), compressionValExp);
        argumentsList.add(compressionField);
        argumentsList.add(comma);

        // create circuitBreaker field
        ExpressionNode circuitBreakerValExp = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(CONFIG)),
                createToken(DOT_TOKEN), createSimpleNameReferenceNode(createIdentifierToken("circuitBreaker")));
        SpecificFieldNode circuitBreakerField = createSpecificFieldNode(null,
                createIdentifierToken("circuitBreaker"),
                createToken(COLON_TOKEN), circuitBreakerValExp);
        argumentsList.add(circuitBreakerField);
        argumentsList.add(comma);

        // create retryConfig field
        ExpressionNode retryConfigValExp = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(CONFIG)),
                createToken(DOT_TOKEN), createSimpleNameReferenceNode(createIdentifierToken("retryConfig")));
        SpecificFieldNode retryConfigField = createSpecificFieldNode(null,
                createIdentifierToken("retryConfig"),
                createToken(COLON_TOKEN), retryConfigValExp);
        argumentsList.add(retryConfigField);
        argumentsList.add(comma);

        ExpressionNode validationValExp = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(CONFIG)),
                createToken(DOT_TOKEN), createSimpleNameReferenceNode(createIdentifierToken("validation")));
        SpecificFieldNode validationField = createSpecificFieldNode(null,
                createIdentifierToken("validation"),
                createToken(COLON_TOKEN), validationValExp);
        argumentsList.add(validationField);

        SeparatedNodeList<MappingFieldNode> arguments = createSeparatedNodeList(argumentsList);
        MappingConstructorExpressionNode mappingConstructorExpressionNode =
                createMappingConstructorExpressionNode(createToken(OPEN_BRACE_TOKEN),
                        arguments, createToken(CLOSE_BRACE_TOKEN));
        return createVariableDeclarationNode(annotationNodes, null, typedBindingPatternNode,
                createToken(EQUAL_TOKEN), mappingConstructorExpressionNode, createToken(SEMICOLON_TOKEN));
    }

    /**
     * Generate http:client initialization node.
     * <pre>
     *     http:Client httpEp = check new (serviceUrl, clientConfig);
     * </pre>
     *
     * @return {@link VariableDeclarationNode}   Synatx tree node of client initialization
     */
    public VariableDeclarationNode getClientInitializationNode() {

        NodeList<AnnotationNode> annotationNodes = createEmptyNodeList();
        // http:Client variable declaration
        BuiltinSimpleNameReferenceNode typeBindingPattern = createBuiltinSimpleNameReferenceNode(null,
                createIdentifierToken("http:Client"));
        CaptureBindingPatternNode bindingPattern = createCaptureBindingPatternNode(
                createIdentifierToken("httpEp"));
        TypedBindingPatternNode typedBindingPatternNode = createTypedBindingPatternNode(typeBindingPattern,
                bindingPattern);

        // Expression node
        List<Node> argumentsList = new ArrayList<>();
        PositionalArgumentNode positionalArgumentNode01 = createPositionalArgumentNode(createSimpleNameReferenceNode(
                createIdentifierToken(GeneratorConstants.SERVICE_URL)));
        argumentsList.add(positionalArgumentNode01);
        Token comma1 = createIdentifierToken(",");

        PositionalArgumentNode positionalArgumentNode02 = createPositionalArgumentNode(createSimpleNameReferenceNode(
                createIdentifierToken(HTTP_CLIENT_CONFIG)));
        argumentsList.add(comma1);
        argumentsList.add(positionalArgumentNode02);

        SeparatedNodeList<FunctionArgumentNode> arguments = createSeparatedNodeList(argumentsList);
        Token closeParenArg = createToken(CLOSE_PAREN_TOKEN);
        ParenthesizedArgList parenthesizedArgList = createParenthesizedArgList(createToken(OPEN_PAREN_TOKEN), arguments,
                closeParenArg);
        ImplicitNewExpressionNode expressionNode = createImplicitNewExpressionNode(createToken(NEW_KEYWORD),
                parenthesizedArgList);
        CheckExpressionNode initializer = createCheckExpressionNode(null, createToken(CHECK_KEYWORD),
                expressionNode);
        return createVariableDeclarationNode(annotationNodes,
                null, typedBindingPatternNode, createToken(EQUAL_TOKEN), initializer,
                createToken(SEMICOLON_TOKEN));
    }

    /**
     * Generate assignment nodes for api key map assignment {@code self.apiKeyConfig=apiKeyConfig.cloneReadOnly();}.
     *
     * @return {@link AssignmentStatementNode} syntax tree assignment statement node.
     */
    public AssignmentStatementNode getApiKeyAssignmentNode() {

        if (httpApiKey) {
            FieldAccessExpressionNode varRefApiKey = createFieldAccessExpressionNode(
                    createSimpleNameReferenceNode(createIdentifierToken(SELF)), createToken(DOT_TOKEN),
                    createSimpleNameReferenceNode(createIdentifierToken(API_KEY_CONFIG_PARAM)));
            ExpressionNode fieldAccessExpressionNode = createRequiredExpressionNode(
                    createIdentifierToken(API_KEY_CONFIG_PARAM));
            ExpressionNode methodCallExpressionNode = createMethodCallExpressionNode(
                    fieldAccessExpressionNode, createToken(DOT_TOKEN),
                    createSimpleNameReferenceNode(createIdentifierToken("cloneReadOnly")),
                    createToken(OPEN_PAREN_TOKEN), createSeparatedNodeList(), createToken(CLOSE_PAREN_TOKEN));
            return createAssignmentStatementNode(varRefApiKey,
                    createToken(EQUAL_TOKEN), methodCallExpressionNode, createToken(SEMICOLON_TOKEN));
        }
        return null;
    }

    /**
     * Returns fields in ClientConfig record.
     *          # Configurations related to client authentication
     *          websocket:BearerTokenConfig|websocket:OAuth2RefreshTokenGrantConfig auth;
     *          # Negotiable sub protocols of the client
     *          string[] subProtocols = [];
     *          # Custom headers, which should be sent to the server
     *          map<string> customHeaders = {};
     *          # Read timeout (in seconds) of the client
     *          decimal readTimeout = -1;
     *          # Write timeout (in seconds) of the client
     *          decimal writeTimeout = -1;
     *          # SSL/TLS-related options
     *          websocket:ClientSecureSocket? secureSocket = ();
     *          # The maximum payload size of a WebSocket frame in bytes
     *          int maxFrameSize = 65536;
     *          # Enable support for compression in the WebSocket
     *          boolean webSocketCompressionEnabled = true;
     *          # Time (in seconds) that a connection waits to get the response of the WebSocket handshake.
     *          decimal handShakeTimeout = 300;
     *          # An Array of http:Cookie
     *          http:Cookie[] cookies?;
     *          # A service to handle the ping/pong frames.
     *          PingPongService pingPongHandler?;
     *          # Configurations associated with retrying
     *          websocket:WebSocketRetryConfig retryConfig? = ();
     *          Enable/disable constraint validation
     *          boolean validation = true;
     */
    private List<Node> getClientConfigRecordFields() {

        List<Node> recordFieldNodes = new ArrayList<>();
        Token semicolonToken = createToken(SEMICOLON_TOKEN);
        Token equalToken = createToken(EQUAL_TOKEN);
        Token questionMarkToken = createToken(QUESTION_MARK_TOKEN);

        // add auth field
        if (isHttpOROAuth() && !isHttpApiKey()) {
            MetadataNode authMetadataNode = getMetadataNode("Configurations related to client authentication");
            IdentifierToken authFieldName = AbstractNodeFactory.createIdentifierToken(escapeIdentifier(
                    AUTH));
            TypeDescriptorNode authFieldTypeNode =
                    createSimpleNameReferenceNode(createIdentifierToken(getAuthFieldTypeName()));
            RecordFieldNode authFieldNode = NodeFactory.createRecordFieldNode(authMetadataNode, null,
                    authFieldTypeNode, authFieldName, null, semicolonToken);
            recordFieldNodes.add(authFieldNode);
        } else if (isHttpOROAuth() && isHttpApiKey()) {
            MetadataNode authMetadataNode = getMetadataNode(
                    "Provides Auth configurations needed when communicating with a remote Websocket " +
                            "service endpoint.");
            IdentifierToken authFieldName = AbstractNodeFactory.createIdentifierToken(escapeIdentifier(
                    AUTH));
            TypeDescriptorNode unionTypeDescriptor = createUnionTypeDescriptorNode(
                    createSimpleNameReferenceNode(createIdentifierToken(getAuthFieldTypeName())),
                    createToken(PIPE_TOKEN),
                    createSimpleNameReferenceNode(createIdentifierToken(API_KEYS_CONFIG)));
            RecordFieldNode authFieldNode = NodeFactory.createRecordFieldNode(authMetadataNode, null,
                    unionTypeDescriptor, authFieldName, null, semicolonToken);
            recordFieldNodes.add(authFieldNode);
        }

        // add subProtocols field
        MetadataNode subProtocolsMetadata = getMetadataNode("Negotiable sub protocols of the client");
        IdentifierToken subProtocolsFieldName = createIdentifierToken("subProtocols");
        NodeList<ArrayDimensionNode> arrayDimensions = NodeFactory.createEmptyNodeList();
        ArrayDimensionNode arrayDimension = NodeFactory.createArrayDimensionNode(
                createToken(SyntaxKind.OPEN_BRACKET_TOKEN), null,
                createToken(SyntaxKind.CLOSE_BRACKET_TOKEN));
        arrayDimensions = arrayDimensions.add(arrayDimension);
        TypeDescriptorNode subProtocolsMemberType = createSimpleNameReferenceNode(createToken(STRING_KEYWORD));
        ArrayTypeDescriptorNode subProtocolsFieldType = createArrayTypeDescriptorNode(subProtocolsMemberType
                , arrayDimensions);
        RequiredExpressionNode subProtocolsExpression =
                createRequiredExpressionNode(createIdentifierToken("[]"));
        RecordFieldWithDefaultValueNode subProtocolsFieldNode = NodeFactory.createRecordFieldWithDefaultValueNode(
                subProtocolsMetadata, null, subProtocolsFieldType, subProtocolsFieldName,
                equalToken, subProtocolsExpression , semicolonToken);
        recordFieldNodes.add(subProtocolsFieldNode);

        // add customHeaders field
        TypeDescriptorNode customHeadersMapParamType = createSimpleNameReferenceNode(createToken(STRING_KEYWORD));
        TypeParameterNode customHeadersTypeParamsNode=createTypeParameterNode(createToken(LT_TOKEN),
                customHeadersMapParamType,createToken(GT_TOKEN));
       MapTypeDescriptorNode customHeadersFieldType= createMapTypeDescriptorNode(createToken(MAP_KEYWORD),
               customHeadersTypeParamsNode);
        MetadataNode customHeadersMetadata = getMetadataNode("Custom headers, " +
                "which should be sent to the server");
        IdentifierToken customHeadersFieldName = createIdentifierToken("customHeaders");
        RequiredExpressionNode customHeadersExpression =
                createRequiredExpressionNode(createIdentifierToken("{}"));
        RecordFieldWithDefaultValueNode customHeadersFieldNode = createRecordFieldWithDefaultValueNode(
                customHeadersMetadata, null, customHeadersFieldType, customHeadersFieldName,
                equalToken,customHeadersExpression, semicolonToken);
        recordFieldNodes.add(customHeadersFieldNode);

        // add readTimeout field
        MetadataNode readTimeOutMetadata = getMetadataNode("Read timeout (in seconds) of the client");
        TypeDescriptorNode readTimeOutFieldType =
                createSimpleNameReferenceNode(createToken(DECIMAL_KEYWORD));
        IdentifierToken readTimeoutFieldName = createIdentifierToken("readTimeout");
        ExpressionNode readTimeOutDecimalLiteralNode = createRequiredExpressionNode(createIdentifierToken("-1"));
        RecordFieldWithDefaultValueNode readTimeoutFieldNode = NodeFactory.createRecordFieldWithDefaultValueNode(
                readTimeOutMetadata, null, readTimeOutFieldType, readTimeoutFieldName,
                equalToken, readTimeOutDecimalLiteralNode,semicolonToken);
        recordFieldNodes.add(readTimeoutFieldNode);

        // add writeTimeout field
        MetadataNode writeTimeOutMetadata = getMetadataNode("Write timeout (in seconds) of the client");
        TypeDescriptorNode writeTimeOutFieldType =
                createSimpleNameReferenceNode(createToken(DECIMAL_KEYWORD));
        IdentifierToken writeTimeoutFieldName = createIdentifierToken("writeTimeout");
        ExpressionNode writeTimeOutDecimalLiteralNode = createRequiredExpressionNode(createIdentifierToken("-1"));
        RecordFieldWithDefaultValueNode writeTimeoutFieldNode = NodeFactory.createRecordFieldWithDefaultValueNode(
                writeTimeOutMetadata, null, writeTimeOutFieldType, writeTimeoutFieldName,
                equalToken, writeTimeOutDecimalLiteralNode,semicolonToken);
        recordFieldNodes.add(writeTimeoutFieldNode);


        // add secureSocket field
        MetadataNode secureSocketMetadata = getMetadataNode("SSL/TLS-related options");
        IdentifierToken secureSocketFieldName = AbstractNodeFactory.createIdentifierToken(SECURE_SOCKET_FIELD);
        TypeDescriptorNode secureSocketfieldType = createSimpleNameReferenceNode(
                createIdentifierToken("websocket:ClientSecureSocket"));
        RecordFieldWithDefaultValueNode secureSocketFieldNode = NodeFactory.createRecordFieldWithDefaultValueNode(
                secureSocketMetadata, null, secureSocketfieldType, secureSocketFieldName,
                equalToken,createRequiredExpressionNode(createIdentifierToken("()")), semicolonToken);
        recordFieldNodes.add(secureSocketFieldNode);


        // add maxFrameSize field
        MetadataNode maxFrameSizeMetadata = getMetadataNode(" The maximum payload size of a WebSocket" +
                " frame in bytes");
        TypeDescriptorNode maxFrameSizeFieldType =
                createSimpleNameReferenceNode(createToken(INT_KEYWORD));
        IdentifierToken maxFrameSizeFieldName = createIdentifierToken("maxFrameSize");
        ExpressionNode maxFrameSizeDecimalLiteralNode = createRequiredExpressionNode(
                createIdentifierToken("65536"));
        RecordFieldWithDefaultValueNode maxFrameSizeFieldNode = NodeFactory.createRecordFieldWithDefaultValueNode(
                maxFrameSizeMetadata, null, maxFrameSizeFieldType, maxFrameSizeFieldName,
                equalToken, maxFrameSizeDecimalLiteralNode,semicolonToken);
        recordFieldNodes.add(maxFrameSizeFieldNode);

        // add webSocketCompressionEnabled field
        MetadataNode webSocketCompressionEnabledMetadata = getMetadataNode("Enable support for compression in"
                + " the WebSocket");
        TypeDescriptorNode webSocketCompressionEnabledFieldType =
                createSimpleNameReferenceNode(createToken(BOOLEAN_KEYWORD));
        IdentifierToken webSocketCompressionEnabledFieldName = createIdentifierToken(
                "webSocketCompressionEnabled");
        RecordFieldWithDefaultValueNode webSocketCompressionEnabledFieldNode = NodeFactory.
                createRecordFieldWithDefaultValueNode(
                webSocketCompressionEnabledMetadata, null, webSocketCompressionEnabledFieldType,
                        webSocketCompressionEnabledFieldName,
                equalToken, createRequiredExpressionNode(
                                createToken(TRUE_KEYWORD)),semicolonToken);
        recordFieldNodes.add(webSocketCompressionEnabledFieldNode);


        // add handShakeTimeout field
        MetadataNode handShakeTimeoutMetadata = getMetadataNode("Time (in seconds) that a connection waits to " +
                "get the response of the WebSocket handshake");
        TypeDescriptorNode handShakeTimeoutFieldType =
                createSimpleNameReferenceNode(createToken(DECIMAL_KEYWORD));
        IdentifierToken handShakeTimeoutFieldName = createIdentifierToken("handShakeTimeout");
        ExpressionNode handShakeTimeoutDecimalLiteralNode = createRequiredExpressionNode(
                createIdentifierToken("300"));
        RecordFieldWithDefaultValueNode handShakeTimeoutFieldNode = NodeFactory.createRecordFieldWithDefaultValueNode(
                handShakeTimeoutMetadata, null, handShakeTimeoutFieldType, handShakeTimeoutFieldName,
                equalToken, handShakeTimeoutDecimalLiteralNode,semicolonToken);
        recordFieldNodes.add(handShakeTimeoutFieldNode);


        // add cookies field
        MetadataNode cookiesMetadata = getMetadataNode("An Array of http:Cookie");
        IdentifierToken cookiesFieldName = createIdentifierToken("cookies");
        NodeList<ArrayDimensionNode> cookiesArrayDimensions = NodeFactory.createEmptyNodeList();
        ArrayDimensionNode cookiesArrayDimension = NodeFactory.createArrayDimensionNode(
                createToken(SyntaxKind.OPEN_BRACKET_TOKEN), null,
                createToken(SyntaxKind.CLOSE_BRACKET_TOKEN));
        cookiesArrayDimensions = cookiesArrayDimensions.add(cookiesArrayDimension);
        TypeDescriptorNode cookiesArrayMemberType = createSimpleNameReferenceNode(createIdentifierToken
                ("http:Cookie"));
        ArrayTypeDescriptorNode cookiesFieldType = createArrayTypeDescriptorNode(cookiesArrayMemberType
                , cookiesArrayDimensions);
        RequiredExpressionNode cookiesExpression =
                createRequiredExpressionNode(createIdentifierToken("[]"));
        RecordFieldNode cookiesFieldNode = NodeFactory.createRecordFieldNode(
                cookiesMetadata, null, cookiesFieldType, cookiesFieldName,
                questionMarkToken,  semicolonToken);
        recordFieldNodes.add(cookiesFieldNode);


        // add pingPongHandler field
        MetadataNode pingPongHandlerMetadata = getMetadataNode("A service to handle the ping/pong frames");
        IdentifierToken pingPongHandlerFieldName = AbstractNodeFactory.createIdentifierToken(PING_PONG_SERVICE_FIELD);
        TypeDescriptorNode pingPongHandlerFieldType = createSimpleNameReferenceNode(
                createIdentifierToken("websocket:PingPongService"));
        RecordFieldNode pingPongHandlerFieldNode = NodeFactory.createRecordFieldNode(
                pingPongHandlerMetadata, null, pingPongHandlerFieldType, pingPongHandlerFieldName,
                questionMarkToken, semicolonToken);
        recordFieldNodes.add(pingPongHandlerFieldNode);

        // add retryConfig field
        MetadataNode retryConfigMetadata = getMetadataNode("Configurations associated with retrying");
        IdentifierToken retryConfigFieldName = AbstractNodeFactory.createIdentifierToken("retryConfig");
        TypeDescriptorNode returConfigFieldType = createSimpleNameReferenceNode(
                createIdentifierToken("websocket:WebSocketRetryConfig"));
        RecordFieldWithDefaultValueNode retryConfigFieldNode = NodeFactory.createRecordFieldWithDefaultValueNode(
                retryConfigMetadata, null, returConfigFieldType, retryConfigFieldName,
                equalToken,createRequiredExpressionNode(createIdentifierToken("()")), semicolonToken);
        recordFieldNodes.add(retryConfigFieldNode);


        // add validation field
        MetadataNode validationMetadata = getMetadataNode("Enable/disable constraint validation");
        IdentifierToken validationFieldName = AbstractNodeFactory.createIdentifierToken(VALIDATION);
        TypeDescriptorNode validationFieldType = createSimpleNameReferenceNode(createIdentifierToken(BOOLEAN));
        RecordFieldWithDefaultValueNode validateFieldNode = NodeFactory.createRecordFieldWithDefaultValueNode(
                validationMetadata, null, validationFieldType, validationFieldName,
                equalToken, createRequiredExpressionNode(createToken(TRUE_KEYWORD)), semicolonToken);
        recordFieldNodes.add(validateFieldNode);

        return recordFieldNodes;
    }

    /**
     * Generate statements for init function when combination of ApiKeys and HTTP/OAuth authentication is used.
     *
     * <pre>
     *     if config.auth is ApiKeysConfig {
     *         self.apiKeyConfig = (<ApiKeysConfig>config.auth).cloneReadOnly();
     *     } else {
     *         config.auth = <http:BearerTokenConfig>config.auth;
     *         self.apiKeyConfig = ();
     *     }
     * </pre>
     */
    public IfElseStatementNode handleInitForMixOfApiKeyAndHTTPOrOAuth() {
        List<StatementNode> apiKeyConfigAssignmentNodes = new ArrayList<>();

        // `self.apiKeyConfig = (<ApiKeysConfig>config.auth).cloneReadOnly();`
        FieldAccessExpressionNode apiKeyConfigRef = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(SELF)), createToken(DOT_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken(API_KEY_CONFIG_PARAM)));
        SimpleNameReferenceNode apiKeyConfigExpr = createSimpleNameReferenceNode(createIdentifierToken(
                "(<ApiKeysConfig>config.auth).cloneReadOnly()"));
        AssignmentStatementNode apiKeyConfigAssignmentStatementNode = createAssignmentStatementNode(apiKeyConfigRef,
                createToken(EQUAL_TOKEN), apiKeyConfigExpr, createToken(SEMICOLON_TOKEN));
        apiKeyConfigAssignmentNodes.add(apiKeyConfigAssignmentStatementNode);
        NodeList<StatementNode> statementList = createNodeList(apiKeyConfigAssignmentNodes);
        BlockStatementNode ifBody = createBlockStatementNode(createToken(OPEN_BRACE_TOKEN), statementList,
                createToken(CLOSE_BRACE_TOKEN));

        List<StatementNode> clientConfigAssignmentNodes = new ArrayList<>();

        // config.auth = <http:BearerTokenConfig>config.auth;
        FieldAccessExpressionNode clientConfigAuthRef = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(CONFIG)), createToken(DOT_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken(AUTH)));
        SimpleNameReferenceNode clientConfigExpr = createSimpleNameReferenceNode(
                createIdentifierToken("<" + getAuthFieldTypeName() +
                        ">" + CONFIG + DOT_TOKEN.stringValue() + AUTH));
        AssignmentStatementNode httpClientAuthConfigAssignment = createAssignmentStatementNode(clientConfigAuthRef,
                createToken(EQUAL_TOKEN), clientConfigExpr, createToken(SEMICOLON_TOKEN));
        clientConfigAssignmentNodes.add(httpClientAuthConfigAssignment);

        // `self.apiKeyConfig = ();`
        FieldAccessExpressionNode apiKeyConfigNilRef = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(SELF)), createToken(DOT_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken(API_KEY_CONFIG_PARAM)));
        SimpleNameReferenceNode apiKeyConfigNilExpr = createSimpleNameReferenceNode(
                createIdentifierToken("()"));
        AssignmentStatementNode apiKeyConfigNilAssignment = createAssignmentStatementNode(apiKeyConfigNilRef,
                createToken(EQUAL_TOKEN), apiKeyConfigNilExpr, createToken(SEMICOLON_TOKEN));
        clientConfigAssignmentNodes.add(apiKeyConfigNilAssignment);

        NodeList<StatementNode> elseBodyNodeList = createNodeList(clientConfigAssignmentNodes);
        StatementNode elseBodyStatement = createBlockStatementNode(createToken(OPEN_BRACE_TOKEN), elseBodyNodeList,
                createToken(CLOSE_BRACE_TOKEN));
        ElseBlockNode elseBody = createElseBlockNode(createToken(ELSE_KEYWORD), elseBodyStatement);

        ExpressionNode condition = createBinaryExpressionNode(null,
                createIdentifierToken(CONFIG + DOT_TOKEN.stringValue() + AUTH),
                createToken(IS_KEYWORD),
                createIdentifierToken(API_KEYS_CONFIG)
        );
        return createIfElseStatementNode(createToken(IF_KEYWORD), condition,
                ifBody, elseBody);
    }

    private List<Node> getCustomProxyRecordFields() {
        List<Node> recordFieldNodes = new ArrayList<>();
        Token semicolonToken = createToken(SEMICOLON_TOKEN);

        MetadataNode hostMetadataNode = getMetadataNode("Host name of the proxy server");
        IdentifierToken hostFieldName = createIdentifierToken(escapeIdentifier("host"));
        TypeDescriptorNode hostFieldType = createSimpleNameReferenceNode(createIdentifierToken(STRING));
        ExpressionNode hostDefaultValue = createRequiredExpressionNode(
                createIdentifierToken("\"\""));
        RecordFieldWithDefaultValueNode hostRecordField = createRecordFieldWithDefaultValueNode(hostMetadataNode,
                null, hostFieldType, hostFieldName, createToken(EQUAL_TOKEN),
                hostDefaultValue, semicolonToken);
        recordFieldNodes.add(hostRecordField);

        MetadataNode proxyMetadataNode = getMetadataNode("Proxy server port");
        IdentifierToken proxyFieldName = createIdentifierToken(escapeIdentifier("port"));
        TypeDescriptorNode proxyFieldType = createSimpleNameReferenceNode(createIdentifierToken("int"));
        ExpressionNode proxyDefaultValue = createRequiredExpressionNode(
                createIdentifierToken("0"));
        RecordFieldWithDefaultValueNode proxyRecordField = createRecordFieldWithDefaultValueNode(proxyMetadataNode,
                null, proxyFieldType, proxyFieldName, createToken(EQUAL_TOKEN),
                proxyDefaultValue, semicolonToken);
        recordFieldNodes.add(proxyRecordField);

        MetadataNode usernameMetadataNode = getMetadataNode("Proxy server username");
        IdentifierToken usernameFieldName = createIdentifierToken(escapeIdentifier("userName"));
        TypeDescriptorNode usernameFieldType = createSimpleNameReferenceNode(createIdentifierToken(STRING));
        ExpressionNode usernameDefaultValue = createRequiredExpressionNode(
                createIdentifierToken("\"\""));
        RecordFieldWithDefaultValueNode usernameRecordField = createRecordFieldWithDefaultValueNode(
                usernameMetadataNode, null, usernameFieldType, usernameFieldName,
                createToken(EQUAL_TOKEN),
                usernameDefaultValue, semicolonToken);
        recordFieldNodes.add(usernameRecordField);

        List<AnnotationNode> annotationNodes = Collections.singletonList(getDisplayAnnotationForPasswordField());

        MetadataNode passwordMetadataNode = getMetadataNode("Proxy server password", annotationNodes);
        IdentifierToken passwordFieldName = createIdentifierToken(escapeIdentifier("password"));
        TypeDescriptorNode passwordFieldType = createSimpleNameReferenceNode(createIdentifierToken(STRING));
        ExpressionNode passwordDefaultValue = createRequiredExpressionNode(
                createIdentifierToken("\"\""));
        RecordFieldWithDefaultValueNode passwordRecordField = createRecordFieldWithDefaultValueNode(
                passwordMetadataNode, null, passwordFieldType, passwordFieldName,
                createToken(EQUAL_TOKEN),
                passwordDefaultValue, semicolonToken);
        recordFieldNodes.add(passwordRecordField);

        return recordFieldNodes;
    }

    private AnnotationNode getDisplayAnnotationForRecord(String label) {
        List<Node> annotFields = new ArrayList<>();
        BasicLiteralNode labelExpr = createBasicLiteralNode(STRING_LITERAL,
                createLiteralValueToken(SyntaxKind.STRING_LITERAL_TOKEN,
                        "\"" + label + "\"",
                        createEmptyMinutiaeList(),
                        createEmptyMinutiaeList()));
        SpecificFieldNode labelField = createSpecificFieldNode(null,
                createIdentifierToken("label"),
                createToken(COLON_TOKEN), labelExpr);
        annotFields.add(labelField);
        MappingConstructorExpressionNode annotValue = createMappingConstructorExpressionNode(
                createToken(OPEN_BRACE_TOKEN), createSeparatedNodeList(annotFields),
                createToken(CLOSE_BRACE_TOKEN));

        SimpleNameReferenceNode annotateReference =
                createSimpleNameReferenceNode(createIdentifierToken("display"));

        return createAnnotationNode(createToken(SyntaxKind.AT_TOKEN)
                , annotateReference, annotValue);
    }

    private AnnotationNode getDisplayAnnotationForPasswordField() {
        List<Node> annotFields = new ArrayList<>();
        BasicLiteralNode labelExpr = createBasicLiteralNode(STRING_LITERAL,
                createLiteralValueToken(SyntaxKind.STRING_LITERAL_TOKEN,
                        "\"\"",
                        createEmptyMinutiaeList(),
                        createEmptyMinutiaeList()));
        SpecificFieldNode labelField = createSpecificFieldNode(null,
                createIdentifierToken("label"),
                createToken(COLON_TOKEN), labelExpr);
        annotFields.add(labelField);
        annotFields.add(createToken(COMMA_TOKEN));
        BasicLiteralNode kindExpr = createBasicLiteralNode(STRING_LITERAL,
                createLiteralValueToken(SyntaxKind.STRING_LITERAL_TOKEN,
                        "\"password\"",
                        createEmptyMinutiaeList(),
                        createEmptyMinutiaeList()));
        SpecificFieldNode kindField = createSpecificFieldNode(null,
                createIdentifierToken("kind"),
                createToken(COLON_TOKEN), kindExpr);
        annotFields.add(kindField);

        MappingConstructorExpressionNode annotValue = createMappingConstructorExpressionNode(
                createToken(OPEN_BRACE_TOKEN), createSeparatedNodeList(annotFields),
                createToken(CLOSE_BRACE_TOKEN));

        SimpleNameReferenceNode annotateReference =
                createSimpleNameReferenceNode(createIdentifierToken("display"));

        return createAnnotationNode(createToken(SyntaxKind.AT_TOKEN)
                , annotateReference, annotValue);
    }

    private List<Node> getClientHttp1SettingsRecordFields() {
        List<Node> recordFieldNodes = new ArrayList<>();
        Token semicolonToken = createToken(SEMICOLON_TOKEN);

        MetadataNode keepAliveMetadataNode = getMetadataNode(
                "Specifies whether to reuse a connection for multiple requests");
        IdentifierToken keepAliveFieldName = createIdentifierToken(escapeIdentifier(KEEP_ALIVE));
        TypeDescriptorNode keepAliveFieldType = createSimpleNameReferenceNode(
                createIdentifierToken("http:KeepAlive"));
        ExpressionNode keepAliveDefaultValue = createRequiredExpressionNode(
                createIdentifierToken("http:KEEPALIVE_AUTO"));
        RecordFieldWithDefaultValueNode keepAliveRecordField = createRecordFieldWithDefaultValueNode(
                keepAliveMetadataNode, null, keepAliveFieldType, keepAliveFieldName,
                createToken(EQUAL_TOKEN), keepAliveDefaultValue, semicolonToken);
        recordFieldNodes.add(keepAliveRecordField);

        MetadataNode chunkingMetadataNode = getMetadataNode("The chunking behaviour of the request");
        IdentifierToken chunkingFieldName = createIdentifierToken(escapeIdentifier(CHUNKING));
        TypeDescriptorNode chunkingFieldType = createSimpleNameReferenceNode(
                createIdentifierToken("http:Chunking"));
        ExpressionNode chunkingDefaultValue = createRequiredExpressionNode(
                createIdentifierToken("http:CHUNKING_AUTO"));
        RecordFieldWithDefaultValueNode chunkingRecordField = createRecordFieldWithDefaultValueNode(
                chunkingMetadataNode, null, chunkingFieldType, chunkingFieldName,
                createToken(EQUAL_TOKEN), chunkingDefaultValue, semicolonToken);
        recordFieldNodes.add(chunkingRecordField);

        MetadataNode proxyMetadataNode = getMetadataNode("Proxy server related options");
        IdentifierToken proxyFieldName = AbstractNodeFactory.createIdentifierToken(PROXY_CONFIG);
        TypeDescriptorNode proxyFieldType = createSimpleNameReferenceNode(
                createIdentifierToken("ProxyConfig"));
        RecordFieldNode proxyRecordField = NodeFactory.createRecordFieldNode(
                proxyMetadataNode, null, proxyFieldType, proxyFieldName,
                createToken(QUESTION_MARK_TOKEN), semicolonToken);
        recordFieldNodes.add(proxyRecordField);

        return recordFieldNodes;

    }

    private MetadataNode getMetadataNode(String comment) {

        List<Node> docs = new ArrayList<>(DocCommentsGenerator.createAPIDescriptionDoc(comment, false));
        MarkdownDocumentationNode authDocumentationNode = createMarkdownDocumentationNode(
                createNodeList(docs));
        return createMetadataNode(authDocumentationNode, createEmptyNodeList());
    }

    private MetadataNode getMetadataNode(String comment, List<AnnotationNode> annotationNodes) {
        List<Node> docs = new ArrayList<>(DocCommentsGenerator.createAPIDescriptionDoc(comment, false));
        MarkdownDocumentationNode authDocumentationNode = createMarkdownDocumentationNode(
                createNodeList(docs));
        return createMetadataNode(authDocumentationNode, createNodeList(annotationNodes));
    }

    /**
     * Travers through the security schemas of the given async api spec.
     * Store api key names which needs to send in the query string and as a request header separately.
     *
     * @param securitySchemeMap Map of security schemas of the given async api spec
     */
    public void setAuthTypes(Map<String, SecurityScheme> securitySchemeMap) throws
            BallerinaAsyncApiException {

        for (Map.Entry<String, SecurityScheme> securitySchemeEntry : securitySchemeMap.entrySet()) {
            AsyncApi25SecuritySchemeImpl securitySchemaValue = (AsyncApi25SecuritySchemeImpl)securitySchemeEntry.getValue();
            if (securitySchemaValue != null && securitySchemaValue.getType() != null) {
                String schemaType = securitySchemaValue.getType().toLowerCase(Locale.getDefault());

                //TODO: Here apiKey and userPassword have to be map
                switch (schemaType) {
                    case HTTP:
                        httpOROAuth = true;
                        String scheme = securitySchemaValue.getScheme();
                        if (scheme.equals(BASIC)) {
                            authTypes.add(BASIC);
                        } else if (scheme.equals(BEARER)) {
                            authTypes.add(BEARER);
                        }
                        break;
                    case OAUTH2:
                        httpOROAuth = true;
                        if (securitySchemaValue.getFlows().getClientCredentials() != null) {
                            if (securitySchemaValue.getFlows().getClientCredentials().getTokenUrl() != null) {
                                clientCredGrantTokenUrl = securitySchemaValue.getFlows().getClientCredentials().getTokenUrl();
                            }
                            authTypes.add(CLIENT_CRED);
                        }
                        if (securitySchemaValue.getFlows().getPassword() != null) {
                            if (securitySchemaValue.getFlows().getPassword().getTokenUrl() != null) {
                                passwordGrantTokenUrl = securitySchemaValue.getFlows().getPassword().getTokenUrl();
                            }
                            authTypes.add(PASSWORD);
                        }
                        if (securitySchemaValue.getFlows().getAuthorizationCode() != null) {
                            if (securitySchemaValue.getFlows().getAuthorizationCode().getTokenUrl() != null) {
                                refreshTokenUrl = securitySchemaValue.getFlows().getAuthorizationCode().getTokenUrl();
                            }
                            authTypes.addAll(Arrays.asList(BEARER, REFRESH_TOKEN));
                        }
                        if (securitySchemaValue.getFlows().getImplicit() != null) {
                            authTypes.add(BEARER);
                        }
                        break;
                    case HTTP_API_KEY:
                        httpApiKey = true;
                        String apiKeyType = securitySchemaValue.getIn().toLowerCase(Locale.getDefault());
                        authTypes.add(HTTP_API_KEY);
                        setApiKeysConfigRecordFields(securitySchemaValue);
                        switch (apiKeyType) {
                            case "query":
                                queryApiKeyNameList.put(securitySchemeEntry.getKey(), securitySchemaValue.getName());
                                break;
                            case "header":
                                headerApiKeyNameList.put(securitySchemeEntry.getKey(), securitySchemaValue.getName());
                                break;
                            default:
                                break;
                        }
                        break;

                    case USER_PASSWORD:
                        throw new BallerinaAsyncApiException("userPassword type security schema doesn't support yet");
                    case API_KEY:
                        throw new BallerinaAsyncApiException("apiKey type security schema doesn't support yet");


                }
            }
        }
        if (!(httpApiKey || httpOROAuth)) {
            throw new BallerinaAsyncApiException("Ballerina unsupported type of security schema");
        }
    }

    /**
     * Returns fields in ApiKeysConfig record.
     * <pre>
     *     # API key related to connector authentication
     *     string apiKey;
     * </pre>
     */
    private void setApiKeysConfigRecordFields(AsyncApi25SecuritySchemeImpl securityScheme) {

        MetadataNode metadataNode = null;
        if (securityScheme.getDescription() != null) {
//            List<AnnotationNode> annotationNodes = Collections.singletonList(getDisplayAnnotationForPasswordField());
//            metadataNode = getMetadataNode(securityScheme.getDescription(), annotationNodes);
            metadataNode = getMetadataNode(securityScheme.getDescription());

        }
        TypeDescriptorNode stringTypeDesc = createSimpleNameReferenceNode(createToken(STRING_KEYWORD));
        IdentifierToken apiKeyName = createIdentifierToken(getValidName(securityScheme.getName(), false));
        apiKeysConfigRecordFields.add(createRecordFieldNode(metadataNode, null, stringTypeDesc,
                apiKeyName, null, createToken(SEMICOLON_TOKEN)));
    }

    /**
     * Travers through the authTypes and generate the field type name of auth field in ClientConfig record.

     * @return {@link String}   Field type name of auth field
     * Ex: {@code http:BearerTokenConfig|http:OAuth2RefreshTokenGrantConfig}
     */
    private String getAuthFieldTypeName() {
        Set<String> httpFieldTypeNames = new HashSet<>();
        for (String authType : authTypes) {
            switch (authType) {
                case BEARER:
                    httpFieldTypeNames.add(GeneratorConstants.AuthConfigTypes.BEARER.getValue());
                    break;
                case BASIC:
                    httpFieldTypeNames.add(GeneratorConstants.AuthConfigTypes.BASIC.getValue());
                    break;
                case CLIENT_CRED:
                    // Previous version of swagger parser returns null value, when it has an
                    // empty string as a value (ex: tokenURL: ""). Latest version `2.0.30` version
                    // returns empty string as it is. Therefore, we have to check both null check and empty string
                    // check.
                    if (clientCredGrantTokenUrl != null && !clientCredGrantTokenUrl.isBlank()) {
                        httpFieldTypeNames.add(GeneratorConstants.AuthConfigTypes.CUSTOM_CLIENT_CREDENTIAL.getValue());
                    } else {
                        httpFieldTypeNames.add(GeneratorConstants.AuthConfigTypes.CLIENT_CREDENTIAL.getValue());
                    }
                    break;
                case PASSWORD:
                    if (passwordGrantTokenUrl != null && !passwordGrantTokenUrl.isBlank()) {
                        httpFieldTypeNames.add(GeneratorConstants.AuthConfigTypes.CUSTOM_PASSWORD.getValue());
                    } else {
                        httpFieldTypeNames.add(GeneratorConstants.AuthConfigTypes.PASSWORD.getValue());
                    }
                    break;
                case REFRESH_TOKEN:
                    if (refreshTokenUrl != null && !refreshTokenUrl.isBlank()) {
                        httpFieldTypeNames.add(GeneratorConstants.AuthConfigTypes.CUSTOM_REFRESH_TOKEN.getValue());
                    } else {
                        httpFieldTypeNames.add(GeneratorConstants.AuthConfigTypes.REFRESH_TOKEN.getValue());
                    }
                    break;
                default:
                    break;
            }
        }
        return buildConfigRecordFieldTypes(httpFieldTypeNames).toString();
    }

    /**
     * This method is used concat the config record authConfig field type.
     *
     * @param fieldtypes Type name set from {@link #setAuthTypes(Map)} method.
     * @return {@link String}   Pipe concatenated list of type names
     */
    private StringBuilder buildConfigRecordFieldTypes(Set<String> fieldtypes) {
        StringBuilder httpAuthFieldTypes = new StringBuilder();
        if (!fieldtypes.isEmpty()) {
            for (String fieldType : fieldtypes) {
                if (httpAuthFieldTypes.length() != 0) {
                    httpAuthFieldTypes.append("|").append(fieldType);
                } else {
                    httpAuthFieldTypes.append(fieldType);
                }
            }
        }
        return httpAuthFieldTypes;
    }
}
