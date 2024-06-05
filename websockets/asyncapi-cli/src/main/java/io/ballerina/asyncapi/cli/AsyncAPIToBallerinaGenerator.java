/*
 * Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.asyncapi.cli;

import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25DocumentImpl;
import io.ballerina.asyncapi.core.GeneratorUtils;
import io.ballerina.asyncapi.core.exception.BallerinaAsyncApiException;
import io.ballerina.asyncapi.core.generators.asyncspec.utils.CodegenUtils;
import io.ballerina.asyncapi.core.generators.client.IntermediateClientGenerator;
import io.ballerina.asyncapi.core.generators.client.TestGenerator;
import io.ballerina.asyncapi.core.generators.client.model.AASClientConfig;
import io.ballerina.asyncapi.core.generators.schema.BallerinaTypesGenerator;
import io.ballerina.asyncapi.core.model.GenSrcFile;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import org.ballerinalang.formatter.core.Formatter;
import org.ballerinalang.formatter.core.FormatterException;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static io.ballerina.asyncapi.cli.CmdConstants.CLIENT_FILE_NAME;
import static io.ballerina.asyncapi.cli.CmdConstants.CONFIG_FILE_NAME;
import static io.ballerina.asyncapi.cli.CmdConstants.DEFAULT_CLIENT_PKG;
import static io.ballerina.asyncapi.cli.CmdConstants.GenType.GEN_CLIENT;
import static io.ballerina.asyncapi.cli.CmdConstants.GenType.GEN_SERVICE;
import static io.ballerina.asyncapi.cli.CmdConstants.TEST_DIR;
import static io.ballerina.asyncapi.cli.CmdConstants.TEST_FILE_NAME;
import static io.ballerina.asyncapi.cli.CmdConstants.TYPE_FILE_NAME;
import static io.ballerina.asyncapi.cli.CmdUtils.setGeneratedFileName;
import static io.ballerina.asyncapi.core.GeneratorConstants.ASYNCAPI_PATH_SEPARATOR;
import static io.ballerina.asyncapi.core.GeneratorConstants.UTIL_FILE_NAME;

/**
 * This class generates Ballerina Websocket client for a provided AsyncAPI definition.
 *
 */
public class AsyncAPIToBallerinaGenerator {
    private static final PrintStream outStream = System.err;
    private String srcPackage;
    private String licenseHeader = "";
    private boolean includeTestFiles;

    /**
     * Generates ballerina websocket client for provided Async API Definition in {@code definitionPath}.
     * Generated source will be written to a ballerina module at {@code outPath}
     * Method can be use for generating Ballerina websocket clients.
     *
     * @param definitionPath Input Async Api Definition file path
     * @param outPath        Destination file path to save generated client files including types.bal, utils.bal
     *                       If not provided {@code definitionPath} will be used as the default destination path
     * @throws IOException   when file operations fail
     * @throws BallerinaAsyncApiException when code generator fails
     */
    public void generateClient(String definitionPath, String outPath)
            throws IOException, BallerinaAsyncApiException, FormatterException {
        Path srcPath = Paths.get(outPath);
        Path implPath = CodegenUtils.getImplPath(srcPackage, srcPath);
        List<GenSrcFile> genFiles = generateClientFiles(Paths.get(definitionPath));
        writeGeneratedSources(genFiles, srcPath, implPath, GEN_CLIENT);
    }


    /**
     *
     * @param sources Generated all sources as a list
     * @param srcPath Output path provided
     * @param implPath Source path
     * @param type  check wheather the file type is service or client
     * @throws IOException
     */

    private void writeGeneratedSources(List<GenSrcFile> sources, Path srcPath, Path implPath,
                                       CmdConstants.GenType type)
            throws IOException {
        //  Remove old generated file with same name
        List<File> listFiles = new ArrayList<>();
        if (Files.exists(srcPath)) {
            File[] files = new File(String.valueOf(srcPath)).listFiles();
            if (files != null) {
                listFiles.addAll(Arrays.asList(files));
                for (File file : files) {
                    if (file.isDirectory() && file.getName().equals("tests")) {
                        File[] innerFiles = new File(srcPath + "/tests").listFiles();
                        if (innerFiles != null) {
                            listFiles.addAll(Arrays.asList(innerFiles));
                        }
                    }
                }
            }
        }

        for (File file : listFiles) {
            for (GenSrcFile gFile : sources) {
                if (file.getName().equals(gFile.getFileName())) {
                    if (System.console() != null) {
                        String userInput = System.console().readLine("There is already a/an " + file.getName() +
                                " in the location. Do you want to override the file? [y/N] ");
                        if (!Objects.equals(userInput.toLowerCase(Locale.ENGLISH), "y")) {
                            int duplicateCount = 0;
                            setGeneratedFileName(listFiles, gFile, duplicateCount);
                        }
                    }
                }
            }
        }

        for (GenSrcFile file : sources) {
            Path filePath;

            // We only overwrite files of overwritable type.
            // So non overwritable files will be written to disk only once.
            if (!file.getType().isOverwritable()) {
                filePath = implPath.resolve(file.getFileName());
                if (Files.notExists(filePath)) {
                    String fileContent = file.getFileName().endsWith(".bal") ?
                            (licenseHeader + file.getContent()) : file.getContent();
                    CodegenUtils.writeFile(filePath, fileContent);
                }
            } else {
                boolean isDuplicatedFileInTests = file.getFileName().matches("test.+[0-9]+.bal") ||
                        file.getFileName().matches("Config.+[0-9]+.toml");
                if (file.getFileName().equals(TEST_FILE_NAME) || file.getFileName().equals(CONFIG_FILE_NAME) ||
                        isDuplicatedFileInTests) {
                    // Create test directory if not exists in the path. If exists do not throw an error
                    Files.createDirectories(Paths.get(srcPath + ASYNCAPI_PATH_SEPARATOR + TEST_DIR));
                    filePath = Paths.get(srcPath.resolve(TEST_DIR + ASYNCAPI_PATH_SEPARATOR +
                            file.getFileName()).toFile().getCanonicalPath());
                } else {
                    filePath = Paths.get(srcPath.resolve(file.getFileName()).toFile().getCanonicalPath());
                }
                String fileContent = file.getFileName().endsWith(".bal") ?
                        (licenseHeader + file.getContent()) : file.getContent();
                CodegenUtils.writeFile(filePath, fileContent);
            }
        }

        //This will print the generated files to the console
        if (type.equals(GEN_SERVICE)) {
            outStream.println("Service generated successfully and the AsyncAPI contract is copied to path " + srcPath
                    + ".");
        } else if (type.equals(GEN_CLIENT)) {
            outStream.println("Client generated successfully.");
        }
        outStream.println("Following files were created.");
        Iterator<GenSrcFile> iterator = sources.iterator();
        while (iterator.hasNext()) {
            outStream.println("-- " + iterator.next().getFileName());
        }
    }

    /**
     * Generate code for ballerina client.
     * @param asyncAPI path to the AsyncAPI definition
     * @return generated source files as a list of {@link GenSrcFile}
     * @throws IOException when code generation with specified templates fails
     */
    private List<GenSrcFile> generateClientFiles(Path asyncAPI)
            throws IOException, BallerinaAsyncApiException, FormatterException {
        if (srcPackage == null || srcPackage.isEmpty()) {
            srcPackage = DEFAULT_CLIENT_PKG;
        }
        List<GenSrcFile> sourceFiles = new ArrayList<>();
        // Normalize AsyncAPI definition

        AsyncApi25DocumentImpl asyncAPIDef = GeneratorUtils.normalizeAsyncAPI(asyncAPI);
        // Generate ballerina service and resources.
        AASClientConfig.Builder clientMetaDataBuilder = new AASClientConfig.Builder();
        AASClientConfig asyncAPIClientConfig = clientMetaDataBuilder
                .withAsyncAPI(asyncAPIDef)
                .withLicense(licenseHeader)
                .build();


        //Generate client intermediate code
        IntermediateClientGenerator intermediateClientGenerator = new IntermediateClientGenerator(asyncAPIClientConfig);
        String mainContent = Formatter.format(intermediateClientGenerator.generateSyntaxTree()).toString();
        sourceFiles.add(new GenSrcFile(GenSrcFile.GenFileType.GEN_SRC, CLIENT_FILE_NAME, mainContent));


        //Generate util functions for client intermediate code
        String utilContent = Formatter.format(
                intermediateClientGenerator.getBallerinaUtilGenerator().generateUtilSyntaxTree()).toString();
        if (!utilContent.isBlank()) {
            sourceFiles.add(new GenSrcFile(GenSrcFile.GenFileType.UTIL_SRC, UTIL_FILE_NAME, utilContent));
        }
        List<TypeDefinitionNode> preGeneratedTypeDefNodes = new ArrayList<>(
                intermediateClientGenerator.getBallerinaAuthConfigGenerator().getAuthRelatedTypeDefinitionNodes());
        preGeneratedTypeDefNodes.addAll(intermediateClientGenerator.getTypeDefinitionNodeList());


        //Generate ballerina records to represent schemas in client intermediate code
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(
                asyncAPIDef, preGeneratedTypeDefNodes);

        // Generate schema generator syntax tree
        SyntaxTree schemaSyntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();
        String schemaContent = Formatter.format(schemaSyntaxTree).toString();


        if (!schemaContent.isBlank()) {
            sourceFiles.add(new GenSrcFile(GenSrcFile.GenFileType.MODEL_SRC,  TYPE_FILE_NAME,
                    schemaContent));
        }

        // Generate test boilerplate code for test cases
        if (this.includeTestFiles) {
            TestGenerator testGenerator = new TestGenerator(intermediateClientGenerator);
            String testContent = Formatter.format(testGenerator.generateSyntaxTree()).toString();
            sourceFiles.add(new GenSrcFile(GenSrcFile.GenFileType.GEN_SRC,  TEST_FILE_NAME, testContent));

            String configContent = testGenerator.getConfigTomlFile();
            if (!configContent.isBlank()) {
                sourceFiles.add(new GenSrcFile(GenSrcFile.GenFileType.GEN_SRC,
                        CONFIG_FILE_NAME, configContent));
            }
        }

        return sourceFiles;
    }

    /**
     * Set the content of license header.
     *
     * @param licenseHeader license header value received from command line.
     */
    public void setLicenseHeader(String licenseHeader) {
        this.licenseHeader = licenseHeader;
    }

    /**
     * set whether to add test files or not.
     *
     * @param includeTestFiles value received from command line by "--with tests"
     */
    public void setIncludeTestFiles(boolean includeTestFiles) {
        this.includeTestFiles = includeTestFiles;
    }
}
