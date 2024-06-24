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
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import org.ballerinalang.formatter.core.FormatterException;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static io.ballerina.asyncapi.wsgenerators.common.TestUtils.getDiagnostics;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * This test class is to contain the test related to constraint validation.
 */
public class ConstraintTests {
    private static final Path RES_DIR = Paths.get("src/test/resources/websockets" +
                    "/asyncapi-to-ballerina/schema")
            .toAbsolutePath();

    @Test(description = "Tests with record field has constraint and record field type can be user defined datatype " +
            "with constraint.")
    public void testRecordFiledConstraint() throws IOException, BallerinaAsyncApiExceptionWs, FormatterException {
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(RES_DIR.resolve(
                "Constraint/record_field.yaml"));
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(asyncAPI);

        SyntaxTree syntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreeWithExpectedSyntaxTree("schema/baloutputs/Constraint/record_field.bal",
                syntaxTree);
        List<Diagnostic> diagnostics = getDiagnostics(syntaxTree);
        assertTrue(diagnostics.isEmpty());
    }

    @Test(description = "Tests for all the array type scenarios:" +
            "Use case 01 : User define array (Annotations on a type)" +
            "Use case 02 : For both array type and array items have constraints," +
            "Use case 03 : Reference array" +
            "Use case 04 : Array items have constrained with number format" +
            "Use case 05 : Only array items have constrained with number format")
    public void testForArray() throws IOException, BallerinaAsyncApiExceptionWs, FormatterException {
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(RES_DIR.resolve(
                "Constraint/array.yaml"));
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(asyncAPI);
        SyntaxTree syntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreeWithExpectedSyntaxTree("schema/baloutputs/Constraint/array.bal",
                syntaxTree);
        List<Diagnostic> diagnostics = getDiagnostics(syntaxTree);
        assertTrue(diagnostics.isEmpty());
    }

    @Test(description = "Tests for the field has reference type scenarios" +
            "Use case 01 : Annotations on a record field" +
            "Use case 02 : Annotations on a type" +
            "Use case 03 : Annotations on a type used as a record field")
    public void testForReference() throws IOException, BallerinaAsyncApiExceptionWs, FormatterException {
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(RES_DIR.resolve(
                "Constraint/type_def_node.yaml")
        );
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(asyncAPI);
        SyntaxTree syntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreeWithExpectedSyntaxTree("schema/baloutputs/Constraint/type_def_node.bal",
                syntaxTree);
        List<Diagnostic> diagnostics = getDiagnostics(syntaxTree);
        assertTrue(diagnostics.isEmpty());
    }


    @Test(description = "Tests with record field has constraint value with zero.")
    public void testRecordFiledConstraintWithZeroValue() throws IOException, BallerinaAsyncApiExceptionWs,
            FormatterException {
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(RES_DIR.resolve(
                "Constraint/record_field_02.yaml"));
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(asyncAPI);

        SyntaxTree syntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreeWithExpectedSyntaxTree("schema/baloutputs/Constraint/record_field_02.bal",
                syntaxTree);
        List<Diagnostic> diagnostics = getDiagnostics(syntaxTree);
        assertTrue(diagnostics.isEmpty());
    }

    @Test(description = "Tests with nested array field has constraint.")
    public void testNestedArrayWithConstraint() throws IOException, BallerinaAsyncApiExceptionWs,
            FormatterException {
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(RES_DIR.resolve("Constraint" +
                "/nested_array_with_constraint.yaml"));
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(asyncAPI);
        SyntaxTree syntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreeWithExpectedSyntaxTree("schema/baloutputs/Constraint/nested_array.bal",
                syntaxTree);
        List<Diagnostic> diagnostics = getDiagnostics(syntaxTree);
        boolean hasErrors = diagnostics.stream()
                .anyMatch(d -> DiagnosticSeverity.ERROR.equals(d.diagnosticInfo().severity()));
        assertFalse(hasErrors);
    }

    @Test
    public void testAdditionalPropertiesWithConstraint() throws IOException, BallerinaAsyncApiExceptionWs,
            FormatterException {
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(RES_DIR.resolve("Constraint" +
                "/additional_properties_with_constraint.yaml"));
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(asyncAPI);
        SyntaxTree syntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreeWithExpectedSyntaxTree(
                "schema/baloutputs/Constraint/additional_properties_with_constraint.bal", syntaxTree);
        List<Diagnostic> diagnostics = getDiagnostics(syntaxTree);
        boolean hasErrors = diagnostics.stream()
                .anyMatch(d -> DiagnosticSeverity.ERROR.equals(d.diagnosticInfo().severity()));
        assertFalse(hasErrors);
    }
    //TODO current tool doesn't handle union type: therefore union type constraint will handle once union type
    // generation available in tool.
}
