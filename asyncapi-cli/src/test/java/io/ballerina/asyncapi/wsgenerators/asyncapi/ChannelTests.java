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
package io.ballerina.asyncapi.wsgenerators.asyncapi;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static io.ballerina.asyncapi.wsgenerators.asyncapi.TestUtils.compareWithGeneratedFile;

/**
 * This {@code ChannelTests} is for the covering the Channel tests for ballerina to asyncapi generation.
 */
public class ChannelTests {

    private static final Path RES_DIR = Paths.get("src/test/resources/websockets" +
            "/ballerina-to-asyncapi/channels").toAbsolutePath();
    private Path tempDir;

    @BeforeMethod
    public void setup() throws IOException {
        this.tempDir = Files.createTempDirectory("bal-to-asyncapi-test-out-" + System.nanoTime());
    }

    @Test(description = "Generate AsyncAPI spec for resource has .")
    public void testChannelScenario01() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("channel_scenario01.bal");
        compareWithGeneratedFile(ballerinaFilePath, "channels/channel_scenario01.yaml");
    }

    @Test(description = "Generate AsyncAPI spec for resource has path param and query param")
    public void testChannelScenario02() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("channel_scenario02.bal");
        compareWithGeneratedFile(ballerinaFilePath, "channels/channel_scenario02.yaml");
    }

    @Test(description = "Generate AsyncAPI spec with multipath including .")
    public void testChannelScenario03() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("channel_scenario03.bal");
        compareWithGeneratedFile(ballerinaFilePath, "channels/channel_scenario03.yaml");
    }

    @Test(description = "Generate AsyncAPI spec with path parameter including .")
    public void testChannelScenario04() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("channel_scenario04.bal");
        compareWithGeneratedFile(ballerinaFilePath, "channels/channel_scenario04.yaml");
    }
}
