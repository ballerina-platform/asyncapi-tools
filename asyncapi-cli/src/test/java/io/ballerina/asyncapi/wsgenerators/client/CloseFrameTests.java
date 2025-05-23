/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
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
package io.ballerina.asyncapi.wsgenerators.client;

import io.ballerina.asyncapi.cmd.AsyncApiCmd;
import io.ballerina.asyncapi.wsgenerators.common.TestUtils;
import io.ballerina.cli.launcher.BLauncherException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Test close frame support.
 */
public class CloseFrameTests {
    private static final Path RES_DIR = Paths.get("src/test/resources/websockets" +
            "/asyncapi-to-ballerina/client").toAbsolutePath();
    private Path tmpDir;

    @BeforeClass
    public void setup() throws IOException {
        this.tmpDir = Files.createTempDirectory("asyncapi-to-ballerina-test-out-" + System.nanoTime());
    }

    @Test(description = "Test the generation of the client with a close frame records")
    public void testGenerateClientWithCloseFrame() {
        String fileName = "graphql_over_websocket_asyncapi";
        Path filePath = RES_DIR.resolve(Paths.get("CloseFrame/" + fileName + ".yaml"));
        Path licenseFilePath = RES_DIR.resolve(Paths.get("CloseFrame/license.txt"));
        String[] args = {"--input", filePath.toString(), "-o", this.tmpDir.toString(), "--protocol", "ws"
                , "--license", licenseFilePath.toString()};
        AsyncApiCmd cmd = new AsyncApiCmd(tmpDir, false);
        new CommandLine(cmd).parseArgs(args);
        try {
            cmd.execute();
            TestUtils.compareGeneratedFiles(this.tmpDir.resolve("utils.bal"),
                    RES_DIR.resolve("baloutputs/CloseFrame/utils.bal"));
            TestUtils.compareGeneratedFiles(this.tmpDir.resolve("client.bal"),
                    RES_DIR.resolve("baloutputs/CloseFrame/client.bal"));
            TestUtils.compareGeneratedFiles(this.tmpDir.resolve("types.bal"),
                    RES_DIR.resolve("baloutputs/CloseFrame/types.bal"));
        } catch (BLauncherException | IOException e) {
            Assert.fail(e.getMessage());
        }
    }

}
