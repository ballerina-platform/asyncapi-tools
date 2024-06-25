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
import io.ballerina.asyncapi.websocketscore.generators.client.model.AASClientConfig;
import io.ballerina.asyncapi.websocketscore.generators.schema.BallerinaTypesGenerator;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static io.ballerina.asyncapi.wsgenerators.common.TestUtils.compareGeneratedSyntaxTreeWithExpectedSyntaxTree;

/**
 * All the tests related to the Header sections.
 */
public class HeadersTests {
    private static final Path RES_DIR = Paths.get("src/test/resources/websockets" +
            "/asyncapi-to-ballerina/client").toAbsolutePath();
    private SyntaxTree syntaxTree;

    @Test(description = "Test for header that comes under the parameter section")
    public void getHeaderTests() throws IOException, BallerinaAsyncApiExceptionWs {
        Path definitionPath = RES_DIR.resolve("HeaderParam/header_parameter.yaml");
        Path expectedPathForTypes = RES_DIR.resolve("baloutputs/HeaderParam/header_parameter_types.bal");
        Path expectedPathForClient = RES_DIR.resolve("baloutputs/HeaderParam/header_parameter_client.bal");
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        AASClientConfig.Builder clientMetaDataBuilder = new AASClientConfig.Builder();
        AASClientConfig oasClientConfig = clientMetaDataBuilder
                .withAsyncAPI(asyncAPI).build();
        IntermediateClientGenerator intermediateClientGenerator = new IntermediateClientGenerator(oasClientConfig);
        syntaxTree = intermediateClientGenerator.generateSyntaxTree();
        List<TypeDefinitionNode> preGeneratedTypeDefNodes = new ArrayList<>(
                intermediateClientGenerator.getBallerinaAuthConfigGenerator().getAuthRelatedTypeDefinitionNodes());
        preGeneratedTypeDefNodes.addAll(intermediateClientGenerator.getTypeDefinitionNodeList());

        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(
                asyncAPI, preGeneratedTypeDefNodes);

        SyntaxTree schemaSyntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();

        compareGeneratedSyntaxTreeWithExpectedSyntaxTree(expectedPathForClient, syntaxTree);
        compareGeneratedSyntaxTreeWithExpectedSyntaxTree(expectedPathForTypes, schemaSyntaxTree);
    }

    //TODO: uncomment after add the capable of sending apikey as a header
//    @Test(description = "Test for header that comes under the parameter section")
//    public void getHeaderTestsWithoutParameter() throws IOException, BallerinaAsyncApiException {
//        Path definitionPath = RES_DIR.resolve("HeaderParam/header_without_parameter.yaml");
//        Path expectedPath = RES_DIR.resolve("baloutputs/HeaderParam/header_without_parameter.bal");
//        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
//        AASClientConfig.Builder clientMetaDataBuilder = new AASClientConfig.Builder();
//        AASClientConfig oasClientConfig = clientMetaDataBuilder
//                .withAsyncAPI(asyncAPI).build();
//        IntermediateClientGenerator intermediateClientGenerator = new IntermediateClientGenerator(oasClientConfig);
//        syntaxTree = intermediateClientGenerator.generateSyntaxTree();
//        compareGeneratedSyntaxTreeWithExpectedSyntaxTree(expectedPath, syntaxTree);
//    }


    @Test(description = "Test for header with default values")
    public void getHeaderTestsWithDefaultValues() throws IOException, BallerinaAsyncApiExceptionWs {
        Path definitionPath = RES_DIR.resolve("HeaderParam/header_param_with_default_value.yaml");
        Path expectedPath = RES_DIR.resolve("baloutputs/HeaderParam/header_param_with_default_value.bal");
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        AASClientConfig.Builder clientMetaDataBuilder = new AASClientConfig.Builder();
        AASClientConfig oasClientConfig = clientMetaDataBuilder
                .withAsyncAPI(asyncAPI).build();
        IntermediateClientGenerator intermediateClientGenerator = new IntermediateClientGenerator(oasClientConfig);
        syntaxTree = intermediateClientGenerator.generateSyntaxTree();
        List<TypeDefinitionNode> preGeneratedTypeDefNodes = new ArrayList<>(
                intermediateClientGenerator.getBallerinaAuthConfigGenerator().getAuthRelatedTypeDefinitionNodes());
        preGeneratedTypeDefNodes.addAll(intermediateClientGenerator.getTypeDefinitionNodeList());

        //Generate ballerina records to represent schemas in client intermediate code
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(
                asyncAPI, preGeneratedTypeDefNodes);

        SyntaxTree schemaSyntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();

        compareGeneratedSyntaxTreeWithExpectedSyntaxTree(expectedPath, schemaSyntaxTree);
    }

    @Test(description = "Test for optional headers without default values")
    public void getOptionalHeaderTestsWithoutDefaultValues() throws IOException, BallerinaAsyncApiExceptionWs {
        Path definitionPath = RES_DIR.resolve("HeaderParam/header_with_query.yaml");
        Path expectedPath = RES_DIR.resolve("baloutputs/HeaderParam/header_with_query.bal");
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        AASClientConfig.Builder clientMetaDataBuilder = new AASClientConfig.Builder();
        AASClientConfig oasClientConfig = clientMetaDataBuilder
                .withAsyncAPI(asyncAPI).build();
        IntermediateClientGenerator intermediateClientGenerator = new IntermediateClientGenerator(oasClientConfig);
        syntaxTree = intermediateClientGenerator.generateSyntaxTree();
        compareGeneratedSyntaxTreeWithExpectedSyntaxTree(expectedPath, syntaxTree);
    }
}
