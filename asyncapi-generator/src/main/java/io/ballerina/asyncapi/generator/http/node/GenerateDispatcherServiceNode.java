/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com)
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.ballerina.asyncapi.generator.http.node;

import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.http.extractor.EventIdentifierExtractor;
import io.ballerina.asyncapi.generator.http.generator.ServiceTypesGenerator;
import io.ballerina.asyncapi.generator.http.model.EventIdentifierConfig;
import io.ballerina.asyncapi.generator.http.model.HttpServiceType;
import io.ballerina.asyncapi.generator.http.model.WebhookAuthConfig;
import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionBodyBlockNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.compiler.syntax.tree.ObjectFieldNode;
import io.ballerina.compiler.syntax.tree.StatementNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createSeparatedNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBuiltinSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createClassDefinitionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionBodyBlockNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionDefinitionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionSignatureNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createImplicitNewExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMapTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMappingConstructorExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createObjectFieldNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createParenthesizedArgList;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createQualifiedNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRequiredParameterNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTypeParameterNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTypeReferenceNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ASTERISK_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLASS_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.COLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.COMMA_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.EQUAL_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FUNCTION_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.GT_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.LT_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.MAP_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.NEW_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OBJECT_METHOD_DEFINITION;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PRIVATE_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SEMICOLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SERVICE_KEYWORD;

/**
 * Generates the {@code service class DispatcherService { ... }} class definition node
 * for {@code dispatcher_service.bal}.
 *
 * <p>Delegates each method to a dedicated function-node generator and composes the full class body
 * including type-reference inheritance, field declarations, and all method definitions.
 */
public class GenerateDispatcherServiceNode implements Generator {

    public static final String DISPATCHER_SERVICE_CLASS_NAME = "DispatcherService";
    public static final String DISPATCHER_SERVICES_FIELD = "services";
    public static final String DISPATCHER_NATIVE_HANDLER_FIELD = "nativeHandler";
    public static final String CLONE_WITH_TYPE_VAR_NAME = "genericDataType";
    private static final String NATIVE_HANDLER_TYPE = "NativeHandler";
    public static final String WEBHOOK_SECRET_FIELD = "webhookSecret";

    private final List<HttpServiceType> serviceTypes;
    private final EventIdentifierConfig identifierConfig;
    private final Optional<WebhookAuthConfig> webhookAuthConfig;
    private final String serviceName;

    /**
     * Creates a generator for the {@code DispatcherService} class.
     *
     * @param serviceTypes      the list of HTTP service type definitions
     * @param identifierConfig  the resolved event identifier type and path
     * @param webhookAuthConfig the optional webhook authentication configuration
     * @param serviceName       a label identifying the generated package, embedded into the
     *                          {@code MATCH_LEVEL_1_*}, {@code MATCH_LEVEL_2_*}, and
     *                          {@code HANDLER_EXECUTED_*} diagnostic trace log messages
     */
    public GenerateDispatcherServiceNode(List<HttpServiceType> serviceTypes, EventIdentifierConfig identifierConfig,
                                         Optional<WebhookAuthConfig> webhookAuthConfig, String serviceName) {
        this.serviceTypes = serviceTypes;
        this.identifierConfig = identifierConfig;
        this.webhookAuthConfig = webhookAuthConfig;
        this.serviceName = serviceName;
    }

    @Override
    public ClassDefinitionNode generate() throws GeneratorException {
        String eventIdentifierPath;
        if (EventIdentifierExtractor.X_BALLERINA_EVENT_TYPE_BODY.equals(identifierConfig.type())) {
            eventIdentifierPath = String.format("%s.%s",
                    CLONE_WITH_TYPE_VAR_NAME, identifierConfig.path());
        } else {
            eventIdentifierPath = "eventIdentifier";
        }

        List<Node> members = new ArrayList<>();
        members.add(buildHttpServiceTypeRef());
        members.add(buildServicesField());
        members.add(buildNativeHandlerField());
        if (webhookAuthConfig.isPresent()) {
            for (String fieldName : getConfigFieldNames()) {
                members.add(buildConfigField(fieldName));
            }
            members.add(buildInitFunction());
        }
        members.add(buildFunc(new GenerateAddServiceRefFuncNode()));
        members.add(buildFunc(new GenerateRemoveServiceRefFuncNode()));
        members.add(buildFunc(new GeneratePostResourceFunctionNode(identifierConfig, webhookAuthConfig)));
        
        if (webhookAuthConfig.isPresent()) {
            members.add(buildFunc(new GenerateVerifyWebhookSignatureFuncNode(webhookAuthConfig.get())));
        }
        
        GenerateMatchRemoteFuncNode matchRemoteFuncGen =
                new GenerateMatchRemoteFuncNode(serviceTypes, identifierConfig, eventIdentifierPath, serviceName);
        members.add(buildFunc(matchRemoteFuncGen));

        // Add one chunk function per channel group if chunking was triggered
        for (GenerateMatchChunkFuncNode chunkGen : matchRemoteFuncGen.getChunkGenerators()) {
            members.add(buildFunc(chunkGen));
        }

        members.add(buildFunc(new GenerateExecuteRemoteFuncNode(serviceName)));

        return createClassDefinitionNode(
                null,
                null,
                createNodeList(createToken(SERVICE_KEYWORD)),
                createToken(CLASS_KEYWORD),
                createIdentifierToken(DISPATCHER_SERVICE_CLASS_NAME),
                createToken(OPEN_BRACE_TOKEN),
                createNodeList(members),
                createToken(CLOSE_BRACE_TOKEN),
                null);
    }

    private Node buildHttpServiceTypeRef() {
        return createTypeReferenceNode(
                createToken(ASTERISK_TOKEN),
                createQualifiedNameReferenceNode(
                        createIdentifierToken(GenerateHttpImportNode.HTTP_MODULE),
                        createToken(COLON_TOKEN),
                        createIdentifierToken("Service")),
                createToken(SEMICOLON_TOKEN));
    }

    private ObjectFieldNode buildServicesField() {
        return createObjectFieldNode(
                null,
                createToken(PRIVATE_KEYWORD),
                createEmptyNodeList(),
                createMapTypeDescriptorNode(
                        createToken(MAP_KEYWORD),
                        createTypeParameterNode(
                                createToken(LT_TOKEN),
                                createSimpleNameReferenceNode(
                                        createIdentifierToken(ServiceTypesGenerator.GENERIC_SERVICE_TYPE)),
                                createToken(GT_TOKEN))),
                createIdentifierToken(DISPATCHER_SERVICES_FIELD),
                createToken(EQUAL_TOKEN),
                createMappingConstructorExpressionNode(
                        createToken(OPEN_BRACE_TOKEN),
                        createSeparatedNodeList(),
                        createToken(CLOSE_BRACE_TOKEN)),
                createToken(SEMICOLON_TOKEN));
    }

    private ObjectFieldNode buildNativeHandlerField() {
        return createObjectFieldNode(
                null,
                createToken(PRIVATE_KEYWORD),
                createEmptyNodeList(),
                createQualifiedNameReferenceNode(
                        createIdentifierToken(GenerateNativeHandlerImportNode.NATIVE_HANDLER_MODULE_ALIAS),
                        createToken(COLON_TOKEN),
                        createIdentifierToken(NATIVE_HANDLER_TYPE)),
                createIdentifierToken(DISPATCHER_NATIVE_HANDLER_FIELD),
                createToken(EQUAL_TOKEN),
                createImplicitNewExpressionNode(
                        createToken(NEW_KEYWORD),
                        createParenthesizedArgList(
                                createToken(OPEN_PAREN_TOKEN),
                                createSeparatedNodeList(),
                                createToken(CLOSE_PAREN_TOKEN))),
                createToken(SEMICOLON_TOKEN));
    }

    /**
     * Returns the names of every configurable field the {@code DispatcherService} needs: the
     * webhook secret, plus one entry per distinct {@code $config('name')} reference in the DSL's
     * {@code input} expression.
     */
    private List<String> getConfigFieldNames() {
        List<String> fieldNames = new ArrayList<>();
        fieldNames.add(WEBHOOK_SECRET_FIELD);
        webhookAuthConfig.ifPresent(config -> fieldNames.addAll(config.configFields()));
        return fieldNames;
    }

    private ObjectFieldNode buildConfigField(String fieldName) {
        return createObjectFieldNode(
                null,
                createToken(PRIVATE_KEYWORD),
                createEmptyNodeList(),
                createBuiltinSimpleNameReferenceNode(null, createIdentifierToken("string")),
                createIdentifierToken(fieldName),
                null,
                null,
                createToken(SEMICOLON_TOKEN));
    }

    private FunctionDefinitionNode buildInitFunction() {
        List<String> fieldNames = getConfigFieldNames();

        List<Node> params = new ArrayList<>();
        for (int i = 0; i < fieldNames.size(); i++) {
            if (i > 0) {
                params.add(createToken(COMMA_TOKEN));
            }
            params.add(createRequiredParameterNode(
                    createEmptyNodeList(),
                    createBuiltinSimpleNameReferenceNode(null, createIdentifierToken("string")),
                    createIdentifierToken(fieldNames.get(i))));
        }

        FunctionSignatureNode signature = createFunctionSignatureNode(
                createToken(OPEN_PAREN_TOKEN),
                createSeparatedNodeList(params),
                createToken(CLOSE_PAREN_TOKEN),
                null);

        List<StatementNode> assignStatements = new ArrayList<>();
        for (String fieldName : fieldNames) {
            assignStatements.add(NodeParser.parseStatement(
                    "self." + fieldName + " = " + fieldName + ";"));
        }

        FunctionBodyBlockNode body = createFunctionBodyBlockNode(
                createToken(OPEN_BRACE_TOKEN), null,
                createNodeList(assignStatements),
                createToken(CLOSE_BRACE_TOKEN), null);

        return createFunctionDefinitionNode(
                OBJECT_METHOD_DEFINITION, null,
                createEmptyNodeList(),
                createToken(FUNCTION_KEYWORD),
                createIdentifierToken("init"),
                createEmptyNodeList(),
                signature, body);
    }

    private FunctionDefinitionNode buildFunc(Generator gen) throws GeneratorException {
        return gen.generate();
    }
}
