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
 * Tests for {@link OutputStage}, the overwrite rule applied to whatever a generator writes.
 */
public class OutputStageTest extends CmdTestBase {

    private static final String SUCCESS = "done";

    private CmdContext context() {
        return new CmdContext(outStream, errStream, false);
    }

    private CmdOptions options(boolean force, boolean dryRun) {
        return new CmdOptions.CmdOptionsBuilder().withForce(force).withDryRun(dryRun).build();
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
            stage.commit(SUCCESS);
        }
        Assert.assertEquals(Files.readString(out.resolve("a.yaml")), "a");
        Assert.assertEquals(Files.readString(out.resolve("tests").resolve("b.bal")), "b");
        Assert.assertTrue(getOut().contains("-- tests/b.bal"), "Out: " + getOut());
        Assert.assertTrue(getOut().startsWith(SUCCESS), "Out: " + getOut());
    }

    @Test(description = "A file that exists with different content is skipped without --force")
    public void testSkipsChangedFile() throws IOException {
        Path out = Files.createDirectory(tmpDir.resolve("out"));
        Files.writeString(out.resolve("a.yaml"), "mine");
        try (OutputStage stage = OutputStage.open(out, options(false, false), context())) {
            generate(stage, "a.yaml", "generated");
            stage.commit(SUCCESS);
        }
        Assert.assertEquals(Files.readString(out.resolve("a.yaml")), "mine");
        Assert.assertTrue(getErr().contains("Skipped"), "Err: " + getErr());
        Assert.assertTrue(getOut().contains("No files were written."), "Out: " + getOut());
    }

    @Test(description = "Skipped files are counted and summarised, so the caller can exit non-zero")
    public void testSkippedFilesAreCountedAndSummarised() throws IOException {
        Path out = Files.createDirectory(tmpDir.resolve("out"));
        Files.writeString(out.resolve("a.yaml"), "mine");
        Files.writeString(out.resolve("b.yaml"), "mine too");
        int skipped;
        try (OutputStage stage = OutputStage.open(out, options(false, false), context())) {
            generate(stage, "a.yaml", "generated");
            generate(stage, "b.yaml", "generated");
            generate(stage, "c.yaml", "new");
            skipped = stage.commit(SUCCESS);
        }
        Assert.assertEquals(skipped, 2);
        Assert.assertEquals(Files.readString(out.resolve("c.yaml")), "new", "Files that can be written still are");
        Assert.assertTrue(getErr().contains("2 file(s) skipped, so the generated code may be out of sync"),
                "Err: " + getErr());
    }

    @Test(description = "Nothing skipped means a zero count and no summary")
    public void testNoSummaryWhenNothingSkipped() throws IOException {
        Path out = tmpDir.resolve("out");
        int skipped;
        try (OutputStage stage = OutputStage.open(out, options(false, false), context())) {
            generate(stage, "a.yaml", "a");
            skipped = stage.commit(SUCCESS);
        }
        Assert.assertEquals(skipped, 0);
        Assert.assertTrue(getErr().isEmpty(), "Err: " + getErr());
    }

    @Test(description = "A directory where a generated file should go gets a clear error")
    public void testDirectoryInPlaceOfFileIsReported() throws IOException {
        Path out = Files.createDirectory(tmpDir.resolve("out"));
        Files.createDirectory(out.resolve("a.yaml"));
        try (OutputStage stage = OutputStage.open(out, options(true, false), context())) {
            generate(stage, "a.yaml", "generated");
            stage.commit(SUCCESS);
            Assert.fail("A directory in place of the file should be reported");
        } catch (IOException e) {
            Assert.assertTrue(e.getMessage().endsWith("is a directory, so the generated file cannot be written there"),
                    e.getMessage());
        }
    }

    @Test(description = "A failure partway through says which files were already written")
    public void testFailurePartwayListsWrittenFiles() throws IOException {
        Path out = Files.createDirectory(tmpDir.resolve("out"));
        // A file named like the folder the second generated file needs makes that write fail.
        Files.writeString(out.resolve("tests"), "not a folder");
        try (OutputStage stage = OutputStage.open(out, options(false, false), context())) {
            generate(stage, "a.yaml", "a");
            generate(stage, "tests/b.bal", "b");
            stage.commit(SUCCESS);
            Assert.fail("Writing under a file should fail");
        } catch (IOException e) {
            Assert.assertTrue(getErr().contains("these files were written before the failure: a.yaml"),
                    "Err: " + getErr());
        }
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
            stage.commit(SUCCESS);
        }
        Assert.assertTrue(Files.isSymbolicLink(out.resolve("a.yaml")), "The link itself is kept");
        Assert.assertEquals(Files.readString(real), "generated");
    }

    @Test(description = "--force replaces a changed file")
    public void testForceReplacesChangedFile() throws IOException {
        Path out = Files.createDirectory(tmpDir.resolve("out"));
        Files.writeString(out.resolve("a.yaml"), "mine");
        try (OutputStage stage = OutputStage.open(out, options(true, false), context())) {
            generate(stage, "a.yaml", "generated");
            stage.commit(SUCCESS);
        }
        Assert.assertEquals(Files.readString(out.resolve("a.yaml")), "generated");
        Assert.assertTrue(getErr().isEmpty(), "Err: " + getErr());
    }

    @Test(description = "A file that already has the generated content is neither skipped nor rewritten")
    public void testIdenticalFileIsUnchanged() throws IOException {
        Path out = Files.createDirectory(tmpDir.resolve("out"));
        Files.writeString(out.resolve("a.yaml"), "same");
        long before = Files.getLastModifiedTime(out.resolve("a.yaml")).toMillis();
        try (OutputStage stage = OutputStage.open(out, options(false, false), context())) {
            generate(stage, "a.yaml", "same");
            stage.commit(SUCCESS);
        }
        Assert.assertTrue(getErr().isEmpty(), "Err: " + getErr());
        Assert.assertTrue(getOut().contains("already up to date"), "Out: " + getOut());
        Assert.assertEquals(Files.getLastModifiedTime(out.resolve("a.yaml")).toMillis(), before);
    }

    @Test(description = "A dry run writes nothing and does not create the output folder")
    public void testDryRunWritesNothing() throws IOException {
        Path out = tmpDir.resolve("never-created");
        try (OutputStage stage = OutputStage.open(out, options(false, true), context())) {
            generate(stage, "a.yaml", "a");
            stage.commit(SUCCESS);
        }
        Assert.assertFalse(Files.exists(out), "A dry run must not create the output folder");
        Assert.assertTrue(getOut().contains("1 to create, 0 to overwrite, 0 to skip, 0 unchanged."),
                "Out: " + getOut());
        Assert.assertFalse(getOut().contains(SUCCESS), "No success message for a dry run. Out: " + getOut());
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

    @Test(description = "A generator that writes nothing reports that no files were written")
    public void testNothingGenerated() throws IOException {
        try (OutputStage stage = OutputStage.open(tmpDir.resolve("out"), options(false, false), context())) {
            stage.commit(SUCCESS);
        }
        Assert.assertTrue(getOut().contains("No files were written."), "Out: " + getOut());
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
