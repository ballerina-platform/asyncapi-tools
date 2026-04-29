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
package io.ballerina.asyncapi.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.ballerina.asyncapi.core.AsyncApiParser;
import io.ballerina.asyncapi.core.AsyncApiParserException;
import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.generator.spi.CodeToSpecOptions;
import io.ballerina.asyncapi.generator.spi.ProtocolHandler;
import io.ballerina.asyncapi.generator.spi.SpecToCodeOptions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Entry point for protocol-agnostic AsyncAPI code generation.
 * Dispatches to the appropriate {@link ProtocolHandler} discovered via ServiceLoader.
 */
public final class CodeGenOrchestrator {

    private static final Map<Protocol, ProtocolHandler> HANDLERS;

    static {
        HANDLERS = new EnumMap<>(Protocol.class);
        ServiceLoader.load(ProtocolHandler.class)
            .forEach(h -> HANDLERS.put(h.getProtocol(), h));
    }

    private CodeGenOrchestrator() {}

    /**
     * Generates Ballerina code from an AsyncAPI spec file.
     *
     * @param protocol  target protocol
     * @param inputPath path to the AsyncAPI spec file (JSON or YAML)
     * @param outputPath directory where generated files are written
     * @param options   generation options
     * @throws GeneratorException if generation fails
     */
    public static void specToCode(Protocol protocol,
                                  Path inputPath,
                                  Path outputPath,
                                  SpecToCodeOptions options)
            throws GeneratorException {
        ProtocolHandler handler = resolve(protocol);
        if (!handler.supportsSpecToCode()) {
            throw new GeneratorException(
                "specToCode is not supported for protocol: "
                + protocol);
        }
        AsyncApiSpec spec = parseSpec(inputPath);
        handler.specToCode(spec, outputPath, options);
    }

    /**
     * Generates an AsyncAPI spec from a Ballerina source file.
     *
     * @param protocol  source protocol
     * @param inputPath path to the Ballerina source file
     * @param outputPath directory where the spec file is written
     * @param options   generation options
     * @throws GeneratorException if generation fails
     */
    public static void codeToSpec(Protocol protocol,
                                  Path inputPath,
                                  Path outputPath,
                                  CodeToSpecOptions options)
            throws GeneratorException {
        ProtocolHandler handler = resolve(protocol);
        if (!handler.supportsCodeToSpec()) {
            throw new GeneratorException(
                "codeToSpec is not supported for protocol: "
                + protocol);
        }
        handler.codeToSpec(inputPath, outputPath, options);
    }

    private static ProtocolHandler resolve(Protocol protocol)
            throws GeneratorException {
        ProtocolHandler handler = HANDLERS.get(protocol);
        if (handler == null) {
            throw new GeneratorException(
                "No handler registered for protocol: "
                + protocol);
        }
        return handler;
    }

    private static AsyncApiSpec parseSpec(Path inputPath)
            throws GeneratorException {
        try {
            String content = Files.readString(inputPath);
            String fileName = inputPath.getFileName()
                                       .toString().toLowerCase();
            if (fileName.endsWith(".yaml")
                    || fileName.endsWith(".yml")) {
                ObjectMapper yamlMapper =
                    new ObjectMapper(new YAMLFactory());
                JsonNode node = yamlMapper.readTree(content);
                content = new ObjectMapper()
                    .writeValueAsString(node);
            }
            return AsyncApiParser.parseFromJsonString(content);
        } catch (AsyncApiParserException e) {
            throw new GeneratorException(
                "Failed to parse AsyncAPI spec: "
                + e.getMessage(), e);
        } catch (IOException e) {
            throw new GeneratorException(
                "Failed to read input file: " + inputPath, e);
        }
    }
}
