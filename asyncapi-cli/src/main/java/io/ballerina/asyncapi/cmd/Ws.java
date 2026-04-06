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
import io.ballerina.asyncapi.generator.Protocol;
import io.ballerina.asyncapi.generator.spi.CodeToSpecOptions;
import io.ballerina.asyncapi.generator.spi.SpecToCodeOptions;
import picocli.CommandLine;

import java.io.PrintStream;
import java.nio.file.Path;

/**
 * Sub-command that generates Ballerina client/service code from a WebSocket AsyncAPI definition.
 *
 */
@CommandLine.Command(name = "ws", description = "Generate Ballerina code from a WebSocket AsyncAPI definition.")
public class Ws extends SubCmdBase {

    /**
     * Constructs a {@code Ws} sub-command with default streams and the current working directory.
     */
    public Ws() {
        super(CmdConstants.CmdType.WS);
    }

    /**
     * Test-only constructor.
     *
     * @param infoStream     stream used for info output
     * @param errStream      stream used for error output
     * @param exitWhenFinish {@code false} to suppress JVM exit during tests
     */
    public Ws(PrintStream infoStream, PrintStream errStream, boolean exitWhenFinish) {
        super(CmdConstants.CmdType.WS, infoStream, errStream, exitWhenFinish);
    }

    @Override
    protected void generate(CmdOptions opts, CmdConstants.Mode mode) {
        try {
            Path inputPath = Path.of(opts.getInput());
            Path outputPath = opts.getOutput() != null
                    ? Path.of(opts.getOutput()) : null;

            if (mode == CmdConstants.Mode.BALLERINA_TO_ASYNCAPI) {
                CodeToSpecOptions options =
                        new CodeToSpecOptions.Builder()
                                .withServiceName(opts.getService())
                                .withNeedJson(opts.isJson())
                                .withOutStream(System.out)
                                .build();
                CodeGenOrchestrator.codeToSpec(
                        Protocol.WS, inputPath, outputPath, options);
                printInfo(ErrorMessages.CODE_TO_SPEC_SUCCESS);

            } else {
                String licenseHeader =
                        opts.getLicenseFilePath() != null
                                ? readLicenseFile(opts.getLicenseFilePath())
                                : "";
                SpecToCodeOptions options =
                        new SpecToCodeOptions.Builder()
                                .withLicenseHeader(licenseHeader)
                                .withIncludeTestFiles(opts.isWithTests())
                                .build();
                CodeGenOrchestrator.specToCode(
                        Protocol.WS, inputPath, outputPath, options);
                printInfo(ErrorMessages.SUCCESS_MESSAGE);
            }

        } catch (GeneratorException e) {
            errStream.println("ERROR: " + e.getMessage());
            exitError();
        }
    }

}
