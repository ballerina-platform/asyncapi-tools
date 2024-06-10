package io.ballerina.asyncapi.wsgenerators.client;

import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25DocumentImpl;
import io.ballerina.asyncapi.websocketscore.GeneratorUtils;
import io.ballerina.asyncapi.websocketscore.exception.BallerinaAsyncApiExceptionWs;
import io.ballerina.asyncapi.websocketscore.generators.client.IntermediateClientGenerator;
import io.ballerina.asyncapi.websocketscore.generators.client.model.AASClientConfig;
import io.ballerina.asyncapi.wsgenerators.common.TestUtils;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import org.ballerinalang.formatter.core.FormatterException;
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
        AASClientConfig.Builder clientMetaDataBuilder = new AASClientConfig.Builder();
        AASClientConfig oasClientConfig = clientMetaDataBuilder
//                .withFilters(filter)
                .withAsyncAPI(asyncAPI).build();
        IntermediateClientGenerator intermediateClientGenerator = new IntermediateClientGenerator(oasClientConfig);
        intermediateClientGenerator.generateSyntaxTree();
        SyntaxTree utlisSyntaxTree = intermediateClientGenerator.getBallerinaUtilGenerator().generateUtilSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreewithExpectedSyntaxTree(path,
                utlisSyntaxTree);
    }

    @Test(description = "Validate the util functions generated for AsyncAPI definition with query parameters")
    public void testUtilFileGenForQueryParams() throws IOException, BallerinaAsyncApiExceptionWs,
            FormatterException {
        Path definitionPath = RESDIR.resolve("Util/query_parameter.yaml");
        String expectedPath = "client/baloutputs/Util/query_parameter_utils.bal";
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        AASClientConfig.Builder clientMetaDataBuilder = new AASClientConfig.Builder();
        AASClientConfig oasClientConfig = clientMetaDataBuilder

                .withAsyncAPI(asyncAPI).build();
        IntermediateClientGenerator intermediateClientGenerator = new IntermediateClientGenerator(oasClientConfig);
        SyntaxTree clientSyntaxTree = intermediateClientGenerator.generateSyntaxTree();
//        List<String> invalidFunctionNames = Arrays.asList(CREATE_FORM_URLENCODED_REQUEST_BODY, GET_MAP_FOR_HEADERS);
//        Assert.assertTrue(checkUtil(invalidFunctionNames,
//                intermediateClientGenerator.getBallerinaUtilGenerator().generateUtilSyntaxTree()));
//        List<Diagnostic> diagnostics = getDiagnostics(clientSyntaxTree, asyncAPI, intermediateClientGenerator);
//        Assert.assertTrue(diagnostics.isEmpty());
        SyntaxTree utlisSyntaxTree = intermediateClientGenerator.getBallerinaUtilGenerator().generateUtilSyntaxTree();

        TestUtils.compareGeneratedSyntaxTreewithExpectedSyntaxTree(expectedPath,
                utlisSyntaxTree);
    }

    //
    @Test(description = "Validate the util functions generated for AsyncAPI definition with headers")
    public void testUtilFileGenForHeader() throws IOException, BallerinaAsyncApiExceptionWs,
            FormatterException {
        Path definitionPath = RESDIR.resolve("Util/header_parameter.yaml");
        String expectedPath = "client/baloutputs/Util/header_parameter_utils.bal";
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        AASClientConfig.Builder clientMetaDataBuilder = new AASClientConfig.Builder();
        AASClientConfig oasClientConfig = clientMetaDataBuilder
                .withAsyncAPI(asyncAPI).build();
        IntermediateClientGenerator intermediateClientGenerator = new IntermediateClientGenerator(oasClientConfig);
        SyntaxTree clientSyntaxTree = intermediateClientGenerator.generateSyntaxTree();
//        List<String> invalidFunctionNames = Arrays.asList(GET_PATH_FOR_QUERY_PARAM,
//                GET_ENCODED_URI,STREAM_GENERATOR);
//        Assert.assertTrue(checkUtil(invalidFunctionNames,
        SyntaxTree utlisSyntaxTree = intermediateClientGenerator.getBallerinaUtilGenerator().generateUtilSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreewithExpectedSyntaxTree(expectedPath,
                utlisSyntaxTree);
//                intermediateClientGenerator.getBallerinaUtilGenerator().generateUtilSyntaxTree()));
//        List<Diagnostic> diagnostics = getDiagnostics(clientSyntaxTree, asyncAPI, intermediateClientGenerator);
//        Assert.assertTrue(diagnostics.isEmpty());
    }

    //
    @Test(description = "Validate the util functions generated for asyncAPI definition with URL encoded request body")
    public void testUtilFileGenURLEncodedRequestBody() throws IOException, BallerinaAsyncApiExceptionWs,
            FormatterException {
        Path definitionPath = RESDIR.resolve("Util/path_param_url_encoded.yaml");
        String expectedPath = "client/baloutputs/Util/path_param_url_encoded.bal";
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        AASClientConfig.Builder clientMetaDataBuilder = new AASClientConfig.Builder();
        AASClientConfig oasClientConfig = clientMetaDataBuilder
                .withAsyncAPI(asyncAPI).build();
        IntermediateClientGenerator intermediateClientGenerator = new IntermediateClientGenerator(oasClientConfig);
        SyntaxTree clientSyntaxTree = intermediateClientGenerator.generateSyntaxTree();
        SyntaxTree utlisSyntaxTree = intermediateClientGenerator.getBallerinaUtilGenerator().generateUtilSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreewithExpectedSyntaxTree(expectedPath,
                utlisSyntaxTree);
//        List<Diagnostic> diagnostics = getDiagnostics(clientSyntaxTree, asyncAPI, intermediateClientGenerator);
//        Assert.assertTrue(diagnostics.isEmpty());
    }

    //
//
    @Test(description = "Validate the util functions generated for AsyncAPI definition" +
            " when all the scenarios are given")
    public void testCompleteUtilFileGen() throws IOException, BallerinaAsyncApiExceptionWs,
            FormatterException {
        Path definitionPath = RESDIR.resolve("Util/complete_util_gen.yaml");
        Path expectedPath = RESDIR.resolve("baloutputs/Util/complete_util_gen.bal");
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        AASClientConfig.Builder clientMetaDataBuilder = new AASClientConfig.Builder();
        AASClientConfig oasClientConfig = clientMetaDataBuilder
                .withAsyncAPI(asyncAPI).build();
        IntermediateClientGenerator intermediateClientGenerator = new IntermediateClientGenerator(oasClientConfig);
        SyntaxTree clientSyntaxTree = intermediateClientGenerator.generateSyntaxTree();
//        List<Diagnostic> diagnostics = getDiagnostics(clientSyntaxTree, asyncAPI, intermediateClientGenerator);
//        Assert.assertTrue(diagnostics.isEmpty());
        SyntaxTree utlisSyntaxTree = intermediateClientGenerator.getBallerinaUtilGenerator().generateUtilSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreewithExpectedSyntaxTree(expectedPath.toString(),
                utlisSyntaxTree);
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
//

//

    @AfterClass
    public void cleanUp() throws IOException {
        TestUtils.deleteGeneratedFiles();
    }
}

