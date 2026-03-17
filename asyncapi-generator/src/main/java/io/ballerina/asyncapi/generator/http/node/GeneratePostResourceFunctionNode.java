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
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionBodyBlockNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionDefinitionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createFunctionSignatureNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createQualifiedNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRequiredParameterNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.COLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.COMMA_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.DOT_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FUNCTION_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RESOURCE_ACCESSOR_DEFINITION;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RESOURCE_KEYWORD;

/**
 * Generates the {@code resource function post .(http:Caller caller, http:Request request) returns error?}
 * method node for the {@code DispatcherService} class in {@code dispatcher_service.bal}.
 *
 * <p>When the identifier type is {@code "header"}, an additional
 * {@code string eventIdentifier = check request.getHeader(...);} statement is injected and
 * the {@code matchRemoteFunc} call passes {@code eventIdentifier} as a second argument.
 */
public class GeneratePostResourceFunctionNode implements Generator {

    private final EventIdentifierConfig identifierConfig;

    /**
     * Creates a generator for the post resource function.
     *
     * @param identifierConfig the resolved event identifier type and path
     */
    public GeneratePostResourceFunctionNode(EventIdentifierConfig identifierConfig) {
        this.identifierConfig = identifierConfig;
    }

    @Override
    public FunctionDefinitionNode generate() throws GeneratorException {
        FunctionSignatureNode signature = createFunctionSignatureNode(
                createToken(OPEN_PAREN_TOKEN),
                createSeparatedNodeList(
                        createRequiredParameterNode(
                                createEmptyNodeList(),
                                createQualifiedNameReferenceNode(
                                        createIdentifierToken(Constants.HTTP_MODULE),
                                        createToken(COLON_TOKEN),
                                        createIdentifierToken("Caller")),
                                createIdentifierToken("caller")),
                        createToken(COMMA_TOKEN),
                        createRequiredParameterNode(
                                createEmptyNodeList(),
                                createQualifiedNameReferenceNode(
                                        createIdentifierToken(Constants.HTTP_MODULE),
                                        createToken(COLON_TOKEN),
                                        createIdentifierToken("Request")),
                                createIdentifierToken("request"))),
                createToken(CLOSE_PAREN_TOKEN),
                buildErrorReturnType());

        boolean isHeader = Constants.X_BALLERINA_EVENT_TYPE_HEADER.equals(identifierConfig.type());

        List<StatementNode> statements = new ArrayList<>();
        statements.add(NodeParser.parseStatement("json payload = check request.getJsonPayload();"));
        if (isHeader) {
            statements.add(NodeParser.parseStatement(
                    "string eventIdentifier = check request.getHeader(\"" + identifierConfig.path() + "\");"));
        }
        statements.add(NodeParser.parseStatement(
                Constants.GENERIC_DATA_TYPE + " " + Constants.CLONE_WITH_TYPE_VAR_NAME
                + " = check payload.cloneWithType(" + Constants.GENERIC_DATA_TYPE + ");"));
        if (isHeader) {
            statements.add(NodeParser.parseStatement(
                    "check self." + Constants.DISPATCHER_MATCH_REMOTE_FUNC
                    + "(" + Constants.CLONE_WITH_TYPE_VAR_NAME + ", eventIdentifier);"));
        } else {
            statements.add(NodeParser.parseStatement(
                    "check self." + Constants.DISPATCHER_MATCH_REMOTE_FUNC
                    + "(" + Constants.CLONE_WITH_TYPE_VAR_NAME + ");"));
        }
        statements.add(NodeParser.parseStatement("check caller->respond(http:STATUS_OK);"));

        FunctionBodyBlockNode body = createFunctionBodyBlockNode(
                createToken(OPEN_BRACE_TOKEN), null, createNodeList(statements),
                createToken(CLOSE_BRACE_TOKEN), null);

        return createFunctionDefinitionNode(
                RESOURCE_ACCESSOR_DEFINITION, null,
                createNodeList(createToken(RESOURCE_KEYWORD)),
                createToken(FUNCTION_KEYWORD),
                createIdentifierToken("post"),
                createNodeList(createToken(DOT_TOKEN)),
                signature, body);
    }
}
