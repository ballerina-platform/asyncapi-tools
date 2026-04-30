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
 * Unit tests for {@link AsyncApiProjectUtils}.
 */
public class AsyncApiProjectUtilsTest extends CmdTestBase {

    private static final String PKG_TOML = """
            [package]
            org = "testorg"
            name = "mypkg"
            version = "0.1.0"
            """;

    private static final String WORKSPACE_TOML = """
            [workspace]
            packages = []
            """;

    private void writeBallerinaToml(Path dir, String content) throws IOException {
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("Ballerina.toml"), content);
    }

    // ── readPackageName ───────────────────────────────────────────────────────

    @Test
    void testReadPackageNameReturnsName() throws IOException {
        writeBallerinaToml(tmpDir, PKG_TOML);
        Assert.assertEquals(AsyncApiProjectUtils.readPackageName(tmpDir), "mypkg");
    }

    @Test
    void testReadPackageNameMissingTomlThrows() {
        // tmpDir has no Ballerina.toml — IOException is wrapped in ASYNC_CLI_008
        AsyncApiCmdToolException ex = Assert.expectThrows(
                AsyncApiCmdToolException.class,
                () -> AsyncApiProjectUtils.readPackageName(tmpDir));
        Assert.assertEquals(ex.getDiagnosticCode(), "ASYNC_CLI_008");
    }

    @Test
    void testReadPackageNameNoNameFieldThrows() throws IOException {
        writeBallerinaToml(tmpDir, "[package]\norg = \"testorg\"\n");
        AsyncApiCmdToolException ex = Assert.expectThrows(
                AsyncApiCmdToolException.class,
                () -> AsyncApiProjectUtils.readPackageName(tmpDir));
        Assert.assertEquals(ex.getDiagnosticCode(), "ASYNC_CLI_008");
    }

    @Test
    void testReadPackageNameNoPackageSectionThrows() throws IOException {
        writeBallerinaToml(tmpDir, "[tool]\nid = \"something\"\n");
        AsyncApiCmdToolException ex = Assert.expectThrows(
                AsyncApiCmdToolException.class,
                () -> AsyncApiProjectUtils.readPackageName(tmpDir));
        Assert.assertEquals(ex.getDiagnosticCode(), "ASYNC_CLI_008");
    }

    // ── resolveOutputPath ─────────────────────────────────────────────────────

    @Test
    void testResolveOutputPathProjectRootNoModule() throws IOException {
        writeBallerinaToml(tmpDir, PKG_TOML);
        Path result = AsyncApiProjectUtils.resolveOutputPath(tmpDir, null, errStream);
        Assert.assertEquals(result.toAbsolutePath().normalize(),
                tmpDir.toAbsolutePath().normalize());
    }

    @Test
    void testResolveOutputPathInsideModulesDirNoModule() throws IOException {
        writeBallerinaToml(tmpDir, PKG_TOML);
        Path modDir = tmpDir.resolve("modules").resolve("mymod");
        Files.createDirectories(modDir);
        Path result = AsyncApiProjectUtils.resolveOutputPath(modDir, null, errStream);
        Assert.assertEquals(result.toAbsolutePath().normalize(), modDir.toAbsolutePath().normalize());
        Assert.assertTrue(getErr().isEmpty(), "No warning expected for modules/ path. Err: " + getErr());
    }

    @Test
    void testResolveOutputPathUnrecognizedSubdirThrows() throws IOException {
        writeBallerinaToml(tmpDir, PKG_TOML);
        // subDir does not exist yet — walk-up finds tmpDir as root, then rejects non-standard location
        Path subDir = tmpDir.resolve("src");
        AsyncApiCmdToolException ex = Assert.expectThrows(
                AsyncApiCmdToolException.class,
                () -> AsyncApiProjectUtils.resolveOutputPath(subDir, null, errStream));
        Assert.assertEquals(ex.getDiagnosticCode(), "ASYNC_CLI_010");
        Assert.assertTrue(ex.getDiagnosticMessage().contains(subDir.toString()),
                "Expected unrecognized location message to include the given path. Got: "
                        + ex.getDiagnosticMessage());
    }

    @Test
    void testResolveOutputPathModuleEqualsPackageName() throws IOException {
        writeBallerinaToml(tmpDir, PKG_TOML);
        Path result = AsyncApiProjectUtils.resolveOutputPath(tmpDir, "mypkg", errStream);
        Assert.assertEquals(result.toAbsolutePath().normalize(),
                tmpDir.toAbsolutePath().normalize());
    }

    @Test
    void testResolveOutputPathValidModuleReturnsModulesSubdir() throws IOException {
        writeBallerinaToml(tmpDir, PKG_TOML);
        Path result = AsyncApiProjectUtils.resolveOutputPath(tmpDir, "mymod", errStream);
        Assert.assertEquals(result.toAbsolutePath().normalize(),
                tmpDir.resolve("modules").resolve("mymod").toAbsolutePath().normalize());
    }

    @Test
    void testResolveOutputPathInvalidModuleNameThrows() throws IOException {
        writeBallerinaToml(tmpDir, PKG_TOML);
        AsyncApiCmdToolException ex = Assert.expectThrows(
                AsyncApiCmdToolException.class,
                () -> AsyncApiProjectUtils.resolveOutputPath(tmpDir, "my-module", errStream));
        Assert.assertEquals(ex.getDiagnosticCode(), "ASYNC_CLI_006");
        Assert.assertTrue(ex.getDiagnosticMessage().contains("my-module"));
    }

    @Test
    void testResolveOutputPathModuleNameTooLongThrows() throws IOException {
        writeBallerinaToml(tmpDir, PKG_TOML);
        String longName = "a".repeat(260);
        AsyncApiCmdToolException ex = Assert.expectThrows(
                AsyncApiCmdToolException.class,
                () -> AsyncApiProjectUtils.resolveOutputPath(tmpDir, longName, errStream));
        Assert.assertEquals(ex.getDiagnosticCode(), "ASYNC_CLI_007");
    }

    @Test
    void testResolveOutputPathNonExistentSubpathWithModuleFindsRoot() throws IOException {
        writeBallerinaToml(tmpDir, PKG_TOML);
        // effectivePath doesn't exist — walk-up locates tmpDir as project root
        Path nonExistent = tmpDir.resolve("does").resolve("not").resolve("exist");
        Path result = AsyncApiProjectUtils.resolveOutputPath(nonExistent, "mymod", errStream);
        Assert.assertEquals(result.toAbsolutePath().normalize(),
                tmpDir.resolve("modules").resolve("mymod").toAbsolutePath().normalize());
    }

    @Test
    void testResolveOutputPathNoProjectThrows() {
        // tmpDir itself has no Ballerina.toml — packageRoot() will not find a project
        AsyncApiCmdToolException ex = Assert.expectThrows(
                AsyncApiCmdToolException.class,
                () -> AsyncApiProjectUtils.resolveOutputPath(tmpDir, null, errStream));
        Assert.assertEquals(ex.getDiagnosticCode(), "ASYNC_CLI_004");
    }

    @Test
    void testResolveOutputPathWorkspaceThrows() throws IOException {
        writeBallerinaToml(tmpDir, WORKSPACE_TOML);
        AsyncApiCmdToolException ex = Assert.expectThrows(
                AsyncApiCmdToolException.class,
                () -> AsyncApiProjectUtils.resolveOutputPath(tmpDir, null, errStream));
        Assert.assertEquals(ex.getDiagnosticCode(), "ASYNC_CLI_003");
    }

    // ── validateBalFile ───────────────────────────────────────────────────────

    @Test
    void testValidateBalFileValidDoesNotThrow() throws IOException {
        Path balFile = tmpDir.resolve("valid.bal");
        Files.writeString(balFile, "public type Foo record { string name; };\n");
        // Must not throw
        AsyncApiProjectUtils.validateBalFile(balFile);
    }

    @Test
    void testValidateBalFileWithErrorsThrows() throws IOException {
        Path balFile = tmpDir.resolve("invalid.bal");
        Files.writeString(balFile, "!!! this is not ballerina !!!\n");
        AsyncApiCmdToolException ex = Assert.expectThrows(
                AsyncApiCmdToolException.class,
                () -> AsyncApiProjectUtils.validateBalFile(balFile));
        Assert.assertEquals(ex.getDiagnosticCode(), "ASYNC_CLI_009");
        Assert.assertTrue(ex.getDiagnosticMessage().contains("compilation errors"),
                "Expected 'compilation errors' in message. Got: " + ex.getDiagnosticMessage());
    }
}
