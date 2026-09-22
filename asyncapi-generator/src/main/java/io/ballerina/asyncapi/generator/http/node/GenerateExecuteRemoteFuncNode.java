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
import io.ballerina.asyncapi.generator.http.generator.DataTypesGenerator;
import io.ballerina.asyncapi.generator.http.generator.ServiceTypesGenerator;
import io.ballerina.compiler.syntax.tree.FunctionBodyBlockNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.compiler.syntax.tree.StatementNode;

import java.util.ArrayList;
import java.util.List;

import static io.ballerina.asyncapi.generator.http.node.GenerateAddServiceRefFuncNode.buildErrorReturnType;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createSeparatedNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBuiltinSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionBodyBlockNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionDefinitionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionSignatureNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRequiredParameterNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.COMMA_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FUNCTION_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ISOLATED_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OBJECT_METHOD_DEFINITION;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PRIVATE_KEYWORD;

/**
 * Generates the {@code private function executeRemoteFunc(...) returns error?} method node
 * for the {@code DispatcherService} class in {@code dispatcher_service.bal}.
 */
public class GenerateExecuteRemoteFuncNode implements Generator {

    public static final String EXECUTE_REMOTE_FUNC_NAME = "executeRemoteFunc";
    private static final String EXECUTE_REMOTE_FUNC_PARAM_EVENT = "genericEvent";
    private static final String EXECUTE_REMOTE_FUNC_PARAM_EVENT_NAME = "eventName";
    private static final String EXECUTE_REMOTE_FUNC_PARAM_SERVICE_TYPE = "serviceTypeStr";
    private static final String EXECUTE_REMOTE_FUNC_PARAM_EVENT_FUNC = "eventFunction";
    private static final String EXECUTE_REMOTE_FUNC_LOCAL_SERVICE = "genericService";

    /**
     * Creates a generator for the {@code executeRemoteFunc} method.
     */
    public GenerateExecuteRemoteFuncNode() {
    }

    @Override
    public FunctionDefinitionNode generate() throws GeneratorException {
        FunctionSignatureNode signature = createFunctionSignatureNode(
                createToken(OPEN_PAREN_TOKEN),
                createSeparatedNodeList(
                        createRequiredParameterNode(
                                createEmptyNodeList(),
                                createSimpleNameReferenceNode(
                                        createIdentifierToken(DataTypesGenerator.GENERIC_DATA_TYPE)),
                                createIdentifierToken(EXECUTE_REMOTE_FUNC_PARAM_EVENT)),
                        createToken(COMMA_TOKEN),
                        createRequiredParameterNode(
                                createEmptyNodeList(),
                                createBuiltinSimpleNameReferenceNode(null, createIdentifierToken("string")),
                                createIdentifierToken(EXECUTE_REMOTE_FUNC_PARAM_EVENT_NAME)),
                        createToken(COMMA_TOKEN),
                        createRequiredParameterNode(
                                createEmptyNodeList(),
                                createBuiltinSimpleNameReferenceNode(null, createIdentifierToken("string")),
                                createIdentifierToken(EXECUTE_REMOTE_FUNC_PARAM_SERVICE_TYPE)),
                        createToken(COMMA_TOKEN),
                        createRequiredParameterNode(
                                createEmptyNodeList(),
                                createBuiltinSimpleNameReferenceNode(null, createIdentifierToken("string")),
                                createIdentifierToken(EXECUTE_REMOTE_FUNC_PARAM_EVENT_FUNC))),
                createToken(CLOSE_PAREN_TOKEN),
                buildErrorReturnType());

        List<StatementNode> statements = new ArrayList<>();
        statements.add(NodeParser.parseStatement(String.format(
                "%s? %s = self.%s[%s];",
                ServiceTypesGenerator.GENERIC_SERVICE_TYPE,
                EXECUTE_REMOTE_FUNC_LOCAL_SERVICE,
                GenerateDispatcherServiceNode.DISPATCHER_SERVICES_FIELD,
                EXECUTE_REMOTE_FUNC_PARAM_SERVICE_TYPE)));
        statements.add(NodeParser.parseStatement(String.format(
                "if %s is %s { check self.%s.invokeRemoteFunction(%s, %s, %s, %s); }",
                EXECUTE_REMOTE_FUNC_LOCAL_SERVICE,
                ServiceTypesGenerator.GENERIC_SERVICE_TYPE,
                GenerateDispatcherServiceNode.DISPATCHER_NATIVE_HANDLER_FIELD,
                EXECUTE_REMOTE_FUNC_PARAM_EVENT,
                EXECUTE_REMOTE_FUNC_PARAM_EVENT_NAME,
                EXECUTE_REMOTE_FUNC_PARAM_EVENT_FUNC,
                EXECUTE_REMOTE_FUNC_LOCAL_SERVICE)));

        FunctionBodyBlockNode body = createFunctionBodyBlockNode(
                createToken(OPEN_BRACE_TOKEN), null, createNodeList(statements),
                createToken(CLOSE_BRACE_TOKEN), null);

        return createFunctionDefinitionNode(
                OBJECT_METHOD_DEFINITION, null,
                createNodeList(createToken(PRIVATE_KEYWORD), createToken(ISOLATED_KEYWORD)),
                createToken(FUNCTION_KEYWORD),
                createIdentifierToken(EXECUTE_REMOTE_FUNC_NAME),
                createEmptyNodeList(),
                signature, body);
    }
}
