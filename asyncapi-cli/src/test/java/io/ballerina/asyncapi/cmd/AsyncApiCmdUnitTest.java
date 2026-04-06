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

/**
 * Unit tests for {@link AsyncApiCmd} root command behaviour —
 * help flag, missing subcommand, and picocli wiring.
 */
public class AsyncApiCmdUnitTest extends CmdTestBase {

    @Test(description = "Root command with --help prints usage "
            + "and does not error")
    public void testHelpFlagPrintsUsage() {
        AsyncApiCmd cmd = new AsyncApiCmd(outStream, errStream);
        new CommandLine(cmd).parseArgs("--help");
        cmd.execute();
        // Help output goes to outStream via printLongDesc
        // No error output expected
        Assert.assertTrue(getErr().isEmpty(),
            "No error expected for --help. Err: " + getErr());
    }

    @Test(description = "Root command with no subcommand prints "
            + "missing subcommand message to errStream")
    public void testNoSubcommandPrintsHint() {
        AsyncApiCmd cmd = new AsyncApiCmd(outStream, errStream);
        new CommandLine(cmd).parseArgs();
        cmd.execute();
        String err = getErr();
        Assert.assertTrue(err.contains("missing subcommand"),
            "Expected missing subcommand hint. Err: " + err);
    }

    @Test(description = "Root command getName() returns 'asyncapi'")
    public void testGetName() {
        AsyncApiCmd cmd = new AsyncApiCmd(outStream, errStream);
        Assert.assertEquals(cmd.getName(), "asyncapi");
    }

    @Test(description = "printUsage writes usage line containing "
            + "'asyncapi' to outStream")
    public void testPrintUsage() {
        AsyncApiCmd cmd = new AsyncApiCmd(outStream, errStream);
        cmd.printUsage(new StringBuilder());
        Assert.assertTrue(
            getOut().contains("asyncapi"),
            "Expected usage line in outStream. Out: " + getOut());
    }

    @Test(description = "printLongDesc writes non-blank output "
            + "to outStream")
    public void testPrintLongDesc() {
        AsyncApiCmd cmd = new AsyncApiCmd(outStream, errStream);
        cmd.printLongDesc(new StringBuilder());
        Assert.assertFalse(
            getOut().isBlank(),
            "Expected non-blank output from printLongDesc. "
                + "Out: " + getOut());
    }

    @Test(description = "setParentCmdParser is a no-op and "
            + "produces no error output")
    public void testSetParentCmdParser() {
        AsyncApiCmd cmd = new AsyncApiCmd(outStream, errStream);
        cmd.setParentCmdParser(new CommandLine(cmd));
        Assert.assertTrue(getErr().isEmpty(),
            "Expected no error output. Err: " + getErr());
    }

    @Test(description = "execute() with no subcommand prints "
            + "missing subcommand message and does not exit JVM")
    public void testNoSubcommandCallsExitError() {
        AsyncApiCmd cmd = new AsyncApiCmd(outStream, errStream);
        new CommandLine(cmd).parseArgs();
        cmd.execute();
        Assert.assertTrue(
            getErr().contains("missing subcommand"),
            "Expected missing subcommand hint. Err: " + getErr());
    }

    @Test(description = "Root command has Http and Ws registered "
            + "as subcommands")
    public void testSubcommandsRegistered() {
        AsyncApiCmd cmd = new AsyncApiCmd(outStream, errStream);
        CommandLine commandLine = new CommandLine(cmd);
        Assert.assertTrue(
            commandLine.getSubcommands().containsKey("http"),
            "Expected 'http' subcommand to be registered");
        Assert.assertTrue(
            commandLine.getSubcommands().containsKey("ws"),
            "Expected 'ws' subcommand to be registered");
    }
}

