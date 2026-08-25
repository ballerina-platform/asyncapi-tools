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
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.StatementNode;

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
 * Generates a private function containing the match statement(s)
 * for all remote functions in one {@link HttpServiceType} (channel group).
 *
 * <p>The generated function is named
 * {@code matchRemoteFuncFor{ServiceTypeName}}.
 */
public class GenerateMatchChunkFuncNode implements Generator {

    public static final String CHUNK_FUNC_PREFIX = "matchRemoteFuncFor";

    private final HttpServiceType serviceType;
    private final String eventIdentifierPath;
    private final String eventTypePath;
    private final boolean isHeader;

    /**
     * Creates a generator for one channel-group match function.
     *
     * @param serviceType         the service type (channel group) whose
     *                            remote functions go into this function
     * @param eventIdentifierPath the expression matched against for events with an enumerable
     *                            action field
     * @param eventTypePath       the expression matched against for events whose action field has
     *                            no enumerable set of values (composite identifier type only), or
     *                            {@code null} for "header" and "body" identifier types
     * @param isHeader            whether the identifier type is "header" or "composite" (i.e. not "body")
     */
    public GenerateMatchChunkFuncNode(HttpServiceType serviceType,
                                      String eventIdentifierPath,
                                      String eventTypePath,
                                      boolean isHeader) {
        this.serviceType = serviceType;
        this.eventIdentifierPath = eventIdentifierPath;
        this.eventTypePath = eventTypePath;
        this.isHeader = isHeader;
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
        if (isHeader && eventTypePath != null) {
            // Composite identifier type: the function may need to match some remote functions
            // against the composite identifier and others against the bare event type, so both
            // variables must be in scope.
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
                    createIdentifierToken(eventIdentifierPath)),
                createToken(COMMA_TOKEN),
                createRequiredParameterNode(
                    createEmptyNodeList(),
                    createBuiltinSimpleNameReferenceNode(
                        null, createIdentifierToken("string")),
                    createIdentifierToken(eventTypePath)));
        } else if (isHeader) {
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

        // Build the match statement(s) for all remote functions in this service type -- one
        // statement per distinct match subject (composite identifier vs. bare event type).
        GenerateMatchStatementNode matchGen =
            new GenerateMatchStatementNode(
                List.of(serviceType), eventIdentifierPath, eventTypePath);
        List<StatementNode> statements = matchGen.generate();

        FunctionBodyBlockNode body = createFunctionBodyBlockNode(
            createToken(OPEN_BRACE_TOKEN), null,
            createNodeList(statements),
            createToken(CLOSE_BRACE_TOKEN), null);

        return createFunctionDefinitionNode(
            OBJECT_METHOD_DEFINITION, null,
            createNodeList(createToken(PRIVATE_KEYWORD), createToken(ISOLATED_KEYWORD)),
            createToken(FUNCTION_KEYWORD),
            createIdentifierToken(chunkFuncName(
                serviceType.serviceTypeName())),
            createEmptyNodeList(),
            signature, body);
    }
}
