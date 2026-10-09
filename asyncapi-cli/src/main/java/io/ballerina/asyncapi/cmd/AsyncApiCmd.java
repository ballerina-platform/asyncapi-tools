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

import io.ballerina.asyncapi.generator.CodeGenOrchestrator;
import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.spi.ProtocolHandler;
import io.ballerina.cli.BLauncherCmd;
import picocli.CommandLine;

import java.io.PrintStream;
import java.util.Optional;

/**
 * The {@code bal asyncapi} command. The protocol is chosen with {@code --protocol}; what each protocol
 * supports comes from its generator {@link ProtocolHandler}.
 */
@CommandLine.Command(
        name = "asyncapi",
        description = "Generate Ballerina code from AsyncAPI "
                + "specifications or generate AsyncAPI specs "
                + "from Ballerina services."
)
public class AsyncApiCmd implements BLauncherCmd {

    @CommandLine.Mixin
    private BaseCmd baseCmd;

    private final CmdContext ctx;

    /**
     * Production constructor.
     */
    public AsyncApiCmd() {
        this.ctx = new CmdContext(System.out, System.err, true);
    }

    /**
     * Test constructor.
     *
     * @param outStream stream for standard output
     * @param errStream stream for error output
     */
    public AsyncApiCmd(PrintStream outStream,
                       PrintStream errStream) {
        this.ctx = new CmdContext(outStream, errStream, false);
    }

    /**
     * Test constructor.
     *
     * @param outStream      stream for standard output
     * @param exitWhenFinish {@code false} to suppress {@link Runtime#exit} calls during tests
     */
    public AsyncApiCmd(PrintStream outStream, boolean exitWhenFinish) {
        this.ctx = new CmdContext(outStream, System.err, exitWhenFinish);
    }

    /**
     * Test constructor.
     *
     * @param outStream      stream for standard output
     * @param errStream      stream for error output
     * @param exitWhenFinish {@code false} to suppress {@link Runtime#exit} calls during tests
     */
    public AsyncApiCmd(PrintStream outStream, PrintStream errStream, boolean exitWhenFinish) {
        this.ctx = new CmdContext(outStream, errStream, exitWhenFinish);
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
        ctx.printInfo(help);
    }

    @Override
    public void printUsage(StringBuilder out) {
        ctx.printInfo("Usage: bal asyncapi <input> --protocol <" + ProtocolCommands.names("|") + "> [flags]");
    }

    @Override
    public void setParentCmdParser(CommandLine parentCmdParser) {
    }

    void setExitWhenFinish(boolean exitWhenFinish) {
        ctx.setExitWhenFinish(exitWhenFinish);
    }

    @Override
    public void execute() {
        if (baseCmd.helpFlag) {
            printLongDesc(null);
            return;
        }
        if (baseCmd.inputPath == null || baseCmd.inputPath.isBlank()) {
            fail(ErrorMessages.MISSING_INPUT);
            return;
        }
        Optional<ProtocolCommand> command = ProtocolCommands.find(baseCmd.protocol);
        if (command.isEmpty()) {
            fail(baseCmd.protocol == null || baseCmd.protocol.isBlank()
                    ? String.format(ErrorMessages.MISSING_PROTOCOL, ProtocolCommands.names(", "))
                    : String.format(ErrorMessages.UNKNOWN_PROTOCOL, baseCmd.protocol,
                            ProtocolCommands.names(", ")));
            return;
        }
        ProtocolHandler handler;
        try {
            handler = CodeGenOrchestrator.getHandler(command.get().protocol());
        } catch (GeneratorException e) {
            fail("ERROR: " + e.getMessage());
            return;
        }
        CmdConstants.Mode mode = CmdUtils.resolveMode(baseCmd);
        if (!validate(mode, handler, command.get())) {
            return;
        }
        warnUnsupportedOptions(handler, mode);
        try {
            command.get().generate(CmdUtils.collectCmdOptions(baseCmd), mode, ctx);
        } catch (RuntimeException e) {
            fail(ErrorMessages.UNEXPECTED_GENERATION_FAILURE + e.getMessage());
        }
    }

    private boolean validate(CmdConstants.Mode mode, ProtocolHandler handler, ProtocolCommand command) {
        if (mode == CmdConstants.Mode.BALLERINA_TO_ASYNCAPI) {
            if (!handler.supportsCodeToSpec()) {
                fail(new AsyncApiCmdToolException(AsyncApiCmdToolDiagnostic.ASYNC_CLI_002,
                        command.protocol().name().toLowerCase()).getMessage());
                return false;
            }
            if (!CmdUtils.isValidBallerinaFile(baseCmd.inputPath)) {
                fail(String.format(ErrorMessages.INVALID_BAL_INPUT_PATH, baseCmd.inputPath));
                return false;
            }
        } else if (!CmdUtils.isValidInputFile(baseCmd.inputPath)) {
            fail(String.format(ErrorMessages.INVALID_INPUT_PATH, baseCmd.inputPath));
            return false;
        }
        return true;
    }

    private void warnUnsupportedOptions(ProtocolHandler handler, CmdConstants.Mode mode) {
        String protocol = handler.getProtocol().name().toLowerCase();
        if (baseCmd.withTests && !handler.supportsTestGeneration()) {
            ctx.printWarning(String.format(ErrorMessages.WARN_OPTION_UNSUPPORTED, "--with-tests", protocol));
        }
        if (baseCmd.licensePath != null && !handler.supportsLicenseHeader()) {
            ctx.printWarning(String.format(ErrorMessages.WARN_OPTION_UNSUPPORTED, "--license", protocol));
        }
        if (mode == CmdConstants.Mode.ASYNCAPI_TO_BALLERINA) {
            if (baseCmd.jsonFlag) {
                ctx.printWarning(String.format(ErrorMessages.WARN_OPTION_ONLY_FOR_SPEC_OUTPUT, "--json"));
            }
            if (baseCmd.serviceName != null) {
                ctx.printWarning(String.format(ErrorMessages.WARN_OPTION_ONLY_FOR_SPEC_OUTPUT, "--service"));
            }
        }
    }

    private void fail(String message) {
        ctx.printError(message);
        ctx.exitError();
    }
}
