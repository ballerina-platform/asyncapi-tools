/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package io.ballerina.asyncapi.cmd;

import io.ballerina.asyncapi.cmd.websockets.AsyncAPIDiagnostic;
import io.ballerina.asyncapi.cmd.websockets.AsyncAPIToBallerinaGenerator;
import io.ballerina.asyncapi.cmd.websockets.BallerinaToAsyncAPIGenerator;
import io.ballerina.asyncapi.cmd.websockets.CmdConstants;
import io.ballerina.asyncapi.cmd.websockets.CmdUtils;
import io.ballerina.asyncapi.codegenerator.application.Application;
import io.ballerina.asyncapi.codegenerator.application.CodeGenerator;
import io.ballerina.asyncapi.codegenerator.configuration.BallerinaAsyncApiException;
import io.ballerina.asyncapi.websocketscore.exception.BallerinaAsyncApiExceptionWs;
import io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants;
import io.ballerina.asyncapi.websocketscore.generators.asyncspec.diagnostic.AsyncAPIConverterDiagnostic;
import io.ballerina.asyncapi.websocketscore.generators.asyncspec.diagnostic.DiagnosticMessages;
import io.ballerina.asyncapi.websocketscore.generators.asyncspec.diagnostic.ExceptionDiagnostic;
import io.ballerina.asyncapi.websocketscore.generators.asyncspec.diagnostic.IncompatibleRemoteDiagnostic;
import io.ballerina.cli.BLauncherCmd;
import org.ballerinalang.formatter.core.FormatterException;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Main class to implement "asyncapi" command for ballerina. Commands for Listener generation from AsyncAPI spec
 */
@CommandLine.Command(
        name = "asyncapi",
        description = "Generate the Ballerina sources for a given AsyncAPI definition."
)
public class AsyncApiCmd implements BLauncherCmd {
    private static final String CMD_NAME = "asyncapi";
    private PrintStream outStream;
    private boolean exitWhenFinish;
    private Path executionPath = Paths.get(System.getProperty("user.dir"));
    private Path targetOutputPath;

    @CommandLine.Option(names = {"-h", "--help"}, hidden = true)
    private boolean helpFlag;

    @CommandLine.Option(names = {"-i", "--input"}, description = "File path to the AsyncAPI specification")
    private boolean inputPath;

    @CommandLine.Option(names = {"-o", "--output"},
            description = "Directory to store the generated Ballerina service. " +
            "If this is not provided, the generated files will be stored in the the current execution directory")
    private String outputPath;

    @CommandLine.Option (names = {"--protocol"}, description = "The protocol to be used for the service")
    private String protocol = "http";

    @CommandLine.Option(names = {"--license"}, description = "Location of the file which contains the license header")
    private String licenseFilePath;

    @CommandLine.Option(names = {"-s", "--service"}, description = "Service name that need to documented as asyncapi " +
            "contract")
    private String service;

    @CommandLine.Option(names = {"--with-tests"}, hidden = true, description = "Generate test files")
    private boolean includeTestFiles;

    @CommandLine.Option(names = {"--json"}, description = "Generate json file")
    private boolean generatedFileType;

    @CommandLine.Parameters
    private List<String> argList;

    /**
     * Constructor that initialize with the default values.
     */
    public AsyncApiCmd() {
        this.outStream = System.err;
        this.exitWhenFinish = true;
    }

    /**
     * Constructor override, which takes output stream and execution dir as inputs.
     *
     * @param outStream      output stream from ballerina
     * @param executionDir   defines the directory location of  execution of ballerina command
     */
    public AsyncApiCmd(PrintStream outStream, Path executionDir) {
        new AsyncApiCmd(outStream, executionDir, true);
    }

    /**
     * Constructor override, which takes output stream and execution dir and exits when finish as inputs.
     *
     * @param outStream         output stream from ballerina
     * @param executionDir      defines the directory location of  execution of ballerina command
     * @param exitWhenFinish    exit when finish the execution
     */
    public AsyncApiCmd(PrintStream outStream, Path executionDir, boolean exitWhenFinish) {
        this.outStream = outStream;
        this.executionPath = executionDir;
        this.exitWhenFinish = exitWhenFinish;
    }

    /**
     * Constructor override, which takes output stream and execution dir and exits when finish as inputs.
     *
     * @param executionDir      defines the directory location of  execution of ballerina command
     * @param exitWhenFinish    exit when finish the execution
     */
    public AsyncApiCmd(Path executionDir, boolean exitWhenFinish) {
        this.outStream = System.err;
        this.executionPath = executionDir;
        this.exitWhenFinish = exitWhenFinish;
    }

    @Override
    public void execute() {
        if (helpFlag) {
            String commandUsageInfo = BLauncherCmd.getCommandUsageInfo(CMD_NAME);
            outStream.println(commandUsageInfo);
            return;
        }
        if (inputPath) {
            if (argList == null) {
                outStream.println(AsyncApiMessages.MESSAGE_FOR_MISSING_INPUT);
                exitError(this.exitWhenFinish);
                return;
            }
            String fileName = argList.get(0);

            if (protocol.equals("http")) {
                verifyValidInputsForHttp();
                Application codeGenerator = new CodeGenerator();
                try {
                    codeGenerator.generate(fileName, (outputPath == null) ?
                            String.valueOf(executionPath) : outputPath);
                } catch (BallerinaAsyncApiException e) {
                    outStream.println(e.getMessage());
                    exitError(this.exitWhenFinish);
                }
            } else if (protocol.equals("ws") || protocol.equals("wss") || protocol.equals("websocket")) {
                if (fileName.endsWith(Constants.YAML_EXTENSION) || fileName.endsWith(Constants.JSON_EXTENSION) ||
                        fileName.endsWith(Constants.YML_EXTENSION)) {
                    try {
                        asyncApiToBallerinaWs(fileName);
                    } catch (IOException e) {
                        outStream.println(e.getLocalizedMessage());
                        exitError(this.exitWhenFinish);
                    }
                    // when -i has bal extension
                } else if (fileName.endsWith(CmdConstants.BAL_EXTENSION)) {
                    try {
                        ballerinaToAsyncApiWs(fileName);
                    } catch (Exception exception) {
                        outStream.println(exception.getMessage());
                        exitError(this.exitWhenFinish);
                    }
                    // If -i has no extensions
                } else {
                    outStream.println(AsyncApiMessages.MISSING_CONTRACT_PATH);
                    exitError(this.exitWhenFinish);
                }
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

    private void verifyValidInputsForHttp() {
        if (licenseFilePath != null) {
            outStream.println(AsyncApiMessages.MESSAGE_FOR_LICENSE_FLAG);
            exitError(this.exitWhenFinish);
        }
        if (service != null) {
            outStream.println(AsyncApiMessages.MESSAGE_FOR_SERVICE_FLAG);
            exitError(this.exitWhenFinish);
        }
        if (includeTestFiles) {
            outStream.println(AsyncApiMessages.MESSAGE_FOR_TEST_FLAG);
            exitError(this.exitWhenFinish);
        }
        if (generatedFileType) {
            outStream.println(AsyncApiMessages.MESSAGE_FOR_JSON_FLAG);
            exitError(this.exitWhenFinish);
        }
    }

    private void ballerinaToAsyncApiWs(String fileName) {
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
        getTargetOutputPathWs();
        // Check service name it is mandatory
        BallerinaToAsyncAPIGenerator asyncApiConverter = new BallerinaToAsyncAPIGenerator();
        asyncApiConverter.generateAsyncAPIDefinitionsAllService(balFilePath, targetOutputPath, service,
                generatedFileType);

        errors.addAll(asyncApiConverter.getErrors());
        if (!errors.isEmpty()) {
            for (AsyncAPIConverterDiagnostic error : errors) {
                if (error instanceof ExceptionDiagnostic) {
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

    private void asyncApiToBallerinaWs(String fileName) throws IOException {
        AsyncAPIToBallerinaGenerator generator = new AsyncAPIToBallerinaGenerator();
        generator.setLicenseHeader(this.setLicenseHeaderWs());
        generator.setIncludeTestFiles(this.includeTestFiles);
        final File asyncAPIFile = new File(fileName);
        getTargetOutputPathWs();
        Path resourcePath = Paths.get(asyncAPIFile.getCanonicalPath());
        generatesClientFileWs(generator, resourcePath);
    }

    /**
     * This util is to set the license header content which is to be added at the beginning of the ballerina files.
     */
    private String setLicenseHeaderWs() {
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
    private void getTargetOutputPathWs() {
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
    private void generatesClientFileWs(AsyncAPIToBallerinaGenerator generator, Path resourcePath) {
        try {
            generator.generateClient(resourcePath.toString(), targetOutputPath.toString());
        } catch (IOException | FormatterException | BallerinaAsyncApiExceptionWs e) {
            if (e.getLocalizedMessage() != null) {
                outStream.println(e.getLocalizedMessage());
                exitError(this.exitWhenFinish);
            } else {
                outStream.println(AsyncApiMessages.CLIENT_GENERATION_FAILED);
                exitError(this.exitWhenFinish);
            }
        }
    }

    @Override
    public String getName() {
        return CMD_NAME;
    }

    @Override
    public void printLongDesc(StringBuilder stringBuilder) {
        try (InputStream inputStream = ClassLoader.getSystemResourceAsStream("ballerina-asyncapi.help");
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(inputStreamReader)) {

            String content;
            while ((content = br.readLine()) != null) {
                stringBuilder.append(content).append('\n');
            }
        } catch (IOException ignored) {
        }
    }

    @Override
    public void printUsage(StringBuilder stringBuilder) {}

    @Override
    public void setParentCmdParser(picocli.CommandLine commandLine) {}

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
}
