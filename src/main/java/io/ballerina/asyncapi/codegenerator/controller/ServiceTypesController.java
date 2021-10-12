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

import io.ballerina.asyncapi.codegenerator.configuration.BallerinaAsyncApiException;
import io.ballerina.asyncapi.codegenerator.configuration.Constants;
import io.ballerina.asyncapi.codegenerator.entity.ServiceType;
import io.ballerina.asyncapi.codegenerator.usecase.GenerateServiceTypeNode;
import io.ballerina.asyncapi.codegenerator.usecase.GenerateUnionDescriptorNode;
import io.ballerina.asyncapi.codegenerator.usecase.Generator;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocuments;
import org.ballerinalang.formatter.core.Formatter;
import org.ballerinalang.formatter.core.FormatterException;

import java.util.ArrayList;
import java.util.List;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSimpleNameReferenceNode;

/**
 * This file contains the logics and functions related to code generation of the service_types.bal.
 */
public class ServiceTypesController implements BalController {
    private final List<ServiceType> serviceTypes;

    public ServiceTypesController(List<ServiceType> serviceTypes) {
        this.serviceTypes = serviceTypes;
    }

    @Override
    public String generateBalCode(String balTemplate) throws BallerinaAsyncApiException {
        List<ModuleMemberDeclarationNode> serviceNodes = new ArrayList<>();
        List<TypeDescriptorNode> serviceTypeNodes = new ArrayList<>();
        for (ServiceType service : serviceTypes) {
            Generator generateServiceTypeNode =
                    new GenerateServiceTypeNode(service.getServiceTypeName(), service.getRemoteFunctions());
            TypeDefinitionNode typeDefinitionNode = generateServiceTypeNode.generate();
            serviceTypeNodes.add(
                    createSimpleNameReferenceNode(createIdentifierToken(typeDefinitionNode.typeName().text())));
            serviceNodes.add(typeDefinitionNode);
        }

        Generator generateUnionNode = new GenerateUnionDescriptorNode(serviceTypeNodes, Constants.GENERIC_SERVICE_TYPE);
        serviceNodes.add(generateUnionNode.generate());

        TextDocument textDocument = TextDocuments.from(balTemplate);
        SyntaxTree syntaxTree = SyntaxTree.from(textDocument);
        ModulePartNode oldRoot = syntaxTree.rootNode();
        ModulePartNode newRoot = oldRoot.modify().withMembers(oldRoot.members().addAll(serviceNodes)).apply();
        SyntaxTree modifiedTree = syntaxTree.replaceNode(oldRoot, newRoot);

        try {
            return Formatter.format(modifiedTree).toSourceCode();
        } catch (FormatterException e) {
            throw new BallerinaAsyncApiException("Could not format the generated code, " +
                    "may be a syntax issue in the generated code", e);
        }
    }
}
