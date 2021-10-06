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

import io.apicurio.datamodels.Library;
import io.apicurio.datamodels.asyncapi.models.AaiDocument;
import io.ballerina.asyncapi.codegenerator.configuration.BallerinaAsyncApiException;
import io.ballerina.asyncapi.codegenerator.configuration.Constants;
import io.ballerina.asyncapi.codegenerator.usecase.GenerateMatchStatement;
import io.ballerina.asyncapi.codegenerator.usecase.UseCase;
import io.ballerina.compiler.syntax.tree.*;
import io.ballerina.tools.text.TextDocuments;
import org.ballerinalang.formatter.core.Formatter;
import org.ballerinalang.formatter.core.FormatterException;

public class DispatcherController implements Controller {

    @Override
    public String generateBalCode(String spec, String balTemplate) throws BallerinaAsyncApiException {

        AaiDocument document = (AaiDocument) Library.readDocumentFromJSONString(spec);
        var textDocument = TextDocuments.from(balTemplate);
        var syntaxTree = SyntaxTree.from(textDocument);
        ModulePartNode oldRoot = syntaxTree.rootNode();
        var functionDefinitionNode = getResourceFuncNode(oldRoot);

        if (functionDefinitionNode == null) {
            throw new BallerinaAsyncApiException("Resource function '.', is not found in the dispatcher_service.bal");
        }

        UseCase generateMatchStatement = new GenerateMatchStatement(document);
        var matchStatementNode = generateMatchStatement.execute();
        var functionBodyBlockNode = (FunctionBodyBlockNode) functionDefinitionNode.functionBody();
        NodeList<StatementNode> oldStatements = functionBodyBlockNode.statements();
        NodeList<StatementNode> newStatements =
                oldStatements.add(oldStatements.size() - 1, (StatementNode) matchStatementNode);
        var functionBodyBlockNodeNew =
                functionBodyBlockNode.modify().withStatements(newStatements).apply();
        ModulePartNode newRoot = oldRoot.replace(functionBodyBlockNode, functionBodyBlockNodeNew);
        var modifiedTree = syntaxTree.replaceNode(oldRoot, newRoot);

        try {
            return Formatter.format(modifiedTree).toSourceCode();
        } catch (FormatterException e) {
            throw new BallerinaAsyncApiException("Could not format the generated code, " +
                    "may be a syntax issue in the generated code", e);
        }
    }

    private FunctionDefinitionNode getResourceFuncNode(ModulePartNode oldRoot) {
        for(ModuleMemberDeclarationNode node: oldRoot.members()) {
            if (node.kind() == SyntaxKind.CLASS_DEFINITION) {
                for(Node funcNode: ((ClassDefinitionNode) node).members()) {
                    if ((funcNode.kind() == SyntaxKind.RESOURCE_ACCESSOR_DEFINITION)
                            && ((FunctionDefinitionNode) funcNode).functionName().text().equals(
                            Constants.DISPATCHER_SERVICE_RESOURCE_FILTER_FUNCTION_NAME)) {
                        return (FunctionDefinitionNode) funcNode;
                    }
                }
            }
        }
        return null;
    }
}


