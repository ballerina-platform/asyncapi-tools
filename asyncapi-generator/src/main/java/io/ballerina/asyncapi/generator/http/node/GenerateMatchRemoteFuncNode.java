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
import io.ballerina.asyncapi.generator.http.extractor.EventIdentifierExtractor;
import io.ballerina.asyncapi.generator.http.generator.DataTypesGenerator;
import io.ballerina.asyncapi.generator.http.model.EventIdentifierConfig;
import io.ballerina.asyncapi.generator.http.model.HttpServiceType;
import io.ballerina.compiler.syntax.tree.FunctionBodyBlockNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.MatchStatementNode;
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
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OBJECT_METHOD_DEFINITION;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_BRACE_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PRIVATE_KEYWORD;

/**
 * Generates the {@code private function matchRemoteFunc(...) returns error?} method node
 * for the {@code DispatcherService} class in {@code dispatcher_service.bal}.
 *
 * <p>For {@code "header"} identifier type, an additional {@code string eventIdentifier} parameter
 * is included and passed through to the injected match statement. Delegates to
 * {@link GenerateMatchStatementNode} for the match statement body.
 */
public class GenerateMatchRemoteFuncNode implements Generator {

    public static final String DISPATCHER_MATCH_REMOTE_FUNC = "matchRemoteFunc";

    private final List<HttpServiceType> serviceTypes;
    private final EventIdentifierConfig identifierConfig;
    private final String eventIdentifierPath;

    /**
     * Creates a generator for the {@code matchRemoteFunc} method.
     *
     * @param serviceTypes        the list of HTTP service type definitions
     * @param identifierConfig    the resolved event identifier type and path
     * @param eventIdentifierPath the expression matched against in the match statement
     */
    public GenerateMatchRemoteFuncNode(List<HttpServiceType> serviceTypes,
                                       EventIdentifierConfig identifierConfig,
                                       String eventIdentifierPath) {
        this.serviceTypes = serviceTypes;
        this.identifierConfig = identifierConfig;
        this.eventIdentifierPath = eventIdentifierPath;
    }

    @Override
    public FunctionDefinitionNode generate() throws GeneratorException {
        boolean isHeader = EventIdentifierExtractor.X_BALLERINA_EVENT_TYPE_HEADER.equals(identifierConfig.type());

        SeparatedNodeList<ParameterNode> params;
        if (isHeader) {
            params = createSeparatedNodeList(
                    createRequiredParameterNode(
                            createEmptyNodeList(),
                            createSimpleNameReferenceNode(createIdentifierToken(DataTypesGenerator.GENERIC_DATA_TYPE)),
                            createIdentifierToken(GenerateDispatcherServiceNode.CLONE_WITH_TYPE_VAR_NAME)),
                    createToken(COMMA_TOKEN),
                    createRequiredParameterNode(
                            createEmptyNodeList(),
                            createBuiltinSimpleNameReferenceNode(null, createIdentifierToken("string")),
                            createIdentifierToken("eventIdentifier")));
        } else {
            params = createSeparatedNodeList(
                    createRequiredParameterNode(
                            createEmptyNodeList(),
                            createSimpleNameReferenceNode(createIdentifierToken(DataTypesGenerator.GENERIC_DATA_TYPE)),
                            createIdentifierToken(GenerateDispatcherServiceNode.CLONE_WITH_TYPE_VAR_NAME)));
        }

        FunctionSignatureNode signature = createFunctionSignatureNode(
                createToken(OPEN_PAREN_TOKEN), params,
                createToken(CLOSE_PAREN_TOKEN), buildErrorReturnType());

        Generator matchGen = new GenerateMatchStatementNode(serviceTypes, eventIdentifierPath);
        MatchStatementNode matchStatement = matchGen.generate();

        FunctionBodyBlockNode body = createFunctionBodyBlockNode(
                createToken(OPEN_BRACE_TOKEN), null,
                createNodeList((StatementNode) matchStatement),
                createToken(CLOSE_BRACE_TOKEN), null);

        return createFunctionDefinitionNode(
                OBJECT_METHOD_DEFINITION, null,
                createNodeList(createToken(PRIVATE_KEYWORD)),
                createToken(FUNCTION_KEYWORD),
                createIdentifierToken(DISPATCHER_MATCH_REMOTE_FUNC),
                createEmptyNodeList(),
                signature, body);
    }
}
