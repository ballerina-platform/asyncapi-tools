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
import io.ballerina.asyncapi.codegenerator.usecase.GenerateMatchStatementNode;
import io.ballerina.asyncapi.codegenerator.usecase.Generator;
import io.ballerina.compiler.syntax.tree.BuiltinSimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.CaptureBindingPatternNode;
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.FunctionBodyBlockNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.MatchStatementNode;
import io.ballerina.compiler.syntax.tree.MethodCallExpressionNode;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.TypedBindingPatternNode;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocuments;
import org.ballerinalang.formatter.core.Formatter;
import org.ballerinalang.formatter.core.FormatterException;

import java.util.List;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createBuiltinSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createCaptureBindingPatternNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createCheckExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createIdentifierToken;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createMethodCallExpressionNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createPositionalArgumentNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSeparatedNodeList;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createSimpleNameReferenceNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createTypedBindingPatternNode;
import static io.ballerina.compiler.syntax.tree.NodeFactory.createVariableDeclarationNode;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CHECK_KEYWORD;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.CLOSE_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.DOT_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.EQUAL_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.OPEN_PAREN_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SEMICOLON_TOKEN;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.STRING_KEYWORD;

/**
 * This file contains the logics and functions related to code generation of the dispatcher_service.bal.
 */
public class DispatcherController implements BalController {
    private final List<ServiceType> serviceTypes;
    private final String eventIdentifierPath;

    public DispatcherController(List<ServiceType> serviceTypes, String eventIdentifierPath) {
        this.serviceTypes = serviceTypes;
        this.eventIdentifierPath = eventIdentifierPath;
    }

    @Override
    public String generateBalCode(String balTemplate) throws BallerinaAsyncApiException {
        TextDocument textDocument = TextDocuments.from(balTemplate);
        SyntaxTree syntaxTree = SyntaxTree.from(textDocument);
        ModulePartNode oldRoot = syntaxTree.rootNode();

        String eventIdentifierPath = this.eventIdentifierPath;
        if (isEventIdentifierInHeader(this.eventIdentifierPath)) {
            eventIdentifierPath = "eventIdentifier";

            FunctionDefinitionNode postFunctionDefinitionNode = getPostFuncNode(oldRoot);
            if (postFunctionDefinitionNode == null) {
                throw new BallerinaAsyncApiException("Resource function '"
                        + Constants.DISPATCHER_SERVICE_POST_FUNCTION_NAME
                        + "', is not found in the dispatcher_service.bal");
            }

            FunctionBodyBlockNode postFunctionBodyBlockNode =
                    (FunctionBodyBlockNode) postFunctionDefinitionNode.functionBody();
            NodeList<StatementNode> oldStatement = postFunctionBodyBlockNode.statements();
            oldStatement = oldStatement.remove(1);
            NodeList<StatementNode> newStatement = oldStatement.add(1, getEventIdentifierNode());
            FunctionBodyBlockNode postFunctionBodyBlockNodeNew =
                    postFunctionBodyBlockNode.modify().withStatements(newStatement).apply();
            ModulePartNode midRoot  = oldRoot.replace(postFunctionBodyBlockNode, postFunctionBodyBlockNodeNew);
            syntaxTree = syntaxTree.replaceNode(oldRoot, midRoot);
            oldRoot = syntaxTree.rootNode();
        }

        FunctionDefinitionNode functionDefinitionNode = getResourceFuncNode(oldRoot);
        if (functionDefinitionNode == null) {
            throw new BallerinaAsyncApiException("Resource function '"
                    + Constants.DISPATCHER_SERVICE_RESOURCE_FILTER_FUNCTION_NAME
                    + "', is not found in the dispatcher_service.bal");
        }

        Generator generateMatchStatement = new GenerateMatchStatementNode(serviceTypes, eventIdentifierPath);
        MatchStatementNode matchStatementNode = generateMatchStatement.generate();

        FunctionBodyBlockNode functionBodyBlockNode = (FunctionBodyBlockNode) functionDefinitionNode.functionBody();
        NodeList<StatementNode> oldStatements = functionBodyBlockNode.statements();
        NodeList<StatementNode> newStatements =
                oldStatements.add(matchStatementNode);
        FunctionBodyBlockNode functionBodyBlockNodeNew =
                functionBodyBlockNode.modify().withStatements(newStatements).apply();
        ModulePartNode newRoot = oldRoot.replace(functionBodyBlockNode, functionBodyBlockNodeNew);
        SyntaxTree modifiedTree = syntaxTree.replaceNode(oldRoot, newRoot);

        try {
            return Formatter.format(modifiedTree).toSourceCode();
        } catch (FormatterException e) {
            throw new BallerinaAsyncApiException("Could not format the generated code, " +
                    "may be a syntax issue in the generated code", e);
        }
    }

    private FunctionDefinitionNode getResourceFuncNode(ModulePartNode oldRoot) {
        for (ModuleMemberDeclarationNode node : oldRoot.members()) {
            if (node.kind() == SyntaxKind.CLASS_DEFINITION) {
                for (Node funcNode : ((ClassDefinitionNode) node).members()) {
                    if ((funcNode.kind() == SyntaxKind.OBJECT_METHOD_DEFINITION)
                            && ((FunctionDefinitionNode) funcNode).functionName().text().equals(
                            Constants.DISPATCHER_SERVICE_RESOURCE_FILTER_FUNCTION_NAME)) {
                        return (FunctionDefinitionNode) funcNode;
                    }
                }
            }
        }
        return null;
    }

    private FunctionDefinitionNode getPostFuncNode(ModulePartNode oldRoot) {
        for (ModuleMemberDeclarationNode node : oldRoot.members()) {
            if (node.kind() == SyntaxKind.CLASS_DEFINITION) {
                for (Node funcNode : ((ClassDefinitionNode) node).members()) {
                    if ((funcNode.kind() == SyntaxKind.RESOURCE_ACCESSOR_DEFINITION)
                            && ((FunctionDefinitionNode) funcNode).functionName().text().equals(
                            Constants.DISPATCHER_SERVICE_POST_FUNCTION_NAME)) {
                        return (FunctionDefinitionNode) funcNode;
                    }
                }
            }
        }
        return null;
    }

    private VariableDeclarationNode getEventIdentifierNode() {
        // {@code string eventIdentifier}
        BuiltinSimpleNameReferenceNode typeBindingPattern = createBuiltinSimpleNameReferenceNode(null,
                createToken(STRING_KEYWORD));
        CaptureBindingPatternNode bindingPattern = createCaptureBindingPatternNode(
                createIdentifierToken("eventIdentifier"));
        TypedBindingPatternNode typedBindingPatternNode = createTypedBindingPatternNode(typeBindingPattern,
                bindingPattern);

        // {@code check request.getHeader("event-name")}
        SimpleNameReferenceNode variableName = createSimpleNameReferenceNode(createIdentifierToken("request"));
        SimpleNameReferenceNode methodName = createSimpleNameReferenceNode(createIdentifierToken("getHeader"));
        SimpleNameReferenceNode argumentName =
                createSimpleNameReferenceNode(createIdentifierToken("\"" + this.eventIdentifierPath + "\""));
        FunctionArgumentNode eventIdentifierArgument = createPositionalArgumentNode(argumentName);
        SeparatedNodeList<FunctionArgumentNode> arguments = createSeparatedNodeList(eventIdentifierArgument);
        MethodCallExpressionNode methodCallExpression =
                createMethodCallExpressionNode(variableName, createToken(DOT_TOKEN), methodName,
                        createToken(OPEN_PAREN_TOKEN), arguments, createToken(CLOSE_PAREN_TOKEN));
        CheckExpressionNode initializer =
                createCheckExpressionNode(null, createToken(CHECK_KEYWORD), methodCallExpression);

        // {@code string eventIdentifier = check request.getHeader("event-name");}
        VariableDeclarationNode eventIdentifierNode =
                createVariableDeclarationNode(createEmptyNodeList(), null, typedBindingPatternNode,
                        createToken(EQUAL_TOKEN), initializer, createToken(SEMICOLON_TOKEN));
        return eventIdentifierNode;
    }

    private Boolean isEventIdentifierInHeader(String eventIdentifierPath) {
        if (eventIdentifierPath.startsWith(Constants.CLONE_WITH_TYPE_VAR_NAME)) {
            return false;
        }
        return true;
    }
}


