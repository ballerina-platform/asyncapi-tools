/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.asyncapi.codegenerator.usecase;

import io.ballerina.asyncapi.codegenerator.configuration.BallerinaAsyncApiException;
import io.ballerina.asyncapi.codegenerator.usecase.utils.CodegenUtils;
import io.ballerina.compiler.syntax.tree.AbstractNodeFactory;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.IfElseStatementNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeFactory;
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
 * Generate the If else statement node for the listener.bal.
 */
public class GenerateListenerStatementNode implements Generator {
    private final List<String> serviceTypes;
    private final CodegenUtils codegenUtils = new CodegenUtils();

    public GenerateListenerStatementNode(List<String> serviceTypes) {
        this.serviceTypes = serviceTypes;
    }

    @Override
    public StatementNode generate() throws BallerinaAsyncApiException {
        if (serviceTypes.isEmpty()) {
            throw new BallerinaAsyncApiException("No service types found, " +
                    "probably there are no channels defined in the async api spec");
        } else if (serviceTypes.size() == 1) {
            return getReturnStatementNode(serviceTypes.get(0));
        }
        return createIfElseNode(serviceTypes);
    }

    public IfElseStatementNode createIfElseNode(List<String> remainingList) {
        String serviceType = remainingList.get(0);
        ExpressionNode serviceTypeNode = NodeFactory.createSimpleNameReferenceNode(
                AbstractNodeFactory.createIdentifierToken("serviceRef"));
        ExpressionNode condition = NodeFactory.createTypeTestExpressionNode(
                serviceTypeNode,
                createToken(SyntaxKind.IS_KEYWORD),
                NodeFactory.createSimpleNameReferenceNode(AbstractNodeFactory.
                        createIdentifierToken(codegenUtils.getServiceTypeNameByServiceName(serviceType)))
        );
        ReturnStatementNode returnStatementNode = getReturnStatementNode(serviceType);
        return NodeFactory.createIfElseStatementNode(
                createToken(SyntaxKind.IF_KEYWORD),
                condition,
                NodeFactory.createBlockStatementNode(
                        createToken(SyntaxKind.OPEN_BRACE_TOKEN),
                        createNodeList(returnStatementNode),
                        createToken(SyntaxKind.CLOSE_BRACE_TOKEN)
                ),
                getElseNode(remainingList)
        );
    }

    private ReturnStatementNode getReturnStatementNode(String serviceType) {
        return NodeFactory.createReturnStatementNode(
                createToken(SyntaxKind.RETURN_KEYWORD),
                createBasicLiteralNode(
                        SyntaxKind.STRING_LITERAL,
                        createLiteralValueToken(
                                SyntaxKind.STRING_LITERAL_TOKEN, '"' +
                                        codegenUtils.getServiceTypeNameByServiceName(serviceType) + '"',
                                createEmptyMinutiaeList(), createEmptyMinutiaeList())),
                createToken(SyntaxKind.SEMICOLON_TOKEN));
    }

    private Node getElseNode(List<String> list) {
        if (list.size() == 1) {
            return null;
        } else if (list.size() == 2) {
            return NodeFactory.createElseBlockNode(createToken(SyntaxKind.ELSE_KEYWORD),
                    NodeFactory.createBlockStatementNode(
                            createToken(SyntaxKind.OPEN_BRACE_TOKEN),
                            createNodeList(getReturnStatementNode(list.get(1))),
                            createToken(SyntaxKind.CLOSE_BRACE_TOKEN)
                    ));
        } else {
            return NodeFactory.createElseBlockNode(createToken(SyntaxKind.ELSE_KEYWORD),
                    createIfElseNode(list.subList(1, list.size())));
        }
    }
}
