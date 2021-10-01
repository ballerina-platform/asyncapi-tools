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
import io.apicurio.datamodels.asyncapi.v2.models.Aai20Document;
import io.ballerina.asyncapi.codegenerator.configuration.BallerinaAsyncApiException;
import io.ballerina.asyncapi.codegenerator.configuration.Constants;
import io.ballerina.asyncapi.codegenerator.usecase.ExtractServiceTypesFromSpec;
import io.ballerina.asyncapi.codegenerator.usecase.GenerateListenerStatementNode;
import io.ballerina.asyncapi.codegenerator.usecase.UseCase;
import io.ballerina.compiler.syntax.tree.*;
import io.ballerina.tools.text.TextDocuments;
import org.ballerinalang.formatter.core.Formatter;
import org.ballerinalang.formatter.core.FormatterException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;

public class ListenerController implements Controller {

    @Override
    public String generateBalCode(String spec, String balTemplate) throws BallerinaAsyncApiException {
        AaiDocument asyncApiSpec = (Aai20Document) Library.readDocumentFromJSONString(spec);

        var textDocument = TextDocuments.from(balTemplate);
        var syntaxTree = SyntaxTree.from(textDocument);
        ModulePartNode oldRoot = syntaxTree.rootNode();
        var functionDefinitionNode = getServiceTypeStrFuncNode(oldRoot);

        if (functionDefinitionNode == null) {
            throw new BallerinaAsyncApiException("Function 'getServiceTypeStr', is not found in the listener.bal");
        }

        var functionBodyBlockNode = (FunctionBodyBlockNode) functionDefinitionNode.functionBody();

        UseCase extractServiceTypes = new ExtractServiceTypesFromSpec(asyncApiSpec);
        Map<String, List<String>> serviceTypesMap = extractServiceTypes.execute();
        List<String> serviceTypes = new ArrayList<>(serviceTypesMap.keySet());
        UseCase genIfElseNode = new GenerateListenerStatementNode(serviceTypes);
        StatementNode ifElseStatementNode = genIfElseNode.execute();
        NodeList<StatementNode> stmts = createNodeList(ifElseStatementNode);

        var functionBodyBlockNodeNew = functionBodyBlockNode.modify().withStatements(stmts).apply();
        ModulePartNode newRoot = oldRoot.replace(functionBodyBlockNode, functionBodyBlockNodeNew);
        var modifiedTree = syntaxTree.replaceNode(oldRoot, newRoot);

        try {
            var formattedSourceCode = Formatter.format(modifiedTree).toSourceCode();
            return formattedSourceCode;
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
