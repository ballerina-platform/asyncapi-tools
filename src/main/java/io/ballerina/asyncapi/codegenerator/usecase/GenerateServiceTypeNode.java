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
import io.ballerina.compiler.syntax.tree.*;

import java.util.ArrayList;
import java.util.List;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.*;
import static io.ballerina.compiler.syntax.tree.NodeFactory.*;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.*;

public class GenerateServiceTypeNode implements UseCase {
    private final String serviceTypeName;
    private final List<String> remoteFunctionNames;
    private final CodegenUtils codegenUtils = new CodegenUtils();

    public GenerateServiceTypeNode(String serviceTypeName, List<String> remoteFunctionNames) {
        this.serviceTypeName = serviceTypeName;
        this.remoteFunctionNames = remoteFunctionNames;
    }

    @Override
    public TypeDefinitionNode execute() throws BallerinaAsyncApiException {
        List<Node> remoteFunctions = new ArrayList<>();
        remoteFunctionNames.forEach(remoteFunction -> {
            var methodDeclarationNode = createMethodDeclarationNode(
                    SyntaxKind.METHOD_DECLARATION, null, createNodeList(createToken(REMOTE_KEYWORD)),
                    createToken(SyntaxKind.FUNCTION_KEYWORD),
                    createIdentifierToken(codegenUtils.getFunctionNameByEventName(remoteFunction)), createEmptyNodeList(),
                    createFunctionSignatureNode(
                            createToken(OPEN_PAREN_TOKEN), createSeparatedNodeList(),
                            createToken(CLOSE_PAREN_TOKEN), null),
                    createToken(SyntaxKind.SEMICOLON_TOKEN));
            remoteFunctions.add(methodDeclarationNode);
        });
        var serviceTypeToken = AbstractNodeFactory
                .createIdentifierToken(codegenUtils.getServiceTypeNameByServiceName(serviceTypeName));
        var recordTypeDescriptorNode =
                NodeFactory.createObjectTypeDescriptorNode(createNodeList(createToken(SERVICE_KEYWORD)),
                        createToken(OBJECT_KEYWORD), createToken(OPEN_BRACE_TOKEN), createNodeList(remoteFunctions),
                        createToken(CLOSE_BRACE_TOKEN));
        return createTypeDefinitionNode(null, createToken(PUBLIC_KEYWORD),
                createToken(TYPE_KEYWORD), serviceTypeToken, recordTypeDescriptorNode, createToken(SEMICOLON_TOKEN));
    }
}
