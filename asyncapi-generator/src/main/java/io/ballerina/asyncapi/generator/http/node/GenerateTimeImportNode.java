/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com)
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.ballerina.asyncapi.generator.http.node;

import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createSeparatedNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createImportDeclarationNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createImportOrgNameNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.IMPORT_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SEMICOLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SLASH_TOKEN;

/**
 * Generates the {@code import ballerina/time;} import declaration node for
 * {@code dispatcher_service.bal}, needed when the webhook auth DSL declares a
 * {@code freshness} (request-timestamp staleness) check.
 */
public class GenerateTimeImportNode {

    public static final String TIME_MODULE = "time";
    private static final String BALLERINA_ORG = "ballerina";

    /**
     * Generates the {@code import ballerina/time;} import declaration node.
     *
     * @return the generated {@link ImportDeclarationNode}
     * @throws GeneratorException never thrown; declared for consistency with other node generators
     */
    public static ImportDeclarationNode generate() throws GeneratorException {
        return createImportDeclarationNode(
                createToken(IMPORT_KEYWORD),
                createImportOrgNameNode(createIdentifierToken(BALLERINA_ORG), createToken(SLASH_TOKEN)),
                createSeparatedNodeList(createIdentifierToken(TIME_MODULE)),
                null,
                createToken(SEMICOLON_TOKEN));
    }
}
