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
 * Tests for {@code --protocol http}: input validation, the capability checks and warn-and-continue handling
 * of options the protocol does not support. Generation itself is covered by the generator tests.
 */
public class HttpCmdUnitTest extends CmdTestBase {

    @Test(description = "A non-existent spec prints the invalid path error")
    public void testInvalidInputPath() {
        parse("/non/existent/file.yaml", "--protocol", "http").execute();
        Assert.assertTrue(getErr().contains("error: invalid input path '/non/existent/file.yaml'"),
                "Expected invalid path error. Err: " + getErr());
    }

    @Test(description = "A .bal input is rejected because http cannot generate a definition from code")
    public void testBalInputRejected() throws IOException {
        Path balFile = Files.createTempFile(tmpDir, "service-", ".bal");
        parse(balFile.toString(), "--protocol", "http").execute();
        Assert.assertTrue(getErr().contains("The http protocol does not support Ballerina-to-AsyncAPI"),
                "Expected the capability error. Err: " + getErr());
    }

    @Test(description = "A package directory is rejected for http too, for the same reason")
    public void testDirectoryInputRejected() throws IOException {
        Path dir = Files.createDirectory(tmpDir.resolve("pkg"));
        parse(dir.toString(), "--protocol", "http").execute();
        Assert.assertTrue(getErr().contains("The http protocol does not support Ballerina-to-AsyncAPI"),
                "Expected the capability error. Err: " + getErr());
    }

    @Test(description = "--service with a .bal input is rejected the same way")
    public void testServiceWithBalInputRejected() throws IOException {
        Path balFile = Files.createTempFile(tmpDir, "service-", ".bal");
        parse(balFile.toString(), "--protocol", "http", "--service", "MyService").execute();
        Assert.assertTrue(getErr().contains("does not support Ballerina-to-AsyncAPI"),
                "Expected the capability error. Err: " + getErr());
    }

    @Test(description = "--with-tests warns that http does not support it, and generation is still attempted")
    public void testWithTestsWarnsAndContinues() throws IOException {
        Path specFile = Files.createTempFile(tmpDir, "spec-", ".yaml");
        parse(specFile.toString(), "--protocol", "http", "--with-tests").execute();
        Assert.assertTrue(getErr().contains("warning: --with-tests is not supported for the http protocol"),
                "Expected the unsupported option warning. Err: " + getErr());
        Assert.assertTrue(getErr().contains("ERROR:"),
                "Generation should still be attempted after the warning. Err: " + getErr());
    }

    @Test(description = "--license warns that http does not support it, and generation is still attempted")
    public void testLicenseWarnsAndContinues() throws IOException {
        Path specFile = Files.createTempFile(tmpDir, "spec-", ".yaml");
        Path licenseFile = Files.createTempFile(tmpDir, "license-", ".txt");
        parse(specFile.toString(), "--protocol", "http", "--license", licenseFile.toString()).execute();
        Assert.assertTrue(getErr().contains("warning: --license is not supported for the http protocol"),
                "Expected the unsupported option warning. Err: " + getErr());
        Assert.assertTrue(getErr().contains("ERROR:"),
                "Generation should still be attempted after the warning. Err: " + getErr());
    }

    @Test(description = "--format and --service warn when generating code from a spec")
    public void testSpecOutputOnlyOptionsWarn() throws IOException {
        Path specFile = Files.createTempFile(tmpDir, "spec-", ".yaml");
        parse(specFile.toString(), "--protocol", "http", "--format", "json", "--service", "MyService").execute();
        Assert.assertTrue(getErr().contains("warning: --format only applies"),
                "Expected the --format warning. Err: " + getErr());
        Assert.assertTrue(getErr().contains("warning: --service only applies"),
                "Expected the --service warning. Err: " + getErr());
    }

    private Path generateTiny(Path pkg, String... extra) throws Exception {
        String[] args = new String[3 + extra.length];
        args[0] = resource("specs/tiny_http.yaml").toString();
        args[1] = "--protocol";
        args[2] = "http";
        System.arraycopy(extra, 0, args, 3, extra.length);
        errContent.reset();
        outContent.reset();
        parse(args).execute();
        return pkg;
    }

    @Test(description = "A first run writes every generated file and lists them")
    public void testFirstRunWritesAndListsFiles() throws Exception {
        Path pkg = newPackage("firstrun");
        generateTiny(pkg, "-o", pkg.toString());
        Assert.assertTrue(getErr().isEmpty(), "Err: " + getErr());
        Assert.assertTrue(getOut().contains("Following files were written."), "Out: " + getOut());
        for (String name : new String[]{"types.bal", "listener.bal", "service_types.bal", "dispatcher_service.bal",
                "tests/dispatch_test.bal"}) {
            Assert.assertTrue(Files.exists(pkg.resolve(name)), name + " should exist");
            Assert.assertTrue(getOut().contains("-- " + name), name + " should be listed. Out: " + getOut());
        }
    }

    @Test(description = "Running again with nothing changed is quiet and writes nothing")
    public void testRerunIsUnchanged() throws Exception {
        Path pkg = newPackage("rerun");
        generateTiny(pkg, "-o", pkg.toString());
        generateTiny(pkg, "-o", pkg.toString());
        Assert.assertTrue(getErr().isEmpty(), "Unchanged files should not warn. Err: " + getErr());
        Assert.assertTrue(getOut().contains("already up to date"), "Out: " + getOut());
    }

    @Test(description = "An existing file that differs is skipped with a warning unless --force is given")
    public void testExistingFileSkippedWithoutForce() throws Exception {
        Path pkg = newPackage("skip");
        generateTiny(pkg, "-o", pkg.toString());
        Files.writeString(pkg.resolve("types.bal"), "// hand edit");
        generateTiny(pkg, "-o", pkg.toString());
        Assert.assertTrue(getErr().contains("warning: Skipped \""), "Expected the skip warning. Err: " + getErr());
        Assert.assertTrue(getErr().contains("types.bal"), "Err: " + getErr());
        Assert.assertTrue(getErr().contains("Use --force to overwrite it."), "Err: " + getErr());
        Assert.assertTrue(getOut().contains("No files were written."), "Out: " + getOut());
        Assert.assertEquals(Files.readString(pkg.resolve("types.bal")), "// hand edit");
    }

    @Test(description = "--force overwrites the file that differs and leaves the unchanged ones alone")
    public void testForceOverwrites() throws Exception {
        Path pkg = newPackage("force");
        generateTiny(pkg, "-o", pkg.toString());
        Files.writeString(pkg.resolve("types.bal"), "// hand edit");
        generateTiny(pkg, "-o", pkg.toString(), "--force");
        Assert.assertTrue(getErr().isEmpty(), "Err: " + getErr());
        Assert.assertNotEquals(Files.readString(pkg.resolve("types.bal")), "// hand edit");
        Assert.assertTrue(getOut().contains("-- types.bal"), "Out: " + getOut());
        Assert.assertFalse(getOut().contains("-- listener.bal"), "Unchanged files are not rewritten. Out: " + getOut());
    }

    @Test(description = "--dry-run reports the plan and writes nothing")
    public void testDryRunWritesNothing() throws Exception {
        Path pkg = newPackage("dryrun");
        generateTiny(pkg, "-o", pkg.toString(), "--dry-run");
        Assert.assertTrue(getOut().startsWith("Dry run: no files were written."), "Out: " + getOut());
        Assert.assertTrue(getOut().contains("5 to create, 0 to overwrite, 0 to skip, 0 unchanged."),
                "Out: " + getOut());
        Assert.assertTrue(getOut().contains("create"), "Out: " + getOut());
        Assert.assertFalse(Files.exists(pkg.resolve("types.bal")), "A dry run must not write");
        Assert.assertFalse(Files.exists(pkg.resolve("tests")), "A dry run must not create folders");
    }

    @Test(description = "--dry-run does not create a module folder that is missing")
    public void testDryRunDoesNotCreateModuleFolder() throws Exception {
        Path pkg = newPackage("drymodule");
        generateTiny(pkg, "-o", pkg.toString(), "--module", "events", "--dry-run");
        Assert.assertTrue(getOut().contains("Dry run"), "Out: " + getOut() + " Err: " + getErr());
        Assert.assertFalse(Files.exists(pkg.resolve("modules")), "A dry run must not create the module");
    }

    @Test(description = "--dry-run shows skip for a changed file, and overwrite when --force is also given")
    public void testDryRunShowsSkipAndOverwrite() throws Exception {
        Path pkg = newPackage("dryskip");
        generateTiny(pkg, "-o", pkg.toString());
        Files.writeString(pkg.resolve("types.bal"), "// hand edit");

        generateTiny(pkg, "-o", pkg.toString(), "--dry-run");
        Assert.assertTrue(getOut().contains("skip"), "Out: " + getOut());
        Assert.assertTrue(getOut().contains("(already exists, use --force to overwrite)"), "Out: " + getOut());
        Assert.assertTrue(getOut().contains("0 to create, 0 to overwrite, 1 to skip, 4 unchanged."),
                "Out: " + getOut());

        generateTiny(pkg, "-o", pkg.toString(), "--dry-run", "--force");
        Assert.assertTrue(getOut().contains("overwrite"), "Out: " + getOut());
        Assert.assertTrue(getOut().contains("0 to create, 1 to overwrite, 0 to skip, 4 unchanged."),
                "Out: " + getOut());
        Assert.assertEquals(Files.readString(pkg.resolve("types.bal")), "// hand edit");
    }

    @Test(description = "No warning is printed when only supported options are given")
    public void testNoWarningForSupportedOptions() throws IOException {
        Path specFile = Files.createTempFile(tmpDir, "spec-", ".yaml");
        parse(specFile.toString(), "--protocol", "http", "--output", tmpDir.resolve("out").toString()).execute();
        Assert.assertFalse(getErr().contains("warning:"), "Unexpected warning. Err: " + getErr());
    }
}
