/*
 *  Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package io.ballerina.asyncapi.wsgenerators.asyncapi;

import io.ballerina.asyncapi.cmd.websockets.BallerinaToAsyncAPIGenerator;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This test class for the covering the unit tests for return type scenarios.
 */
public class ResponseTests {
    private static final Path RES_DIR = Paths.get("src/test/resources/websockets/" +
            "ballerina-to-asyncapi").toAbsolutePath();
    private Path tempDir;

    @BeforeMethod
    public void setup() throws IOException {
        this.tempDir = Files.createTempDirectory("bal-to-asyncapi-test-out-" + System.nanoTime());
    }



    @Test(description = "Response scenario01 without return type")
    public void testResponse02() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("response/rs_scenario01_json.bal");
        TestUtils.compareWithGeneratedFile(ballerinaFilePath, "response/rs_scenario01_json.yaml");
    }

    @Test(description = "Response scenario02 - return type with Record")
    public void testResponse03() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("response/rs_scenario02_simple_name_reference_record.bal");
        TestUtils.compareWithGeneratedFile(ballerinaFilePath,
                "response/rs_scenario02_simple_name_reference_record.yaml");
    }

    @Test(description = "Response scenario 03 - Array type response with a schema")
    public void testResponse10() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("response/rs_scenario03_builtin_array.bal");
        TestUtils.compareWithGeneratedFile(ballerinaFilePath, "response/rs_scenario03_builtin_array.yaml");
    }

    @Test(description = "When the return type is string")
    public void testStringReturn() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("response/rs_scenario04_builtin.bal");
        //Compare generated yaml file with expected yaml content
        TestUtils.compareWithGeneratedFile(ballerinaFilePath, "response/rs_scenario04_builtin.yaml");
    }

    //re-enable after issue #6583 is fixed
    @Test(enabled = false, description = "When the return type is inline record")
    public void testInlineRecordReturn() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("response/rs_scenario05_inline_record_builtin.bal");
        //Compare generated yaml file with expected yaml content
        TestUtils.compareWithGeneratedFile(ballerinaFilePath,
                "response/rs_scenario05_inline_record_builtin.yaml");
    }

    @Test(description = "When the return type is xml")
    public void testReturnTypeXml() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("response/rs_scenario06_xml.bal");
        //Compare generated yaml file with expected yaml content
        TestUtils.compareWithGeneratedFile(ballerinaFilePath, "response/rs_scenario06_xml.yaml");
    }

    //re-enable after issue #6583 is fixed
    @Test(enabled = false, description = "When the return type is inline record and type reference")
    public void testInlineRecordHasReference() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("response/rs_scenario07_inline_record_type_reference.bal");
        //Compare generated yaml file with expected yaml content
        TestUtils.compareWithGeneratedFile(ballerinaFilePath,
                "response/rs_scenario07_inline_record_type_reference.yaml");
    }

    @Test(description = "When the response has float return type")
    public void testResponseWithFloatReturnType() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("response/rs_scenario08_float.bal");
        BallerinaToAsyncAPIGenerator asyncAPIConverterUtils = new BallerinaToAsyncAPIGenerator();
        asyncAPIConverterUtils.generateAsyncAPIDefinitionsAllService(ballerinaFilePath, this.tempDir, null
                , false);
        Assert.assertTrue(asyncAPIConverterUtils.getErrors().isEmpty());
        TestUtils.compareWithGeneratedFile(ballerinaFilePath, "response/rs_scenario08_float.yaml");
    }

    @Test(description = "When the response has decimal return type")
    public void testResponseWithDecimalReturnType() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("response/rs_scenario09_decimal.bal");
        BallerinaToAsyncAPIGenerator asyncAPIConverterUtils = new BallerinaToAsyncAPIGenerator();
        asyncAPIConverterUtils.generateAsyncAPIDefinitionsAllService(ballerinaFilePath, this.tempDir, null
                , false);
        Assert.assertTrue(asyncAPIConverterUtils.getErrors().isEmpty());
        TestUtils.compareWithGeneratedFile(ballerinaFilePath, "response/rs_scenario09_decimal.yaml");
    }

    @Test(description = "When the return type is inline record with typeInclusion fields", enabled = false)
    public void testInlineRecordHasTypeInclusionReference() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("response/rs_scenario10_inline_record_type_inclusion.bal");
        //Compare generated yaml file with expected yaml content
        TestUtils.compareWithGeneratedFile(ballerinaFilePath,
                "response/rs_scenario10_inline_record_type_inclusion.yaml");
    }
    @Test(description = "When the return type is record with typeInclusion field")
    public void testTypeInclusion() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("response/rs_scenario11_record_including_type_inclusion.bal");
        //Compare generated yaml file with expected yaml content
        TestUtils.compareWithGeneratedFile(ballerinaFilePath,
                "response/rs_scenario11_record_including_type_inclusion.yaml");
    }

    @Test(description = "When the return type is array record ")
    public void testArrayRecord() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("response/rs_scenario12_array_record.bal");
        //Compare generated yaml file with expected yaml content
        TestUtils.compareWithGeneratedFile(ballerinaFilePath, "response/rs_scenario12_array_record.yaml");
    }

    //re-enable after issue #6583 is fixed
    @Test(enabled = false, description = "When multiple return types are returning")
    public void testReturningMultipleResponses() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("response/rs_scenario13_multiple_builtin_return.bal");
        //Compare generated yaml file with expected yaml content
        TestUtils.compareWithGeneratedFile(ballerinaFilePath,
                "response/rs_scenario13_multiple_builtin_return.yaml");
    }


    @Test(description = "When stream of strings returns")
    public void testReturningStringStream() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("response/rs_scenario14_stream_string_return.bal");
        //Compare generated yaml file with expected yaml content
        TestUtils.compareWithGeneratedFile(ballerinaFilePath,
                "response/rs_scenario14_stream_string_return.yaml");
    }

    @Test(description = "When multiple streams return as union type")
    public void testReturningMultipleTypesOfStreams() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("response/rs_scenario15_multiple_stream_types_return.bal");
        //Compare generated yaml file with expected yaml content
        TestUtils.compareWithGeneratedFile(ballerinaFilePath,
                "response/rs_scenario15_multiple_stream_types_return.yaml");
    }



    @AfterMethod
    public void cleanUp() {
        TestUtils.deleteDirectory(this.tempDir);
    }

    @AfterTest
    public void clean() {
        System.setErr(null);
        System.setOut(null);
    }
}
