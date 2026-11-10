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
import io.ballerina.asyncapi.core.model.channel.AsyncApiChannel;
import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.ws.client.generator.BallerinaAuthConfigGenerator;
import io.ballerina.asyncapi.generator.ws.client.model.WsClientConfig;
import io.ballerina.asyncapi.generator.ws.client.utils.CodegenUtils;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.Token;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.COMMA_TOKEN;

/**
 * Builds the {@code init} function definition node for a WebSocket client class.
 */
public class ClientInitGenerator {

    private static final String X_BALLERINA_INIT_DESCRIPTION = "x-ballerina-init-description";
    private static final String PIPES = "pipes";
    private static final String STREAM_GENERATORS = "streamGenerators";
    private static final String WRITE_MESSAGE_QUEUE = "writeMessageQueue";
    private static final String QUEUE_DEFAULT_SIZE = "1000";
    private static final String CLIENT_EP = "clientEp";
    private static final String IS_ACTIVE = "isActive";
    private static final String START_MESSAGE_WRITING = "startMessageWriting";
    private static final String START_MESSAGE_READING = "startMessageReading";
    private static final String SERVICE_URL = "serviceUrl";
    private static final String MODIFIED_URL = "modifiedUrl";
    private static final String PATH_PARAMS = "pathParams";
    private static final String QUERY_PARAMS = "queryParams";
    private static final String HEADER_PARAMS = "headerParams";
    private static final String API_KEY_CONFIG = "apiKeyConfig";

    private final WsClientConfig config;
    private final AsyncApiChannel channel;
    private final JsonNode querySchema;
    private final JsonNode headerSchema;
    private final String serverUrl;
    private final BallerinaAuthConfigGenerator authGen;
    private final boolean isStreamPresent;

    /**
     * Creates a new init function generator.
     *
     * @param config          the WebSocket client generation configuration
     * @param channel         the channel definition
     * @param querySchema     WS binding query schema
     * @param headerSchema    WS binding header schema
     * @param serverUrl       computed server URL
     * @param authGen         auth config generator
     * @param isStreamPresent whether streaming return types exist
     */
    public ClientInitGenerator(WsClientConfig config, AsyncApiChannel channel,
                                JsonNode querySchema, JsonNode headerSchema,
                                String serverUrl, BallerinaAuthConfigGenerator authGen,
                                boolean isStreamPresent) {
        this.config = config;
        this.channel = channel;
        this.querySchema = querySchema;
        this.headerSchema = headerSchema;
        this.serverUrl = serverUrl;
        this.authGen = authGen;
        this.isStreamPresent = isStreamPresent;
    }

    /**
     * Builds the {@code init} function definition node.
     *
     * @return the init function definition node
     * @throws GeneratorException if parameter generation fails
     */
    public FunctionDefinitionNode buildInitFunction() throws GeneratorException {
        List<Node> extraParamNodes = new ArrayList<>();
        Token comma = createToken(COMMA_TOKEN);
        authGen.setFunctionParameters(channel, extraParamNodes, comma, querySchema, headerSchema);

        List<String> paramParts = new ArrayList<>();
        for (Node node : extraParamNodes) {
            // The parameter nodes carry no whitespace, so write "Type name" ourselves
            if (node instanceof RequiredParameterNode param && param.paramName().isPresent()) {
                paramParts.add(param.typeName().toSourceCode().trim() + " " + param.paramName().get().text());
            } else if (node.kind() != COMMA_TOKEN) {
                paramParts.add(node.toSourceCode().trim());
            }
        }
        String path = channelPath();
        // The channel path is added to serviceUrl, so the default must not end with a slash
        String defaultUrl = !path.isEmpty() && serverUrl.length() > 1 && serverUrl.endsWith("/")
                ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
        paramParts.add(authGen.buildInitParamString(defaultUrl));
        String allParams = String.join(", ", paramParts);

        List<StatementNode> bodyStatements = buildBodyStatements();
        String bodySource = bodyStatements.stream()
                .map(StatementNode::toSourceCode)
                .collect(Collectors.joining("\n"));

        String description = getInitDescription();
        String funcSource = String.format(
                "\n# %s\n#\n"
                + "# + config - The configurations to be used when initializing the `connector`\n"
                + "# + serviceUrl - URL of the target service\n"
                + "# + return - An error if connector initialization failed\n"
                + "public isolated function init(%s) returns error? {\n%s\n}",
                description, allParams, bodySource);
        return (FunctionDefinitionNode) NodeParser.parseObjectMember(funcSource);
    }

    /**
     * Returns the init function description from the spec extensions or the default.
     *
     * @return init description text
     */
    private String getInitDescription() {
        String description = "Gets invoked to initialize the `connector`.";
        Map<String, JsonNode> extensions = config.getAsyncApi().getAsyncApiExtensions().orElse(null);
        if (extensions != null && extensions.containsKey(X_BALLERINA_INIT_DESCRIPTION)) {
            description = extensions.get(X_BALLERINA_INIT_DESCRIPTION).asText();
        }
        return description;
    }

    /**
     * Builds the init function body statements.
     *
     * @return list of statement nodes
     */
    private List<StatementNode> buildBodyStatements() {
        List<StatementNode> statements = new ArrayList<>();

        statements.add(NodeParser.parseStatement(String.format("self.%s = new ();", PIPES)));
        if (isStreamPresent) {
            statements.add(NodeParser.parseStatement(String.format("self.%s = new ();", STREAM_GENERATORS)));
        }
        statements.add(NodeParser.parseStatement(
                String.format("self.%s = new (%s);", WRITE_MESSAGE_QUEUE, QUEUE_DEFAULT_SIZE)));

        String url = SERVICE_URL;
        String path = channelPath();
        if (!path.isEmpty()) {
            statements.add(NodeParser.parseStatement(
                    String.format("string %s = %s + string `%s`;", MODIFIED_URL, SERVICE_URL, path)));
            url = MODIFIED_URL;
        }
        List<String> queryEntries = new ArrayList<>(paramEntries(querySchema, QUERY_PARAMS, false));
        // With HTTP or OAuth as well, the API key is optional; a nil query value is skipped
        boolean optionalKey = authGen.isApiKeyOptional();
        authGen.getQueryApiKeyNameList().values().forEach(name -> queryEntries.add(
                apiKeyEntry(name, optionalKey ? "?." : ".")));
        if (!queryEntries.isEmpty()) {
            if (path.isEmpty()) {
                statements.add(NodeParser.parseStatement(
                        String.format("string %s = %s;", MODIFIED_URL, SERVICE_URL)));
                url = MODIFIED_URL;
            }
            statements.add(NodeParser.parseStatement(
                    String.format("map<anydata> queryParam = {%s};", String.join(", ", queryEntries))));
            statements.add(NodeParser.parseStatement(
                    String.format("%s = %s + check getPathForQueryParam(queryParam);", MODIFIED_URL, MODIFIED_URL)));
        }
        List<String> headerEntries = new ArrayList<>(paramEntries(headerSchema, HEADER_PARAMS, true));
        Collection<String> headerKeys = authGen.getHeaderApiKeyNameList().values();
        if (!optionalKey) {
            headerKeys.forEach(name -> headerEntries.add(apiKeyEntry(name, ".")));
        }
        if (!headerEntries.isEmpty() || !headerKeys.isEmpty()) {
            statements.add(NodeParser.parseStatement(
                    String.format("map<string> headerParam = {%s};", String.join(", ", headerEntries))));
            if (optionalKey) {
                // A header map holds only strings, so an optional key is added only when given
                String puts = headerKeys.stream().map(name -> String.format("headerParam[%s] = %s.%s;",
                        CodegenUtils.toStringLiteral(name), API_KEY_CONFIG, CodegenUtils.getValidName(name, false)))
                        .collect(Collectors.joining(" "));
                statements.add(NodeParser.parseStatement(
                        String.format("if %s is ApiKeysConfig { %s }", API_KEY_CONFIG, puts)));
            }
            statements.add(NodeParser.parseStatement(
                    "clientConfig.customHeaders = getCombineHeaders(clientConfig.customHeaders, headerParam);"));
        }

        statements.add(NodeParser.parseStatement(
                String.format("websocket:Client websocketEp = check new (%s, clientConfig);", url)));
        statements.add(buildSelfAssignment(CLIENT_EP, "websocketEp"));
        statements.add(buildSelfAssignment(IS_ACTIVE, "true"));

        if (authGen.isHttpApiKey()) {
            statements.add(NodeParser.parseStatement("self.apiKeyConfig = apiKeyConfig.cloneReadOnly();"));
        }

        statements.add(NodeParser.parseStatement(String.format("self.%s();", START_MESSAGE_WRITING)));
        statements.add(NodeParser.parseStatement(String.format("self.%s();", START_MESSAGE_READING)));
        statements.add(NodeParser.parseStatement("return;"));

        return statements;
    }

    /**
     * Returns the channel address as the body of a string template, with each path parameter filled in
     * from {@code pathParams}, or {@code ""} for the root channel.
     *
     * @return the path to add to {@code serviceUrl}
     */
    private String channelPath() {
        String address = channel.address();
        if (address == null || address.isBlank() || "/".equals(address)) {
            return "";
        }
        String path = address.startsWith("/") ? address : "/" + address;
        if (channel.parameters() != null) {
            for (String name : channel.parameters().keySet()) {
                path = path.replace("{" + name + "}", String.format("${getEncodedUri(%s.%s)}",
                        PATH_PARAMS, CodegenUtils.escapeIdentifier(name)));
            }
        }
        return path;
    }

    /**
     * Returns the {@code "name": record.field} map entries for the properties of a WS binding schema.
     *
     * @param schema   the {@code query} or {@code headers} schema (may be null)
     * @param record   the parameter holding the values
     * @param asString whether each value is converted to a string, as headers need
     * @return the map entries, empty when the schema has no properties
     */
    private static List<String> paramEntries(JsonNode schema, String record, boolean asString) {
        List<String> entries = new ArrayList<>();
        if (schema == null || schema.get("properties") == null) {
            return entries;
        }
        Iterator<String> names = schema.get("properties").fieldNames();
        while (names.hasNext()) {
            String name = names.next();
            entries.add(String.format("%s: %s.%s%s", CodegenUtils.toStringLiteral(name), record,
                    CodegenUtils.escapeIdentifier(name), asString ? ".toString()" : ""));
        }
        return entries;
    }

    private static String apiKeyEntry(String name, String access) {
        return String.format("%s: %s%s%s", CodegenUtils.toStringLiteral(name), API_KEY_CONFIG, access,
                CodegenUtils.getValidName(name, false));
    }

    /**
     * Builds a {@code self.fieldName = value;} assignment statement.
     *
     * @param fieldName the field on {@code self}
     * @param value     the rhs expression text
     * @return the assignment statement node
     */
    private StatementNode buildSelfAssignment(String fieldName, String value) {
        return NodeParser.parseStatement(String.format("self.%s = %s;", fieldName, value));
    }
}
