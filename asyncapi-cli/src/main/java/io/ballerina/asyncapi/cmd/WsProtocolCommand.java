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

import java.io.IOException;
import java.nio.file.Path;

/**
 * Generates Ballerina client code from a WebSocket AsyncAPI definition, or an AsyncAPI definition from a
 * Ballerina service.
 */
final class WsProtocolCommand implements ProtocolCommand {

    @Override
    public Protocol protocol() {
        return Protocol.WS;
    }

    @Override
    public void generate(CmdOptions opts, CmdConstants.Mode mode, CmdContext ctx) {
        try {
            Path inputPath = Path.of(opts.getInput());
            if (mode == CmdConstants.Mode.BALLERINA_TO_ASYNCAPI) {
                codeToSpec(opts, inputPath, ctx);
            } else {
                specToCode(opts, inputPath, ctx);
            }
        } catch (GeneratorException e) {
            ctx.printError("error: " + e.getMessage());
            ctx.exitError();
        } catch (IOException e) {
            ctx.printError("error: failed to write the generated files: " + e.getMessage());
            ctx.exitError();
        }
    }

    private void codeToSpec(CmdOptions opts, Path inputPath, CmdContext ctx)
            throws GeneratorException, IOException {
        try {
            AsyncApiProjectUtils.validateBalFile(inputPath);
        } catch (AsyncApiCmdToolException e) {
            ctx.printError(e.getMessage());
            ctx.exitError();
            return;
        }
        Path outputPath = opts.getOutput() != null ? Path.of(opts.getOutput()) : ctx.executionPath();
        int skipped;
        try (OutputStage stage = OutputStage.open(outputPath, opts, ctx)) {
            CodeToSpecOptions options = new CodeToSpecOptions.Builder()
                    .withServiceName(opts.getService())
                    .withModuleName(opts.getModule())
                    .withNeedJson(opts.isJson())
                    .withOutStream(ctx.infoStream())
                    .build();
            CodeGenOrchestrator.codeToSpec(Protocol.WS, inputPath, stage.stagingDir(), options);
            skipped = stage.commit(ErrorMessages.CODE_TO_SPEC_SUCCESS);
        }
        exitIfSkipped(skipped, ctx);
    }

    // A skipped file can leave the output out of sync, so a script must not see this as a success.
    private static void exitIfSkipped(int skipped, CmdContext ctx) {
        if (skipped > 0) {
            ctx.exitError();
        }
    }

    private void specToCode(CmdOptions opts, Path inputPath, CmdContext ctx)
            throws GeneratorException, IOException {
        Path effectivePath = opts.getOutput() != null ? Path.of(opts.getOutput()) : ctx.executionPath();
        Path outputPath;
        try {
            outputPath = AsyncApiProjectUtils.resolveOutputPath(effectivePath, opts.getModule(), ctx.errStream());
        } catch (AsyncApiCmdToolException e) {
            ctx.printError(e.getMessage());
            ctx.exitError();
            return;
        }

        String licenseHeader = opts.getLicenseFilePath() != null ? ctx.readLicenseFile(opts.getLicenseFilePath()) : "";
        SpecToCodeOptions options = new SpecToCodeOptions.Builder()
                .withLicenseHeader(licenseHeader)
                .withIncludeTestFiles(opts.isWithTests())
                .build();
        int skipped;
        try (OutputStage stage = OutputStage.open(outputPath, opts, ctx)) {
            CodeGenOrchestrator.specToCode(Protocol.WS, inputPath, stage.stagingDir(), options);
            skipped = stage.commit(ErrorMessages.SUCCESS_MESSAGE);
        }
        exitIfSkipped(skipped, ctx);
    }
}
