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
package io.ballerina.asyncapi.generator.ws.client.generator;

import com.fasterxml.jackson.databind.JsonNode;
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.core.model.channel.AsyncApiChannel;
import io.ballerina.asyncapi.core.model.component.AsyncApiComponent;
import io.ballerina.asyncapi.core.model.security.AsyncApiSecurityScheme;
import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.ws.client.utils.CodegenUtils;
import io.ballerina.compiler.syntax.tree.AbstractNodeFactory;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.BlockStatementNode;
import io.ballerina.compiler.syntax.tree.BuiltinSimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.DefaultableParameterNode;
import io.ballerina.compiler.syntax.tree.ElseBlockNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FieldAccessExpressionNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.IfElseStatementNode;
import io.ballerina.compiler.syntax.tree.MarkdownDocumentationNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.ObjectFieldNode;
import io.ballerina.compiler.syntax.tree.RecordTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createAssignmentStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBasicLiteralNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBinaryExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBlockStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBuiltinSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createDefaultableParameterNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createElseBlockNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFieldAccessExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createIfElseStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createIntersectionTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMarkdownDocumentationNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMetadataNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createObjectFieldNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createOptionalTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRecordFieldNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRecordTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRequiredExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRequiredParameterNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTypeDefinitionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTypeReferenceTypeDescNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.BITWISE_AND_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACE_PIPE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.COMMA_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.DOT_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ELSE_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.EQUAL_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FINAL_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.IF_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.IS_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_PIPE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PUBLIC_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.QUESTION_MARK_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.READONLY_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RECORD_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SEMICOLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.STRING_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.STRING_LITERAL;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.TYPE_KEYWORD;

/**
 * Generates authentication-related Ballerina syntax tree nodes for a WebSocket client.
 */
public class BallerinaAuthConfigGenerator {

    private static final String CONNECTION_CONFIG = "websocket:ClientConfiguration";
    private static final String SERVICE_URL = "serviceUrl";
    private static final String API_KEY_CONFIG = "apiKeyConfig";
    private static final String API_KEYS_CONFIG = "ApiKeysConfig";
    private static final String AUTH = "auth";
    private static final String CLIENT_CONFIG = "clientConfig";
    private static final String QUERY_PARAMS = "queryParams";
    private static final String SELF = "self";

    private static final String HTTP_TYPE = "http";
    private static final String HTTP_API_KEY_TYPE = "httpApiKey";
    private static final String OAUTH2_TYPE = "oauth2";
    private static final String BASIC_SCHEME = "basic";
    private static final String BEARER_SCHEME = "bearer";
    private static final String PASSWORD_GRANT = "password";
    private static final String CLIENT_CRED_GRANT = "clientCred";
    private static final String REFRESH_TOKEN_GRANT = "refreshToken";
    private static final String USER_PASSWORD_TYPE = "userPassword";
    private static final String API_KEY_TYPE = "apiKey";
    private static final String PATH_PARAMS_TYPE = "PathParams";
    private static final String HEADER_PARAMS_TYPE = "HeaderParams";

    private final Map<String, String> headerApiKeyNameList = new HashMap<>();
    private final Map<String, String> queryApiKeyNameList = new HashMap<>();
    private final List<Node> apiKeysConfigRecordFields = new ArrayList<>();
    private final Set<String> authTypes = new LinkedHashSet<>();
    private boolean httpApiKey;
    private boolean isPathParam = false;
    private boolean isQueryParam = false;
    private boolean isHeaderParam = false;
    private boolean httpOROAuth;
    private String clientCredGrantTokenUrl;
    private String passwordGrantTokenUrl;
    private String refreshTokenUrl;
    private List<TypeDefinitionNode> authRelatedTypeDefinitionNodes = new ArrayList<>();

    /**
     * Creates a new {@link BallerinaAuthConfigGenerator} with no authentication configured.
     */
    public BallerinaAuthConfigGenerator() {
        this.httpApiKey = false;
        this.httpOROAuth = false;
    }

    /**
     * Returns {@code true} if API-key authentication is used.
     *
     * @return whether API-key auth is active
     */
    public boolean isHttpApiKey() {
        return httpApiKey;
    }


    /**
     * Returns {@code true} if path parameters were found while processing the channel.
     *
     * @return whether path parameters exist
     */
    public boolean isPathParam() {
        return isPathParam;
    }

    /**
     * Returns {@code true} if query parameters were found while processing the channel.
     *
     * @return whether query parameters exist
     */
    public boolean isQueryParam() {
        return isQueryParam;
    }

    /**
     * Returns {@code true} if header parameters were found while processing the channel.
     *
     * @return whether header parameters exist
     */
    public boolean isHeaderParam() {
        return isHeaderParam;
    }

    /**
     * Returns the map of security scheme names to header API-key parameter names.
     *
     * @return header API-key name map
     */
    public Map<String, String> getHeaderApiKeyNameList() {
        return headerApiKeyNameList;
    }

    /**
     * Returns the map of security scheme names to query API-key parameter names.
     *
     * @return query API-key name map
     */
    public Map<String, String> getQueryApiKeyNameList() {
        return queryApiKeyNameList;
    }

    /**
     * Returns the set of auth type identifiers derived from the security schemes.
     *
     * @return auth type set
     */
    public Set<String> getAuthType() {
        return authTypes;
    }

    /**
     * Returns all authentication-related type definition nodes that were generated.
     *
     * @return list of auth-related type definition nodes
     */
    public List<TypeDefinitionNode> getAuthRelatedTypeDefinitionNodes() {
        return authRelatedTypeDefinitionNodes;
    }

    /**
     * Reads security schemes from the given spec, sets auth flags, and generates
     * the corresponding type definition nodes (e.g. {@code ApiKeysConfig}).
     *
     * @param asyncApiSpec the parsed AsyncAPI specification
     * @throws GeneratorException if an unsupported security scheme type is encountered
     */
    public void addAuthRelatedRecords(AsyncApiSpec asyncApiSpec) throws GeneratorException {
        List<TypeDefinitionNode> nodes = new ArrayList<>();
        Optional<AsyncApiComponent> components = asyncApiSpec.getAsyncApiComponents();
        if (components.isPresent() && components.get().securitySchemes() != null) {
            setAuthTypes(components.get().securitySchemes());
            if (isHttpApiKey()) {
                nodes.add(generateApiKeysConfig());
            }
            if (clientCredGrantTokenUrl != null) {
                nodes.add(getOAuth2ClientCredsGrantConfigRecord());
            }
            if (passwordGrantTokenUrl != null) {
                nodes.add(getOAuth2PasswordGrantConfigRecord());
            }
            if (refreshTokenUrl != null) {
                nodes.add(getOAuth2RefreshTokenGrantConfigRecord());
            }
        }
        this.authRelatedTypeDefinitionNodes = nodes;
    }

    /**
     * Inspects each security scheme and sets the auth-type flags and API-key name maps.
     *
     * @param securitySchemeMap the security scheme entries from the AsyncAPI components
     * @throws GeneratorException if an unsupported scheme type is found
     */
    public void setAuthTypes(Map<String, AsyncApiSecurityScheme> securitySchemeMap) throws GeneratorException {
        for (Map.Entry<String, AsyncApiSecurityScheme> schemeEntry : securitySchemeMap.entrySet()) {
            AsyncApiSecurityScheme scheme = schemeEntry.getValue();
            if (scheme == null || scheme.type() == null) {
                continue;
            }
            String schemaType = scheme.type();
            switch (schemaType) {
                case HTTP_TYPE:
                    httpOROAuth = true;
                    String httpScheme = scheme.scheme();
                    if (BASIC_SCHEME.equals(httpScheme)) {
                        authTypes.add(BASIC_SCHEME);
                    } else if (BEARER_SCHEME.equals(httpScheme)) {
                        authTypes.add(BEARER_SCHEME);
                    }
                    break;
                case OAUTH2_TYPE:
                    processOAuth2Scheme(scheme);
                    break;
                case HTTP_API_KEY_TYPE:
                    httpApiKey = true;
                    authTypes.add(HTTP_API_KEY_TYPE);
                    addApiKeyEntry(schemeEntry.getKey(), scheme);
                    break;
                case USER_PASSWORD_TYPE:
                    throw new GeneratorException("userPassword security schema type is not supported");
                case API_KEY_TYPE:
                    throw new GeneratorException("apiKey security schema type is not supported");
                default:
                    break;
            }
        }
    }

    /**
     * Builds function parameter nodes for path, header, and query parameters from the channel.
     * Appends type definition nodes for {@code PathParams}, {@code HeaderParams}, and
     * {@code QueryParams} to {@link #getAuthRelatedTypeDefinitionNodes()} as needed.
     *
     * @param channel         the channel whose parameters and bindings are inspected
     * @param parameterList   the list to which new parameter nodes are appended
     * @param comma           separator token placed after each appended parameter
     * @param queryJsonSchema the {@code query} schema from the channel's WS bindings (may be null)
     * @param headerJsonSchema the {@code headers} schema from the channel's WS bindings (may be null)
     * @throws GeneratorException if an unsupported parameter type is found
     */
    public void setFunctionParameters(AsyncApiChannel channel, List<Node> parameterList, Token comma,
                                      JsonNode queryJsonSchema, JsonNode headerJsonSchema)
            throws GeneratorException {
        if (channel.parameters() != null && !channel.parameters().isEmpty()) {
            buildPathParams(channel, parameterList, comma);
        }
        if (headerJsonSchema != null && headerJsonSchema.get("properties") != null) {
            buildHeaderParams(headerJsonSchema, parameterList, comma);
        }
        if (queryJsonSchema != null && queryJsonSchema.get("properties") != null) {
            buildQueryParams(queryJsonSchema, parameterList, comma);
        }
    }

    /**
     * Builds the parameter string for the client {@code init} function signature as source text.
     * The exact parameters depend on the active auth type.
     *
     * @param serviceUrl the server URL (used as the default value, or {@code "/"} for required)
     * @return the parameter source string (e.g. {@code "websocket:ClientConfiguration clientConfig = {}, string
     * serviceUrl = \"ws://...\""}
     */
    public String buildInitParamString(String serviceUrl) {
        boolean required = "/".equals(serviceUrl);
        String serviceUrlParam = required
                ? "string " + SERVICE_URL
                : "string " + SERVICE_URL + " = \"" + serviceUrl + "\"";
        if (httpOROAuth) {
            return CONNECTION_CONFIG + " " + CLIENT_CONFIG + ", " + serviceUrlParam;
        } else if (httpApiKey) {
            return API_KEYS_CONFIG + " " + API_KEY_CONFIG + ", "
                    + CONNECTION_CONFIG + " " + CLIENT_CONFIG + " = {}, " + serviceUrlParam;
        } else {
            return CONNECTION_CONFIG + " " + CLIENT_CONFIG + " = {}, " + serviceUrlParam;
        }
    }

    /**
     * Appends the config and serviceUrl parameters to the client {@code init} function signature.
     * The exact parameters depend on the active auth type.
     *
     * @param serviceUrl the server URL (used as the default value, or {@code "/"} for required)
     * @param parameters the parameter list to which nodes are appended
     */
    private void getConfigParamForClassInit(String serviceUrl, List<Node> parameters) {
        NodeList<AnnotationNode> annotations = createEmptyNodeList();
        Node serviceUrlNode = getServiceURLNode(serviceUrl);
        Token comma = createToken(COMMA_TOKEN);

        if (httpOROAuth) {
            BuiltinSimpleNameReferenceNode configType = createBuiltinSimpleNameReferenceNode(null,
                    createIdentifierToken(CONNECTION_CONFIG));
            RequiredParameterNode configParam = createRequiredParameterNode(annotations, configType,
                    createIdentifierToken(CLIENT_CONFIG));
            parameters.add(configParam);
            parameters.add(comma);
            parameters.add(serviceUrlNode);
        } else {
            if (httpApiKey) {
                BuiltinSimpleNameReferenceNode apiKeyType = createBuiltinSimpleNameReferenceNode(null,
                        createIdentifierToken(API_KEYS_CONFIG));
                RequiredParameterNode apiKeyParam = createRequiredParameterNode(annotations, apiKeyType,
                        createIdentifierToken(API_KEY_CONFIG));
                parameters.add(apiKeyParam);
                parameters.add(comma);
            }
            BuiltinSimpleNameReferenceNode configType = createBuiltinSimpleNameReferenceNode(null,
                    createIdentifierToken(CONNECTION_CONFIG));
            BasicLiteralNode emptyExpr = createBasicLiteralNode(null, createIdentifierToken(" {}"));
            DefaultableParameterNode configParam = createDefaultableParameterNode(annotations, configType,
                    createIdentifierToken(CLIENT_CONFIG), createToken(EQUAL_TOKEN), emptyExpr);
            if (serviceUrlNode instanceof RequiredParameterNode) {
                parameters.add(serviceUrlNode);
                parameters.add(comma);
                parameters.add(configParam);
            } else {
                parameters.add(configParam);
                parameters.add(comma);
                parameters.add(serviceUrlNode);
            }
        }
    }

    /**
     * Generates the {@code final readonly & ApiKeysConfig apiKeyConfig;} class-level field node.
     * Returns {@code null} when API-key auth is not active.
     *
     * @return the object field node, or {@code null}
     */
    public ObjectFieldNode getApiKeyMapClassVariable() {
        if (!httpApiKey) {
            return null;
        }
        NodeList<Token> qualifiers = createNodeList(createToken(FINAL_KEYWORD));
        TypeDescriptorNode readonlyNode = createTypeReferenceTypeDescNode(
                createSimpleNameReferenceNode(createToken(READONLY_KEYWORD)));
        TypeDescriptorNode apiKeyMapType = createSimpleNameReferenceNode(createIdentifierToken(API_KEYS_CONFIG));
        if (httpOROAuth) {
            apiKeyMapType = createOptionalTypeDescriptorNode(apiKeyMapType, createToken(QUESTION_MARK_TOKEN));
        }
        TypeDescriptorNode intersectionType = createIntersectionTypeDescriptorNode(
                readonlyNode, createToken(BITWISE_AND_TOKEN), apiKeyMapType);
        MetadataNode metadata = createMetadataNode(null, createEmptyNodeList());
        return createObjectFieldNode(metadata, null, qualifiers, intersectionType,
                createIdentifierToken(API_KEY_CONFIG), null, null, createToken(SEMICOLON_TOKEN));
    }

    /**
     * Generates the {@code ApiKeysConfig} closed record type definition.
     *
     * @return the {@code ApiKeysConfig} type definition node
     */
    public TypeDefinitionNode generateApiKeysConfig() {
        MetadataNode metadata = getMetadataNode(
                "Provides API key configurations needed when communicating with a remote WebSocket service.");
        Token typeName = AbstractNodeFactory.createIdentifierToken(API_KEYS_CONFIG);
        NodeList<Node> fields = createNodeList(apiKeysConfigRecordFields);
        RecordTypeDescriptorNode recordType = createRecordTypeDescriptorNode(createToken(RECORD_KEYWORD),
                createToken(OPEN_BRACE_PIPE_TOKEN), fields, null, createToken(CLOSE_BRACE_PIPE_TOKEN));
        return createTypeDefinitionNode(metadata, createToken(PUBLIC_KEYWORD), createToken(TYPE_KEYWORD),
                typeName, recordType, createToken(SEMICOLON_TOKEN));
    }

    /**
     * Generates the {@code if config.auth is ApiKeysConfig {...} else {...}} statement
     * used in the init function when both API-key and HTTP/OAuth authentication are active.
     *
     * @return the if/else statement node
     */
    private IfElseStatementNode handleInitForMixOfApiKeyAndHTTPOrOAuth() {
        List<StatementNode> ifStatements = new ArrayList<>();
        FieldAccessExpressionNode apiKeyRef = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(SELF)), createToken(DOT_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken(API_KEY_CONFIG)));
        SimpleNameReferenceNode cloneExpr = createSimpleNameReferenceNode(
                createIdentifierToken(String.format("(<%s>config.%s).cloneReadOnly()",
                        API_KEYS_CONFIG, AUTH)));
        ifStatements.add(createAssignmentStatementNode(apiKeyRef, createToken(EQUAL_TOKEN),
                cloneExpr, createToken(SEMICOLON_TOKEN)));
        BlockStatementNode ifBody = createBlockStatementNode(createToken(OPEN_BRACE_TOKEN),
                createNodeList(ifStatements), createToken(CLOSE_BRACE_TOKEN));

        List<StatementNode> elseStatements = new ArrayList<>();
        FieldAccessExpressionNode configAuthRef = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(CLIENT_CONFIG)), createToken(DOT_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken(AUTH)));
        SimpleNameReferenceNode castExpr = createSimpleNameReferenceNode(createIdentifierToken(
                String.format("<%s>%s.%s", getAuthFieldTypeName(), CLIENT_CONFIG, AUTH)));
        elseStatements.add(createAssignmentStatementNode(configAuthRef, createToken(EQUAL_TOKEN),
                castExpr, createToken(SEMICOLON_TOKEN)));
        FieldAccessExpressionNode apiKeyNilRef = createFieldAccessExpressionNode(
                createSimpleNameReferenceNode(createIdentifierToken(SELF)), createToken(DOT_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken(API_KEY_CONFIG)));
        elseStatements.add(createAssignmentStatementNode(apiKeyNilRef, createToken(EQUAL_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken("()")),
                createToken(SEMICOLON_TOKEN)));
        BlockStatementNode elseBodyBlock = createBlockStatementNode(createToken(OPEN_BRACE_TOKEN),
                createNodeList(elseStatements), createToken(CLOSE_BRACE_TOKEN));
        ElseBlockNode elseBody = createElseBlockNode(createToken(ELSE_KEYWORD), elseBodyBlock);

        ExpressionNode condition = createBinaryExpressionNode(null,
                createIdentifierToken(String.format("%s.%s", CLIENT_CONFIG, AUTH)),
                createToken(IS_KEYWORD),
                createIdentifierToken(API_KEYS_CONFIG));
        return createIfElseStatementNode(createToken(IF_KEYWORD), condition, ifBody, elseBody);
    }

    private void processOAuth2Scheme(AsyncApiSecurityScheme scheme) {
        httpOROAuth = true;
        if (scheme.flows() == null) {
            return;
        }
        if (scheme.flows().clientCredentials() != null) {
            if (scheme.flows().clientCredentials().tokenUrl() != null) {
                clientCredGrantTokenUrl = scheme.flows().clientCredentials().tokenUrl().toString();
            }
            authTypes.add(CLIENT_CRED_GRANT);
        }
        if (scheme.flows().password() != null) {
            if (scheme.flows().password().tokenUrl() != null) {
                passwordGrantTokenUrl = scheme.flows().password().tokenUrl().toString();
            }
            authTypes.add(PASSWORD_GRANT);
        }
        if (scheme.flows().authorizationCode() != null) {
            if (scheme.flows().authorizationCode().tokenUrl() != null) {
                refreshTokenUrl = scheme.flows().authorizationCode().tokenUrl().toString();
            }
            authTypes.add(BEARER_SCHEME);
            authTypes.add(REFRESH_TOKEN_GRANT);
        }
        if (scheme.flows().implicit() != null) {
            authTypes.add(BEARER_SCHEME);
        }
    }

    private void addApiKeyEntry(String schemeKey, AsyncApiSecurityScheme scheme) {
        String apiKeyIn = scheme.in() != null ? scheme.in().toLowerCase(Locale.ROOT) : "";
        if ("query".equals(apiKeyIn)) {
            queryApiKeyNameList.put(schemeKey, scheme.name());
        } else if ("header".equals(apiKeyIn)) {
            headerApiKeyNameList.put(schemeKey, scheme.name());
        }
        MetadataNode metadata = scheme.description() != null ? getMetadataNode(scheme.description()) : null;
        TypeDescriptorNode stringType = createSimpleNameReferenceNode(createToken(STRING_KEYWORD));
        IdentifierToken fieldName = createIdentifierToken(CodegenUtils.getValidName(scheme.name(), false));
        apiKeysConfigRecordFields.add(createRecordFieldNode(metadata, null, stringType,
                fieldName, null, createToken(SEMICOLON_TOKEN)));
    }

    private void buildPathParams(AsyncApiChannel channel, List<Node> parameterList, Token comma)
            throws GeneratorException {
        List<Node> docNodes = new ArrayList<>(DocCommentsGenerator.createAPIDescriptionDoc(
                "Path parameters as a record", true));
        List<Node> fields = new ArrayList<>();
        for (Map.Entry<String, io.ballerina.asyncapi.core.model.channel.AsyncApiChannelParameter> entry
                : channel.parameters().entrySet()) {
            String paramName = CodegenUtils.escapeIdentifier(entry.getKey());
            if (entry.getValue() != null && entry.getValue().description() != null) {
                docNodes.add(DocCommentsGenerator.createAPIParamDoc(
                        CodegenUtils.getValidName(entry.getKey(), false), entry.getValue().description()));
            }
            TypeDescriptorNode fieldType = createSimpleNameReferenceNode(createToken(STRING_KEYWORD));
            fields.add(createRecordFieldNode(null, null, fieldType,
                    createIdentifierToken(paramName), null, createToken(SEMICOLON_TOKEN)));
        }
        authRelatedTypeDefinitionNodes.add(
                buildClosedRecordTypeDefinition(PATH_PARAMS_TYPE, fields, docNodes));
        RequiredParameterNode pathParam = createRequiredParameterNode(createEmptyNodeList(),
                createBuiltinSimpleNameReferenceNode(null, createIdentifierToken(PATH_PARAMS_TYPE)),
                createIdentifierToken("pathParams"));
        parameterList.add(pathParam);
        parameterList.add(comma);
        isPathParam = true;
    }

    private void buildHeaderParams(JsonNode headerJsonSchema, List<Node> parameterList, Token comma)
            throws GeneratorException {
        List<Node> docNodes = new ArrayList<>(DocCommentsGenerator.createAPIDescriptionDoc(
                "Header parameters as a record", true));
        List<Node> fields = buildJsonSchemaRecordFields(headerJsonSchema);
        authRelatedTypeDefinitionNodes.add(
                buildClosedRecordTypeDefinition(HEADER_PARAMS_TYPE, fields, docNodes));
        RequiredParameterNode headerParam = createRequiredParameterNode(createEmptyNodeList(),
                createBuiltinSimpleNameReferenceNode(null, createIdentifierToken(HEADER_PARAMS_TYPE)),
                createIdentifierToken("headerParams"));
        parameterList.add(headerParam);
        parameterList.add(comma);
        isHeaderParam = true;
    }

    private void buildQueryParams(JsonNode queryJsonSchema, List<Node> parameterList, Token comma)
            throws GeneratorException {
        List<Node> docNodes = new ArrayList<>(DocCommentsGenerator.createAPIDescriptionDoc(
                "Query parameters as a record", true));
        List<Node> fields = buildJsonSchemaRecordFields(queryJsonSchema);
        authRelatedTypeDefinitionNodes.add(
                buildClosedRecordTypeDefinition("QueryParams", fields, docNodes));
        RequiredParameterNode queryParam = createRequiredParameterNode(createEmptyNodeList(),
                createBuiltinSimpleNameReferenceNode(null, createIdentifierToken("QueryParams")),
                createIdentifierToken(QUERY_PARAMS));
        parameterList.add(queryParam);
        parameterList.add(comma);
        isQueryParam = true;
    }

    private List<Node> buildJsonSchemaRecordFields(JsonNode schemaNode) throws GeneratorException {
        List<Node> fields = new ArrayList<>();
        JsonNode propsNode = schemaNode.get("properties");
        Iterator<String> fieldNames = propsNode.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            JsonNode propSchema = propsNode.get(fieldName);
            String typeStr = getTypeFromJsonSchema(propSchema);
            if ("array".equals(typeStr)) {
                throw new GeneratorException(
                        String.format("Ballerina does not support array type binding parameters: %s",
                                fieldName));
            }
            if ("object".equals(typeStr)) {
                throw new GeneratorException(
                        String.format("Ballerina does not support record type binding parameters: %s",
                                fieldName));
            }
            String ballerinaType = CodegenUtils.TYPE_MAP.getOrDefault(typeStr, "string");
            TypeDescriptorNode fieldType = createSimpleNameReferenceNode(createIdentifierToken(ballerinaType));
            fields.add(createRecordFieldNode(null, null, fieldType,
                    createIdentifierToken(CodegenUtils.escapeIdentifier(fieldName)),
                    null, createToken(SEMICOLON_TOKEN)));
        }
        return fields;
    }

    private String getTypeFromJsonSchema(JsonNode propSchema) {
        if (propSchema == null) {
            return "string";
        }
        JsonNode typeNode = propSchema.get("type");
        return typeNode != null ? typeNode.asText() : "string";
    }

    private TypeDefinitionNode buildClosedRecordTypeDefinition(String typeName, List<Node> fields,
                                                               List<Node> docNodes) {
        MarkdownDocumentationNode docNode = createMarkdownDocumentationNode(createNodeList(docNodes));
        MetadataNode metadata = createMetadataNode(docNode, createEmptyNodeList());
        RecordTypeDescriptorNode recordType = createRecordTypeDescriptorNode(createToken(RECORD_KEYWORD),
                createToken(OPEN_BRACE_PIPE_TOKEN), createNodeList(fields), null,
                createToken(CLOSE_BRACE_PIPE_TOKEN));
        return createTypeDefinitionNode(metadata, createToken(PUBLIC_KEYWORD), createToken(TYPE_KEYWORD),
                createIdentifierToken(typeName), recordType, createToken(SEMICOLON_TOKEN));
    }

    private Node getServiceURLNode(String serviceUrl) {
        NodeList<AnnotationNode> annotations = createEmptyNodeList();
        BuiltinSimpleNameReferenceNode serviceUrlType = createBuiltinSimpleNameReferenceNode(null,
                createToken(STRING_KEYWORD));
        IdentifierToken serviceUrlVarName = createIdentifierToken(SERVICE_URL);
        if ("/".equals(serviceUrl)) {
            return createRequiredParameterNode(annotations, serviceUrlType, serviceUrlVarName);
        }
        BasicLiteralNode defaultExpr = createBasicLiteralNode(STRING_LITERAL,
                createIdentifierToken('"' + serviceUrl + '"'));
        return createDefaultableParameterNode(annotations, serviceUrlType, serviceUrlVarName,
                createToken(EQUAL_TOKEN), defaultExpr);
    }

    private MetadataNode getMetadataNode(String comment) {
        List<Node> docs = new ArrayList<>(DocCommentsGenerator.createAPIDescriptionDoc(comment, false));
        MarkdownDocumentationNode docNode = createMarkdownDocumentationNode(createNodeList(docs));
        return createMetadataNode(docNode, createEmptyNodeList());
    }

    private String getAuthFieldTypeName() {
        Set<String> typeNames = new HashSet<>();
        for (String authType : authTypes) {
            switch (authType) {
                case BEARER_SCHEME:
                    typeNames.add(AuthConfigTypes.BEARER.getValue());
                    break;
                case BASIC_SCHEME:
                    typeNames.add(AuthConfigTypes.BASIC.getValue());
                    break;
                case CLIENT_CRED_GRANT:
                    if (clientCredGrantTokenUrl != null && !clientCredGrantTokenUrl.isBlank()) {
                        typeNames.add(AuthConfigTypes.CUSTOM_CLIENT_CREDENTIAL.getValue());
                    } else {
                        typeNames.add(AuthConfigTypes.CLIENT_CREDENTIAL.getValue());
                    }
                    break;
                case PASSWORD_GRANT:
                    if (passwordGrantTokenUrl != null && !passwordGrantTokenUrl.isBlank()) {
                        typeNames.add(AuthConfigTypes.CUSTOM_PASSWORD.getValue());
                    } else {
                        typeNames.add(AuthConfigTypes.PASSWORD.getValue());
                    }
                    break;
                case REFRESH_TOKEN_GRANT:
                    if (refreshTokenUrl != null && !refreshTokenUrl.isBlank()) {
                        typeNames.add(AuthConfigTypes.CUSTOM_REFRESH_TOKEN.getValue());
                    } else {
                        typeNames.add(AuthConfigTypes.REFRESH_TOKEN.getValue());
                    }
                    break;
                default:
                    break;
            }
        }
        StringBuilder sb = new StringBuilder();
        for (String name : typeNames) {
            if (sb.length() > 0) {
                sb.append("|");
            }
            sb.append(name);
        }
        return sb.toString();
    }

    private TypeDefinitionNode getOAuth2ClientCredsGrantConfigRecord() {
        Token typeName = AbstractNodeFactory.createIdentifierToken(
                AuthConfigTypes.CUSTOM_CLIENT_CREDENTIAL.getValue());
        List<Node> fields = new ArrayList<>();
        MetadataNode fieldMeta = getMetadataNode("Token URL");
        TypeDescriptorNode stringType = createSimpleNameReferenceNode(createToken(STRING_KEYWORD));
        io.ballerina.compiler.syntax.tree.RecordFieldWithDefaultValueNode tokenUrlField =
                io.ballerina.compiler.syntax.tree.NodeFactory.createRecordFieldWithDefaultValueNode(
                        fieldMeta, null, stringType, createIdentifierToken("tokenUrl"),
                        createToken(EQUAL_TOKEN),
                        createRequiredExpressionNode(createIdentifierToken(
                                String.format("\"%s\"", clientCredGrantTokenUrl))),
                        createToken(SEMICOLON_TOKEN));
        fields.add(io.ballerina.compiler.syntax.tree.NodeFactory.createIncludedRecordParameterNode(
                createEmptyNodeList(),
                createToken(io.ballerina.compiler.syntax.tree.SyntaxKind.ASTERISK_TOKEN),
                createSimpleNameReferenceNode(
                        createIdentifierToken("websocket:OAuth2ClientCredentialsGrantConfig")),
                createToken(SEMICOLON_TOKEN)));
        fields.add(tokenUrlField);
        RecordTypeDescriptorNode recordType = createRecordTypeDescriptorNode(createToken(RECORD_KEYWORD),
                createToken(OPEN_BRACE_PIPE_TOKEN), createNodeList(fields), null,
                createToken(CLOSE_BRACE_PIPE_TOKEN));
        MetadataNode metadata = getMetadataNode("OAuth2 Client Credentials Grant Configs");
        return createTypeDefinitionNode(metadata, createToken(PUBLIC_KEYWORD), createToken(TYPE_KEYWORD),
                typeName, recordType, createToken(SEMICOLON_TOKEN));
    }

    private TypeDefinitionNode getOAuth2PasswordGrantConfigRecord() {
        Token typeName = AbstractNodeFactory.createIdentifierToken(
                AuthConfigTypes.CUSTOM_PASSWORD.getValue());
        List<Node> fields = new ArrayList<>();
        MetadataNode fieldMeta = getMetadataNode("Token URL");
        TypeDescriptorNode stringType = createSimpleNameReferenceNode(createToken(STRING_KEYWORD));
        io.ballerina.compiler.syntax.tree.RecordFieldWithDefaultValueNode tokenUrlField =
                io.ballerina.compiler.syntax.tree.NodeFactory.createRecordFieldWithDefaultValueNode(
                        fieldMeta, null, stringType, createIdentifierToken("tokenUrl"),
                        createToken(EQUAL_TOKEN),
                        createRequiredExpressionNode(createIdentifierToken(
                                String.format("\"%s\"", passwordGrantTokenUrl))),
                        createToken(SEMICOLON_TOKEN));
        fields.add(io.ballerina.compiler.syntax.tree.NodeFactory.createIncludedRecordParameterNode(
                createEmptyNodeList(),
                createToken(io.ballerina.compiler.syntax.tree.SyntaxKind.ASTERISK_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken("websocket:OAuth2PasswordGrantConfig")),
                createToken(SEMICOLON_TOKEN)));
        fields.add(tokenUrlField);
        RecordTypeDescriptorNode recordType = createRecordTypeDescriptorNode(createToken(RECORD_KEYWORD),
                createToken(OPEN_BRACE_PIPE_TOKEN), createNodeList(fields), null,
                createToken(CLOSE_BRACE_PIPE_TOKEN));
        MetadataNode metadata = getMetadataNode("OAuth2 Password Grant Configs");
        return createTypeDefinitionNode(metadata, createToken(PUBLIC_KEYWORD), createToken(TYPE_KEYWORD),
                typeName, recordType, createToken(SEMICOLON_TOKEN));
    }

    private TypeDefinitionNode getOAuth2RefreshTokenGrantConfigRecord() {
        Token typeName = AbstractNodeFactory.createIdentifierToken(
                AuthConfigTypes.CUSTOM_REFRESH_TOKEN.getValue());
        List<Node> fields = new ArrayList<>();
        MetadataNode fieldMeta = getMetadataNode("Refresh URL");
        TypeDescriptorNode stringType = createSimpleNameReferenceNode(createToken(STRING_KEYWORD));
        io.ballerina.compiler.syntax.tree.RecordFieldWithDefaultValueNode refreshUrlField =
                io.ballerina.compiler.syntax.tree.NodeFactory.createRecordFieldWithDefaultValueNode(
                        fieldMeta, null, stringType, createIdentifierToken("refreshUrl"),
                        createToken(EQUAL_TOKEN),
                        createRequiredExpressionNode(createIdentifierToken(
                                String.format("\"%s\"", refreshTokenUrl))),
                        createToken(SEMICOLON_TOKEN));
        fields.add(io.ballerina.compiler.syntax.tree.NodeFactory.createIncludedRecordParameterNode(
                createEmptyNodeList(),
                createToken(io.ballerina.compiler.syntax.tree.SyntaxKind.ASTERISK_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken("websocket:OAuth2RefreshTokenGrantConfig")),
                createToken(SEMICOLON_TOKEN)));
        fields.add(refreshUrlField);
        RecordTypeDescriptorNode recordType = createRecordTypeDescriptorNode(createToken(RECORD_KEYWORD),
                createToken(OPEN_BRACE_PIPE_TOKEN), createNodeList(fields), null,
                createToken(CLOSE_BRACE_PIPE_TOKEN));
        MetadataNode metadata = getMetadataNode("OAuth2 Refresh Token Grant Configs");
        return createTypeDefinitionNode(metadata, createToken(PUBLIC_KEYWORD), createToken(TYPE_KEYWORD),
                typeName, recordType, createToken(SEMICOLON_TOKEN));
    }

    /**
     * Enum to select the relevant Ballerina WebSocket auth record type.
     */
    public enum AuthConfigTypes {
        /** HTTP basic auth (credentials). */
        BASIC("websocket:CredentialsConfig"),
        /** Bearer token auth. */
        BEARER("websocket:BearerTokenConfig"),
        /** OAuth2 client credentials grant. */
        CLIENT_CREDENTIAL("websocket:OAuth2ClientCredentialsGrantConfig"),
        /** Custom OAuth2 client credentials grant. */
        CUSTOM_CLIENT_CREDENTIAL("OAuth2ClientCredentialsGrantConfig"),
        /** OAuth2 refresh token grant. */
        REFRESH_TOKEN("websocket:OAuth2RefreshTokenGrantConfig"),
        /** Custom OAuth2 refresh token grant. */
        CUSTOM_REFRESH_TOKEN("OAuth2RefreshTokenGrantConfig"),
        /** OAuth2 password grant. */
        PASSWORD("websocket:OAuth2PasswordGrantConfig"),
        /** Custom OAuth2 password grant. */
        CUSTOM_PASSWORD("OAuth2PasswordGrantConfig");

        private final String authType;

        AuthConfigTypes(String authType) {
            this.authType = authType;
        }

        /**
         * Returns the Ballerina type string for this auth config type.
         *
         * @return the Ballerina type name
         */
        public String getValue() {
            return authType;
        }
    }
}
