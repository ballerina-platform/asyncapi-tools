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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The {@code bal asyncapi} command. The protocol is chosen with {@code --protocol}; what each protocol
 * supports comes from its generator {@link ProtocolHandler}.
 *
 * <p>The {@code --help} text is generated from the annotations here and on {@link BaseCmd}, including the usage
 * line, so there is no separate help text to keep in step. Anything that needs prose beyond a flag's own
 * description belongs in the documentation, not here.
 */
@CommandLine.Command(
        name = "asyncapi",
        usageHelpWidth = 100,
        sortOptions = false,
        headerHeading = "NAME%n",
        header = AsyncApiCmd.HEADER,
        synopsisHeading = "%nSYNOPSIS%n",
        descriptionHeading = "%nDESCRIPTION%n",
        description = AsyncApiCmd.DESCRIPTION,
        parameterListHeading = "%nARGUMENTS%n",
        optionListHeading = "%nOPTIONS%n"
)
public class AsyncApiCmd implements BLauncherCmd {

    // Not private: the @Command annotation on the class can only reference non-private constants.
    static final String HEADER =
            "  bal asyncapi - Generate Ballerina code from AsyncAPI definitions, or the reverse";
    static final String DESCRIPTION =
            "Generate Ballerina listeners and clients from AsyncAPI definitions, or generate AsyncAPI "
                    + "definitions from Ballerina code.";
    private static final String COMMAND_NAME = "bal asyncapi";
    private static final int INDENT = 2;

    @CommandLine.Mixin
    private BaseCmd baseCmd;

    private final CmdContext ctx;
    private String resolvedModule;

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
        // ANSI is off because ANSI codes print as raw characters when the help goes to a file, pipe or test.
        helpCommandLine().usage(ctx.infoStream(), CommandLine.Help.Ansi.OFF);
    }

    @Override
    public void printUsage(StringBuilder out) {
        CommandLine commandLine = helpCommandLine();
        ctx.printInfo(new CommandLine.Help(commandLine.getCommandSpec(), CommandLine.Help.Ansi.OFF)
                .synopsis(0).stripTrailing());
    }

    /**
     * A separate parser model used only for printing: it shows the command as {@code bal asyncapi} and shows a
     * flag's value after a space, which does not change how the real command line is parsed.
     */
    private CommandLine helpCommandLine() {
        CommandLine commandLine = new CommandLine(this);
        commandLine.setCommandName(COMMAND_NAME);
        commandLine.getCommandSpec().parser().separator(" ");
        commandLine.getHelpSectionMap().put(CommandLine.Model.UsageMessageSpec.SECTION_KEY_SYNOPSIS,
                help -> help.synopsis(0).indent(INDENT));
        commandLine.getHelpSectionMap().put(CommandLine.Model.UsageMessageSpec.SECTION_KEY_DESCRIPTION,
                help -> help.description().indent(INDENT));
        return commandLine;
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
        if (rejectRemovedSyntax()) {
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
        if (baseCmd.format != null && !CmdConstants.FORMAT_JSON.equalsIgnoreCase(baseCmd.format)
                && !CmdConstants.FORMAT_YAML.equalsIgnoreCase(baseCmd.format)) {
            fail(String.format(ErrorMessages.UNKNOWN_FORMAT, baseCmd.format));
            return;
        }
        ProtocolHandler handler;
        try {
            handler = CodeGenOrchestrator.getHandler(command.get().protocol());
        } catch (GeneratorException e) {
            fail("error: " + e.getMessage());
            return;
        }
        CmdConstants.Mode mode = CmdUtils.resolveMode(baseCmd);
        if (!validate(mode, handler, command.get())) {
            return;
        }
        warnUnsupportedOptions(handler, mode);
        String module = mode == CmdConstants.Mode.BALLERINA_TO_ASYNCAPI ? resolvedModule : baseCmd.module;
        try {
            command.get().generate(CmdUtils.collectCmdOptions(baseCmd, module), mode, ctx);
        } catch (RuntimeException e) {
            fail(ErrorMessages.UNEXPECTED_GENERATION_FAILURE + e.getMessage());
        }
    }

    /**
     * Fails with the replacement when the command uses syntax of the released 0.12.0 tool that the revamp
     * removed: {@code -i/--input}, {@code --json}, and the {@code wss}/{@code websocket} protocol names.
     */
    private boolean rejectRemovedSyntax() {
        List<String> errors = new ArrayList<>();
        boolean oldWsName = isOldWebSocketName(baseCmd.protocol);
        if (baseCmd.removedInputPath != null) {
            String protocol = oldWsName ? "ws"
                    : baseCmd.protocol == null ? "<" + ProtocolCommands.names("|") + ">" : baseCmd.protocol;
            String suggestion = "bal asyncapi " + baseCmd.removedInputPath + " --protocol " + protocol
                    + (baseCmd.removedJsonFlag ? " --format json" : "");
            errors.add(String.format(ErrorMessages.REMOVED_INPUT_FLAG, suggestion));
        }
        if (baseCmd.removedJsonFlag) {
            errors.add(ErrorMessages.REMOVED_JSON_FLAG);
        }
        if (oldWsName) {
            errors.add(String.format(ErrorMessages.RENAMED_PROTOCOL, baseCmd.protocol));
        }
        if (errors.isEmpty()) {
            return false;
        }
        fail(String.join(System.lineSeparator(), errors));
        return true;
    }

    private static boolean isOldWebSocketName(String protocol) {
        return "wss".equalsIgnoreCase(protocol) || "websocket".equalsIgnoreCase(protocol);
    }

    private boolean validate(CmdConstants.Mode mode, ProtocolHandler handler, ProtocolCommand command) {
        if (mode == CmdConstants.Mode.BALLERINA_TO_ASYNCAPI) {
            if (!handler.supportsCodeToSpec()) {
                fail(new AsyncApiCmdToolException(AsyncApiCmdToolDiagnostic.ASYNC_CLI_002,
                        command.protocol().name().toLowerCase()).getMessage());
                return false;
            }
            if (!CmdUtils.isValidBallerinaInput(baseCmd.inputPath)) {
                fail(String.format(ErrorMessages.INVALID_BAL_INPUT_PATH, baseCmd.inputPath));
                return false;
            }
            return resolveCodeInput();
        } else if (!CmdUtils.isValidInputFile(baseCmd.inputPath)) {
            fail(String.format(ErrorMessages.INVALID_INPUT_PATH, baseCmd.inputPath));
            return false;
        }
        return true;
    }

    /**
     * Applies the input rules for Ballerina code: a package or a standalone file, never a file inside a
     * package. {@code --module} picks a module of a package and is ignored for a standalone file.
     */
    private boolean resolveCodeInput() {
        try {
            Path input = Path.of(baseCmd.inputPath);
            if (AsyncApiProjectUtils.resolveCodeInput(input) == AsyncApiProjectUtils.CodeInputKind.PACKAGE) {
                resolvedModule = AsyncApiProjectUtils.resolveModule(input.toAbsolutePath().normalize(),
                        baseCmd.module);
            } else if (baseCmd.module != null) {
                ctx.printWarning(ErrorMessages.WARN_MODULE_STANDALONE_FILE);
            }
            return true;
        } catch (AsyncApiCmdToolException e) {
            fail(e.getMessage());
            return false;
        }
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
            if (baseCmd.format != null) {
                ctx.printWarning(String.format(ErrorMessages.WARN_OPTION_ONLY_FOR_SPEC_OUTPUT, "--format"));
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
