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

import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This test class for the resolve reference in the other module in ballerina package.
 */
public class ModuleReferenceTests {
    private static final Path RES_DIR = Paths.get(
            "src/test/resources/websockets/ballerina-to-asyncapi/ballerina-project/service").toAbsolutePath();

    //re-enable after issue #6583 is fixed
    @Test(enabled = false, description = "Response with separate modules")
    public void testResponse01() throws IOException {
       Path ballerinaFilePath = RES_DIR.resolve("snowpeak.bal");
        TestUtils.compareWithGeneratedFile(ballerinaFilePath, "ballerina-project/service/snowpeak.yaml");
    }


    // re-enable after issue #6583 is fixed
    @Test(enabled = false, description = "Response has array type the array item type in separate module")
    public void testResponse02() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("arrayTypeResponse.bal");
        TestUtils.compareWithGeneratedFile(ballerinaFilePath,
                "ballerina-project/service/arrayTypeResponse.yaml");
    }

    //re-enable after issue #6583 is fixed
    @Test(enabled = false, description =  "check readonly")
    public void testRecordReferenceWithReadOnly() throws IOException {
        Path ballerinaFilePath = RES_DIR.resolve("readonly.bal");
        TestUtils.compareWithGeneratedFile(ballerinaFilePath, "ballerina-project/service/readonly.yaml");
    }
}
