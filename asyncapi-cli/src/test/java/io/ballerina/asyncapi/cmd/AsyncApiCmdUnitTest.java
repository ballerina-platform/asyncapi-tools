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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Unit tests for the {@link AsyncApiCmd} command shape: positional input, the {@code --protocol} flag,
 * protocol lookup and the shared context.
 */
public class AsyncApiCmdUnitTest extends CmdTestBase {

    @Test(description = "--help prints usage and does not error")
    public void testHelpFlagPrintsUsage() {
        AsyncApiCmd cmd = parse("--help");
        cmd.execute();
        Assert.assertTrue(getErr().isEmpty(), "No error expected for --help. Err: " + getErr());
        Assert.assertFalse(getOut().isBlank(), "Expected help text. Out: " + getOut());
    }

    @Test(description = "No input prints the missing-input error")
    public void testMissingInput() {
        parse("--protocol", "http").execute();
        Assert.assertTrue(getErr().contains(ErrorMessages.MISSING_INPUT),
                "Expected missing input error. Err: " + getErr());
    }

    @Test(description = "No --protocol prints the missing-protocol error listing supported protocols")
    public void testMissingProtocol() {
        parse("spec.yaml").execute();
        Assert.assertTrue(getErr().contains("--protocol flag is required"),
                "Expected missing protocol error. Err: " + getErr());
        Assert.assertTrue(getErr().contains("http, ws"), "Expected supported protocols. Err: " + getErr());
    }

    @Test(description = "An unknown protocol prints an error naming it and the supported ones")
    public void testUnknownProtocol() {
        parse("spec.yaml", "--protocol", "kafka").execute();
        Assert.assertTrue(getErr().contains("unknown protocol 'kafka'"),
                "Expected unknown protocol error. Err: " + getErr());
        Assert.assertTrue(getErr().contains("http, ws"), "Expected supported protocols. Err: " + getErr());
    }

    @Test(description = "The protocol name is matched case-insensitively")
    public void testProtocolCaseInsensitive() {
        parse("/non/existent/spec.yaml", "--protocol", "HTTP").execute();
        Assert.assertFalse(getErr().contains("unknown protocol"),
                "HTTP should be accepted. Err: " + getErr());
        Assert.assertTrue(getErr().contains("/non/existent/spec.yaml"),
                "Expected the input path error. Err: " + getErr());
    }

    @Test(description = "An unknown --format prints an error listing the supported formats")
    public void testUnknownFormat() {
        parse("service.bal", "--protocol", "ws", "--format", "xml").execute();
        Assert.assertTrue(getErr().contains("unknown format 'xml'"),
                "Expected unknown format error. Err: " + getErr());
        Assert.assertTrue(getErr().contains("json, yaml"), "Expected supported formats. Err: " + getErr());
    }

    @Test(description = "--format accepts json and yaml in any case")
    public void testKnownFormatsAccepted() {
        for (String format : new String[]{"json", "yaml", "JSON", "Yaml"}) {
            errContent.reset();
            parse("/non/existent/service.bal", "--protocol", "ws", "--format", format).execute();
            Assert.assertFalse(getErr().contains("unknown format"),
                    format + " should be accepted. Err: " + getErr());
        }
    }

    @Test(description = "--format selects JSON output only for json, yaml and no flag give YAML")
    public void testFormatMapsToJsonOption() {
        BaseCmd base = new BaseCmd();
        Assert.assertFalse(CmdUtils.collectCmdOptions(base).isJson(), "no --format should mean yaml");
        base.format = "yaml";
        Assert.assertFalse(CmdUtils.collectCmdOptions(base).isJson());
        base.format = "JSON";
        Assert.assertTrue(CmdUtils.collectCmdOptions(base).isJson());
    }

    @Test(description = "The --json flag no longer exists")
    public void testJsonFlagRemoved() {
        Assert.expectThrows(CommandLine.UnmatchedArgumentException.class,
                () -> parse("service.bal", "--protocol", "ws", "--json"));
    }

    @Test(description = "The input is a positional argument")
    public void testPositionalInput() {
        parse("/non/existent/spec.yaml", "--protocol", "ws").execute();
        Assert.assertTrue(getErr().contains("/non/existent/spec.yaml"),
                "Expected the positional input in the error. Err: " + getErr());
    }

    @Test(description = "The -i/--input flag no longer exists")
    public void testInputFlagRemoved() {
        Assert.expectThrows(CommandLine.UnmatchedArgumentException.class,
                () -> parse("-i", "spec.yaml", "--protocol", "http"));
        Assert.expectThrows(CommandLine.UnmatchedArgumentException.class,
                () -> parse("--input", "spec.yaml", "--protocol", "http"));
    }

    @Test(description = "The command has no subcommands")
    public void testNoSubcommands() {
        Assert.assertTrue(new CommandLine(new AsyncApiCmd(outStream, errStream)).getSubcommands().isEmpty(),
                "http/ws must not be subcommands any more");
    }

    @Test(description = "getName() returns 'asyncapi'")
    public void testGetName() {
        Assert.assertEquals(new AsyncApiCmd(outStream, errStream).getName(), "asyncapi");
    }

    @Test(description = "printUsage shows the positional input and the supported protocols")
    public void testPrintUsage() {
        new AsyncApiCmd(outStream, errStream).printUsage(new StringBuilder());
        Assert.assertTrue(getOut().contains("bal asyncapi <input> --protocol <http|ws>"),
                "Expected usage line. Out: " + getOut());
    }

    @Test(description = "printLongDesc writes non-blank output")
    public void testPrintLongDesc() {
        new AsyncApiCmd(outStream, errStream).printLongDesc(new StringBuilder());
        Assert.assertFalse(getOut().isBlank(), "Expected help text. Out: " + getOut());
    }

    @Test(description = "setParentCmdParser is a no-op")
    public void testSetParentCmdParser() {
        AsyncApiCmd cmd = new AsyncApiCmd(outStream, errStream);
        cmd.setParentCmdParser(new CommandLine(cmd));
        Assert.assertTrue(getErr().isEmpty(), "Expected no error output. Err: " + getErr());
    }

    @Test(description = "The registry finds registered protocols and nothing else")
    public void testProtocolRegistry() {
        Assert.assertTrue(ProtocolCommands.find("http").isPresent());
        Assert.assertTrue(ProtocolCommands.find(" WS ").isPresent());
        Assert.assertTrue(ProtocolCommands.find("kafka").isEmpty());
        Assert.assertTrue(ProtocolCommands.find(null).isEmpty());
        Assert.assertTrue(ProtocolCommands.find(" ").isEmpty());
        Assert.assertEquals(ProtocolCommands.names("|"), "http|ws");
    }

    @Test(description = "readLicenseFile returns the file content when readable")
    public void testReadLicenseFileValidPath() throws IOException {
        Path licenseFile = Files.createTempFile(tmpDir, "license-", ".txt");
        try {
            Files.writeString(licenseFile, "// license header");
            CmdContext ctx = new CmdContext(outStream, errStream, false);
            Assert.assertEquals(ctx.readLicenseFile(licenseFile.toString()), "// license header");
        } finally {
            Files.deleteIfExists(licenseFile);
        }
    }

    @Test(description = "readLicenseFile returns an empty string and warns when the file is missing")
    public void testReadLicenseFileInvalidPath() {
        CmdContext ctx = new CmdContext(outStream, errStream, false);
        Assert.assertEquals(ctx.readLicenseFile("/nonexistent/path/license.txt"), "");
        Assert.assertTrue(getErr().contains("could not read license file"),
                "Expected warning in errStream. Err: " + getErr());
    }
}
