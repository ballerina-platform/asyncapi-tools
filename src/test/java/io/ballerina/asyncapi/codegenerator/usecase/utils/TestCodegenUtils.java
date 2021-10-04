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

package io.ballerina.asyncapi.codegenerator.usecase.utils;

import io.ballerina.asyncapi.codegenerator.configuration.BallerinaAsyncApiException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class TestCodegenUtils {
    private final CodegenUtils codegenUtils = new CodegenUtils();

    @Test(description = "Test the functionality of the escapeIdentifier function")
    public void testEscapeIdentifier() {
        Map<String, String> testData = new HashMap<>();
        testData.put("type", "'type");
        testData.put("error", "'error");
        testData.put("string?", "'string?");
        testData.put("foo?", "foo?");

        testData.forEach((input, expected) -> {
            String result = codegenUtils.escapeIdentifier(input);
            Assert.assertEquals(result, expected);
        });
    }

    @Test(description = "Test the functionality of the getValidName function")
    public void testGetValidName() {
        Map<String, String> testData1 = new HashMap<>();
        testData1.put("1foo", "1foo");
        testData1.put("foo", "Foo");
        testData1.put("foo#foo#", "FooFoo");
        testData1.put("foo#foo0#", "FooFoo0");

        testData1.forEach((input, expected) -> {
            String result = codegenUtils.getValidName(input, true);
            Assert.assertEquals(result, expected);
        });

        Map<String, String> testData2 = new HashMap<>();
        testData2.put("1foo", "1foo");
        testData2.put("foo", "foo");
        testData2.put("foo#foo#", "fooFoo");
        testData2.put("foo#foo0#", "fooFoo0");

        testData2.forEach((input, expected) -> {
            String result = codegenUtils.getValidName(input, false);
            Assert.assertEquals(result, expected);
        });
    }

    @Test(
            description = "Test the functionality of the extractReferenceType function",
            expectedExceptions = BallerinaAsyncApiException.class,
            expectedExceptionsMessageRegExp = "Invalid reference value: .*"
    )
    public void testExtractReferenceType() throws BallerinaAsyncApiException {
        Map<String, String> testData = new HashMap<>();
        testData.put("#/foo", "foo");
        testData.put("#/foo/bar", "bar");
        testData.put("#/foo/bar foo", "bar\\ foo");

        for (Map.Entry<String, String> entry : testData.entrySet()) {
            Assert.assertEquals(
                    codegenUtils.extractReferenceType(entry.getKey()), entry.getValue());
        }

        // Test the exception
        codegenUtils.extractReferenceType("#foo/bar");
        codegenUtils.extractReferenceType("/foo/bar");
        codegenUtils.extractReferenceType("/foo#/bar");
        codegenUtils.extractReferenceType("/foo/bar#");
    }
}
