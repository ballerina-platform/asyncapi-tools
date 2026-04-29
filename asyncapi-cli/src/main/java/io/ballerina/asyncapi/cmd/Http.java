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
import io.ballerina.asyncapi.generator.spi.SpecToCodeOptions;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Sub-command that generates Ballerina listener/service code from an HTTP AsyncAPI definition.
 *
 */
@CommandLine.Command(name = "http", description = "Generate Ballerina code from an HTTP AsyncAPI definition.")
public class Http extends SubCmdBase {

    /**
     * Constructs an {@code Http} sub-command with default streams and the current working directory.
     */
    public Http() {
        super(CmdConstants.CmdType.HTTP);
    }

    /**
     * Test-only constructor.
     *
     * @param infoStream     stream used for info output
     * @param errStream      stream used for error output
     * @param exitWhenFinish {@code false} 
     */
    public Http(PrintStream infoStream, PrintStream errStream, boolean exitWhenFinish) {
        super(CmdConstants.CmdType.HTTP, infoStream, errStream, exitWhenFinish);
    }

    @Override
    protected void generate(CmdOptions opts, CmdConstants.Mode mode) {
        if (opts.isWithTests()) {
            printInfo("warning: --with-tests is not yet supported "
                    + "for the http subcommand and will be ignored.");
        }
        if (mode == CmdConstants.Mode.BALLERINA_TO_ASYNCAPI) {
            throw new AsyncApiCmdToolException(AsyncApiCmdToolDiagnostic.ASYNC_CLI_002);
        }
        try {
            Path inputPath = Path.of(opts.getInput());
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

            SpecToCodeOptions options = new SpecToCodeOptions.Builder()
                    .withLicenseHeader(
                            opts.getLicenseFilePath() != null
                                    ? readLicenseFile(opts.getLicenseFilePath())
                                    : "")
                    .withIncludeTestFiles(false)
                    .build();

            CodeGenOrchestrator.specToCode(
                    Protocol.HTTP, inputPath, outputPath, options);
            printInfo(ErrorMessages.SUCCESS_MESSAGE);

        } catch (GeneratorException e) {
            errStream.println("ERROR: " + e.getMessage());
            exitError();
        }
    }

}
