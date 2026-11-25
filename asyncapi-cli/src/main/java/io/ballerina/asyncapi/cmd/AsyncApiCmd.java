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

import io.ballerina.asyncapi.cmd.AsyncApiCmdToolException.Kind;
import io.ballerina.asyncapi.cmd.AsyncApiProjectUtils.CodeInput;
import io.ballerina.asyncapi.cmd.AsyncApiProjectUtils.CodeInputKind;
import io.ballerina.asyncapi.generator.CodeGenOrchestrator;
import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.Protocol;
import io.ballerina.asyncapi.generator.spi.ProtocolHandler;
import io.ballerina.cli.BLauncherCmd;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The {@code bal asyncapi} command, following the Ballerina CLI Tool Design Guide. The input is the only positional
 * argument; whether it is a definition or Ballerina code decides the direction, and the definition decides the
 * protocol unless {@code --protocol} says otherwise. Every check runs before anything is written, and every failure
 * is one {@link AsyncApiCmdToolException} whose kind decides the exit code.
 */
@CommandLine.Command(name = "asyncapi", description = HelpText.DESCRIPTION)
public class AsyncApiCmd implements BLauncherCmd {

    private static final Pattern MODULE_NAME = Pattern.compile("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)*");
    private static final int MAX_MODULE_NAME_LENGTH = 256;
    private static final String STDIO_PATH = "-";

    @CommandLine.Mixin
    private BaseCmd baseCmd;

    private final CmdContext ctx;

    /**
     * Production constructor: text on a terminal, JSON otherwise.
     */
    public AsyncApiCmd() {
        this.ctx = CmdContext.forTerminal();
    }

    /**
     * Test constructor.
     *
     * @param outStream stream for standard output
     * @param errStream stream for error output
     */
    public AsyncApiCmd(PrintStream outStream, PrintStream errStream) {
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

    AsyncApiCmd(CmdContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String getName() {
        return CmdConstants.ASYNCAPI_CMD;
    }

    @Override
    public void printLongDesc(StringBuilder out) {
        ctx.printInfo(HelpText.render(new CommandLine(this).getCommandSpec()));
    }

    @Override
    public void printUsage(StringBuilder out) {
        ctx.printInfo(CmdContext.SYNOPSIS.substring("Usage: ".length()));
    }

    @Override
    public void setParentCmdParser(CommandLine parentCmdParser) {
    }

    void setExitWhenFinish(boolean exitWhenFinish) {
        ctx.setExitWhenFinish(exitWhenFinish);
    }

    /**
     * Returns the code the last run ended with.
     *
     * @return the exit code
     */
    int exitCode() {
        return ctx.exitCode();
    }

    @Override
    public void execute() {
        if (baseCmd.helpFlag) {
            printLongDesc(null);
            return;
        }
        if (baseCmd.versionFlag) {
            ctx.printInfo("asyncapi " + HelpText.toolVersion());
            return;
        }
        CmdConstants.Mode mode = CmdConstants.Mode.ASYNCAPI_TO_BALLERINA;
        try {
            applyOutputFormat();
            mode = CmdUtils.resolveMode(baseCmd);
            run(mode);
        } catch (AsyncApiCmdToolException e) {
            ctx.fail(e);
        } catch (GeneratorException e) {
            boolean fromDefinition = mode == CmdConstants.Mode.ASYNCAPI_TO_BALLERINA;
            ctx.fail(generatorError(e.getMessage(), fromDefinition ? Kind.INVALID_INPUT : Kind.RUNTIME));
        } catch (IOException e) {
            ctx.fail(AsyncApiCmdToolException.of(Kind.RUNTIME,
                    "failed to write the generated files: " + CmdUtils.oneLine(e.getMessage())));
        } catch (RuntimeException e) {
            ctx.fail(AsyncApiCmdToolException.of(Kind.RUNTIME,
                    ErrorMessages.UNEXPECTED_FAILURE + CmdUtils.oneLine(e.getMessage())));
        }
    }

    // The first line says what is wrong; the lines after it, such as the messages to fix and the YAML to add, stay
    // as they are, so a fix shown over several lines can still be read
    static AsyncApiCmdToolException generatorError(String message, Kind kind) {
        String text = message == null ? "" : message.strip();
        int newline = text.indexOf('\n');
        AsyncApiCmdToolException error = AsyncApiCmdToolException.of(kind,
                CmdUtils.oneLine(newline < 0 ? text : text.substring(0, newline)));
        return newline < 0 ? error : error.withSuggestion(text.substring(newline + 1).stripTrailing());
    }

    // OUT-3: the flag overrides the terminal check either way. An invalid value is reported as text.
    private void applyOutputFormat() {
        if (baseCmd.outputFormat == null) {
            return;
        }
        Optional<CmdContext.Format> format = CmdContext.Format.parse(baseCmd.outputFormat);
        if (format.isEmpty()) {
            ctx.setFormat(CmdContext.Format.TEXT);
            throw AsyncApiCmdToolException.of(Kind.USAGE,
                    "invalid value '" + baseCmd.outputFormat + "' for --output-format")
                    .withSuggestion("Use text or json.");
        }
        ctx.setFormat(format.get());
    }

    // The phases of FF-4 in order: arguments, target, input content, then generation, which plans and writes.
    private void run(CmdConstants.Mode mode) throws GeneratorException, IOException {
        checkArguments();
        ProtocolCommand command = mode == CmdConstants.Mode.BALLERINA_TO_ASYNCAPI
                ? codeToSpecProtocol() : specToCodeProtocol();
        ProtocolHandler handler = handler(command.protocol());
        String module = baseCmd.module;
        CodeInput codeInput = null;
        String license = "";
        if (mode == CmdConstants.Mode.BALLERINA_TO_ASYNCAPI) {
            codeInput = AsyncApiProjectUtils.resolveCodeInput(Path.of(baseCmd.inputPath));
            if (codeInput.kind() == CodeInputKind.STANDALONE_FILE && module != null) {
                throw AsyncApiCmdToolException.of(Kind.USAGE, "--module can't be used with a standalone .bal file")
                        .withSuggestion("Pass the package the module belongs to, or leave out --module.");
            }
            if (codeInput.kind() == CodeInputKind.PACKAGE) {
                module = AsyncApiProjectUtils.resolveModule(codeInput.packageRoot(), module);
            }
            AsyncApiProjectUtils.checkDependenciesLocked(codeInput, CmdUtils.quote(baseCmd.inputPath));
            AsyncApiProjectUtils.validateBalFile(codeInput, baseCmd.inputPath);
        } else {
            ctx.setSpecSource(Path.of(baseCmd.inputPath));
            if (baseCmd.licensePath != null) {
                license = GeneratedHeader.readLicense(baseCmd.licensePath);
            }
        }
        warnIgnoredOptions(handler, mode);
        command.generate(CmdUtils.collectCmdOptions(baseCmd, module), mode,
                new ProtocolCommand.Resolved(license, codeInput), ctx);
    }

    // FF-4 phase 1: everything that is wrong with the command line itself, before anything is read.
    private void checkArguments() {
        rejectRemovedSyntax();
        if (baseCmd.inputPath == null || baseCmd.inputPath.isBlank()) {
            throw AsyncApiCmdToolException.of(Kind.USAGE, "missing required argument <input>")
                    .withSuggestion("Pass an AsyncAPI definition, or a Ballerina package or .bal file.");
        }
        for (String path : new String[]{baseCmd.inputPath, baseCmd.outputPath, baseCmd.licensePath}) {
            if (STDIO_PATH.equals(path)) {
                throw AsyncApiCmdToolException.of(Kind.USAGE, "'-' is not accepted as a path")
                        .withSuggestion("Pass a file or directory; standard input and output are not supported.");
            }
        }
        if (baseCmd.format != null && baseCmd.specFormat != null
                && !baseCmd.format.equalsIgnoreCase(baseCmd.specFormat)) {
            throw AsyncApiCmdToolException.of(Kind.USAGE, "--format and --spec-format give different formats")
                    .withSuggestion("Use only --spec-format.");
        }
        String specFormat = CmdUtils.specFormat(baseCmd);
        if (specFormat != null && !CmdConstants.FORMAT_JSON.equalsIgnoreCase(specFormat)
                && !CmdConstants.FORMAT_YAML.equalsIgnoreCase(specFormat)) {
            throw AsyncApiCmdToolException.of(Kind.USAGE, "invalid value '" + specFormat + "' for "
                    + (baseCmd.specFormat != null ? "--spec-format" : "--format")).withSuggestion("Use yaml or json.");
        }
        if (baseCmd.specSide != null && !CmdConstants.SPEC_SIDE_CLIENT.equalsIgnoreCase(baseCmd.specSide)
                && !CmdConstants.SPEC_SIDE_SERVICE.equalsIgnoreCase(baseCmd.specSide)) {
            throw AsyncApiCmdToolException.of(Kind.USAGE, "invalid value '" + baseCmd.specSide + "' for --spec-side")
                    .withSuggestion("Use client or service.");
        }
        if (baseCmd.protocol != null && ProtocolCommands.find(baseCmd.protocol).isEmpty()) {
            throw AsyncApiCmdToolException.of(Kind.USAGE, "unknown protocol '" + baseCmd.protocol + "'")
                    .withSuggestion("Use one of: " + ProtocolCommands.names(", ") + ".");
        }
        if (baseCmd.module != null) {
            if (!MODULE_NAME.matcher(baseCmd.module).matches()) {
                throw new AsyncApiCmdToolException(AsyncApiCmdToolDiagnostic.ASYNC_CLI_006, baseCmd.module);
            }
            if (baseCmd.module.length() > MAX_MODULE_NAME_LENGTH) {
                throw new AsyncApiCmdToolException(AsyncApiCmdToolDiagnostic.ASYNC_CLI_007, baseCmd.module);
            }
        }
        if (baseCmd.format != null) {
            ctx.printWarning(ErrorMessages.WARN_FORMAT_RENAMED, "Use --spec-format instead.");
        }
    }

    /**
     * Fails with the replacement when the command uses syntax of the released 0.12.0 tool that the revamp
     * removed: {@code -i/--input}, {@code --json}, and the {@code wss}/{@code websocket} protocol names.
     */
    private void rejectRemovedSyntax() {
        boolean oldWsName = "wss".equalsIgnoreCase(baseCmd.protocol)
                || "websocket".equalsIgnoreCase(baseCmd.protocol);
        List<String> problems = new ArrayList<>();
        List<String> fixes = new ArrayList<>();
        if (baseCmd.removedInputPath != null) {
            problems.add("-i/--input has been removed");
            fixes.add("pass the input as the first argument");
        }
        if (baseCmd.removedJsonFlag) {
            problems.add("--json has been replaced by --spec-format json");
            fixes.add("use --spec-format json");
        }
        if (oldWsName) {
            problems.add("unknown protocol '" + baseCmd.protocol + "'");
            fixes.add("use --protocol ws for WebSocket");
        }
        if (problems.isEmpty()) {
            return;
        }
        BaseCmd fixed = copyOf(baseCmd);
        if (oldWsName) {
            fixed.protocol = "ws";
        }
        if (baseCmd.removedJsonFlag) {
            fixed.specFormat = CmdConstants.FORMAT_JSON;
        }
        String input = baseCmd.removedInputPath != null ? baseCmd.removedInputPath : baseCmd.inputPath;
        String suggestion = String.join(", and ", fixes) + ".";
        throw AsyncApiCmdToolException.of(Kind.USAGE, String.join("; ", problems))
                .withSuggestion(Character.toUpperCase(suggestion.charAt(0)) + suggestion.substring(1))
                .withCommand(input == null ? null : CmdUtils.commandLine(fixed, input));
    }

    private static BaseCmd copyOf(BaseCmd base) {
        BaseCmd copy = new BaseCmd();
        copy.protocol = base.protocol;
        copy.outputPath = base.outputPath;
        copy.module = base.module;
        copy.licensePath = base.licensePath;
        copy.specFormat = CmdUtils.specFormat(base);
        copy.serviceName = base.serviceName;
        copy.specSide = base.specSide;
        copy.withTests = base.withTests;
        copy.force = base.force;
        copy.dryRun = base.dryRun;
        copy.outputFormat = base.outputFormat;
        return copy;
    }

    // Generating from code: --protocol if given, otherwise the one protocol that can do it.
    private ProtocolCommand codeToSpecProtocol() throws GeneratorException {
        if (baseCmd.protocol != null) {
            ProtocolCommand command = ProtocolCommands.find(baseCmd.protocol).orElseThrow();
            if (!handler(command.protocol()).supportsCodeToSpec()) {
                throw new AsyncApiCmdToolException(AsyncApiCmdToolDiagnostic.ASYNC_CLI_002,
                        command.protocol().name().toLowerCase());
            }
            return command;
        }
        return ProtocolCommands.find(Protocol.WS.name()).orElseThrow();
    }

    // Generating from a definition: --protocol if given, otherwise what the definition says. Never a guess.
    private ProtocolCommand specToCodeProtocol() {
        Path spec = Path.of(baseCmd.inputPath);
        if (!CmdUtils.isAsyncApiSpecFile(baseCmd.inputPath)) {
            throw AsyncApiCmdToolException.of(Kind.USAGE,
                            "'" + baseCmd.inputPath + "' is not an AsyncAPI definition or Ballerina code")
                    .withSuggestion("Pass a .yaml, .yml or .json definition, or a Ballerina package or .bal file.");
        }
        if (!Files.isRegularFile(spec)) {
            throw AsyncApiCmdToolException.of(Kind.INVALID_INPUT, "'" + baseCmd.inputPath + "' does not exist")
                    .withSuggestion("Pass the path to an AsyncAPI definition.");
        }
        if (baseCmd.protocol != null) {
            return ProtocolCommands.find(baseCmd.protocol).orElseThrow();
        }
        Optional<Protocol> detected;
        Set<String> otherProtocols;
        try {
            detected = ProtocolDetector.detect(spec);
            otherProtocols = detected.isEmpty() ? ProtocolDetector.otherProtocols(spec) : Set.of();
        } catch (IOException | RuntimeException e) {
            throw AsyncApiCmdToolException.of(Kind.INVALID_INPUT,
                            "cannot read '" + baseCmd.inputPath + "': " + CmdUtils.oneLine(e.getMessage()))
                    .withSuggestion("Fix the definition so it is valid YAML or JSON.");
        }
        // Servers that all speak another protocol, such as mqtt, can't be generated for with either --protocol
        if (!otherProtocols.isEmpty()) {
            throw AsyncApiCmdToolException.of(Kind.INVALID_INPUT, "the servers of '" + baseCmd.inputPath
                            + "' use " + String.join(", ", otherProtocols) + ", which the tool doesn't generate for")
                    .withSuggestion("The tool generates webhook triggers (http) and WebSocket clients (ws or wss). "
                            + "If a server is a plain WebSocket endpoint, set its protocol to ws or wss.");
        }
        if (detected.isEmpty()) {
            throw AsyncApiCmdToolException.of(Kind.USAGE,
                            "cannot tell whether '" + baseCmd.inputPath + "' is for HTTP or WebSocket")
                    .withSuggestion("Pass --protocol http for a webhook trigger or --protocol ws for a WebSocket "
                            + "client.");
        }
        return ProtocolCommands.find(detected.get().name()).orElseThrow();
    }

    private static ProtocolHandler handler(Protocol protocol) {
        try {
            return CodeGenOrchestrator.getHandler(protocol);
        } catch (GeneratorException e) {
            throw AsyncApiCmdToolException.of(Kind.RUNTIME, CmdUtils.oneLine(e.getMessage()));
        }
    }

    private void warnIgnoredOptions(ProtocolHandler handler, CmdConstants.Mode mode) {
        String protocol = handler.getProtocol().name().toLowerCase();
        if (baseCmd.withTests && !handler.supportsTestGeneration()) {
            ctx.printWarning(String.format(ErrorMessages.WARN_OPTION_UNSUPPORTED, "--with-tests", protocol));
        }
        if (baseCmd.specSide != null && !handler.supportsSpecSide()) {
            ctx.printWarning(String.format(ErrorMessages.WARN_OPTION_UNSUPPORTED, "--spec-side", protocol));
        }
        if (mode == CmdConstants.Mode.BALLERINA_TO_ASYNCAPI) {
            for (String[] option : new String[][]{{"--license", baseCmd.licensePath},
                    {"--spec-side", handler.supportsSpecSide() ? baseCmd.specSide : null},
                    {"--with-tests", baseCmd.withTests && handler.supportsTestGeneration() ? "set" : null}}) {
                if (option[1] != null) {
                    ctx.printWarning(String.format(ErrorMessages.WARN_OPTION_ONLY_FOR_CODE_OUTPUT, option[0]));
                }
            }
        } else {
            if (CmdUtils.specFormat(baseCmd) != null) {
                ctx.printWarning(String.format(ErrorMessages.WARN_OPTION_ONLY_FOR_SPEC_OUTPUT,
                        baseCmd.specFormat != null ? "--spec-format" : "--format"));
            }
            if (baseCmd.serviceName != null) {
                ctx.printWarning(String.format(ErrorMessages.WARN_OPTION_ONLY_FOR_SPEC_OUTPUT, "--service"));
            }
        }
    }
}
