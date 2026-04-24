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

import io.ballerina.asyncapi.core.model.channel.AsyncApiChannel;
import io.ballerina.asyncapi.core.model.component.AsyncApiComponent;
import io.ballerina.asyncapi.core.model.component.AsyncApiSchema;
import io.ballerina.asyncapi.core.model.message.AsyncApiMessage;
import io.ballerina.asyncapi.core.model.operation.AsyncApiOperation;
import io.ballerina.asyncapi.core.model.security.AsyncApiSecurityScheme;
import io.ballerina.asyncapi.core.model.server.AsyncApiServer;
import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.ws.client.model.WsClientConfig;
import io.ballerina.asyncapi.generator.ws.client.utils.CodegenUtils;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.BuiltinSimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.CaptureBindingPatternNode;
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.ExplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.FunctionBodyNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingFieldNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.NamedArgumentNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.ParenthesizedArgList;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.TypedBindingPatternNode;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocuments;
import org.ballerinalang.formatter.core.Formatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createSeparatedNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createAnnotationNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBuiltinSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createCaptureBindingPatternNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createCheckExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createExplicitNewExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionBodyBlockNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionDefinitionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionSignatureNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMappingConstructorExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMetadataNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createModulePartNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createModuleVariableDeclarationNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createNamedArgumentNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createParenthesizedArgList;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRequiredExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTypedBindingPatternNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.AT_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CHECK_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.EQUAL_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FUNCTION_DEFINITION;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FUNCTION_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ISOLATED_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.NEW_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SEMICOLON_TOKEN;

/**
 * Generates {@code test.bal} test boilerplate for a WebSocket client.
 */
public class TestGenerator {

    private static final String SERVICE_URL = "serviceUrl";
    private static final String DEFAULT_URL = "/";
    private static final String API_KEY_CONFIG = "apiKeyConfig";
    private static final String API_KEY_TYPE = "apiKey";
    private static final String CONFIG_PLACEHOLDER = "\"<Enter Value>\"";

    private final WsClientConfig config;

    /**
     * Creates a new test generator.
     *
     * @param config the WebSocket client generation configuration
     */
    public TestGenerator(WsClientConfig config) {
        this.config = config;
    }

    /**
     * Generates the Ballerina source content for {@code test.bal}.
     * Returns {@code ""} when the spec has no operations to generate tests for.
     *
     * @return generated source as a string, or {@code ""} if nothing needs to be generated
     * @throws GeneratorException if generation or formatting fails
     */
    public String generateTest() throws GeneratorException {
        // Derive client class name: {Title}{ChannelName}Client
        String title = "AsyncApi";
        if (config.getAsyncApi().getAsyncApiInfo() != null
                && config.getAsyncApi().getAsyncApiInfo().title() != null) {
            title = CodegenUtils.getValidName(config.getAsyncApi().getAsyncApiInfo().title(), true);
        }
        Map<String, AsyncApiChannel> channelMap = config.getAsyncApi().getAsyncApiChannels().orElse(null);
        String channelName = "";
        if (channelMap != null && !channelMap.isEmpty()) {
            Map.Entry<String, AsyncApiChannel> firstEntry = channelMap.entrySet().iterator().next();
            AsyncApiChannel channel = firstEntry.getValue();
            channelName = "/".equals(channel.address())
                    ? "" : CodegenUtils.getValidName(firstEntry.getKey(), true);
        }
        String className = title + channelName + "Client";

        // Derive server URL
        String serverUrl = DEFAULT_URL;
        Map<String, AsyncApiServer> servers = config.getAsyncApi().getAsyncApiServers().orElse(null);
        if (servers != null && !servers.isEmpty()) {
            AsyncApiServer server = servers.values().iterator().next();
            serverUrl = CodegenUtils.buildUrl(server.host(), server.pathname(), server.variables());
        }

        // Collect message names for test functions
        List<String> remoteMsgNames = collectRemoteMsgNames();
        if (remoteMsgNames.isEmpty()) {
            return "";
        }

        // Build module members
        List<ModuleMemberDeclarationNode> nodes = new ArrayList<>();
        nodes.add(buildClientInitNode(className, serverUrl));
        for (String msgName : remoteMsgNames) {
            nodes.add(buildTestFunctionNode(msgName));
        }

        // Assemble syntax tree
        NodeList<io.ballerina.compiler.syntax.tree.ImportDeclarationNode> imports =
                createNodeList(CodegenUtils.getImportDeclarationNode("ballerina", "test"));
        ModulePartNode modulePartNode = createModulePartNode(
                imports, createNodeList(nodes), createIdentifierToken(""));
        TextDocument textDoc = TextDocuments.from("");
        SyntaxTree syntaxTree = SyntaxTree.from(textDoc).modifyWith(modulePartNode);

        try {
            String source = Formatter.format(syntaxTree).toSourceCode();
            String license = config.getLicenseHeader();
            if (license == null || license.isBlank()) {
                return source;
            }
            return license.stripTrailing() + "\n\n" + source;
        } catch (Exception e) {
            throw new GeneratorException("Failed to format generated test source", e);
        }
    }

    /**
     * Generates the content for {@code Config.toml}.
     * Returns {@code ""} for the no-authentication case.
     *
     * @return generated content as a string
     * @throws GeneratorException if generation fails
     */
    public String generateConfig() throws GeneratorException {
        AsyncApiComponent component = config.getAsyncApi().getAsyncApiComponents().orElse(null);
        if (component == null || component.securitySchemes() == null
                || component.securitySchemes().isEmpty()) {
            return "";
        }
        StringBuilder content = new StringBuilder();
        for (Map.Entry<String, AsyncApiSecurityScheme> entry : component.securitySchemes().entrySet()) {
            AsyncApiSecurityScheme scheme = entry.getValue();
            if (API_KEY_TYPE.equals(scheme.type()) && scheme.name() != null) {
                if (content.length() == 0) {
                    content.append("[").append(API_KEY_CONFIG).append("]\n");
                }
                content.append(CodegenUtils.getValidName(scheme.name(), false))
                        .append(" = ").append(CONFIG_PLACEHOLDER).append("\n");
            }
        }
        return content.toString();
    }

    private List<String> collectRemoteMsgNames() {
        List<String> msgNames = new ArrayList<>();
        Map<String, AsyncApiOperation> operations =
                config.getAsyncApi().getAsyncApiOperations().orElse(null);
        if (operations == null) {
            return msgNames;
        }
        for (Map.Entry<String, AsyncApiOperation> opEntry : operations.entrySet()) {
            AsyncApiOperation op = opEntry.getValue();
            if (op.messages() == null) {
                continue;
            }
            for (Map.Entry<String, AsyncApiMessage> msgEntry : op.messages().entrySet()) {
                AsyncApiMessage message = msgEntry.getValue();
                if (message.payload() instanceof AsyncApiSchema) {
                    if (CodegenUtils.isCloseFrameSchema((AsyncApiSchema) message.payload())) {
                        continue;
                    }
                }
                msgNames.add(msgEntry.getKey());
            }
        }
        return msgNames;
    }

    private ModuleVariableDeclarationNode buildClientInitNode(String className, String serverUrl) {
        MetadataNode metadataNode = createMetadataNode(null, createEmptyNodeList());
        BuiltinSimpleNameReferenceNode typeRef =
                createBuiltinSimpleNameReferenceNode(null, createIdentifierToken(className));
        CaptureBindingPatternNode bindingPattern = createCaptureBindingPatternNode(
                createIdentifierToken("baseClient"));
        TypedBindingPatternNode typedBinding = createTypedBindingPatternNode(typeRef, bindingPattern);

        NamedArgumentNode serviceUrlArg = createNamedArgumentNode(
                createSimpleNameReferenceNode(createIdentifierToken(SERVICE_URL)),
                createToken(EQUAL_TOKEN),
                createRequiredExpressionNode(createIdentifierToken('"' + serverUrl + '"')));
        SeparatedNodeList<FunctionArgumentNode> args = createSeparatedNodeList(serviceUrlArg);
        ParenthesizedArgList argList = createParenthesizedArgList(
                createToken(OPEN_PAREN_TOKEN), args, createToken(CLOSE_PAREN_TOKEN));
        TypeDescriptorNode clientClassType =
                createBuiltinSimpleNameReferenceNode(null, createIdentifierToken(className));
        ExplicitNewExpressionNode newExpr =
                createExplicitNewExpressionNode(createToken(NEW_KEYWORD), clientClassType, argList);
        CheckExpressionNode checkExpr =
                createCheckExpressionNode(null, createToken(CHECK_KEYWORD), newExpr);

        return createModuleVariableDeclarationNode(
                metadataNode, null, createEmptyNodeList(), typedBinding,
                createToken(EQUAL_TOKEN), checkExpr, createToken(SEMICOLON_TOKEN));
    }

    private FunctionDefinitionNode buildTestFunctionNode(String msgName) {
        String testFuncName = "test" + CodegenUtils.getValidName(msgName, true);

        SimpleNameReferenceNode annotRef =
                createSimpleNameReferenceNode(createIdentifierToken("test:Config"));
        List<Node> fields = new ArrayList<>();
        SeparatedNodeList<MappingFieldNode> fieldNodes = createSeparatedNodeList(fields);
        MappingConstructorExpressionNode annotValue = createMappingConstructorExpressionNode(
                createToken(OPEN_BRACE_TOKEN), fieldNodes, createToken(CLOSE_BRACE_TOKEN));
        AnnotationNode annotationNode =
                createAnnotationNode(createToken(AT_TOKEN), annotRef, annotValue);
        MetadataNode metadataNode = createMetadataNode(null, createNodeList(annotationNode));

        List<Node> paramList = new ArrayList<>();
        SeparatedNodeList<ParameterNode> params = createSeparatedNodeList(paramList);
        FunctionSignatureNode funcSignature = createFunctionSignatureNode(
                createToken(OPEN_PAREN_TOKEN), params, createToken(CLOSE_PAREN_TOKEN), null);

        List<StatementNode> stmts = new ArrayList<>();
        FunctionBodyNode funcBody = createFunctionBodyBlockNode(
                createToken(OPEN_BRACE_TOKEN), null, createNodeList(stmts), createToken(CLOSE_BRACE_TOKEN), null);

        NodeList<Token> qualifiers = createNodeList(createToken(ISOLATED_KEYWORD));

        return createFunctionDefinitionNode(
                FUNCTION_DEFINITION, metadataNode, qualifiers, createToken(FUNCTION_KEYWORD),
                createIdentifierToken(testFuncName), createEmptyNodeList(), funcSignature, funcBody);
    }
}
