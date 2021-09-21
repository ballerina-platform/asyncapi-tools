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

package io.ballerina.asyncapi.codegenerator.controller;

import io.apicurio.datamodels.Library;
import io.apicurio.datamodels.asyncapi.models.AaiChannelItem;
import io.apicurio.datamodels.asyncapi.models.AaiDocument;
import io.apicurio.datamodels.asyncapi.models.AaiMessage;
import io.apicurio.datamodels.asyncapi.v2.models.Aai20Document;
import io.ballerina.asyncapi.codegenerator.configuration.BallerinaAsyncApiException;
import io.ballerina.asyncapi.codegenerator.configuration.Constants;
import io.ballerina.asyncapi.codegenerator.usecase.ExtractServiceTypesFromSpec;
import io.ballerina.asyncapi.codegenerator.usecase.GenerateServiceTypeNode;
import io.ballerina.asyncapi.codegenerator.usecase.UseCase;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.tools.text.TextDocuments;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ballerinalang.formatter.core.Formatter;
import org.ballerinalang.formatter.core.FormatterException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceTypesController implements Controller {
    private static final Logger logger = LogManager.getLogger(ServiceTypesController.class);

    @Override
    public void generateBalCode(String spec, String balTemplate) throws BallerinaAsyncApiException {
        AaiDocument asyncApiSpec = (Aai20Document) Library.readDocumentFromJSONString(spec);

        UseCase extractServiceTypes = new ExtractServiceTypesFromSpec(asyncApiSpec);
        Map<String, List<String>> serviceTypes = extractServiceTypes.execute();

        List<ModuleMemberDeclarationNode> serviceNodes = new ArrayList<>();
        for (Map.Entry<String, List<String>> service : serviceTypes.entrySet()) {
            UseCase generateServiceTypeNode = new GenerateServiceTypeNode(service.getKey(), service.getValue());
            serviceNodes.add(generateServiceTypeNode.execute());
        }

        var textDocument = TextDocuments.from(balTemplate);
        var syntaxTree = SyntaxTree.from(textDocument);
        ModulePartNode oldRoot = syntaxTree.rootNode();
        ModulePartNode newRoot = oldRoot.modify().withMembers(oldRoot.members().addAll(serviceNodes)).apply();
        var modifiedTree = syntaxTree.replaceNode(oldRoot, newRoot);

        try {
            var formattedSourceCode = Formatter.format(modifiedTree).toSourceCode();
            logger.debug("Generated the source code for the service types: {}", formattedSourceCode);
        } catch (FormatterException e) {
            logger.error("Could not format the generated code, may be syntax issue in the generated code. " +
                    "Generated code: {}", modifiedTree.toSourceCode());
        }
    }
}
