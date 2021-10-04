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
import io.ballerina.asyncapi.codegenerator.repository.FileRepository;
import io.ballerina.asyncapi.codegenerator.repository.FileRepositoryImpl;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

public class TestExtractServiceTypesFromSpec {
    FileRepository fileRepository = new FileRepositoryImpl();

    @Test(
            description = "Test the functionality of the execute function " +
                    "when the Async API spec contains only one channel"
    )
    public void testExecuteWithOneChannel() throws BallerinaAsyncApiException {
        var asyncApiSpecStr = fileRepository
                .getFileContentFromResources("specs/spec-single-channel.yml");
        var asyncApeSpecJson = fileRepository.convertYamlToJson(asyncApiSpecStr);
        AaiDocument asyncApiSpec = (Aai20Document) Library.readDocumentFromJSONString(asyncApeSpecJson);
        UseCase extractServiceTypes = new ExtractServiceTypesFromSpec(asyncApiSpec);
        Map<String, List<String>> serviceTypes = extractServiceTypes.execute();
        Assert.assertTrue(serviceTypes.containsKey("FooService"));

        List<String> events = serviceTypes.get("FooService");
        Assert.assertEquals(events.get(0), "bar_event_1");
        Assert.assertEquals(events.get(1), "bar_event_2");
    }

    @Test(
            description = "Test the functionality of the execute function " +
                    "when the Async API spec contains multiple channels"
    )
    public void testExecuteWithMultipleChannels() throws BallerinaAsyncApiException {
        var asyncApiSpecStr = fileRepository
                .getFileContentFromResources("specs/spec-multiple-channels.yml");
        var asyncApeSpecJson = fileRepository.convertYamlToJson(asyncApiSpecStr);
        AaiDocument asyncApiSpec = (Aai20Document) Library.readDocumentFromJSONString(asyncApeSpecJson);
        UseCase extractServiceTypes = new ExtractServiceTypesFromSpec(asyncApiSpec);
        Map<String, List<String>> serviceTypes = extractServiceTypes.execute();
        Assert.assertTrue(serviceTypes.containsKey("FooService1"));
        Assert.assertTrue(serviceTypes.containsKey("FooService2"));
        Assert.assertTrue(serviceTypes.containsKey("FooService3"));

        List<String> events1 = serviceTypes.get("FooService1");
        Assert.assertEquals(events1.get(0), "bar_1_event_1");
        Assert.assertEquals(events1.get(1), "bar_1_event_2");

        List<String> events2 = serviceTypes.get("FooService2");
        Assert.assertEquals(events2.get(0), "bar_2_event_1");
        Assert.assertEquals(events2.get(1), "bar_2_event_2");

        List<String> events3 = serviceTypes.get("FooService3");
        Assert.assertEquals(events3.get(0), "bar_3_event_1");
    }

    @Test(
            description = "Test the functionality of the execute function " +
                    "when the Async API spec does not contains the x-ballerina-service-type attribute in the channel"
    )
    public void testExecuteWithMissingXServiceType() throws BallerinaAsyncApiException {
        var asyncApiSpecStr = fileRepository
                .getFileContentFromResources("specs/spec-single-channel-missing-x-service-type.yml");
        var asyncApeSpecJson = fileRepository.convertYamlToJson(asyncApiSpecStr);
        AaiDocument asyncApiSpec = (Aai20Document) Library.readDocumentFromJSONString(asyncApeSpecJson);
        UseCase extractServiceTypes = new ExtractServiceTypesFromSpec(asyncApiSpec);
        Map<String, List<String>> serviceTypes = extractServiceTypes.execute();
        Assert.assertTrue(serviceTypes.containsKey("EventsFoo1Service"));

        List<String> events = serviceTypes.get("EventsFoo1Service");
        Assert.assertEquals(events.get(0), "bar_event_1");
        Assert.assertEquals(events.get(1), "bar_event_2");

    }

    @Test(
            description = "Test the functionality of the execute function " +
                    "when the Async API spec does not contains the x-ballerina-event-type attribute in the channel",
            expectedExceptions = BallerinaAsyncApiException.class,
            expectedExceptionsMessageRegExp = "Could not find the x-ballerina-event-type attribute " +
                    "in the message of the channel events/foo/1"
    )
    public void testExecuteWithMissingXEventTypeWithOneOf() throws BallerinaAsyncApiException {
        var asyncApiSpecStr = fileRepository
                .getFileContentFromResources("specs/spec-single-channel-missing-x-event-type-with-oneof.yml");
        var asyncApeSpecJson = fileRepository.convertYamlToJson(asyncApiSpecStr);
        AaiDocument asyncApiSpec = (Aai20Document) Library.readDocumentFromJSONString(asyncApeSpecJson);
        UseCase extractServiceTypes = new ExtractServiceTypesFromSpec(asyncApiSpec);
        extractServiceTypes.execute();
    }

    @Test(
            description = "Test the functionality of the execute function " +
                    "when the Async API spec does not contains the x-ballerina-event-type attribute in the channel",
            expectedExceptions = BallerinaAsyncApiException.class,
            expectedExceptionsMessageRegExp = "Could not find the x-ballerina-event-type attribute " +
                    "in the message of the channel events/foo/1"
    )
    public void testExecuteWithMissingXEventTypeWithoutOneOf() throws BallerinaAsyncApiException {
        var asyncApiSpecStr = fileRepository.getFileContentFromResources(
                "specs/spec-single-channel-missing-x-event-type-without-oneof.yml");
        var asyncApeSpecJson = fileRepository.convertYamlToJson(asyncApiSpecStr);
        AaiDocument asyncApiSpec = (Aai20Document) Library.readDocumentFromJSONString(asyncApeSpecJson);
        UseCase extractServiceTypes = new ExtractServiceTypesFromSpec(asyncApiSpec);
        extractServiceTypes.execute();
    }
}
