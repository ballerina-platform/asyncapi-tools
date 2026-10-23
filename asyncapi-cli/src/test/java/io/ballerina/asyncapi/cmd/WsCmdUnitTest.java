/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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
package io.ballerina.asyncapi.cmd;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests for {@code --protocol ws}: input validation, mode resolution and the options ws supports.
 * Generation itself is covered by the generator tests.
 */
public class WsCmdUnitTest extends CmdTestBase {

    @Test(description = "A non-existent spec prints the invalid path error")
    public void testInvalidInputPath() {
        parse("/non/existent/file.yaml", "--protocol", "ws").execute();
        Assert.assertTrue(getErr().contains("error: invalid input path '/non/existent/file.yaml'"),
                "Expected invalid path error. Err: " + getErr());
    }

    @Test(description = "A .bal input is accepted (code to spec) with --service")
    public void testBalFileAcceptedWithServiceFlag() throws IOException {
        Path balFile = Files.createTempFile(tmpDir, "service-", ".bal");
        parse(balFile.toString(), "--protocol", "ws", "--service", "ChatService").execute();
        Assert.assertFalse(getErr().contains("is not a valid Ballerina source file"),
                "A .bal file should be accepted. Err: " + getErr());
        Assert.assertFalse(getErr().contains("does not support"), "Unexpected capability error. Err: " + getErr());
    }

    @Test(description = "A .bal input is accepted without --service")
    public void testBalFileAcceptedWithoutServiceFlag() throws IOException {
        Path balFile = Files.createTempFile(tmpDir, "service-", ".bal");
        parse(balFile.toString(), "--protocol", "ws").execute();
        Assert.assertFalse(getErr().contains("does not support"), "Unexpected capability error. Err: " + getErr());
        Assert.assertFalse(getErr().contains("is not a valid Ballerina source file"),
                "A .bal file should be accepted. Err: " + getErr());
    }

    @Test(description = "--with-tests and --license are supported for ws and produce no warning")
    public void testSupportedOptionsDoNotWarn() throws IOException {
        Path specFile = Files.createTempFile(tmpDir, "spec-", ".yaml");
        Path licenseFile = Files.createTempFile(tmpDir, "license-", ".txt");
        parse(specFile.toString(), "--protocol", "ws", "--with-tests", "--license", licenseFile.toString(),
                "--output", tmpDir.resolve("out").toString()).execute();
        Assert.assertFalse(getErr().contains("warning:"), "Unexpected warning. Err: " + getErr());
    }

    @Test(description = "--format json with a .bal input produces no warning")
    public void testJsonFlagOnBalInput() throws IOException {
        Path balFile = Files.createTempFile(tmpDir, "service-", ".bal");
        parse(balFile.toString(), "--protocol", "ws", "--service", "MyService", "--format", "json").execute();
        Assert.assertFalse(getErr().contains("warning: --format"), "Unexpected warning. Err: " + getErr());
    }

    @Test(description = "--format and --service warn when generating code from a spec")
    public void testSpecOutputOnlyOptionsWarn() throws IOException {
        Path specFile = Files.createTempFile(tmpDir, "spec-", ".yaml");
        parse(specFile.toString(), "--protocol", "ws", "--format", "json", "--service", "MyService").execute();
        Assert.assertTrue(getErr().contains("warning: --format only applies"),
                "Expected the --format warning. Err: " + getErr());
        Assert.assertTrue(getErr().contains("warning: --service only applies"),
                "Expected the --service warning. Err: " + getErr());
    }

    @Test(description = "A package directory is accepted as the input for code to spec")
    public void testPackageDirectoryAccepted() throws IOException {
        Path pkg = newPackage("chatpkg");
        parse(pkg.toString(), "--protocol", "ws").execute();
        Assert.assertTrue(getErr().isEmpty(), "A package should be accepted. Err: " + getErr());
        Assert.assertTrue(getOut().contains("No WebSocket service found"),
                "Expected the package to be read. Out: " + getOut());
    }

    @Test(description = "A directory without a Ballerina.toml is not a package")
    public void testDirectoryWithoutBallerinaToml() throws IOException {
        Path dir = Files.createDirectory(tmpDir.resolve("plain"));
        parse(dir.toString(), "--protocol", "ws").execute();
        Assert.assertTrue(getErr().contains("is not a Ballerina package"),
                "Expected the not-a-package error. Err: " + getErr());
    }

    @Test(description = "A .bal file inside a package is rejected, as bal run does")
    public void testBalFileInsidePackageRejected() throws IOException {
        Path pkg = newPackage("insidepkg");
        parse(pkg.resolve("main.bal").toString(), "--protocol", "ws").execute();
        Assert.assertTrue(getErr().contains("belongs to the Ballerina package"),
                "Expected the file-in-package error. Err: " + getErr());
        Assert.assertTrue(getErr().contains("Pass the package directory"),
                "Expected the hint. Err: " + getErr());
    }

    @Test(description = "--module reads a module of the package")
    public void testModuleOfPackageAccepted() throws IOException {
        Path pkg = newPackage("modpkg");
        Files.createDirectories(pkg.resolve("modules").resolve("chat"));
        Files.writeString(pkg.resolve("modules").resolve("chat").resolve("chat.bal"), "");
        parse(pkg.toString(), "--protocol", "ws", "--module", "chat").execute();
        Assert.assertTrue(getErr().isEmpty(), "A module of the package should be accepted. Err: " + getErr());
    }

    @Test(description = "A module the package does not have is rejected and the available ones are listed")
    public void testUnknownModuleRejected() throws IOException {
        Path pkg = newPackage("nomodpkg");
        Files.createDirectories(pkg.resolve("modules").resolve("chat"));
        Files.createDirectories(pkg.resolve("modules").resolve("admin"));
        parse(pkg.toString(), "--protocol", "ws", "--module", "missing").execute();
        Assert.assertTrue(getErr().contains("Module 'missing' not found"),
                "Expected the module error. Err: " + getErr());
        Assert.assertTrue(getErr().contains("Available modules: admin, chat"),
                "Expected the available modules. Err: " + getErr());
    }

    @Test(description = "A package without modules says there are none")
    public void testUnknownModuleWithoutModulesDirectory() throws IOException {
        Path pkg = newPackage("barepkg");
        parse(pkg.toString(), "--protocol", "ws", "--module", "chat").execute();
        Assert.assertTrue(getErr().contains("Available modules: none"), "Err: " + getErr());
    }

    @Test(description = "The package's own name selects its default module")
    public void testPackageNameSelectsDefaultModule() throws IOException {
        Path pkg = newPackage("selfpkg");
        parse(pkg.toString(), "--protocol", "ws", "--module", "selfpkg").execute();
        Assert.assertTrue(getErr().isEmpty(), "The package name should mean the default module. Err: " + getErr());
    }

    @Test(description = "--module is ignored with a warning for a standalone .bal file")
    public void testModuleIgnoredForStandaloneFile() throws IOException {
        Path balFile = Files.createTempFile(tmpDir, "service-", ".bal");
        parse(balFile.toString(), "--protocol", "ws", "--module", "chat").execute();
        Assert.assertTrue(getErr().contains("warning: --module only applies to a Ballerina package"),
                "Expected the module warning. Err: " + getErr());
        Assert.assertFalse(getErr().contains("error:"), "Generation should continue. Err: " + getErr());
    }

    @Test(description = "CmdUtils.resolveMode returns BALLERINA_TO_ASYNCAPI for a directory")
    public void testResolveModeDirectory() throws IOException {
        BaseCmd base = new BaseCmd();
        base.inputPath = Files.createDirectory(tmpDir.resolve("somedir")).toString();
        Assert.assertEquals(CmdUtils.resolveMode(base), CmdConstants.Mode.BALLERINA_TO_ASYNCAPI);
    }

    @Test(description = "CmdUtils.resolveMode returns ASYNCAPI_TO_BALLERINA for a .yaml input")
    public void testResolveModeSpecToCode() {
        BaseCmd base = new BaseCmd();
        base.inputPath = "definition.yaml";
        Assert.assertEquals(CmdUtils.resolveMode(base), CmdConstants.Mode.ASYNCAPI_TO_BALLERINA);
    }

    @Test(description = "CmdUtils.resolveMode returns BALLERINA_TO_ASYNCAPI for a .bal input")
    public void testResolveModeCodeToSpec() {
        BaseCmd base = new BaseCmd();
        base.inputPath = "service.bal";
        Assert.assertEquals(CmdUtils.resolveMode(base), CmdConstants.Mode.BALLERINA_TO_ASYNCAPI);
    }
}
