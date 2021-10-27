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
import io.ballerina.asyncapi.codegenerator.entity.RemoteFunction;
import io.ballerina.asyncapi.codegenerator.entity.ServiceType;
import io.ballerina.compiler.syntax.tree.MatchStatementNode;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Test the generation of Ballerina match statement nodes.
 */
public class GenerateMatchStatementNodeTest {
    @Test(description = "Test the functionality of the generate function " +
            "when the service types list is not empty")
    public void testGenerate() throws BallerinaAsyncApiException {
        RemoteFunction remoteFunction = new RemoteFunction("app_mention", "GenericEvent");
        List<RemoteFunction> remoteFunctions = new ArrayList<>();
        remoteFunctions.add(remoteFunction);
        List<ServiceType> serviceTypes = new ArrayList<>();
        ServiceType serviceType = new ServiceType("AppMentionHandlingService", remoteFunctions);
        serviceTypes.add(serviceType);
        String expression = "genericEvent.event.'type";
        Generator generateMatchStatementNode =
                new GenerateMatchStatementNode(serviceTypes, expression);
        MatchStatementNode matchStatementNode = generateMatchStatementNode.generate();
        Assert.assertEquals(matchStatementNode.matchClauses().size(), 1);

        Assert.assertEquals(matchStatementNode.condition().toSourceCode(), expression);

        String matchStatement = matchStatementNode.matchClauses().get(0).blockStatement()
                .statements().get(0).toSourceCode();
        Assert.assertEquals(matchStatement,
                "checkself.executeRemoteFunc(genericDataType,\"app_mention\"," +
                        "\"AppMentionHandlingService\",\"onAppMention\");");
    }

    @Test(description = "Test the functionality of the generate function " +
            "when the service types list is empty",
            expectedExceptions = BallerinaAsyncApiException.class,
            expectedExceptionsMessageRegExp = "No service types found, probably there are no channels defined " +
                    "in the async api spec")
    public void testGenerateWithEmptyServiceTypesList() throws BallerinaAsyncApiException {
        String expression = "genericEvent.event.'type";
        Generator generateMatchStatementNode =
                new GenerateMatchStatementNode(new ArrayList<>(), expression);
        generateMatchStatementNode.generate();
    }

    @Test(description = "Test the functionality of the generate function " +
            "when the service types list is empty",
            expectedExceptions = BallerinaAsyncApiException.class,
            expectedExceptionsMessageRegExp = "Event identifier path is empty")
    public void testGenerateWithEmptyIdentifierPath() throws BallerinaAsyncApiException {
        Generator generateMatchStatementNode =
                new GenerateMatchStatementNode(new ArrayList<>(), "");
        generateMatchStatementNode.generate();
    }
}
