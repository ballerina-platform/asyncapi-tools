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
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Tests for the primitive data type.
 */
public class PrimitiveDataTypeTests {
    private static final Path RES_DIR = Paths.get("src/test/resources/asyncapi-to-ballerina/schema").
            toAbsolutePath();
    private SyntaxTree syntaxTree;
    private ByteArrayOutputStream outContent;

    @BeforeTest
    public void setUp() {
        outContent = new ByteArrayOutputStream();
        System.setErr(new PrintStream(outContent));
    }

    @Test(description = "Generate single record")
    public void generateScenario01() throws IOException, BallerinaAsyncApiException {
        Path definitionPath = RES_DIR.resolve("Primitive/scenario01.yaml");
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(asyncAPI);
        syntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreewithExpectedSyntaxTree(
                "schema/baloutputs/Primitive/schema01.bal", syntaxTree);
    }

    @Test(description = "Generate multiple record")
    public void generateScenario02() throws IOException, BallerinaAsyncApiException {
        Path definitionPath = RES_DIR.resolve("Primitive/scenario02.yaml");
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(asyncAPI);
        syntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreewithExpectedSyntaxTree(
                "schema/baloutputs/Primitive/schema02.bal", syntaxTree);
    }

    @Test(description = "Scenario for missing DataType")
    public void generateMissingDatatype() throws IOException, BallerinaAsyncApiException {
        Path definitionPath = RES_DIR.resolve("Primitive/missDataType.yaml");
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(asyncAPI);
        syntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreewithExpectedSyntaxTree(
                "schema/baloutputs/Primitive/missDataType.bal",
                syntaxTree);

    }

    @AfterTest
    public void clean() {
        System.setErr(null);
    }
}
