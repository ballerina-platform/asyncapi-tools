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

package io.ballerina.asyncapi.cmd;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Test AsyncAPI commands.
 */
public class AsyncApiCmdTest {
    protected Path tmpDir;
    protected PrintStream printStream;
    protected final Path resourceDir = Paths.get("src/test/resources/").toAbsolutePath();
    private ByteArrayOutputStream console;

    @BeforeClass
    public void setup() throws IOException {
        this.tmpDir = Files.createTempDirectory("asyncapi-cmd-test-out-" + System.nanoTime());
        this.console = new ByteArrayOutputStream();
        this.printStream = new PrintStream(this.console);
    }

    @AfterClass
    public void cleanup() throws IOException {
        Files.walk(this.tmpDir)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        Assert.fail(e.getMessage(), e);
                    }
                });
        this.console.close();
        this.printStream.close();
    }

    protected String readOutput(boolean silent) throws IOException {
        String output = "";
        output = this.console.toString();
        this.console.close();
        this.console = new ByteArrayOutputStream();
        this.printStream = new PrintStream(this.console);
        if (!silent) {
            PrintStream out = System.out;
            out.println(output);
        }
        return output;
    }


    @Test(description = "Test the results of a successful asyncapi command execution")
    public void testExecute() throws IOException {
        Path specYaml = resourceDir.resolve(Paths.get("specs", "spec-complete-slack.yml"));
        String[] args = {"--input", specYaml.toString(), "-o", this.tmpDir.toString()};
        AsyncApiCmd cmd = new AsyncApiCmd(printStream, tmpDir, false);
        new CommandLine(cmd).parseArgs(args);
        cmd.execute();
        Path expectedDataTypesFile = resourceDir.resolve(Paths.get("expected_gen", "data_types.bal"));
        Path expectedDispatcherServiceFile = resourceDir.resolve(
                Paths.get("expected_gen", "dispatcher_service.bal"));
        Path expectedListenerFile = resourceDir.resolve(Paths.get("expected_gen", "listener.bal"));
        Path expectedServiceTypesFile = resourceDir.resolve(Paths.get("expected_gen", "service_types.bal"));
        String expectedDataTypesContent = readContent(expectedDataTypesFile);
        String expectedDispatcherServiceContent = readContent(expectedDispatcherServiceFile);
        String expectedListenerContent = readContent(expectedListenerFile);
        String expectedServiceTypesContent = readContent(expectedServiceTypesFile);
        if (Files.exists(this.tmpDir.resolve("listener.bal")) &&
                Files.exists(this.tmpDir.resolve("dispatcher_service.bal")) &&
                Files.exists(this.tmpDir.resolve("data_types.bal")) &&
                Files.exists(this.tmpDir.resolve("service_types.bal"))) {

            String generatedDataTypesContent = readContent(this.tmpDir.resolve("data_types.bal"));
            String generatedDispatcherServiceContent = readContent(this.tmpDir.resolve("dispatcher_service.bal"));
            String generatedListenerContent = readContent(this.tmpDir.resolve("listener.bal"));
            String generatedServiceTypesContent = readContent(this.tmpDir.resolve("service_types.bal"));

            Assert.assertEquals(generatedDataTypesContent, expectedDataTypesContent);
            Assert.assertEquals(generatedDispatcherServiceContent, expectedDispatcherServiceContent);
            Assert.assertEquals(generatedListenerContent, expectedListenerContent);
            Assert.assertEquals(generatedServiceTypesContent, expectedServiceTypesContent);
        } else {
            Assert.fail("Code generation failed. : " + readOutput(true));
        }
    }

    @Test(description = "Test the results of a successful asyncapi command execution")
    public void testExecuteWithJson() throws IOException {
        Path specYaml = resourceDir.resolve(Paths.get("specs", "spec-complete-slack.json"));
        String[] args = {"--input", specYaml.toString(), "-o", this.tmpDir.toString()};
        AsyncApiCmd cmd = new AsyncApiCmd(printStream, tmpDir, false);
        new CommandLine(cmd).parseArgs(args);
        cmd.execute();
        Path expectedDataTypesFile = resourceDir.resolve(Paths.get("expected_gen", "data_types.bal"));
        Path expectedDispatcherServiceFile = resourceDir.resolve(
                Paths.get("expected_gen", "dispatcher_service.bal"));
        Path expectedListenerFile = resourceDir.resolve(Paths.get("expected_gen", "listener.bal"));
        Path expectedServiceTypesFile = resourceDir.resolve(Paths.get("expected_gen", "service_types.bal"));
        String expectedDataTypesContent = readContent(expectedDataTypesFile);
        String expectedDispatcherServiceContent = readContent(expectedDispatcherServiceFile);
        String expectedListenerContent = readContent(expectedListenerFile);
        String expectedServiceTypesContent = readContent(expectedServiceTypesFile);
        if (Files.exists(this.tmpDir.resolve("listener.bal")) &&
                Files.exists(this.tmpDir.resolve("dispatcher_service.bal")) &&
                Files.exists(this.tmpDir.resolve("data_types.bal")) &&
                Files.exists(this.tmpDir.resolve("service_types.bal"))) {

            String generatedDataTypesContent = readContent(this.tmpDir.resolve("data_types.bal"));
            String generatedDispatcherServiceContent = readContent(this.tmpDir.resolve("dispatcher_service.bal"));
            String generatedListenerContent = readContent(this.tmpDir.resolve("listener.bal"));
            String generatedServiceTypesContent = readContent(this.tmpDir.resolve("service_types.bal"));

            Assert.assertEquals(generatedDataTypesContent, expectedDataTypesContent);
            Assert.assertEquals(generatedDispatcherServiceContent, expectedDispatcherServiceContent);
            Assert.assertEquals(generatedListenerContent, expectedListenerContent);
            Assert.assertEquals(generatedServiceTypesContent, expectedServiceTypesContent);
        } else {
            Assert.fail("Code generation failed. : " + readOutput(true));
        }
    }

    @Test(description = "Test the functionality of the asyncapi command when the given input directory is invalid")
    public void testExecuteWithInvalidSpecPath() throws IOException {
        Path specYaml = resourceDir.resolve(Paths.get("specs", "invalid-file-name.yml"));
        String[] args = {"--input", specYaml.toString(), "-o", this.tmpDir.toString()};
        AsyncApiCmd cmd = new AsyncApiCmd(printStream, tmpDir, false);
        new CommandLine(cmd).parseArgs(args);
        cmd.execute();
        Assert.assertTrue(readOutput(true).startsWith("File not found in the given path: "));
    }

    @Test(description = "Test the functionality of the asyncapi command when the input file path is not given")
    public void testExecuteWhenSpecPathNotGiven() throws IOException {
        String[] args = {"--input"};
        AsyncApiCmd cmd = new AsyncApiCmd(printStream, tmpDir, false);
        new CommandLine(cmd).parseArgs(args);
        cmd.execute();
        Assert.assertEquals(readOutput(true).trim(), "Missing the input file path," +
                " Please provide the path of the AsyncAPI specification with -i flag");
    }

    private String readContent(Path path) {
        String output = "";
        try (Stream<String> line = Files.lines(path)) {
            output = line.collect(Collectors.joining("\n"));
        } catch (IOException e) {
            Assert.fail("Could not read the file in the path " + path.toString() + e.getMessage());
        }
        return (output.trim()).replaceAll("\\s+", "");
    }
}

