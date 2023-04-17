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

import ballerina/websocket;

type ResponseError record {|
    int? id;
    ResponseError resError?;
    string event;
|};

type ResponseError02 record {|
    int? id;
    ResponseError02|string resError?;
    string event;
|};

type ResponseError03 record {|
    int? id;
    ResponseError03[] resError?;
    string event;
|};

type ResponseError04 record {|
    int? id;
    ResponseError04[][] resError?;
    string event;
|};

type ResponseError05 record {|
    int? id;
    ResponseError05? resError?;
    string event;
|};

type ResponseError06 record {|
    int? id;
    ResponseError06[]? resError?;
    string event;
|};

type ResponseError07 record {|
    int? id;
    ResponseError07[]? resError;
    string event;
|};

listener websocket:Listener ep0 = new (443, config = {host: "petstore.swagger.io"});

@websocket:ServiceConfig{dispatcherKey: "event"}
service /payloadV on ep0 {
    resource function get albums/[string id]() returns websocket:Service| websocket:UpgradeError{
        return new ChatServer();
    }


}
service class ChatServer{
    *websocket:Service;

    remote function onResponseError (websocket:Caller caller,ResponseError message) returns ResponseError{
        return {id:4,resError: {id: 4, event: ""},event: ""};
    }
    remote function onResponseError02(websocket:Caller caller,ResponseError02 message) returns ResponseError02 {
        return {id:4,resError: {id: (),event: ""},event: ""};
    }
    remote function onResponseError03(websocket:Caller caller,ResponseError03 message) returns ResponseError03{
        return {id:4,resError: [],event: ""};

    }
    remote function onResponseError04(websocket:Caller caller,ResponseError04 message) returns ResponseError04 {
        return  {id:4,resError: [[]],event: ""};

    }
    remote function onResponseError05(websocket:Caller caller,ResponseError05 message) returns ResponseError05 {
         return {id: (),event: ""};
    }
    remote function onResponseError06(websocket:Caller caller,ResponseError06 message) returns ResponseError06 {
        return {id:3,resError: (),event: ""};
    }
    remote function onResponseError07(websocket:Caller caller,ResponseError07 message) returns ResponseError07 {
        return {id:5,resError: (),event: ""};
    }


}



