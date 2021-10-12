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
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;

import java.util.List;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTypeDefinitionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createUnionTypeDescriptorNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PUBLIC_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SEMICOLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.TYPE_KEYWORD;

/**
 * Generate the union descriptor node for service_types.bal and data_types.bal files.
 */
public class GenerateUnionDescriptorNode implements Generator {
    private final List<TypeDescriptorNode> serviceTypeNodes;
    private final String identifierName;

    public GenerateUnionDescriptorNode(List<TypeDescriptorNode> serviceTypeNodes, String identifierName) {
        this.serviceTypeNodes = serviceTypeNodes;
        this.identifierName = identifierName;
    }

    @Override
    public TypeDefinitionNode generate() throws BallerinaAsyncApiException {
        return createTypeDefinitionNode(null, createToken(PUBLIC_KEYWORD),
                createToken(TYPE_KEYWORD), createIdentifierToken(identifierName),
                getUnionDescriptorNode(serviceTypeNodes), createToken(SEMICOLON_TOKEN));
    }

    private TypeDescriptorNode getUnionDescriptorNode(List<TypeDescriptorNode> nodes) {
        if (nodes.size() == 1) {
            return nodes.get(0);
        }
        return createUnionTypeDescriptorNode(
                getUnionDescriptorNode(nodes.subList(0, nodes.size() - 1)),
                createToken(SyntaxKind.PIPE_TOKEN),
                nodes.get(nodes.size() - 1));
    }
}
