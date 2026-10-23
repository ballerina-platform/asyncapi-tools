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
import io.ballerina.asyncapi.core.model.message.AsyncApiMessage;
import io.ballerina.asyncapi.core.model.operation.AsyncApiOperation;
import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.ws.client.model.WsClientConfig;
import io.ballerina.asyncapi.generator.ws.client.utils.CodegenUtils;
import io.ballerina.compiler.syntax.tree.AssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.ChildNodeEntry;
import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionBodyNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.MarkdownDocumentationNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.compiler.syntax.tree.ObjectFieldNode;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.ReturnTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.TypeReferenceNode;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocuments;
import org.ballerinalang.formatter.core.Formatter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.ballerina.asyncapi.generator.ws.client.generator.ClientGenerator.SERVER_STREAMING;
import static io.ballerina.asyncapi.generator.ws.client.generator.ClientGenerator.X_RESPONSE;
import static io.ballerina.asyncapi.generator.ws.client.generator.ClientGenerator.X_RESPONSE_TYPE;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createSeparatedNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createAssignmentStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBlockStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createClassDefinitionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionBodyBlockNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionDefinitionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionSignatureNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createIfElseStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMarkdownDocumentationNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMetadataNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createModulePartNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createObjectFieldNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRequiredParameterNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createReturnTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSingletonTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTypeDefinitionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTypeReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createWhileStatementNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ASTERISK_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLASS_DEFINITION;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLASS_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLIENT_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.COMMA_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CONTINUE_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.EOF_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.EQUAL_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FINAL_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FUNCTION_DEFINITION;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FUNCTION_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.IF_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ISOLATED_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OBJECT_METHOD_DEFINITION;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PRIVATE_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PUBLIC_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RETURNS_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RETURN_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SEMICOLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.TYPE_DEFINITION;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.TYPE_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.WHILE_KEYWORD;

/**
 * Generates {@code util.bal} content for a WebSocket client.
 */
public class UtilGenerator {

    private static final String BALLERINA = "ballerina";
    private static final String XLIBB = "xlibb";
    private static final String URL = "url";
    private static final String PIPE = "pipe";
    private static final String PIPES_MAP_CLASS = "PipesMap";
    private static final String GENERATOR_TYPE = "Generator";
    private static final String STREAM_GENERATORS_MAP_CLASS = "StreamGeneratorsMap";
    private static final String STREAM_GENERATOR_SUFFIX = "StreamGenerator";
    private static final String PIPES = "pipes";
    private static final String PIPE_ID = "pipeId";
    private static final String TIMEOUT = "timeout";
    private static final String SELF_DOT = "self.";
    private static final String DECIMAL = "decimal";
    private static final String STRING = "string";
    private static final String CLOSE_STREAM_STATEMENT = "check self.pipes.removePipe(self.pipeId);";
    private static final String STREAM_NEXT_CONSUME_MESSAGE =
            "anydata|error? message = self.pipes.getPipe(self.pipeId).consume(self.timeout);";
    private static final String STREAM_NEXT_RESPONSE_CLONE = "%s response = check message.cloneWithType();";
    private static final String MESSAGE_VAR_NAME = "message";
    private static final String OPTIONAL_ERROR = "error?";
    private static final String RESPONSE = "response";
    private static final String TEMPLATE_PATH = "templates/utils_asyncapi.bal";

    private static final Token OPEN_PAREN = createToken(OPEN_PAREN_TOKEN);
    private static final Token CLOSE_PAREN = createToken(CLOSE_PAREN_TOKEN);
    private static final Token OPEN_BRACE = createToken(OPEN_BRACE_TOKEN);
    private static final Token CLOSE_BRACE = createToken(CLOSE_BRACE_TOKEN);
    private static final Token SEMICOLON = createToken(SEMICOLON_TOKEN);

    private final WsClientConfig config;

    /**
     * Creates a new utility generator.
     *
     * @param config the WebSocket client generation configuration
     */
    public UtilGenerator(WsClientConfig config) {
        this.config = config;
    }

    /**
     * Generates the Ballerina source content for {@code util.bal}.
     * Derives all generation flags (stream returns, query/header/path params) from the spec.
     *
     * @return generated source as a string, or {@code ""} if nothing needs to be generated
     * @throws GeneratorException if the spec is invalid or generation fails
     */
    public String generate() throws GeneratorException {
        AsyncApiSpec spec = config.getAsyncApi();

        // Derive flags from spec
        List<String> streamReturns = extractStreamReturns(spec);
        boolean queryParamsFound = hasQueryParams(spec);
        boolean headersFound = hasHeaderParams(spec);
        boolean pathParamsFound = hasPathParams(spec);
        boolean streamPresent = !streamReturns.isEmpty();
        boolean needsUrl = queryParamsFound || pathParamsFound;

        // Load static declarations from template
        Map<String, ModuleMemberDeclarationNode> templateMembers = loadTemplateMembers();

        // Build imports
        List<ImportDeclarationNode> imports = new ArrayList<>();
        if (needsUrl) {
            imports.add(CodegenUtils.getImportDeclarationNode(BALLERINA, URL));
        }
        imports.add(CodegenUtils.getImportDeclarationNode(XLIBB, PIPE));

        // Build member declarations
        List<ModuleMemberDeclarationNode> members = new ArrayList<>();
        if (queryParamsFound || headersFound) {
            members.add(buildSimpleBasicType());
        }
        members.add(templateMembers.get(PIPES_MAP_CLASS));
        if (streamPresent) {
            members.add(templateMembers.get(GENERATOR_TYPE));
            members.add(templateMembers.get(STREAM_GENERATORS_MAP_CLASS));
            for (String returnType : streamReturns) {
                members.add(buildStreamGeneratorClass(returnType));
            }
        }
        if (needsUrl) {
            members.add(templateMembers.get("getEncodedUri"));
        }
        if (queryParamsFound) {
            members.add(templateMembers.get("getPathForQueryParam"));
        }
        if (headersFound) {
            members.add(templateMembers.get("getCombineHeaders"));
        }

        // Assemble module and format
        NodeList<ImportDeclarationNode> importList = createNodeList(imports);
        NodeList<ModuleMemberDeclarationNode> memberList = createNodeList(members);
        Token eofToken = createToken(EOF_TOKEN);
        ModulePartNode modulePartNode = createModulePartNode(importList, memberList, eofToken);
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
            throw new GeneratorException("Failed to format generated util source", e);
        }
    }

    /**
     * Loads {@code utils_asyncapi.bal} from classpath resources and returns a map of module
     * member declaration nodes keyed by their class, type, or function name.
     *
     * @return map from declaration name to node
     * @throws GeneratorException if the template cannot be read or parsed
     */
    private Map<String, ModuleMemberDeclarationNode> loadTemplateMembers() throws GeneratorException {
        String source;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(TEMPLATE_PATH)) {
            if (is == null) {
                throw new GeneratorException("Cannot find template resource: " + TEMPLATE_PATH);
            }
            source = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new GeneratorException("Failed to read template: " + TEMPLATE_PATH, e);
        }
        ModulePartNode modulePartNode = (ModulePartNode) SyntaxTree.from(TextDocuments.from(source)).rootNode();
        Map<String, ModuleMemberDeclarationNode> result = new HashMap<>();
        for (ModuleMemberDeclarationNode node : modulePartNode.members()) {
            if (node.kind() == FUNCTION_DEFINITION || node.kind() == CLASS_DEFINITION
                    || node.kind() == TYPE_DEFINITION) {
                for (ChildNodeEntry entry : node.childEntries()) {
                    String entryName = entry.name();
                    if ("functionName".equals(entryName) || "className".equals(entryName)
                            || "typeName".equals(entryName)) {
                        entry.node().ifPresent(n -> result.put(n.toString().trim(), node));
                        break;
                    }
                }
            }
        }
        return result;
    }

    /**
     * Builds the {@code SimpleBasicType} type definition node using NodeFactory.
     *
     * @return type definition node for {@code type SimpleBasicType string|boolean|int|float|decimal;}
     */
    private TypeDefinitionNode buildSimpleBasicType() {
        TypeDescriptorNode typeDesc = createSingletonTypeDescriptorNode(
                createSimpleNameReferenceNode(createIdentifierToken("string|boolean|int|float|decimal")));
        return createTypeDefinitionNode(null, null, createToken(TYPE_KEYWORD),
                createIdentifierToken("SimpleBasicType"), typeDesc, SEMICOLON);
    }

    /**
     * Builds a stream generator class definition node for the given return type.
     *
     * @param returnType the Ballerina return type (e.g. {@code "Foo"} or {@code "Foo|Bar"})
     * @return the class definition node for the stream generator
     */
    private ClassDefinitionNode buildStreamGeneratorClass(String returnType) {
        List<Node> memberNodeList = new ArrayList<>();
        memberNodeList.addAll(createClassInstanceVariables());
        memberNodeList.add(createInitFunction());
        memberNodeList.add(createNextFunction(returnType));
        memberNodeList.add(createCloseFunction());

        MetadataNode metadataNode = getClassMetadataNode(returnType);
        String className = CodegenUtils.getStreamGeneratorName(returnType) + STREAM_GENERATOR_SUFFIX;
        NodeList<Token> classTypeQualifiers = createNodeList(createToken(CLIENT_KEYWORD),
                createToken(ISOLATED_KEYWORD));
        return createClassDefinitionNode(metadataNode, createToken(PUBLIC_KEYWORD), classTypeQualifiers,
                createToken(CLASS_KEYWORD), createIdentifierToken(className), OPEN_BRACE,
                createNodeList(memberNodeList), CLOSE_BRACE, null);
    }

    /**
     * Builds the instance variable list for a stream generator class.
     *
     * @return list of field nodes: type inclusion, pipes, pipeId, timeout
     */
    private List<Node> createClassInstanceVariables() {
        List<Node> fieldNodeList = new ArrayList<>();
        NodeList<Token> qualifierList = createNodeList(createToken(PRIVATE_KEYWORD),
                createToken(FINAL_KEYWORD));

        TypeReferenceNode typeReferenceNode = createTypeReferenceNode(createToken(ASTERISK_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken(GENERATOR_TYPE)), SEMICOLON);

        ObjectFieldNode pipesField = createObjectFieldNode(null, null, qualifierList,
                NodeParser.parseTypeDescriptor(PIPES_MAP_CLASS), createIdentifierToken(PIPES),
                null, null, SEMICOLON);

        ObjectFieldNode pipeIdField = createObjectFieldNode(null, null, qualifierList,
                createSimpleNameReferenceNode(createIdentifierToken(STRING)),
                createIdentifierToken(PIPE_ID), null, null, SEMICOLON);

        ObjectFieldNode timeoutField = createObjectFieldNode(null, null, qualifierList,
                createSimpleNameReferenceNode(createIdentifierToken(DECIMAL)),
                createIdentifierToken(TIMEOUT), null, null, SEMICOLON);

        fieldNodeList.add(typeReferenceNode);
        fieldNodeList.add(pipesField);
        fieldNodeList.add(pipeIdField);
        fieldNodeList.add(timeoutField);
        return fieldNodeList;
    }

    /**
     * Builds the {@code init} function for a stream generator class.
     *
     * @return the init function definition node
     */
    private FunctionDefinitionNode createInitFunction() {
        NodeList<Token> qualifierList = createNodeList(createToken(PUBLIC_KEYWORD),
                createToken(ISOLATED_KEYWORD));
        return createFunctionDefinitionNode(OBJECT_METHOD_DEFINITION, getInitDocComment(),
                qualifierList, createToken(FUNCTION_KEYWORD), createIdentifierToken("init"),
                createEmptyNodeList(), getStreamInitFunctionSignatureNode(),
                getStreamInitFunctionBodyNode());
    }

    /**
     * Builds the signature for the stream generator {@code init} function.
     *
     * @return function signature with pipes, pipeId, and timeout parameters
     */
    private FunctionSignatureNode getStreamInitFunctionSignatureNode() {
        RequiredParameterNode pipesParam = createRequiredParameterNode(createNodeList(),
                createSimpleNameReferenceNode(createIdentifierToken(PIPES_MAP_CLASS)),
                createIdentifierToken(PIPES));
        RequiredParameterNode pipeIdParam = createRequiredParameterNode(createNodeList(),
                createSimpleNameReferenceNode(createIdentifierToken(STRING)),
                createIdentifierToken(PIPE_ID));
        RequiredParameterNode timeoutParam = createRequiredParameterNode(createNodeList(),
                createSimpleNameReferenceNode(createIdentifierToken(DECIMAL)),
                createIdentifierToken(TIMEOUT));
        List<Node> paramList = new ArrayList<>();
        paramList.add(pipesParam);
        paramList.add(createToken(COMMA_TOKEN));
        paramList.add(pipeIdParam);
        paramList.add(createToken(COMMA_TOKEN));
        paramList.add(timeoutParam);
        SeparatedNodeList<ParameterNode> parameters = createSeparatedNodeList(paramList);
        return createFunctionSignatureNode(OPEN_PAREN, parameters, CLOSE_PAREN, null);
    }

    /**
     * Builds the body for the stream generator {@code init} function.
     *
     * @return function body with self.pipes, self.pipeId, self.timeout assignments
     */
    private FunctionBodyNode getStreamInitFunctionBodyNode() {
        AssignmentStatementNode selfPipes = createAssignmentStatementNode(
                createIdentifierToken(SELF_DOT + PIPES), createToken(EQUAL_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken(PIPES)), createToken(SEMICOLON_TOKEN));
        AssignmentStatementNode selfPipeId = createAssignmentStatementNode(
                createIdentifierToken(SELF_DOT + PIPE_ID), createToken(EQUAL_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken(PIPE_ID)), createToken(SEMICOLON_TOKEN));
        AssignmentStatementNode selfTimeout = createAssignmentStatementNode(
                createIdentifierToken(SELF_DOT + TIMEOUT), createToken(EQUAL_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken(TIMEOUT)), createToken(SEMICOLON_TOKEN));
        return createFunctionBodyBlockNode(OPEN_BRACE, null,
                createNodeList(selfPipes, selfPipeId, selfTimeout), CLOSE_BRACE, null);
    }

    /**
     * Builds the metadata (doc comment) for the stream generator {@code init} function.
     *
     * @return metadata node with StreamGenerator description and param docs
     */
    private MetadataNode getInitDocComment() {
        List<Node> docs = new ArrayList<>();
        docs.addAll(DocCommentsGenerator.createAPIDescriptionDoc("StreamGenerator", true));
        docs.add(DocCommentsGenerator.createAPIParamDoc(PIPE, "Pipe to hold stream messages"));
        docs.add(DocCommentsGenerator.createAPIParamDoc(TIMEOUT, "Waiting time"));
        MarkdownDocumentationNode docNode = createMarkdownDocumentationNode(createNodeList(docs));
        return createMetadataNode(docNode, createEmptyNodeList());
    }

    /**
     * Builds the {@code next} function for a stream generator class.
     *
     * @param returnType the Ballerina return type of the stream
     * @return the next function definition node
     */
    private FunctionDefinitionNode createNextFunction(String returnType) {
        NodeList<Token> qualifierList = createNodeList(createToken(PUBLIC_KEYWORD),
                createToken(ISOLATED_KEYWORD));
        return createFunctionDefinitionNode(OBJECT_METHOD_DEFINITION, null, qualifierList,
                createToken(FUNCTION_KEYWORD), createIdentifierToken("next"), createEmptyNodeList(),
                getNextFunctionSignatureNode(returnType), getNextFunctionBodyNode(returnType));
    }

    /**
     * Builds the signature for the {@code next} function.
     *
     * @param returnType the stream return type
     * @return function signature returning {@code record {|T value;|}|error}
     */
    private FunctionSignatureNode getNextFunctionSignatureNode(String returnType) {
        SimpleNameReferenceNode returnTypeNode = createSimpleNameReferenceNode(
                createIdentifierToken("record {|" + returnType + " value;|}|error"));
        ReturnTypeDescriptorNode returnTypeDesc = createReturnTypeDescriptorNode(
                createToken(RETURNS_KEYWORD), createEmptyNodeList(), returnTypeNode);
        return createFunctionSignatureNode(OPEN_PAREN, createSeparatedNodeList(), CLOSE_PAREN,
                returnTypeDesc);
    }

    /**
     * Builds the body for the {@code next} function.
     *
     * @param returnType the stream return type
     * @return function body with while loop consuming from pipe
     */
    private FunctionBodyNode getNextFunctionBodyNode(String returnType) {
        List<StatementNode> statements = new ArrayList<>();
        statements.add(NodeParser.parseStatement(STREAM_NEXT_CONSUME_MESSAGE));
        statements.add(createIfElseStatementNode(createToken(IF_KEYWORD),
                NodeParser.parseExpression(MESSAGE_VAR_NAME + " is " + OPTIONAL_ERROR),
                createBlockStatementNode(OPEN_BRACE,
                        createNodeList(NodeParser.parseStatement(CONTINUE_KEYWORD.stringValue() + ";")),
                        CLOSE_BRACE),
                null));
        statements.add(NodeParser.parseStatement(String.format(STREAM_NEXT_RESPONSE_CLONE, returnType)));
        statements.add(NodeParser.parseStatement(
                RETURN_KEYWORD.stringValue() + " {value: " + RESPONSE + "};"));
        NodeList<StatementNode> whileBody = createNodeList(statements);
        return createFunctionBodyBlockNode(OPEN_BRACE, null,
                createNodeList(createWhileStatementNode(createToken(WHILE_KEYWORD),
                        NodeParser.parseExpression("true"),
                        createBlockStatementNode(OPEN_BRACE, whileBody, CLOSE_BRACE), null)),
                CLOSE_BRACE, null);
    }

    /**
     * Builds the {@code close} function for a stream generator class.
     *
     * @return the close function definition node
     */
    private FunctionDefinitionNode createCloseFunction() {
        NodeList<Token> qualifierList = createNodeList(createToken(PUBLIC_KEYWORD),
                createToken(ISOLATED_KEYWORD));
        SimpleNameReferenceNode returnTypeNode = createSimpleNameReferenceNode(
                createIdentifierToken(OPTIONAL_ERROR));
        ReturnTypeDescriptorNode returnTypeDesc = createReturnTypeDescriptorNode(
                createToken(RETURNS_KEYWORD), createEmptyNodeList(), returnTypeNode);
        FunctionSignatureNode signature = createFunctionSignatureNode(OPEN_PAREN,
                createSeparatedNodeList(), CLOSE_PAREN, returnTypeDesc);
        FunctionBodyNode body = createFunctionBodyBlockNode(OPEN_BRACE, null,
                createNodeList(NodeParser.parseStatement(CLOSE_STREAM_STATEMENT)), CLOSE_BRACE, null);
        return createFunctionDefinitionNode(OBJECT_METHOD_DEFINITION, null, qualifierList,
                createToken(FUNCTION_KEYWORD), createIdentifierToken("close"), createEmptyNodeList(),
                signature, body);
    }

    /**
     * Builds the metadata (doc comment) for a stream generator class.
     *
     * @param returnType the stream return type
     * @return metadata node with class description
     */
    private MetadataNode getClassMetadataNode(String returnType) {
        List<Node> documentationLines = new ArrayList<>(DocCommentsGenerator.createAPIDescriptionDoc(
                "Stream generator class for " + returnType + " return type", false));
        MarkdownDocumentationNode apiDoc = createMarkdownDocumentationNode(
                createNodeList(documentationLines));
        return createMetadataNode(apiDoc, createEmptyNodeList());
    }

    /**
     * Scans SEND operations for messages with {@code x-response-type: server-streaming}
     * and collects their return type strings.
     *
     * @param spec the parsed spec
     * @return list of stream return type strings
     * @throws GeneratorException if return type computation fails
     */
    private List<String> extractStreamReturns(AsyncApiSpec spec) throws GeneratorException {
        List<String> streamReturns = new ArrayList<>();
        Map<String, AsyncApiOperation> operations = spec.getAsyncApiOperations().orElse(null);
        if (operations == null) {
            return streamReturns;
        }
        RemoteFunctionReturnTypeGenerator returnTypeGen = new RemoteFunctionReturnTypeGenerator(spec);
        for (AsyncApiOperation op : operations.values()) {
            if (op.action() != AsyncApiOperation.Action.SEND || op.messages() == null) {
                continue;
            }
            for (AsyncApiMessage message : op.messages().values()) {
                Map<String, JsonNode> ext = message.extensions();
                if (ext == null || !ext.containsKey(X_RESPONSE)) {
                    continue;
                }
                JsonNode xResponseType = ext.get(X_RESPONSE_TYPE);
                if (xResponseType == null || !SERVER_STREAMING.equals(xResponseType.asText())) {
                    continue;
                }
                List<String> responseMessages = new ArrayList<>();
                String returnType = returnTypeGen.getReturnType(ext.get(X_RESPONSE), xResponseType,
                        responseMessages);
                if (!"null".equals(returnType) && !streamReturns.contains(returnType)) {
                    streamReturns.add(returnType);
                }
            }
        }
        return streamReturns;
    }

    /**
     * Returns {@code true} if the first channel has a WS binding query schema with properties.
     *
     * @param spec the parsed spec
     * @return {@code true} if query parameters are present
     */
    private boolean hasQueryParams(AsyncApiSpec spec) {
        AsyncApiChannel channel = getFirstChannel(spec);
        if (channel == null || channel.bindings() == null
                || channel.bindings().wsChannelBindings() == null) {
            return false;
        }
        JsonNode query = channel.bindings().wsChannelBindings().query();
        return query != null && query.get("properties") != null;
    }

    /**
     * Returns {@code true} if the first channel has a WS binding headers schema with properties.
     *
     * @param spec the parsed spec
     * @return {@code true} if header parameters are present
     */
    private boolean hasHeaderParams(AsyncApiSpec spec) {
        AsyncApiChannel channel = getFirstChannel(spec);
        if (channel == null || channel.bindings() == null
                || channel.bindings().wsChannelBindings() == null) {
            return false;
        }
        JsonNode headers = channel.bindings().wsChannelBindings().headers();
        return headers != null && headers.get("properties") != null;
    }

    /**
     * Returns {@code true} if the first channel has path parameters.
     *
     * @param spec the parsed spec
     * @return {@code true} if path parameters are present
     */
    private boolean hasPathParams(AsyncApiSpec spec) {
        AsyncApiChannel channel = getFirstChannel(spec);
        return channel != null && channel.parameters() != null && !channel.parameters().isEmpty();
    }

    /**
     * Returns the first channel from the spec, or {@code null} if none.
     *
     * @param spec the parsed spec
     * @return the first channel, or {@code null}
     */
    private AsyncApiChannel getFirstChannel(AsyncApiSpec spec) {
        Map<String, AsyncApiChannel> channels = spec.getAsyncApiChannels().orElse(null);
        if (channels == null || channels.isEmpty()) {
            return null;
        }
        return channels.values().iterator().next();
    }
}
