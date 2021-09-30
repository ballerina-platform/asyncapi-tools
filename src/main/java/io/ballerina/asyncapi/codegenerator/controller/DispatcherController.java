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
import io.ballerina.asyncapi.codegenerator.usecase.GenerateMatchStatement;
import io.ballerina.asyncapi.codegenerator.usecase.UseCase;
import io.ballerina.compiler.syntax.tree.*;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocuments;
import org.ballerinalang.formatter.core.Formatter;
import org.ballerinalang.formatter.core.FormatterException;


public class DispatcherController implements Controller {

    @Override
    public void generateBalCode(String spec, String balTemplate) throws BallerinaAsyncApiException {

        AaiDocument document = (AaiDocument) Library.readDocumentFromJSONString(spec);
        TextDocument textDocument = TextDocuments.from(balTemplate);
        SyntaxTree syntaxTree = SyntaxTree.from(textDocument);
        ModulePartNode rootNode = syntaxTree.rootNode();
        FunctionDefinitionNode resourceFunction = (FunctionDefinitionNode) ((ClassDefinitionNode) rootNode.members().get(0)).members().get(3);

        UseCase generateMatchStatement = new GenerateMatchStatement(document);
        MatchStatementNode msn = generateMatchStatement.execute();
        syntaxTree = syntaxTree.replaceNode(((FunctionBodyBlockNode) resourceFunction.functionBody()).statements().get(2), msn);
        SyntaxTree formatted = null;
        try {
            formatted = Formatter.format(syntaxTree);
        } catch (FormatterException e) {
            e.printStackTrace();
        }
        System.out.println(formatted.toString());
    }
}


