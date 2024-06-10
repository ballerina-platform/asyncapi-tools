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
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Tests for AsyncAPI definition errors.
 */
public class AsyncAPIFileParserTests {
    private static final Path RES_DIR = Paths.get("src/test/resources/websockets" +
                    "/asyncapi-to-ballerina/schema").
            toAbsolutePath();

    @Test(description = "Test invalid file path",
            expectedExceptions = BallerinaAsyncApiExceptionWs.class,
            expectedExceptionsMessageRegExp = "AsyncAPI contract doesn't exist in the given .*")
    public void testInvalidFilePath() throws IOException, BallerinaAsyncApiExceptionWs {
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.getAsyncAPIFromAsyncAPIParser(RES_DIR.resolve("user.yaml"));
    }

    //TODO: expectedExceptionsMessageRegExp = "Invalid file type.*"
    @Test(description = "Test invalid file type",
            expectedExceptions = BallerinaAsyncApiExceptionWs.class)
    public void testInvalidFileType() throws IOException, BallerinaAsyncApiExceptionWs {
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.getAsyncAPIFromAsyncAPIParser(RES_DIR.resolve(
                "Invalid/petstore.txt"));
    }

    //TODO expectedExceptionsMessageRegExp = "AsyncAPI file has errors: .*"
    @Test(description = "Test invalid asyncapi version file ",
            expectedExceptions = BallerinaAsyncApiExceptionWs.class,
    expectedExceptionsMessageRegExp = "AsyncAPI definition has errors." +
            " Ballerina client code can only be generate for 2.5.0 version")
    public void testInvalidFile() throws IOException, BallerinaAsyncApiExceptionWs {
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.getAsyncAPIFromAsyncAPIParser(RES_DIR.resolve(
                "Invalid/invalid.yaml"));
    }

    //TODO: This error message need to improve
    @Test(description = "Test asyncapi specification has undocumented reference in schema.",
            expectedExceptions = BallerinaAsyncApiExceptionWs.class,
            expectedExceptionsMessageRegExp = "Undefined \\$ref: '#/components/schemas/Request' in asyncAPI contract.")
    public void testForUndocumentedReference() throws IOException, BallerinaAsyncApiExceptionWs {
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(RES_DIR.resolve(
                "Invalid/undocument_ref.yaml"));
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(asyncAPI);
        SyntaxTree syntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();
    }
}
