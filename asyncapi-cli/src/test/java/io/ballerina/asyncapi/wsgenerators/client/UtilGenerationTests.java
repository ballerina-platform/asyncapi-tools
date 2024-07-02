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
package io.ballerina.asyncapi.wsgenerators.client;

import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25DocumentImpl;
import io.ballerina.asyncapi.websocketscore.GeneratorUtils;
import io.ballerina.asyncapi.websocketscore.exception.BallerinaAsyncApiExceptionWs;
import io.ballerina.asyncapi.websocketscore.generators.client.IntermediateClientGenerator;
import io.ballerina.asyncapi.websocketscore.generators.client.model.AasClientConfig;
import io.ballerina.asyncapi.wsgenerators.common.TestUtils;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Test util file generation for ballerina connectors.
 */
public class UtilGenerationTests {
    private static final Path RESDIR =
            Paths.get("src/test/resources/websockets/asyncapi-to-ballerina/client").toAbsolutePath();

    @Test(description = "Test default util file generation")
    public void testDefaultUtilFileGen() throws IOException, BallerinaAsyncApiExceptionWs {
        Path definitionPath = RESDIR.resolve("Util/default_util.yaml");
        Path expectedPath = RESDIR.resolve("baloutputs/Util/default_util.bal");
        String path = expectedPath.toString();
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        AasClientConfig.Builder clientMetaDataBuilder = new AasClientConfig.Builder();
        AasClientConfig oasClientConfig = clientMetaDataBuilder.withAsyncApi(asyncAPI).build();
        IntermediateClientGenerator intermediateClientGenerator = new IntermediateClientGenerator(oasClientConfig);
        intermediateClientGenerator.generateSyntaxTree();
        SyntaxTree utlisSyntaxTree = intermediateClientGenerator.getBallerinaUtilGenerator().generateUtilSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreeWithExpectedSyntaxTree(path, utlisSyntaxTree);
    }

    @Test(description = "Validate the util functions generated for AsyncAPI definition with query parameters")
    public void testUtilFileGenForQueryParams() throws IOException, BallerinaAsyncApiExceptionWs {
        Path definitionPath = RESDIR.resolve("Util/query_parameter.yaml");
        String expectedPath = "client/baloutputs/Util/query_parameter_utils.bal";
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        AasClientConfig.Builder clientMetaDataBuilder = new AasClientConfig.Builder();
        AasClientConfig oasClientConfig = clientMetaDataBuilder.withAsyncApi(asyncAPI).build();
        IntermediateClientGenerator intermediateClientGenerator = new IntermediateClientGenerator(oasClientConfig);
        SyntaxTree clientSyntaxTree = intermediateClientGenerator.generateSyntaxTree();
        SyntaxTree utlisSyntaxTree = intermediateClientGenerator.getBallerinaUtilGenerator().generateUtilSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreeWithExpectedSyntaxTree(expectedPath, utlisSyntaxTree);
    }

    @Test(description = "Validate the util functions generated for AsyncAPI definition with headers")
    public void testUtilFileGenForHeader() throws IOException, BallerinaAsyncApiExceptionWs {
        Path definitionPath = RESDIR.resolve("Util/header_parameter.yaml");
        String expectedPath = "client/baloutputs/Util/header_parameter_utils.bal";
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        AasClientConfig.Builder clientMetaDataBuilder = new AasClientConfig.Builder();
        AasClientConfig oasClientConfig = clientMetaDataBuilder.withAsyncApi(asyncAPI).build();
        IntermediateClientGenerator intermediateClientGenerator = new IntermediateClientGenerator(oasClientConfig);
        SyntaxTree clientSyntaxTree = intermediateClientGenerator.generateSyntaxTree();
        SyntaxTree utlisSyntaxTree = intermediateClientGenerator.getBallerinaUtilGenerator().generateUtilSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreeWithExpectedSyntaxTree(expectedPath, utlisSyntaxTree);
    }

    @Test(description = "Validate the util functions generated for asyncAPI definition with URL encoded request body")
    public void testUtilFileGenURLEncodedRequestBody() throws IOException, BallerinaAsyncApiExceptionWs {
        Path definitionPath = RESDIR.resolve("Util/path_param_url_encoded.yaml");
        String expectedPath = "client/baloutputs/Util/path_param_url_encoded.bal";
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        AasClientConfig.Builder clientMetaDataBuilder = new AasClientConfig.Builder();
        AasClientConfig oasClientConfig = clientMetaDataBuilder.withAsyncApi(asyncAPI).build();
        IntermediateClientGenerator intermediateClientGenerator = new IntermediateClientGenerator(oasClientConfig);
        SyntaxTree clientSyntaxTree = intermediateClientGenerator.generateSyntaxTree();
        SyntaxTree utlisSyntaxTree = intermediateClientGenerator.getBallerinaUtilGenerator().generateUtilSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreeWithExpectedSyntaxTree(expectedPath, utlisSyntaxTree);
    }

    @Test(description = "Validate the util functions generated for AsyncAPI definition" +
            " when all the scenarios are given")
    public void testCompleteUtilFileGen() throws IOException, BallerinaAsyncApiExceptionWs {
        Path definitionPath = RESDIR.resolve("Util/complete_util_gen.yaml");
        Path expectedPath = RESDIR.resolve("baloutputs/Util/complete_util_gen.bal");
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        AasClientConfig.Builder clientMetaDataBuilder = new AasClientConfig.Builder();
        AasClientConfig oasClientConfig = clientMetaDataBuilder.withAsyncApi(asyncAPI).build();
        IntermediateClientGenerator intermediateClientGenerator = new IntermediateClientGenerator(oasClientConfig);
        SyntaxTree clientSyntaxTree = intermediateClientGenerator.generateSyntaxTree();
        SyntaxTree utlisSyntaxTree = intermediateClientGenerator.getBallerinaUtilGenerator().generateUtilSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreeWithExpectedSyntaxTree(expectedPath.toString(), utlisSyntaxTree);
    }
//
//    @Test(description = "Test the utilsbal file generation when only in:query api-key auth given")
//    public void testApiKeyauthUtilGen() throws IOException, BallerinaAsyncApiException,
//            FormatterException {
//        Path definitionPath = RESDIR.resolve("swagger/apikey_with_no_query_param.yaml");
//        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
//        AASClientConfig.Builder clientMetaDataBuilder = new AASClientConfig.Builder();
//        AASClientConfig oasClientConfig = clientMetaDataBuilder
//                .withAsyncAPI(asyncAPI).build();
//        IntermediateClientGenerator intermediateClientGenerator = new IntermediateClientGenerator(oasClientConfig);
//        SyntaxTree clientSyntaxTree = intermediateClientGenerator.generateSyntaxTree();
//        List<Diagnostic> diagnostics = getDiagnostics(clientSyntaxTree, asyncAPI, intermediateClientGenerator);
//        Assert.assertTrue(diagnostics.isEmpty());
//    }
//

    @AfterClass
    public void cleanUp() throws IOException {
        TestUtils.deleteGeneratedFiles();
    }
}

