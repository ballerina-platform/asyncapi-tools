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
import java.nio.file.Path;

/**
 * Generates Ballerina listener/service code from an HTTP AsyncAPI definition.
 */
final class HttpProtocolCommand implements ProtocolCommand {

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

            SpecToCodeOptions options = new SpecToCodeOptions.Builder()
                    .withLicenseHeader("")
                    .withIncludeTestFiles(false)
                    .build();

            try (OutputStage stage = OutputStage.open(outputPath, opts, ctx)) {
                CodeGenOrchestrator.specToCode(Protocol.HTTP, inputPath, stage.stagingDir(), options);
                stage.commit(ErrorMessages.SUCCESS_MESSAGE);
            }
        } catch (GeneratorException e) {
            ctx.printError("error: " + e.getMessage());
            ctx.exitError();
        } catch (IOException e) {
            ctx.printError("error: failed to write the generated files: " + e.getMessage());
            ctx.exitError();
        }
    }
}
