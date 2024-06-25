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

/**
 * This test class for optional returns.
 */
public class OptionalTests {
    private static final Path RES_DIR = Paths.get("src/test/resources/websockets" +
            "/ballerina-to-asyncapi").toAbsolutePath();
    private Path tempDir;

    @BeforeMethod
    public void setup() throws IOException {
        this.tempDir = Files.createTempDirectory("bal-to-asyncapi-test-out-" + System.nanoTime());
    }

    @Test(description = "When the remote method has an optional return")
    public void testIntReturnOptional() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("optional/optional_int_return.bal");
        TestUtils.compareWithGeneratedFile(ballerinaFilePath, "optional/optional_int_return.yaml");
    }

    @Test(description = "When the remote method has an optional return as error")
    public void testErrorReturnOptional() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("optional/optional_error_return.bal");
        TestUtils.compareWithGeneratedFile(ballerinaFilePath, "optional/optional_error_return.yaml");
    }

    @Test(description = "When the remote method has two union type returns with optional return")
    public void testTwoTypesReturnOptional() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("optional/optional_two_union_return.bal");
        TestUtils.compareWithGeneratedFile(ballerinaFilePath, "optional/optional_two_union_return.yaml");
    }

    //re-enable after issue #6583 is fixed
    @Test(enabled = false, description = "When the remote method has multiple return types including streaming " +
            "with optional return")
    public void testMultipleTypesStreamIncludeReturnOptional() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("optional/optional_multiple_type_return.bal");
        TestUtils.compareWithGeneratedFile(ballerinaFilePath, "optional/optional_multiple_type_return.yaml");
    }
}
