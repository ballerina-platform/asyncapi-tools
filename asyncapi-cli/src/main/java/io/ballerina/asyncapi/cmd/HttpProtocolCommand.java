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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Generates Ballerina listener/service code from an HTTP AsyncAPI definition.
 */
final class HttpProtocolCommand implements ProtocolCommand {

    private static final String TYPES_BAL = "types.bal";
    private static final String SERVICE_TYPES_BAL = "service_types.bal";
    private static final String LISTENER_BAL = "listener.bal";
    private static final String DISPATCHER_SERVICE_BAL = "dispatcher_service.bal";

    @Override
    public Protocol protocol() {
        return Protocol.HTTP;
    }

    @Override
    public void generate(CmdOptions opts, CmdConstants.Mode mode, CmdContext ctx) {
        try {
            Path inputPath = Path.of(opts.getInput());
            Path effectivePath = opts.getOutput() != null
                    ? Path.of(opts.getOutput())
                    : ctx.executionPath();

            Path outputPath;
            try {
                outputPath = AsyncApiProjectUtils.resolveOutputPath(effectivePath, opts.getModule(),
                        ctx.errStream());
            } catch (AsyncApiCmdToolException e) {
                ctx.printError(e.getMessage());
                ctx.exitError();
                return;
            }

            if (!Files.exists(outputPath)) {
                try {
                    Files.createDirectories(outputPath);
                } catch (IOException e) {
                    ctx.printError("ERROR: failed to create output directory: " + e.getMessage());
                    ctx.exitError();
                    return;
                }
            }

            SpecToCodeOptions options = new SpecToCodeOptions.Builder()
                    .withLicenseHeader("")
                    .withIncludeTestFiles(false)
                    .build();

            CodeGenOrchestrator.specToCode(Protocol.HTTP, inputPath, outputPath, options);
            ctx.printInfo(ErrorMessages.SUCCESS_MESSAGE);
            ctx.printInfo("Following files were created.");
            ctx.printInfo("-- " + TYPES_BAL);
            ctx.printInfo("-- " + SERVICE_TYPES_BAL);
            ctx.printInfo("-- " + LISTENER_BAL);
            ctx.printInfo("-- " + DISPATCHER_SERVICE_BAL);
        } catch (GeneratorException e) {
            ctx.printError("ERROR: " + e.getMessage());
            ctx.exitError();
        }
    }
}
