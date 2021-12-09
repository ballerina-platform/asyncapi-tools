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
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.IfElseStatementNode;
import io.ballerina.compiler.syntax.tree.ReturnStatementNode;
import io.ballerina.compiler.syntax.tree.StatementNode;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Test the generation of Ballerina listener statement nodes.
 */
public class GenerateListenerStatementNodeTest {
    @Test(description = "Test the functionality of the generate function " +
            "when the service types list is empty",
            expectedExceptions = BallerinaAsyncApiException.class,
            expectedExceptionsMessageRegExp = "No service types found, probably there are no channels defined " +
                    "in the async api spec")
    public void testGenerateWithEmptyServiceType() throws BallerinaAsyncApiException {
        Generator generateListenerStatementNode = new GenerateListenerStatementNode(new ArrayList<>());
        generateListenerStatementNode.generate();
    }

    @Test(description = "Test the functionality of the generate function " +
            "when the service types list has only one item")
    public void testGenerateWithOneServiceType() throws BallerinaAsyncApiException {
        List<String> serviceTypesNames = new ArrayList<>();
        serviceTypesNames.add("AppMentionHandlingService");
        Generator generateListenerStatementNode = new GenerateListenerStatementNode(serviceTypesNames);
        StatementNode statementNode = generateListenerStatementNode.generate();
        Assert.assertTrue(statementNode instanceof ReturnStatementNode);
        Assert.assertTrue(((ReturnStatementNode) statementNode).expression().isPresent());
        ExpressionNode expressionNode = ((ReturnStatementNode) statementNode).expression().get();
        Assert.assertEquals(expressionNode.syntaxTree().toSourceCode(),
                "return\"AppMentionHandlingService\";");
    }

    @Test(description = "Test the functionality of the generate function " +
            "when the service types list has two items")
    public void testGenerateWithTwoServiceTypes() throws BallerinaAsyncApiException {
        List<String> serviceTypesNames = new ArrayList<>();
        serviceTypesNames.add("AppMentionHandlingService");
        serviceTypesNames.add("AppCreatedHandlingService");
        Generator generateListenerStatementNode = new GenerateListenerStatementNode(serviceTypesNames);
        StatementNode statementNode = generateListenerStatementNode.generate();
        Assert.assertTrue(statementNode instanceof IfElseStatementNode);
        Assert.assertEquals(((IfElseStatementNode) statementNode).condition().toSourceCode(),
                "serviceRefisAppMentionHandlingService");
        Assert.assertTrue(((IfElseStatementNode) statementNode).elseBody().isPresent());
        Assert.assertEquals(((IfElseStatementNode) statementNode).elseBody().get().toSourceCode(),
                "else{return\"AppCreatedHandlingService\";}");
    }

    @Test(description = "Test the functionality of the generate function " +
            "when the service types list has multiple items")
    public void testGenerateWithMultipleServiceTypes() throws BallerinaAsyncApiException {
        List<String> serviceTypesNames = new ArrayList<>();
        serviceTypesNames.add("AppMentionHandlingService");
        serviceTypesNames.add("AppCreatedHandlingService");
        serviceTypesNames.add("AppInstalledHandlingService");
        Generator generateListenerStatementNode = new GenerateListenerStatementNode(serviceTypesNames);
        StatementNode statementNode = generateListenerStatementNode.generate();
        Assert.assertTrue(statementNode instanceof IfElseStatementNode);
        Assert.assertEquals(((IfElseStatementNode) statementNode).condition().toSourceCode(),
                "serviceRefisAppMentionHandlingService");
        Assert.assertTrue(((IfElseStatementNode) statementNode).elseBody().isPresent());
        Assert.assertEquals(((IfElseStatementNode) statementNode).elseBody().get().toSourceCode(),
                "elseifserviceRefisAppCreatedHandlingService{return" +
                        "\"AppCreatedHandlingService\";}else{return\"AppInstalledHandlingService\";}");
    }
}
