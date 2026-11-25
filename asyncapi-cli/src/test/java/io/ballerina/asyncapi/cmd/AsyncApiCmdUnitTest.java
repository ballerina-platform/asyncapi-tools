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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ballerina.asyncapi.generator.Protocol;
import org.testng.Assert;
import org.testng.annotations.Test;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for the {@link AsyncApiCmd} command shape against the Ballerina CLI Tool Design Guide: the help, the
 * version, the error format and exit codes, removed syntax, and how the protocol is chosen.
 */
public class AsyncApiCmdUnitTest extends CmdTestBase {

    private static final String NL = System.lineSeparator();

    private String help() {
        outContent.reset();
        new AsyncApiCmd(outStream, errStream).printLongDesc(new StringBuilder());
        return getOut();
    }

    @Test(description = "--help prints the help to stdout and nothing to stderr")
    public void testHelpFlagPrintsHelp() {
        AsyncApiCmd cmd = run("--help");
        Assert.assertTrue(getErr().isEmpty(), "Err: " + getErr());
        Assert.assertTrue(getOut().startsWith(HelpText.DESCRIPTION), "Out: " + getOut());
        Assert.assertEquals(cmd.exitCode(), ExitCodes.SUCCESS);
        outContent.reset();
        run("-h");
        Assert.assertTrue(getOut().startsWith(HelpText.DESCRIPTION), "Out: " + getOut());
    }

    @Test(description = "The help has the guide's sections in order: description, usage, options, examples (HLP-1)")
    public void testHelpSections() {
        String help = help().replace(NL, "\n");
        int usage = help.indexOf("\nUsage: bal asyncapi [OPTIONS] <input>\n");
        int options = help.indexOf("\nOptions:\n");
        int examples = help.indexOf("\nExamples:\n");
        Assert.assertTrue(help.startsWith(HelpText.DESCRIPTION + "\n"), "Out: " + help);
        Assert.assertFalse(HelpText.DESCRIPTION.endsWith("."), "No trailing period on the description");
        Assert.assertTrue(usage > 0 && options > usage && examples > options, "Out: " + help);
        long commented = help.lines().filter(line -> line.startsWith("  # ")).count();
        Assert.assertTrue(commented >= 2, "At least two examples, each with a comment. Out: " + help);
        Assert.assertFalse(help.toLowerCase().contains("exit code"), "Help does not list exit codes (HLP-3)");
    }

    @Test(description = "Every help line fits in 80 columns, and each option is one lowercase line (HLP-2)")
    public void testHelpLines() {
        for (String line : help().lines().toList()) {
            Assert.assertTrue(line.length() <= HelpText.MAX_WIDTH, "Too long: " + line);
        }
        CommandLine.Model.CommandSpec spec = new CommandLine(new AsyncApiCmd(outStream, errStream)).getCommandSpec();
        for (CommandLine.Model.OptionSpec option : spec.options()) {
            if (option.hidden()) {
                continue;
            }
            Assert.assertEquals(option.description().length, 1, option.longestName());
            String description = option.description()[0];
            Assert.assertTrue(Character.isLowerCase(description.charAt(0)), option.longestName());
            Assert.assertFalse(description.endsWith("."), option.longestName());
        }
    }

    @Test(description = "Every visible flag is in the help with its description, in alphabetical order")
    public void testEveryFlagIsInTheHelp() {
        String help = help();
        CommandLine.Model.CommandSpec spec = new CommandLine(new AsyncApiCmd(outStream, errStream)).getCommandSpec();
        List<String> names = new ArrayList<>();
        List<Integer> positions = new ArrayList<>();
        for (CommandLine.Model.OptionSpec option : spec.options()) {
            if (option.hidden()) {
                continue;
            }
            int at = help.indexOf(option.longestName() + (option.typeInfo().isBoolean() ? " " : " <"));
            Assert.assertTrue(at > 0, option.longestName() + " is missing from the help");
            Assert.assertTrue(help.contains(option.description()[0]), option.longestName() + " description");
            names.add(option.longestName().replaceFirst("^-+", ""));
            positions.add(at);
        }
        Assert.assertTrue(names.size() >= 13, "Expected the command's flags. Found " + names);
        List<String> byPosition = new ArrayList<>(names);
        byPosition.sort((a, b) -> positions.get(names.indexOf(a)) - positions.get(names.indexOf(b)));
        List<String> sorted = new ArrayList<>(names);
        sorted.sort(String::compareTo);
        Assert.assertEquals(byPosition, sorted, "Options are listed alphabetically");
    }

    @Test(description = "The protocols and formats in the flag labels match what the command accepts")
    public void testHelpListsRegisteredProtocolsAndFormats() {
        String help = help();
        Assert.assertTrue(help.contains("--protocol <" + ProtocolCommands.names("|") + ">"), help);
        Assert.assertTrue(help.contains("--spec-format <" + CmdConstants.FORMAT_YAML + "|" + CmdConstants.FORMAT_JSON
                + ">"), help);
    }

    @Test(description = "The removed and renamed flags stay out of the help")
    public void testRemovedFlagsAreNotInHelp() {
        String help = help();
        Assert.assertFalse(help.contains("--input"), help);
        Assert.assertFalse(help.contains("--json"), help);
        Assert.assertFalse(help.contains(" --format"), help);
    }

    @Test(description = "-V and --version print the version to stdout (INV-2)")
    public void testVersion() {
        AsyncApiCmd cmd = run("--version");
        Assert.assertTrue(getOut().startsWith("asyncapi "), "Out: " + getOut());
        Assert.assertTrue(getErr().isEmpty(), "Err: " + getErr());
        Assert.assertEquals(cmd.exitCode(), ExitCodes.SUCCESS);
        outContent.reset();
        run("-V");
        Assert.assertTrue(getOut().startsWith("asyncapi "), "Out: " + getOut());
    }

    @Test(description = "A missing input is a usage error: the error, the synopsis and the help pointer (ERR-8)")
    public void testMissingInput() {
        AsyncApiCmd cmd = run("--protocol", "http");
        Assert.assertEquals(cmd.exitCode(), ExitCodes.USAGE);
        Assert.assertTrue(getErr().startsWith("bal asyncapi: error: missing required argument <input>" + NL),
                "Err: " + getErr());
        Assert.assertTrue(getErr().contains(NL + "Usage: bal asyncapi [OPTIONS] <input>" + NL
                + "Try 'bal asyncapi --help' for more information." + NL), "Err: " + getErr());
        Assert.assertTrue(getOut().isEmpty(), "stdout is empty on failure. Out: " + getOut());
    }

    @Test(description = "An unknown protocol is a usage error listing the supported ones")
    public void testUnknownProtocol() {
        AsyncApiCmd cmd = run("spec.yaml", "--protocol", "kafka");
        Assert.assertEquals(cmd.exitCode(), ExitCodes.USAGE);
        Assert.assertTrue(getErr().contains("bal asyncapi: error: unknown protocol 'kafka'"), "Err: " + getErr());
        Assert.assertTrue(getErr().contains("  Use one of: http, ws."), "Err: " + getErr());
    }

    @Test(description = "The removed -i flag gives the replacement command to run")
    public void testRemovedInputFlagSuggestsNewSyntax() {
        AsyncApiCmd cmd = run("-i", "service.bal", "--protocol", "ws");
        Assert.assertEquals(cmd.exitCode(), ExitCodes.USAGE);
        Assert.assertTrue(getErr().contains("error: -i/--input has been removed"), "Err: " + getErr());
        Assert.assertTrue(getErr().contains("  Try: bal asyncapi service.bal --protocol ws" + NL), "Err: " + getErr());
    }

    @Test(description = "The removed --json flag points at --spec-format json, also inside the -i fix")
    public void testRemovedJsonFlagSuggestsSpecFormat() {
        run("-i", "service.bal", "--protocol", "ws", "--json");
        Assert.assertTrue(getErr().contains("--json has been replaced by --spec-format json"), "Err: " + getErr());
        Assert.assertTrue(getErr().contains("Try: bal asyncapi service.bal --protocol ws --spec-format json"),
                "Err: " + getErr());
    }

    @Test(description = "The old WebSocket protocol names point at ws")
    public void testOldWebSocketProtocolNames() {
        AsyncApiCmd cmd = run("service.bal", "--protocol", "websocket");
        Assert.assertEquals(cmd.exitCode(), ExitCodes.USAGE);
        Assert.assertTrue(getErr().contains("Use --protocol ws for WebSocket."), "Err: " + getErr());
        Assert.assertTrue(getErr().contains("Try: bal asyncapi service.bal --protocol ws"), "Err: " + getErr());
    }

    @Test(description = "The protocol name is matched case-insensitively")
    public void testProtocolCaseInsensitive() {
        AsyncApiCmd cmd = run("/non/existent/spec.yaml", "--protocol", "HTTP");
        Assert.assertFalse(getErr().contains("unknown protocol"), "Err: " + getErr());
        Assert.assertTrue(getErr().contains("'/non/existent/spec.yaml' does not exist"), "Err: " + getErr());
        Assert.assertEquals(cmd.exitCode(), ExitCodes.RUNTIME);
    }

    @Test(description = "Without --protocol, a definition that says neither HTTP nor WebSocket is not guessed")
    public void testProtocolNotGuessed() throws IOException {
        Path spec = Files.writeString(tmpDir.resolve("plain.yaml"),
                "asyncapi: 2.6.0\ninfo: {title: Plain, version: 1.0.0}\nservers:\n  prod: {url: example.com, "
                        + "protocol: https}\nchannels: {}\n");
        AsyncApiCmd cmd = run(spec.toString());
        Assert.assertEquals(cmd.exitCode(), ExitCodes.USAGE);
        Assert.assertTrue(getErr().contains("cannot tell whether"), "Err: " + getErr());
        Assert.assertTrue(getErr().contains("--protocol http") && getErr().contains("--protocol ws"),
                "Err: " + getErr());
    }

    @Test(description = "The protocol is read from the definition: ws/wss servers or the trigger extension")
    public void testProtocolDetected() throws Exception {
        Path ws = Files.writeString(tmpDir.resolve("ws.yaml"),
                "asyncapi: 3.0.0\ninfo: {title: Chat, version: 1.0.0}\nservers:\n  prod: {host: example.com, "
                        + "protocol: wss}\n");
        Assert.assertEquals(ProtocolDetector.detect(ws).orElseThrow(), Protocol.WS);
        Assert.assertEquals(ProtocolDetector.detect(resource("specs/tiny_http.yaml")).orElseThrow(), Protocol.HTTP);
    }

    @Test(description = "An unknown --spec-format is a usage error naming the accepted values")
    public void testUnknownSpecFormat() {
        AsyncApiCmd cmd = run("service.bal", "--spec-format", "xml");
        Assert.assertEquals(cmd.exitCode(), ExitCodes.USAGE);
        Assert.assertTrue(getErr().contains("invalid value 'xml' for --spec-format"), "Err: " + getErr());
        Assert.assertTrue(getErr().contains("Use yaml or json."), "Err: " + getErr());
    }

    @Test(description = "--format still works, with a warning that it is now --spec-format")
    public void testFormatIsRenamed() {
        run("/non/existent/service.bal", "--format", "JSON");
        Assert.assertTrue(getErr().contains("bal asyncapi: warning: --format is renamed to --spec-format"),
                "Err: " + getErr());
        Assert.assertFalse(getErr().contains("invalid value"), "Err: " + getErr());
    }

    @Test(description = "--spec-format selects JSON only for json; yaml and no flag give YAML")
    public void testSpecFormatMapsToJsonOption() {
        BaseCmd base = new BaseCmd();
        Assert.assertFalse(CmdUtils.collectCmdOptions(base).isJson(), "no flag should mean yaml");
        base.specFormat = "yaml";
        Assert.assertFalse(CmdUtils.collectCmdOptions(base).isJson());
        base.specFormat = "JSON";
        Assert.assertTrue(CmdUtils.collectCmdOptions(base).isJson());
        base.specFormat = null;
        base.format = "json";
        Assert.assertTrue(CmdUtils.collectCmdOptions(base).isJson(), "the old --format still counts");
    }

    @Test(description = "An input that is neither a definition nor Ballerina code is a usage error")
    public void testUnknownInputKind() throws IOException {
        Path text = Files.writeString(tmpDir.resolve("notes.txt"), "hello");
        AsyncApiCmd cmd = run(text.toString());
        Assert.assertEquals(cmd.exitCode(), ExitCodes.USAGE);
        Assert.assertTrue(getErr().contains("is not an AsyncAPI definition or Ballerina code"), "Err: " + getErr());
    }

    @Test(description = "'-' is not accepted as a path (INV-7)")
    public void testDashIsNotAPath() {
        Assert.assertEquals(run("-", "--protocol", "ws").exitCode(), ExitCodes.USAGE);
        Assert.assertTrue(getErr().contains("'-' is not accepted as a path"), "Err: " + getErr());
        errContent.reset();
        Assert.assertEquals(run("spec.yaml", "-o", "-").exitCode(), ExitCodes.USAGE);
    }

    @Test(description = "An invalid module name is a usage error (MOD-3)")
    public void testInvalidModuleName() {
        AsyncApiCmd cmd = run("spec.yaml", "--protocol", "ws", "--module", "my-module");
        Assert.assertEquals(cmd.exitCode(), ExitCodes.USAGE);
        Assert.assertTrue(getErr().contains("invalid module name 'my-module'"), "Err: " + getErr());
    }

    @Test(description = "--output-format json prints the error as one JSON object on stderr (OUT-1, OUT-8)")
    public void testJsonError() throws IOException {
        AsyncApiCmd cmd = run("spec.yaml", "--protocol", "kafka", "--output-format", "json");
        Assert.assertEquals(cmd.exitCode(), ExitCodes.USAGE);
        List<String> lines = getErr().lines().toList();
        Assert.assertEquals(lines.size(), 1, "Err: " + getErr());
        JsonNode error = new ObjectMapper().readTree(lines.get(0));
        Assert.assertEquals(error.get("severity").asText(), "error");
        Assert.assertEquals(error.get("kind").asText(), "usage");
        Assert.assertEquals(error.get("message").asText(), "unknown protocol 'kafka'");
        Assert.assertEquals(error.get("suggestion").asText(), "Use one of: http, ws.");
        Assert.assertTrue(getOut().isEmpty(), "Out: " + getOut());
    }

    @Test(description = "An unknown --output-format is a usage error, reported as text")
    public void testUnknownOutputFormat() {
        AsyncApiCmd cmd = run("spec.yaml", "--output-format", "xml");
        Assert.assertEquals(cmd.exitCode(), ExitCodes.USAGE);
        Assert.assertTrue(getErr().startsWith("bal asyncapi: error: invalid value 'xml' for --output-format"),
                "Err: " + getErr());
    }

    @Test(description = "A flag's value can be given with = as well as after a space (INV-3)")
    public void testEqualsFormStillParses() {
        run("/non/existent/spec.yaml", "--protocol=HTTP", "--spec-format=json", "--output=somewhere");
        Assert.assertFalse(getErr().contains("unknown protocol"), "Err: " + getErr());
        Assert.assertFalse(getErr().contains("invalid value"), "Err: " + getErr());
        Assert.assertTrue(getErr().contains("/non/existent/spec.yaml"), "Err: " + getErr());
    }

    @Test(description = "Printing the help or usage does not disturb options that were already parsed")
    public void testPrintingHelpKeepsParsedOptions() {
        AsyncApiCmd cmd = parse("/non/existent/spec.yaml", "--protocol", "http");
        cmd.printUsage(new StringBuilder());
        cmd.printLongDesc(new StringBuilder());
        errContent.reset();
        cmd.execute();
        Assert.assertFalse(getErr().contains("missing required argument"), "Err: " + getErr());
        Assert.assertTrue(getErr().contains("/non/existent/spec.yaml"), "Err: " + getErr());
    }

    @Test(description = "printUsage prints the synopsis")
    public void testPrintUsage() {
        new AsyncApiCmd(outStream, errStream).printUsage(new StringBuilder());
        Assert.assertEquals(getOut().strip(), "bal asyncapi [OPTIONS] <input>");
    }

    @Test(description = "The command has no subcommands (INV-8)")
    public void testNoSubcommands() {
        Assert.assertTrue(new CommandLine(new AsyncApiCmd(outStream, errStream)).getSubcommands().isEmpty());
    }

    @Test(description = "getName() returns 'asyncapi'")
    public void testGetName() {
        Assert.assertEquals(new AsyncApiCmd(outStream, errStream).getName(), "asyncapi");
    }

    @Test(description = "setParentCmdParser is a no-op")
    public void testSetParentCmdParser() {
        AsyncApiCmd cmd = new AsyncApiCmd(outStream, errStream);
        cmd.setParentCmdParser(new CommandLine(cmd));
        Assert.assertTrue(getErr().isEmpty(), "Err: " + getErr());
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

    @Test(description = "The rebuilt command quotes what a shell would split")
    public void testCommandLineQuotes() {
        BaseCmd base = new BaseCmd();
        base.outputPath = "my dir";
        base.force = true;
        Assert.assertEquals(CmdUtils.commandLine(base, "spec.yaml", "--force", "--dry-run"),
                "bal asyncapi spec.yaml -o 'my dir' --force --dry-run");
    }

    @Test(description = "Messages from elsewhere become one lowercase line without a trailing period")
    public void testOneLine() {
        Assert.assertEquals(CmdUtils.oneLine("Spec generation failed:\nNo service.\n"),
                "spec generation failed: No service");
        Assert.assertEquals(CmdUtils.oneLine("AsyncAPI is broken."), "AsyncAPI is broken");
        Assert.assertEquals(CmdUtils.oneLine(null), "an unexpected error occurred");
    }
}
