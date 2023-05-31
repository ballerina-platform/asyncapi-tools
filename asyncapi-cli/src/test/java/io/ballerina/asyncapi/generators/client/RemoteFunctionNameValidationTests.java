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
import java.util.ArrayList;
import java.util.List;

import static io.ballerina.asyncapi.core.GeneratorUtils.getValidName;


/**
 * check whether the remote function names generated are matching with ballerina standard.
 */
public class RemoteFunctionNameValidationTests {
    private static final Path RESDIR =
            Paths.get("src/test/resources/generators/client/swagger").toAbsolutePath();
    SyntaxTree syntaxTree;
    List<String> list1 = new ArrayList<>();
    List<String> list2 = new ArrayList<>();

    @Test(description = "When path parameter has given unmatch data type in ballerina",
            expectedExceptions = BallerinaAsyncApiException.class,
            expectedExceptionsMessageRegExp = "OpenAPI definition has errors: " +
                    "\\ROperationId is missing in the resource path: .*")
    public void testMissionOperationId() throws IOException, BallerinaAsyncApiException{
        Path definitionPath = RESDIR.resolve("petstore_without_operation_id.yaml");
        AsyncApi25DocumentImpl openAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        AASClientConfig.Builder clientMetaDataBuilder = new AASClientConfig.Builder();
        AASClientConfig oasClientConfig = clientMetaDataBuilder
                .withAsyncAPI(openAPI).build();
        IntermediateClientGenerator intermediateClientGenerator = new IntermediateClientGenerator(oasClientConfig);
        intermediateClientGenerator.generateSyntaxTree();
    }

    @Test(description = "Check whether the formatted function namee are meeting ballerina coding conventions",
            dataProvider = "sampleProvider")
    public void testFunctionNameGeneration(String operationId, String expectedFunctionName) {
        String generatedFunctionName = getValidName(operationId, false);
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
