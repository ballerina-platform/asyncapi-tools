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
package io.ballerina.asyncapi.generator.http.node;

import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.http.generator.ServiceTypesGenerator;
import io.ballerina.asyncapi.generator.http.model.ConnectionAuthConfig;
import io.ballerina.asyncapi.generator.http.model.HttpServiceType;
import io.ballerina.asyncapi.generator.http.model.WebhookAuthConfig;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionBodyBlockNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.compiler.syntax.tree.ObjectFieldNode;
import io.ballerina.compiler.syntax.tree.StatementNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.ballerina.asyncapi.generator.http.node.GenerateAddServiceRefFuncNode.buildErrorReturnType;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyMinutiaeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createLiteralValueToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createSeparatedNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createAnnotationNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBasicLiteralNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBuiltinSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createClassDefinitionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createDefaultableParameterNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionBodyBlockNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionDefinitionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionSignatureNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMappingConstructorExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMetadataNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createObjectFieldNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createQualifiedNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRequiredParameterNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createReturnTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSpecificFieldNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createUnionTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.AT_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLASS_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.COLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.COMMA_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.EQUAL_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FUNCTION_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ISOLATED_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OBJECT_METHOD_DEFINITION;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PIPE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PRIVATE_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PUBLIC_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RETURNS_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SEMICOLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.STRING_LITERAL;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.STRING_LITERAL_TOKEN;

/**
 * Generates the {@code public class Listener { ... }} class definition node for {@code listener.bal}.
 *
 * <p>Builds the entire {@code Listener} class from Ballerina Compiler API AST factory methods,
 * including fields, all method definitions, and the {@code @display} annotation.
 * The dynamic {@code getServiceTypeStr} method body is delegated to
 * {@link GenerateListenerStatementNode}.
 */
public class GenerateListenerClassNode implements Generator {

    private static final String LISTENER_CLASS_NAME = "Listener";
    private static final String LISTENER_HTTP_LISTENER_FIELD = "httpListener";
    private static final String LISTENER_CONFIG_FIELD = "listenerConfig";
    private static final String LISTENER_DISPATCHERS_FIELD = "dispatchers";
    private static final String LISTENER_ATTACHMENTS_FIELD = "attachments";
    private static final String LISTENER_GET_SERVICE_TYPE_FUNC = "getServiceTypeStr";
    private static final String LISTENER_PATH_KEY_FUNC = "pathKey";

    private final List<HttpServiceType> serviceTypes;
    private final Optional<WebhookAuthConfig> webhookAuthConfig;
    private final Optional<ConnectionAuthConfig> connectionAuthConfig;
    private final String displayLabel;

    /**
     * Creates a generator for the {@code Listener} class.
     *
     * @param serviceTypes         the list of HTTP service type definitions
     * @param webhookAuthConfig    the optional webhook authentication configuration
     * @param connectionAuthConfig the optional outbound (client-side) API authentication
     *                             configuration - when present, {@code listenerConfig} is
     *                             generated as a required constructor parameter instead of a
     *                             defaultable one, since the generated {@code ListenerConfig}
     *                             then has its own required fields (e.g. a password or client
     *                             secret) with no safe default value to fall back on
     * @param displayLabel         the connector's display name, from the spec's {@code info.title}
     */
    public GenerateListenerClassNode(List<HttpServiceType> serviceTypes,
            Optional<WebhookAuthConfig> webhookAuthConfig, Optional<ConnectionAuthConfig> connectionAuthConfig,
            String displayLabel) {
        this.serviceTypes = serviceTypes;
        this.webhookAuthConfig = webhookAuthConfig;
        this.connectionAuthConfig = connectionAuthConfig;
        this.displayLabel = displayLabel;
    }

    @Override
    public ClassDefinitionNode generate() throws GeneratorException {
        List<Node> members = new ArrayList<>();
        members.add(buildHttpListenerField());
        members.add(buildPrivateField(GenerateListenerConfigNode.LISTENER_CONFIG_TYPE, LISTENER_CONFIG_FIELD, null));
        // One dispatcher per attach path, so triggers can share an http:Listener (#8818).
        members.add(buildPrivateField("map<" + GenerateDispatcherServiceNode.DISPATCHER_SERVICE_CLASS_NAME + ">",
                LISTENER_DISPATCHERS_FIELD, "{}"));
        members.add(buildPrivateField("[string, " + ServiceTypesGenerator.GENERIC_SERVICE_TYPE + "][]",
                LISTENER_ATTACHMENTS_FIELD, "[]"));
        members.add(buildInitFunc());
        members.add(buildAttachFunc());
        members.add(buildDetachFunc());
        members.add(buildStartFunc());
        members.add(buildGracefulStopFunc());
        members.add(buildImmediateStopFunc());
        members.add(buildGetServiceTypeStrFunc());
        members.add(buildPathKeyFunc());

        return createClassDefinitionNode(
                buildDisplayAnnotationMetadata(),
                createToken(PUBLIC_KEYWORD),
                createEmptyNodeList(),
                createToken(CLASS_KEYWORD),
                createIdentifierToken(LISTENER_CLASS_NAME),
                createToken(OPEN_BRACE_TOKEN),
                createNodeList(members),
                createToken(CLOSE_BRACE_TOKEN),
                null);
    }

    private MetadataNode buildDisplayAnnotationMetadata() {
        AnnotationNode annotation = createAnnotationNode(
                createToken(AT_TOKEN),
                createSimpleNameReferenceNode(createIdentifierToken("display")),
                createMappingConstructorExpressionNode(
                        createToken(OPEN_BRACE_TOKEN),
                        createSeparatedNodeList(
                                createSpecificFieldNode(
                                        null,
                                        createIdentifierToken("label"),
                                        createToken(COLON_TOKEN),
                                        createStringLiteralField(displayLabel)),
                                createToken(COMMA_TOKEN),
                                createSpecificFieldNode(
                                        null,
                                        createIdentifierToken("iconPath"),
                                        createToken(COLON_TOKEN),
                                        createStringLiteralField("icon.png"))),
                        createToken(CLOSE_BRACE_TOKEN)));
        return createMetadataNode(null, createNodeList(annotation));
    }

    private BasicLiteralNode createStringLiteralField(String value) {
        String escaped = value.replace("\\", "\\\\").replace("\"", "\\\"");
        return createBasicLiteralNode(STRING_LITERAL,
                createLiteralValueToken(STRING_LITERAL_TOKEN, "\"" + escaped + "\"",
                        createEmptyMinutiaeList(), createEmptyMinutiaeList()));
    }

    private ObjectFieldNode buildHttpListenerField() {
        return createObjectFieldNode(
                null,
                createToken(PRIVATE_KEYWORD),
                createEmptyNodeList(),
                createQualifiedNameReferenceNode(
                        createIdentifierToken(GenerateHttpImportNode.HTTP_MODULE),
                        createToken(COLON_TOKEN),
                        createIdentifierToken(LISTENER_CLASS_NAME)),
                createIdentifierToken(LISTENER_HTTP_LISTENER_FIELD),
                null,
                null,
                createToken(SEMICOLON_TOKEN));
    }

    private ObjectFieldNode buildPrivateField(String type, String name, String initializer) {
        return createObjectFieldNode(
                null,
                createToken(PRIVATE_KEYWORD),
                createEmptyNodeList(),
                NodeParser.parseTypeDescriptor(type),
                createIdentifierToken(name),
                initializer == null ? null : createToken(EQUAL_TOKEN),
                initializer == null ? null : NodeParser.parseExpression(initializer),
                createToken(SEMICOLON_TOKEN));
    }

    private FunctionDefinitionNode buildInitFunc() {
        // public function init(ListenerConfig listenerConfig = {}, int|http:Listener listenOn = 8090) returns error?
        // No @cloud:Expose on the port: it would make every trigger depend on ballerina/cloud.

        // Connection auth (when present) always contributes at least one required field with no
        // safe default (a password, client secret, API key value, or certificate) - {} alone would
        // then be missing those required fields, which Ballerina rejects outright. So listenerConfig
        // can only stay defaultable when there's no connection auth to account for; otherwise it
        // must be a required parameter, forcing the caller to supply real credentials rather than
        // accepting an incomplete or fabricated default.
        Node listenerConfigParam = connectionAuthConfig.isPresent()
                ? createRequiredParameterNode(
                        createEmptyNodeList(),
                        createSimpleNameReferenceNode(
                                createIdentifierToken(GenerateListenerConfigNode.LISTENER_CONFIG_TYPE)),
                        createIdentifierToken("listenerConfig"))
                : createDefaultableParameterNode(
                        createEmptyNodeList(),
                        createSimpleNameReferenceNode(
                                createIdentifierToken(GenerateListenerConfigNode.LISTENER_CONFIG_TYPE)),
                        createIdentifierToken("listenerConfig"),
                        createToken(EQUAL_TOKEN),
                        NodeParser.parseExpression("{}"));

        FunctionSignatureNode signature = createFunctionSignatureNode(
                createToken(OPEN_PAREN_TOKEN),
                createSeparatedNodeList(
                        listenerConfigParam,
                        createToken(COMMA_TOKEN),
                        createDefaultableParameterNode(
                                createEmptyNodeList(),
                                createUnionTypeDescriptorNode(
                                        createBuiltinSimpleNameReferenceNode(null,
                                                createIdentifierToken("int")),
                                        createToken(PIPE_TOKEN),
                                        createQualifiedNameReferenceNode(
                                                createIdentifierToken(GenerateHttpImportNode.HTTP_MODULE),
                                                createToken(COLON_TOKEN),
                                                createIdentifierToken(LISTENER_CLASS_NAME))),
                                createIdentifierToken("listenOn"),
                                createToken(EQUAL_TOKEN),
                                NodeParser.parseExpression("8090"))),
                createToken(CLOSE_PAREN_TOKEN),
                buildErrorReturnType());

        List<StatementNode> statements = new ArrayList<>();
        statements.add(NodeParser.parseStatement(String.format(
                "if listenOn is http:Listener { self.%s = listenOn; } else {"
                        + " self.%s = check new (listenOn); }",
                LISTENER_HTTP_LISTENER_FIELD,
                LISTENER_HTTP_LISTENER_FIELD)));
        statements.add(NodeParser.parseStatement(String.format(
                "self.%s = listenerConfig;", LISTENER_CONFIG_FIELD)));

        return createFunctionDefinitionNode(
                OBJECT_METHOD_DEFINITION, null,
                createNodeList(createToken(PUBLIC_KEYWORD)),
                createToken(FUNCTION_KEYWORD),
                createIdentifierToken("init"),
                createEmptyNodeList(),
                signature, buildBody(statements));
    }

    private FunctionDefinitionNode buildAttachFunc() {
        // public isolated function attach(GenericServiceType serviceRef, string[]|string? attachPoint) returns error?
        FunctionSignatureNode signature = createFunctionSignatureNode(
                createToken(OPEN_PAREN_TOKEN),
                createSeparatedNodeList(
                        createRequiredParameterNode(
                                createEmptyNodeList(),
                                createSimpleNameReferenceNode(
                                        createIdentifierToken(ServiceTypesGenerator.GENERIC_SERVICE_TYPE)),
                                createIdentifierToken("serviceRef")),
                        createToken(COMMA_TOKEN),
                        createRequiredParameterNode(
                                createEmptyNodeList(),
                                NodeParser.parseTypeDescriptor("string[]|string?"),
                                createIdentifierToken("attachPoint"))),
                createToken(CLOSE_PAREN_TOKEN),
                buildErrorReturnType());

        String dispatcherClass = GenerateDispatcherServiceNode.DISPATCHER_SERVICE_CLASS_NAME;
        List<StatementNode> statements = new ArrayList<>();
        statements.add(NodeParser.parseStatement(String.format(
                "string serviceTypeStr = check self.%s(serviceRef);",
                LISTENER_GET_SERVICE_TYPE_FUNC)));
        statements.add(NodeParser.parseStatement(String.format(
                "string path = self.%s(attachPoint);", LISTENER_PATH_KEY_FUNC)));
        statements.add(NodeParser.parseStatement(dispatcherClass + " dispatcher;"));
        statements.add(NodeParser.parseStatement(String.format(
                "%1$s? existing = self.%2$s[path];", dispatcherClass, LISTENER_DISPATCHERS_FIELD)));
        // The first service on a path mounts a new dispatcher there. The service is registered before the
        // path is mounted, so a request on a started listener never reaches a dispatcher without its handler.
        statements.add(NodeParser.parseStatement(String.format(
                "if existing is %1$s { dispatcher = existing; check dispatcher.%5$s(serviceTypeStr, serviceRef); }"
                        + " else { dispatcher = new (%2$s);"
                        + " check dispatcher.%5$s(serviceTypeStr, serviceRef);"
                        + " check self.%3$s.attach(dispatcher, path == \"\" ? () : path);"
                        + " self.%4$s[path] = dispatcher; }",
                dispatcherClass, buildDispatcherConstructorArgs(), LISTENER_HTTP_LISTENER_FIELD,
                LISTENER_DISPATCHERS_FIELD, GenerateAddServiceRefFuncNode.ADD_SERVICE_REF_FUNC)));
        statements.add(NodeParser.parseStatement(String.format(
                "self.%s.push([path, serviceRef]);", LISTENER_ATTACHMENTS_FIELD)));

        return createFunctionDefinitionNode(
                OBJECT_METHOD_DEFINITION, null,
                createNodeList(createToken(PUBLIC_KEYWORD), createToken(ISOLATED_KEYWORD)),
                createToken(FUNCTION_KEYWORD),
                createIdentifierToken("attach"),
                createEmptyNodeList(),
                signature, buildBody(statements));
    }

    private FunctionDefinitionNode buildDetachFunc() {
        // public isolated function detach(GenericServiceType serviceRef) returns error?
        FunctionSignatureNode signature = createFunctionSignatureNode(
                createToken(OPEN_PAREN_TOKEN),
                createSeparatedNodeList(
                        createRequiredParameterNode(
                                createEmptyNodeList(),
                                createSimpleNameReferenceNode(
                                        createIdentifierToken(ServiceTypesGenerator.GENERIC_SERVICE_TYPE)),
                                createIdentifierToken("serviceRef"))),
                createToken(CLOSE_PAREN_TOKEN),
                buildErrorReturnType());

        List<StatementNode> statements = new ArrayList<>();
        statements.add(NodeParser.parseStatement(String.format(
                "string serviceTypeStr = check self.%s(serviceRef);",
                LISTENER_GET_SERVICE_TYPE_FUNC)));
        // Remove the service from every path it is attached on, and unmount a path's dispatcher once its
        // last service is detached, freeing the path.
        statements.add(NodeParser.parseStatement("boolean detached = false;"));
        statements.add(NodeParser.parseStatement("int i = 0;"));
        statements.add(NodeParser.parseStatement(String.format(
                "while i < self.%1$s.length() {"
                        + " if self.%1$s[i][1] !== serviceRef { i += 1; continue; }"
                        + " string path = self.%1$s[i][0];"
                        + " %2$s dispatcher = self.%3$s.get(path);"
                        + " check dispatcher.%4$s(serviceTypeStr);"
                        + " _ = self.%1$s.remove(i);"
                        + " detached = true;"
                        + " boolean pathInUse = false;"
                        + " foreach [string, %5$s] attachment in self.%1$s {"
                        + " if attachment[0] == path { pathInUse = true; } }"
                        + " if !pathInUse { check self.%6$s.detach(dispatcher); _ = self.%3$s.remove(path); } }",
                LISTENER_ATTACHMENTS_FIELD, GenerateDispatcherServiceNode.DISPATCHER_SERVICE_CLASS_NAME,
                LISTENER_DISPATCHERS_FIELD, GenerateRemoveServiceRefFuncNode.REMOVE_SERVICE_REF_FUNC,
                ServiceTypesGenerator.GENERIC_SERVICE_TYPE, LISTENER_HTTP_LISTENER_FIELD)));
        statements.add(NodeParser.parseStatement(
                "if !detached { return error(string `Cannot detach the service of type ${serviceTypeStr}. "
                        + "Service has not been attached to the listener before`); }"));

        return createFunctionDefinitionNode(
                OBJECT_METHOD_DEFINITION, null,
                createNodeList(createToken(PUBLIC_KEYWORD), createToken(ISOLATED_KEYWORD)),
                createToken(FUNCTION_KEYWORD),
                createIdentifierToken("detach"),
                createEmptyNodeList(),
                signature, buildBody(statements));
    }

    private FunctionDefinitionNode buildStartFunc() {
        // public isolated function 'start() returns error?
        FunctionSignatureNode signature = createFunctionSignatureNode(
                createToken(OPEN_PAREN_TOKEN),
                createSeparatedNodeList(),
                createToken(CLOSE_PAREN_TOKEN),
                buildErrorReturnType());

        List<StatementNode> statements = new ArrayList<>();
        statements.add(NodeParser.parseStatement(String.format(
                "return self.%s.'start();",
                LISTENER_HTTP_LISTENER_FIELD)));

        return createFunctionDefinitionNode(
                OBJECT_METHOD_DEFINITION, null,
                createNodeList(createToken(PUBLIC_KEYWORD), createToken(ISOLATED_KEYWORD)),
                createToken(FUNCTION_KEYWORD),
                createIdentifierToken("'start"),
                createEmptyNodeList(),
                signature, buildBody(statements));
    }

    private FunctionDefinitionNode buildGracefulStopFunc() {
        // public isolated function gracefulStop() returns error?
        FunctionSignatureNode signature = createFunctionSignatureNode(
                createToken(OPEN_PAREN_TOKEN),
                createSeparatedNodeList(),
                createToken(CLOSE_PAREN_TOKEN),
                buildErrorReturnType());

        List<StatementNode> statements = new ArrayList<>();
        statements.add(NodeParser.parseStatement(String.format(
                "return self.%s.gracefulStop();",
                LISTENER_HTTP_LISTENER_FIELD)));

        return createFunctionDefinitionNode(
                OBJECT_METHOD_DEFINITION, null,
                createNodeList(createToken(PUBLIC_KEYWORD), createToken(ISOLATED_KEYWORD)),
                createToken(FUNCTION_KEYWORD),
                createIdentifierToken("gracefulStop"),
                createEmptyNodeList(),
                signature, buildBody(statements));
    }

    private FunctionDefinitionNode buildImmediateStopFunc() {
        // public isolated function immediateStop() returns error?
        FunctionSignatureNode signature = createFunctionSignatureNode(
                createToken(OPEN_PAREN_TOKEN),
                createSeparatedNodeList(),
                createToken(CLOSE_PAREN_TOKEN),
                buildErrorReturnType());

        List<StatementNode> statements = new ArrayList<>();
        statements.add(NodeParser.parseStatement(String.format(
                "return self.%s.immediateStop();",
                LISTENER_HTTP_LISTENER_FIELD)));

        return createFunctionDefinitionNode(
                OBJECT_METHOD_DEFINITION, null,
                createNodeList(createToken(PUBLIC_KEYWORD), createToken(ISOLATED_KEYWORD)),
                createToken(FUNCTION_KEYWORD),
                createIdentifierToken("immediateStop"),
                createEmptyNodeList(),
                signature, buildBody(statements));
    }

    private FunctionDefinitionNode buildGetServiceTypeStrFunc() throws GeneratorException {
        // private isolated function getServiceTypeStr(GenericServiceType serviceRef) returns string|error
        // - string|error (not plain string) because an unrecognized serviceRef returns an error
        // (see GenerateListenerStatementNode) rather than panicking, so a caller attaching a
        // service type this listener doesn't understand gets a normal error instead of crashing
        // the whole runtime process.
        FunctionSignatureNode signature = createFunctionSignatureNode(
                createToken(OPEN_PAREN_TOKEN),
                createSeparatedNodeList(
                        createRequiredParameterNode(
                                createEmptyNodeList(),
                                createSimpleNameReferenceNode(
                                        createIdentifierToken(ServiceTypesGenerator.GENERIC_SERVICE_TYPE)),
                                createIdentifierToken("serviceRef"))),
                createToken(CLOSE_PAREN_TOKEN),
                createReturnTypeDescriptorNode(
                        createToken(RETURNS_KEYWORD),
                        createEmptyNodeList(),
                        createUnionTypeDescriptorNode(
                                createBuiltinSimpleNameReferenceNode(null, createIdentifierToken("string")),
                                createToken(PIPE_TOKEN),
                                createBuiltinSimpleNameReferenceNode(null, createIdentifierToken("error")))));

        List<String> typeNames = serviceTypes.stream()
                .map(HttpServiceType::serviceTypeName)
                .collect(Collectors.toList());
        StatementNode body = new GenerateListenerStatementNode(typeNames).generate();

        return createFunctionDefinitionNode(
                OBJECT_METHOD_DEFINITION, null,
                createNodeList(createToken(PRIVATE_KEYWORD), createToken(ISOLATED_KEYWORD)),
                createToken(FUNCTION_KEYWORD),
                createIdentifierToken(LISTENER_GET_SERVICE_TYPE_FUNC),
                createEmptyNodeList(),
                signature, buildBody(List.of(body)));
    }

    private String buildDispatcherConstructorArgs() {
        if (webhookAuthConfig.isEmpty()) {
            return "";
        }
        List<String> configFieldNames = new ArrayList<>();
        configFieldNames.add(GenerateListenerConfigNode.WEBHOOK_SECRET_FIELD);
        configFieldNames.addAll(webhookAuthConfig.get().configFields());
        return configFieldNames.stream()
                .map(fieldName -> "self." + LISTENER_CONFIG_FIELD + "." + fieldName)
                .collect(Collectors.joining(", "));
    }

    private FunctionDefinitionNode buildPathKeyFunc() {
        // private isolated function pathKey(string[]|string? attachPoint) returns string
        // "/github/", "github" and ["github"] all name the same path, so they share a dispatcher.
        FunctionSignatureNode signature = createFunctionSignatureNode(
                createToken(OPEN_PAREN_TOKEN),
                createSeparatedNodeList(
                        createRequiredParameterNode(
                                createEmptyNodeList(),
                                NodeParser.parseTypeDescriptor("string[]|string?"),
                                createIdentifierToken("attachPoint"))),
                createToken(CLOSE_PAREN_TOKEN),
                createReturnTypeDescriptorNode(
                        createToken(RETURNS_KEYWORD),
                        createEmptyNodeList(),
                        createBuiltinSimpleNameReferenceNode(null, createIdentifierToken("string"))));

        List<StatementNode> statements = List.of(
                NodeParser.parseStatement("if attachPoint is () { return \"\"; }"),
                NodeParser.parseStatement(
                        "string path = attachPoint is string ? attachPoint : string:'join(\"/\", ...attachPoint);"),
                NodeParser.parseStatement("while path.startsWith(\"/\") { path = path.substring(1); }"),
                NodeParser.parseStatement(
                        "while path.endsWith(\"/\") { path = path.substring(0, path.length() - 1); }"),
                NodeParser.parseStatement("return path;"));

        return createFunctionDefinitionNode(
                OBJECT_METHOD_DEFINITION, null,
                createNodeList(createToken(PRIVATE_KEYWORD), createToken(ISOLATED_KEYWORD)),
                createToken(FUNCTION_KEYWORD),
                createIdentifierToken(LISTENER_PATH_KEY_FUNC),
                createEmptyNodeList(),
                signature, buildBody(statements));
    }

    private FunctionBodyBlockNode buildBody(List<StatementNode> statements) {
        return createFunctionBodyBlockNode(
                createToken(OPEN_BRACE_TOKEN), null,
                createNodeList(statements),
                createToken(CLOSE_BRACE_TOKEN), null);
    }
}
