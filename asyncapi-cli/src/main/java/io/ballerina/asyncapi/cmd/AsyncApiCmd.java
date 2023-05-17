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

import io.ballerina.asyncapi.codegenerator.application.Application;
import io.ballerina.asyncapi.codegenerator.application.CodeGenerator;
import io.ballerina.asyncapi.codegenerator.configuration.BallerinaAsyncApiException;
import io.ballerina.cli.BLauncherCmd;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    @CommandLine.Option(names = {"-h", "--help"}, hidden = true)
    private boolean helpFlag;

    @CommandLine.Option(names = {"-i", "--input"}, description = "File path to the AsyncAPI specification")
    private boolean inputPath;

    @CommandLine.Option(names = {"-o", "--output"},
            description = "Directory to store the generated Ballerina service. " +
            "If this is not provided, the generated files will be stored in the the current execution directory")
    private String outputPath;

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
            Application codeGenerator = new CodeGenerator();
            try {
                codeGenerator.generate(fileName, (outputPath == null) ? String.valueOf(executionPath) : outputPath);
            } catch (BallerinaAsyncApiException e) {
                outStream.println(e.getMessage());
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
