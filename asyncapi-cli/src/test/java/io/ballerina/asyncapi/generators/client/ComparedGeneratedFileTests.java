/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package io.ballerina.asyncapi.generators.client;

import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25DocumentImpl;
import io.ballerina.asyncapi.core.GeneratorUtils;
import io.ballerina.asyncapi.core.exception.BallerinaAsyncApiException;
import io.ballerina.asyncapi.core.generators.client.IntermediateClientGenerator;
import io.ballerina.asyncapi.core.generators.client.model.AASClientConfig;
import io.ballerina.asyncapi.generators.common.TestUtils;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import org.ballerinalang.formatter.core.FormatterException;
import org.testng.annotations.AfterTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static io.ballerina.asyncapi.generators.common.TestUtils.compareGeneratedSyntaxTreeWithExpectedSyntaxTree;

/**
 * All the tests related to the {{@link io.ballerina.asyncapi.core.generators.client.IntermediateClientGenerator}}
 * util.
 */
public class ComparedGeneratedFileTests {
    private static final Path RES_DIR = Paths.get("src/test/resources/asyncapi-to-ballerina/client").toAbsolutePath();
    SyntaxTree syntaxTree;

    @Test(description = "Test openAPI definition to ballerina client source code generation",
            dataProvider = "fileProviderForFilesComparison")
    public void openApiToBallerinaCodeGenTestForClient(String yamlFile, String expectedFile) throws IOException,
            BallerinaAsyncApiException, FormatterException, URISyntaxException {
        Path definitionPath = RES_DIR.resolve("Real/" + yamlFile);
        Path expectedPath = RES_DIR.resolve("baloutputs/Real/" + expectedFile);
        AsyncApi25DocumentImpl openAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        AASClientConfig.Builder clientMetaDataBuilder = new AASClientConfig.Builder();
        AASClientConfig oasClientConfig = clientMetaDataBuilder
                .withAsyncAPI(openAPI).build();
        IntermediateClientGenerator ballerinaClientGenerator = new IntermediateClientGenerator(oasClientConfig);
        syntaxTree = ballerinaClientGenerator.generateSyntaxTree();
        compareGeneratedSyntaxTreeWithExpectedSyntaxTree(expectedPath, syntaxTree);
    }

    @DataProvider(name = "fileProviderForFilesComparison")
    public Object[][] fileProviderForFilesComparison() {
        return new Object[][]{
//                {"gemini_websocket.yaml", "gemini_websocket.bal"},
                {"kraken_websocket.yaml", "kraken_websocket.bal"}

        };
    }

    @AfterTest
    private void deleteGeneratedFiles() {
        try {
            TestUtils.deleteGeneratedFiles();
        } catch (IOException ignored) {
        }
    }
}
