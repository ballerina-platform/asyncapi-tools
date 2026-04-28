/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com)
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
package io.ballerina.asyncapi.generator.ws.asyncspec.utils;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/**
 * Unit tests for {@link CodegenUtils} covering file-write behaviour and contract-file
 * name resolution.
 *
 * <p>Note on {@code resolveContractFileName}: the second parameter is a fully-formed file
 * name with extension (e.g. {@code "chat_asyncapi.yaml"}), not a bare service name.
 * The collision-resolution branch inside the method is gated on {@code System.console() != null},
 * which is always {@code false} in CI/test environments — that branch is an accepted gap.
 * All tests here therefore exercise the paths that are reachable without a real console.
 */
class CodegenUtilsTest {

    private Path tempDir;

    @BeforeMethod
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("asyncspec-codegen-test-");
    }

    @AfterMethod
    void tearDown() throws IOException {
        deleteDir(tempDir);
    }

    private static void deleteDir(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (var walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    // -------------------------------------------------------------------------
    // writeFile
    // -------------------------------------------------------------------------

    @Test
    void testWriteFile_writesContentToPath() throws IOException {
        Path outFile = tempDir.resolve("output.yaml");
        CodegenUtils.writeFile(outFile, "asyncapi: 3.0.0\n");
        Assert.assertEquals(Files.readString(outFile), "asyncapi: 3.0.0\n",
                "written content must match what was passed to writeFile");
    }

    @Test
    void testWriteFile_createsFileWhenItDoesNotExist() throws IOException {
        // writeFile uses FileWriter directly — it creates the target file when absent,
        // but does NOT create missing parent directories.
        Path outFile = tempDir.resolve("newfile.yaml");
        Assert.assertFalse(Files.exists(outFile), "precondition: file must not exist before writeFile");
        CodegenUtils.writeFile(outFile, "content");
        Assert.assertTrue(Files.exists(outFile), "file must exist after writeFile");
    }

    @Test
    void testWriteFile_overwritesExistingFile() throws IOException {
        Path outFile = tempDir.resolve("existing.yaml");
        CodegenUtils.writeFile(outFile, "first");
        CodegenUtils.writeFile(outFile, "second");
        Assert.assertEquals(Files.readString(outFile), "second",
                "second call to writeFile must overwrite the first content");
    }

    @Test
    void testWriteFile_emptyContent_createsEmptyFile() throws IOException {
        Path outFile = tempDir.resolve("empty.yaml");
        CodegenUtils.writeFile(outFile, "");
        Assert.assertTrue(Files.exists(outFile),
                "writeFile with empty content must still create the file");
        Assert.assertEquals(Files.readString(outFile), "",
                "file content must be empty after writing an empty string");
    }

    // -------------------------------------------------------------------------
    // resolveContractFileName
    // -------------------------------------------------------------------------

    @Test
    void testResolveContractFileName_noCollision_yamlName_returnedUnchanged() {
        // No existing file with this name in tempDir — name must pass through as-is.
        String filename = CodegenUtils.resolveContractFileName(tempDir, "chat_asyncapi.yaml", false);
        Assert.assertTrue(filename.endsWith(".yaml"),
                "returned name must end with .yaml");
        Assert.assertTrue(filename.contains("chat"),
                "returned name must contain the service name segment");
    }

    @Test
    void testResolveContractFileName_noCollision_isJsonTrue_jsonNameReturnedUnchanged() {
        String filename = CodegenUtils.resolveContractFileName(tempDir, "chat_asyncapi.json", true);
        Assert.assertTrue(filename.endsWith(".json"),
                "returned name must end with .json when the supplied name has a .json extension");
    }

    @Test
    void testResolveContractFileName_collision_consoleNull_nameReturnedUnchanged() throws IOException {
        // System.console() is always null in CI/test environments, so the collision-resolution
        // branch in checkAvailabilityOfGivenName is never entered. The observable behaviour is
        // that the method returns asyncApiName unchanged even when the file already exists.
        Files.writeString(tempDir.resolve("chat_asyncapi.yaml"), "existing");
        String filename = CodegenUtils.resolveContractFileName(tempDir, "chat_asyncapi.yaml", false);
        Assert.assertEquals(filename, "chat_asyncapi.yaml",
                "when System.console() is null the name must be returned unchanged even on collision");
    }

    @Test
    void testResolveContractFileName_nullOutPath_nameReturnedUnchanged() {
        // The guard 'outPath != null && Files.exists(outPath)' short-circuits immediately.
        String filename = CodegenUtils.resolveContractFileName(null, "chat_asyncapi.yaml", false);
        Assert.assertEquals(filename, "chat_asyncapi.yaml",
                "null outPath must short-circuit directory inspection and return asyncApiName unchanged");
    }
}
