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
package io.ballerina.asyncapi.generator.ws.client;

import io.ballerina.asyncapi.core.api.AsyncApiSpec;
import io.ballerina.asyncapi.generator.GeneratorException;
import io.ballerina.asyncapi.generator.ws.client.generator.ClientGenerator;
import io.ballerina.asyncapi.generator.ws.client.generator.DataTypesGenerator;
import io.ballerina.asyncapi.generator.ws.client.generator.TestGenerator;
import io.ballerina.asyncapi.generator.ws.client.generator.UtilGenerator;
import io.ballerina.asyncapi.generator.ws.client.model.GenSrcFile;
import io.ballerina.asyncapi.generator.ws.client.model.WsClientConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Console;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controls the flow of Ballerina WebSocket client code generation from a parsed AsyncAPI specification.
 */
public class WsClientCodeGenerator {

    private static final Logger LOG = LogManager.getLogger(WsClientCodeGenerator.class);

    private static final String CLIENT_FILE_NAME = "client.bal";
    private static final String TYPE_FILE_NAME = "types.bal";
    private static final String UTIL_FILE_NAME = "utils.bal";
    private static final String TEST_FILE_NAME = "test.bal";
    private static final String CONFIG_FILE_NAME = "Config.toml";
    private static final String TEST_DIR = "tests";

    private final AsyncApiSpec asyncApiSpec;
    private final String licenseHeader;
    private final boolean includeTestFiles;

    /**
     * Creates a new WebSocket client code generator.
     *
     * @param asyncApiSpec     the parsed AsyncAPI specification
     * @param licenseHeader    license header to prepend to generated {@code .bal} files
     * @param includeTestFiles whether to generate test boilerplate files
     */
    public WsClientCodeGenerator(AsyncApiSpec asyncApiSpec, String licenseHeader, boolean includeTestFiles) {
        this.asyncApiSpec = asyncApiSpec;
        this.licenseHeader = licenseHeader;
        this.includeTestFiles = includeTestFiles;
    }

    /**
     * Runs the full generation pipeline and writes Ballerina source files to {@code outputPath}.
     *
     * @param outputPath the directory to write generated files into
     * @throws GeneratorException if generation or file writing fails
     */
    public void generate(Path outputPath) throws GeneratorException {
        if (outputPath == null) {
            outputPath = Path.of("");
        }
        WsClientConfig config = new WsClientConfig.Builder()
                .withAsyncApi(asyncApiSpec)
                .withLicense(licenseHeader)
                .build();

        // Generate source content from each sub-generator
        String clientContent = new ClientGenerator(config).generate();
        String utilContent = new UtilGenerator(config).generate();
        String typesContent = new DataTypesGenerator(config).generate();

        // Collect generated files
        List<GenSrcFile> sourceFiles = new ArrayList<>();
        sourceFiles.add(new GenSrcFile(GenSrcFile.GenFileType.GEN_SRC, CLIENT_FILE_NAME, clientContent));
        if (!utilContent.isBlank()) {
            sourceFiles.add(new GenSrcFile(GenSrcFile.GenFileType.UTIL_SRC, UTIL_FILE_NAME, utilContent));
        }
        if (!typesContent.isBlank()) {
            sourceFiles.add(new GenSrcFile(GenSrcFile.GenFileType.MODEL_SRC, TYPE_FILE_NAME, typesContent));
        }

        // Optionally generate test boilerplate
        if (includeTestFiles) {
            TestGenerator testGenerator = new TestGenerator(config);
            String testContent = testGenerator.generateTest();
            String configContent = testGenerator.generateConfig();
            if (!testContent.isBlank()) {
                sourceFiles.add(new GenSrcFile(GenSrcFile.GenFileType.TEST_SRC, TEST_FILE_NAME, testContent));
            }
            if (!configContent.isBlank()) {
                sourceFiles.add(new GenSrcFile(GenSrcFile.GenFileType.CONFIG_SRC, CONFIG_FILE_NAME, configContent));
            }
        }

        // Write all files to disk
        List<Path> filePathsToWrite = new ArrayList<>();
        for (GenSrcFile file : sourceFiles) {
            boolean isTestFile = file.getType() == GenSrcFile.GenFileType.TEST_SRC
                    || file.getType() == GenSrcFile.GenFileType.CONFIG_SRC;
            Path filePath = isTestFile
                    ? outputPath.resolve(TEST_DIR).resolve(file.getFileName())
                    : outputPath.resolve(file.getFileName());
            filePathsToWrite.add(filePath);
        }

        validateOverwriteDecisions(filePathsToWrite);

        List<Path> writtenPaths = new ArrayList<>();
        for (int i = 0; i < sourceFiles.size(); i++) {
            GenSrcFile file = sourceFiles.get(i);
            Path filePath = filePathsToWrite.get(i);
            writtenPaths.add(writeFile(filePath, file.getContent()));
        }
        String fileList = writtenPaths.stream()
                .map(p -> "-- " + p.getFileName())
                .collect(Collectors.joining("\n"));
        LOG.info("Following files were created.\n{}", fileList);
    }

    /**
     * Writes generated content to a file at the given path,
     * creating parent directories as needed.
     *
     * @param filePath path of the file to write
     * @param content  the source content
     * @throws GeneratorException if the file cannot be written
     */
    private Path writeFile(Path filePath, String content) throws GeneratorException {
        try {
            if (filePath.getParent() != null) {
                Files.createDirectories(filePath.getParent());
            }
            Files.writeString(filePath, content, StandardCharsets.UTF_8);
            return filePath;
        } catch (IOException e) {
            throw new GeneratorException(String.format("Could not write to file: %s", filePath), e);
        }
    }

    private void validateOverwriteDecisions(List<Path> filePaths) throws GeneratorException {
        Console console = System.console();
        if (console == null) {
            return;
        }
        for (Path filePath : filePaths) {
            if (!Files.exists(filePath)) {
                continue;
            }
            while (true) {
                String answer = console.readLine(
                        "'%s' already exists. Overwrite? [y/n]: ",
                        filePath.getFileName());
                if ("y".equalsIgnoreCase(answer)) {
                    break;
                } else if ("n".equalsIgnoreCase(answer)) {
                    throw new GeneratorException(
                            String.format("Generation cancelled by user: '%s' already exists.", filePath));
                }
            }
        }
    }
}
