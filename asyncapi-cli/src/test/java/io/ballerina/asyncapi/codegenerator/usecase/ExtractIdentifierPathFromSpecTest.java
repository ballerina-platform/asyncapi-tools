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
import io.apicurio.datamodels.models.asyncapi.AsyncApiDocument;
import io.ballerina.asyncapi.codegenerator.configuration.BallerinaAsyncApiException;
import io.ballerina.asyncapi.codegenerator.repository.FileRepository;
import io.ballerina.asyncapi.codegenerator.repository.FileRepositoryImpl;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test the extraction of identifier path from the AsyncAPI specification.
 */
public class ExtractIdentifierPathFromSpecTest {
    FileRepository fileRepository = new FileRepositoryImpl();

    @Test(
            description = "Test the functionality of the extract function " +
                    "when the Async API spec contains the correct x-ballerina-identifier-path attribute in the channel"
    )
    public void testExtractWithIdentifierPath() throws BallerinaAsyncApiException {
        String asyncApiSpecStr = fileRepository
                .getFileContentFromResources("specs/spec-with-identifier-path.yml");
        String asyncApiSpecJson = fileRepository.convertYamlToJson(asyncApiSpecStr);
        AsyncApiDocument asyncApiSpec = (AsyncApiDocument) Library.readDocumentFromJSONString(asyncApiSpecJson);
        Extractor extractIdentifierPathFromSpec = new ExtractIdentifierPathFromSpec(asyncApiSpec);
        String identifierPath = extractIdentifierPathFromSpec.extract();

        Assert.assertEquals(identifierPath, "event.'type");
    }

    @Test(
            description = "Test the functionality of the extract function " +
                    "when the Async API spec contains the x-ballerina-identifier-path attribute in the channel " +
                    "but missing the path attribute inside it",
            expectedExceptions = BallerinaAsyncApiException.class,
            expectedExceptionsMessageRegExp = "path attribute is not found within the attribute " +
                    "x-ballerina-event-identifier in the Async API Specification"
    )
    public void testExtractWithIdentifierPathMissingPath() throws BallerinaAsyncApiException {
        String asyncApiSpecStr = fileRepository
                .getFileContentFromResources("specs/spec-with-identifier-path-missing-path.yml");
        String asyncApiSpecJson = fileRepository.convertYamlToJson(asyncApiSpecStr);
        AsyncApiDocument asyncApiSpec = (AsyncApiDocument) Library.readDocumentFromJSONString(asyncApiSpecJson);
        Extractor extractIdentifierPathFromSpec = new ExtractIdentifierPathFromSpec(asyncApiSpec);
        extractIdentifierPathFromSpec.extract();
    }
}
