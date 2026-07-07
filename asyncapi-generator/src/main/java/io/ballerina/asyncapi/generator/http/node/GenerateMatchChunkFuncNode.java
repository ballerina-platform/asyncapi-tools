/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com)
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.asyncapi.generator.http.node;

import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.http.generator.DataTypesGenerator;
import io.ballerina.asyncapi.generator.http.model.HttpServiceType;
import io.ballerina.compiler.syntax.tree.FunctionBodyBlockNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.MatchClauseNode;
import io.ballerina.compiler.syntax.tree.MatchStatementNode;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
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
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMatchStatementNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createRequiredParameterNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.COMMA_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FUNCTION_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.MATCH_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OBJECT_METHOD_DEFINITION;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PRIVATE_KEYWORD;

/**
 * Generates a private function containing a match statement
 * for all remote functions in one {@link HttpServiceType} (channel group).
 *
 * <p>The generated function is named
 * {@code matchRemoteFuncFor{ServiceTypeName}}.
 */
public class GenerateMatchChunkFuncNode implements Generator {

    public static final String CHUNK_FUNC_PREFIX = "matchRemoteFuncFor";

    private final HttpServiceType serviceType;
    private final String eventIdentifierPath;
    private final boolean isHeader;
    private final String serviceName;

    /**
     * Creates a generator for one channel-group match function.
     *
     * @param serviceType         the service type (channel group) whose
     *                            remote functions go into this function
     * @param eventIdentifierPath the expression matched against
     * @param isHeader            whether the identifier type is "header"
     * @param serviceName         a label identifying the generated package, embedded into the
     *                            {@code MATCH_LEVEL_2_*} diagnostic trace log message
     */
    public GenerateMatchChunkFuncNode(HttpServiceType serviceType,
                                      String eventIdentifierPath,
                                      boolean isHeader,
                                      String serviceName) {
        this.serviceType = serviceType;
        this.eventIdentifierPath = eventIdentifierPath;
        this.isHeader = isHeader;
        this.serviceName = serviceName;
    }

    /**
     * Returns the function name for a given service type name.
     * Public so callers can build call expressions with the
     * correct name.
     *
     * @param serviceTypeName the service type name
     * @return the chunk function name
     */
    public static String chunkFuncName(String serviceTypeName) {
        return CHUNK_FUNC_PREFIX + serviceTypeName;
    }

    @Override
    public FunctionDefinitionNode generate() throws GeneratorException {
        SeparatedNodeList<ParameterNode> params;
        if (isHeader) {
            params = createSeparatedNodeList(
                createRequiredParameterNode(
                    createEmptyNodeList(),
                    createSimpleNameReferenceNode(createIdentifierToken(
                        DataTypesGenerator.GENERIC_DATA_TYPE)),
                    createIdentifierToken(
                        GenerateDispatcherServiceNode.CLONE_WITH_TYPE_VAR_NAME)),
                createToken(COMMA_TOKEN),
                createRequiredParameterNode(
                    createEmptyNodeList(),
                    createBuiltinSimpleNameReferenceNode(
                        null, createIdentifierToken("string")),
                    createIdentifierToken("eventIdentifier")));
        } else {
            params = createSeparatedNodeList(
                createRequiredParameterNode(
                    createEmptyNodeList(),
                    createSimpleNameReferenceNode(createIdentifierToken(
                        DataTypesGenerator.GENERIC_DATA_TYPE)),
                    createIdentifierToken(
                        GenerateDispatcherServiceNode.CLONE_WITH_TYPE_VAR_NAME)));
        }

        FunctionSignatureNode signature = createFunctionSignatureNode(
            createToken(OPEN_PAREN_TOKEN), params,
            createToken(CLOSE_PAREN_TOKEN), buildErrorReturnType());

        // Build match clauses for all remote functions in this service type
        List<MatchClauseNode> clauses = new ArrayList<>();
        GenerateMatchStatementNode matchGen =
            new GenerateMatchStatementNode(
                List.of(serviceType), eventIdentifierPath, serviceName);
        matchGen.generate().matchClauses().forEach(clauses::add);

        MatchStatementNode matchStatement = createMatchStatementNode(
            createToken(MATCH_KEYWORD),
            createSimpleNameReferenceNode(
                createIdentifierToken(eventIdentifierPath)),
            createToken(OPEN_BRACE_TOKEN),
            createNodeList(clauses),
            createToken(CLOSE_BRACE_TOKEN), null);

        FunctionBodyBlockNode body = createFunctionBodyBlockNode(
            createToken(OPEN_BRACE_TOKEN), null,
            createNodeList((StatementNode) matchStatement),
            createToken(CLOSE_BRACE_TOKEN), null);

        return createFunctionDefinitionNode(
            OBJECT_METHOD_DEFINITION, null,
            createNodeList(createToken(PRIVATE_KEYWORD)),
            createToken(FUNCTION_KEYWORD),
            createIdentifierToken(chunkFuncName(
                serviceType.serviceTypeName())),
            createEmptyNodeList(),
            signature, body);
    }
}
