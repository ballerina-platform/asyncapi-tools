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

package io.ballerina.asyncapi.generators.schema;

import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25DocumentImpl;
import io.ballerina.asyncapi.core.GeneratorUtils;
import io.ballerina.asyncapi.core.exception.BallerinaAsyncApiException;
import io.ballerina.asyncapi.core.generators.schema.BallerinaTypesGenerator;
import io.ballerina.asyncapi.generators.common.TestUtils;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Test implementation to verify the `oneOf` property related scenarios in asyncAPI schema generation, handled by
 * the {@link BallerinaTypesGenerator}.
 */
public class OneOfDataTypeTests {

    private static final Path RES_DIR = Paths.get("src/test/resources/asyncapi-to-ballerina/schema")
            .toAbsolutePath();

    @Test(description = "Generate record for schema has two references for oneOf")
    public void generateForSchemaHasOneOf() throws IOException, BallerinaAsyncApiException {
        Path definitionPath = RES_DIR.resolve("OneOf/twoOneOf.yaml");
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(asyncAPI);
        SyntaxTree syntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreewithExpectedSyntaxTree(
                "schema/baloutputs/OneOf/twoOneOf.bal", syntaxTree);
    }

    @Test(description = "Generate record for schema has object type with OneOf")
    public void generateForSchemaObjectType() throws IOException, BallerinaAsyncApiException {
        Path definitionPath = RES_DIR.resolve("OneOf/twoOneOfWithObjectType.yaml");
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(asyncAPI);
        SyntaxTree syntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreewithExpectedSyntaxTree(
                "schema/baloutputs/OneOf/twoOneOfWithObjectType.bal", syntaxTree);
    }


    @Test(description = "Tests full schema generations with oneOf type")
    public void generateOneOFTests() throws IOException, BallerinaAsyncApiException {
        Path definitionPath = RES_DIR.resolve("OneOf/oneOfAsProperties.yaml");
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(asyncAPI);
        SyntaxTree syntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreewithExpectedSyntaxTree("schema/baloutputs/OneOf/" +
                "oneOfAsProperties.bal", syntaxTree);
    }

    @Test(description = "Tests record generation for oneOf schemas with inline object schemas")
    public void oneOfWithInlineObject() throws IOException, BallerinaAsyncApiException {
        Path definitionPath = RES_DIR.resolve("OneOf/oneOfWithInlineSchemas.yaml");
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(asyncAPI);
        SyntaxTree syntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreewithExpectedSyntaxTree(
                "schema/baloutputs/OneOf/oneOfWithInlineSchemas.bal", syntaxTree);
    }

    @Test(description = "Tests record generation for nested OneOf schema inside AllOf schema")
    public void oneOfWithNestedAllOf() throws IOException, BallerinaAsyncApiException {
        Path definitionPath = RES_DIR.resolve("OneOf/nestedOneOfWithAllOf.yaml");
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(asyncAPI);
        SyntaxTree syntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreewithExpectedSyntaxTree(
                "schema/baloutputs/OneOf/nestedOneOfWithAllOf.bal", syntaxTree);
    }
}
