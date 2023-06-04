package io.ballerina.asyncapi.generators.client;

import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25DocumentImpl;
import io.ballerina.asyncapi.core.GeneratorUtils;
import io.ballerina.asyncapi.core.exception.BallerinaAsyncApiException;
import io.ballerina.asyncapi.core.generators.client.IntermediateClientGenerator;
import io.ballerina.asyncapi.core.generators.client.model.AASClientConfig;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static io.ballerina.asyncapi.core.GeneratorUtils.getValidName;
import static io.ballerina.asyncapi.generators.common.TestUtils.compareGeneratedSyntaxTreeWithExpectedSyntaxTree;

public class RemoteFunctionTests {
    private static final Path RES_DIR = Paths.get("src/test/resources/asyncapi-to-ballerina/client").toAbsolutePath();
    private SyntaxTree syntaxTree;

    @Test(description = "Test for stream request with no dispatcherStreamId")
    public void testRemoteFunctionDescription() throws IOException, BallerinaAsyncApiException {
        Path definitionPath = RES_DIR.resolve("RemoteFunction/remote_function_description.yaml");
//        Path expectedPathForTypes = RES_DIR.resolve("baloutputs/HeaderParam/header_parameter_types.bal");
        Path expectedPathForClient = RES_DIR.resolve("baloutputs/RemoteFunction/remote_function_description.bal");
//        Path expectedPathForUtils = RES_DIR.resolve("baloutputs/HeaderParam/header_parameter_utils.bal");
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        AASClientConfig.Builder clientMetaDataBuilder = new AASClientConfig.Builder();
        AASClientConfig oasClientConfig = clientMetaDataBuilder
                .withAsyncAPI(asyncAPI).build();
        IntermediateClientGenerator intermediateClientGenerator = new IntermediateClientGenerator(oasClientConfig);
        syntaxTree = intermediateClientGenerator.generateSyntaxTree();

        compareGeneratedSyntaxTreeWithExpectedSyntaxTree(expectedPathForClient, syntaxTree);
//        compareGeneratedSyntaxTreeWithExpectedSyntaxTree(expectedPathForUtils, utilsSyntaxTree);
//        compareGeneratedSyntaxTreeWithExpectedSyntaxTree(expectedPathForTypes, schemaSyntaxTree);
    }

    @Test(description = "Check whether the formatted function name are meeting ballerina coding conventions",
            dataProvider = "sampleProvider")
    public void testFunctionNameGeneration(String requestName, String expectedFunctionName) {
        String generatedFunctionName = getValidName(requestName, false);
        Assert.assertEquals(generatedFunctionName, expectedFunctionName);
    }

    @DataProvider(name = "sampleProvider")
    public Object[][] dataProvider() {
        return new Object[][]{
                {"get-pet-name", "getPetName"},
                {"GET_Add_permission", "gET_Add_permission"},
                {"ListBankAccount", "listBankAccount"},
                {"chat.media.download", "chatMediaDownload"}
        };
    }
}
