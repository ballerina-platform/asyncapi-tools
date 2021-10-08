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

import io.apicurio.datamodels.Library;
import io.apicurio.datamodels.asyncapi.models.AaiDocument;
import io.apicurio.datamodels.asyncapi.v2.models.Aai20Document;
import io.ballerina.asyncapi.codegenerator.configuration.BallerinaAsyncApiException;
import io.ballerina.asyncapi.codegenerator.entity.RemoteFunction;
import io.ballerina.asyncapi.codegenerator.entity.ServiceType;
import io.ballerina.asyncapi.codegenerator.repository.FileRepository;
import io.ballerina.asyncapi.codegenerator.repository.FileRepositoryImpl;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

public class TestExtractServiceTypesFromSpec {
    FileRepository fileRepository = new FileRepositoryImpl();

    @Test(
            description = "Test the functionality of the extract function " +
                    "when the Async API spec contains only one channel"
    )
    public void testExecuteWithOneChannel() throws BallerinaAsyncApiException {
        String asyncApiSpecStr = fileRepository
                .getFileContentFromResources("specs/spec-single-channel.yml");
        String asyncApeSpecJson = fileRepository.convertYamlToJson(asyncApiSpecStr);
        AaiDocument asyncApiSpec = (Aai20Document) Library.readDocumentFromJSONString(asyncApeSpecJson);
        ExtractUseCase extractServiceTypes = new ExtractServiceTypesFromSpec(asyncApiSpec);
        List<ServiceType> serviceTypes = extractServiceTypes.extract();

        Assert.assertEquals(serviceTypes.get(0).getServiceTypeName(), "FooService");
        List<RemoteFunction> remoteFunctions = serviceTypes.get(0).getRemoteFunctions();
        Assert.assertEquals(remoteFunctions.get(0).getEventName(), "bar_event_1");
        Assert.assertEquals(remoteFunctions.get(1).getEventName(), "bar_event_2");
    }

    @Test(
            description = "Test the functionality of the extract function " +
                    "when the Async API spec contains multiple channels"
    )
    public void testExecuteWithMultipleChannels() throws BallerinaAsyncApiException {
        String asyncApiSpecStr = fileRepository
                .getFileContentFromResources("specs/spec-multiple-channels.yml");
        String asyncApeSpecJson = fileRepository.convertYamlToJson(asyncApiSpecStr);
        AaiDocument asyncApiSpec = (Aai20Document) Library.readDocumentFromJSONString(asyncApeSpecJson);
        ExtractUseCase extractServiceTypes = new ExtractServiceTypesFromSpec(asyncApiSpec);
        List<ServiceType> serviceTypes = extractServiceTypes.extract();

        Assert.assertEquals(serviceTypes.get(0).getServiceTypeName(), "FooService1");
        List<RemoteFunction> remoteFunctions1 = serviceTypes.get(0).getRemoteFunctions();
        Assert.assertEquals(remoteFunctions1.get(0).getEventName(), "bar_1_event_1");
        Assert.assertEquals(remoteFunctions1.get(1).getEventName(), "bar_1_event_2");

        Assert.assertEquals(serviceTypes.get(1).getServiceTypeName(), "FooService2");
        List<RemoteFunction> remoteFunctions2 = serviceTypes.get(1).getRemoteFunctions();
        Assert.assertEquals(remoteFunctions2.get(0).getEventName(), "bar_2_event_1");
        Assert.assertEquals(remoteFunctions2.get(1).getEventName(), "bar_2_event_2");

        Assert.assertEquals(serviceTypes.get(2).getServiceTypeName(), "FooService3");
        List<RemoteFunction> remoteFunctions = serviceTypes.get(2).getRemoteFunctions();
        Assert.assertEquals(remoteFunctions.get(0).getEventName(), "bar_3_event_1");
    }

    @Test(
            description = "Test the functionality of the extract function " +
                    "when the Async API spec does not contains the x-ballerina-service-type attribute in the channel"
    )
    public void testExecuteWithMissingXServiceType() throws BallerinaAsyncApiException {
        String asyncApiSpecStr = fileRepository
                .getFileContentFromResources("specs/spec-single-channel-missing-x-service-type.yml");
        String asyncApeSpecJson = fileRepository.convertYamlToJson(asyncApiSpecStr);
        AaiDocument asyncApiSpec = (Aai20Document) Library.readDocumentFromJSONString(asyncApeSpecJson);
        ExtractUseCase extractServiceTypes = new ExtractServiceTypesFromSpec(asyncApiSpec);
        List<ServiceType> serviceTypes = extractServiceTypes.extract();

        Assert.assertEquals(serviceTypes.get(0).getServiceTypeName(), "EventsFoo1");
        List<RemoteFunction> remoteFunctions1 = serviceTypes.get(0).getRemoteFunctions();
        Assert.assertEquals(remoteFunctions1.get(0).getEventName(), "bar_event_1");
        Assert.assertEquals(remoteFunctions1.get(1).getEventName(), "bar_event_2");

    }

    @Test(
            description = "Test the functionality of the extract function " +
                    "when the Async API spec does not contains the x-ballerina-event-type attribute in the channel",
            expectedExceptions = BallerinaAsyncApiException.class,
            expectedExceptionsMessageRegExp = "Could not find the x-ballerina-event-type attribute " +
                    "in the message of the channel events/foo/1"
    )
    public void testExecuteWithMissingXEventTypeWithOneOf() throws BallerinaAsyncApiException {
        String asyncApiSpecStr = fileRepository
                .getFileContentFromResources("specs/spec-single-channel-missing-x-event-type-with-oneof.yml");
        String asyncApeSpecJson = fileRepository.convertYamlToJson(asyncApiSpecStr);
        AaiDocument asyncApiSpec = (Aai20Document) Library.readDocumentFromJSONString(asyncApeSpecJson);
        ExtractUseCase extractServiceTypes = new ExtractServiceTypesFromSpec(asyncApiSpec);
        extractServiceTypes.extract();
    }

    @Test(
            description = "Test the functionality of the extract function " +
                    "when the Async API spec does not contains the x-ballerina-event-type attribute in the channel",
            expectedExceptions = BallerinaAsyncApiException.class,
            expectedExceptionsMessageRegExp = "Could not find the x-ballerina-event-type attribute " +
                    "in the message of the channel events/foo/1"
    )
    public void testExecuteWithMissingXEventTypeWithoutOneOf() throws BallerinaAsyncApiException {
        String asyncApiSpecStr = fileRepository.getFileContentFromResources(
                "specs/spec-single-channel-missing-x-event-type-without-oneof.yml");
        String asyncApeSpecJson = fileRepository.convertYamlToJson(asyncApiSpecStr);
        AaiDocument asyncApiSpec = (Aai20Document) Library.readDocumentFromJSONString(asyncApeSpecJson);
        ExtractUseCase extractServiceTypes = new ExtractServiceTypesFromSpec(asyncApiSpec);
        extractServiceTypes.extract();
    }
}
