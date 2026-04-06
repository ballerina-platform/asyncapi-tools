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
import java.nio.file.Path;

/**
 * Abstract base class for protocol-specific asyncapi sub-commands ({@link Http}, {@link Ws}).
 * Implements {@link BLauncherCmd} and provides a concrete execution lifecycle so that concrete
 * sub-commands only need to implement {@link #generate(CmdOptions, CmdConstants.Mode)}.
 *
 * Common option flags are provided by {@link BaseCmd} via {@code @Mixin} injection.
 * The sub-command identity (name, help-text lookup key) is determined by the {@link CmdConstants.CmdType}
 * supplied at construction time.
 *
 */
public abstract class SubCmdBase implements BLauncherCmd {

    private static final String INFO_USAGE_PATTERN =
            "bal asyncapi %s [flags]";
    private static final String WARNING_LICENSE_FILE_UNREADABLE =
            "warning: could not read license file: %s. Proceeding without license header.";
    private static final String ERROR_UNEXPECTED_GENERATION_FAILURE =
            "error: unexpected failure during generation: ";

    @CommandLine.Mixin
    protected BaseCmd baseCmd;

    private PrintStream infoStream = System.out;

    protected final CmdConstants.CmdType cmdType;
    protected PrintStream errStream = System.err;
    protected final Path executionPath;
    private boolean exitWhenFinish = true;

    /**
     * Constructs a {@code SubCmdBase} that writes to {@code System.out}/{@code System.err}
     * and resolves paths relative to the current working directory.
     *
     * @param cmdType identifies which protocol sub-command this instance represents
     */
    protected SubCmdBase(CmdConstants.CmdType cmdType) {
        this.cmdType = cmdType;
        this.executionPath = Path.of(System.getProperty("user.dir"));
    }

    /**
     * Test-only constructor — injects info and error streams and controls whether the JVM exits on error.
     *
     * @param cmdType        identifies which protocol sub-command this instance represents
     * @param infoStream     stream used for info output
     * @param errStream      stream used for error output
     * @param exitWhenFinish {@code false} to suppress {@link Runtime#exit} calls during tests
     */
    protected SubCmdBase(CmdConstants.CmdType cmdType, PrintStream infoStream,
                         PrintStream errStream, boolean exitWhenFinish) {
        this.cmdType = cmdType;
        this.infoStream = infoStream;
        this.errStream = errStream;
        this.exitWhenFinish = exitWhenFinish;
        this.executionPath = Path.of(System.getProperty("user.dir"));
    }

    /**
     * Returns the sub-command name registered with the Ballerina launcher.
     *
     * @return {@code cmdType.getName()} (e.g. {@code "http"} or {@code "ws"})
     */
    @Override
    public String getName() {
        return cmdType.getName();
    }

    /**
     * Prints the long-form help text for this sub-command, resolved from the classpath resource
     * identified by {@link CmdConstants#COMMAND_IDENTIFIER}.
     *
     * @param out ignored; output is written directly to {@code outStream}
     */
    @Override
    public void printLongDesc(StringBuilder out) {
        String identifier = String.format(CmdConstants.COMMAND_IDENTIFIER, cmdType.getName());
        String help = BLauncherCmd.getCommandUsageInfo(identifier, this.getClass().getClassLoader());
        infoStream.println(help);
    }

    /**
     * Prints a one-line usage summary for this sub-command.
     *
     * @param out ignored; output is written directly to {@code outStream}
     */
    @Override
    public void printUsage(StringBuilder out) {
        infoStream.println(String.format(INFO_USAGE_PATTERN, cmdType.getName()));
    }

    @Override
    public void execute() {
        if (baseCmd.helpFlag) {
            printLongDesc(null);
            return;
        }
        if (baseCmd.inputPath == null
                || baseCmd.inputPath.isBlank()) {
            errStream.println(ErrorMessages.MISSING_INPUT_FLAG);
            exitError();
            return;
        }
        CmdConstants.Mode mode = CmdUtils.resolveMode(baseCmd);
        if (mode == CmdConstants.Mode.ASYNCAPI_TO_BALLERINA) {
            if (!CmdUtils.isValidInputFile(baseCmd.inputPath)) {
                errStream.println(String.format(
                        ErrorMessages.INVALID_INPUT_PATH,
                        baseCmd.inputPath));
                exitError();
                return;
            }
        } else {
            if (!CmdUtils.isValidBallerinaFile(baseCmd.inputPath)) {
                errStream.println(String.format(
                        ErrorMessages.INVALID_BAL_INPUT_PATH,
                        baseCmd.inputPath));
                exitError();
                return;
            }
        }
        try {
            CmdOptions opts = CmdUtils.collectCmdOptions(baseCmd);
            generate(opts, mode);
        } catch (RuntimeException e) {
            errStream.println(ERROR_UNEXPECTED_GENERATION_FAILURE + e.getMessage());
            exitError();
        }
    }

    void setExitWhenFinish(boolean exitWhenFinish) {
        this.exitWhenFinish = exitWhenFinish;
    }

    protected void exitError() {
        if (exitWhenFinish) {
            Runtime.getRuntime().exit(1);
        }
    }

    @Override
    public void setParentCmdParser(CommandLine parentCmdParser) {
    }

    /**
     * Writes {@code message} to the shared info stream ({@code System.out}).
     * Subclasses must use this method instead of accessing {@code System.out} directly
     * so that all info-level output is routed through a single owner.
     *
     * @param message the message to print
     */
    protected void printInfo(String message) {
        infoStream.println(message);
    }

    /**
     * Reads and returns the content of the license file at {@code licensePath}.
     * If the file cannot be read, emits a warning to {@code errStream} and returns an empty string.
     *
     * @param licensePath path to the license header file
     * @return the file content, or {@code ""} if the file is unreadable
     */
    protected String readLicenseFile(String licensePath) {
        try {
            return java.nio.file.Files.readString(java.nio.file.Path.of(licensePath));
        } catch (java.io.IOException e) {
            errStream.println(String.format(WARNING_LICENSE_FILE_UNREADABLE, licensePath));
            return "";
        }
    }

    /**
     * Performs the protocol-specific code generation.
     * Called by {@link #execute()} after input validation and option collection.
     *
     * @param opts the resolved, validated generation options
     * @param mode the direction of generation (AsyncAPI→Ballerina or Ballerina→AsyncAPI)
     */
    protected abstract void generate(CmdOptions opts, CmdConstants.Mode mode);
}
