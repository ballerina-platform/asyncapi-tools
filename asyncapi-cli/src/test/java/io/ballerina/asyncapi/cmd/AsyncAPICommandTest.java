/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.ballerina.asyncapi.cmd;

import io.ballerina.asyncapi.cli.AsyncAPICmd;
import io.ballerina.cli.launcher.BLauncherException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * OpenAPI command cmd common class to handle temp dirs and outputs.
 */
public abstract class AsyncAPICommandTest {
    protected Path tmpDir;
    protected PrintStream printStream;
    protected final Path resourceDir = Paths.get("src/test/resources/").toAbsolutePath();
    private ByteArrayOutputStream console;
    
    @BeforeClass
    public void setup() throws IOException {
        this.tmpDir = Files.createTempDirectory("asyncapi-cmd-test-out-" + System.nanoTime());
        this.console = new ByteArrayOutputStream();
        this.printStream = new PrintStream(this.console);
    }

    protected String readOutput(boolean slient) throws IOException {
        String output = "";
        output = this.console.toString();
        this.console.close();
        this.console = new ByteArrayOutputStream();
        this.printStream = new PrintStream(this.console);
        if (!slient) {
            PrintStream out = System.out;
            out.println(output);
        }
        return output;
    }
}
