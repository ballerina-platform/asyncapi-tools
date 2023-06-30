/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.ballerina.asyncapi.cmd;

import io.ballerina.asyncapi.AsyncAPITest;
import io.ballerina.asyncapi.TestUtil;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import static io.ballerina.asyncapi.TestUtil.DISTRIBUTIONS_DIR;
import static io.ballerina.asyncapi.TestUtil.RESOURCES_PATH;

/**
 * Integration tests for client resource functions.
 */
public class ClientGenerationTests extends AsyncAPITest {
    public static final String DISTRIBUTION_FILE_NAME = DISTRIBUTIONS_DIR.toString();
    public static final Path TEST_RESOURCE = Paths.get(RESOURCES_PATH + "/client");

    @Test(description = "Client generation using cmd")
    public void clientWithResourceFunction() throws IOException, InterruptedException {
        String asyncAPIFilePath = TEST_RESOURCE.resolve("asyncapi.yaml").toString();
        List<String> buildArgs = new LinkedList<>();
        buildArgs.add("-i");
        buildArgs.add(asyncAPIFilePath);
        buildArgs.add("-o");
        buildArgs.add(tmpDir.toString());
        TestUtil.executeAsyncAPI(DISTRIBUTION_FILE_NAME, TEST_RESOURCE, buildArgs);
        Assert.assertTrue(Files.exists(Paths.get(tmpDir.toString()).resolve("client.bal")));
        Assert.assertTrue(Files.exists(Paths.get(tmpDir.toString()).resolve("types.bal")));
        Assert.assertTrue(Files.exists(Paths.get(tmpDir.toString()).resolve("utils.bal")));
    }

    @Test(description = "`--client-methods` option with service")
    public void serviceWithResourceFunction() throws IOException, InterruptedException {
        String asyncAPIFilePath = "asyncapi.yaml";
        List<String> buildArgs = new LinkedList<>();
        buildArgs.add("-i");
        buildArgs.add(asyncAPIFilePath);
        buildArgs.add("-o");
        buildArgs.add(tmpDir.toString());
        boolean successful = TestUtil.executeAsyncAPI(DISTRIBUTION_FILE_NAME, TEST_RESOURCE, buildArgs);
    }

    @Test(description = "`--client-methods` option without any mode")
    public void commonWithResourceFunction() throws IOException, InterruptedException {
        String asyncAPIFilePath = "asyncapi.yaml";
        List<String> buildArgs = new LinkedList<>();
        buildArgs.add("-i");
        buildArgs.add(asyncAPIFilePath);
        buildArgs.add("-o");
        buildArgs.add(tmpDir.toString());
        boolean successful = TestUtil.executeAsyncAPI(DISTRIBUTION_FILE_NAME, TEST_RESOURCE, buildArgs);
        Assert.assertTrue(Files.exists(Paths.get(tmpDir.toString()).resolve("client.bal")));
        Assert.assertTrue(Files.exists(Paths.get(tmpDir.toString()).resolve("openapi_service.bal")));
    }
}
