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
package io.ballerina.asyncapi.wsgenerators.client;

import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25DocumentImpl;
import io.ballerina.asyncapi.cmd.websockets.AsyncAPIToBallerinaGenerator;
import io.ballerina.asyncapi.websocketscore.GeneratorUtils;
import io.ballerina.asyncapi.websocketscore.exception.BallerinaAsyncApiExceptionWs;
import io.ballerina.asyncapi.websocketscore.generators.client.IntermediateClientGenerator;
import io.ballerina.asyncapi.websocketscore.generators.client.model.AASClientConfig;
import io.ballerina.asyncapi.websocketscore.generators.schema.BallerinaTypesGenerator;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import org.ballerinalang.formatter.core.FormatterException;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static io.ballerina.asyncapi.wsgenerators.common.TestUtils.compareGeneratedSyntaxTreeWithExpectedSyntaxTree;

/**
 * This tests class for the tests Path parameters in asyncapi file.
 */
public class PathParameterTests {
    private static final Path RESDIR = Paths.get("src/test/resources/websockets" +
                    "/asyncapi-to-ballerina/client")
            .toAbsolutePath();
    private SyntaxTree syntaxTree;


    @Test(description = "Generate Client for path parameter has parameter name as key word - unit tests for method")
    public void generatePathWithPathParameterTests() throws IOException, BallerinaAsyncApiExceptionWs,
            FormatterException {
        // "/v1/v2"), "/v1/v2"
        // "/v1/{version}/v2/{name}", "/v1/${'version}/v2/${name}"
        // "/v1/{version}/v2/{limit}", "/v1/${'version}/v2/${'limit}"
        // "/v1/{age}/v2/{name}", "/v1/${age}/v2/${name}"

        AsyncAPIToBallerinaGenerator codeGenerator = new AsyncAPIToBallerinaGenerator();
//        Path definitionPath = RESDIR.resolve(RESDIR + "/swagger/path_parameter_valid.yaml");
//        Path expectedPath = RESDIR.resolve("ballerina/path_parameter_valid.bal");
//        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        AsyncAPIToBallerinaGenerator asyncAPIToBallerinaGenerator = new AsyncAPIToBallerinaGenerator();
//        asyncAPIToBallerinaGenerator.generateClient("src/test/resources/asyncapi-to-ballerina/client/PathParam" +
//                "/path_parameter_valid.yaml", "/Users/thushalya/Documents/out");
        asyncAPIToBallerinaGenerator.generateClient(
                "src/test/resources/websockets/asyncapi-to-ballerina/client/StreamResponse/" +
                        "multiple_stream_with_dispatcherStreamId.yaml",
                "src/test/resources/websockets/out");
//        AASClientConfig.Builder clientMetaDataBuilder = new AASClientConfig.Builder();
////        AASClientConfig oasClientConfig = clientMetaDataBuilder
//////                .withFilters(filter)
////                .withAsyncAPI(asyncAPI).build();
////                .withResourceMode(false).build();
//        IntermediateClientGenerator intermediateClientGenerator = new IntermediateClientGenerator(oasClientConfig);
//        syntaxTree = intermediateClientGenerator.generateSyntaxTree();
//        compareGeneratedSyntaxTreeWithExpectedSyntaxTree(expectedPath, syntaxTree);
    }

    //DOne
    @Test(description = "Generate Client for path parameter with referenced schema")
    public void generatePathParamWithReferencedSchema() throws IOException, BallerinaAsyncApiExceptionWs {
        Path definitionPath = RESDIR.resolve("PathParam/path_param_with_ref_schemas.yaml");
        Path expectedPath = RESDIR.resolve("baloutputs/PathParam/path_param_with_ref_schema.bal");
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        AASClientConfig.Builder clientMetaDataBuilder = new AASClientConfig.Builder();
        AASClientConfig oasClientConfig = clientMetaDataBuilder
                .withAsyncAPI(asyncAPI).build();
        IntermediateClientGenerator intermediateClientGenerator = new IntermediateClientGenerator(oasClientConfig);
        intermediateClientGenerator.generateSyntaxTree();

        List<TypeDefinitionNode> preGeneratedTypeDefNodes = new ArrayList<>(
                intermediateClientGenerator.getBallerinaAuthConfigGenerator().getAuthRelatedTypeDefinitionNodes());
        preGeneratedTypeDefNodes.addAll(intermediateClientGenerator.getTypeDefinitionNodeList());

        //Generate ballerina records to represent schemas in client intermediate code
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(
                asyncAPI, preGeneratedTypeDefNodes);

        SyntaxTree schemaSyntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();

        compareGeneratedSyntaxTreeWithExpectedSyntaxTree(expectedPath, schemaSyntaxTree);
    }

    //Done
    @Test(description = "Generate Client while handling special characters in path parameter name")
    public void generateFormattedPathParamName() throws IOException, BallerinaAsyncApiExceptionWs {
        Path definitionPath = RESDIR.resolve("PathParam/path_parameter_special_name.yaml");
        Path expectedPath = RESDIR.resolve("baloutputs/PathParam/path_parameter_with_special_name.bal");
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        AASClientConfig.Builder clientMetaDataBuilder = new AASClientConfig.Builder();
        AASClientConfig oasClientConfig = clientMetaDataBuilder
                .withAsyncAPI(asyncAPI).build();
        IntermediateClientGenerator intermediateClientGenerator = new IntermediateClientGenerator(oasClientConfig);
        syntaxTree = intermediateClientGenerator.generateSyntaxTree();
        compareGeneratedSyntaxTreeWithExpectedSyntaxTree(expectedPath, syntaxTree);
    }

    @Test(description = "Generate Client with duplicated path parameter name in the path")
    public void generateFormattedDuplicatedPathParamName() throws IOException, BallerinaAsyncApiExceptionWs {
        Path definitionPath = RESDIR.resolve("PathParam/path_param_duplicated_name.yaml");
        Path expectedPath = RESDIR.resolve("baloutputs/PathParam/path_param_duplicated_name.bal");
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        AASClientConfig.Builder clientMetaDataBuilder = new AASClientConfig.Builder();
        AASClientConfig oasClientConfig = clientMetaDataBuilder
                .withAsyncAPI(asyncAPI).build();
        IntermediateClientGenerator intermediateClientGenerator = new IntermediateClientGenerator(oasClientConfig);
        syntaxTree = intermediateClientGenerator.generateSyntaxTree();
        compareGeneratedSyntaxTreeWithExpectedSyntaxTree(expectedPath, syntaxTree);
    }

    @Test(description = "When path parameter has given unmatch data type in ballerina",
            expectedExceptions = BallerinaAsyncApiExceptionWs.class,
            expectedExceptionsMessageRegExp = "Ballerina doesn't support array type path parameters")
    public void testInvalidPathParameterType() throws IOException, BallerinaAsyncApiExceptionWs {
        Path definitionPath = RESDIR.resolve("PathParam/path_parameter_invalid.yaml");
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        AASClientConfig.Builder clientMetaDataBuilder = new AASClientConfig.Builder();
        AASClientConfig oasClientConfig = clientMetaDataBuilder
                .withAsyncAPI(asyncAPI).build();

        IntermediateClientGenerator intermediateClientGenerator = new IntermediateClientGenerator(oasClientConfig);
        intermediateClientGenerator.generateSyntaxTree();

    }

    @Test(description = "When given data type not match with ballerina data type",
            expectedExceptions = BallerinaAsyncApiExceptionWs.class,
            expectedExceptionsMessageRegExp = "Unsupported AsyncAPI data type .*")
    public void testInvalidDataType() throws IOException, BallerinaAsyncApiExceptionWs {
        Path definitionPath = RESDIR.resolve("PathParam/path_parameter_invalid02.yaml");
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        AASClientConfig.Builder clientMetaDataBuilder = new AASClientConfig.Builder();
        AASClientConfig oasClientConfig = clientMetaDataBuilder
                .withAsyncAPI(asyncAPI).build();
        IntermediateClientGenerator intermediateClientGenerator = new IntermediateClientGenerator(oasClientConfig);
        intermediateClientGenerator.generateSyntaxTree();
    }
}
