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

import picocli.CommandLine;

/**
 * Picocli mixin that holds the option flags and the positional input of the asyncapi command.
 * This class is a pure data holder with no lifecycle of its own.
 *
 * <p>The descriptions and parameter labels below are the only place the flags are documented: the
 * {@code --help} text is generated from them, so a flag cannot be missing from it or described differently.
 * Each description is one line, lowercase and imperative, and the protocol and format lists in the labels are
 * checked against {@link ProtocolCommands} and {@link CmdConstants} by a test.
 */
public class BaseCmd {

    @CommandLine.Option(names = {"-h", "--help"}, description = "print this help")
    public boolean helpFlag;

    @CommandLine.Option(names = {"-V", "--version"}, description = "print the tool version")
    public boolean versionFlag;

    @CommandLine.Parameters(arity = "0..1", paramLabel = "<input>",
            description = "AsyncAPI definition, or Ballerina package or .bal file")
    public String inputPath;

    @CommandLine.Option(names = {"--protocol"}, paramLabel = "<http|ws>",
            description = "use this protocol (default: detected)")
    public String protocol;

    @CommandLine.Option(names = {"-o", "--output"}, paramLabel = "<dir>",
            description = "write files to this directory (default: .)")
    public String outputPath;

    @CommandLine.Option(names = {"--module"}, paramLabel = "<name>",
            description = "use this module of the package")
    public String module;

    @CommandLine.Option(names = {"--license"}, paramLabel = "<file>",
            description = "add this license to generated files")
    public String licensePath;

    @CommandLine.Option(names = {"--spec-format"}, paramLabel = "<yaml|json>",
            description = "write yaml or json (default: yaml)")
    public String specFormat;

    @CommandLine.Option(names = {"--service"}, paramLabel = "<path>",
            description = "export only the service at this path")
    public String serviceName;

    @CommandLine.Option(names = {"--spec-side"}, paramLabel = "<client|service>",
            description = "read as this side (default: client)")
    public String specSide;

    @CommandLine.Option(names = {"--with-tests"},
            description = "generate tests along with the code")
    public boolean withTests;

    @CommandLine.Option(names = {"--force"},
            description = "overwrite existing files without asking")
    public boolean force;

    @CommandLine.Option(names = {"-n", "--dry-run"},
            description = "list the files without writing them")
    public boolean dryRun;

    @CommandLine.Option(names = {"--output-format"}, paramLabel = "<text|json>",
            description = "print text or json (default: json if piped)")
    public String outputFormat;

    // Renamed to --spec-format; still read, with a warning.
    @CommandLine.Option(names = {"--format"}, hidden = true)
    public String format;

    @CommandLine.Option(names = {"-i", "--input"}, hidden = true)
    public String removedInputPath;

    @CommandLine.Option(names = {"--json"}, hidden = true)
    public boolean removedJsonFlag;
}
