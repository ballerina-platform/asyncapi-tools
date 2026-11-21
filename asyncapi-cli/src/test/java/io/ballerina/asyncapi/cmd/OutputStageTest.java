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
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests for {@link OutputStage}: the fail-fast pipeline applied to whatever a generator writes.
 */
public class OutputStageTest extends CmdTestBase {

    private CmdContext context() {
        return new CmdContext(outStream, errStream, false);
    }

    private CmdOptions options(boolean force, boolean dryRun) {
        return new CmdOptions.CmdOptionsBuilder().withForce(force).withDryRun(dryRun)
                .withCommandLine("bal asyncapi spec.yaml").build();
    }

    private void generate(OutputStage stage, String name, String content) throws IOException {
        Path file = stage.stagingDir().resolve(name);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    @Test(description = "New files, including ones in sub-folders, are created and listed with forward slashes")
    public void testCreatesNewFilesInSubFolders() throws IOException {
        Path out = tmpDir.resolve("out");
        try (OutputStage stage = OutputStage.open(out, options(false, false), context())) {
            generate(stage, "a.yaml", "a");
            generate(stage, "tests/b.bal", "b");
            stage.commit();
        }
        Assert.assertEquals(Files.readString(out.resolve("a.yaml")), "a");
        Assert.assertEquals(Files.readString(out.resolve("tests").resolve("b.bal")), "b");
        Assert.assertTrue(getOut().contains("tests/b.bal"), "Out: " + getOut());
        Assert.assertFalse(getOut().contains("\\"), "Out: " + getOut());
        Assert.assertTrue(getOut().contains("Generated 2 files."), "Out: " + getOut());
    }

    @Test(description = "A changed file without --force and without a terminal stops the run before any write")
    public void testChangedFileIsRefused() throws IOException {
        Path out = Files.createDirectory(tmpDir.resolve("out"));
        Files.writeString(out.resolve("a.yaml"), "mine");
        Files.writeString(out.resolve("b.yaml"), "mine too");
        try (OutputStage stage = OutputStage.open(out, options(false, false), context())) {
            generate(stage, "a.yaml", "generated");
            generate(stage, "b.yaml", "generated");
            generate(stage, "c.yaml", "new");
            AsyncApiCmdToolException error = Assert.expectThrows(AsyncApiCmdToolException.class, stage::commit);
            Assert.assertEquals(error.exitCode(), ExitCodes.OVERWRITE_REFUSED);
            Assert.assertTrue(error.getMessage().startsWith("2 files already exist: "), error.getMessage());
            Assert.assertEquals(error.getSuggestion(), "Re-run with --force to overwrite.");
            Assert.assertEquals(error.getCommand(), "bal asyncapi spec.yaml --force");
        }
        Assert.assertEquals(Files.readString(out.resolve("a.yaml")), "mine");
        Assert.assertFalse(Files.exists(out.resolve("c.yaml")), "Nothing is written when the run can't complete");
    }

    @Test(description = "A directory where a generated file should go is a clear error")
    public void testDirectoryInPlaceOfFileIsReported() throws IOException {
        Path out = Files.createDirectory(tmpDir.resolve("out"));
        Files.createDirectory(out.resolve("a.yaml"));
        try (OutputStage stage = OutputStage.open(out, options(true, false), context())) {
            generate(stage, "a.yaml", "generated");
            AsyncApiCmdToolException error = Assert.expectThrows(AsyncApiCmdToolException.class, stage::commit);
            Assert.assertTrue(error.getMessage().endsWith("is a directory, so the generated file cannot be written "
                    + "there"), error.getMessage());
            Assert.assertEquals(error.exitCode(), ExitCodes.RUNTIME);
        }
    }

    @Test(description = "A file in the way of a folder the plan needs fails the writability check, before writing")
    public void testUnwritableFolderFailsBeforeWriting() throws IOException {
        Path out = Files.createDirectory(tmpDir.resolve("out"));
        // A file named like the folder the second generated file needs.
        Files.writeString(out.resolve("tests"), "not a folder");
        try (OutputStage stage = OutputStage.open(out, options(false, false), context())) {
            generate(stage, "a.yaml", "a");
            generate(stage, "tests/b.bal", "b");
            AsyncApiCmdToolException error = Assert.expectThrows(AsyncApiCmdToolException.class, stage::commit);
            Assert.assertTrue(error.getMessage().contains("is a file, not a folder"), error.getMessage());
        }
        Assert.assertFalse(Files.exists(out.resolve("a.yaml")), "Nothing is written when a folder is in the way");
    }

    @Test(description = "A symbolic link is written through to the file it points to")
    public void testSymbolicLinkIsWrittenThrough() throws IOException {
        Path out = Files.createDirectory(tmpDir.resolve("out"));
        Path real = Files.writeString(out.resolve("real.yaml"), "old");
        try {
            Files.createSymbolicLink(out.resolve("a.yaml"), real.getFileName());
        } catch (IOException | UnsupportedOperationException e) {
            throw new SkipException("Symbolic links cannot be created here: " + e.getMessage());
        }
        try (OutputStage stage = OutputStage.open(out, options(true, false), context())) {
            generate(stage, "a.yaml", "generated");
            stage.commit();
        }
        Assert.assertTrue(Files.isSymbolicLink(out.resolve("a.yaml")), "The link itself is kept");
        Assert.assertEquals(Files.readString(real), "generated");
    }

    @Test(description = "--force replaces a changed file without asking")
    public void testForceReplacesChangedFile() throws IOException {
        Path out = Files.createDirectory(tmpDir.resolve("out"));
        Files.writeString(out.resolve("a.yaml"), "mine");
        try (OutputStage stage = OutputStage.open(out, options(true, false), context())) {
            generate(stage, "a.yaml", "generated");
            stage.commit();
        }
        Assert.assertEquals(Files.readString(out.resolve("a.yaml")), "generated");
        Assert.assertTrue(getErr().isEmpty(), "Err: " + getErr());
        Assert.assertTrue(getOut().startsWith("overwritten"), "Out: " + getOut());
    }

    @Test(description = "A file marked as merged is reported as merged, and needs --force like an overwrite")
    public void testMergedFile() throws IOException {
        Path out = Files.createDirectory(tmpDir.resolve("out"));
        Files.writeString(out.resolve("Ballerina.toml"), "old");
        try (OutputStage stage = OutputStage.open(out, options(true, false), context())) {
            generate(stage, "Ballerina.toml", "updated");
            stage.markMerged(Path.of("Ballerina.toml"));
            stage.commit();
        }
        Assert.assertTrue(getOut().startsWith("merged"), "Out: " + getOut());
    }

    @Test(description = "A file that already has the generated content is neither refused nor rewritten")
    public void testIdenticalFileIsUnchanged() throws IOException {
        Path out = Files.createDirectory(tmpDir.resolve("out"));
        Files.writeString(out.resolve("a.yaml"), "same");
        long before = Files.getLastModifiedTime(out.resolve("a.yaml")).toMillis();
        try (OutputStage stage = OutputStage.open(out, options(false, false), context())) {
            generate(stage, "a.yaml", "same");
            stage.commit();
        }
        Assert.assertTrue(getErr().isEmpty(), "Err: " + getErr());
        Assert.assertTrue(getOut().contains("already up to date"), "Out: " + getOut());
        Assert.assertEquals(Files.getLastModifiedTime(out.resolve("a.yaml")).toMillis(), before);
    }

    @Test(description = "A dry run writes nothing, does not create the output folder and never prompts")
    public void testDryRunWritesNothing() throws IOException {
        Path out = tmpDir.resolve("never-created");
        try (OutputStage stage = OutputStage.open(out, options(false, true), context())) {
            generate(stage, "a.yaml", "a");
            stage.commit();
        }
        Assert.assertFalse(Files.exists(out), "A dry run must not create the output folder");
        Assert.assertTrue(getOut().startsWith("would create  "), "Out: " + getOut());
        Assert.assertTrue(getOut().strip().endsWith("Dry run: nothing was written."), "Out: " + getOut());
    }

    @Test(description = "A dry run lists a changed file as an overwrite, without --force and without failing")
    public void testDryRunListsOverwrite() throws IOException {
        Path out = Files.createDirectory(tmpDir.resolve("out"));
        Files.writeString(out.resolve("a.yaml"), "mine");
        try (OutputStage stage = OutputStage.open(out, options(false, true), context())) {
            generate(stage, "a.yaml", "generated");
            stage.commit();
        }
        Assert.assertTrue(getOut().startsWith("would overwrite  "), "Out: " + getOut());
        Assert.assertEquals(Files.readString(out.resolve("a.yaml")), "mine");
    }

    @Test(description = "The JSON result lists every file with its action, including unchanged ones")
    public void testJsonResult() throws IOException {
        Path out = Files.createDirectory(tmpDir.resolve("out"));
        Files.writeString(out.resolve("same.yaml"), "same");
        CmdContext ctx = context();
        ctx.setFormat(CmdContext.Format.JSON);
        try (OutputStage stage = OutputStage.open(out, options(false, false), ctx)) {
            generate(stage, "new.yaml", "new");
            generate(stage, "same.yaml", "same");
            stage.commit();
        }
        Assert.assertTrue(getOut().contains("\"action\":\"created\""), "Out: " + getOut());
        Assert.assertTrue(getOut().contains("\"action\":\"unchanged\""), "Out: " + getOut());
        Assert.assertFalse(getOut().contains("dryRun"), "Out: " + getOut());
    }

    @Test(description = "The temporary staging folder is removed when the stage is closed")
    public void testStagingDirectoryIsRemoved() throws IOException {
        Path staging;
        try (OutputStage stage = OutputStage.open(tmpDir.resolve("out"), options(false, false), context())) {
            staging = stage.stagingDir();
            generate(stage, "tests/a.bal", "a");
            Assert.assertTrue(Files.exists(staging));
        }
        Assert.assertFalse(Files.exists(staging), "The staging folder should be deleted");
    }

    @Test(description = "A generator that writes nothing reports that no files were generated")
    public void testNothingGenerated() throws IOException {
        try (OutputStage stage = OutputStage.open(tmpDir.resolve("out"), options(false, false), context())) {
            stage.commit();
        }
        Assert.assertTrue(getOut().contains("No files were generated."), "Out: " + getOut());
    }

    @Test(description = "--force and --dry-run reach the options from the command line")
    public void testFlagsMapToOptions() {
        BaseCmd base = new BaseCmd();
        Assert.assertFalse(CmdUtils.collectCmdOptions(base).isForce());
        Assert.assertFalse(CmdUtils.collectCmdOptions(base).isDryRun());
        base.force = true;
        base.dryRun = true;
        Assert.assertTrue(CmdUtils.collectCmdOptions(base).isForce());
        Assert.assertTrue(CmdUtils.collectCmdOptions(base).isDryRun());
    }
}
