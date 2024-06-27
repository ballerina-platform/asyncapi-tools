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

import io.ballerina.asyncapi.cmd.websockets.BallerinaToAsyncApiGenerator;
import io.ballerina.asyncapi.websocketscore.generators.asyncspec.diagnostic.AsyncApiConverterDiagnostic;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.ASYNC_API_SUFFIX;
import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.utils.ConverterCommonUtils.unescapeIdentifier;
import static io.ballerina.asyncapi.wsgenerators.asyncapi.TestUtils.compareWithGeneratedFile;
import static io.ballerina.asyncapi.wsgenerators.asyncapi.TestUtils.deleteDirectory;
import static io.ballerina.asyncapi.wsgenerators.asyncapi.TestUtils.deleteGeneratedFiles;
import static io.ballerina.asyncapi.wsgenerators.common.TestUtils.getStringFromGivenBalFile;


/**
 * Ballerina conversion to AsyncApi will test in this class.
 */
public class AsyncApiConverterUtilsTest {
    private static final Path RES_DIR = Paths.get("src/test/resources/websockets" +
            "/ballerina-to-asyncapi").toAbsolutePath();
    private Path tempDir;
    private PrintStream outStream = System.out;

    @BeforeMethod
    public void setup() throws IOException {
        this.tempDir = Files.createTempDirectory("bal-to-asyncapi-test-out-" + System.nanoTime());
    }

    @Test(description = "Generate AsyncAPI spec")
    public void testBasicServices() {
        Path ballerinaFilePath = RES_DIR.resolve("service/basic_service.bal");
        BallerinaToAsyncApiGenerator.generateAsyncAPIDefinitionsAllService(ballerinaFilePath,
                this.tempDir, null, false, System.out);

        Assert.assertTrue(Files.exists(this.tempDir.resolve(String.format("hello%s.yaml", ASYNC_API_SUFFIX))));
        Assert.assertTrue(Files.exists(this.tempDir.resolve(String.format("hello02%s.yaml", ASYNC_API_SUFFIX))));
    }

    @Test(description = "Generate AsyncAPI spec by filtering non existing service")
    public void testBasicServicesWithInvalidServiceName() {
        Path ballerinaFilePath = RES_DIR.resolve("service/basic_service.bal");
        List<AsyncApiConverterDiagnostic> errors = BallerinaToAsyncApiGenerator
                .generateAsyncAPIDefinitionsAllService(ballerinaFilePath, this.tempDir, "/abc", false, outStream);
        Assert.assertFalse(errors.isEmpty());
        Assert.assertEquals(errors.get(0).getMessage(),
                "No Ballerina service found with name '/abc' to generate an AsyncAPI specification. " +
                        "These are the available services: [/hello, /hello02]");
    }

    @Test(description = "Test if invalid 'exampleSetFlag' attribute is coming it the generated spec")
    public void testIfExampleSetFlagContains() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("service/basic_service.bal");
        BallerinaToAsyncApiGenerator.generateAsyncAPIDefinitionsAllService(ballerinaFilePath, this.tempDir,
                null, false, outStream);

        Assert.assertTrue(Files.exists(this.tempDir.resolve(String.format("hello%s.yaml", ASYNC_API_SUFFIX))));
        Assert.assertFalse(Files.readString(this.tempDir.resolve(String.format("hello%s.yaml",
                ASYNC_API_SUFFIX))).contains("exampleSetFlag"));
    }

    @Test(description = "Generate AsyncAPI spec by filtering service name")
    public void testBasicServicesByFiltering() {
        Path ballerinaFilePath = RES_DIR.resolve("service/basic_service.bal");
        BallerinaToAsyncApiGenerator.generateAsyncAPIDefinitionsAllService(ballerinaFilePath, this.tempDir,
                "/hello02", false, outStream);

        Assert.assertFalse(Files.exists(this.tempDir.resolve(String.format("hello%s.yaml", ASYNC_API_SUFFIX))));
        Assert.assertTrue(Files.exists(this.tempDir.resolve(String.format("hello02%s.yaml", ASYNC_API_SUFFIX))));
    }

    @Test(description = "Generate AsyncAPI spec with complex base paths")
    public void testComplexBasePathServices() {
        Path ballerinaFilePath = RES_DIR.resolve("service/complex_base_path.bal");
        BallerinaToAsyncApiGenerator.generateAsyncAPIDefinitionsAllService(ballerinaFilePath, this.tempDir,
                null, false, outStream);

        Assert.assertTrue(Files.exists(this.tempDir.resolve(String.format("hello_foo_bar%s.yaml",
                ASYNC_API_SUFFIX))));
        Assert.assertTrue(Files.exists(this.tempDir.resolve(String.format("hello02_bar_baz%s.yaml",
                ASYNC_API_SUFFIX))));
    }

    @Test(description = "Generate AsyncAPI spec with no base path")
    public void testServicesWithNoBasePath() {
        Path ballerinaFilePath = RES_DIR.resolve("service/no_base_path_service.bal");
        BallerinaToAsyncApiGenerator.generateAsyncAPIDefinitionsAllService(ballerinaFilePath, this.tempDir,
                null, false, outStream);
        Assert.assertTrue(Files.exists(this.tempDir.resolve(String.format("no_base_path_service%s.yaml",
                ASYNC_API_SUFFIX))));
    }

    @Test(description = "Generate AsyncAPI spec with no base path")
    public void testServicesWithNoBasePathWithFilterina() {
        Path ballerinaFilePath = RES_DIR.resolve("service/no_base_path_service.bal");
        BallerinaToAsyncApiGenerator.generateAsyncAPIDefinitionsAllService(ballerinaFilePath, this.tempDir,
                "/", false, outStream);
        Assert.assertTrue(Files.exists(this.tempDir.resolve(String.format("no_base_path_service%s.yaml",
                ASYNC_API_SUFFIX))));
    }

    @Test(description = "Generate AsyncAPI spec for build project")
    public void testRecordFieldPayLoad() {
        Path ballerinaFilePath = RES_DIR.resolve("service/project_bal/record_request_service.bal");
        BallerinaToAsyncApiGenerator.generateAsyncAPIDefinitionsAllService(ballerinaFilePath, this.tempDir,
                null, false, outStream);
        Assert.assertTrue(Files.exists(this.tempDir.resolve(String.format("payloadV%s.yaml", ASYNC_API_SUFFIX))));
    }

    @Test(description = "Generate AsyncAPI spec for given ballerina file has only compiler warning", enabled = false)
    public void testForCompilerWarning() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("service/compiler_warning.bal");
        compareWithGeneratedFile(ballerinaFilePath, "service/compiler_warning.yaml");
    }

    @Test(description = "Test for non websocket rabbitmq service")
    public void testForNonWebsocketRabbitMqServices() {
        Path ballerinaFilePath = RES_DIR.resolve("service/rabbitmq_service.bal");
        BallerinaToAsyncApiGenerator.generateAsyncAPIDefinitionsAllService(ballerinaFilePath, tempDir,
                null, false, outStream);
        Assert.assertFalse(Files.exists(tempDir.resolve(String.format("query%s.yaml", ASYNC_API_SUFFIX))));
    }

    @Test(description = "Test for non websocket kafka service")
    public void testForNonWebsocketKafkaServices() {
        Path ballerinaFilePath = RES_DIR.resolve("service/kafka_service.bal");
        BallerinaToAsyncApiGenerator.generateAsyncAPIDefinitionsAllService(ballerinaFilePath, tempDir,
                null, false, outStream);
        Assert.assertFalse(Files.exists(tempDir.resolve(String.format("query%s.yaml", ASYNC_API_SUFFIX))));
    }


    @Test(description = "Test for non websocket websubhub service")
    public void testForNonWebsocketWebSubHubServices() {
        Path ballerinaFilePath = RES_DIR.resolve("service/websubhub_service.bal");
        BallerinaToAsyncApiGenerator.generateAsyncAPIDefinitionsAllService(ballerinaFilePath, tempDir,
                null, false, outStream);
        Assert.assertFalse(Files.exists(tempDir.resolve(String.format("query%s.yaml", ASYNC_API_SUFFIX))));
    }

    //TODO : There was a bug in the lang for escape characters,
    // it is now fixing and try this testing after the issue get fixed
    // https://github.com/ballerina-platform/ballerina-lang/issues/39770
    // I have created the bal file, but yaml file has to be checked and created using debugger
    @Test(description = "Given ballerina service has escape character", enabled = false)
    public void testForRemovingEscapeIdentifier() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("service/escape_identifier.bal");
        Path tempDir = Files.createTempDirectory("bal-to-asyncapi-test-out-" + System.nanoTime());
        try {
            BallerinaToAsyncApiGenerator.generateAsyncAPIDefinitionsAllService(ballerinaFilePath, tempDir, null,
                    false, outStream);
            if (Files.exists(tempDir.resolve(String.format("v1_abc_hello%s.yaml", ASYNC_API_SUFFIX)))) {
                String expectedYamlContent = getStringFromGivenBalFile(RES_DIR.resolve("yaml_outputs/service"),
                        "escape_identifier.yaml");
                String generatedYaml = getStringFromGivenBalFile(tempDir, String.format("v1_abc_hello%s.yaml",
                        ASYNC_API_SUFFIX));
                generatedYaml = (generatedYaml.trim()).replaceAll("\\s+", "");
                expectedYamlContent = (expectedYamlContent.trim()).replaceAll("\\s+", "");
                Assert.assertTrue(generatedYaml.contains(expectedYamlContent));
            } else {
                Assert.fail("Yaml was not generated");
            }
            if (Files.exists(tempDir.resolve(String.format("limit%s.yaml", ASYNC_API_SUFFIX)))) {
                String expectedYamlContent = getStringFromGivenBalFile(RES_DIR.resolve("yaml_outputs/service"),
                        "escape_identifier_02.yaml");
                String generatedYaml = getStringFromGivenBalFile(tempDir, String.format("limit%s.yaml",
                        ASYNC_API_SUFFIX));
                generatedYaml = (generatedYaml.trim()).replaceAll("\\s+", "");
                expectedYamlContent = (expectedYamlContent.trim()).replaceAll("\\s+", "");
                Assert.assertTrue(generatedYaml.contains(expectedYamlContent));
            } else {
                Assert.fail("Yaml was not generated");
            }
        } catch (IOException e) {
            Assert.fail("Error while generating the service. " + e.getMessage());
        } finally {
            deleteGeneratedFiles(String.format("v1_abc-hello%s.yaml", ASYNC_API_SUFFIX), tempDir);
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
        Assert.assertEquals(unescapeIdentifier("ชื่\\u{E2D}"),
                "ชื่อ");
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
