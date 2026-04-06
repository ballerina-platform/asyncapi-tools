package io.ballerina.asyncapi.cmd;

import org.testng.Assert;
import org.testng.annotations.Test;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Unit tests for {@link Http} subcommand — validation logic,
 * flag handling, and error output without real code generation.
 */
public class HttpCmdUnitTest extends CmdTestBase {

    @Test(description = "Http with --help does not print error")
    public void testHelpFlag() {
        Http cmd = new Http(outStream, errStream, false);
        new CommandLine(cmd).parseArgs("--help");
        cmd.execute();
        Assert.assertTrue(getErr().isEmpty(),
            "No error expected for --help. Err: " + getErr());
    }

    @Test(description = "Http with no --input prints missing "
            + "input error to errStream")
    public void testMissingInputFlag() {
        Http cmd = new Http(outStream, errStream, false);
        new CommandLine(cmd).parseArgs();
        cmd.setExitWhenFinish(false);
        cmd.execute();
        String err = getErr();
        Assert.assertTrue(
            err.contains(ErrorMessages.MISSING_INPUT_FLAG),
            "Expected missing input error. Err: " + err);
    }

    @Test(description = "Http with non-existent input file "
            + "prints invalid path error")
    public void testInvalidInputPath() {
        Http cmd = new Http(outStream, errStream, false);
        new CommandLine(cmd).parseArgs(
            "--input", "/non/existent/file.yaml");
        cmd.setExitWhenFinish(false);
        cmd.execute();
        String err = getErr();
        Assert.assertTrue(
            err.contains("error:"),
            "Expected error message for invalid path. Err: "
                + err);
    }

    @Test(description = "Http with a .bal input file prints "
            + "invalid path error — .bal files are not valid "
            + "AsyncAPI specs")
    public void testBalFileRejectedAsInput() {
        Http cmd = new Http(outStream, errStream, false);
        new CommandLine(cmd).parseArgs(
            "--input", "some/service.bal");
        cmd.setExitWhenFinish(false);
        cmd.execute();
        String err = getErr();
        Assert.assertTrue(
            err.contains("error:"),
            "Expected error for .bal input on http. Err: "
                + err);
    }

    @Test(description = "Http getName() returns 'http'")
    public void testGetName() {
        Http cmd = new Http(outStream, errStream, false);
        Assert.assertEquals(cmd.getName(), "http");
    }

    @Test(description = "--with-tests flag on http prints "
            + "unsupported warning but does not error")
    public void testWithTestsWarning() {
        Http cmd = new Http(outStream, errStream, false);
        new CommandLine(cmd).parseArgs(
            "--input", specsDir.resolve(
                "spec-complete-slack.yml").toString(),
            "--with-tests");
        // We only check the warning is emitted — generation
        // may succeed or fail depending on environment.
        // The warning must always appear.
        // prevents JVM exit if generation fails in CI
        cmd.setExitWhenFinish(false);
        cmd.execute();
        Assert.assertTrue(
            getOut().contains("--with-tests is not yet supported"),
            "Expected with-tests warning. Out: " + getOut());
    }

    @Test(description = "printUsage writes 'bal asyncapi http' "
            + "to infoStream")
    public void testPrintUsage() {
        Http cmd = new Http(outStream, errStream, false);
        cmd.printUsage(new StringBuilder());
        Assert.assertTrue(
            getOut().contains("bal asyncapi http"),
            "Expected usage line in outStream. Out: " + getOut());
    }

    @Test(description = "Http with --output flag and valid .yaml "
            + "input does not print a validation error")
    public void testOutputFlagOnHttp() throws IOException {
        Path yamlFile = Files.createTempFile(
            tmpDir, "spec-", ".yaml");
        try {
            Http cmd = new Http(outStream, errStream, false);
            new CommandLine(cmd).parseArgs(
                "--input", yamlFile.toString(),
                "--output", "some/output");
            cmd.setExitWhenFinish(false);
            cmd.execute();
            Assert.assertFalse(
                getErr().contains(ErrorMessages.MISSING_INPUT_FLAG),
                "Expected no missing input error. Err: "
                    + getErr());
            Assert.assertFalse(
                getErr().contains(ErrorMessages.INVALID_INPUT_PATH),
                "Expected no invalid path error. Err: "
                    + getErr());
        } finally {
            Files.deleteIfExists(yamlFile);
        }
    }

    @Test(description = "Http with --license flag and valid .yaml "
            + "input does not print a validation error")
    public void testLicenseFlagOnHttp() throws IOException {
        Path yamlFile = Files.createTempFile(
            tmpDir, "spec-", ".yaml");
        Path licenseFile = Files.createTempFile(
            tmpDir, "license-", ".txt");
        try {
            Files.writeString(licenseFile, "// license header");
            Http cmd = new Http(outStream, errStream, false);
            new CommandLine(cmd).parseArgs(
                "--input", yamlFile.toString(),
                "--license", licenseFile.toString());
            cmd.setExitWhenFinish(false);
            cmd.execute();
            Assert.assertFalse(
                getErr().contains(ErrorMessages.MISSING_INPUT_FLAG),
                "Expected no missing input error. Err: "
                    + getErr());
        } finally {
            Files.deleteIfExists(yamlFile);
            Files.deleteIfExists(licenseFile);
        }
    }

    @Test(description = "readLicenseFile returns file content "
            + "when the file exists and is readable")
    public void testReadLicenseFileValidPath() throws IOException {
        Path licenseFile = Files.createTempFile(
            tmpDir, "license-", ".txt");
        try {
            Files.writeString(licenseFile, "// license header");
            Http cmd = new Http(outStream, errStream, false);
            String result = cmd.readLicenseFile(
                licenseFile.toString());
            Assert.assertEquals(result, "// license header");
        } finally {
            Files.deleteIfExists(licenseFile);
        }
    }

    @Test(description = "readLicenseFile returns empty string and "
            + "prints warning when the file does not exist")
    public void testReadLicenseFileInvalidPath() {
        Http cmd = new Http(outStream, errStream, false);
        String result = cmd.readLicenseFile(
            "/nonexistent/path/license.txt");
        Assert.assertEquals(result, "");
        Assert.assertTrue(
            getErr().contains("could not read license file"),
            "Expected warning in errStream. Err: " + getErr());
    }

    @Test(description = "Http with --service triggers mode guard "
            + "and prints does-not-support error")
    void testBallerinaToAsyncApiModeThrows() throws IOException {
        Path balFile = Files.createTempFile(
            tmpDir, "service-", ".bal");
        Http cmd = new Http(outStream, errStream, false);
        new CommandLine(cmd).parseArgs(
            "--input", balFile.toString(),
            "--service", "MyService");
        cmd.setExitWhenFinish(false);
        cmd.execute();
        Assert.assertTrue(
            getErr().contains(
                "does not support Ballerina-to-AsyncAPI"),
            "Expected mode-guard error. Err: " + getErr());
    }

}

