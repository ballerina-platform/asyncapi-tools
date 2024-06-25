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
import org.ballerinalang.formatter.core.FormatterException;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * Test implementation to verify the `anyOf` property related scenarios in asyncAPI schema generation, handled by
 * the {@link BallerinaTypesGenerator}.
 */
public class AnyOfDataTypeTests {

    private static final Path RES_DIR = Paths.get("src/test/resources/websockets" +
                    "/asyncapi-to-ballerina/schema").
            toAbsolutePath();

    //TODO: This need to check  (this has to include in upcomming version)
    @Test(description = "Test for the schema generations", enabled = false)
    public void testAnyOfSchema() throws BallerinaAsyncApiExceptionWs, IOException, FormatterException {
        Path definitionPath = RES_DIR.resolve("AnyOf/oneAnyOf.yaml");
        Path expectedPath = RES_DIR.resolve("baloutputs/AnyOf/oneAnyOf.bal");
        AsyncApi25DocumentImpl asyncAPI = GeneratorUtils.normalizeAsyncAPI(definitionPath);
        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(asyncAPI);
        SyntaxTree syntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();
        TestUtils.compareGeneratedSyntaxTreeWithExpectedSyntaxTree(
                expectedPath, syntaxTree);
    }
}
