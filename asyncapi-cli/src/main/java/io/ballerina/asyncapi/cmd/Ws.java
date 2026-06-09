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

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Sub-command that generates Ballerina client/service code from a WebSocket AsyncAPI definition.
 *
 */
@CommandLine.Command(name = "ws", description = "Generate Ballerina code from a WebSocket AsyncAPI definition.")
public class Ws extends SubCmdBase {
    private static final String CLIENT_BAL = "client.bal";
    private static final String TYPES_BAL = "types.bal";
    private static final String UTILS_BAL = "utils.bal";
    private static final String TESTS_TEST_BAL = "tests/test.bal";
    private static final String TESTS_CONFIG_TOML = "tests/Config.toml";

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

            if (mode == CmdConstants.Mode.BALLERINA_TO_ASYNCAPI) {
                try {
                    AsyncApiProjectUtils.validateBalFile(inputPath);
                } catch (AsyncApiCmdToolException e) {
                    errStream.println(e.getMessage());
                    exitError();
                    return;
                }
                Path outputPath = opts.getOutput() != null
                        ? Path.of(opts.getOutput()) : executionPath;
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
                Path effectivePath = opts.getOutput() != null
                        ? Path.of(opts.getOutput())
                        : Path.of(System.getProperty("user.dir"));

                Path outputPath;
                try {
                    outputPath = AsyncApiProjectUtils
                            .resolveOutputPath(effectivePath, opts.getModule(), errStream);
                } catch (AsyncApiCmdToolException e) {
                    errStream.println(e.getMessage());
                    exitError();
                    return;
                }

                if (!Files.exists(outputPath)) {
                    try {
                        Files.createDirectories(outputPath);
                    } catch (IOException e) {
                        errStream.println("ERROR: failed to create output directory: " + e.getMessage());
                        exitError();
                        return;
                    }
                }

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
                printInfo("Following files were created.");
                printInfo("-- " + CLIENT_BAL);
                printInfo("-- " + TYPES_BAL);
                printInfo("-- " + UTILS_BAL);
                if (opts.isWithTests()) {
                    printInfo("-- " + TESTS_TEST_BAL);
                    printInfo("-- " + TESTS_CONFIG_TOML);
                }
            }

        } catch (GeneratorException e) {
            errStream.println("ERROR: " + e.getMessage());
            exitError();
        }
    }

}
