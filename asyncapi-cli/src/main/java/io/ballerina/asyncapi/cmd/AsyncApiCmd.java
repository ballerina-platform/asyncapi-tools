/*
 *  Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
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

import io.ballerina.asyncapi.cmd.websockets.AsyncApiDiagnostic;
import io.ballerina.asyncapi.cmd.websockets.AsyncApiToBallerinaGenerator;
import io.ballerina.asyncapi.cmd.websockets.BallerinaToAsyncApiGenerator;
import io.ballerina.asyncapi.cmd.websockets.CmdConstants;
import io.ballerina.asyncapi.cmd.websockets.CmdUtils;
import io.ballerina.asyncapi.codegenerator.application.Application;
import io.ballerina.asyncapi.codegenerator.application.CodeGenerator;
import io.ballerina.asyncapi.codegenerator.configuration.BallerinaAsyncApiException;
import io.ballerina.asyncapi.websocketscore.exception.BallerinaAsyncApiExceptionWs;
import io.ballerina.asyncapi.websocketscore.generators.asyncspec.Constants;
import io.ballerina.asyncapi.websocketscore.generators.asyncspec.diagnostic.AsyncApiConverterDiagnostic;
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

import static io.ballerina.asyncapi.cmd.AsyncApiConstants.EXPERIMENTAL_WARNING;
import static io.ballerina.asyncapi.cmd.AsyncApiConstants.INPUT_FLAG;
import static io.ballerina.asyncapi.cmd.AsyncApiConstants.INPUT_FLAG_ALT;
import static io.ballerina.asyncapi.cmd.AsyncApiConstants.JSON_FLAG;
import static io.ballerina.asyncapi.cmd.AsyncApiConstants.LICENSE_FLAG;
import static io.ballerina.asyncapi.cmd.AsyncApiConstants.LINE_SEPARATOR;
import static io.ballerina.asyncapi.cmd.AsyncApiConstants.OUTPUT_FLAG;
import static io.ballerina.asyncapi.cmd.AsyncApiConstants.OUTPUT_FLAG_ALT;
import static io.ballerina.asyncapi.cmd.AsyncApiConstants.PROTOCOL_FLAG;
import static io.ballerina.asyncapi.cmd.AsyncApiConstants.SERVICE_FLAG;
import static io.ballerina.asyncapi.cmd.AsyncApiConstants.TEST_FLAG;
import static io.ballerina.asyncapi.cmd.AsyncApiConstants.VALID_HTTP_NAMES;
import static io.ballerina.asyncapi.cmd.AsyncApiConstants.VALID_WS_NAMES;
import static io.ballerina.asyncapi.cmd.AsyncApiMessages.CLIENT_GENERATION_FAILED;
import static io.ballerina.asyncapi.cmd.AsyncApiMessages.INVALID_OPTION_ERROR_HTTP;
import static io.ballerina.asyncapi.cmd.AsyncApiMessages.INVALID_USE_OF_JSON_FLAG_WARNING;
import static io.ballerina.asyncapi.cmd.AsyncApiMessages.INVALID_USE_OF_LICENSE_FLAG_WARNING;
import static io.ballerina.asyncapi.cmd.AsyncApiMessages.INVALID_USE_OF_SERVICE_FLAG_WARNING;
import static io.ballerina.asyncapi.cmd.AsyncApiMessages.INVALID_USE_OF_TEST_FLAG_WARNING;
import static io.ballerina.asyncapi.cmd.AsyncApiMessages.MESSAGE_INVALID_LICENSE_STREAM;

/**
 * Main class to implement "asyncapi" command for ballerina. Commands for Listener generation from AsyncApi spec
 *
 */
@CommandLine.Command(
        name = "asyncapi",
        description = "Generate the Ballerina sources for a given AsyncAPI definition."
)
public class AsyncApiCmd implements BLauncherCmd {
    private static final String CMD_NAME = "asyncapi";
    private static final int EXIT_CODE_0 = 0;
    private static final int EXIT_CODE_1 = 1;
    private static final int EXIT_CODE_2 = 2;
    private static final ExitHandler DEFAULT_EXIT_HANDLER = code -> Runtime.getRuntime().exit(code);

    private PrintStream outStream;
    private Path executionPath = Paths.get(System.getProperty("user.dir"));
    private Path targetOutputPath;
    private final ExitHandler exitHandler;

    /**
     * Functional interface for handling exit behavior.
     * Public to allow test access from other packages.
     */
    @FunctionalInterface
    public interface ExitHandler {
        void exit(int code);
    }

    @CommandLine.Option(names = {"-h", "--help"}, hidden = true)
    private boolean helpFlag;

    @CommandLine.Option(names = {INPUT_FLAG_ALT, INPUT_FLAG}, description = "File path to the AsyncAPI specification")
    private boolean inputPath;

    @CommandLine.Option(names = {OUTPUT_FLAG_ALT, OUTPUT_FLAG},
            description = "Directory to store the generated Ballerina service. If this is not provided, the generated" +
                    " files will be stored in the current execution directory")
    private String outputPath;

    @CommandLine.Option(names = {PROTOCOL_FLAG}, description = "The protocol to be used for the service")
    private String protocol = "http";

    @CommandLine.Option(names = {LICENSE_FLAG}, description = "Location of the file which contains the license header")
    private String licenseFilePath;

    @CommandLine.Option(names = {SERVICE_FLAG}, description = "Service name that need to documented as asyncapi " +
            "contract")
    private String service;

    @CommandLine.Option(names = {TEST_FLAG}, hidden = true, description = "Generate test files")
    private boolean includeTestFiles;

    @CommandLine.Option(names = {JSON_FLAG}, description = "Generate json file")
    private boolean generatedFileType;

    @CommandLine.Parameters
    private List<String> argList;

    /**
     * Constructor that initialize with the default values.
     */
    public AsyncApiCmd() {
        this(System.err, Paths.get(System.getProperty("user.dir")), DEFAULT_EXIT_HANDLER);
    }

    /**
     * Constructor override, which takes output stream and execution dir as inputs.
     *
     * @param outStream    output stream from ballerina
     * @param executionDir defines the directory location of  execution of ballerina command
     */
    public AsyncApiCmd(PrintStream outStream, Path executionDir) {
        this(outStream, executionDir, DEFAULT_EXIT_HANDLER);
    }

    /**
     * Constructor for testing with custom exit handler.
     * This is public to allow tests in other packages to use it.
     *
     * @param outStream    output stream from ballerina
     * @param executionDir defines the directory location of execution of ballerina command
     * @param exitHandler  custom exit handler (for testing)
     */
    public AsyncApiCmd(PrintStream outStream, Path executionDir, ExitHandler exitHandler) {
        this.outStream = outStream;
        this.executionPath = executionDir;
        this.exitHandler = exitHandler;
    }

    private void exit(int code) {
        exitHandler.exit(code);
    }

    @Override
    public void execute() {
        if (helpFlag) {
            printLongDesc(new StringBuilder());
            exit(EXIT_CODE_0);
            return;
        }
        if (inputPath) {
            if (argList == null) {
                outStream.println(AsyncApiMessages.MESSAGE_FOR_MISSING_INPUT);
                outStream.flush();
                exit(EXIT_CODE_1);
                return;
            }
            String fileName = argList.get(0);

            if (VALID_HTTP_NAMES.contains(protocol.toLowerCase())) {
                verifyValidInputsForHttp();
                Application codeGenerator = new CodeGenerator();
                try {
                    codeGenerator.generate(fileName, (outputPath == null) ?
                            String.valueOf(executionPath) : outputPath);
                } catch (BallerinaAsyncApiException e) {
                    outStream.println(e.getMessage());
                    outStream.flush();
                    exit(EXIT_CODE_1);
                    return;
                }
            } else if (VALID_WS_NAMES.contains(protocol.toLowerCase())) {
                outStream.println(EXPERIMENTAL_WARNING);
                if (fileName.endsWith(Constants.YAML_EXTENSION) || fileName.endsWith(Constants.JSON_EXTENSION) ||
                        fileName.endsWith(Constants.YML_EXTENSION)) {
                    giveWarningsForInvalidClientGenOptions();
                    try {
                        asyncApiToBallerinaWs(fileName);
                    } catch (IOException e) {
                        outStream.println(e.getLocalizedMessage());
                        outStream.flush();
                        exit(EXIT_CODE_1);
                        return;
                    }
                    // when -i has bal extension
                } else if (fileName.endsWith(CmdConstants.BAL_EXTENSION)) {
                    giveWarningsForInvalidSpecGenOptions();
                    try {
                        ballerinaToAsyncApiWs(fileName);
                    } catch (Exception e) {
                        outStream.println(e.getLocalizedMessage());
                        outStream.flush();
                        exit(EXIT_CODE_1);
                        return;
                    }
                    // If -i has no extensions
                } else {
                    outStream.println(AsyncApiMessages.MISSING_CONTRACT_PATH);
                    outStream.flush();
                    exit(EXIT_CODE_1);
                    return;
                }
            } else {
                outStream.println(String.format(AsyncApiMessages.MESSAGE_INVALID_PROTOCOL, protocol));
                outStream.flush();
                exit(EXIT_CODE_1);
                return;
            }
        } else {
            printLongDesc(new StringBuilder());
            outStream.flush();
            exit(EXIT_CODE_2);
            return;
        }

        outStream.flush();
        exit(EXIT_CODE_0);
    }

    private void giveWarningsForInvalidSpecGenOptions() {
        if (licenseFilePath != null) {
            outStream.println(INVALID_USE_OF_LICENSE_FLAG_WARNING);
        }
        if (includeTestFiles) {
            outStream.println(INVALID_USE_OF_TEST_FLAG_WARNING);
        }
    }

    private void giveWarningsForInvalidClientGenOptions() {
        if (generatedFileType) {
            outStream.println(INVALID_USE_OF_JSON_FLAG_WARNING);
        }
        if (service != null) {
            outStream.println(INVALID_USE_OF_SERVICE_FLAG_WARNING);
        }
    }

    private void verifyValidInputsForHttp() {
        if (licenseFilePath != null) {
            outStream.println(String.format(INVALID_OPTION_ERROR_HTTP, LICENSE_FLAG));
            exit(EXIT_CODE_1);
        }
        if (service != null) {
            outStream.println(String.format(INVALID_OPTION_ERROR_HTTP, SERVICE_FLAG));
            exit(EXIT_CODE_1);
        }
        if (includeTestFiles) {
            outStream.println(String.format(INVALID_OPTION_ERROR_HTTP, TEST_FLAG));
            exit(EXIT_CODE_1);
        }
        if (generatedFileType) {
            outStream.println(String.format(INVALID_OPTION_ERROR_HTTP, JSON_FLAG));
            exit(EXIT_CODE_1);
        }
    }

    private void ballerinaToAsyncApiWs(String fileName) {
        List<AsyncApiConverterDiagnostic> errors = new ArrayList<>();
        final File balFile = new File(fileName);
        try {
            Path balFilePath = Paths.get(balFile.getCanonicalPath());
            setOutputPathWs();
            // Check service name it is mandatory
            List<AsyncApiConverterDiagnostic> generationErrors = BallerinaToAsyncApiGenerator
                    .generateAsyncAPIDefinitionsAllService(balFilePath, targetOutputPath, service, generatedFileType,
                            outStream);
            errors.addAll(generationErrors);
        } catch (IOException e) {
            DiagnosticMessages message = DiagnosticMessages.AAS_CONVERTOR_102;
            ExceptionDiagnostic error = new ExceptionDiagnostic(message.getCode(), message.getDescription(), null,
                    e.getLocalizedMessage());
            errors.add(error);
        }

        if (!errors.isEmpty()) {
            for (AsyncApiConverterDiagnostic error : errors) {
                if (error instanceof ExceptionDiagnostic exceptionDiagnostic) {
                    AsyncApiDiagnostic diagnostic = CmdUtils.constructAsyncAPIDiagnostic(exceptionDiagnostic.getCode(),
                            exceptionDiagnostic.getMessage(), exceptionDiagnostic.getDiagnosticSeverity(),
                            exceptionDiagnostic.getLocation().orElse(null));
                    outStream.println(diagnostic);
                    exit(EXIT_CODE_1);
                } else if (error instanceof IncompatibleRemoteDiagnostic incompatibleError) {
                    AsyncApiDiagnostic diagnostic = CmdUtils.constructAsyncAPIDiagnostic(incompatibleError.getCode(),
                            incompatibleError.getMessage(), incompatibleError.getDiagnosticSeverity(),
                            incompatibleError.getLocation().get());
                    outStream.println(diagnostic);
                }
            }
        }
    }

    private void asyncApiToBallerinaWs(String fileName) throws IOException {
        AsyncApiToBallerinaGenerator generator = new AsyncApiToBallerinaGenerator(this.extractLicenseHeaderWs(),
                this.includeTestFiles);
        final File asyncApiFile = new File(fileName);
        setOutputPathWs();
        Path resourcePath = Paths.get(asyncApiFile.getCanonicalPath());
        generatesClientFileWs(generator, resourcePath);
    }

    /**
     * This util is to set the license header content which is to be added at the beginning of the ballerina files.
     */
    private String extractLicenseHeaderWs() {
        try {
            String licenseHeader;
            if (this.licenseFilePath != null && !this.licenseFilePath.isBlank()) {
                Path filePath = Paths.get((new File(this.licenseFilePath).getCanonicalPath()));
                licenseHeader = Files.readString(Paths.get(filePath.toString()));
                if (!licenseHeader.endsWith(LINE_SEPARATOR)) {
                    licenseHeader = licenseHeader + LINE_SEPARATOR + LINE_SEPARATOR;
                } else if (!licenseHeader.endsWith(LINE_SEPARATOR + LINE_SEPARATOR)) {
                    licenseHeader = licenseHeader + LINE_SEPARATOR;
                }
                return licenseHeader;
            }
        } catch (IOException e) {
            outStream.println(String.format(MESSAGE_INVALID_LICENSE_STREAM, this.licenseFilePath, e.getMessage()));
            exit(EXIT_CODE_1);
        }
        return "";
    }

    /**
     * This util is to get the output Path.
     */
    private void setOutputPathWs() {
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
    private void generatesClientFileWs(AsyncApiToBallerinaGenerator generator, Path resourcePath) {
        try {
            generator.generateClient(resourcePath, targetOutputPath);
        } catch (IOException | FormatterException | BallerinaAsyncApiExceptionWs e) {
            if (e.getLocalizedMessage() != null) {
                outStream.println(e.getLocalizedMessage());
                exit(EXIT_CODE_1);
            } else {
                outStream.println(CLIENT_GENERATION_FAILED);
                exit(EXIT_CODE_1);
            }
        }
    }

    @Override
    public String getName() {
        return CMD_NAME;
    }

    @Override
    public void printLongDesc(StringBuilder stringBuilder) {
        ClassLoader classLoader = AsyncApiCmd.class.getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream("ballerina-asyncapi.help");
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(inputStreamReader)) {
            String content = br.readLine();
            outStream.append(content);
            while ((content = br.readLine()) != null) {
                outStream.append('\n').append(content);
            }
            outStream.append('\n');
        } catch (IOException ignored) {
        }
    }

    @Override
    public void printUsage(StringBuilder stringBuilder) {
        stringBuilder.append("  ballerina " + CMD_NAME + " --input chat.proto\n");
    }

    @Override
    public void setParentCmdParser(picocli.CommandLine commandLine) {
    }
}
