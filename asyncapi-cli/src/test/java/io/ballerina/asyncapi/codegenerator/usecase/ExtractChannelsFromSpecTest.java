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
import io.ballerina.asyncapi.codegenerator.entity.MultiChannel;
import io.ballerina.asyncapi.codegenerator.entity.RemoteFunction;
import io.ballerina.asyncapi.codegenerator.entity.ServiceType;
import io.ballerina.asyncapi.codegenerator.repository.FileRepository;
import io.ballerina.asyncapi.codegenerator.repository.FileRepositoryImpl;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Test the extraction of service types from the AsyncAPI specification.
 */
public class ExtractChannelsFromSpecTest {
    FileRepository fileRepository = new FileRepositoryImpl();

    @Test(
            description = "Test the functionality of the extract function " +
                    "when the Async API spec contains only one channel"
    )
    public void testExtractWithOneChannel() throws BallerinaAsyncApiException {
        String asyncApiSpecStr = fileRepository
                .getFileContentFromResources("specs/spec-single-channel.yml");
        String asyncApiSpecJson = fileRepository.convertYamlToJson(asyncApiSpecStr);
        AaiDocument asyncApiSpec = (Aai20Document) Library.readDocumentFromJSONString(asyncApiSpecJson);
        Extractor extractServiceTypes = new ExtractChannelsFromSpec(asyncApiSpec);
        MultiChannel multiChannel = extractServiceTypes.extract();
        List<ServiceType> serviceTypes = multiChannel.getServiceTypes();

        Assert.assertEquals(serviceTypes.get(0).getServiceTypeName(), "FooService");
        List<RemoteFunction> remoteFunctions = serviceTypes.get(0).getRemoteFunctions();
        Assert.assertEquals(remoteFunctions.get(0).getEventName(), "bar_event_1");
        Assert.assertEquals(remoteFunctions.get(1).getEventName(), "bar_event_2");
    }

    @Test(
            description = "Test the functionality of the extract function " +
                    "when the Async API spec contains only one channel, but the message contains inline schemas"
    )
    public void testExtractWithOneChannelWithInlineSchemas() throws BallerinaAsyncApiException {
        String asyncApiSpecStr = fileRepository
                .getFileContentFromResources("specs/spec-single-channel-with-inline-schema.yml");
        String asyncApiSpecJson = fileRepository.convertYamlToJson(asyncApiSpecStr);
        AaiDocument asyncApiSpec = (Aai20Document) Library.readDocumentFromJSONString(asyncApiSpecJson);
        Extractor extractServiceTypes = new ExtractChannelsFromSpec(asyncApiSpec);
        MultiChannel multiChannel = extractServiceTypes.extract();
        List<ServiceType> serviceTypes = multiChannel.getServiceTypes();

        Assert.assertEquals(serviceTypes.get(0).getServiceTypeName(), "FooService");
        List<RemoteFunction> remoteFunctions = serviceTypes.get(0).getRemoteFunctions();
        Assert.assertEquals(remoteFunctions.get(0).getEventName(), "bar_event_1");
    }

    @Test(
            description = "Test the functionality of the extract function " +
                    "when the Async API spec contains multiple channels"
    )
    public void testExtractWithMultipleChannels() throws BallerinaAsyncApiException {
        String asyncApiSpecStr = fileRepository
                .getFileContentFromResources("specs/spec-multiple-channels.yml");
        String asyncApiSpecJson = fileRepository.convertYamlToJson(asyncApiSpecStr);
        AaiDocument asyncApiSpec = (Aai20Document) Library.readDocumentFromJSONString(asyncApiSpecJson);
        Extractor extractServiceTypes = new ExtractChannelsFromSpec(asyncApiSpec);
        MultiChannel multiChannel = extractServiceTypes.extract();
        List<ServiceType> serviceTypes = multiChannel.getServiceTypes();

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
    public void testExtractWithMissingXServiceType() throws BallerinaAsyncApiException {
        String asyncApiSpecStr = fileRepository
                .getFileContentFromResources("specs/spec-single-channel-missing-x-service-type.yml");
        String asyncApiSpecJson = fileRepository.convertYamlToJson(asyncApiSpecStr);
        AaiDocument asyncApiSpec = (Aai20Document) Library.readDocumentFromJSONString(asyncApiSpecJson);
        Extractor extractServiceTypes = new ExtractChannelsFromSpec(asyncApiSpec);
        MultiChannel multiChannel = extractServiceTypes.extract();
        List<ServiceType> serviceTypes = multiChannel.getServiceTypes();

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
    public void testExtractWithMissingXEventTypeWithOneOf() throws BallerinaAsyncApiException {
        String asyncApiSpecStr = fileRepository
                .getFileContentFromResources("specs/spec-single-channel-missing-x-event-type-with-oneof.yml");
        String asyncApiSpecJson = fileRepository.convertYamlToJson(asyncApiSpecStr);
        AaiDocument asyncApiSpec = (Aai20Document) Library.readDocumentFromJSONString(asyncApiSpecJson);
        Extractor extractServiceTypes = new ExtractChannelsFromSpec(asyncApiSpec);
        extractServiceTypes.extract();
    }

    @Test(
            description = "Test the functionality of the extract function " +
                    "when the Async API spec does not contains the x-ballerina-event-type attribute in the channel",
            expectedExceptions = BallerinaAsyncApiException.class,
            expectedExceptionsMessageRegExp = "Could not find the x-ballerina-event-type attribute " +
                    "in the message of the channel events/foo/1"
    )
    public void testExtractWithMissingXEventTypeWithoutOneOf() throws BallerinaAsyncApiException {
        String asyncApiSpecStr = fileRepository.getFileContentFromResources(
                "specs/spec-single-channel-missing-x-event-type-without-oneof.yml");
        String asyncApiSpecJson = fileRepository.convertYamlToJson(asyncApiSpecStr);
        AaiDocument asyncApiSpec = (Aai20Document) Library.readDocumentFromJSONString(asyncApiSpecJson);
        Extractor extractServiceTypes = new ExtractChannelsFromSpec(asyncApiSpec);
        extractServiceTypes.extract();
    }

    @Test(
            description = "Test the functionality of the extract function " +
                    "when the payload does not exist in the AsyncAPI specification",
            expectedExceptions = BallerinaAsyncApiException.class,
            expectedExceptionsMessageRegExp = "Could not find the schema 'GenericEventWrapper' " +
                    "in the the path #/components/schemas"
    )
    public void testExtractWithMissingPayload() throws BallerinaAsyncApiException {
        String asyncApiSpecStr = fileRepository.getFileContentFromResources(
                "specs/spec-single-channel-missing-reference.yml");
        String asyncApiSpecJson = fileRepository.convertYamlToJson(asyncApiSpecStr);
        AaiDocument asyncApiSpec = (Aai20Document) Library.readDocumentFromJSONString(asyncApiSpecJson);
        Extractor extractServiceTypes = new ExtractChannelsFromSpec(asyncApiSpec);
        extractServiceTypes.extract();
    }

    @Test(
            description = "Test the functionality of the extract function " +
                    "when the payload has a null value",
            expectedExceptions = BallerinaAsyncApiException.class,
            expectedExceptionsMessageRegExp = "Could not find the payload reference " +
                    "in the message of the channel events/foo/1"
    )
    public void testExtractWithNullPayload() throws BallerinaAsyncApiException {
        String asyncApiSpecStr = fileRepository.getFileContentFromResources(
                "specs/spec-single-channel-missing-payload.yml");
        String asyncApiSpecJson = fileRepository.convertYamlToJson(asyncApiSpecStr);
        AaiDocument asyncApiSpec = (Aai20Document) Library.readDocumentFromJSONString(asyncApiSpecJson);
        Extractor extractServiceTypes = new ExtractChannelsFromSpec(asyncApiSpec);
        extractServiceTypes.extract();
    }
}
