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

import io.ballerina.cli.BLauncherCmd;
import picocli.CommandLine;

import java.io.PrintStream;

/**
 * Root CLI command for the Ballerina AsyncAPI tool.
 * Registers {@link Http} and {@link Ws} as subcommands.
 */
@CommandLine.Command(
        name = "asyncapi",
        description = "Generate Ballerina code from AsyncAPI "
                + "specifications or generate AsyncAPI specs "
                + "from Ballerina services.",
        subcommands = {Http.class, Ws.class}
)
public class AsyncApiCmd implements BLauncherCmd {

    @CommandLine.Mixin
    private BaseCmd baseCmd;

    private final PrintStream outStream;
    private final PrintStream errStream;
    private final boolean exitWhenFinish;

    /**
     * Production constructor.
     */
    public AsyncApiCmd() {
        this.outStream = System.out;
        this.errStream = System.err;
        this.exitWhenFinish = true;
    }

    /**
     * Test constructor.
     *
     * @param outStream stream for standard output
     * @param errStream stream for error output
     */
    public AsyncApiCmd(PrintStream outStream,
                       PrintStream errStream) {
        this.outStream = outStream;
        this.errStream = errStream;
        this.exitWhenFinish = false;
    }

    /**
     * Test constructor.
     *
     * @param outStream      stream for standard output
     * @param exitWhenFinish {@code false} to suppress {@link Runtime#exit} calls during tests
     */
    public AsyncApiCmd(PrintStream outStream, boolean exitWhenFinish) {
        this.outStream = outStream;
        this.errStream = System.err;
        this.exitWhenFinish = exitWhenFinish;
    }

    @Override
    public String getName() {
        return CmdConstants.ASYNCAPI_CMD;
    }

    @Override
    public void printLongDesc(StringBuilder out) {
        String help = BLauncherCmd.getCommandUsageInfo(
                CmdConstants.ASYNCAPI_CMD,
                AsyncApiCmd.class.getClassLoader());
        outStream.println(help);
    }

    @Override
    public void printUsage(StringBuilder out) {
        outStream.println(
                "Usage: bal asyncapi [http|ws] [flags]");
    }

    @Override
    public void setParentCmdParser(
            picocli.CommandLine parentCmdParser) {
    }

    @Override
    public void execute() {
        if (baseCmd.helpFlag) {
            printLongDesc(null);
            return;
        }
        errStream.println(
                "bal asyncapi: missing subcommand. "
                + "Use 'bal asyncapi --help' for usage, or "
                + "'bal asyncapi http --help' / "
                + "'bal asyncapi ws --help' "
                + "for subcommand details.");
        exitError();
    }

    private void exitError() {
        if (exitWhenFinish) {
            Runtime.getRuntime().exit(1);
        }
    }
}
