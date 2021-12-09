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

/**
 * Test the functionality and flow control of the data extraction process.
 */
public class SchemaControllerTest {
    FileRepository fileRepository = new FileRepositoryImpl();

    @Test(description = "Test the functionality of the 'generateBalCode' function")
    public void testGenerateBalCode() throws BallerinaAsyncApiException {
        String asyncApiSpecStr = fileRepository
                .getFileContentFromResources("specs/spec-complete-slack.yml");
        String asyncApiSpecJson = fileRepository.convertYamlToJson(asyncApiSpecStr);
        SpecController specController = new AsyncApiSpecController(asyncApiSpecJson);


        String dataTypesTemplate = fileRepository
                .getFileContentFromResources(Constants.DATA_TYPES_BAL_FILE_NAME);
        String schemaResult = fileRepository
                .getFileContentFromResources("expected_gen/".concat(Constants.DATA_TYPES_BAL_FILE_NAME));
        BalController schemaController = new SchemaController(specController.getSchemas());
        Assert.assertEquals(schemaController.generateBalCode(dataTypesTemplate), schemaResult);
    }
}
