///*
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
//package io.ballerina.asyncapi.generators.client;
//
//import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25DocumentImpl;
//import io.ballerina.asyncapi.core.exception.BallerinaAsyncApiException;
//import io.ballerina.asyncapi.core.generators.client.RemoteFunctionReturnTypeGenerator;
//import io.ballerina.asyncapi.core.generators.schema.BallerinaTypesGenerator;
//import org.testng.Assert;
//import org.testng.annotations.Test;
//
//import java.io.IOException;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.ArrayList;
//
//
//
///**
// * All the tests related to the functionSignatureNode  Return type tests in
// * {{@link io.ballerina.asyncAPI.core.generators.client.IntermediateClientGenerator}} util.
// */
//public class FunctionSignatureReturnTypeTests {
//    private static final Path RES_DIR = Paths.get("src/test/resources/generators/client").toAbsolutePath();
//    @Test(description = "Tests for returnType")
//    public void getReturnTypeTests() throws IOException, BallerinaAsyncApiException {
//        RemoteFunctionReturnTypeGenerator functionReturnType = new RemoteFunctionReturnTypeGenerator();
//        AsyncApi25DocumentImpl array = getasyncAPI(RES_DIR.resolve("swagger/return_type/
//        all_return_type_operation.yaml"));
//        Assert.assertEquals(functionReturnType.getReturnType(array.getPaths().get("/jsonproducts").getGet(),
//                true), "json|error");
//        Assert.assertEquals(functionReturnType.getReturnType(array.getPaths().get("/stringproducts/record").getGet(),
//                true), "Product[]|error");
//        Assert.assertEquals(functionReturnType.getReturnType(array.getPaths().get("/stringproducts/record").getGet(),
//                false), "ProductArr|error");
//        Assert.assertEquals(functionReturnType.getReturnType(array.getPaths().get("/xmlproducts").getGet(),
//                true), "xml|error");
//        Assert.assertEquals(functionReturnType.getReturnType(array.getPaths().get("/xmlarrayproducts").getGet(),
//                true), "xml[]|error");
//        Assert.assertEquals(functionReturnType.getReturnType(array.getPaths().get("/xmlarrayproducts").getGet(),
//                false), "XMLArr|error");
//    }
//
//    @Test(description = "Tests for the object response without property")
//    public void getReturnTypeForObjectSchema() throws IOException, BallerinaAsyncApiException {
//        AsyncApi25DocumentImpl array = getasyncAPI(RES_DIR.resolve("swagger/return_type/
//        response_without_properties_with_additional" +
//                ".yaml"));
//        RemoteFunctionReturnTypeGenerator functionReturnType = new RemoteFunctionReturnTypeGenerator();
//        String returnType = functionReturnType.getReturnType(array.getPaths().get("/products").getGet(),
//                true);
//        Assert.assertEquals(returnType, "json|error");
//    }
//
//    @Test(description = "Tests for the object response without property")
//    public void getReturnTypeForMapSchema() throws IOException, BallerinaAsyncApiException {
//        AsyncApi25DocumentImpl array = getasyncAPI(RES_DIR.resolve("swagger/return_type/" +
//                "response_with_properties_with_additional.yaml"));
//        RemoteFunctionReturnTypeGenerator functionReturnType = new RemoteFunctionReturnTypeGenerator(array,
//                new BallerinaTypesGenerator(array), new ArrayList<>());
//        String returnType = functionReturnType.getReturnType(array.getPaths().get("/products").getGet(),
//                true);
//        Assert.assertEquals(returnType, "TestsProductsResponse|error");
//    }
//
//    @Test(description = "Tests for the object response without property and without additional properties")
//    public void getReturnTypeForObjectSchemaWithOutAdditional() throws IOException, BallerinaAsyncApiException {
//        AsyncApi25DocumentImpl array = getasyncAPI(RES_DIR.resolve("swagger/return_type" +
//                "/response_without_properties_without_additional" +
//                ".yaml"));
//        RemoteFunctionReturnTypeGenerator functionReturnType = new RemoteFunctionReturnTypeGenerator();
//        String returnType = functionReturnType.getReturnType(array.getPaths().get("/products").getGet(),
//                true);
//        Assert.assertEquals(returnType, "json|error");
//    }
//
//    @Test(description = "Tests for the map response with property without additional properties")
//    public void getReturnTypeForMapSchemaWithOutAdditionalProperties() throws IOException,
//    BallerinaAsyncApiException{
//        AsyncApi25DocumentImpl array = getasyncAPI(RES_DIR.resolve("swagger/return_type/
//        response_with_properties_without_additional" +
//                ".yaml"));
//        RemoteFunctionReturnTypeGenerator functionReturnType = new RemoteFunctionReturnTypeGenerator(array,
//                new BallerinaTypesGenerator(array), new ArrayList<>());
//        String returnType = functionReturnType.getReturnType(array.getPaths().get("/products").getGet(),
//                true);
//        Assert.assertEquals(returnType, "TestsProductsResponse|error");
//    }
//
//    @Test(description = "Tests for the response with no schema")
//    public void getReturnTypeForResponseWithoutSchema() throws IOException, BallerinaAsyncApiException {
//        AsyncApi25DocumentImpl array = getasyncAPI(RES_DIR.resolve("swagger/return_type/response_no_schema.yaml"));
//        RemoteFunctionReturnTypeGenerator functionReturnType = new RemoteFunctionReturnTypeGenerator(array,
//                new BallerinaTypesGenerator(array), new ArrayList<>());
//        String returnType = functionReturnType.getReturnType(array.getPaths().get("/path01").getGet(),
//                true);
//        Assert.assertEquals(returnType, "json|error");
//    }
//
//    @Test(description = "Tests for the empty response")
//    public void getReturnTypeForEmptyResponse() throws IOException, BallerinaAsyncApiException {
//        AsyncApi25DocumentImpl array = getasyncAPI(RES_DIR.resolve("swagger/return_type/no_response.yaml"));
//        RemoteFunctionReturnTypeGenerator functionReturnType = new RemoteFunctionReturnTypeGenerator(array,
//                new BallerinaTypesGenerator(array), new ArrayList<>());
//        String returnType = functionReturnType.getReturnType(array.getPaths().get("/pets").getGet(),
//                true);
//        Assert.assertEquals(returnType, "http:Response|error");
//    }
//}
