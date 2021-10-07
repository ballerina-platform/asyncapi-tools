/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
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

package io.ballerina.asyncapi.codegenerator.controller;

import io.ballerina.asyncapi.codegenerator.configuration.BallerinaAsyncApiException;
import io.ballerina.asyncapi.codegenerator.configuration.Constants;
import io.ballerina.asyncapi.codegenerator.entity.ServiceType;
import io.ballerina.asyncapi.codegenerator.usecase.GenerateListenerStatementNode;
import io.ballerina.asyncapi.codegenerator.usecase.GenerateUseCase;
import io.ballerina.compiler.syntax.tree.*;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocuments;
import org.ballerinalang.formatter.core.Formatter;
import org.ballerinalang.formatter.core.FormatterException;

import java.util.List;
import java.util.stream.Collectors;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;

public class ListenerController implements BalController {
    private final List<ServiceType> serviceTypes;

    public ListenerController(List<ServiceType> serviceTypes) {
        this.serviceTypes = serviceTypes;
    }

    @Override
    public String generateBalCode(String balTemplate) throws BallerinaAsyncApiException {
        TextDocument textDocument = TextDocuments.from(balTemplate);
        SyntaxTree syntaxTree = SyntaxTree.from(textDocument);
        ModulePartNode oldRoot = syntaxTree.rootNode();
        FunctionDefinitionNode functionDefinitionNode = getServiceTypeStrFuncNode(oldRoot);

        if (functionDefinitionNode == null) {
            throw new BallerinaAsyncApiException("Function 'getServiceTypeStr', is not found in the listener.bal");
        }

        FunctionBodyBlockNode functionBodyBlockNode = (FunctionBodyBlockNode) functionDefinitionNode.functionBody();
        List<String> serviceTypeNames = serviceTypes.stream()
                .map(ServiceType::getServiceTypeName).collect(Collectors.toList());
        GenerateUseCase genIfElseNode = new GenerateListenerStatementNode(serviceTypeNames);
        StatementNode ifElseStatementNode = genIfElseNode.generate();
        NodeList<StatementNode> statements = createNodeList(ifElseStatementNode);

        FunctionBodyBlockNode functionBodyBlockNodeNew = functionBodyBlockNode
                .modify().withStatements(statements).apply();
        SyntaxTree modifiedTree = syntaxTree.replaceNode(functionBodyBlockNode, functionBodyBlockNodeNew);

        try {
            return Formatter.format(modifiedTree).toSourceCode();
        } catch (FormatterException e) {
            throw new BallerinaAsyncApiException("Could not format the generated code, " +
                    "may be a syntax issue in the generated code", e);
        }
    }

    private FunctionDefinitionNode getServiceTypeStrFuncNode(ModulePartNode oldRoot) {
        for(ModuleMemberDeclarationNode node: oldRoot.members()) {
            if (node.kind() == SyntaxKind.CLASS_DEFINITION) {
                for(Node funcNode: ((ClassDefinitionNode) node).members()) {
                    if ((funcNode.kind() == SyntaxKind.OBJECT_METHOD_DEFINITION)
                            && ((FunctionDefinitionNode) funcNode).functionName().text().equals(
                                    Constants.LISTENER_SERVICE_TYPE_FILTER_FUNCTION_NAME)) {
                        return (FunctionDefinitionNode) funcNode;
                    }
                }
            }
        }
        return null;
    }
}
