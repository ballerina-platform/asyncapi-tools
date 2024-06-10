package io.ballerina.asyncapi.wsgenerators.common;
//package io.ballerina.asyncapi.generators.common;///*
// * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
// *
// * WSO2 Inc. licenses this file to you under the Apache License,
// * Version 2.0 (the "License"); you may not use this file except
// * in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing,
// * software distributed under the License is distributed on an
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// * KIND, either express or implied. See the License for the
// * specific language governing permissions and limitations
// * under the License.
// */
//
//package io.ballerina.asyncAPI.generators.common;
//
//import io.ballerina.compiler.syntax.tree.SyntaxTree;
//import io.ballerina.asyncAPI.core.GeneratorUtils;
//import io.ballerina.asyncAPI.core.exception.BallerinaasyncAPIException;
//import io.ballerina.asyncAPI.core.generators.schema.BallerinaTypesGenerator;
//import io.swagger.v3.oas.models.asyncAPI;
//import org.testng.Assert;
//import org.testng.annotations.Test;
//
//import java.io.IOException;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//
//import static io.ballerina.asyncAPI.core.GeneratorUtils.extractReferenceType;
//import static io.ballerina.asyncAPI.core.GeneratorUtils.getValidName;
//import static io.ballerina.asyncAPI.generators.common.TestUtils.compareGeneratedSyntaxTreeWithExpectedSyntaxTree;
//
///**
// * This util class for testing functionality for {@GeneratorUtils.java}.
// */
//public class GeneratorUtilsTests {
//    private static final Path RES_DIR = Paths.get("src/test/resources/generators").toAbsolutePath();
//
//    //TODO: expectedExceptionsMessageRegExp = "asyncAPI file has errors: .*"
//    @Test(description = "Functionality tests for getBallerinaasyncAPIType",
//            expectedExceptions = BallerinaasyncAPIException.class)
//    public static void getIncorrectYamlContract() throws IOException, BallerinaasyncAPIException {
//        Path path = RES_DIR.resolve("swagger/invalid/petstore_without_info.yaml");
//        asyncAPI ballerinaasyncAPIType = GeneratorUtils.getasyncAPIFromasyncAPIV3Parser(path);
//    }
//
//    //TODO: expectedExceptionsMessageRegExp = "asyncAPI file has errors: .*"
//    @Test(description = "Functionality tests for When info section null",
//            expectedExceptions = BallerinaasyncAPIException.class)
//    public static void testForInfoNull() throws IOException, BallerinaasyncAPIException {
//        Path path = RES_DIR.resolve("swagger/invalid/petstore_without_info.yaml");
//        asyncAPI ballerinaasyncAPIType = GeneratorUtils.getasyncAPIFromasyncAPIV3Parser(path);
//    }
//
//    //TODO: expectedExceptionsMessageRegExp = "Invalid reference value : .*"
//    @Test(description = "Functionality negative tests for extractReferenceType",
//            expectedExceptions = BallerinaasyncAPIException.class)
//    public static void testForReferenceLinkInvalid() throws BallerinaasyncAPIException {
//        String recordName = extractReferenceType("/components/schemas/Error");
//    }
//
//    @Test(description = "Add valid reference path for extract")
//    public static void testForReferenceLinkValid() throws BallerinaasyncAPIException {
//        Assert.assertEquals(GeneratorUtils.extractReferenceType("#/components/schemas/Error"), "Error");
//        Assert.assertEquals(GeneratorUtils.extractReferenceType("#/components/schemas/Pet.-id"), "Pet.-id");
//        Assert.assertEquals(GeneratorUtils.extractReferenceType("#/components/schemas/Pet."), "Pet.");
//        Assert.assertEquals(GeneratorUtils.extractReferenceType("#/components/schemas/200"), "200");
//        Assert.assertEquals(getValidName(GeneratorUtils.extractReferenceType("#/components/schemas/worker"),
//                true), "Worker");
//        Assert.assertEquals(getValidName(GeneratorUtils.extractReferenceType("#/components/schemas/worker abc"),
//                true), "WorkerAbc");
//    }
//
//    @Test(description = "Generate the readable function, record name removing special characters")
//    public static void testGenerateReadableName() {
//        Assert.assertEquals(getValidName("endpoint-remove-shows-user", true),
//                "EndpointRemoveShowsUser");
//    }
//
//    @Test(description = "Set record name with removing special Characters")
//    public static void testRecordName() throws IOException, BallerinaasyncAPIException {
//        asyncAPI asyncAPI = GeneratorUtils.normalizeasyncAPI(RES_DIR.resolve("schema/swagger/recordName" +
//                ".yaml"), false);
//        BallerinaTypesGenerator ballerinaSchemaGenerator = new BallerinaTypesGenerator(asyncAPI);
//        SyntaxTree syntaxTree = ballerinaSchemaGenerator.generateSyntaxTree();
//        Path expectedPath = RES_DIR.resolve("schema/ballerina/recordName.bal");
//        compareGeneratedSyntaxTreeWithExpectedSyntaxTree(expectedPath, syntaxTree);
//    }
//}
