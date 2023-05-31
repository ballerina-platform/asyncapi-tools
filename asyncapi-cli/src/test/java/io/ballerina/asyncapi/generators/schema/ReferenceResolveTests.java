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
import io.ballerina.asyncapi.core.generators.client.FunctionSignatureGenerator;
import io.ballerina.asyncapi.core.generators.schema.BallerinaTypesGenerator;
import io.ballerina.asyncapi.generators.common.TestUtils;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests for Schema Reference resolve.
 */
public class ReferenceResolveTests {
    private static final Path RES_DIR = Paths.get("src/test/resources/generators/schema").toAbsolutePath();
    @Test(description = "Tests with object type include reference")
    public void testReferenceIncludeWithObjectType() throws IOException, BallerinaAsyncApiException {
        AsyncApi25DocumentImpl openAPI = GeneratorUtils.normalizeAsyncAPI(RES_DIR.resolve("swagger/world_bank.yaml"));
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(openAPI);

        SyntaxTree syntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreewithExpectedSyntaxTree("schema/ballerina/world_bank.bal", syntaxTree);
    }

    @Test(description = "Test for object data type when absent reference and properties fields")
    public void testWorldBank() throws IOException, BallerinaAsyncApiException {
        AsyncApi25DocumentImpl openAPI = GeneratorUtils.normalizeAsyncAPI(RES_DIR.resolve("swagger" +
                "/object_without_fields_reference.yaml"));
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(openAPI);

        SyntaxTree syntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreewithExpectedSyntaxTree(
                "schema/ballerina/object_without_fields_reference.bal", syntaxTree);
    }
    @Test(description = "Test for type generation for query parameters with referenced schemas")
    public void testParameterSchemaReferences() throws IOException, BallerinaAsyncApiException {
        AsyncApi25DocumentImpl openAPI = GeneratorUtils.normalizeAsyncAPI(RES_DIR.resolve("swagger" +
                "/schema_referenced_in_parameters.yaml"));
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(openAPI);

        SyntaxTree syntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreewithExpectedSyntaxTree(
                "schema/ballerina/parameter_schema_refs.bal", syntaxTree);
    }

    @Test(description = "Test Ballerina types generation when referred by another record with no additional fields")
    public void testReferredTypesWithoutAdditionalFields() throws IOException, BallerinaAsyncApiException {
        AsyncApi25DocumentImpl openAPI = GeneratorUtils.normalizeAsyncAPI(RES_DIR.resolve("swagger" +
                "/referred_inclusion.yaml"));
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(openAPI);
        SyntaxTree syntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreewithExpectedSyntaxTree(
                "schema/ballerina/referred_inclusion.bal", syntaxTree);
    }

    @Test(description = "Test doc comment generation of record fields when property is reffered to another schema")
    public void testDocCommentResolvingForRefferedSchemas() throws IOException, BallerinaAsyncApiException {
        AsyncApi25DocumentImpl openAPI = GeneratorUtils.normalizeAsyncAPI(RES_DIR.resolve("swagger" +
                "/resolve_reference_docs.yaml"));
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(openAPI);
        SyntaxTree syntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreewithExpectedSyntaxTree(
                "schema/ballerina/resolve_reference_docs.bal", syntaxTree);
    }


}
