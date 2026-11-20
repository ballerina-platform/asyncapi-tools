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

import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.core.model.channel.AsyncApiChannel;
import io.ballerina.asyncapi.core.model.server.AsyncApiServer;
import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.ws.client.extractor.ChannelExtractor;
import io.ballerina.asyncapi.generator.ws.client.extractor.DispatcherKeyExtractor;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
    private static final String NO_SERVER_URL = "/";

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
        // A spec without a key it needs fails first, before its channel or auth is checked
        new DispatcherKeyExtractor(asyncApiSpec).extractIfNeeded();

        // Each channel is a connection of its own, so each gets its own client
        List<ChannelExtractor.ChannelInfo> channels = new ChannelExtractor(asyncApiSpec).extract(config.getWarnings());
        boolean severalChannels = channels.size() > 1;
        String serverUrl = getServerUrl(asyncApiSpec);
        List<ImportDeclarationNode> imports = buildImports();

        Map<String, TypeDefinitionNode> typeDefinitions = new LinkedHashMap<>();
        List<ClassDefinitionNode> classDefNodes = new ArrayList<>();
        Set<String> classNames = new HashSet<>();
        for (ChannelExtractor.ChannelInfo channelInfo : channels) {
            // With several channels, each client's parameter records are named after its channel
            String paramsPrefix = severalChannels ? CodegenUtils.getValidName(channelInfo.channelName(), true) : "";
            BallerinaAuthConfigGenerator authGen =
                    new BallerinaAuthConfigGenerator().withParamsTypePrefix(paramsPrefix);
            authGen.addAuthRelatedRecords(asyncApiSpec);
            List<TypeDefinitionNode> typeDefinitionNodeList =
                    new ArrayList<>(authGen.getAuthRelatedTypeDefinitionNodes());

            // How each incoming message is told apart and which pipe it goes to
            RoutingPlan plan = RoutingPlan.of(channelInfo.spec());
            classDefNodes.add(getClassDefinitionNode(plan, channelInfo, serverUrl, authGen, typeDefinitionNodeList,
                    imports, classNames));

            // init adds the PathParams, HeaderParams and QueryParams records while the class is built
            for (TypeDefinitionNode paramRecord : authGen.getAuthRelatedTypeDefinitionNodes()) {
                if (!typeDefinitionNodeList.contains(paramRecord)) {
                    typeDefinitionNodeList.add(paramRecord);
                }
            }
            // The clients share the spec-wide records, such as the auth config
            for (TypeDefinitionNode typeDef : typeDefinitionNodeList) {
                typeDefinitions.putIfAbsent(typeDef.typeName().text(), typeDef);
            }
        }

        // Build module part
        List<ModuleMemberDeclarationNode> memberDeclarations = new ArrayList<>(typeDefinitions.values());
        memberDeclarations.addAll(classDefNodes);

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
            // No server to default to, so the caller must give serviceUrl
            return NO_SERVER_URL;
        }
        AsyncApiServer server = servers.values().iterator().next();
        String url = CodegenUtils.buildUrl(server.host(), server.pathname(), server.variables());
        // A 3.x host has no scheme; the server's protocol (ws or wss) gives it
        String protocol = server.protocol() != null ? server.protocol().toLowerCase(Locale.ROOT) : "";
        if (!url.contains("://") && ("ws".equals(protocol) || "wss".equals(protocol))) {
            url = protocol + "://" + url;
        }
        // Still no scheme (say protocol: https), so a default would fail to connect; make serviceUrl required
        return url.contains("://") ? url : NO_SERVER_URL;
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
     * Builds the full class definition node for the WebSocket client.
     *
     * @param plan                   how incoming messages are routed
     * @param channelInfo            the client's channel, its bindings and the spec its client is generated from
     * @param serverUrl              computed server URL
     * @param authGen                auth config generator
     * @param typeDefinitionNodeList accumulated type definitions
     * @param imports                accumulated imports
     * @param classNames             the client names already taken
     * @return the class definition node
     * @throws GeneratorException if generation fails
     */
    private ClassDefinitionNode getClassDefinitionNode(RoutingPlan plan, ChannelExtractor.ChannelInfo channelInfo,
                                                        String serverUrl, BallerinaAuthConfigGenerator authGen,
                                                        List<TypeDefinitionNode> typeDefinitionNodeList,
                                                        List<ImportDeclarationNode> imports, Set<String> classNames)
            throws GeneratorException {
        boolean isStreamPresent = !plan.streamReturns().isEmpty();
        AsyncApiSpec asyncApiSpec = channelInfo.spec();
        AsyncApiChannel channel = channelInfo.channel();

        // Build class members using focused node builders
        List<Node> classMembers = new ArrayList<>();
        ClientHelperGenerator helperGen = new ClientHelperGenerator(isStreamPresent);

        classMembers.addAll(new ClientFieldGenerator(isStreamPresent, authGen, plan.responseMap(),
                plan.dispatchValueMap()).buildFields());
        classMembers.add(new ClientInitGenerator(config, channel, channelInfo.querySchema(), channelInfo.headerSchema(),
                serverUrl, authGen, isStreamPresent).buildInitFunction());
        classMembers.add(helperGen.buildGetRecordName());
        classMembers.add(helperGen.buildGetPipeName());

        ClientWorkerGenerator workerGen = new ClientWorkerGenerator(asyncApiSpec, plan);
        classMembers.add(workerGen.buildStartMessageWriting());
        classMembers.add(workerGen.buildStartMessageReading());

        classMembers.addAll(new ClientRemoteFunctionGenerator(asyncApiSpec, plan, imports, typeDefinitionNodeList)
                .withWarnings(config.getWarnings()).buildRemoteFunctions());

        classMembers.add(helperGen.buildAttemptToCloseConnection());
        classMembers.add(helperGen.buildConnectionClose());

        // Class metadata
        MetadataNode classMetadata = getClassMetadataNode(asyncApiSpec);

        // Class qualifiers: client isolated (public is passed as visibilityQualifier separately)
        NodeList<Token> classQualifiers = createNodeList(
                CodegenUtils.tokenWithSpace(CLIENT_KEYWORD),
                CodegenUtils.tokenWithSpace(ISOLATED_KEYWORD));

        String className = clientClassName(asyncApiSpec, channelInfo.channelName(), channel);
        // Two channel names can give the same client name, as in 'chat-room' and 'chat_room'
        String uniqueName = className;
        for (int i = 2; !classNames.add(uniqueName); i++) {
            uniqueName = className + i;
        }
        className = uniqueName;

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
     * Returns the name of the client for a channel: {@code {Title}{ChannelName}Client}, or {@code {Title}Client}
     * for the root channel.
     *
     * @param spec        the spec
     * @param channelName the channel's key in the spec
     * @param channel     the channel
     * @return the client's class name
     */
    public static String clientClassName(AsyncApiSpec spec, String channelName, AsyncApiChannel channel) {
        String title = CodegenUtils.getClientTitleName(spec.getAsyncApiInfo());
        String chanName = "/".equals(channel.address()) ? "" : CodegenUtils.getValidName(channelName, true);
        return String.format("%s%sClient", title, chanName);
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
