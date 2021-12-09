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

package io.ballerina.asyncapi.codegenerator.application;

import io.ballerina.asyncapi.codegenerator.configuration.BallerinaAsyncApiException;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test the functionality of the Code Generator.
 */
public class CodeGeneratorTest {
    @Test(description = "Test the functionality of the 'generate' function")
    public void testGenerate() throws BallerinaAsyncApiException {
        MockFileRepositoryImpl mockRepository = new MockFileRepositoryImpl();
        CodeGenerator codeGenerator = new CodeGenerator(mockRepository);
        codeGenerator.generate("test.yaml", "/");

        Assert.assertEquals(mockRepository.getWriteCount(), 4);
    }

    @Test(description = "Test the functionality of the generate function " +
            "when AsyncAPI spec file has a different file extension than .yaml, .yml or .json",
            expectedExceptions = BallerinaAsyncApiException.class,
            expectedExceptionsMessageRegExp =
                    "Unknown file type: test.ext")
    public void testGenerateWithInvalidFileExtension() throws BallerinaAsyncApiException {
        MockFileRepositoryImpl mockRepository = new MockFileRepositoryImpl();
        CodeGenerator codeGenerator = new CodeGenerator(mockRepository);
        codeGenerator.generate("test.ext", "/");
    }
}
