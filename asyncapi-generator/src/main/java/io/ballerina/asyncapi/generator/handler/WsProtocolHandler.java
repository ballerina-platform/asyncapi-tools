/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com)
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
package io.ballerina.asyncapi.generator.handler;

import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.Protocol;
import io.ballerina.asyncapi.generator.spi.CodeToSpecOptions;
import io.ballerina.asyncapi.generator.spi.ProtocolHandler;
import io.ballerina.asyncapi.generator.spi.SpecToCodeOptions;
import io.ballerina.asyncapi.generator.ws.asyncspec.BallerinaToAsyncApiGenerator;
import io.ballerina.asyncapi.generator.ws.asyncspec.diagnostic.AsyncApiConverterDiagnostic;
import io.ballerina.asyncapi.generator.ws.client.WsClientCodeGenerator;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@link ProtocolHandler} implementation for WebSocket AsyncAPI generation.
 * Supports both spec-to-code (client generation) and code-to-spec (spec export).
 */
public final class WsProtocolHandler implements ProtocolHandler {

    @Override
    public Protocol getProtocol() {
        return Protocol.WS;
    }

    @Override
    public boolean supportsSpecToCode() {
        return true;
    }

    @Override
    public boolean supportsCodeToSpec() {
        return true;
    }

    @Override
    public void specToCode(AsyncApiSpec spec,
                           Path outputPath,
                           SpecToCodeOptions options)
            throws GeneratorException {
        new WsClientCodeGenerator(
            spec,
            options.getLicenseHeader() != null
                ? options.getLicenseHeader() : "",
            options.isIncludeTestFiles()
        ).generate(outputPath);
    }

    @Override
    public void codeToSpec(Path inputPath,
                           Path outputPath,
                           CodeToSpecOptions options)
            throws GeneratorException {
        List<AsyncApiConverterDiagnostic> diagnostics =
            BallerinaToAsyncApiGenerator
                .generateAsyncAPIDefinitionsAllService(
                    inputPath,
                    outputPath,
                    options.getServiceName(),
                    options.isNeedJson(),
                    options.getOutStream());

        boolean hasErrors = diagnostics.stream()
            .anyMatch(d -> d.getDiagnosticSeverity()
                            == DiagnosticSeverity.ERROR);
        if (hasErrors) {
            String messages = diagnostics.stream()
                .filter(d -> d.getDiagnosticSeverity()
                              == DiagnosticSeverity.ERROR)
                .map(AsyncApiConverterDiagnostic::getMessage)
                .collect(Collectors.joining(
                    System.lineSeparator()));
            throw new GeneratorException(
                "AsyncAPI spec generation failed:"
                + System.lineSeparator() + messages);
        }
    }
}
