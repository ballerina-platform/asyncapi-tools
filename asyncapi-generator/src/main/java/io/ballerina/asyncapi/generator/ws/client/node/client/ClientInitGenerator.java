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
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.Token;

import java.util.ArrayList;
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
            if (node.kind() != COMMA_TOKEN) {
                paramParts.add(node.toSourceCode().trim());
            }
        }
        paramParts.add(authGen.buildInitParamString(serverUrl));
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

        if (authGen.isQueryParam() && querySchema != null && querySchema.get("properties") != null) {
            statements.add(NodeParser.parseStatement("string queryString = \"\";"));
            statements.add(NodeParser.parseStatement("serviceUrl = serviceUrl + queryString;"));
        }

        statements.add(NodeParser.parseStatement(
                "websocket:Client websocketEp = check new (serviceUrl, clientConfig);"));
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
