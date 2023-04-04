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
package io.ballerina.asyncapi.cmd;

import io.ballerina.asyncapi.cli.AsyncAPICmd;
import io.ballerina.cli.launcher.BLauncherException;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This {@code BallerinaToAsyncAPITests} represents the tests for all the special scenarios in the ballerina to asyncapi
 * command.
 *
 * @since 2.0.0
 */
public class BallerinaToAsyncAPITests extends AsyncAPICommandTest {
    private static final Path RES_DIR = Paths.get("src/test/resources/").toAbsolutePath();

    @BeforeTest(description = "This will create a new ballerina project for testing below scenarios.")
    public void setupBallerinaProject() throws IOException {
        super.setup();
    }

//    @Test(description = "Test asyncapi command with help flag")
//    public void testOpenAPICmdHelp() throws IOException {
//        String[] args = {"-h"};
//        AsyncAPICmd openApiCommand = new AsyncAPICmd(printStream, tmpDir, false);
//        new CommandLine(openApiCommand).parseArgs(args);
//        openApiCommand.execute();
//        String output = readOutput(true);
//        System.out.println(output.toString());
//        Assert.assertTrue(output.contains("NAME\n" +
//                "       ballerina-asyncapi - Generate a Ballerina service"));
//    }

//    @Test(description = "Test openapi command without help flag")
//    public void testOpenAPICmdHelpWithoutFlag() throws IOException {
//        AsyncAPICmd asyncApiCommand = new AsyncAPICmd(printStream, tmpDir, false);
//        new CommandLine(asyncApiCommand);
//        asyncApiCommand.execute();
//        String output = readOutput(true);
//        Assert.assertTrue(output.contains("NAME\n" +
//                "       ballerina-asyncapi - Generate a Ballerina service"));
//    }

    @Test(description = "Test ballerina to asyncApi")
    public void testBallerinaToAsyncAPIGeneration() {
        Path filePath = resourceDir.resolve(Paths.get("cmd/ballerina-to-asyncapi/ballerina-file.bal"));
        String[] args = {"--input", filePath.toString(), "-o", this.tmpDir.toString()};
        AsyncAPICmd cmd = new AsyncAPICmd(printStream, tmpDir, false);
        new CommandLine(cmd).parseArgs(args);

        String output = "";
        try {
            cmd.execute();
        } catch (BLauncherException e) {
            output = e.getDetailedMessages().get(0);
            Assert.fail(output);
        }
    }
    @Test(description = "Without asyncapi annotation ballerina to asyncapi")
    public void asyncapiAnnotationWithOutContract() {
        Path filePath = resourceDir.resolve(Paths.get("cmd/ballerina-to-asyncapi/project_2/service.bal"));
        String[] args = {"--input", filePath.toString(), "-o", this.tmpDir.toString()};
        AsyncAPICmd cmd = new AsyncAPICmd(printStream, tmpDir, false);
        new CommandLine(cmd).parseArgs(args);

        String output = "";
        try {
            cmd.execute();
            output = readOutput(true);
            Path definitionPath = resourceDir.resolve("cmd/ballerina-to-asyncapi/project_2/result.yaml");
            if (Files.exists(this.tmpDir.resolve("service_asyncapi.yaml"))) {
                String generatedAsyncAPI = getStringFromGivenBalFile(this.tmpDir.resolve("service_asyncapi.yaml"));
                String expectedYaml = getStringFromGivenBalFile(definitionPath);
                Assert.assertEquals(expectedYaml, generatedAsyncAPI);
            }
        } catch (BLauncherException | IOException e) {
            output = e.toString();
            Assert.fail(output);
        }
    }


    @Test(description = "AsyncAPI Annotation with ballerina to asyncapi",enabled = false)
    public void asyncapiAnnotationWithContract() {
        Path filePath = resourceDir.resolve(Paths.get("cmd/ballerina-to-asyncapi/project1/service.bal"));
        String[] args = {"--input", filePath.toString(), "-o", this.tmpDir.toString()};
        AsyncAPICmd cmd = new AsyncAPICmd(printStream, tmpDir, false);
        new CommandLine(cmd).parseArgs(args);

        String output = "";
        try {
            cmd.execute();
            output = readOutput(true);
            Path definitionPath = resourceDir.resolve("cmd/ballerina-to-asyncapi/project_1/result.yaml");
            if (Files.exists(this.tmpDir.resolve("service_asyncapi.yaml"))) {
                String generatedAsyncAPI = getStringFromGivenBalFile(this.tmpDir.resolve("service_asyncapi.yaml"));
                String expectedYaml = getStringFromGivenBalFile(definitionPath);
                Assert.assertEquals(expectedYaml, generatedAsyncAPI);

            }
        } catch (BLauncherException | IOException e) {
            output = e.toString();
            Assert.fail(output);
        }
    }


    @Test(description = "AsyncAPI Annotation with ballerina to asyncapi",enabled = false)
    public void asyncapiAnnotationWithoutFields() {
        Path filePath = resourceDir.resolve(Paths.get("cmd/ballerina-to-asyncapi/project_3/service.bal"));
        String[] args = {"--input", filePath.toString(), "-o", this.tmpDir.toString()};
        AsyncAPICmd cmd = new AsyncAPICmd(printStream, tmpDir, false);
        new CommandLine(cmd).parseArgs(args);

        String output = "";
        try {
            cmd.execute();
            output = readOutput(true);
            Path definitionPath = resourceDir.resolve("cmd/ballerina-to-asyncapi/project_3/result.yaml");
            if (Files.exists(this.tmpDir.resolve("service_asyncapi.yaml"))) {
                String generatedAsyncAPI = getStringFromGivenBalFile(this.tmpDir.resolve("service_asyncapi.yaml"));
                String expectedYaml = getStringFromGivenBalFile(definitionPath);
                Assert.assertEquals(expectedYaml, generatedAsyncAPI);
            }
        } catch (BLauncherException | IOException e) {
            output = e.toString();
            Assert.fail(output);
        }
    }

    private String getStringFromGivenBalFile(Path expectedServiceFile) throws IOException {
        Stream<String> expectedServiceLines = Files.lines(expectedServiceFile);
        String expectedServiceContent = expectedServiceLines.collect(Collectors.joining(System.lineSeparator()));
        expectedServiceLines.close();
        return expectedServiceContent.trim().replaceAll("\\s+", "").replaceAll(System.lineSeparator(), "");
    }

    @AfterTest
    public void clean() {
        System.setErr(null);
        System.setOut(null);
    }
}
