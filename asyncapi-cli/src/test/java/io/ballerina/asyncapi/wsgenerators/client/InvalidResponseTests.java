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
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import org.ballerinalang.formatter.core.FormatterException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Test invalid responses for given asyncapi specification.
 */
public class InvalidResponseTests {
    private static final Path RES_DIR = Paths.get(
            "src/test/resources/websockets/asyncapi-to-ballerina/client").toAbsolutePath();
    private SyntaxTree syntaxTree;

    @Test(description = "When response has array type payload",
            expectedExceptions = BallerinaAsyncApiExceptionWs.class,
            expectedExceptionsMessageRegExp = "Ballerina service file cannot be generate " +
                    "to the given AsyncAPI specification, Response type must be a Record")
    public void testPayloadTypeResponse() throws IOException, BallerinaAsyncApiExceptionWs {
        Path definitionPath = RES_DIR.resolve("InvalidResponse/payload_response.yaml");
//        Path expectedPathForTypes = RES_DIR.resolve("baloutputs/HeaderParam/header_parameter_types.bal");
//        Path expectedPathForClient = RES_DIR.resolve("baloutputs/InvalidResponse/map_type_response.bal");
//        Path expectedPathForUtils = RES_DIR.resolve("baloutputs/HeaderParam/header_parameter_utils.bal");
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        AasClientConfig.Builder clientMetaDataBuilder = new AasClientConfig.Builder();
        AasClientConfig oasClientConfig = clientMetaDataBuilder
                .withAsyncApi(asyncAPI).build();
        IntermediateClientGenerator intermediateClientGenerator = new IntermediateClientGenerator(oasClientConfig);
        intermediateClientGenerator.generateSyntaxTree();


//        compareGeneratedSyntaxTreeWithExpectedSyntaxTree(expectedPathForClient, syntaxTree);
//        compareGeneratedSyntaxTreeWithExpectedSyntaxTree(expectedPathForUtils, utilsSyntaxTree);
//        compareGeneratedSyntaxTreeWithExpectedSyntaxTree(expectedPathForTypes, schemaSyntaxTree);
    }

    @Test(description = "When response has array type payload",
            expectedExceptions = BallerinaAsyncApiExceptionWs.class,
            expectedExceptionsMessageRegExp = "Ballerina service file cannot be generate " +
                    "to the given AsyncAPI specification, Response type must be a Record")
    public void testOneOfTypeResponse() throws IOException, BallerinaAsyncApiExceptionWs {
        Path definitionPath = RES_DIR.resolve("InvalidResponse/one_of_response.yaml");
        Path expectedPathForTypes = RES_DIR.resolve("baloutputs/HeaderParam/header_parameter_types.bal");
        Path expectedPathForClient = RES_DIR.resolve("baloutputs/InvalidResponse/map_type_response.bal");
        Path expectedPathForUtils = RES_DIR.resolve("baloutputs/HeaderParam/header_parameter_utils.bal");
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        AasClientConfig.Builder clientMetaDataBuilder = new AasClientConfig.Builder();
        AasClientConfig oasClientConfig = clientMetaDataBuilder
                .withAsyncApi(asyncAPI).build();
        IntermediateClientGenerator intermediateClientGenerator = new IntermediateClientGenerator(oasClientConfig);
        intermediateClientGenerator.generateSyntaxTree();


//        compareGeneratedSyntaxTreeWithExpectedSyntaxTree(expectedPathForClient, syntaxTree);
//        compareGeneratedSyntaxTreeWithExpectedSyntaxTree(expectedPathForUtils, utilsSyntaxTree);
//        compareGeneratedSyntaxTreeWithExpectedSyntaxTree(expectedPathForTypes, schemaSyntaxTree);
    }

//    @Test(description = "When response has map type",
//            expectedExceptions = BallerinaAsyncApiException.class,
//            expectedExceptionsMessageRegExp = "Response type must be a record, .*")
//    public void testMapTypeResponse() throws IOException, BallerinaAsyncApiException {
//        Path definitionPath = RES_DIR.resolve("InvalidResponse/map_type_response.yaml");
////        Path expectedPathForTypes = RES_DIR.resolve("baloutputs/HeaderParam/header_parameter_types.bal");
////        Path expectedPathForUtils = RES_DIR.resolve("baloutputs/HeaderParam/header_parameter_utils.bal");
//        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
//        AASClientConfig.Builder clientMetaDataBuilder = new AASClientConfig.Builder();
//        AASClientConfig oasClientConfig = clientMetaDataBuilder
//                .withAsyncAPI(asyncAPI).build();
//        IntermediateClientGenerator intermediateClientGenerator = new IntermediateClientGenerator(oasClientConfig);
//        intermediateClientGenerator.generateSyntaxTree();
//
////        compareGeneratedSyntaxTreeWithExpectedSyntaxTree(expectedPathForClient, syntaxTree);
////        compareGeneratedSyntaxTreeWithExpectedSyntaxTree(expectedPathForUtils, utilsSyntaxTree);
////        compareGeneratedSyntaxTreeWithExpectedSyntaxTree(expectedPathForTypes, schemaSyntaxTree);
//    }

    @Test(description = "Test primitive type responses", expectedExceptions = BallerinaAsyncApiExceptionWs.class,
            expectedExceptionsMessageRegExp = "Response type must be a record, .*",
            dataProvider = "fileProviderForFilesComparison")
    public void testPrimitiveTypeResponses(String yamlFile) throws IOException,
            BallerinaAsyncApiExceptionWs, FormatterException, URISyntaxException {
        Path definitionPath = RES_DIR.resolve("InvalidResponse/" + yamlFile);
//        Path expectedPath = RES_DIR.resolve(" baloutputs/InvalidResponse/" + expectedFile);
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        AasClientConfig.Builder clientMetaDataBuilder = new AasClientConfig.Builder();
        AasClientConfig oasClientConfig = clientMetaDataBuilder
                .withAsyncApi(asyncAPI).build();
        IntermediateClientGenerator ballerinaClientGenerator = new IntermediateClientGenerator(oasClientConfig);
        ballerinaClientGenerator.generateSyntaxTree();
//        List<Diagnostic> diagnostics = getDiagnostics(syntaxTree, asyncAPI, ballerinaClientGenerator);
//        Assert.assertTrue(diagnostics.isEmpty());
//        compareGeneratedSyntaxTreeWithExpectedSyntaxTree(expectedPath, syntaxTree);
    }

    @DataProvider(name = "fileProviderForFilesComparison")
    public Object[][] fileProviderForFilesComparison() {
        return new Object[][]{
                {"float_type_response.yaml"},
                {"boolean_type_response.yaml"},
                {"string_type_response.yaml"},
                {"array_type_response.yaml"},
        };
    }
}
