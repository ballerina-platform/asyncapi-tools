// Copyright (c) 2023 WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

//import ballerina/http;
//import ballerina/openapi;

//listener http:Listener ep0 = new (9090);

//@openapi:serviceInfo {
//    contract: "hello_openapi.yaml"
//}
//service /payloadV on ep0 {
//    resource function get pets() returns http:Ok {
//        http:Ok ok = {body: ()};
//        return ok;
//    }
//}

//asyncapi_info- project 02 can’t  be done because @asyncapi:ServiceInfo annotation is not yet present