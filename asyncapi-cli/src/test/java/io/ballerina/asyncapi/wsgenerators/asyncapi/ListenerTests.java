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

import io.ballerina.asyncapi.cmd.websockets.BallerinaToAsyncApiGenerator;
import io.ballerina.asyncapi.websocketscore.generators.asyncspec.diagnostic.AsyncApiConverterDiagnostic;
import io.ballerina.asyncapi.websocketscore.generators.asyncspec.service.AsyncApiEndpointMapper;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * This Test class for storing all the endpoint related tests
 * {@link AsyncApiEndpointMapper}.
 */
public class ListenerTests {
    private static final Path RES_DIR = Paths.get("src/test/resources/websockets" +
            "/ballerina-to-asyncapi/").toAbsolutePath();
    private Path tempDir;
    private static final PrintStream outStream = System.out;

    @BeforeMethod
    public void setup() throws IOException {
        this.tempDir = Files.createTempDirectory("bal-to-asyncapi-test-out-" + System.nanoTime());
    }
    //Listeners
    @Test(description = "Generate AsyncAPI spec for single listener")
    public void testListeners01() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("listeners/listener_scenario01.bal");
        TestUtils.compareWithGeneratedFile(ballerinaFilePath, "listeners/listener_scenario01.yaml");
    }

    @Test(description = "Generate AsyncAPI spec for listener only have port")
    public void testListeners02() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("listeners/listener_scenario02.bal");
        TestUtils.compareWithGeneratedFile(ballerinaFilePath, "listeners/listener_scenario02.yaml");
    }

    @Test(description = "Generate AsyncAPI spec for multiple listeners")
    public void testListeners03() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("listeners/listener_scenario03.bal");
        TestUtils.compareWithGeneratedFile(ballerinaFilePath, "listeners/listener_scenario03.yaml");
    }

    @Test(description = "Generate AsyncAPI spec for ExplicitNewExpressionNode listeners")
    public void testListeners04() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("listeners/listener_scenario04.bal");
        TestUtils.compareWithGeneratedFile(ballerinaFilePath, "listeners/listener_scenario04.yaml");
    }

    @Test(description = "Generate AsyncAPI spec for multiple listeners")
    public void testListeners05() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("listeners/listener_scenario05.bal");
        TestUtils.compareWithGeneratedFile(ballerinaFilePath, "listeners/listener_scenario05.yaml");
    }

    @Test(description = "When given ballerina file contain some compilation issue.")
    public void testListeners06() {
        Path ballerinaFilePath = RES_DIR.resolve("listeners/listener_scenario06.bal");
        List<AsyncApiConverterDiagnostic> errors = BallerinaToAsyncApiGenerator
                .generateAsyncAPIDefinitionsAllService(ballerinaFilePath, tempDir, null, false, outStream);
        Assert.assertTrue(errors.isEmpty());
    }

    @Test(description = "Generate AsyncAPI spec for http load balancer listeners")
    public void testListeners07() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("listeners/listener_http_load_balancer.bal");
        TestUtils.compareWithGeneratedFile(ballerinaFilePath, "listeners/with_check_key_word.yaml");
    }

    @AfterMethod
    public void cleanUp() {
        TestUtils.deleteDirectory(this.tempDir);
    }

    @AfterTest
    public void clean() {
        System.setErr(null);
        System.setOut(null);
    }
}
