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
 * Picocli mixin that holds the common option flags shared across all asyncapi sub-commands.
 * Injected into sub-commands via {@code @CommandLine.Mixin} — this class is a pure data holder
 * with no lifecycle of its own.
 *
 */
public class BaseCmd {

    @CommandLine.Option(names = {"-h", "--help"}, hidden = true)
    public boolean helpFlag;

    @CommandLine.Option(names = {"-i", "--input"},
            description = "Path to the AsyncAPI definition file.")
    public String inputPath;

    @CommandLine.Option(names = {"-o", "--output"},
            description = "Path to the output directory.")
    public String outputPath;

    @CommandLine.Option(names = {"--license"},
            description = "Path to a file containing the license header for generated files.")
    public String licensePath;

    @CommandLine.Option(names = {"--json"},
            description = "Generate the AsyncAPI definition in JSON format.")
    public boolean jsonFlag;

    @CommandLine.Option(names = {"--service"},
            description = "Name of the service to generate.")
    public String serviceName;

    @CommandLine.Option(names = {"--with-tests"},
            description = "Generate test files along with the service.")
    public boolean withTests;

}
