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
package io.ballerina.asyncapi.generator.spi;

import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.Protocol;

import java.nio.file.Path;

/**
 * SPI contract for protocol-specific AsyncAPI generation handlers.
 * Implementations declare which direction(s) they support via
 * {@link #supportsSpecToCode()} and {@link #supportsCodeToSpec()}.
 */
public interface ProtocolHandler {

    /**
     * Returns the protocol this handler is responsible for.
     *
     * @return the protocol
     */
    Protocol getProtocol();

    /**
     * Returns true if this handler supports spec-to-code generation.
     * Defaults to false — override to opt in.
     *
     * @return true if spec-to-code is supported
     */
    default boolean supportsSpecToCode() {
        return false;
    }

    /**
     * Returns true if this handler supports code-to-spec generation.
     * Defaults to false — override to opt in.
     *
     * @return true if code-to-spec is supported
     */
    default boolean supportsCodeToSpec() {
        return false;
    }

    /**
     * Generates Ballerina code from a parsed AsyncAPI spec.
     * Only called when supportsSpecToCode() returns true.
     *
     * @param spec       parsed AsyncAPI specification
     * @param outputPath directory where generated files are written
     * @param options    generation options
     * @throws GeneratorException if generation fails
     */
    default void specToCode(AsyncApiSpec spec,
                            Path outputPath,
                            SpecToCodeOptions options)
            throws GeneratorException {
        throw new GeneratorException(
            "specToCode is not supported for protocol: "
            + getProtocol());
    }

    /**
     * Generates an AsyncAPI spec from a Ballerina source file.
     * Only called when supportsCodeToSpec() returns true.
     *
     * @param inputPath  path to the Ballerina source file
     * @param outputPath directory where the spec file is written
     * @param options    generation options
     * @throws GeneratorException if generation fails
     */
    default void codeToSpec(Path inputPath,
                            Path outputPath,
                            CodeToSpecOptions options)
            throws GeneratorException {
        throw new GeneratorException(
            "codeToSpec is not supported for protocol: "
            + getProtocol());
    }
}
