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
import io.ballerina.asyncapi.codegenerator.repository.FileRepository;
import io.ballerina.asyncapi.codegenerator.repository.FileRepositoryImpl;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test the functionality and flow control of the data extraction process.
 */
public class AsyncApiSpecControllerTest {
    FileRepository fileRepository = new FileRepositoryImpl();

    @Test(description = "Test the functionality of the 'getServiceTypes' function")
    public void testGetServiceTypes() throws BallerinaAsyncApiException {
        String asyncApiSpecStr = fileRepository
                .getFileContentFromResources("specs/spec-complete-slack.yml");
        String asyncApiSpecJson = fileRepository.convertYamlToJson(asyncApiSpecStr);
        SpecController specController = new AsyncApiSpecController(asyncApiSpecJson);

        Assert.assertEquals(specController.getServiceTypes().size(), 3);

        Assert.assertEquals(specController.getServiceTypes().get(0).getServiceTypeName(),
                "AppMentionHandlingService");
        Assert.assertEquals(specController.getServiceTypes().get(0).getRemoteFunctions().size(), 2);
        Assert.assertEquals(specController.getServiceTypes().get(0).getRemoteFunctions().get(0).getEventName(),
                "app_mention_added");
        Assert.assertEquals(specController.getServiceTypes().get(0).getRemoteFunctions().get(0).getEventType(),
                "GenericEventWrapper");

        Assert.assertEquals(specController.getServiceTypes().get(1).getServiceTypeName(),
                "AppRateLimitedHandlingService");
        Assert.assertEquals(specController.getServiceTypes().get(1).getRemoteFunctions().size(), 1);
        Assert.assertEquals(specController.getServiceTypes().get(1).getRemoteFunctions().get(0).getEventName(),
                "app_rate_limited");
        Assert.assertEquals(specController.getServiceTypes().get(1).getRemoteFunctions().get(0).getEventType(),
                "GenericEventWrapper");

        Assert.assertEquals(specController.getServiceTypes().get(2).getServiceTypeName(),
                "AppCreatedHandlingService");
        Assert.assertEquals(specController.getServiceTypes().get(2).getRemoteFunctions().size(), 1);
        Assert.assertEquals(specController.getServiceTypes().get(2).getRemoteFunctions().get(0).getEventName(),
                "app_created");
        Assert.assertEquals(specController.getServiceTypes().get(2).getRemoteFunctions().get(0).getEventType(),
                "CustomTestSchema");
    }

    @Test(description = "Test the functionality of the 'getSchemas' function")
    public void testGetSchemas() throws BallerinaAsyncApiException {
        String asyncApiSpecStr = fileRepository
                .getFileContentFromResources("specs/spec-complete-slack.yml");
        String asyncApiSpecJson = fileRepository.convertYamlToJson(asyncApiSpecStr);
        SpecController specController = new AsyncApiSpecController(asyncApiSpecJson);

        Assert.assertEquals(specController.getSchemas().size(), 2);
        Assert.assertTrue(specController.getSchemas().containsKey("CustomTestSchema"));
        Assert.assertTrue(specController.getSchemas().containsKey("GenericEventWrapper"));
    }

    @Test(description = "Test the functionality of the 'getEventIdentifierPath' function")
    public void testGetEventIdentifierPath() throws BallerinaAsyncApiException {
        String asyncApiSpecStr = fileRepository
                .getFileContentFromResources("specs/spec-complete-slack.yml");
        String asyncApiSpecJson = fileRepository.convertYamlToJson(asyncApiSpecStr);
        SpecController specController = new AsyncApiSpecController(asyncApiSpecJson);

        Assert.assertEquals(specController.getEventIdentifierPath(), "genericEvent.event.'type");
    }
}
