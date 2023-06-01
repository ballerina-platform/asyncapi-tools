package io.ballerina.asyncapi.generators.client;

import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25DocumentImpl;
import io.ballerina.asyncapi.core.GeneratorUtils;
import io.ballerina.asyncapi.core.exception.BallerinaAsyncApiException;
import io.ballerina.asyncapi.core.generators.client.IntermediateClientGenerator;
import io.ballerina.asyncapi.core.generators.client.model.AASClientConfig;
import io.ballerina.asyncapi.generators.common.TestUtils;
import io.ballerina.compiler.syntax.tree.ChildNodeEntry;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.tools.diagnostics.Diagnostic;
import org.ballerinalang.formatter.core.FormatterException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.ballerina.asyncapi.generators.common.TestUtils.getDiagnostics;

/**
 * Test util file generation for ballerina connectors.
 */
public class UtilGenerationTests {
    private static final Path RESDIR =
            Paths.get("src/test/resources/generators/client/utils").toAbsolutePath();
    private static final String CREATE_FORM_URLENCODED_REQUEST_BODY = "createFormURLEncodedRequestBody";
    private static final String GET_DEEP_OBJECT_STYLE_REQUEST = "getDeepObjectStyleRequest";
    private static final String GET_FORM_STYLE_REQUEST = "getFormStyleRequest";
    private static final String GET_SERIALIZED_ARRAY = "getSerializedArray";
    private static final String GET_ENCODED_URI = "getEncodedUri";
    private static final String GET_ORIGINAL_KEY = "getOriginalKey";
    private static final String GET_PATH_FOR_QUERY_PARAM = "getPathForQueryParam";
    private static final String GET_MAP_FOR_HEADERS = "getMapForHeaders";
    private static final String GET_SERIALIZED_RECORD_ARRAY = "getSerializedRecordArray";
    List<String> list1 = new ArrayList<>();
    List<String> list2 = new ArrayList<>();

    @Test(description = "Test default util file generation")
    public void testDefaultUtilFileGen() throws IOException, BallerinaAsyncApiException {
        Path definitionPath = RESDIR.resolve("swagger/no_util.yaml");
        AsyncApi25DocumentImpl openAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        AASClientConfig.Builder clientMetaDataBuilder = new AASClientConfig.Builder();
        AASClientConfig oasClientConfig = clientMetaDataBuilder
//                .withFilters(filter)
                .withAsyncAPI(openAPI).build();
        IntermediateClientGenerator intermediateClientGenerator = new IntermediateClientGenerator(oasClientConfig);
        SyntaxTree clientSyntaxTree = intermediateClientGenerator.generateSyntaxTree();
        SyntaxTree utlisSyntaxTree = intermediateClientGenerator.getBallerinaUtilGenerator().generateUtilSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreewithExpectedSyntaxTree("client/ballerina/default_util.bal",
                utlisSyntaxTree);
    }

    @Test(description = "Validate the util functions generated for OpenAPI definition with query parameters")
    public void testUtilFileGenForQueryParams() throws IOException, BallerinaAsyncApiException,
            FormatterException {
        Path definitionPath = RESDIR.resolve("swagger/query_param.yaml");
        AsyncApi25DocumentImpl openAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        AASClientConfig.Builder clientMetaDataBuilder = new AASClientConfig.Builder();
        AASClientConfig oasClientConfig = clientMetaDataBuilder

                .withAsyncAPI(openAPI).build();
        IntermediateClientGenerator intermediateClientGenerator = new IntermediateClientGenerator(oasClientConfig);
        SyntaxTree clientSyntaxTree = intermediateClientGenerator.generateSyntaxTree();
        List<String> invalidFunctionNames = Arrays.asList(CREATE_FORM_URLENCODED_REQUEST_BODY, GET_MAP_FOR_HEADERS);
        Assert.assertTrue(checkUtil(invalidFunctionNames,
                intermediateClientGenerator.getBallerinaUtilGenerator().generateUtilSyntaxTree()));
        List<Diagnostic> diagnostics = getDiagnostics(clientSyntaxTree, openAPI, intermediateClientGenerator);
        Assert.assertTrue(diagnostics.isEmpty());
    }

    @Test(description = "Validate the util functions generated for OpenAPI definition with headers")
    public void testUtilFileGenForHeader() throws IOException, BallerinaAsyncApiException,
            FormatterException {
        Path definitionPath = RESDIR.resolve("swagger/header.yaml");
        AsyncApi25DocumentImpl openAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        AASClientConfig.Builder clientMetaDataBuilder = new AASClientConfig.Builder();
        AASClientConfig oasClientConfig = clientMetaDataBuilder
                .withAsyncAPI(openAPI).build();
        IntermediateClientGenerator intermediateClientGenerator = new IntermediateClientGenerator(oasClientConfig);
        SyntaxTree clientSyntaxTree = intermediateClientGenerator.generateSyntaxTree();
        List<String> invalidFunctionNames = Arrays.asList(CREATE_FORM_URLENCODED_REQUEST_BODY,
                GET_DEEP_OBJECT_STYLE_REQUEST, GET_FORM_STYLE_REQUEST, GET_SERIALIZED_ARRAY,
                GET_ORIGINAL_KEY, GET_PATH_FOR_QUERY_PARAM, GET_SERIALIZED_RECORD_ARRAY);
        Assert.assertTrue(checkUtil(invalidFunctionNames,
                intermediateClientGenerator.getBallerinaUtilGenerator().generateUtilSyntaxTree()));
        List<Diagnostic> diagnostics = getDiagnostics(clientSyntaxTree, openAPI, intermediateClientGenerator);
        Assert.assertTrue(diagnostics.isEmpty());
    }

    @Test(description = "Validate the util functions generated for OpenAPI definition with URL encoded request body")
    public void testUtilFileGenURLEncodedRequestBody() throws IOException, BallerinaAsyncApiException,
            FormatterException {
        Path definitionPath = RESDIR.resolve("swagger/url_encoded.yaml");
        AsyncApi25DocumentImpl openAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        AASClientConfig.Builder clientMetaDataBuilder = new AASClientConfig.Builder();
        AASClientConfig oasClientConfig = clientMetaDataBuilder
                .withAsyncAPI(openAPI).build();
        IntermediateClientGenerator intermediateClientGenerator = new IntermediateClientGenerator(oasClientConfig);
        SyntaxTree clientSyntaxTree = intermediateClientGenerator.generateSyntaxTree();
        List<String> invalidFunctionNames = Arrays.asList(GET_PATH_FOR_QUERY_PARAM, GET_MAP_FOR_HEADERS);
        Assert.assertTrue(checkUtil(invalidFunctionNames,
                intermediateClientGenerator.getBallerinaUtilGenerator().generateUtilSyntaxTree()));
        List<Diagnostic> diagnostics = getDiagnostics(clientSyntaxTree, openAPI, intermediateClientGenerator);
        Assert.assertTrue(diagnostics.isEmpty());
    }

    @Test(description = "Validate the util functions generated for OpenAPI definition with URL encoded " +
            "request body with encoding styles specified")
    public void testUtilFileGenURLEncodedRequestWithEncoding() throws IOException, BallerinaAsyncApiException,
            FormatterException {
        Path definitionPath = RESDIR.resolve("swagger/url_encoded_with_map.yaml");
        AsyncApi25DocumentImpl openAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        AASClientConfig.Builder clientMetaDataBuilder = new AASClientConfig.Builder();
        AASClientConfig oasClientConfig = clientMetaDataBuilder
                .withAsyncAPI(openAPI).build();
        IntermediateClientGenerator intermediateClientGenerator = new IntermediateClientGenerator(oasClientConfig);
        SyntaxTree clientSyntaxTree = intermediateClientGenerator.generateSyntaxTree();
        List<String> invalidFunctionNames = Arrays.asList(GET_PATH_FOR_QUERY_PARAM, GET_MAP_FOR_HEADERS);
        Assert.assertTrue(checkUtil(invalidFunctionNames,
                intermediateClientGenerator.getBallerinaUtilGenerator().generateUtilSyntaxTree()));
        List<Diagnostic> diagnostics = getDiagnostics(clientSyntaxTree, openAPI, intermediateClientGenerator);
        Assert.assertTrue(diagnostics.isEmpty());
    }

    @Test(description = "Validate the util functions generated for OpenAPI definition when all the scenarios are given")
    public void testCompleteUtilFileGen() throws IOException, BallerinaAsyncApiException,
            FormatterException {
        Path definitionPath = RESDIR.resolve("swagger/complete_util_gen.yaml");
        AsyncApi25DocumentImpl openAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        AASClientConfig.Builder clientMetaDataBuilder = new AASClientConfig.Builder();
        AASClientConfig oasClientConfig = clientMetaDataBuilder
                .withAsyncAPI(openAPI).build();
        IntermediateClientGenerator intermediateClientGenerator = new IntermediateClientGenerator(oasClientConfig);
        SyntaxTree clientSyntaxTree = intermediateClientGenerator.generateSyntaxTree();
        List<Diagnostic> diagnostics = getDiagnostics(clientSyntaxTree, openAPI, intermediateClientGenerator);
        Assert.assertTrue(diagnostics.isEmpty());
    }

    @Test(description = "Test the utilsbal file generation when only in:query api-key auth given")
    public void testApiKeyauthUtilGen() throws IOException, BallerinaAsyncApiException,
            FormatterException {
        Path definitionPath = RESDIR.resolve("swagger/apikey_with_no_query_param.yaml");
        AsyncApi25DocumentImpl openAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        AASClientConfig.Builder clientMetaDataBuilder = new AASClientConfig.Builder();
        AASClientConfig oasClientConfig = clientMetaDataBuilder
                .withAsyncAPI(openAPI).build();
        IntermediateClientGenerator intermediateClientGenerator = new IntermediateClientGenerator(oasClientConfig);
        SyntaxTree clientSyntaxTree = intermediateClientGenerator.generateSyntaxTree();
        List<Diagnostic> diagnostics = getDiagnostics(clientSyntaxTree, openAPI, intermediateClientGenerator);
        Assert.assertTrue(diagnostics.isEmpty());
    }

    @Test(description = "Validate the util functions generated for OpenAPI definition with multi part request bodies")
    public void testMultipartBodyParts() throws IOException, BallerinaAsyncApiException, FormatterException {
        Path definitionPath = RESDIR.resolve("swagger/multipart_formdata.yaml");
        AsyncApi25DocumentImpl openAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        AASClientConfig.Builder clientMetaDataBuilder = new AASClientConfig.Builder();
        AASClientConfig oasClientConfig = clientMetaDataBuilder
                .withAsyncAPI(openAPI).build();
        IntermediateClientGenerator intermediateClientGenerator = new IntermediateClientGenerator(oasClientConfig);
        SyntaxTree clientSyntaxTree = intermediateClientGenerator.generateSyntaxTree();
        List<Diagnostic> diagnostics = getDiagnostics(clientSyntaxTree, openAPI, intermediateClientGenerator);
        Assert.assertTrue(diagnostics.isEmpty());
    }

    @Test(description = "Validate the util functions generated for OpenAPI definition with multi part " +
            "request custom bodies")
    public void testMultipartCustomBodyParts() throws IOException, BallerinaAsyncApiException, FormatterException {
        Path definitionPath = RESDIR.resolve("swagger/multipart_formdata_custom.yaml");
        AsyncApi25DocumentImpl openAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        AASClientConfig.Builder clientMetaDataBuilder = new AASClientConfig.Builder();
        AASClientConfig oasClientConfig = clientMetaDataBuilder
                .withAsyncAPI(openAPI).build();
        IntermediateClientGenerator intermediateClientGenerator = new IntermediateClientGenerator(oasClientConfig);
        SyntaxTree clientSyntaxTree = intermediateClientGenerator.generateSyntaxTree();
        List<Diagnostic> diagnostics = getDiagnostics(clientSyntaxTree, openAPI, intermediateClientGenerator);
        Assert.assertTrue(diagnostics.isEmpty());
    }

    private boolean checkUtil(List<String> invalidFunctionNames, SyntaxTree utilSyntaxTree) {
        ModulePartNode modulePartNode = utilSyntaxTree.rootNode();
        NodeList<ModuleMemberDeclarationNode> members = modulePartNode.members();
        if (members.size() > 0) {
            for (ModuleMemberDeclarationNode node : members) {
                if (node.kind().equals(SyntaxKind.FUNCTION_DEFINITION)) {
                    for (ChildNodeEntry childNodeEntry : node.childEntries()) {
                        if (childNodeEntry.name().equals("functionName")) {
                            if (invalidFunctionNames.contains(childNodeEntry.node().get().toString())) {
                                return false;
                            }
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }

    @AfterClass
    public void cleanUp() throws IOException {
        TestUtils.deleteGeneratedFiles();
    }
}

