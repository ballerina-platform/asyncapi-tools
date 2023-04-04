/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
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

package io.ballerina.asyncapi.generators.asyncapi;

import io.ballerina.asyncapi.cli.AsyncAPIContractGenerator;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


import static io.ballerina.asyncapi.core.generators.asyncspec.utils.ConverterCommonUtils.unescapeIdentifier;
import static io.ballerina.asyncapi.generators.asyncapi.TestUtils.*;
import static io.ballerina.asyncapi.generators.common.TestUtils.getStringFromGivenBalFile;


/**
 * Ballerina conversion to AsyncApi will test in this class.
 */
public class AsyncApiConverterUtilsTest {
    private static final Path RES_DIR = Paths.get("src/test/resources/ballerina-to-asyncapi").toAbsolutePath();
    private Path tempDir;

    @BeforeMethod
    public void setup() throws IOException {
        this.tempDir = Files.createTempDirectory("bal-to-asyncapi-test-out-" + System.nanoTime());
    }

    @Test(description = "Generate AsyncAPI spec")
    public void testBasicServices() {
        Path ballerinaFilePath = RES_DIR.resolve("service/basic_service.bal");
        AsyncAPIContractGenerator asyncApiConverterUtils = new AsyncAPIContractGenerator();
        asyncApiConverterUtils.generateAsyncAPIDefinitionsAllService(ballerinaFilePath, this.tempDir, null,
                false);

        Assert.assertTrue(Files.exists(this.tempDir.resolve("hello_asyncapi.yaml")));
        Assert.assertTrue(Files.exists(this.tempDir.resolve("hello02_asyncapi.yaml")));
    }

    @Test(description = "Generate AsyncAPI spec by filtering non existing service")
    public void testBasicServicesWithInvalidServiceName() {
        Path ballerinaFilePath = RES_DIR.resolve("service/basic_service.bal");
        AsyncAPIContractGenerator asyncApiConverter = new AsyncAPIContractGenerator();
        asyncApiConverter.generateAsyncAPIDefinitionsAllService(ballerinaFilePath, this.tempDir, "/abc",
                false);
        Assert.assertFalse(asyncApiConverter.getErrors().isEmpty());
        Assert.assertEquals(asyncApiConverter.getErrors().get(0).getMessage(), "No Ballerina META-INF.services found " +
                "with name '/abc' to generate an AsyncAPI specification. These META-INF.services are available in " +
                "ballerina file. [/hello, /hello02]");
    }

    @Test(description = "Test if invalid 'exampleSetFlag' attribute is coming it the generated spec")
    public void testIfExampleSetFlagContains() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("service/basic_service.bal");
        AsyncAPIContractGenerator asyncApiConverter = new AsyncAPIContractGenerator();
        asyncApiConverter.generateAsyncAPIDefinitionsAllService(ballerinaFilePath, this.tempDir, null,
                false);

        Assert.assertTrue(Files.exists(this.tempDir.resolve("hello_asyncapi.yaml")));
        Assert.assertFalse(Files.readString(this.tempDir.resolve("hello_asyncapi.yaml")).contains("exampleSetFlag"));
    }

    @Test(description = "Generate AsyncAPI spec by filtering service name")
    public void testBasicServicesByFiltering() {
        Path ballerinaFilePath = RES_DIR.resolve("service/basic_service.bal");
        AsyncAPIContractGenerator asyncApiConverter = new AsyncAPIContractGenerator();
        asyncApiConverter.generateAsyncAPIDefinitionsAllService(ballerinaFilePath, this.tempDir,
                "/hello02", false);

        Assert.assertFalse(Files.exists(this.tempDir.resolve("hello_asyncapi.yaml")));
        Assert.assertTrue(Files.exists(this.tempDir.resolve("hello02_asyncapi.yaml")));
    }

    @Test(description = "Generate AsyncAPI spec with complex base paths")
    public void testComplexBasePathServices() {
        Path ballerinaFilePath = RES_DIR.resolve("service/complex_base_path.bal");
        AsyncAPIContractGenerator asyncApiConverter = new AsyncAPIContractGenerator();
        asyncApiConverter.generateAsyncAPIDefinitionsAllService(ballerinaFilePath, this.tempDir, null,
                false);

        Assert.assertTrue(Files.exists(this.tempDir.resolve("hello_foo_bar_asyncapi.yaml")));
        Assert.assertTrue(Files.exists(this.tempDir.resolve("hello02_bar_baz_asyncapi.yaml")));
    }

    @Test(description = "Generate AsyncAPI spec with no base path")
    public void testServicesWithNoBasePath() {
        Path ballerinaFilePath = RES_DIR.resolve("service/no_base_path_service.bal");
        AsyncAPIContractGenerator asyncApiConverter = new AsyncAPIContractGenerator();
        asyncApiConverter.generateAsyncAPIDefinitionsAllService(ballerinaFilePath, this.tempDir, null,
                false);
        Assert.assertTrue(Files.exists(this.tempDir.resolve("no_base_path_service_asyncapi.yaml")));
    }

    @Test(description = "Generate AsyncAPI spec with no base path")
    public void testServicesWithNoBasePathWithFilterina() {
        Path ballerinaFilePath = RES_DIR.resolve("service/no_base_path_service.bal");
        AsyncAPIContractGenerator asyncApiConverter = new AsyncAPIContractGenerator();
        asyncApiConverter.generateAsyncAPIDefinitionsAllService(ballerinaFilePath, this.tempDir, "/",
                false);
        Assert.assertTrue(Files.exists(this.tempDir.resolve("no_base_path_service_asyncapi.yaml")));
    }

    @Test(description = "Generate AsyncAPI spec for build project")
    public void testRecordFieldPayLoad() {
        Path ballerinaFilePath = RES_DIR.resolve("service/project_bal/record_request_service.bal");
        AsyncAPIContractGenerator asyncApiConverter = new AsyncAPIContractGenerator();
        asyncApiConverter.generateAsyncAPIDefinitionsAllService(ballerinaFilePath, this.tempDir, null,
                false);
        Assert.assertTrue(Files.exists(this.tempDir.resolve("payloadV_asyncapi.yaml")));
    }

    @Test(description = "Generate AsyncAPI spec for given ballerina file has only compiler warning")
    public void testForCompilerWarning() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("service/compiler_warning.bal");
        compareWithGeneratedFile(ballerinaFilePath, "service/compiler_warning.yaml");
    }

    @Test(description = "Test for non websocket rabbitmq service")
    public void testForNonWebsocketRabbitMqServices() {
        Path ballerinaFilePath = RES_DIR.resolve("service/rabbitmq_service.bal");
        new AsyncAPIContractGenerator().generateAsyncAPIDefinitionsAllService(ballerinaFilePath, tempDir, null
                , false);
        Assert.assertTrue(!Files.exists(tempDir.resolve("query_asyncapi.yaml")));
    }

    @Test(description = "Test for non websocket kafka service")
    public void testForNonWebsocketKafkaServices() {
        Path ballerinaFilePath = RES_DIR.resolve("service/kafka_service.bal");
        new AsyncAPIContractGenerator().generateAsyncAPIDefinitionsAllService(ballerinaFilePath, tempDir, null
                , false);
        Assert.assertTrue(!Files.exists(tempDir.resolve("query_asyncapi.yaml")));
    }


    //TODO : There was a bug in the lang for escape characters, it is now fixing and try this testing after the issue get fixed https://github.com/ballerina-platform/ballerina-lang/issues/39770
    // I have created the bal file, but yaml file has to be checked and created using debugger
    @Test(description = "Given ballerina service has escape character",enabled = false)
    public void testForRemovingEscapeIdentifier() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("service/escape_identifier.bal");
        Path tempDir = Files.createTempDirectory("bal-to-asyncapi-test-out-" + System.nanoTime());
        try {
            AsyncAPIContractGenerator asyncApiConverter = new AsyncAPIContractGenerator();
            asyncApiConverter.generateAsyncAPIDefinitionsAllService(ballerinaFilePath, tempDir, null
                    , false);
            if (Files.exists(tempDir.resolve("v1_abc_hello_asyncapi.yaml"))) {
                String expectedYamlContent = getStringFromGivenBalFile(RES_DIR.resolve("yaml_outputs/service"),
                        "escape_identifier.yaml");
                String generatedYaml = getStringFromGivenBalFile(tempDir, "v1_abc_hello_asyncapi.yaml");
                generatedYaml = (generatedYaml.trim()).replaceAll("\\s+", "");
                expectedYamlContent = (expectedYamlContent.trim()).replaceAll("\\s+", "");
                Assert.assertTrue(generatedYaml.contains(expectedYamlContent));
            } else {
                Assert.fail("Yaml was not generated");
            }
            if (Files.exists(tempDir.resolve("limit_asyncapi.yaml"))) {
                String expectedYamlContent = getStringFromGivenBalFile(RES_DIR.resolve("yaml_outputs/service"),
                        "escape_identifier_02.yaml");
                String generatedYaml = getStringFromGivenBalFile(tempDir, "limit_asyncapi.yaml");
                generatedYaml = (generatedYaml.trim()).replaceAll("\\s+", "");
                expectedYamlContent = (expectedYamlContent.trim()).replaceAll("\\s+", "");
                Assert.assertTrue(generatedYaml.contains(expectedYamlContent));
            } else {
                Assert.fail("Yaml was not generated");
            }
        } catch (IOException e) {
            Assert.fail("Error while generating the service. " + e.getMessage());
        } finally {
            deleteGeneratedFiles("v1_abc-hello_asyncapi.yaml", tempDir);
            deleteDirectory(tempDir);
            System.gc();
        }
    }

    @Test
    public void testUnescapeIdentifiers() {
        Assert.assertEquals(unescapeIdentifier("'limit"), "limit");
        Assert.assertEquals(unescapeIdentifier("x\\-client"), "x-client");
        Assert.assertEquals(unescapeIdentifier("/'limit"), "/limit");
        Assert.assertEquals(unescapeIdentifier("/'limit/x\\-cl"), "/limit/x-cl");
        Assert.assertEquals(unescapeIdentifier("'พิมพ์ชื่อ"), "พิมพ์ชื่อ");
    }

    @Test
    public void testDecodeIdentifier() {
        Assert.assertEquals(unescapeIdentifier("ชื่\\u{E2D}"), "ชื่อ");
    }

    @AfterMethod
    public void cleanUp() {
        deleteDirectory(this.tempDir);
    }

    @AfterTest
    public void clean() {

        System.setErr(null);
        System.setOut(null);
    }
}
