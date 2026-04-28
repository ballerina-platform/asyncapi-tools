package io.ballerina.asyncapi.cmd;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Unit tests for {@link CmdUtils} and {@link CmdOptions}.
 */
public class CmdUtilsTest extends CmdTestBase {

    @Test(description = "isValidInputFile returns false for null")
    public void testNullInput() {
        Assert.assertFalse(CmdUtils.isValidInputFile(null));
    }

    @Test(description = "isValidInputFile returns false for "
            + "blank string")
    public void testBlankInput() {
        Assert.assertFalse(CmdUtils.isValidInputFile("   "));
    }

    @Test(description = "isValidInputFile returns false for "
            + "non-existent file")
    public void testNonExistentFile() {
        Assert.assertFalse(
            CmdUtils.isValidInputFile("/does/not/exist.yaml"));
    }

    @Test(description = "isValidInputFile returns true for "
            + "existing .yaml file")
    public void testValidYamlFile() throws IOException {
        Path file = Files.createTempFile(
            tmpDir, "test-", ".yaml");
        Assert.assertTrue(
            CmdUtils.isValidInputFile(file.toString()));
    }

    @Test(description = "isValidInputFile returns true for "
            + "existing .yml file")
    public void testValidYmlFile() throws IOException {
        Path file = Files.createTempFile(
            tmpDir, "test-", ".yml");
        Assert.assertTrue(
            CmdUtils.isValidInputFile(file.toString()));
    }

    @Test(description = "isValidInputFile returns true for "
            + "existing .json file")
    public void testValidJsonFile() throws IOException {
        Path file = Files.createTempFile(
            tmpDir, "test-", ".json");
        Assert.assertTrue(
            CmdUtils.isValidInputFile(file.toString()));
    }

    @Test(description = "isValidInputFile returns false for "
            + "existing .bal file")
    public void testBalFileIsInvalid() throws IOException {
        Path file = Files.createTempFile(
            tmpDir, "test-", ".bal");
        Assert.assertFalse(
            CmdUtils.isValidInputFile(file.toString()));
    }

    @Test(description = "isValidBallerinaFile returns false for "
            + "non-existent .bal file")
    void testIsValidBallerinaFileNonExistent() {
        Assert.assertFalse(
            CmdUtils.isValidBallerinaFile("/does/not/exist.bal"));
    }

    @Test(description = "isValidBallerinaFile returns true for "
            + "existing .bal file")
    void testIsValidBallerinaFileExists() throws IOException {
        Path file = Files.createTempFile(
            tmpDir, "test-", ".bal");
        Assert.assertTrue(
            CmdUtils.isValidBallerinaFile(file.toString()));
    }

    @Test(description = "isValidBallerinaFile returns false for "
            + "existing .yaml file")
    void testIsValidBallerinaFileWrongExtension()
            throws IOException {
        Path file = Files.createTempFile(
            tmpDir, "test-", ".yaml");
        Assert.assertFalse(
            CmdUtils.isValidBallerinaFile(file.toString()));
    }

    @Test(description = "INVALID_INPUT_PATH formats with the "
            + "actual path, not literal {0}")
    void testInvalidInputPathFormatContainsPath() {
        String path = "/some/missing/file.yaml";
        String msg = String.format(
            ErrorMessages.INVALID_INPUT_PATH, path);
        Assert.assertTrue(
            msg.contains(path),
            "Formatted message should contain the path. Got: "
                + msg);
        Assert.assertFalse(
            msg.contains("{0}"),
            "Formatted message must not contain literal {0}. "
                + "Got: " + msg);
    }

    @Test(description = "collectCmdOptions maps all BaseCmd "
            + "fields into CmdOptions correctly")
    public void testCollectCmdOptions() {
        BaseCmd base = new BaseCmd();
        base.inputPath = "input.yaml";
        base.outputPath = "/out";
        base.licensePath = "license.txt";
        base.serviceName = "MyService";
        base.jsonFlag = true;
        base.withTests = true;

        CmdOptions opts = CmdUtils.collectCmdOptions(base);

        Assert.assertEquals(opts.getInput(), "input.yaml");
        Assert.assertEquals(opts.getOutput(), "/out");
        Assert.assertEquals(
            opts.getLicenseFilePath(), "license.txt");
        Assert.assertEquals(opts.getService(), "MyService");
        Assert.assertTrue(opts.isJson());
        Assert.assertTrue(opts.isWithTests());
    }

    @Test(description = "collectCmdOptions maps outputPath correctly")
    public void testCollectCmdOptionsWithOutput() {
        BaseCmd base = new BaseCmd();
        base.outputPath = "some/output/dir";
        CmdOptions opts = CmdUtils.collectCmdOptions(base);
        Assert.assertEquals(opts.getOutput(), "some/output/dir");
    }

    @Test(description = "collectCmdOptions maps licensePath correctly")
    public void testCollectCmdOptionsWithLicense() {
        BaseCmd base = new BaseCmd();
        base.licensePath = "some/license.txt";
        CmdOptions opts = CmdUtils.collectCmdOptions(base);
        Assert.assertEquals(
            opts.getLicenseFilePath(), "some/license.txt");
    }

    @Test(description = "collectCmdOptions maps serviceName correctly")
    public void testCollectCmdOptionsWithServiceName() {
        BaseCmd base = new BaseCmd();
        base.serviceName = "MyService";
        CmdOptions opts = CmdUtils.collectCmdOptions(base);
        Assert.assertEquals(opts.getService(), "MyService");
    }

    @Test(description = "collectCmdOptions maps withTests = true correctly")
    public void testCollectCmdOptionsWithTests() {
        BaseCmd base = new BaseCmd();
        base.withTests = true;
        CmdOptions opts = CmdUtils.collectCmdOptions(base);
        Assert.assertTrue(opts.isWithTests());
    }

    @Test(description = "collectCmdOptions maps jsonFlag = true correctly")
    public void testCollectCmdOptionsWithJson() {
        BaseCmd base = new BaseCmd();
        base.jsonFlag = true;
        CmdOptions opts = CmdUtils.collectCmdOptions(base);
        Assert.assertTrue(opts.isJson());
    }

    @Test(description = "collectCmdOptions maps all six fields correctly")
    public void testCollectCmdOptionsAllFields() {
        BaseCmd base = new BaseCmd();
        base.inputPath = "input.yaml";
        base.outputPath = "some/output/dir";
        base.licensePath = "some/license.txt";
        base.serviceName = "MyService";
        base.withTests = true;
        base.jsonFlag = true;
        CmdOptions opts = CmdUtils.collectCmdOptions(base);
        Assert.assertEquals(opts.getInput(), "input.yaml");
        Assert.assertEquals(opts.getOutput(), "some/output/dir");
        Assert.assertEquals(
            opts.getLicenseFilePath(), "some/license.txt");
        Assert.assertEquals(opts.getService(), "MyService");
        Assert.assertTrue(opts.isWithTests());
        Assert.assertTrue(opts.isJson());
    }

    @Test(description = "CmdOptions builder produces correct "
            + "immutable object")
    public void testCmdOptionsBuilder() {
        CmdOptions opts = new CmdOptions.CmdOptionsBuilder()
            .withInput("spec.yaml")
            .withOutput("/output")
            .withLicenseFilePath(null)
            .withService(null)
            .withJson(false)
            .withTests(false)
            .build();

        Assert.assertEquals(opts.getInput(), "spec.yaml");
        Assert.assertEquals(opts.getOutput(), "/output");
        Assert.assertNull(opts.getLicenseFilePath());
        Assert.assertNull(opts.getService());
        Assert.assertFalse(opts.isJson());
        Assert.assertFalse(opts.isWithTests());
    }
}

