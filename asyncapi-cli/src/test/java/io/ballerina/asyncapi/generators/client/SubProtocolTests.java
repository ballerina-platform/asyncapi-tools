package io.ballerina.asyncapi.generators.client;

import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25DocumentImpl;
import io.ballerina.asyncapi.core.GeneratorUtils;
import io.ballerina.asyncapi.core.exception.BallerinaAsyncApiException;
import io.ballerina.asyncapi.core.generators.client.IntermediateClientGenerator;
import io.ballerina.asyncapi.core.generators.client.model.AASClientConfig;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static io.ballerina.asyncapi.generators.common.TestUtils.compareGeneratedSyntaxTreeWithExpectedSyntaxTree;

public class SubProtocolTests {
    private static final Path RES_DIR = Paths.get("src/test/resources/asyncapi-to-ballerina/client").toAbsolutePath();
    private SyntaxTree syntaxTree;

    @Test(description = "Test client generation of graphql over websocket sub protocol ")
    public void testGraphQlOverWebsocket() throws IOException, BallerinaAsyncApiException {
        Path definitionPath = RES_DIR.resolve("SubProtocols/graphqloverwebsocket.yaml");
//        Path expectedPathForTypes = RES_DIR.resolve("baloutputs/HeaderParam/header_parameter_types.bal");
        Path expectedPathForClient = RES_DIR.resolve("baloutputs/SubProtocols/graphqloverwebsocket.bal");
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
}
