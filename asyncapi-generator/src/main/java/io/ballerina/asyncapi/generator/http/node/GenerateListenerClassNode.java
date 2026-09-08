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
import static io.ballerina.compiler.syntax.tree.NodeFactory.createNilTypeDescriptorNode;
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
    private static final String LISTENER_DISPATCHER_SERVICE_FIELD = "dispatcherService";
    private static final String LISTENER_GET_SERVICE_TYPE_FUNC = "getServiceTypeStr";

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
        members.add(buildDispatcherServiceField());
        members.add(buildInitFunc());
        members.add(buildAttachFunc());
        members.add(buildDetachFunc());
        members.add(buildStartFunc());
        members.add(buildGracefulStopFunc());
        members.add(buildImmediateStopFunc());
        members.add(buildGetServiceTypeStrFunc());

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

    private ObjectFieldNode buildDispatcherServiceField() {
        return createObjectFieldNode(
                null,
                createToken(PRIVATE_KEYWORD),
                createEmptyNodeList(),
                createSimpleNameReferenceNode(
                        createIdentifierToken(GenerateDispatcherServiceNode.DISPATCHER_SERVICE_CLASS_NAME)),
                createIdentifierToken(LISTENER_DISPATCHER_SERVICE_FIELD),
                null,
                null,
                createToken(SEMICOLON_TOKEN));
    }

    private FunctionDefinitionNode buildInitFunc() {
        // public function init(ListenerConfig listenerConfig = {webhookSecret: DEFAULT_SECRET},
        //                      @cloud:Expose int|http:Listener listenOn = 8090) returns error?
        AnnotationNode cloudExposeAnnotation = createAnnotationNode(
                createToken(AT_TOKEN),
                createQualifiedNameReferenceNode(
                        createIdentifierToken(GenerateCloudImportNode.CLOUD_MODULE),
                        createToken(COLON_TOKEN),
                        createIdentifierToken("Expose")),
                null);

        // Connection auth (when present) always contributes at least one required field with no
        // safe default (a password, client secret, API key value, or certificate) - {webhookSecret:
        // DEFAULT_SECRET} alone would then be missing those required fields, which Ballerina
        // rejects outright. So listenerConfig can only stay defaultable when there's no connection
        // auth to account for; otherwise it must be a required parameter, forcing the caller to
        // supply real credentials rather than accepting an incomplete or fabricated default.
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
                        NodeParser.parseExpression(String.format("{%s: %s}",
                                GenerateListenerConfigNode.WEBHOOK_SECRET_FIELD,
                                GenerateListenerConfigNode.DEFAULT_SECRET_CONST)));

        FunctionSignatureNode signature = createFunctionSignatureNode(
                createToken(OPEN_PAREN_TOKEN),
                createSeparatedNodeList(
                        listenerConfigParam,
                        createToken(COMMA_TOKEN),
                        createDefaultableParameterNode(
                                createNodeList(cloudExposeAnnotation),
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

        List<String> configFieldNames = new ArrayList<>();
        configFieldNames.add(GenerateListenerConfigNode.WEBHOOK_SECRET_FIELD);
        webhookAuthConfig.ifPresent(config -> configFieldNames.addAll(config.configFields()));

        List<StatementNode> statements = new ArrayList<>();
        statements.add(NodeParser.parseStatement(String.format(
                "if listenOn is http:Listener { self.%s = listenOn; } else {"
                        + " self.%s = check new (listenOn); }",
                LISTENER_HTTP_LISTENER_FIELD,
                LISTENER_HTTP_LISTENER_FIELD)));
        if (webhookAuthConfig.isPresent()) {
            String constructorArgs = configFieldNames.stream()
                    .map(fieldName -> "listenerConfig." + fieldName)
                    .collect(Collectors.joining(", "));
            statements.add(NodeParser.parseStatement(String.format(
                    "self.%s = new %s(%s);",
                    LISTENER_DISPATCHER_SERVICE_FIELD,
                    GenerateDispatcherServiceNode.DISPATCHER_SERVICE_CLASS_NAME,
                    constructorArgs)));
        } else {
            statements.add(NodeParser.parseStatement(String.format(
                    "self.%s = new %s();",
                    LISTENER_DISPATCHER_SERVICE_FIELD,
                    GenerateDispatcherServiceNode.DISPATCHER_SERVICE_CLASS_NAME)));
        }

        return createFunctionDefinitionNode(
                OBJECT_METHOD_DEFINITION, null,
                createNodeList(createToken(PUBLIC_KEYWORD)),
                createToken(FUNCTION_KEYWORD),
                createIdentifierToken("init"),
                createEmptyNodeList(),
                signature, buildBody(statements));
    }

    private FunctionDefinitionNode buildAttachFunc() {
        // public isolated function attach(GenericServiceType serviceRef, () attachPoint) returns error?
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
                                createNilTypeDescriptorNode(
                                        createToken(OPEN_PAREN_TOKEN), createToken(CLOSE_PAREN_TOKEN)),
                                createIdentifierToken("attachPoint"))),
                createToken(CLOSE_PAREN_TOKEN),
                buildErrorReturnType());

        List<StatementNode> statements = new ArrayList<>();
        statements.add(NodeParser.parseStatement(String.format(
                "string serviceTypeStr = check self.%s(serviceRef);",
                LISTENER_GET_SERVICE_TYPE_FUNC)));
        statements.add(NodeParser.parseStatement(String.format(
                "check self.%s.%s(serviceTypeStr, serviceRef);",
                LISTENER_DISPATCHER_SERVICE_FIELD,
                GenerateAddServiceRefFuncNode.ADD_SERVICE_REF_FUNC)));

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
        statements.add(NodeParser.parseStatement(String.format(
                "check self.%s.%s(serviceTypeStr);",
                LISTENER_DISPATCHER_SERVICE_FIELD,
                GenerateRemoveServiceRefFuncNode.REMOVE_SERVICE_REF_FUNC)));

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
                "check self.%s.attach(self.%s, ());",
                LISTENER_HTTP_LISTENER_FIELD,
                LISTENER_DISPATCHER_SERVICE_FIELD)));
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

    private FunctionBodyBlockNode buildBody(List<StatementNode> statements) {
        return createFunctionBodyBlockNode(
                createToken(OPEN_BRACE_TOKEN), null,
                createNodeList(statements),
                createToken(CLOSE_BRACE_TOKEN), null);
    }
}
