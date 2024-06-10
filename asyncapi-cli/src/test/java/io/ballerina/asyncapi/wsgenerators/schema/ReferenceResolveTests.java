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

package io.ballerina.asyncapi.wsgenerators.schema;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25DocumentImpl;
import io.ballerina.asyncapi.websocketscore.GeneratorUtils;
import io.ballerina.asyncapi.websocketscore.exception.BallerinaAsyncApiExceptionWs;
import io.ballerina.asyncapi.websocketscore.generators.schema.BallerinaTypesGenerator;
import io.ballerina.asyncapi.wsgenerators.common.TestUtils;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Tests for Schema Reference resolve.
 */
public class ReferenceResolveTests {
    private static final Path RES_DIR = Paths.get("src/test/resources/websockets" +
                    "/asyncapi-to-ballerina/schema")
            .toAbsolutePath();
    @Test(description = "Tests with object type include reference")
    public void testReferenceIncludeWithObjectType() throws IOException, BallerinaAsyncApiExceptionWs {
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(RES_DIR.resolve(
                "Reference/multiple_references.yaml"));
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(asyncAPI);

        SyntaxTree syntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreewithExpectedSyntaxTree("schema/baloutputs/" +
                "Reference/multiple_references.bal", syntaxTree);
    }
    @Test(description = "Test Ballerina types generation when referred by another record with no additional fields")
    public void testReferredTypesWithoutAdditionalFields() throws IOException, BallerinaAsyncApiExceptionWs {
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(RES_DIR.resolve("Reference" +
                "/referred_inclusion.yaml"));
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(asyncAPI);
        SyntaxTree syntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreewithExpectedSyntaxTree(
                "schema/baloutputs/Reference/referred_inclusion.bal", syntaxTree);
    }

    @Test(description = "Test doc comment generation of record fields when property is refered to another schema")
    public void testDocCommentResolvingForRefferedSchemas() throws IOException, BallerinaAsyncApiExceptionWs {
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(RES_DIR.resolve("Reference" +
                "/resolve_reference_docs.yaml"));
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(asyncAPI);
        SyntaxTree syntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreewithExpectedSyntaxTree(
                "schema/baloutputs/Reference/resolve_reference_docs.bal", syntaxTree);
    }


}
