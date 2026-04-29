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

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/**
 * Base class for CLI command tests.
 * Manages temporary output directories and output stream capture.
 */
public abstract class CmdTestBase {

    protected Path tmpDir;
    protected ByteArrayOutputStream outContent;
    protected ByteArrayOutputStream errContent;
    protected PrintStream outStream;
    protected PrintStream errStream;

    @BeforeMethod
    public void setUpStreams() throws IOException {
        this.tmpDir = Files.createTempDirectory(
            "cmd-test-" + System.nanoTime());
        this.outContent = new ByteArrayOutputStream();
        this.errContent = new ByteArrayOutputStream();
        this.outStream = new PrintStream(outContent);
        this.errStream = new PrintStream(errContent);
    }

    @AfterMethod
    public void tearDown() throws IOException {
        if (tmpDir != null && Files.exists(tmpDir)) {
            Files.walk(tmpDir)
                .sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        Assert.fail(
                            "Failed to delete: " + p, e);
                    }
                });
        }
        outStream.close();
        errStream.close();
    }

    protected String getOut() {
        return outContent.toString();
    }

    protected String getErr() {
        return errContent.toString();
    }

    protected boolean outputFileExists(String fileName) {
        return Files.exists(tmpDir.resolve(fileName));
    }
}
