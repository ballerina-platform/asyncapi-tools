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
import io.ballerina.asyncapi.core.model.component.AsyncApiSchema;
import io.ballerina.asyncapi.core.model.message.AsyncApiMessage;
import io.ballerina.asyncapi.core.model.operation.AsyncApiOperation;
import io.ballerina.asyncapi.core.model.server.AsyncApiServer;
import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.ws.client.extractor.ChannelExtractor;
import io.ballerina.asyncapi.generator.ws.client.extractor.DispatcherKeyExtractor;
import io.ballerina.asyncapi.generator.ws.client.extractor.DispatcherStreamIdExtractor;
import io.ballerina.asyncapi.generator.ws.client.model.WsClientConfig;
import io.ballerina.asyncapi.generator.ws.client.node.client.ClientFieldGenerator;
import io.ballerina.asyncapi.generator.ws.client.node.client.ClientHelperGenerator;
import io.ballerina.asyncapi.generator.ws.client.node.client.ClientInitGenerator;
import io.ballerina.asyncapi.generator.ws.client.node.client.ClientRemoteFunctionGenerator;
import io.ballerina.asyncapi.generator.ws.client.node.client.ClientWorkerGenerator;
import io.ballerina.asyncapi.generator.ws.client.utils.CodegenUtils;
import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.MarkdownDocumentationNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocuments;
import org.ballerinalang.formatter.core.Formatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createClassDefinitionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMarkdownDocumentationNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMetadataNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createModulePartNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLASS_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLIENT_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.EOF_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ISOLATED_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PUBLIC_KEYWORD;

/**
 * Generates {@code client.bal} content for a WebSocket client.
 */
public class ClientGenerator {

    /** Simple request/response pattern. */
    public static final String SIMPLE_RPC = "simple-rpc";

    /** Server-streaming response pattern. */
    public static final String SERVER_STREAMING = "server-streaming";

    /** Extension key for the response message reference. */
    public static final String X_RESPONSE = "x-response";

    /** Extension key for the response type (simple-rpc, server-streaming, etc.). */
    public static final String X_RESPONSE_TYPE = "x-response-type";

    /** writeMessageQueue pipe field name. */
    public static final String WRITE_MESSAGE_QUEUE = "writeMessageQueue";

    /** pipes field name. */
    public static final String PIPES = "pipes";

    /** isActive field name. */
    public static final String IS_ACTIVE = "isActive";

    /** attemptToCloseConnection function name. */
    public static final String ATTEMPT_TO_CLOSE_CONNECTION = "attemptToCloseConnection";

    /** Pipe module name (xlibb). */
    public static final String SIMPLE_PIPE = "pipe";

    private static final String BALLERINA = "ballerina";
    private static final String XLIBB = "xlibb";
    private static final String WEBSOCKET = "websocket";
    private static final String LOG_MODULE = "log";
    private static final String LANG_REGEXP_MODULE = "lang.regexp";
    private static final String DEFAULT_URL = "ws://localhost:9090/v1";
    private static final String MESSAGE_TYPE = "Message";

    private final WsClientConfig config;

    /**
     * Creates a new client generator.
     *
     * @param config the WebSocket client generation configuration
     */
    public ClientGenerator(WsClientConfig config) {
        this.config = config;
    }

    /**
     * Generates the Ballerina source content for {@code client.bal}.
     *
     * @return generated source as a string
     * @throws GeneratorException if the spec is invalid or generation fails
     */
    public String generate() throws GeneratorException {
        AsyncApiSpec asyncApiSpec = config.getAsyncApi();

        // Extract dispatcherKey
        String dispatcherKey = new DispatcherKeyExtractor(asyncApiSpec).extract();

        // Extract optional dispatcherStreamId
        String dispatcherStreamId = new DispatcherStreamIdExtractor(asyncApiSpec).extract().orElse(null);

        // Get channel and WS bindings — only a single channel is supported
        ChannelExtractor.ChannelInfo channelInfo = new ChannelExtractor(asyncApiSpec).extract();
        String channelName = channelInfo.channelName();
        AsyncApiChannel channel = channelInfo.channel();
        JsonNode querySchema = channelInfo.querySchema();
        JsonNode headerSchema = channelInfo.headerSchema();

        // Compute server URL
        String serverUrl = getServerUrl(asyncApiSpec);

        // Set up auth generator and process auth records
        BallerinaAuthConfigGenerator authGen = new BallerinaAuthConfigGenerator();
        authGen.addAuthRelatedRecords(asyncApiSpec);

        // Collect type definitions
        List<TypeDefinitionNode> typeDefinitionNodeList = new ArrayList<>(authGen.getAuthRelatedTypeDefinitionNodes());

        // Compute streamReturns and responseMap
        List<String> streamReturns = new ArrayList<>();
        Map<String, String> responseMap = buildResponseMap(asyncApiSpec, streamReturns);

        // Build imports
        List<ImportDeclarationNode> imports = buildImports();

        // Build class definition
        ClassDefinitionNode classDefNode = getClassDefinitionNode(dispatcherKey, dispatcherStreamId, channelName,
                channel, querySchema, headerSchema, serverUrl, authGen, streamReturns, responseMap,
                typeDefinitionNodeList, imports);

        // Build module part
        List<ModuleMemberDeclarationNode> memberDeclarations = new ArrayList<>();
        for (TypeDefinitionNode typeDef : typeDefinitionNodeList) {
            memberDeclarations.add(typeDef);
        }
        memberDeclarations.add(classDefNode);

        NodeList<ImportDeclarationNode> importsList = createNodeList(imports);
        NodeList<ModuleMemberDeclarationNode> members = createNodeList(memberDeclarations);
        Token eofToken = createToken(EOF_TOKEN);
        ModulePartNode modulePartNode = createModulePartNode(importsList, members, eofToken);

        TextDocument textDocument = TextDocuments.from("");
        SyntaxTree syntaxTree = SyntaxTree.from(textDocument);
        SyntaxTree result = syntaxTree.modifyWith(modulePartNode);
        try {
            String source = Formatter.format(result).toSourceCode();
            String license = config.getLicenseHeader();
            if (license == null || license.isBlank()) {
                return source;
            }
            return license.stripTrailing() + "\n\n" + source;
        } catch (Exception e) {
            throw new GeneratorException("Failed to format generated source", e);
        }
    }

    /**
     * Computes the server URL from the spec's server definitions.
     *
     * @param asyncApiSpec the parsed spec
     * @return server URL string
     */
    private String getServerUrl(AsyncApiSpec asyncApiSpec) {
        Map<String, AsyncApiServer> servers = asyncApiSpec.getAsyncApiServers().orElse(null);
        if (servers == null || servers.isEmpty()) {
            return DEFAULT_URL;
        }
        AsyncApiServer server = servers.values().iterator().next();
        return CodegenUtils.buildUrl(server.host(), server.pathname(), server.variables());
    }

    /**
     * Builds the list of required Ballerina imports.
     *
     * @return list of import declaration nodes
     */
    private List<ImportDeclarationNode> buildImports() {
        List<ImportDeclarationNode> imports = new ArrayList<>();
        imports.add(CodegenUtils.getImportDeclarationNode(BALLERINA, WEBSOCKET));
        imports.add(CodegenUtils.getImportDeclarationNode(XLIBB, SIMPLE_PIPE));
        imports.add(CodegenUtils.getImportDeclarationNode(BALLERINA, LOG_MODULE));
        imports.add(CodegenUtils.getImportDeclarationNode(BALLERINA, LANG_REGEXP_MODULE));
        return imports;
    }

    /**
     * Builds a map from each inbound response-schema name to the request type that owns it,
     * used by the dispatcher service to route incoming frames to the correct pipe.
     *
     * @param asyncApiSpec  the parsed AsyncAPI spec
     * @param streamReturns collector for return types that require a stream wrapper
     * @return map from PascalCase response schema name to Ballerina request type name
     * @throws GeneratorException if return-type resolution fails
     */
    private Map<String, String> buildResponseMap(AsyncApiSpec asyncApiSpec, List<String> streamReturns)
            throws GeneratorException {
        Map<String, String> responseMap = new HashMap<>();
        Map<String, AsyncApiOperation> operations = asyncApiSpec.getAsyncApiOperations().orElse(null);
        if (operations == null) {
            return responseMap;
        }
        RemoteFunctionReturnTypeGenerator returnTypeGen = new RemoteFunctionReturnTypeGenerator(asyncApiSpec);

        Set<String> claimedResponses = new HashSet<>();
        for (Map.Entry<String, AsyncApiOperation> opEntry : operations.entrySet()) {
            AsyncApiOperation operation = opEntry.getValue();
            if (operation.action() != AsyncApiOperation.Action.SEND || operation.messages() == null) {
                continue;
            }

            for (Map.Entry<String, AsyncApiMessage> msgEntry : operation.messages().entrySet()) {
                AsyncApiMessage message = msgEntry.getValue();
                if (message.extensions() == null || !message.extensions().containsKey(X_RESPONSE)) {
                    continue;
                }
                JsonNode xResponseType = message.extensions().get(X_RESPONSE_TYPE);
                if (xResponseType != null && SERVER_STREAMING.equals(xResponseType.asText())) {
                    continue; 
                }
                JsonNode xResponse = message.extensions().get(X_RESPONSE);
                List<String> responseMessages = new ArrayList<>();
                String returnType = returnTypeGen.getReturnType(xResponse, xResponseType, responseMessages);
                if (!"null".equals(returnType)) {
                    String requestType = getRequestTypeFromMessage(message);
                    for (String responseMsg : responseMessages) {
                        String key = CodegenUtils.getValidName(responseMsg, true);
                        responseMap.put(key, requestType);
                        claimedResponses.add(key);
                    }
                }
            }

            for (Map.Entry<String, AsyncApiMessage> msgEntry : operation.messages().entrySet()) {
                AsyncApiMessage message = msgEntry.getValue();
                if (message.extensions() == null || !message.extensions().containsKey(X_RESPONSE)) {
                    continue;
                }
                JsonNode xResponseType = message.extensions().get(X_RESPONSE_TYPE);
                if (xResponseType == null || !SERVER_STREAMING.equals(xResponseType.asText())) {
                    continue; 
                }
                JsonNode xResponse = message.extensions().get(X_RESPONSE);
                List<String> responseMessages = new ArrayList<>();
                String returnType = returnTypeGen.getReturnType(xResponse, xResponseType, responseMessages);
                if (!"null".equals(returnType)) {
                    if (!streamReturns.contains(returnType)) {
                        streamReturns.add(returnType);
                    }
                    String requestType = getRequestTypeFromMessage(message);
                    for (String responseMsg : responseMessages) {
                        String key = CodegenUtils.getValidName(responseMsg, true);
                        if (!claimedResponses.contains(key)) {
                            responseMap.put(key, requestType);
                        }
                    }
                }
            }
        }
        return responseMap;
    }

    /**
     * Extracts the request type name from a message's payload schema.
     *
     * @param message the message
     * @return the Ballerina type name for the request
     */
    private String getRequestTypeFromMessage(AsyncApiMessage message) {
        if (message.payload() instanceof AsyncApiSchema) {
            AsyncApiSchema payload = (AsyncApiSchema) message.payload();
            if (payload.name() != null) {
                return CodegenUtils.getValidName(payload.name(), false);
            }
        }
        return MESSAGE_TYPE;
    }

    /**
     * Builds the full class definition node for the WebSocket client.
     *
     * @param dispatcherKey          the dispatcher key field name
     * @param dispatcherStreamId     optional stream ID field name
     * @param channelName            the channel key name
     * @param channel                the channel definition
     * @param querySchema            WS binding query schema
     * @param headerSchema           WS binding header schema
     * @param serverUrl              computed server URL
     * @param authGen                auth config generator
     * @param streamReturns          list of streaming return types
     * @param responseMap            response type to pipe name map
     * @param typeDefinitionNodeList accumulated type definitions
     * @param imports                accumulated imports
     * @return the class definition node
     * @throws GeneratorException if generation fails
     */
    private ClassDefinitionNode getClassDefinitionNode(String dispatcherKey, String dispatcherStreamId,
                                                        String channelName, AsyncApiChannel channel,
                                                        JsonNode querySchema, JsonNode headerSchema,
                                                        String serverUrl, BallerinaAuthConfigGenerator authGen,
                                                        List<String> streamReturns, Map<String, String> responseMap,
                                                        List<TypeDefinitionNode> typeDefinitionNodeList,
                                                        List<ImportDeclarationNode> imports)
            throws GeneratorException {
        boolean isStreamPresent = !streamReturns.isEmpty();
        AsyncApiSpec asyncApiSpec = config.getAsyncApi();

        // Build class members using focused node builders
        List<Node> classMembers = new ArrayList<>();
        ClientHelperGenerator helperGen = new ClientHelperGenerator(isStreamPresent);

        classMembers.addAll(new ClientFieldGenerator(isStreamPresent, authGen, responseMap).buildFields());
        classMembers.add(new ClientInitGenerator(config, channel, querySchema, headerSchema,
                serverUrl, authGen, isStreamPresent).buildInitFunction());
        classMembers.add(helperGen.buildGetRecordName());
        classMembers.add(helperGen.buildGetPipeName());

        ClientWorkerGenerator workerGen =
                new ClientWorkerGenerator(asyncApiSpec, dispatcherKey, dispatcherStreamId);
        classMembers.add(workerGen.buildStartMessageWriting());
        classMembers.add(workerGen.buildStartMessageReading());

        classMembers.addAll(new ClientRemoteFunctionGenerator(asyncApiSpec, dispatcherStreamId,
                streamReturns, imports, typeDefinitionNodeList, responseMap).buildRemoteFunctions());

        classMembers.add(helperGen.buildAttemptToCloseConnection());
        classMembers.add(helperGen.buildConnectionClose());

        // Class metadata
        MetadataNode classMetadata = getClassMetadataNode(asyncApiSpec);

        // Class qualifiers: client isolated (public is passed as visibilityQualifier separately)
        NodeList<Token> classQualifiers = createNodeList(
                CodegenUtils.tokenWithSpace(CLIENT_KEYWORD),
                CodegenUtils.tokenWithSpace(ISOLATED_KEYWORD));

        // Class name: {Title}{ChannelName}Client
        String title = asyncApiSpec.getAsyncApiInfo() != null
                && asyncApiSpec.getAsyncApiInfo().title() != null
                ? CodegenUtils.getValidName(asyncApiSpec.getAsyncApiInfo().title(), true)
                : "AsyncApi";
        String chanName = "/".equals(channel.address()) ? "" : CodegenUtils.getValidName(channelName, true);
        String className = String.format("%s%sClient", title, chanName);

        // createClassDefinitionNode(metadata, visibilityQualifier, qualifiers, classKeyword,
        //   className, openBrace, members, closeBrace, semicolonOrNull)
        return createClassDefinitionNode(classMetadata, CodegenUtils.tokenWithSpace(PUBLIC_KEYWORD), classQualifiers,
                CodegenUtils.tokenWithSpace(CLASS_KEYWORD),
                createIdentifierToken(className),
                createToken(OPEN_BRACE_TOKEN),
                createNodeList(classMembers),
                createToken(CLOSE_BRACE_TOKEN),
                null);
    }

    /**
     * Creates the class-level metadata/doc comment node.
     *
     * @param asyncApiSpec the spec
     * @return the metadata node
     */
    private MetadataNode getClassMetadataNode(AsyncApiSpec asyncApiSpec) {
        String description = "";
        if (asyncApiSpec.getAsyncApiInfo() != null && asyncApiSpec.getAsyncApiInfo().description() != null) {
            description = asyncApiSpec.getAsyncApiInfo().description();
        } else if (asyncApiSpec.getAsyncApiInfo() != null && asyncApiSpec.getAsyncApiInfo().title() != null) {
            description = asyncApiSpec.getAsyncApiInfo().title();
        }
        if (description.isBlank()) {
            description = "WebSocket client generated from the AsyncAPI specification.";
        }
        List<Node> classDocNodes = new ArrayList<>(
                DocCommentsGenerator.createAPIDescriptionDoc(description, false));
        MarkdownDocumentationNode docNode = createMarkdownDocumentationNode(createNodeList(classDocNodes));
        return createMetadataNode(docNode, createEmptyNodeList());
    }
}
