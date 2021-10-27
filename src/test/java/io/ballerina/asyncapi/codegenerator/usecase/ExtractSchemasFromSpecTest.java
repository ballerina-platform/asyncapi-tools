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
import io.ballerina.asyncapi.codegenerator.entity.Schema;
import io.ballerina.asyncapi.codegenerator.repository.FileRepository;
import io.ballerina.asyncapi.codegenerator.repository.FileRepositoryImpl;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * Test the extraction of schemas from the AsyncAPI specification.
 */
public class ExtractSchemasFromSpecTest {
    FileRepository fileRepository = new FileRepositoryImpl();

    @Test(
            description = "Test the functionality of the extract function " +
                    "when the Async API spec contains only one schema"
    )
    public void testExtractWithOneSchema() throws BallerinaAsyncApiException {
        String asyncApiSpecStr = fileRepository
                .getFileContentFromResources("specs/spec-single-schema.yml");
        String asyncApiSpecJson = fileRepository.convertYamlToJson(asyncApiSpecStr);
        AaiDocument asyncApiSpec = (Aai20Document) Library.readDocumentFromJSONString(asyncApiSpecJson);
        Extractor extractSchemasFromSpec = new ExtractSchemasFromSpec(asyncApiSpec);
        Map<String, Schema> schemas = extractSchemasFromSpec.extract();

        Assert.assertEquals(schemas.get("GenericEventWrapper").getTitle(),
                "Standard event wrapper for the Events API");
        Assert.assertEquals(schemas.get("GenericEventWrapper").getSchemaProperties().get("api_app_id").getType(),
                "string");
        Assert.assertEquals(schemas.get("GenericEventWrapper")
                        .getSchemaProperties().get("event").getSchemaProperties().get("type").getType(),
                "string");
        Assert.assertEquals(schemas.get("GenericEventWrapper")
                        .getSchemaProperties().get("event").getSchemaProperties().get("type").getType(),
                "string");
        Assert.assertEquals(schemas.get("GenericEventWrapper")
                        .getSchemaProperties().get("event").getSchemaProperties().get("type").getTitle(),
                "The specific name of the event");
    }

    @Test(
            description = "Test the functionality of the extract function " +
                    "when the Async API spec contains only one schema"
    )
    public void testExtractWithMultipleSchemas() throws BallerinaAsyncApiException {
        String asyncApiSpecStr = fileRepository
                .getFileContentFromResources("specs/spec-multiple-schemas.yml");
        String asyncApiSpecJson = fileRepository.convertYamlToJson(asyncApiSpecStr);
        AaiDocument asyncApiSpec = (Aai20Document) Library.readDocumentFromJSONString(asyncApiSpecJson);
        Extractor extractSchemasFromSpec = new ExtractSchemasFromSpec(asyncApiSpec);
        Map<String, Schema> serviceTypes = extractSchemasFromSpec.extract();

        Assert.assertEquals(serviceTypes.get("GenericEventWrapper").getTitle(),
                "Standard event wrapper for the Events API");
        Assert.assertEquals(serviceTypes.get("GenericEventWrapper").getSchemaProperties().get("api_app_id").getType(),
                "string");
        Assert.assertEquals(serviceTypes.get("GenericEventWrapper")
                        .getSchemaProperties().get("event").getSchemaProperties().get("type").getType(),
                "string");
        Assert.assertEquals(serviceTypes.get("GenericEventWrapper")
                        .getSchemaProperties().get("event").getSchemaProperties().get("type").getType(),
                "string");
        Assert.assertEquals(serviceTypes.get("GenericEventWrapper")
                        .getSchemaProperties().get("event").getSchemaProperties().get("type").getTitle(),
                "The specific name of the event");


        Assert.assertEquals(serviceTypes.get("CustomTestSchema")
                .getSchemaProperties().get("test_id").getType(), "string");
        Assert.assertEquals(serviceTypes.get("GenericEventWrapper")
                        .getSchemaProperties().get("custom_test_schema").getRef(),
                "#/components/schemas/CustomTestSchema");
    }
}
