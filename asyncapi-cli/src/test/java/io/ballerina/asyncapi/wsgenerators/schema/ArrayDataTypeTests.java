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
package io.ballerina.asyncapi.wsgenerators.schema;

import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25DocumentImpl;
import io.ballerina.asyncapi.websocketscore.GeneratorUtils;
import io.ballerina.asyncapi.websocketscore.exception.BallerinaAsyncApiExceptionWs;
import io.ballerina.asyncapi.websocketscore.generators.schema.BallerinaTypesGenerator;
import io.ballerina.asyncapi.wsgenerators.common.TestUtils;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Tests for all the possible scenarios with array type in the asyncapi definition.
 */
public class ArrayDataTypeTests {
    private static final Path RES_DIR = Paths.get("src/test/resources/websockets" +
                    "/asyncapi-to-ballerina/schema").
            toAbsolutePath();
    private final PrintStream originalStdOut = System.out;
    SyntaxTree syntaxTree;
    private ByteArrayOutputStream consoleContent = new ByteArrayOutputStream();

    @BeforeMethod
    public void beforeTest() {
        // Redirect all System.out to consoleContent.
        System.setOut(new PrintStream(this.consoleContent));
    }

    @Test(description = "Generate record with array filed record")
    public void generateRecordWithArrayField() throws IOException, BallerinaAsyncApiExceptionWs {
        Path definitionPath = RES_DIR.resolve("Array/oneArray.yaml");
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(asyncAPI);
        syntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreeWithExpectedSyntaxTree("schema/baloutputs/Array/oneArray.bal",
                syntaxTree);
    }

    @Test(description = "Scenario04-Generate record with nested array filed record")
    public void generateScenario04() throws IOException, BallerinaAsyncApiExceptionWs {
        Path definitionPath = RES_DIR.resolve("Array/nestedArray.yaml");
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(asyncAPI);
        syntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreeWithExpectedSyntaxTree("schema/baloutputs/Array/nestedArray.bal",
                syntaxTree);
    }

    @Test(description = "Generate record with record type array filed record")
    public void generateRecordWithRecordArrayField() throws IOException, BallerinaAsyncApiExceptionWs {
        Path definitionPath = RES_DIR.resolve("Array/refArray.yaml");
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(asyncAPI);
        syntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreeWithExpectedSyntaxTree("schema/baloutputs/Array/refArray.bal",
                syntaxTree);
    }

    @Test(description = "Generate record for schema has array reference")
    public void generateSchemaHasArrayReference() throws IOException, BallerinaAsyncApiExceptionWs {
        Path definitionPath = RES_DIR.resolve("Array/schemaArray.yaml");
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(asyncAPI);
        syntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreeWithExpectedSyntaxTree("schema/baloutputs/Array/schemaArray.bal",
                syntaxTree);
    }

    @Test(description = "Array schema has no data type in items")
    public void arrayNoDatatype() throws IOException, BallerinaAsyncApiExceptionWs {
        Path definitionPath = RES_DIR.resolve("Array/array_no_item_type.yaml");
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(asyncAPI);
        syntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreeWithExpectedSyntaxTree("schema/baloutputs/Array/array_no_item_type.bal",
                syntaxTree);
    }

    @Test(description = "Array schema has max item count")
    public void arrayHasMaxItems() throws IOException, BallerinaAsyncApiExceptionWs {
        Path definitionPath = RES_DIR.resolve("Array/array_max_item.yaml");
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(asyncAPI);
        syntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreeWithExpectedSyntaxTree("schema/baloutputs/Array/array_max_item.bal",
                syntaxTree);
    }

    @Test(description = "Array schema with allOf")
    public void arrayHasAllOfItems() throws IOException, BallerinaAsyncApiExceptionWs {
        Path definitionPath = RES_DIR.resolve("Array/array_with_allOf.yaml");
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(asyncAPI);
        syntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreeWithExpectedSyntaxTree("schema/baloutputs/Array/array_with_allOf.bal",
                syntaxTree);
    }

    @Test(description = "Array schema with oneOf")
    public void arrayHasOneOFfItems() throws IOException, BallerinaAsyncApiExceptionWs {
        Path definitionPath = RES_DIR.resolve("Array/array_with_oneOf.yaml");
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(asyncAPI);
        syntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreeWithExpectedSyntaxTree("schema/baloutputs/Array/array_with_oneOf.bal",
                syntaxTree);
    }

    @Test(description = "Array schema with oneOf schema with nullable item")
    public void arrayHasOneOfItemsWithNullable() throws IOException, BallerinaAsyncApiExceptionWs {
        Path definitionPath = RES_DIR.resolve("Array/array_with_oneOf_complex.yaml");
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(asyncAPI);
        syntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreeWithExpectedSyntaxTree(
                "schema/baloutputs/Array/array_with_oneOf_complex.bal", syntaxTree);
    }

    @Test(description = "Array schema has max items count that ballerina doesn't support",
            expectedExceptions = BallerinaAsyncApiExceptionWs.class,
            expectedExceptionsMessageRegExp = "Maximum item count defined in the definition exceeds the.*")
    public void arrayHasMaxItemsExceedLimit() throws IOException, BallerinaAsyncApiExceptionWs {
        Path definitionPath = RES_DIR.resolve("Array/array_exceed_max_item.yaml");
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(asyncAPI);
        syntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();
    }

    @Test(description = "Array schema has max items count that ballerina doesn't support, in record field",
            expectedExceptions = BallerinaAsyncApiExceptionWs.class,
            expectedExceptionsMessageRegExp = "Maximum item count defined in the definition exceeds the.*")
    public void arrayHasMaxItemsExceedLimit02() throws IOException, BallerinaAsyncApiExceptionWs {
        Path definitionPath = RES_DIR.resolve("Array/array_exceed_max_item_02.yaml");
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(asyncAPI);
        syntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();
    }

    @AfterMethod
    public void afterTest() {
        // Put back the standard out.
        System.setOut(this.originalStdOut);
        // Clear the consoleContent.
        this.consoleContent = new ByteArrayOutputStream();
    }

    // Simulate some processing time by pausing.
    private void pause(long lPauseInMillisSec) {
        try {
            Thread.sleep(lPauseInMillisSec);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
