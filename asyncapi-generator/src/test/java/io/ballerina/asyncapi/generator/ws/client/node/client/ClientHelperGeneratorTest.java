/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com)
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
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
package io.ballerina.asyncapi.generator.ws.client.node.client;

import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link ClientHelperGenerator} verifying that each helper function node
 * contains the expected source fragments: parameter names, body statements, and qualifiers.
 */
public class ClientHelperGeneratorTest {

    @Test
    void testBuildGetRecordName_containsDispatchingValueAndResult() {
        FunctionDefinitionNode node = new ClientHelperGenerator(false).buildGetRecordName();
        String src = node.toString();
        Assert.assertTrue(src.contains("dispatchingValue"),
                "getRecordName should declare a 'dispatchingValue' parameter to receive the raw event type string");
        Assert.assertTrue(src.contains("result"),
                "getRecordName should build up a 'result' variable by capitalising each word segment");
    }

    @Test
    void testBuildGetPipeName_containsResponseMapLookup() {
        FunctionDefinitionNode node = new ClientHelperGenerator(false).buildGetPipeName();
        String src = node.toString();
        Assert.assertTrue(src.contains("responseMap"),
                "getPipeName should look up the response type in 'self.responseMap' to find the pipe name");
        Assert.assertTrue(src.contains("hasKey"),
                "getPipeName should call 'hasKey' to check whether the resolved record type exists in responseMap");
    }

    @Test
    void testBuildAttemptToCloseConnection_containsConnectionCloseCall() {
        FunctionDefinitionNode node = new ClientHelperGenerator(false).buildAttemptToCloseConnection();
        String src = node.toString();
        Assert.assertTrue(src.contains("connectionClose"),
                "attemptToCloseConnection should delegate to 'self->connectionClose()' to perform the shutdown");
    }

    @Test
    void testBuildConnectionClose_isRemoteAndReturnsError() {
        FunctionDefinitionNode node = new ClientHelperGenerator(true).buildConnectionClose();
        String src = node.toString();
        Assert.assertTrue(src.contains("remote"),
                "connectionClose should carry the 'remote' qualifier so it can be invoked as a remote call");
        Assert.assertTrue(src.contains("error?"),
                "connectionClose should declare 'error?' as its return type to propagate close failures");
        Assert.assertTrue(src.contains("isActive"),
                "connectionClose should set 'isActive' to false inside a lock block to signal shutdown");
    }
}
