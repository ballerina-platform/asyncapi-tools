/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
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
package io.ballerina.asyncapi.cli;

import io.ballerina.asyncapi.core.exception.BallerinaAsyncApiException;
import io.ballerina.asyncapi.core.generators.asyncspec.diagnostic.AsyncAPIConverterDiagnostic;
import io.ballerina.asyncapi.core.generators.asyncspec.diagnostic.DiagnosticMessages;
import io.ballerina.asyncapi.core.generators.asyncspec.diagnostic.ExceptionDiagnostic;
import io.ballerina.asyncapi.core.generators.asyncspec.diagnostic.IncompatibleRemoteDiagnostic;
import io.ballerina.cli.BLauncherCmd;
import org.ballerinalang.formatter.core.FormatterException;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static io.ballerina.asyncapi.cli.CmdConstants.BAL_EXTENSION;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.JSON_EXTENSION;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.YAML_EXTENSION;
import static io.ballerina.asyncapi.core.generators.asyncspec.Constants.YML_EXTENSION;

/**
 * Main class to implement "asyncapi1" command for ballerina. Commands for AsyncAPI spec
 * generation.
 */
@CommandLine.Command(
        name = "asyncapi1",
        description = "Generates AsyncAPI contract for Ballerina Service."
)
public class AsyncAPICmd implements BLauncherCmd {
    private static final String CMD_NAME = "asyncapi1";

    private PrintStream outStream = System.err;
    private Path executionPath = Paths.get(System.getProperty("user.dir"));
    private Path targetOutputPath;
    private boolean exitWhenFinish;

    @CommandLine.Option(names = {"-h", "--help"}, hidden = true)
    private boolean helpFlag;

    @CommandLine.Option(names = {"-i", "--input"}, description = "Generating the asyncapi definitions for bal files")
    private boolean inputPath;

    @CommandLine.Option(names = {"--license"}, description = "Location of the file which contains the license header")
    private String licenseFilePath;

    @CommandLine.Option(names = {"-o", "--output"}, description = "Location of the generated asyncapi definitions")
    private String outputPath;
    @CommandLine.Option(names = {"-s", "--service"}, description = "Service name that need to documented as asyncapi " +
            "contract")
    private String service;

    @CommandLine.Option(names = {"--with-tests"}, hidden = true, description = "Generate test files")
    private boolean includeTestFiles;

    @CommandLine.Option(names = {"--service-name"}, description = "Service name for generated files")
    private String generatedServiceName;

    @CommandLine.Option(names = {"--json"}, description = "Generate json file")
    private boolean generatedFileType;

    @CommandLine.Parameters
    private List<String> argList;

    public AsyncAPICmd(Path executionDir) {
        new AsyncAPICmd(executionDir, true);
    }

    public AsyncAPICmd(Path executionDir, boolean exitWhenFinish) {
        this.executionPath = executionDir;
        this.exitWhenFinish = exitWhenFinish;
    }

    /**
     * Exit with error code 1.
     *
     * @param exit Whether to exit or not.
     */
    private static void exitError(boolean exit) {
        if (exit) {
            Runtime.getRuntime().exit(1);
        }
    }

    @Override
    public void execute() {
        //Check help flag is enabled
        if (helpFlag) {
            String commandUsageInfo = BLauncherCmd.getCommandUsageInfo(getName());
            outStream.println(commandUsageInfo);
            return;
        }
        //Check if cli input argument is present
        if (inputPath) {
            //Check if an AsyncApi definition is provided
            if (argList == null) {
                outStream.println(ErrorMessages.MISSING_CONTRACT_PATH);
                exitError(this.exitWhenFinish);
                return;
            }
            // if given ballerina service file it generates asyncapi contract file
            // else it generates error message to enter correct input file
            String fileName = argList.get(0);

            //When -i has a yaml or json extension
            if (fileName.endsWith(YAML_EXTENSION) || fileName.endsWith(JSON_EXTENSION) ||
                    fileName.endsWith(YML_EXTENSION)) {
                try {
                    asyncApiToBallerina(fileName);
                } catch (IOException e) {
                    outStream.println(e.getLocalizedMessage());
                    exitError(this.exitWhenFinish);
                }
                // when -i has bal extension
            } else if (fileName.endsWith(BAL_EXTENSION)) {
                try {

                    ballerinaToAsyncApi(fileName);

                } catch (Exception exception) {
                    outStream.println(exception.getMessage());
                    exitError(this.exitWhenFinish);
                }
                // If -i has no extensions
            } else {
                outStream.println(ErrorMessages.MISSING_CONTRACT_PATH);
                exitError(this.exitWhenFinish);
            }
        } else {
            String commandUsageInfo = BLauncherCmd.getCommandUsageInfo(getName());
            outStream.println(commandUsageInfo);
            exitError(this.exitWhenFinish);
            return;
        }

        if (this.exitWhenFinish) {
            Runtime.getRuntime().exit(0);
        }
    }

    /**
     * This util method to generate asyncApi contract based on the given service ballerina file.
     *
     * @param fileName input resource file
     */
    private void ballerinaToAsyncApi(String fileName) {
        List<AsyncAPIConverterDiagnostic> errors = new ArrayList<>();
        final File balFile = new File(fileName);
        Path balFilePath = null;
        try {
            balFilePath = Paths.get(balFile.getCanonicalPath());
        } catch (IOException e) {
            DiagnosticMessages message = DiagnosticMessages.AAS_CONVERTOR_102;
            ExceptionDiagnostic error = new ExceptionDiagnostic(message.getCode(),
                    message.getDescription(), null, e.getLocalizedMessage());
            errors.add(error);
        }
        getTargetOutputPath();
        // Check service name it is mandatory
        BallerinaToAsyncAPIGenerator asyncApiConverter = new BallerinaToAsyncAPIGenerator();
        asyncApiConverter.generateAsyncAPIDefinitionsAllService(balFilePath, targetOutputPath, service,
                generatedFileType);

        errors.addAll(asyncApiConverter.getErrors());
        if (!errors.isEmpty()) {
            for (AsyncAPIConverterDiagnostic error : errors) {
                if (error instanceof ExceptionDiagnostic) {
                    this.outStream = System.err;
                    ExceptionDiagnostic exceptionDiagnostic = (ExceptionDiagnostic) error;
                    AsyncAPIDiagnostic diagnostic = CmdUtils.constructAsyncAPIDiagnostic(exceptionDiagnostic.getCode(),
                            exceptionDiagnostic.getMessage(), exceptionDiagnostic.getDiagnosticSeverity(),
                            exceptionDiagnostic.getLocation().orElse(null));
                    outStream.println(diagnostic);
                    exitError(this.exitWhenFinish);
                } else if (error instanceof IncompatibleRemoteDiagnostic) {
                    IncompatibleRemoteDiagnostic incompatibleError = (IncompatibleRemoteDiagnostic) error;
                    AsyncAPIDiagnostic diagnostic = CmdUtils.constructAsyncAPIDiagnostic(incompatibleError.getCode(),
                            incompatibleError.getMessage(), incompatibleError.getDiagnosticSeverity(),
                            incompatibleError.getLocation().get());
                    outStream.println(diagnostic);
                }
            }
        }
    }


    /**
     * This util method for generating service and client stub using given contract file.
     *
     * @param fileName input resource file
     */
    private void asyncApiToBallerina(String fileName) throws IOException {
        AsyncAPIToBallerinaGenerator generator = new AsyncAPIToBallerinaGenerator();
        generator.setLicenseHeader(this.setLicenseHeader());
        generator.setIncludeTestFiles(this.includeTestFiles);
        final File asyncAPIFile = new File(fileName);
        getTargetOutputPath();
        Path resourcePath = Paths.get(asyncAPIFile.getCanonicalPath());
        generatesClientFile(generator, resourcePath);
    }

    /**
     * This util is to set the license header content which is to be added at the beginning of the ballerina files.
     */
    private String setLicenseHeader() {
        String licenseHeader = "";
        try {
            if (this.licenseFilePath != null && !this.licenseFilePath.isBlank()) {
                Path filePath = Paths.get((new File(this.licenseFilePath).getCanonicalPath()));
                licenseHeader = Files.readString(Paths.get(filePath.toString()));
                if (!licenseHeader.endsWith("\n")) {
                    licenseHeader = licenseHeader + "\n\n";
                } else if (!licenseHeader.endsWith("\n\n")) {
                    licenseHeader = licenseHeader + "\n";
                }
            }
        } catch (IOException e) {
            outStream.println("Invalid license file path : " + this.licenseFilePath +
                    ". " + e.getMessage() + ".");
            exitError(this.exitWhenFinish);
        }
        return licenseHeader;
    }

    /**
     * This util is to get the output Path.
     */
    private void getTargetOutputPath() {
        targetOutputPath = executionPath;
        if (this.outputPath != null) {
            if (Paths.get(outputPath).isAbsolute()) {
                targetOutputPath = Paths.get(outputPath);
            } else {
                targetOutputPath = Paths.get(targetOutputPath.toString(), outputPath);
            }
        }
    }

    /**
     * A Util to Client generation.
     *
     * @param generator    generator object
     * @param resourcePath resource Path
     */
    private void generatesClientFile(AsyncAPIToBallerinaGenerator generator, Path resourcePath) {
        try {
            generator.generateClient(resourcePath.toString(), targetOutputPath.toString());
        } catch (IOException | FormatterException | BallerinaAsyncApiException e) {
            if (e.getLocalizedMessage() != null) {
                outStream.println(e.getLocalizedMessage());
                exitError(this.exitWhenFinish);
            } else {
                outStream.println(ErrorMessages.CLIENT_GENERATION_FAILED);
                exitError(this.exitWhenFinish);
            }
        }
    }


    @Override
    public String getName() {
        return CMD_NAME;
    }

    @Override
    public void printLongDesc(StringBuilder out) {
    }

    @Override
    public void printUsage(StringBuilder stringBuilder) {
    }

    @Override
    public void setParentCmdParser(CommandLine parentCmdParser) {
    }

}
