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
import picocli.CommandLine;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;

/**
 * Tests for {@code --protocol http}: input validation, the capability checks, the trigger's compiler plugin, and
 * the fail-fast pipeline: plan, refuse or ask before replacing files, dry run, then write.
 */
public class HttpCmdUnitTest extends CmdTestBase {

    @Test(description = "A definition that does not exist fails with exit 1")
    public void testInvalidInputPath() {
        AsyncApiCmd cmd = run("/non/existent/file.yaml", "--protocol", "http");
        Assert.assertTrue(getErr().contains("error: '/non/existent/file.yaml' does not exist"), "Err: " + getErr());
        Assert.assertEquals(cmd.exitCode(), ExitCodes.RUNTIME);
    }

    @Test(description = "A .bal input is a usage error because http cannot generate a definition from code")
    public void testBalInputRejected() throws IOException {
        Path balFile = Files.createTempFile(tmpDir, "service-", ".bal");
        AsyncApiCmd cmd = run(balFile.toString(), "--protocol", "http");
        Assert.assertTrue(getErr().contains("the http protocol can't generate an AsyncAPI definition from Ballerina "
                + "code"), "Err: " + getErr());
        Assert.assertEquals(cmd.exitCode(), ExitCodes.USAGE);
    }

    @Test(description = "A package directory is rejected for http too, for the same reason")
    public void testDirectoryInputRejected() throws IOException {
        Path dir = Files.createDirectory(tmpDir.resolve("pkg"));
        run(dir.toString(), "--protocol", "http");
        Assert.assertTrue(getErr().contains("the http protocol can't generate"), "Err: " + getErr());
    }

    @Test(description = "Without --with-tests no test file is written, even when the spec has x-ballerina-auth")
    public void testNoTestsWithoutFlag() throws Exception {
        Path pkg = newPackage("notests");
        generateTiny("-o", pkg.toString());
        Assert.assertTrue(getErr().isEmpty(), "Err: " + getErr());
        Assert.assertFalse(Files.exists(pkg.resolve("tests/dispatch_test.bal")), "Tests were not requested");
    }

    @Test(description = "--with-tests writes the dispatch tests for a spec with x-ballerina-auth")
    public void testWithTestsWritesDispatchTests() throws Exception {
        Path pkg = newPackage("withtests");
        generateTiny("-o", pkg.toString(), "--with-tests");
        Assert.assertTrue(getErr().isEmpty(), "Err: " + getErr());
        Assert.assertTrue(Files.exists(pkg.resolve("tests/dispatch_test.bal")), "dispatch_test.bal should exist");
        Assert.assertTrue(getOut().contains("tests/dispatch_test.bal"), "Out: " + getOut());
    }

    @Test(description = "--with-tests on a spec without x-ballerina-auth warns that no tests could be written")
    public void testWithTestsWithoutAuthWarns() throws Exception {
        Path pkg = newPackage("noauth");
        String tiny = Files.readString(resource("specs/tiny_http.yaml"));
        String noAuth = tiny.replaceAll("(?s)x-ballerina-auth:.*?(?=info:)", "");
        Path spec = Files.writeString(tmpDir.resolve("noauth.yaml"), noAuth);
        run(spec.toString(), "--protocol", "http", "-o", pkg.toString(), "--with-tests");
        Assert.assertTrue(getErr().contains("warning: --with-tests generated no tests"), "Err: " + getErr());
        Assert.assertTrue(Files.exists(pkg.resolve("listener.bal")), "The code should still be generated");
        Assert.assertFalse(Files.exists(pkg.resolve("tests/dispatch_test.bal")));
    }

    @Test(description = "The protocol is read from x-ballerina-event-identifier, so --protocol is not needed")
    public void testProtocolDetected() throws Exception {
        Path pkg = newPackage("detected");
        AsyncApiCmd cmd = run(resource("specs/tiny_http.yaml").toString(), "-o", pkg.toString());
        Assert.assertEquals(cmd.exitCode(), ExitCodes.SUCCESS, "Err: " + getErr());
        Assert.assertTrue(Files.exists(pkg.resolve("listener.bal")), "Err: " + getErr());
    }

    @Test(description = "--spec-side warns that http does not support it")
    public void testSpecSideWarns() throws IOException {
        Path specFile = Files.createTempFile(tmpDir, "spec-", ".yaml");
        run(specFile.toString(), "--protocol", "http", "--spec-side", "service");
        Assert.assertTrue(getErr().contains("warning: --spec-side is not supported for the http protocol"),
                "Err: " + getErr());
    }

    @Test(description = "--license is put at the top of every generated .bal file, before the notice (IO-2)")
    public void testLicenseIsApplied() throws Exception {
        Path pkg = newPackage("licensed");
        Path license = Files.writeString(tmpDir.resolve("license.txt"), "// Licensed to Example\n");
        generateTiny("-o", pkg.toString(), "--license", license.toString());
        Assert.assertTrue(getErr().isEmpty(), "Err: " + getErr());
        for (String name : new String[]{"types.bal", "listener.bal", "service_types.bal", "dispatcher_service.bal"}) {
            Assert.assertTrue(Files.readString(pkg.resolve(name)).startsWith("// Licensed to Example\n\n"
                    + GeneratedHeader.NOTICE + "\n\n"), name);
        }
        Assert.assertFalse(Files.readString(pkg.resolve("CompilerPlugin.toml")).contains("Licensed"),
                "Only .bal files get the license");
    }

    @Test(description = "--spec-format and --service warn when generating code from a spec")
    public void testSpecOutputOnlyOptionsWarn() throws IOException {
        Path specFile = Files.createTempFile(tmpDir, "spec-", ".yaml");
        run(specFile.toString(), "--protocol", "http", "--spec-format", "json", "--service", "MyService");
        Assert.assertTrue(getErr().contains("warning: --spec-format only applies"), "Err: " + getErr());
        Assert.assertTrue(getErr().contains("warning: --service only applies"), "Err: " + getErr());
    }

    private void generateTiny(String... extra) throws Exception {
        String[] args = new String[3 + extra.length];
        args[0] = resource("specs/tiny_http.yaml").toString();
        args[1] = "--protocol";
        args[2] = "http";
        System.arraycopy(extra, 0, args, 3, extra.length);
        errContent.reset();
        outContent.reset();
        lastExitCode = run(args).exitCode();
    }

    private int lastExitCode;

    @Test(description = "A first run writes every generated file and lists them")
    public void testFirstRunWritesAndListsFiles() throws Exception {
        Path pkg = newPackage("firstrun");
        generateTiny("-o", pkg.toString());
        Assert.assertTrue(getErr().isEmpty(), "Err: " + getErr());
        Assert.assertEquals(lastExitCode, ExitCodes.SUCCESS);
        Assert.assertTrue(getOut().strip().endsWith("files."), "Out: " + getOut());
        for (String name : new String[]{"types.bal", "listener.bal", "service_types.bal", "dispatcher_service.bal"}) {
            Assert.assertTrue(Files.exists(pkg.resolve(name)), name + " should exist");
            Assert.assertTrue(getOut().contains(name), name + " should be listed. Out: " + getOut());
        }
        Assert.assertTrue(getOut().startsWith("created"), "Out: " + getOut());
    }

    @Test(description = "A trigger at a package root gets the plugin jar in lib/ and a CompilerPlugin.toml for it")
    public void testTriggerGetsCompilerPlugin() throws Exception {
        Path pkg = newPackage("plugin");
        String ballerinaToml = Files.readString(pkg.resolve("Ballerina.toml"));
        generateTiny("-o", pkg.toString());
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
        Assert.assertTrue(getOut().contains("CompilerPlugin.toml"), "Out: " + getOut());
        Assert.assertTrue(getOut().contains("lib/asyncapi-trigger-compiler-plugin.jar"), "Out: " + getOut());
    }

    @Test(description = "Regenerating a trigger made by 0.13.3 removes the [[tool.asyncapi]] entry, as a merge")
    public void testRegenerationRemovesTheOldToolEntry() throws Exception {
        Path pkg = newPackage("regen");
        String before = Files.readString(pkg.resolve("Ballerina.toml"));
        Files.writeString(pkg.resolve("Ballerina.toml"), before + "\n# Copies the trigger's compiler plugin from the "
                + "asyncapi tool during the build\n[[tool.asyncapi]]\nid = \"trigger-plugin\"\nversion = \"0.13.3\"\n");
        generateTiny("-o", pkg.toString(), "--force");
        Assert.assertEquals(lastExitCode, ExitCodes.SUCCESS, "Err: " + getErr());
        Assert.assertEquals(Files.readString(pkg.resolve("Ballerina.toml")), before);
        Assert.assertTrue(getOut().contains("merged") && getOut().contains("Ballerina.toml"), "Out: " + getOut());
    }

    @Test(description = "Without --force and a terminal, nothing at all is written when a file would be replaced")
    public void testNothingWrittenWhenAReplaceIsRefused() throws Exception {
        Path pkg = newPackage("refused");
        String withEntry = Files.readString(pkg.resolve("Ballerina.toml"))
                + "\n[[tool.asyncapi]]\nid = \"trigger-plugin\"\nversion = \"0.13.3\"\n";
        String oldPluginToml = "[plugin]\nid = \"asyncapi-compiler-plugin\"\nclass = \"x.Plugin\"\n\n"
                + "[[dependency]]\npath = \"target/tool-cache/asyncapi-trigger-compiler-plugin.jar\"\n";
        Files.writeString(pkg.resolve("Ballerina.toml"), withEntry);
        Files.writeString(pkg.resolve("CompilerPlugin.toml"), oldPluginToml);
        generateTiny("-o", pkg.toString());
        Assert.assertEquals(lastExitCode, ExitCodes.OVERWRITE_REFUSED);
        Assert.assertTrue(getErr().contains("2 files already exist"), "Err: " + getErr());
        Assert.assertTrue(getErr().contains("Ballerina.toml") && getErr().contains("CompilerPlugin.toml"),
                "Err: " + getErr());
        Assert.assertEquals(Files.readString(pkg.resolve("CompilerPlugin.toml")), oldPluginToml);
        Assert.assertEquals(Files.readString(pkg.resolve("Ballerina.toml")), withEntry);
        Assert.assertFalse(Files.exists(pkg.resolve("listener.bal")), "No new file is written either (FF-1)");
        Assert.assertTrue(getOut().isEmpty(), "Out: " + getOut());
    }

    @Test(description = "A destination that can't be written fails before anything is written (FF-4 phase 5)")
    public void testUnwritableDestinationFailsFirst() throws Exception {
        Path pkg = newPackage("readonly");
        Path ballerinaToml = pkg.resolve("Ballerina.toml");
        Files.writeString(ballerinaToml, Files.readString(ballerinaToml)
                + "\n[[tool.asyncapi]]\nid = \"trigger-plugin\"\nversion = \"0.13.3\"\n");
        Assert.assertTrue(ballerinaToml.toFile().setWritable(false), "Could not make Ballerina.toml read-only");
        try {
            generateTiny("-o", pkg.toString(), "--force");
        } finally {
            ballerinaToml.toFile().setWritable(true);
        }
        Assert.assertEquals(lastExitCode, ExitCodes.RUNTIME);
        Assert.assertTrue(getErr().contains("cannot write") && getErr().contains("Ballerina.toml: the file is "
                + "read-only"), "Err: " + getErr());
        Assert.assertFalse(Files.exists(pkg.resolve("lib")), "Nothing is written");
    }

    @Test(description = "--dry-run lists the jar and the Ballerina.toml merge without writing anything")
    public void testDryRunReportsThePlugin() throws Exception {
        Path pkg = newPackage("dryentry");
        String before = Files.readString(pkg.resolve("Ballerina.toml"))
                + "\n[[tool.asyncapi]]\nid = \"trigger-plugin\"\nversion = \"0.13.3\"\n";
        Files.writeString(pkg.resolve("Ballerina.toml"), before);
        generateTiny("-o", pkg.toString(), "--dry-run");
        Assert.assertEquals(lastExitCode, ExitCodes.SUCCESS, "Err: " + getErr());
        Assert.assertTrue(getOut().contains("would create"), "Out: " + getOut());
        Assert.assertTrue(getOut().contains("asyncapi-trigger-compiler-plugin.jar"), "Out: " + getOut());
        Assert.assertTrue(getOut().contains("would merge") && getOut().contains("Ballerina.toml"), "Out: " + getOut());
        Assert.assertTrue(getOut().strip().endsWith("Dry run: nothing was written."), "Out: " + getOut());
        Assert.assertEquals(Files.readString(pkg.resolve("Ballerina.toml")), before);
        Assert.assertFalse(Files.exists(pkg.resolve("lib")));
    }

    @Test(description = "A package with another compiler plugin keeps it, with a warning that ours was not added")
    public void testOtherCompilerPluginIsKept() throws Exception {
        Path pkg = newPackage("otherplugin");
        String theirs = "[plugin]\nid = \"their-plugin\"\nclass = \"org.example.Plugin\"\n";
        Files.writeString(pkg.resolve("CompilerPlugin.toml"), theirs);
        generateTiny("-o", pkg.toString(), "--force");
        Assert.assertTrue(getErr().contains("already has a compiler plugin (their-plugin)"), "Err: " + getErr());
        Assert.assertEquals(Files.readString(pkg.resolve("CompilerPlugin.toml")), theirs);
        Assert.assertFalse(Files.readString(pkg.resolve("Ballerina.toml")).contains("[[tool.asyncapi]]"));
        Assert.assertTrue(Files.exists(pkg.resolve("listener.bal")), "The code should still be generated");
    }

    @Test(description = "A trigger generated into a new module creates it and says so (MOD-2), with no plugin")
    public void testModuleIsCreatedWithoutCompilerPlugin() throws Exception {
        Path pkg = newPackage("moduleplugin");
        generateTiny("-o", pkg.toString(), "--module", "events");
        Assert.assertTrue(getErr().isEmpty(), "Err: " + getErr());
        Assert.assertTrue(getOut().contains("module events"), "Out: " + getOut());
        Assert.assertTrue(Files.exists(pkg.resolve("modules/events/listener.bal")), "The code should be generated");
        Assert.assertFalse(Files.exists(pkg.resolve("CompilerPlugin.toml")));
        Assert.assertFalse(Files.exists(pkg.resolve("lib")));
    }

    @Test(description = "Running again with nothing changed is quiet and writes nothing")
    public void testRerunIsUnchanged() throws Exception {
        Path pkg = newPackage("rerun");
        generateTiny("-o", pkg.toString());
        generateTiny("-o", pkg.toString());
        Assert.assertTrue(getErr().isEmpty(), "Unchanged files need no --force. Err: " + getErr());
        Assert.assertEquals(lastExitCode, ExitCodes.SUCCESS);
        Assert.assertTrue(getOut().contains("No changes: the files are already up to date."), "Out: " + getOut());
    }

    @Test(description = "A changed file is not replaced without --force: exit 4 and the command to run (FF-6)")
    public void testChangedFileRefusedWithoutForce() throws Exception {
        Path pkg = newPackage("changed");
        generateTiny("-o", pkg.toString());
        Files.writeString(pkg.resolve("types.bal"), "// hand edit");
        generateTiny("-o", pkg.toString());
        Assert.assertEquals(lastExitCode, ExitCodes.OVERWRITE_REFUSED);
        Assert.assertTrue(getErr().contains("types.bal already exists"), "Err: " + getErr());
        Assert.assertTrue(getErr().contains("Try: bal asyncapi ") && getErr().strip().endsWith("--force"),
                "Err: " + getErr());
        Assert.assertEquals(Files.readString(pkg.resolve("types.bal")), "// hand edit");
    }

    @Test(description = "--force overwrites the file that differs and leaves the unchanged ones alone")
    public void testForceOverwrites() throws Exception {
        Path pkg = newPackage("force");
        generateTiny("-o", pkg.toString());
        Files.writeString(pkg.resolve("types.bal"), "// hand edit");
        generateTiny("-o", pkg.toString(), "--force");
        Assert.assertTrue(getErr().isEmpty(), "Err: " + getErr());
        Assert.assertNotEquals(Files.readString(pkg.resolve("types.bal")), "// hand edit");
        Assert.assertTrue(getOut().startsWith("overwritten"), "Out: " + getOut());
        Assert.assertFalse(getOut().contains("listener.bal"), "Unchanged files are not listed. Out: " + getOut());
        Assert.assertTrue(getOut().contains("Generated 1 file."), "Out: " + getOut());
    }

    @Test(description = "On a terminal the user is asked once; yes writes, no exits 4 (FF-5)")
    public void testPromptOnTerminal() throws Exception {
        Path pkg = newPackage("prompt");
        generateTiny("-o", pkg.toString());
        Files.writeString(pkg.resolve("types.bal"), "// hand edit");

        runInteractive("n\n", "-o", pkg.toString());
        Assert.assertEquals(lastExitCode, ExitCodes.OVERWRITE_REFUSED, "Err: " + getErr());
        Assert.assertTrue(getErr().contains("Overwrite these file(s)? [y/N]"), "Err: " + getErr());
        Assert.assertEquals(Files.readString(pkg.resolve("types.bal")), "// hand edit");

        runInteractive("y\n", "-o", pkg.toString());
        Assert.assertEquals(lastExitCode, ExitCodes.SUCCESS, "Err: " + getErr());
        Assert.assertNotEquals(Files.readString(pkg.resolve("types.bal")), "// hand edit");
    }

    private void runInteractive(String answer, String... extra) throws Exception {
        errContent.reset();
        outContent.reset();
        CmdContext ctx = new CmdContext(outStream, errStream,
                new ByteArrayInputStream(answer.getBytes(StandardCharsets.UTF_8)), false, CmdContext.Format.TEXT, true);
        AsyncApiCmd cmd = new AsyncApiCmd(ctx);
        String[] args = new String[3 + extra.length];
        args[0] = resource("specs/tiny_http.yaml").toString();
        args[1] = "--protocol";
        args[2] = "http";
        System.arraycopy(extra, 0, args, 3, extra.length);
        new CommandLine(cmd).parseArgs(args);
        cmd.execute();
        lastExitCode = cmd.exitCode();
    }

    @Test(description = "--dry-run lists the plan and writes nothing")
    public void testDryRunWritesNothing() throws Exception {
        Path pkg = newPackage("dryrun");
        // --with-tests so the run would also create the tests folder
        generateTiny("-o", pkg.toString(), "--dry-run", "--with-tests");
        Assert.assertEquals(lastExitCode, ExitCodes.SUCCESS);
        Assert.assertTrue(getOut().contains("would create"), "Out: " + getOut());
        Assert.assertTrue(getOut().strip().endsWith("Dry run: nothing was written."), "Out: " + getOut());
        Assert.assertFalse(Files.exists(pkg.resolve("types.bal")), "A dry run must not write");
        Assert.assertFalse(Files.exists(pkg.resolve("tests")), "A dry run must not create folders");
    }

    @Test(description = "--dry-run does not create a module folder that is missing, and says it would")
    public void testDryRunDoesNotCreateModuleFolder() throws Exception {
        Path pkg = newPackage("drymodule");
        generateTiny("-o", pkg.toString(), "--module", "events", "--dry-run");
        Assert.assertTrue(getOut().contains("would create  module events"), "Out: " + getOut() + " Err: " + getErr());
        Assert.assertFalse(Files.exists(pkg.resolve("modules")), "A dry run must not create the module");
    }

    @Test(description = "--dry-run lists a changed file as an overwrite without prompting, and exits 0 (FF-7)")
    public void testDryRunListsOverwrites() throws Exception {
        Path pkg = newPackage("dryoverwrite");
        generateTiny("-o", pkg.toString());
        Files.writeString(pkg.resolve("types.bal"), "// hand edit");
        generateTiny("-o", pkg.toString(), "--dry-run");
        Assert.assertEquals(lastExitCode, ExitCodes.SUCCESS, "Err: " + getErr());
        Assert.assertTrue(getOut().contains("would overwrite") && getOut().contains("types.bal"), "Out: " + getOut());
        Assert.assertEquals(Files.readString(pkg.resolve("types.bal")), "// hand edit");
    }

    @Test(description = "No warning is printed when only supported options are given")
    public void testNoWarningForSupportedOptions() throws IOException {
        Path specFile = Files.createTempFile(tmpDir, "spec-", ".yaml");
        run(specFile.toString(), "--protocol", "http", "--output", tmpDir.resolve("out").toString());
        Assert.assertFalse(getErr().contains("warning:"), "Unexpected warning. Err: " + getErr());
    }
}
