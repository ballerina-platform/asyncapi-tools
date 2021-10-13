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
import io.ballerina.compiler.syntax.tree.MethodDeclarationNode;
import io.ballerina.compiler.syntax.tree.ObjectTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Test the generation of Ballerina Service Type nodes.
 */
public class GenerateServiceTypeNodeTest {
    @Test(description = "Test the functionality of the generate function " +
            "when there is only one remote function")
    public void testGenerateWithSingleRemoteFunction() throws BallerinaAsyncApiException {
        String serviceTypeName = "AppMentionHandlingService";
        List<RemoteFunction> remoteFunctions = new ArrayList<>();
        RemoteFunction remoteFunction = new RemoteFunction("app_mention", "GenericEventWrapper");
        remoteFunctions.add(remoteFunction);

        Generator generateServiceTypeNode = new GenerateServiceTypeNode(serviceTypeName, remoteFunctions);
        TypeDefinitionNode typeDefinitionNode = generateServiceTypeNode.generate();

        Assert.assertEquals(typeDefinitionNode.typeName().text(), "AppMentionHandlingService");
        Assert.assertTrue(typeDefinitionNode.visibilityQualifier().isPresent());
        Assert.assertEquals(typeDefinitionNode.visibilityQualifier().get().text(), "public");

        ObjectTypeDescriptorNode objectTypeDescriptorNode =
                (ObjectTypeDescriptorNode) typeDefinitionNode.typeDescriptor();
        MethodDeclarationNode methodDeclarationNode = (MethodDeclarationNode) objectTypeDescriptorNode.members().get(0);
        Assert.assertEquals(methodDeclarationNode.methodName().text(), "onAppMention");
        Assert.assertEquals(methodDeclarationNode.qualifierList().get(0).text(), "remote");
        Assert.assertTrue(methodDeclarationNode.methodSignature().returnTypeDesc().isPresent());
        Assert.assertEquals(methodDeclarationNode.methodSignature().returnTypeDesc().get().type().toSourceCode(),
                "error?");
        Assert.assertEquals(methodDeclarationNode.methodSignature().parameters().get(0).toSourceCode(),
                "GenericEventWrapperevent");
    }

    @Test(description = "Test the functionality of the generate function " +
            "when there are multiple remote functions")
    public void testGenerateWithMultipleRemoteFunctions() throws BallerinaAsyncApiException {
        String serviceTypeName = "AppMentionHandlingService";
        List<RemoteFunction> remoteFunctions = new ArrayList<>();
        RemoteFunction remoteFunction1 = new RemoteFunction("app_mention", "AppMentionWrapper");
        RemoteFunction remoteFunction2 = new RemoteFunction("app_created", "AppCreatedWrapper");
        remoteFunctions.add(remoteFunction1);
        remoteFunctions.add(remoteFunction2);

        Generator generateServiceTypeNode = new GenerateServiceTypeNode(serviceTypeName, remoteFunctions);
        TypeDefinitionNode typeDefinitionNode = generateServiceTypeNode.generate();

        Assert.assertEquals(typeDefinitionNode.typeName().text(), "AppMentionHandlingService");
        Assert.assertTrue(typeDefinitionNode.visibilityQualifier().isPresent());
        Assert.assertEquals(typeDefinitionNode.visibilityQualifier().get().text(), "public");

        ObjectTypeDescriptorNode objectTypeDescriptorNode =
                (ObjectTypeDescriptorNode) typeDefinitionNode.typeDescriptor();

        MethodDeclarationNode methodDeclarationNode1 =
                (MethodDeclarationNode) objectTypeDescriptorNode.members().get(0);
        Assert.assertEquals(methodDeclarationNode1.methodName().text(), "onAppMention");
        Assert.assertEquals(methodDeclarationNode1.qualifierList().get(0).text(), "remote");
        Assert.assertTrue(methodDeclarationNode1.methodSignature().returnTypeDesc().isPresent());
        Assert.assertEquals(methodDeclarationNode1.methodSignature().returnTypeDesc().get().type().toSourceCode(),
                "error?");
        Assert.assertEquals(methodDeclarationNode1.methodSignature().parameters().get(0).toSourceCode(),
                "AppMentionWrapperevent");

        MethodDeclarationNode methodDeclarationNode2 =
                (MethodDeclarationNode) objectTypeDescriptorNode.members().get(1);
        Assert.assertEquals(methodDeclarationNode2.methodName().text(), "onAppCreated");
        Assert.assertEquals(methodDeclarationNode2.qualifierList().get(0).text(), "remote");
        Assert.assertTrue(methodDeclarationNode2.methodSignature().returnTypeDesc().isPresent());
        Assert.assertEquals(methodDeclarationNode2.methodSignature().returnTypeDesc().get().type().toSourceCode(),
                "error?");
        Assert.assertEquals(methodDeclarationNode2.methodSignature().parameters().get(0).toSourceCode(),
                "AppCreatedWrapperevent");
    }

    @Test(description = "Test the functionality of the generate function " +
            "when the there are no remote functions in the service type",
            expectedExceptions = BallerinaAsyncApiException.class,
            expectedExceptionsMessageRegExp =
                    "Remote functions list is empty in the service type AppMentionHandlingService")
    public void testGenerateWithEmptyRemoteFunctionsList() throws BallerinaAsyncApiException {
        String serviceTypeName = "AppMentionHandlingService";
        Generator generateServiceTypeNode = new GenerateServiceTypeNode(serviceTypeName, new ArrayList<>());
        generateServiceTypeNode.generate();
    }
}
