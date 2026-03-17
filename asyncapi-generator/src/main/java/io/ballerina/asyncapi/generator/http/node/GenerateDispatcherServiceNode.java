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
import io.ballerina.asyncapi.generator.http.Constants;
import io.ballerina.asyncapi.generator.http.model.EventIdentifierConfig;
import io.ballerina.asyncapi.generator.http.model.HttpServiceType;
import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.ObjectFieldNode;

import java.util.ArrayList;
import java.util.List;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createSeparatedNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createClassDefinitionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createImplicitNewExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMapTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMappingConstructorExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createObjectFieldNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createParenthesizedArgList;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createQualifiedNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTypeParameterNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTypeReferenceNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ASTERISK_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLASS_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.COLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.EQUAL_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.GT_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.LT_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.MAP_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.NEW_KEYWORD;
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

    private final List<HttpServiceType> serviceTypes;
    private final EventIdentifierConfig identifierConfig;

    /**
     * Creates a generator for the {@code DispatcherService} class.
     *
     * @param serviceTypes     the list of HTTP service type definitions
     * @param identifierConfig the resolved event identifier type and path
     */
    public GenerateDispatcherServiceNode(List<HttpServiceType> serviceTypes, EventIdentifierConfig identifierConfig) {
        this.serviceTypes = serviceTypes;
        this.identifierConfig = identifierConfig;
    }

    @Override
    public ClassDefinitionNode generate() throws GeneratorException {
        String eventIdentifierPath;
        if (Constants.X_BALLERINA_EVENT_TYPE_BODY.equals(identifierConfig.type())) {
            eventIdentifierPath = Constants.CLONE_WITH_TYPE_VAR_NAME + "." + identifierConfig.path();
        } else {
            eventIdentifierPath = "eventIdentifier";
        }

        List<Node> members = new ArrayList<>();
        members.add(buildHttpServiceTypeRef());
        members.add(buildServicesField());
        members.add(buildNativeHandlerField());
        members.add(buildFunc(new GenerateAddServiceRefFuncNode()));
        members.add(buildFunc(new GenerateRemoveServiceRefFuncNode()));
        members.add(buildFunc(new GeneratePostResourceFunctionNode(identifierConfig)));
        members.add(buildFunc(
                new GenerateMatchRemoteFuncNode(serviceTypes, identifierConfig, eventIdentifierPath)));
        members.add(buildFunc(new GenerateExecuteRemoteFuncNode()));

        return createClassDefinitionNode(
                null,
                null,
                createNodeList(createToken(SERVICE_KEYWORD)),
                createToken(CLASS_KEYWORD),
                createIdentifierToken(Constants.DISPATCHER_SERVICE_CLASS_NAME),
                createToken(OPEN_BRACE_TOKEN),
                createNodeList(members),
                createToken(CLOSE_BRACE_TOKEN),
                null);
    }

    private Node buildHttpServiceTypeRef() {
        return createTypeReferenceNode(
                createToken(ASTERISK_TOKEN),
                createQualifiedNameReferenceNode(
                        createIdentifierToken(Constants.HTTP_MODULE),
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
                                        createIdentifierToken(Constants.GENERIC_SERVICE_TYPE)),
                                createToken(GT_TOKEN))),
                createIdentifierToken(Constants.DISPATCHER_SERVICES_FIELD),
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
                        createIdentifierToken(Constants.NATIVE_HANDLER_MODULE_ALIAS),
                        createToken(COLON_TOKEN),
                        createIdentifierToken(Constants.NATIVE_HANDLER_TYPE)),
                createIdentifierToken(Constants.DISPATCHER_NATIVE_HANDLER_FIELD),
                createToken(EQUAL_TOKEN),
                createImplicitNewExpressionNode(
                        createToken(NEW_KEYWORD),
                        createParenthesizedArgList(
                                createToken(OPEN_PAREN_TOKEN),
                                createSeparatedNodeList(),
                                createToken(CLOSE_PAREN_TOKEN))),
                createToken(SEMICOLON_TOKEN));
    }

    private FunctionDefinitionNode buildFunc(Generator gen) throws GeneratorException {
        return gen.generate();
    }
}
