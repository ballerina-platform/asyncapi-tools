/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package io.ballerina.asyncapi.wsgenerators.asyncapi;

import io.ballerina.asyncapi.cmd.websockets.BallerinaToAsyncAPIGenerator;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants.ASYNC_API_SUFFIX;
import static io.ballerina.asyncapi.wsgenerators.asyncapi.TestUtils.deleteDirectory;

/**
 * This test class contains the service nodes related special scenarios.
 */
public class AdvanceTests {
    private static final Path RES_DIR = Paths.get("src/test/resources/websockets" +
            "/ballerina-to-asyncapi").toAbsolutePath();
    private Path tempDir;

    private static String getStringFromGivenBalFile(Path expectedServiceFile, String s) throws IOException {
        Stream<String> expectedServiceLines = Files.lines(expectedServiceFile.resolve(s));
        String expectedServiceContent = expectedServiceLines.collect(Collectors.joining("\n"));
        expectedServiceLines.close();
        return expectedServiceContent;
    }

    public static String findFile(Path dir, String dirName) {
        FilenameFilter fileNameFilter = (dir1, name) -> name.startsWith(dirName);
        String[] fileNames = Objects.requireNonNull(dir.toFile().list(fileNameFilter));
        return fileNames.length > 0 ? fileNames[0] : null;
    }

    @BeforeMethod
    public void setup() throws IOException {
        this.tempDir = Files.createTempDirectory("bal-to-asyncapi-test-out-" + System.nanoTime());
    }

    @Test(description = "Multiple META-INF.services with same absolute path")
    public void multipleServiceWithSameAbsolute() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("advance/multiple_services.bal");
        executeMethod(ballerinaFilePath, "multiple_service_01.yaml", String.format("hello%s.yaml",
                ASYNC_API_SUFFIX), "hello_");
    }

    @Test(description = "Multiple META-INF.services with absolute path as '/'. ")
    public void multipleServiceWithOutAbsolute() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("advance/multiple_services_without_base_path.bal");
        executeMethod(ballerinaFilePath, "multiple_service_02.yaml", String.format("multiple_services" +
                "_without_base_path%s.yaml", ASYNC_API_SUFFIX), "multiple_services_without" +
                "_base_path_");
    }

    @Test(description = "Multiple META-INF.services with no absolute path")
    public void multipleServiceNoBasePath() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("advance/multiple_services_no_base_path.bal");
        executeMethod(ballerinaFilePath, "multiple_service_03.yaml", String.format("multiple_services" +
                "_no_base_path%s.yaml", ASYNC_API_SUFFIX), "multiple_services_no_base_path_");
    }


    @Test(description = "When graphql protocol use as a sub-protocol over websocket")
    public void testGraphqlOverWebsocket() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("advance/graphql_over_websocket.bal");
        //Compare generated yaml file with expected yaml content
        TestUtils.compareWithGeneratedFile(ballerinaFilePath, "advance/graphql_over_websocket.yaml");
    }

    private void executeMethod(Path ballerinaFilePath, String yamlFile, String generatedYamlFile,
                               String secondGeneratedFile) throws IOException {
        Path tempDir = Files.createTempDirectory("bal-to-asyncapi-test-out-" + System.nanoTime());
        try {
            String expectedYamlContent = getStringFromGivenBalFile(RES_DIR.resolve("yaml_outputs/service"),
                    yamlFile);
            BallerinaToAsyncAPIGenerator asyncApiConverter = new BallerinaToAsyncAPIGenerator();
            asyncApiConverter.generateAsyncAPIDefinitionsAllService(ballerinaFilePath, tempDir,
                    null, false);

            if (Files.exists(tempDir.resolve(generatedYamlFile)) && findFile(tempDir, secondGeneratedFile) != null) {
                String generatedYaml = getStringFromGivenBalFile(tempDir, generatedYamlFile);
                generatedYaml = (generatedYaml.trim()).replaceAll("\\s+", "");
                expectedYamlContent = (expectedYamlContent.trim()).replaceAll("\\s+", "");
                Assert.assertTrue(generatedYaml.contains(expectedYamlContent));
            } else {
                Assert.fail("Yaml was not generated, Ballerina service file might have compilation errors");
            }
        } catch (IOException e) {
            Assert.fail("Error while generating the service. " + e.getMessage());
        } finally {
            deleteDirectory(tempDir);
            System.gc();
        }
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
