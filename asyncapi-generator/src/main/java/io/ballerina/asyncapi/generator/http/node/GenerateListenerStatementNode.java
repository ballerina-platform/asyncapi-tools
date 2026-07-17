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
import io.ballerina.asyncapi.generator.http.utils.CodegenUtils;
import io.ballerina.compiler.syntax.tree.AbstractNodeFactory;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.IfElseStatementNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.compiler.syntax.tree.ReturnStatementNode;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;

import java.util.List;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyMinutiaeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createLiteralValueToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBasicLiteralNode;

/**
 * Generates the {@code if-else} statement body for {@code getServiceTypeStr} in
 * {@code listener.bal}, mapping each attached service instance to its type name string.
 */
public class GenerateListenerStatementNode implements Generator {

    private final List<String> serviceTypeNames;

    /**
     * Creates a generator for the {@code getServiceTypeStr} function body.
     *
     * @param serviceTypeNames the raw service type names to generate branches for
     */
    public GenerateListenerStatementNode(List<String> serviceTypeNames) {
        this.serviceTypeNames = serviceTypeNames;
    }

    @Override
    public StatementNode generate() throws GeneratorException {
        if (serviceTypeNames.isEmpty()) {
            throw new GeneratorException(
                    "No service types found, probably there are no channels defined in the async api spec");
        }
        return buildIfElseChain(serviceTypeNames);
    }

    private IfElseStatementNode buildIfElseChain(List<String> remaining) {
        String serviceType = remaining.get(0);
        ExpressionNode serviceRefNode = NodeFactory.createSimpleNameReferenceNode(
                AbstractNodeFactory.createIdentifierToken("serviceRef"));
        ExpressionNode condition = NodeFactory.createTypeTestExpressionNode(serviceRefNode,
                createToken(SyntaxKind.IS_KEYWORD),
                NodeFactory.createSimpleNameReferenceNode(AbstractNodeFactory.createIdentifierToken(
                        CodegenUtils.getServiceTypeNameByServiceName(serviceType))));
        ReturnStatementNode returnNode = buildReturnStatement(serviceType);
        return NodeFactory.createIfElseStatementNode(
                createToken(SyntaxKind.IF_KEYWORD), condition,
                NodeFactory.createBlockStatementNode(
                        createToken(SyntaxKind.OPEN_BRACE_TOKEN),
                        createNodeList(returnNode),
                        createToken(SyntaxKind.CLOSE_BRACE_TOKEN)),
                buildElseNode(remaining));
    }

    private ReturnStatementNode buildReturnStatement(String serviceType) {
        return NodeFactory.createReturnStatementNode(
                createToken(SyntaxKind.RETURN_KEYWORD),
                createBasicLiteralNode(SyntaxKind.STRING_LITERAL,
                        createLiteralValueToken(SyntaxKind.STRING_LITERAL_TOKEN,
                                '"' + CodegenUtils.getServiceTypeNameByServiceName(serviceType) + '"',
                                createEmptyMinutiaeList(), createEmptyMinutiaeList())),
                createToken(SyntaxKind.SEMICOLON_TOKEN));
    }

    /**
     * Builds the {@code else} branch: either a nested {@code if is <NextType>} check for the
     * remaining candidates, or -- once every known type has been explicitly tested and none
     * matched -- a {@code panic}, so an unrecognized {@code serviceRef} fails loudly instead of
     * being silently mislabeled as whichever type happened to be last in the list.
     *
     * @param list the service types not yet tested by an enclosing {@code if}
     */
    private Node buildElseNode(List<String> list) {
        List<String> remaining = list.subList(1, list.size());
        if (remaining.isEmpty()) {
            return NodeFactory.createElseBlockNode(createToken(SyntaxKind.ELSE_KEYWORD),
                    NodeFactory.createBlockStatementNode(
                            createToken(SyntaxKind.OPEN_BRACE_TOKEN),
                            createNodeList(buildPanicStatement()),
                            createToken(SyntaxKind.CLOSE_BRACE_TOKEN)));
        }
        return NodeFactory.createElseBlockNode(createToken(SyntaxKind.ELSE_KEYWORD),
                buildIfElseChain(remaining));
    }

    private StatementNode buildPanicStatement() {
        return NodeParser.parseStatement(
                "panic error(\"Unrecognized service type attached to the listener\");");
    }
}
