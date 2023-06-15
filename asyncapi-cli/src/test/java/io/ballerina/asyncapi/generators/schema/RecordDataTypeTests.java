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

import static io.ballerina.asyncapi.generators.common.TestUtils.compareGeneratedSyntaxTreewithExpectedSyntaxTree;

/**
 * Tests related to the record data structure.
 */
public class RecordDataTypeTests {
    private static final Path RES_DIR = Paths.get("src/test/resources/asyncapi-to-ballerina/schema").toAbsolutePath();
    SyntaxTree syntaxTree;

    @Test(description = "Generate record with record type filed record")
    public void generateRecordWithRecordField() throws IOException, BallerinaAsyncApiException {
        Path definitionPath = RES_DIR.resolve("Record/scenario05.yaml");
        AsyncApi25DocumentImpl openAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(openAPI);
        syntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreewithExpectedSyntaxTree("schema/baloutputs/Record/schema05.bal", syntaxTree);
    }

    @Test(description = "Generate empty record when no properties are given")
    public void generateEmptyRecord() throws IOException, BallerinaAsyncApiException {
        Path definitionPath = RES_DIR.resolve("Record/empty_record.yaml");
        AsyncApi25DocumentImpl openAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(openAPI);
        syntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreewithExpectedSyntaxTree("schema/baloutputs/Record/empty_record.bal", syntaxTree);
    }

    @Test(description = "Test for default optional primitive fields in records")
    public void testDefaultPrimitive() throws IOException, BallerinaAsyncApiException {
        AsyncApi25DocumentImpl openAPI = GeneratorUtils.normalizeAsyncAPI(RES_DIR.resolve("Record" +
                "/default_optional_primitive_schema.yaml"));
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(openAPI);
        SyntaxTree syntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreewithExpectedSyntaxTree("schema/baloutputs/Record/" +
                "default_optional_primitive_schema.bal", syntaxTree);
    }

    @Test(description = "Test for default optional String fields in records")
    public void testDefaultString() throws IOException, BallerinaAsyncApiException {
        AsyncApi25DocumentImpl openAPI = GeneratorUtils.normalizeAsyncAPI(RES_DIR.resolve("Record" +
                "/default_optional_string_schema.yaml"));
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(openAPI);
        SyntaxTree syntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreewithExpectedSyntaxTree("schema/baloutputs/Record/" +
                "default_optional_string_schema.bal", syntaxTree);
    }

    @Test(description = "Test for default optional String fields with value double quote in records")
    public void testDefaultWithDoubleQuote() throws IOException, BallerinaAsyncApiException {
        AsyncApi25DocumentImpl openAPI = GeneratorUtils.normalizeAsyncAPI(RES_DIR.resolve("Record" +
                "/default_optional_schema_with_doublequote.yaml"));
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(openAPI);
        SyntaxTree syntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreewithExpectedSyntaxTree("schema/baloutputs/Record/" +
                "default_optional_schema_with_doublequote.bal", syntaxTree);
    }

    @Test(description = "Test for default value for array record")
    public void testDefaultArray() throws IOException, BallerinaAsyncApiException {
        AsyncApi25DocumentImpl openAPI = GeneratorUtils.normalizeAsyncAPI(RES_DIR.resolve("Record" +
                "/default_optional_array_schema.yaml"));
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(openAPI);
        SyntaxTree syntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreewithExpectedSyntaxTree("schema/baloutputs/Record/" +
                "default_optional_array_schema.bal", syntaxTree);
    }

    @Test(description = "Test for default value for required fields")
    public void testDefaultRequired() throws IOException, BallerinaAsyncApiException {
        AsyncApi25DocumentImpl openAPI = GeneratorUtils.normalizeAsyncAPI(RES_DIR.resolve("Record" +
                "/default_required_field_schema.yaml"));
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(openAPI);
        SyntaxTree syntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreewithExpectedSyntaxTree("schema/baloutputs/Record/" +
                "default_required_field_schema.bal", syntaxTree);
    }

    @Test(description = "Generate record for schema has inline record in fields reference")
    public void generateSchemaHasInlineRecord() throws IOException, BallerinaAsyncApiException {
        Path definitionPath = RES_DIR.resolve("Record/scenario11.yaml");

        AsyncApi25DocumentImpl openAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(openAPI);
        syntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();
        compareGeneratedSyntaxTreewithExpectedSyntaxTree("schema/baloutputs/Record/schema11.bal", syntaxTree);
    }


    @Test(description = "Generate record for schema has object type only")
    public void generateForSchemaHasObjectTypeOnly() throws IOException, BallerinaAsyncApiException {
        Path definitionPath = RES_DIR.resolve("Record/scenario10.yaml");
        AsyncApi25DocumentImpl openAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(openAPI);
        syntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();
        compareGeneratedSyntaxTreewithExpectedSyntaxTree("schema/baloutputs/Record/schema10.bal", syntaxTree);
    }
}
