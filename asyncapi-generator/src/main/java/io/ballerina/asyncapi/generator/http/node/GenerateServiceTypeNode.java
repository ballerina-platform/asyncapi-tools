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
import io.ballerina.asyncapi.generator.http.model.HttpRemoteFunction;
import io.ballerina.asyncapi.generator.http.utils.CodegenUtils;
import io.ballerina.compiler.syntax.tree.AbstractNodeFactory;
import io.ballerina.compiler.syntax.tree.BuiltinSimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.MethodDeclarationNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.ObjectTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.OptionalTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.ReturnTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;

import java.util.ArrayList;
import java.util.List;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createSeparatedNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBuiltinSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionSignatureNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMethodDeclarationNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createOptionalTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRequiredParameterNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createReturnTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTypeDefinitionNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ERROR_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OBJECT_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PUBLIC_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.QUESTION_MARK_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.REMOTE_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RETURNS_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SEMICOLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SERVICE_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.TYPE_KEYWORD;

/**
 * Generates a {@code public type <ServiceName> service object { ... };} definition node
 * for {@code service_types.bal}.
 */
public class GenerateServiceTypeNode implements Generator {

    private final String serviceTypeName;
    private final List<HttpRemoteFunction> remoteFunctions;

    /**
     * Creates a generator for a single service type.
     *
     * @param serviceTypeName  the raw service type name (will be Pascal-cased and suffixed)
     * @param remoteFunctions  the remote functions to declare in the service type
     */
    public GenerateServiceTypeNode(String serviceTypeName, List<HttpRemoteFunction> remoteFunctions) {
        this.serviceTypeName = serviceTypeName;
        this.remoteFunctions = remoteFunctions;
    }

    @Override
    public TypeDefinitionNode generate() throws GeneratorException {
        if (remoteFunctions.isEmpty()) {
            throw new GeneratorException(String.format(
                    "Remote functions list is empty in the service type %s", serviceTypeName));
        }

        OptionalTypeDescriptorNode returnType = createOptionalTypeDescriptorNode(
                createToken(ERROR_KEYWORD), createToken(QUESTION_MARK_TOKEN));
        ReturnTypeDescriptorNode returnTypeDescriptor = createReturnTypeDescriptorNode(
                createToken(RETURNS_KEYWORD), createEmptyNodeList(), returnType);

        List<Node> methods = new ArrayList<>();
        for (HttpRemoteFunction fn : remoteFunctions) {
            String eventType = CodegenUtils.getValidName(
                    CodegenUtils.escapeIdentifier(fn.eventType().trim()), true);
            BuiltinSimpleNameReferenceNode typeNode = createBuiltinSimpleNameReferenceNode(
                    null, createIdentifierToken(eventType));
            List<Node> params = new ArrayList<>();
            params.add(createRequiredParameterNode(createEmptyNodeList(), typeNode, createIdentifierToken("payload")));
            MethodDeclarationNode method = createMethodDeclarationNode(
                    SyntaxKind.METHOD_DECLARATION, null,
                    createNodeList(createToken(REMOTE_KEYWORD)),
                    createToken(SyntaxKind.FUNCTION_KEYWORD),
                    createIdentifierToken(CodegenUtils.getFunctionNameByEventName(fn.functionName())),
                    createEmptyNodeList(),
                    createFunctionSignatureNode(
                            createToken(OPEN_PAREN_TOKEN), createSeparatedNodeList(params),
                            createToken(CLOSE_PAREN_TOKEN), returnTypeDescriptor),
                    createToken(SEMICOLON_TOKEN));
            methods.add(method);
        }

        IdentifierToken typeNameToken = AbstractNodeFactory.createIdentifierToken(
                CodegenUtils.getServiceTypeNameByServiceName(serviceTypeName));
        ObjectTypeDescriptorNode objectType = NodeFactory.createObjectTypeDescriptorNode(
                createNodeList(createToken(SERVICE_KEYWORD)),
                createToken(OBJECT_KEYWORD), createToken(OPEN_BRACE_TOKEN),
                createNodeList(methods), createToken(CLOSE_BRACE_TOKEN));
        return createTypeDefinitionNode(null, createToken(PUBLIC_KEYWORD), createToken(TYPE_KEYWORD),
                typeNameToken, objectType, createToken(SEMICOLON_TOKEN));
    }
}
