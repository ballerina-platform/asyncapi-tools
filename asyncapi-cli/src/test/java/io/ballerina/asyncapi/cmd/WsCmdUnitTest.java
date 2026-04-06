package io.ballerina.asyncapi.cmd;

import org.testng.Assert;
import org.testng.annotations.Test;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Unit tests for {@link Ws} subcommand — validation logic,
 * mode resolution, and flag handling without real generation.
 */
public class WsCmdUnitTest extends CmdTestBase {

    @Test(description = "Ws with --help does not print error")
    public void testHelpFlag() {
        Ws cmd = new Ws(outStream, errStream, false);
        new CommandLine(cmd).parseArgs("--help");
        cmd.execute();
        Assert.assertTrue(getErr().isEmpty(),
            "No error expected for --help. Err: " + getErr());
    }

    @Test(description = "Ws with no --input prints missing "
            + "input error")
    public void testMissingInputFlag() {
        Ws cmd = new Ws(outStream, errStream, false);
        new CommandLine(cmd).parseArgs();
        cmd.setExitWhenFinish(false);
        cmd.execute();
        String err = getErr();
        Assert.assertTrue(
            err.contains(ErrorMessages.MISSING_INPUT_FLAG),
            "Expected missing input error. Err: " + err);
    }

    @Test(description = "Ws with non-existent input prints "
            + "invalid path error")
    public void testInvalidInputPath() {
        Ws cmd = new Ws(outStream, errStream, false);
        new CommandLine(cmd).parseArgs(
            "--input", "/non/existent/file.yaml");
        cmd.setExitWhenFinish(false);
        cmd.execute();
        String err = getErr();
        Assert.assertTrue(
            err.contains("error:"),
            "Expected error for invalid path. Err: " + err);
    }

    @Test(description = "Ws getName() returns 'ws'")
    public void testGetName() {
        Ws cmd = new Ws(outStream, errStream, false);
        Assert.assertEquals(cmd.getName(), "ws");
    }

    @Test(description = "Ws with --input .bal and --service "
            + "accepts the .bal file (BALLERINA_TO_ASYNCAPI)")
    void testBalFileAcceptedWithServiceFlag() throws IOException {
        Path balFile = Files.createTempFile(
            tmpDir, "service-", ".bal");
        Ws cmd = new Ws(outStream, errStream, false);
        new CommandLine(cmd).parseArgs(
            "--input", balFile.toString(),
            "--service", "ChatService");
        cmd.setExitWhenFinish(false);
        cmd.execute();
        String err = getErr();
        Assert.assertFalse(
            err.contains(ErrorMessages.MISSING_INPUT_FLAG),
            "Unexpected missing-input error. Err: " + err);
        Assert.assertFalse(
            err.contains(ErrorMessages.INVALID_BAL_INPUT_PATH),
            "Unexpected invalid-path error — .bal should be "
                + "accepted in B2A mode. Err: " + err);
    }

    @Test(description = "Ws with --input .bal but no --service "
            + "rejects the .bal file (ASYNCAPI_TO_BALLERINA)")
    void testBalFileRejectedWithoutServiceFlag()
            throws IOException {
        Path balFile = Files.createTempFile(
            tmpDir, "service-", ".bal");
        Ws cmd = new Ws(outStream, errStream, false);
        new CommandLine(cmd).parseArgs(
            "--input", balFile.toString());
        cmd.setExitWhenFinish(false);
        cmd.execute();
        Assert.assertTrue(
            getErr().contains("error:"),
            "Expected validation error for .bal without "
                + "--service. Err: " + getErr());
    }

    @Test(description = "Ws with --with-tests flag and valid .yaml "
            + "input does not print an error before generation")
    public void testWithTestsFlagOnWs() throws IOException {
        Path yamlFile = Files.createTempFile(
            tmpDir, "spec-", ".yaml");
        try {
            Ws cmd = new Ws(outStream, errStream, false);
            new CommandLine(cmd).parseArgs(
                "--input", yamlFile.toString(),
                "--with-tests");
            cmd.setExitWhenFinish(false);
            cmd.execute();
            Assert.assertFalse(
                getErr().contains(ErrorMessages.MISSING_INPUT_FLAG),
                "Expected no missing input error. Err: " + getErr());
            Assert.assertFalse(
                getErr().contains(ErrorMessages.INVALID_INPUT_PATH),
                "Expected no invalid path error. Err: " + getErr());
        } finally {
            Files.deleteIfExists(yamlFile);
        }
    }

    @Test(description = "Ws with --json flag and valid .bal input "
            + "does not print an error before generation")
    public void testJsonFlagOnWs() throws IOException {
        Path balFile = Files.createTempFile(
            tmpDir, "service-", ".bal");
        try {
            Ws cmd = new Ws(outStream, errStream, false);
            new CommandLine(cmd).parseArgs(
                "--input", balFile.toString(),
                "--service", "MyService",
                "--json");
            cmd.setExitWhenFinish(false);
            cmd.execute();
            Assert.assertFalse(
                getErr().contains(ErrorMessages.MISSING_INPUT_FLAG),
                "Expected no missing input error. Err: " + getErr());
            Assert.assertFalse(
                getErr().contains(ErrorMessages.INVALID_BAL_INPUT_PATH),
                "Expected no invalid path error. Err: " + getErr());
        } finally {
            Files.deleteIfExists(balFile);
        }
    }

    @Test(description = "Ws with --output flag and valid .yaml "
            + "input does not print an error before generation")
    public void testOutputFlagOnWs() throws IOException {
        Path yamlFile = Files.createTempFile(
            tmpDir, "spec-", ".yaml");
        try {
            Ws cmd = new Ws(outStream, errStream, false);
            new CommandLine(cmd).parseArgs(
                "--input", yamlFile.toString(),
                "--output", "some/output");
            cmd.setExitWhenFinish(false);
            cmd.execute();
            Assert.assertFalse(
                getErr().contains(ErrorMessages.MISSING_INPUT_FLAG),
                "Expected no missing input error. Err: " + getErr());
            Assert.assertFalse(
                getErr().contains(ErrorMessages.INVALID_INPUT_PATH),
                "Expected no invalid path error. Err: " + getErr());
        } finally {
            Files.deleteIfExists(yamlFile);
        }
    }

    @Test(description = "printUsage writes 'bal asyncapi ws' to "
            + "infoStream")
    public void testPrintUsage() {
        Ws cmd = new Ws(outStream, errStream, false);
        cmd.printUsage(new StringBuilder());
        Assert.assertTrue(
            getOut().contains("bal asyncapi ws"),
            "Expected usage line in outStream. Out: " + getOut());
    }

    @Test(description = "CmdUtils.resolveMode returns "
            + "ASYNCAPI_TO_BALLERINA when --service is absent")
    public void testResolveModeSpecToCode() {
        BaseCmd base = new BaseCmd();
        base.serviceName = null;
        Assert.assertEquals(
            CmdUtils.resolveMode(base),
            CmdConstants.Mode.ASYNCAPI_TO_BALLERINA,
            "Expected ASYNCAPI_TO_BALLERINA when no service");
    }

    @Test(description = "CmdUtils.resolveMode returns "
            + "BALLERINA_TO_ASYNCAPI when --service is set")
    public void testResolveModeCodeToSpec() {
        BaseCmd base = new BaseCmd();
        base.serviceName = "ChatService";
        Assert.assertEquals(
            CmdUtils.resolveMode(base),
            CmdConstants.Mode.BALLERINA_TO_ASYNCAPI,
            "Expected BALLERINA_TO_ASYNCAPI when service set");
    }
}

