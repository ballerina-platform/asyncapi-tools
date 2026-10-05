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
import io.ballerina.compiler.syntax.tree.FunctionBodyBlockNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.compiler.syntax.tree.OptionalTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.ReturnTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.StatementNode;

import java.util.ArrayList;
import java.util.List;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createSeparatedNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBuiltinSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionBodyBlockNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionDefinitionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionSignatureNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createOptionalTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRequiredParameterNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createReturnTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.COMMA_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ERROR_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FUNCTION_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ISOLATED_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OBJECT_METHOD_DEFINITION;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.QUESTION_MARK_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RETURNS_KEYWORD;

/**
 * Generates the {@code isolated function addServiceRef(...) returns error?} method node
 * for the {@code DispatcherService} class in {@code dispatcher_service.bal}.
 */
public class GenerateAddServiceRefFuncNode implements Generator {

    public static final String ADD_SERVICE_REF_FUNC = "addServiceRef";

    @Override
    public FunctionDefinitionNode generate() throws GeneratorException {
        FunctionSignatureNode signature = createFunctionSignatureNode(
                createToken(OPEN_PAREN_TOKEN),
                createSeparatedNodeList(
                        createRequiredParameterNode(
                                createEmptyNodeList(),
                                createBuiltinSimpleNameReferenceNode(null, createIdentifierToken("string")),
                                createIdentifierToken("serviceType")),
                        createToken(COMMA_TOKEN),
                        createRequiredParameterNode(
                                createEmptyNodeList(),
                                createSimpleNameReferenceNode(
                                        createIdentifierToken(ServiceTypesGenerator.GENERIC_SERVICE_TYPE)),
                                createIdentifierToken("genericService"))),
                createToken(CLOSE_PAREN_TOKEN),
                buildErrorReturnType());

        List<StatementNode> statements = new ArrayList<>();
        statements.add(NodeParser.parseStatement(String.format(
                "if (self.%s.hasKey(serviceType)) {"
                        + " return error(string `Service of type ${serviceType} has already been attached`); }",
                GenerateDispatcherServiceNode.DISPATCHER_SERVICES_FIELD)));
        statements.add(NodeParser.parseStatement(String.format(
                "self.%s[serviceType] = genericService;",
                GenerateDispatcherServiceNode.DISPATCHER_SERVICES_FIELD)));

        FunctionBodyBlockNode body = createFunctionBodyBlockNode(
                createToken(OPEN_BRACE_TOKEN), null, createNodeList(statements),
                createToken(CLOSE_BRACE_TOKEN), null);

        return createFunctionDefinitionNode(
                OBJECT_METHOD_DEFINITION, null,
                createNodeList(createToken(ISOLATED_KEYWORD)),
                createToken(FUNCTION_KEYWORD),
                createIdentifierToken(ADD_SERVICE_REF_FUNC),
                createEmptyNodeList(),
                signature, body);
    }

    static ReturnTypeDescriptorNode buildErrorReturnType() {
        OptionalTypeDescriptorNode optionalError = createOptionalTypeDescriptorNode(
                createToken(ERROR_KEYWORD), createToken(QUESTION_MARK_TOKEN));
        return createReturnTypeDescriptorNode(createToken(RETURNS_KEYWORD), createEmptyNodeList(), optionalError);
    }
}
