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

import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Ballerina header conversion to AsyncAPI will test in this class.
 */
public class HeaderTests {
    private static final Path RES_DIR = Paths.get("src/test/resources/websockets" +
            "/ballerina-to-asyncapi/headers").toAbsolutePath();
    private Path tempDir;

    @BeforeMethod
    public void setup() throws IOException {
        this.tempDir = Files.createTempDirectory("bal-to-asyncapi-test-out-" + System.nanoTime());
    }

    @Test(description = "Generate AsyncAPI spec with header type parameter")
    public void testHeadscenario01() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("header_scenario01.bal");
        TestUtils.compareWithGeneratedFile(ballerinaFilePath, "headers/header_scenario01.yaml");
    }

    @Test(description = "Generate AsyncAPI spec with header type parameter with annotation values")
    public void testHeadersWithAnnotation() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("header_scenario02.bal");
        TestUtils.compareWithGeneratedFile(ballerinaFilePath, "headers/header_scenario02.yaml");
    }

    @Test(description = "Generate AsyncAPI spec with header type parameter without curly brace")
    public void testHeadersWithOutCurlyBrace() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("header_scenario03.bal");
        TestUtils.compareWithGeneratedFile(ballerinaFilePath, "headers/header_scenario03.yaml");
    }

    @Test(description = "Generate AsyncAPI spec with for multiple headers")
    public void testWithMultipleHeaders() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("header_scenario04.bal");
        TestUtils.compareWithGeneratedFile(ballerinaFilePath, "headers/header_scenario04.yaml");
    }

    @Test(description = "Generate AsyncAPI spec with for optional headers")
    public void testOptionalHeaders() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("header_scenario05.bal");
        TestUtils.compareWithGeneratedFile(ballerinaFilePath, "headers/header_scenario05.yaml");
    }

    //TODO : Uncomment this after created treatNilableAsOptional
//    @Test(description = "Generate AsyncAPI spec with when the service config has nullable and optional enable field")
//    public void testHeadersWithAnnotations() throws IOException {
//        Path ballerinaFilePath = RES_DIR.resolve("header_scenario06.bal");
//        TestUtils.compareWithGeneratedFile(ballerinaFilePath, "header_scenario06.yaml");
//    }

    @Test(description = "Generate AsyncAPI spec when the header has defaultable parameter")
    public void testHeadersWithDefaultValue() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("header_scenario07.bal");
        TestUtils.compareWithGeneratedFile(ballerinaFilePath, "headers/header_scenario07.yaml");
    }

    @Test(description = "Generate AsyncAPI spec when the header has defaultable parameter with nullable enable data " +
            "type")
    public void testHeadersWithDefaultValueWithNullable() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("header_scenario08.bal");
        TestUtils.compareWithGeneratedFile(ballerinaFilePath, "headers/header_scenario08.yaml");
    }

    @Test(description = "Generate AsyncAPI spec when the header has defaultable parameter with nullable enable data " +
            "type and service config enable", enabled = false)
    public void testHeadersWithDefaultValueWithNullableServiceConfig() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("header_scenario09.bal");
        TestUtils.compareWithGeneratedFile(ballerinaFilePath, "headers/header_scenario09.yaml");
    }

    @Test(description = "Tests for header parameter has default value as expression")
    public void testHeaderWithDefaultExpressionValue() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("header_scenario10.bal");
        TestUtils.compareWithGeneratedFile(ballerinaFilePath, "headers/header_scenario10.yaml");
    }

    @Test(description = "Tests for header parameters having int and boolean types with array, default and nullable " +
            "scenarios")
    public void testHeaderWithIntAndBoolean() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("header_scenario11.bal");
        TestUtils.compareWithGeneratedFile(ballerinaFilePath, "headers/header_scenario11.yaml");
    }

    @Test(description = "Test for header parameters having boolean array type")
    public void testBooleanHeaderArray() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("header_scenario12.bal");
        TestUtils.compareWithGeneratedFile(ballerinaFilePath, "headers/header_scenario12.yaml");
    }

    @Test(description = "Generate AsyncAPI spec with header type parameter")
    public void testHeadScenario13() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("header_scenario13.bal");
        TestUtils.compareWithGeneratedFile(ballerinaFilePath, "headers/header_scenario13.yaml");
    }
    @Test(description = "Generate AsyncAPI spec with header type parameter with annotation values")
    public void testHeadScenario14() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("header_scenario14.bal");
        TestUtils.compareWithGeneratedFile(ballerinaFilePath, "headers/header_scenario14.yaml");
    }
    @Test(description = "Generate AsyncAPI spec with header type parameter with annotation values")
    public void testHeadScenario15() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("header_scenario15.bal");
        TestUtils.compareWithGeneratedFile(ballerinaFilePath, "headers/header_scenario15.yaml");
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
