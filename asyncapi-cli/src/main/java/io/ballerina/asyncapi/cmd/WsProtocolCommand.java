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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
    public void generate(CmdOptions opts, CmdConstants.Mode mode, Resolved resolved, CmdContext ctx)
            throws GeneratorException, IOException {
        Path inputPath = Path.of(opts.getInput());
        if (mode == CmdConstants.Mode.BALLERINA_TO_ASYNCAPI) {
            codeToSpec(opts, inputPath, ctx);
        } else {
            specToCode(opts, inputPath, resolved, ctx);
        }
    }

    private void codeToSpec(CmdOptions opts, Path inputPath, CmdContext ctx) throws GeneratorException, IOException {
        // A definition is not Ballerina code, so it can go to any directory (TGT-9).
        Path outputPath = opts.getOutput() != null ? Path.of(opts.getOutput()) : ctx.executionPath();
        ByteArrayOutputStream messages = new ByteArrayOutputStream();
        try (OutputStage stage = OutputStage.open(outputPath, opts, ctx);
             PrintStream messageStream = new PrintStream(messages, true, StandardCharsets.UTF_8)) {
            CodeToSpecOptions options = new CodeToSpecOptions.Builder()
                    .withServiceName(opts.getService())
                    .withModuleName(opts.getModule())
                    .withNeedJson(opts.isJson())
                    .withOutStream(messageStream)
                    .build();
            CodeGenOrchestrator.codeToSpec(Protocol.WS, inputPath, stage.stagingDir(), options);
            // stdout carries only the result, so what the generator says becomes warnings on stderr
            messages.toString(StandardCharsets.UTF_8).lines().map(String::strip).filter(line -> !line.isEmpty())
                    .forEach(line -> ctx.printWarning(CmdUtils.oneLine(line)));
            stage.commit();
        }
    }

    private void specToCode(CmdOptions opts, Path inputPath, Resolved resolved, CmdContext ctx)
            throws GeneratorException, IOException {
        Path outputPath = outputPackage(opts, ctx);
        SpecToCodeOptions options = new SpecToCodeOptions.Builder()
                .withLicenseHeader("")
                .withIncludeTestFiles(opts.isWithTests())
                .withWarnings(ctx::printSpecWarning)
                .withServiceSide(opts.isServiceSide())
                .build();
        try (OutputStage stage = openStage(outputPath, opts, ctx)) {
            CodeGenOrchestrator.specToCode(Protocol.WS, inputPath, stage.stagingDir(), options);
            GeneratedHeader.apply(stage.stagingDir(), resolved.license());
            stage.commit();
        }
    }

    /**
     * Resolves where generated Ballerina code goes: the package given with {@code --output}, or the current
     * directory, and the module given with {@code --module}.
     *
     * @param opts the options
     * @param ctx  the run's context
     * @return the package root or module directory
     * @throws AsyncApiCmdToolException if the location is not a package or a module of one
     */
    static Path outputPackage(CmdOptions opts, CmdContext ctx) {
        Path effectivePath = opts.getOutput() != null ? Path.of(opts.getOutput()) : ctx.executionPath();
        return AsyncApiProjectUtils.resolveOutputPath(effectivePath, opts.getModule(), ctx.errStream());
    }

    /**
     * Opens the stage for generated code, noting a module the run creates (MOD-2).
     *
     * @param outputPath the package root or module directory
     * @param opts       the options
     * @param ctx        the run's context
     * @return the stage
     * @throws IOException if the temporary directory can't be created
     */
    static OutputStage openStage(Path outputPath, CmdOptions opts, CmdContext ctx) throws IOException {
        OutputStage stage = OutputStage.open(outputPath, opts, ctx);
        if (opts.getModule() != null && !Files.exists(outputPath)) {
            stage.createsModule(opts.getModule());
        }
        return stage;
    }
}
