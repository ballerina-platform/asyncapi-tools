package io.ballerina.asyncapi.generators.client;

import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25DocumentImpl;
import io.ballerina.asyncapi.core.GeneratorUtils;
import io.ballerina.asyncapi.core.exception.BallerinaAsyncApiException;
import io.ballerina.asyncapi.core.generators.client.IntermediateClientGenerator;
import io.ballerina.asyncapi.core.generators.client.model.AASClientConfig;
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
            "src/test/resources/asyncapi-to-ballerina/client").toAbsolutePath();
    private SyntaxTree syntaxTree;

    @Test(description = "When response has array type payload",
            expectedExceptions = BallerinaAsyncApiException.class,
            expectedExceptionsMessageRegExp = "Ballerina service file cannot be generate " +
                    "to the given AsyncAPI specification, Response type must be a Record")
    public void testPayloadTypeResponse() throws IOException, BallerinaAsyncApiException {
        Path definitionPath = RES_DIR.resolve("InvalidResponse/payload_response.yaml");
//        Path expectedPathForTypes = RES_DIR.resolve("baloutputs/HeaderParam/header_parameter_types.bal");
//        Path expectedPathForClient = RES_DIR.resolve("baloutputs/InvalidResponse/map_type_response.bal");
//        Path expectedPathForUtils = RES_DIR.resolve("baloutputs/HeaderParam/header_parameter_utils.bal");
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        AASClientConfig.Builder clientMetaDataBuilder = new AASClientConfig.Builder();
        AASClientConfig oasClientConfig = clientMetaDataBuilder
                .withAsyncAPI(asyncAPI).build();
        IntermediateClientGenerator intermediateClientGenerator = new IntermediateClientGenerator(oasClientConfig);
        intermediateClientGenerator.generateSyntaxTree();


//        compareGeneratedSyntaxTreeWithExpectedSyntaxTree(expectedPathForClient, syntaxTree);
//        compareGeneratedSyntaxTreeWithExpectedSyntaxTree(expectedPathForUtils, utilsSyntaxTree);
//        compareGeneratedSyntaxTreeWithExpectedSyntaxTree(expectedPathForTypes, schemaSyntaxTree);
    }

    @Test(description = "When response has array type payload",
            expectedExceptions = BallerinaAsyncApiException.class,
            expectedExceptionsMessageRegExp = "Ballerina service file cannot be generate " +
                    "to the given AsyncAPI specification, Response type must be a Record")
    public void testOneOfTypeResponse() throws IOException, BallerinaAsyncApiException {
        Path definitionPath = RES_DIR.resolve("InvalidResponse/one_of_response.yaml");
//        Path expectedPathForTypes = RES_DIR.resolve("baloutputs/HeaderParam/header_parameter_types.bal");
//        Path expectedPathForClient = RES_DIR.resolve("baloutputs/InvalidResponse/map_type_response.bal");
//        Path expectedPathForUtils = RES_DIR.resolve("baloutputs/HeaderParam/header_parameter_utils.bal");
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        AASClientConfig.Builder clientMetaDataBuilder = new AASClientConfig.Builder();
        AASClientConfig oasClientConfig = clientMetaDataBuilder
                .withAsyncAPI(asyncAPI).build();
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

    @Test(description = "Test primitive type responses", expectedExceptions = BallerinaAsyncApiException.class,
            expectedExceptionsMessageRegExp = "Response type must be a record, .*",
            dataProvider = "fileProviderForFilesComparison")
    public void testPrimitiveTypeResponses(String yamlFile) throws IOException,
            BallerinaAsyncApiException, FormatterException, URISyntaxException {
        Path definitionPath = RES_DIR.resolve("InvalidResponse/" + yamlFile);
//        Path expectedPath = RES_DIR.resolve(" baloutputs/InvalidResponse/" + expectedFile);
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        AASClientConfig.Builder clientMetaDataBuilder = new AASClientConfig.Builder();
        AASClientConfig oasClientConfig = clientMetaDataBuilder
                .withAsyncAPI(asyncAPI).build();
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
