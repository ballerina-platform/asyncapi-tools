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
import java.util.zip.ZipFile;

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

    @Test(description = "Without --with-tests no test file is written, even when the spec has x-ballerina-auth")
    public void testNoTestsWithoutFlag() throws Exception {
        Path pkg = newPackage("notests");
        generateTiny(pkg, "-o", pkg.toString());
        Assert.assertTrue(getErr().isEmpty(), "Err: " + getErr());
        Assert.assertFalse(Files.exists(pkg.resolve("tests/dispatch_test.bal")), "Tests were not requested");
    }

    @Test(description = "--with-tests writes the dispatch tests for a spec with x-ballerina-auth")
    public void testWithTestsWritesDispatchTests() throws Exception {
        Path pkg = newPackage("withtests");
        generateTiny(pkg, "-o", pkg.toString(), "--with-tests");
        Assert.assertTrue(getErr().isEmpty(), "Err: " + getErr());
        Assert.assertTrue(Files.exists(pkg.resolve("tests/dispatch_test.bal")), "dispatch_test.bal should exist");
        Assert.assertTrue(getOut().contains("-- tests/dispatch_test.bal"), "Out: " + getOut());
    }

    @Test(description = "--with-tests on a spec without x-ballerina-auth warns that no tests could be written")
    public void testWithTestsWithoutAuthWarns() throws Exception {
        Path pkg = newPackage("noauth");
        String tiny = Files.readString(resource("specs/tiny_http.yaml"));
        String noAuth = tiny.replaceAll("(?s)x-ballerina-auth:.*?(?=info:)", "");
        Path spec = Files.writeString(tmpDir.resolve("noauth.yaml"), noAuth);
        parse(spec.toString(), "--protocol", "http", "-o", pkg.toString(), "--with-tests").execute();
        Assert.assertTrue(getErr().contains("warning: --with-tests generated no tests"), "Err: " + getErr());
        Assert.assertTrue(Files.exists(pkg.resolve("listener.bal")), "The code should still be generated");
        Assert.assertFalse(Files.exists(pkg.resolve("tests/dispatch_test.bal")));
    }

    @Test(description = "--license warns that http does not support it, and generation is still attempted")
    public void testLicenseWarnsAndContinues() throws IOException {
        Path specFile = Files.createTempFile(tmpDir, "spec-", ".yaml");
        Path licenseFile = Files.createTempFile(tmpDir, "license-", ".txt");
        parse(specFile.toString(), "--protocol", "http", "--license", licenseFile.toString()).execute();
        Assert.assertTrue(getErr().contains("warning: --license is not supported for the http protocol"),
                "Expected the unsupported option warning. Err: " + getErr());
        Assert.assertTrue(getErr().contains("error:"),
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
        for (String name : new String[]{"types.bal", "listener.bal", "service_types.bal", "dispatcher_service.bal"}) {
            Assert.assertTrue(Files.exists(pkg.resolve(name)), name + " should exist");
            Assert.assertTrue(getOut().contains("-- " + name), name + " should be listed. Out: " + getOut());
        }
    }

    @Test(description = "A trigger at a package root gets the plugin jar in lib/ and a CompilerPlugin.toml for it")
    public void testTriggerGetsCompilerPlugin() throws Exception {
        Path pkg = newPackage("plugin");
        String ballerinaToml = Files.readString(pkg.resolve("Ballerina.toml"));
        generateTiny(pkg, "-o", pkg.toString());
        Assert.assertTrue(getErr().isEmpty(), "Err: " + getErr());
        String toml = Files.readString(pkg.resolve("CompilerPlugin.toml"));
        Assert.assertTrue(toml.contains("id = \"asyncapi-compiler-plugin\""), toml);
        Assert.assertTrue(toml.contains("path = \"lib/asyncapi-trigger-compiler-plugin.jar\""), toml);
        try (ZipFile zip = new ZipFile(pkg.resolve("lib/asyncapi-trigger-compiler-plugin.jar").toFile())) {
            Assert.assertNotNull(zip.getEntry("io/ballerina/asyncapi/trigger/plugin/AsyncAPICompilerPlugin.class"),
                    "The jar should hold the plugin class");
        }
        Assert.assertEquals(Files.readString(pkg.resolve("Ballerina.toml")), ballerinaToml,
                "Ballerina.toml needs no entry");
        Assert.assertTrue(getOut().contains("-- CompilerPlugin.toml"), "Out: " + getOut());
        Assert.assertTrue(getOut().contains("-- lib/asyncapi-trigger-compiler-plugin.jar"), "Out: " + getOut());
    }

    @Test(description = "Regenerating a trigger made by 0.13.3 removes the [[tool.asyncapi]] entry it added")
    public void testRegenerationRemovesTheOldToolEntry() throws Exception {
        Path pkg = newPackage("regen");
        String before = Files.readString(pkg.resolve("Ballerina.toml"));
        Files.writeString(pkg.resolve("Ballerina.toml"), before + "\n# Copies the trigger's compiler plugin from the "
                + "asyncapi tool during the build\n[[tool.asyncapi]]\nid = \"trigger-plugin\"\nversion = \"0.13.3\"\n");
        generateTiny(pkg, "-o", pkg.toString());
        Assert.assertEquals(Files.readString(pkg.resolve("Ballerina.toml")), before);
        Assert.assertTrue(getOut().contains("-- Ballerina.toml: removed [[tool.asyncapi]]"), "Out: " + getOut());
    }

    @Test(description = "The old entry stays while the old CompilerPlugin.toml is kept, and goes with --force")
    public void testOldToolEntryStaysWhenThePluginFileIsSkipped() throws Exception {
        Path pkg = newPackage("skipped");
        String before = Files.readString(pkg.resolve("Ballerina.toml"));
        String withEntry = before + "\n[[tool.asyncapi]]\nid = \"trigger-plugin\"\nversion = \"0.13.3\"\n";
        String oldPluginToml = "[plugin]\nid = \"asyncapi-compiler-plugin\"\nclass = \"x.Plugin\"\n\n"
                + "[[dependency]]\npath = \"target/tool-cache/asyncapi-trigger-compiler-plugin.jar\"\n";
        Files.writeString(pkg.resolve("Ballerina.toml"), withEntry);
        Files.writeString(pkg.resolve("CompilerPlugin.toml"), oldPluginToml);
        generateTiny(pkg, "-o", pkg.toString());
        Assert.assertEquals(Files.readString(pkg.resolve("CompilerPlugin.toml")), oldPluginToml);
        Assert.assertEquals(Files.readString(pkg.resolve("Ballerina.toml")), withEntry,
                "The old CompilerPlugin.toml still needs the jar the entry copies in");
        Assert.assertFalse(getOut().contains("removed [[tool.asyncapi]]"), "Out: " + getOut());

        generateTiny(pkg, "-o", pkg.toString(), "--force");
        Assert.assertTrue(Files.readString(pkg.resolve("CompilerPlugin.toml")).contains("lib/"));
        Assert.assertEquals(Files.readString(pkg.resolve("Ballerina.toml")), before);
    }

    @Test(description = "When the old entry can't be removed, the code is still generated and a warning says why")
    public void testOldToolEntryThatCantBeRemovedWarns() throws Exception {
        Path pkg = newPackage("readonly");
        Path ballerinaToml = pkg.resolve("Ballerina.toml");
        Files.writeString(ballerinaToml, Files.readString(ballerinaToml)
                + "\n[[tool.asyncapi]]\nid = \"trigger-plugin\"\nversion = \"0.13.3\"\n");
        Assert.assertTrue(ballerinaToml.toFile().setWritable(false), "Could not make Ballerina.toml read-only");
        try {
            generateTiny(pkg, "-o", pkg.toString());
        } finally {
            ballerinaToml.toFile().setWritable(true);
        }
        Assert.assertTrue(Files.exists(pkg.resolve("lib/asyncapi-trigger-compiler-plugin.jar")));
        Assert.assertTrue(getErr().contains("Ballerina.toml could not be updated"), "Err: " + getErr());
        Assert.assertTrue(getErr().contains("id = \"trigger-plugin\" by hand"), "Err: " + getErr());
    }

    @Test(description = "--dry-run lists the jar and the old entry's removal without writing anything")
    public void testDryRunReportsThePlugin() throws Exception {
        Path pkg = newPackage("dryentry");
        String before = Files.readString(pkg.resolve("Ballerina.toml"))
                + "\n[[tool.asyncapi]]\nid = \"trigger-plugin\"\nversion = \"0.13.3\"\n";
        Files.writeString(pkg.resolve("Ballerina.toml"), before);
        generateTiny(pkg, "-o", pkg.toString(), "--dry-run");
        Assert.assertTrue(getOut().contains("asyncapi-trigger-compiler-plugin.jar"), "Out: " + getOut());
        Assert.assertTrue(getOut().contains("update     Ballerina.toml"), "Out: " + getOut());
        Assert.assertEquals(Files.readString(pkg.resolve("Ballerina.toml")), before);
        Assert.assertFalse(Files.exists(pkg.resolve("lib")));
    }

    @Test(description = "A package with another compiler plugin keeps it, with a warning that ours was not added")
    public void testOtherCompilerPluginIsKept() throws Exception {
        Path pkg = newPackage("otherplugin");
        String theirs = "[plugin]\nid = \"their-plugin\"\nclass = \"org.example.Plugin\"\n";
        Files.writeString(pkg.resolve("CompilerPlugin.toml"), theirs);
        generateTiny(pkg, "-o", pkg.toString(), "--force");
        Assert.assertTrue(getErr().contains("already has a compiler plugin (their-plugin)"), "Err: " + getErr());
        Assert.assertEquals(Files.readString(pkg.resolve("CompilerPlugin.toml")), theirs);
        Assert.assertFalse(Files.readString(pkg.resolve("Ballerina.toml")).contains("[[tool.asyncapi]]"),
                "No tool entry for a plugin that wasn't added");
        Assert.assertTrue(Files.exists(pkg.resolve("listener.bal")), "The code should still be generated");
    }

    @Test(description = "A trigger generated into a module gets no plugin, as the plugin belongs to the package")
    public void testModuleGetsNoCompilerPlugin() throws Exception {
        Path pkg = newPackage("moduleplugin");
        generateTiny(pkg, "-o", pkg.toString(), "--module", "events");
        Assert.assertTrue(getErr().isEmpty(), "Err: " + getErr());
        Assert.assertTrue(Files.exists(pkg.resolve("modules/events/listener.bal")), "The code should be generated");
        Assert.assertFalse(Files.exists(pkg.resolve("CompilerPlugin.toml")));
        Assert.assertFalse(Files.exists(pkg.resolve("modules/events/CompilerPlugin.toml")));
        Assert.assertFalse(Files.exists(pkg.resolve("lib")));
        Assert.assertFalse(Files.readString(pkg.resolve("Ballerina.toml")).contains("[[tool.asyncapi]]"));
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
        // --with-tests so the run would also create the tests folder
        generateTiny(pkg, "-o", pkg.toString(), "--dry-run", "--with-tests");
        Assert.assertTrue(getOut().startsWith("Dry run: no files were written."), "Out: " + getOut());
        Assert.assertTrue(getOut().contains("7 to create, 0 to overwrite, 0 to skip, 0 unchanged."),
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
        Assert.assertTrue(getOut().contains("0 to create, 0 to overwrite, 1 to skip, 5 unchanged."),
                "Out: " + getOut());

        generateTiny(pkg, "-o", pkg.toString(), "--dry-run", "--force");
        Assert.assertTrue(getOut().contains("overwrite"), "Out: " + getOut());
        Assert.assertTrue(getOut().contains("0 to create, 1 to overwrite, 0 to skip, 5 unchanged."),
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
