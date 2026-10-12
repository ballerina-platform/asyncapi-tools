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
 */
public class BaseCmd {

    @CommandLine.Option(names = {"-h", "--help"}, hidden = true)
    public boolean helpFlag;

    @CommandLine.Parameters(arity = "0..1", paramLabel = "<input>",
            description = "Path to the AsyncAPI definition to generate code from, or to the Ballerina package "
                    + "directory or standalone .bal file to generate a definition from.")
    public String inputPath;

    @CommandLine.Option(names = {"--protocol"},
            description = "Protocol of the AsyncAPI definition: http or ws.")
    public String protocol;

    @CommandLine.Option(names = {"-o", "--output"},
            description = "Path to the output directory.")
    public String outputPath;

    @CommandLine.Option(names = {"--module"},
            description = "Module of the Ballerina package to read services from, or to write generated code "
                    + "into. Not used with a standalone .bal file.")
    public String module;

    @CommandLine.Option(names = {"--license"},
            description = "Path to a file containing the license header for generated files.")
    public String licensePath;

    @CommandLine.Option(names = {"--format"},
            description = "Format of the generated AsyncAPI definition: yaml or json. Defaults to yaml.")
    public String format;

    @CommandLine.Option(names = {"--service"},
            description = "Base path of the service to generate the definition for, e.g. /chat. "
                    + "Every service with that base path is generated.")
    public String serviceName;

    @CommandLine.Option(names = {"--with-tests"},
            description = "Generate test files along with the service.")
    public boolean withTests;

    @CommandLine.Option(names = {"--force"},
            description = "Overwrite files that already exist in the output location. Without it, existing "
                    + "files are skipped with a warning.")
    public boolean force;

    @CommandLine.Option(names = {"--dry-run"},
            description = "Show which files would be created, overwritten or skipped, without writing anything.")
    public boolean dryRun;

}
