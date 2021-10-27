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
import io.ballerina.asyncapi.codegenerator.repository.FileRepository;
import io.ballerina.asyncapi.codegenerator.repository.FileRepositoryImpl;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;

/**
 * Test the functionality and flow control of the data extraction process.
 */
public class DispatcherControllerTest {
    FileRepository fileRepository = new FileRepositoryImpl();

    @Test(description = "Test the functionality of the 'generateBalCode' function")
    public void testGenerateBalCode() throws BallerinaAsyncApiException {
        String asyncApiSpecStr = fileRepository
                .getFileContentFromResources("specs/spec-complete-slack.yml");
        String asyncApiSpecJson = fileRepository.convertYamlToJson(asyncApiSpecStr);
        SpecController specController = new AsyncApiSpecController(asyncApiSpecJson);

        String dispatcherTemplate = fileRepository
                .getFileContentFromResources(Constants.DISPATCHER_SERVICE_BAL_FILE_NAME);
        String dispatcherResult = fileRepository
                .getFileContentFromResources("expected_gen/".concat(Constants.DISPATCHER_SERVICE_BAL_FILE_NAME));
        BalController dispatcherController = new DispatcherController(
                specController.getServiceTypes(), specController.getEventIdentifierPath());
        Assert.assertEquals(dispatcherController.generateBalCode(dispatcherTemplate), dispatcherResult);
    }

    @Test(description = "Test the functionality of the generate function " +
            "when the there is no resource function named as '.' in the dispatcher_service.bal template",
            expectedExceptions = BallerinaAsyncApiException.class,
            expectedExceptionsMessageRegExp =
                    "Resource function '.', is not found in the dispatcher_service.bal")
    public void testGenerateWithInvalidTemplate() throws BallerinaAsyncApiException {
        BalController dispatcherController = new DispatcherController(new ArrayList<>(), "");
        dispatcherController.generateBalCode("");
    }
}
