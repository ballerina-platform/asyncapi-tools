package io.ballerina.asyncapi.generators.client;

import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25DocumentImpl;
import io.ballerina.asyncapi.cli.BallerinaCodeGenerator;
import io.ballerina.asyncapi.core.GeneratorUtils;
import io.ballerina.asyncapi.core.exception.BallerinaAsyncApiException;
import io.ballerina.asyncapi.core.generators.client.IntermediateClientGenerator;
import io.ballerina.asyncapi.core.generators.client.model.AASClientConfig;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static io.ballerina.asyncapi.generators.common.TestUtils.compareGeneratedSyntaxTreeWithExpectedSyntaxTree;


/**
 * Test client generation when server url is not given in the async-api definition.
 */
public class NoServerURLTest {
    private static final Path RES_DIR = Paths.get("src/test/resources/asyncapi-to-ballerina/client").toAbsolutePath();
    private SyntaxTree syntaxTree;
    List<String> list1 = new ArrayList<>();
    List<String> list2 = new ArrayList<>();

    @Test(description = "Test for no server url with no security schema given")
    public void getClientForNoServerURL() throws IOException, BallerinaAsyncApiException {
        Path definitionPath = RES_DIR.resolve("NoServerUrl/missing_server_url.yaml");
        Path expectedPath = RES_DIR.resolve("baloutputs/NoServerUrl/missing_server_url.bal");

        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        AASClientConfig.Builder clientMetaDataBuilder = new AASClientConfig.Builder();
        AASClientConfig aasClientConfig = clientMetaDataBuilder
                .withAsyncAPI(asyncAPI).build();
        IntermediateClientGenerator intermediateClientGenerator = new IntermediateClientGenerator(aasClientConfig);
        syntaxTree = intermediateClientGenerator.generateSyntaxTree();
        compareGeneratedSyntaxTreeWithExpectedSyntaxTree(expectedPath, syntaxTree);
    }
//TODO: Uncomment after adding authentication mechanisms

//    @Test(description = "Test for no server url with HTTP authentication mechanism")
//    public void getClientForNoServerURLWithHTTPAuth() {
//        BallerinaAuthConfigGenerator ballerinaAuthConfigGenerator = new BallerinaAuthConfigGenerator(
//                false, true);
//        String expectedParams = TestConstants.WEBSOCKET_CLIENT_CONFIG_PARAM_NO_URL;
//        StringBuilder generatedParams = new StringBuilder();
//        List<Node> generatedInitParamNodes = ballerinaAuthConfigGenerator.getConfigParamForClassInit(
//                "/");
//        for (Node param: generatedInitParamNodes) {
//            generatedParams.append(param.toString());
//        }
//        expectedParams = (expectedParams.trim()).replaceAll("\\s+", "");
//        String generatedParamsStr = (generatedParams.toString().trim()).replaceAll("\\s+", "");
//        Assert.assertEquals(expectedParams, generatedParamsStr);
//    }
//
//    @Test(description = "Test for no server url with API key authentication mechanism")
//    public void getClientForNoServerURLWithAPIKeyAuth() {
//        BallerinaAuthConfigGenerator ballerinaAuthConfigGenerator = new BallerinaAuthConfigGenerator(
//                true, false);
//        String expectedParams = TestConstants.API_KEY_CONFIG_PARAM_NO_URL;
//        StringBuilder generatedParams = new StringBuilder();
//        List<Node> generatedInitParamNodes = ballerinaAuthConfigGenerator.getConfigParamForClassInit(
//                "/");
//        for (Node param: generatedInitParamNodes) {
//            generatedParams.append(param.toString());
//        }
//        expectedParams = (expectedParams.trim()).replaceAll("\\s+", "");
//        String generatedParamsStr = (generatedParams.toString().trim()).replaceAll("\\s+", "");
//        Assert.assertEquals(expectedParams, generatedParamsStr);
//    }
}
