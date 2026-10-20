/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Output streams, exit behaviour and shared helpers handed to every {@link ProtocolCommand}, so protocol
 * implementations never touch {@code System.out}, {@code System.err} or {@link Runtime#exit} directly.
 */
final class CmdContext {

    private static final String WARNING_LICENSE_FILE_UNREADABLE =
            "warning: could not read license file: %s. Proceeding without license header.";

    private final PrintStream infoStream;
    private final PrintStream errStream;
    private final Path executionPath;
    private boolean exitWhenFinish;

    /**
     * Creates a context.
     *
     * @param infoStream     stream used for info output
     * @param errStream      stream used for warnings and errors
     * @param exitWhenFinish {@code false} to suppress {@link Runtime#exit} calls during tests
     */
    CmdContext(PrintStream infoStream, PrintStream errStream, boolean exitWhenFinish) {
        this.infoStream = infoStream;
        this.errStream = errStream;
        this.exitWhenFinish = exitWhenFinish;
        this.executionPath = Path.of(System.getProperty("user.dir"));
    }

    PrintStream infoStream() {
        return infoStream;
    }

    PrintStream errStream() {
        return errStream;
    }

    Path executionPath() {
        return executionPath;
    }

    void setExitWhenFinish(boolean exitWhenFinish) {
        this.exitWhenFinish = exitWhenFinish;
    }

    void printInfo(String message) {
        infoStream.println(message);
    }

    void printWarning(String message) {
        errStream.println("warning: " + message);
    }

    void printError(String message) {
        errStream.println(message);
    }

    void exitError() {
        if (exitWhenFinish) {
            Runtime.getRuntime().exit(1);
        }
    }

    /**
     * Reads the license header file; an unreadable file yields a warning and an empty header.
     *
     * @param licensePath path to the license header file
     * @return the file content, or {@code ""} if the file is unreadable
     */
    String readLicenseFile(String licensePath) {
        try {
            return Files.readString(Path.of(licensePath));
        } catch (IOException e) {
            errStream.println(String.format(WARNING_LICENSE_FILE_UNREADABLE, licensePath));
            return "";
        }
    }
}
