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
        ExitCodeCaptor exitCaptor = new ExitCodeCaptor();
        AsyncApiCmd cmd = new AsyncApiCmd(printStream, tmpDir, exitCaptor);
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
        ExitCodeCaptor exitCaptor = new ExitCodeCaptor();
        AsyncApiCmd cmd = new AsyncApiCmd(printStream, tmpDir, exitCaptor);
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
        ExitCodeCaptor exitCaptor = new ExitCodeCaptor();
        AsyncApiCmd cmd = new AsyncApiCmd(printStream, tmpDir, exitCaptor);
        new CommandLine(cmd).parseArgs(args);
        cmd.execute();
        String output = readOutput(true);
        Assert.assertTrue(output.contains("File not found in the given path: "),
                "Expected error message not found. Actual output: " + output);
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

    @Test(description = "Test asyncapi command execution without arguments - should return exit code 2")
    public void testExecuteWithoutArguments() {
        String[] args = {};
        ExitCodeCaptor exitCaptor = new ExitCodeCaptor();
        AsyncApiCmd cmd = new AsyncApiCmd(printStream, tmpDir, exitCaptor);
        new CommandLine(cmd).parseArgs(args);
        cmd.execute();
        Assert.assertEquals(exitCaptor.getExitCode(), 2,
                "asyncapi command without arguments should exit with code 2");
    }

    @Test(description = "Test asyncapi command execution with help flag - should return exit code 0")
    public void testExecuteWithHelpFlagExitCode() {
        String[] args = {"-h"};
        ExitCodeCaptor exitCaptor = new ExitCodeCaptor();
        AsyncApiCmd cmd = new AsyncApiCmd(printStream, tmpDir, exitCaptor);
        new CommandLine(cmd).parseArgs(args);
        cmd.execute();
        Assert.assertEquals(exitCaptor.getExitCode(), 0,
                "asyncapi command with -h flag should exit with code 0");
    }

    @Test(description = "Test asyncapi command execution with invalid flag - should throw exception during parsing")
    public void testExecuteWithInvalidFlagException() {
        String[] args = {"--invalidFlag"};
        ExitCodeCaptor exitCaptor = new ExitCodeCaptor();
        AsyncApiCmd cmd = new AsyncApiCmd(printStream, tmpDir, exitCaptor);
        try {
            new CommandLine(cmd).parseArgs(args);
            Assert.fail("Expected picocli to throw exception for invalid flag");
        } catch (CommandLine.UnmatchedArgumentException e) {
            // Expected: picocli rejects invalid flags
            Assert.assertTrue(e.getMessage().contains("Unknown option"));
        }
    }

    @Test(description = "Ensure HTTP protocol rejects Ballerina service input with a clear error")
    public void testHttpProtocolRejectsBalInput() throws IOException {
        Path balFile = resourceDir.resolve(Paths.get("websockets", "ballerina-to-asyncapi", "service",
                "basic_service.bal"));
        String[] args = {"--input", balFile.toString()};
        ExitCodeCaptor exitCaptor = new ExitCodeCaptor();
        AsyncApiCmd cmd = new AsyncApiCmd(printStream, tmpDir, exitCaptor);
        new CommandLine(cmd).parseArgs(args);
        cmd.execute();
        String output = readOutput(true);
        Assert.assertEquals(exitCaptor.getExitCode(), 1,
                "HTTP execution with a .bal input should exit with code 1");
        Assert.assertTrue(output.contains("An AsyncApi definition file is required"),
                "Expected informative error when HTTP flow receives a .bal input. Actual: " + output);
    }

    @Test(description = "Accept AsyncAPI inputs that use upper-case file extensions")
    public void testExecuteWithUpperCaseAsyncApiExtension() throws IOException {
        Path specYaml = resourceDir.resolve(Paths.get("specs", "SPEC_COMPLETE_SLACK_UPPERCASE.YAML"));
        Path outputDir = Files.createTempDirectory(this.tmpDir, "upper-case-out-");
        String[] args = {"--input", specYaml.toString(), "-o", outputDir.toString()};
        ExitCodeCaptor exitCaptor = new ExitCodeCaptor();
        AsyncApiCmd cmd = new AsyncApiCmd(printStream, outputDir, exitCaptor);
        new CommandLine(cmd).parseArgs(args);
        cmd.execute();
        readOutput(true);
        Assert.assertEquals(exitCaptor.getExitCode(), 0,
                "Upper-case AsyncAPI file extensions should be accepted for HTTP generation");
        Assert.assertTrue(Files.exists(outputDir.resolve("listener.bal")),
                "Expected generation output when using upper-case AsyncAPI extension");
    }

    @Test(description = "HTTP protocol should stop execution when unsupported flags are provided")
    public void testHttpProtocolInvalidFlagTerminatesExecution() throws IOException {
        Path specYaml = resourceDir.resolve(Paths.get("specs", "spec-complete-slack.yml"));
        String[] args = {"--input", specYaml.toString(), "--license", specYaml.toString()};
        ExitCodeCaptor exitCaptor = new ExitCodeCaptor();
        AsyncApiCmd cmd = new AsyncApiCmd(printStream, tmpDir, exitCaptor);
        new CommandLine(cmd).parseArgs(args);
        cmd.execute();
        String output = readOutput(true);
        Assert.assertEquals(exitCaptor.getExitCode(), 1,
                "HTTP execution with unsupported flags should exit with code 1");
        Assert.assertTrue(output.contains("unsupported --license flag for http protocol"),
                "Expected unsupported flag warning for HTTP protocol. Actual: " + output);
    }

    @Test(description = "WebSocket generation aborts cleanly when license file cannot be read")
    public void testWsGenerationStopsOnInvalidLicense() throws IOException {
        Path specYaml = resourceDir.resolve(Paths.get("specs", "spec-complete-slack.yml"));
        Path invalidLicense = tmpDir.resolve("missing-license.txt");
        String[] args = {"--input", specYaml.toString(), "--protocol", "ws", "--license",
                invalidLicense.toString()};
        ExitCodeCaptor exitCaptor = new ExitCodeCaptor();
        AsyncApiCmd cmd = new AsyncApiCmd(printStream, tmpDir, exitCaptor);
        new CommandLine(cmd).parseArgs(args);
        cmd.execute();
        String output = readOutput(true);
        Assert.assertEquals(exitCaptor.getExitCode(), 1,
                "WebSocket generation with invalid license path should exit with code 1");
        Assert.assertTrue(output.contains("Invalid license file path"),
                "Expected invalid license file warning. Actual: " + output);
    }

    @Test(description = "HTTP protocol rejects --service flag with a clear error")
    public void testHttpProtocolRejectsServiceFlag() throws IOException {
        Path specYaml = resourceDir.resolve(Paths.get("specs", "spec-complete-slack.yml"));
        String[] args = {"--input", specYaml.toString(), "--service", "MyService"};
        ExitCodeCaptor exitCaptor = new ExitCodeCaptor();
        AsyncApiCmd cmd = new AsyncApiCmd(printStream, tmpDir, exitCaptor);
        new CommandLine(cmd).parseArgs(args);
        cmd.execute();
        String output = readOutput(true);
        String expectedMessage = String.format(AsyncApiMessages.INVALID_OPTION_ERROR_HTTP, AsyncApiCmd.SERVICE_FLAG);
        Assert.assertEquals(exitCaptor.getExitCode(), 1,
                "HTTP execution with --service flag should exit with code 1");
        Assert.assertTrue(output.contains(expectedMessage),
                "Expected HTTP --service rejection message not found. Actual output: " + output);
    }

    @Test(description = "HTTP protocol rejects --with-tests flag with a clear error")
    public void testHttpProtocolRejectsWithTestsFlag() throws IOException {
        Path specYaml = resourceDir.resolve(Paths.get("specs", "spec-complete-slack.yml"));
        String[] args = {"--input", specYaml.toString(), "--with-tests"};
        ExitCodeCaptor exitCaptor = new ExitCodeCaptor();
        AsyncApiCmd cmd = new AsyncApiCmd(printStream, tmpDir, exitCaptor);
        new CommandLine(cmd).parseArgs(args);
        cmd.execute();
        String output = readOutput(true);
        String expectedMessage = String.format(AsyncApiMessages.INVALID_OPTION_ERROR_HTTP, AsyncApiCmd.TEST_FLAG);
        Assert.assertEquals(exitCaptor.getExitCode(), 1,
                "HTTP execution with --with-tests flag should exit with code 1");
        Assert.assertTrue(output.contains(expectedMessage),
                "Expected HTTP --with-tests rejection message not found. Actual output: " + output);
    }

    @Test(description = "HTTP protocol rejects --json flag with a clear error")
    public void testHttpProtocolRejectsJsonFlag() throws IOException {
        Path specYaml = resourceDir.resolve(Paths.get("specs", "spec-complete-slack.yml"));
        String[] args = {"--input", specYaml.toString(), "--json"};
        ExitCodeCaptor exitCaptor = new ExitCodeCaptor();
        AsyncApiCmd cmd = new AsyncApiCmd(printStream, tmpDir, exitCaptor);
        new CommandLine(cmd).parseArgs(args);
        cmd.execute();
        String output = readOutput(true);
        String expectedMessage = String.format(AsyncApiMessages.INVALID_OPTION_ERROR_HTTP, AsyncApiCmd.JSON_FLAG);
        Assert.assertEquals(exitCaptor.getExitCode(), 1,
                "HTTP execution with --json flag should exit with code 1");
        Assert.assertTrue(output.contains(expectedMessage),
                "Expected HTTP --json rejection message not found. Actual output: " + output);
    }

    @Test(description = "Protocol names are case-insensitive for HTTP generation")
    public void testExecuteWithUpperCaseHttpsProtocol() throws IOException {
        Path specYaml = resourceDir.resolve(Paths.get("specs", "spec-complete-slack.yml"));
        Path outputDir = Files.createTempDirectory(this.tmpDir, "https-case-");
        String[] args = {"--input", specYaml.toString(), "--protocol", "HTTPS", "-o", outputDir.toString()};
        ExitCodeCaptor exitCaptor = new ExitCodeCaptor();
        AsyncApiCmd cmd = new AsyncApiCmd(printStream, outputDir, exitCaptor);
        new CommandLine(cmd).parseArgs(args);
        cmd.execute();
        readOutput(true);
        Assert.assertEquals(exitCaptor.getExitCode(), 0,
                "Upper-case HTTPS protocol should be accepted and exit with code 0");
        Assert.assertTrue(Files.exists(outputDir.resolve("listener.bal")),
                "Expected listener.bal to be generated for HTTPS protocol");
    }

    @Test(description = "Protocol names are case-insensitive for WebSocket generation")
    public void testExecuteWithUpperCaseWsProtocol() throws IOException {
        Path balFile = resourceDir.resolve(Paths.get("websockets", "ballerina-to-asyncapi", "service",
                "basic_service.bal"));
        Path outputDir = Files.createTempDirectory(this.tmpDir, "ws-case-");
        String[] args = {"--input", balFile.toString(), "--protocol", "WS", "-o", outputDir.toString()};
        ExitCodeCaptor exitCaptor = new ExitCodeCaptor();
        AsyncApiCmd cmd = new AsyncApiCmd(printStream, outputDir, exitCaptor);
        new CommandLine(cmd).parseArgs(args);
        cmd.execute();
        String output = readOutput(true);
        Assert.assertFalse(exitCaptor.wasExitCalled(),
                "Successful WS execution should not invoke exit handler");
        Assert.assertTrue(output.contains("The support for the WebSocket protocol is currently"),
                "Expected experimental WebSocket warning in output. Actual output: " + output);
        boolean hasAsyncApiSpec;
        try (Stream<Path> stream = Files.list(outputDir)) {
            hasAsyncApiSpec = stream.anyMatch(path -> {
                String fileName = path.getFileName().toString();
                return fileName.endsWith(".yaml") || fileName.endsWith(".json");
            });
        }
        Assert.assertTrue(hasAsyncApiSpec, "Expected AsyncAPI definition to be generated for WS protocol");
    }

    @Test(description = "Invalid protocol names are rejected with a helpful error")
    public void testExecuteWithInvalidProtocol() throws IOException {
        Path specYaml = resourceDir.resolve(Paths.get("specs", "spec-complete-slack.yml"));
        String[] args = {"--input", specYaml.toString(), "--protocol", "grpc"};
        ExitCodeCaptor exitCaptor = new ExitCodeCaptor();
        AsyncApiCmd cmd = new AsyncApiCmd(printStream, tmpDir, exitCaptor);
        new CommandLine(cmd).parseArgs(args);
        cmd.execute();
        String output = readOutput(true);
        Assert.assertEquals(exitCaptor.getExitCode(), 1,
                "Invalid protocol should exit with code 1");
        Assert.assertTrue(output.contains("ERROR invalid protocol: grpc"),
                "Expected invalid protocol error in output. Actual output: " + output);
    }

    @Test(description = "WebSocket client generation warns when service/json options are provided")
    public void testWsClientGenerationWarnsForInvalidOptions() throws IOException {
        Path specYaml = resourceDir.resolve(Paths.get("websockets", "asyncapi-to-ballerina", "test_cases",
                "sample_yamls", "no_auth.yaml"));
        Path outputDir = Files.createTempDirectory(this.tmpDir, "ws-client-");
        String[] args = {"--input", specYaml.toString(), "--protocol", "ws", "--service", "ChatService", "--json",
                "-o", outputDir.toString()};
        ExitCodeCaptor exitCaptor = new ExitCodeCaptor();
        AsyncApiCmd cmd = new AsyncApiCmd(printStream, outputDir, exitCaptor);
        new CommandLine(cmd).parseArgs(args);
        cmd.execute();
        String output = readOutput(true);
        Assert.assertFalse(exitCaptor.wasExitCalled(),
                "Warnings for invalid WebSocket client options should not stop execution");
        Assert.assertTrue(output.contains(AsyncApiMessages.INVALID_USE_OF_SERVICE_FLAG_WARNING),
                "Expected service warning when generating WebSocket client. Actual output: " + output);
        Assert.assertTrue(output.contains(AsyncApiMessages.INVALID_USE_OF_JSON_FLAG_WARNING),
                "Expected json warning when generating WebSocket client. Actual output: " + output);
    }

    @Test(description = "WebSocket spec export warns when license/tests options are provided")
    public void testWsSpecGenerationWarnsForLicenseAndTests() throws IOException {
        Path balFile = resourceDir.resolve(Paths.get("websockets", "ballerina-to-asyncapi", "service",
                "basic_service.bal"));
        Path outputDir = Files.createTempDirectory(this.tmpDir, "ws-spec-");
        Path licenseFile = Files.createTempFile(this.tmpDir, "license-", ".txt");
        Files.writeString(licenseFile, "license header");
        String[] args = {"--input", balFile.toString(), "--protocol", "ws", "--license",
                licenseFile.toString(), "--with-tests", "-o", outputDir.toString()};
        ExitCodeCaptor exitCaptor = new ExitCodeCaptor();
        AsyncApiCmd cmd = new AsyncApiCmd(printStream, outputDir, exitCaptor);
        new CommandLine(cmd).parseArgs(args);
        cmd.execute();
        String output = readOutput(true);
        Assert.assertFalse(exitCaptor.wasExitCalled(),
                "Warnings for invalid WebSocket spec options should not stop execution");
        Assert.assertTrue(output.contains(AsyncApiMessages.INVALID_USE_OF_LICENSE_FLAG_WARNING),
                "Expected license warning when exporting AsyncAPI spec. Actual output: " + output);
        Assert.assertTrue(output.contains(AsyncApiMessages.INVALID_USE_OF_TEST_FLAG_WARNING),
                "Expected test warning when exporting AsyncAPI spec. Actual output: " + output);
        boolean hasJsonOrYaml;
        try (Stream<Path> stream = Files.list(outputDir)) {
            hasJsonOrYaml = stream.anyMatch(path -> {
                String fileName = path.getFileName().toString();
                return fileName.endsWith(".yaml") || fileName.endsWith(".json");
            });
        }
        Assert.assertTrue(hasJsonOrYaml, "Expected AsyncAPI definition to be generated for WebSocket spec export");
    }
}
