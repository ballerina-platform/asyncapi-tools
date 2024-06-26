// Copyright (c) 2023 WSO2 LLC. (http://www.wso2.com).
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
import ballerina/http;

@websocket:ServiceConfig {dispatcherKey: "type1"}
service /v1/abc\-hello on new websocket:Listener(9090) {
    # Description
    #
    # + path\-param - Path Parameter Description
    # + q\-paramter - Query Parameter Description
    resource function get say\-hello/[string path\-param](string q\-paramter,string ชื่\u{E2D}) returns websocket:Service|websocket:UpgradeError {
        return new FirstChatServer();
    }
}

@websocket:ServiceConfig{dispatcherKey: "event"}
service /'limit on new websocket:Listener(9091) {
    # Query parameter
    #
    # + 'limit - QParameter Description
    # + 'check - HParameter Description
    resource function get steps/'from/date/[int 'join]/พิมพ์ชื่อ(@http:Header string 'check,string 'limit,string ชื่อ) returns websocket:Service|websocket:UpgradeError {
        return new SecondChatServer();
    }

}
service class FirstChatServer{
    *websocket:Service;

     remote function onSubscribe(websocket:Caller caller, Subscribe message) returns int {
        return 5;
    }
}

service class SecondChatServer{
    *websocket:Service;

    remote function onUnSubscribe(websocket:Caller caller, UnSubscribe message)returns string {
        return "testing";
    }
}

public type Subscribe record {
    int id;
    string type1;
};

public type UnSubscribe record {
    string id;
    string event;
};
