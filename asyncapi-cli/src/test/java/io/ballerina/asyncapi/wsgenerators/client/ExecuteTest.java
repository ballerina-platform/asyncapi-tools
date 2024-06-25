/*
 *  Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
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
package io.ballerina.asyncapi.wsgenerators.client;

import io.ballerina.asyncapi.cmd.websockets.AsyncApiToBallerinaGenerator;
import io.ballerina.asyncapi.websocketscore.exception.BallerinaAsyncApiExceptionWs;
import org.ballerinalang.formatter.core.FormatterException;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * This tests class is to generate ballerina client codes.
 */
public class ExecuteTest {

    @Test(description = "Generate Client for testings", enabled = false)
    public void generatePathWithPathParameterTests() throws IOException, BallerinaAsyncApiExceptionWs,
            FormatterException {
        AsyncApiToBallerinaGenerator asyncAPIToBallerinaGenerator = new AsyncApiToBallerinaGenerator();

        asyncAPIToBallerinaGenerator.generateClient(
                "src/test/resources/websockets/asyncapi-to-ballerina/client/StreamResponse/" +
                        "multiple_stream_with_dispatcherStreamId.yaml",
                "/Users/thushalya/Documents/out");

    }
}
