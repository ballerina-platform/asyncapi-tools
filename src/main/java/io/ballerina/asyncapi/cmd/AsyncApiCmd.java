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

import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Main class to implement "asyncapi" command for ballerina. Commands for Listener generation from AsyncAPI spec
 */
@CommandLine.Command(
        name = "asyncapi",
        description = "Generates Ballerina client for AsyncAPI contract of an Event-API."
)
public class AsyncApiCmd implements BLauncherCmd {
    private static final String CMD_NAME = "asyncapi";
    private PrintStream outStream;
    private boolean exitWhenFinish;
    private Path executionPath = Paths.get(System.getProperty("user.dir"));

    @CommandLine.Option(names = {"-i", "--input"}, description = "Generating the client and service both files")
    private boolean inputPath;

    @CommandLine.Option(names = {"-o", "--output"}, description = "Location of the generated Ballerina service, " +
            "client and model files.")
    private String outputPath;

    @CommandLine.Parameters
    private List<String> argList;

    public AsyncApiCmd() {
        this.outStream = System.err;
        this.exitWhenFinish = true;
    }

    public AsyncApiCmd(PrintStream outStream, Path executionDir) {
        new AsyncApiCmd(outStream, executionDir, true);
    }

    public AsyncApiCmd(PrintStream outStream, Path executionDir, boolean exitWhenFinish) {
        this.outStream = outStream;
        this.executionPath = executionDir;
        this.exitWhenFinish = exitWhenFinish;
    }

    @Override
    public void execute() {
        if (inputPath) {
            if (argList == null) {
                outStream.println("Missing the input file path," +
                        " Please provide the path of the AsyncAPI specification with -i flag");
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
        }
    }

    @Override
    public String getName() {
        return CMD_NAME;
    }

    @Override
    public void printLongDesc(StringBuilder stringBuilder) {}

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
