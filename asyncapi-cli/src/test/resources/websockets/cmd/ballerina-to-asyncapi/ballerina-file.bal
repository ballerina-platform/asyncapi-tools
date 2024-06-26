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

listener websocket:Listener ep0 = new(80,config = { secureSocket : {
        key: {
            certFile: "../resource/path/to/public.crt",
            keyFile: "../resource/path/to/private.key"
        }
    }
});

@websocket:ServiceConfig {subProtocols: [],dispatcherKey: "bb"}
service /hello on ep0 {
    resource function get payment/[string id]() returns websocket:Service|websocket:UpgradeError {
        return new ChatServer();
    }
}

@websocket:ServiceConfig{dispatcherKey: "fdf"}
service /hello2 on ep0,new websocket:Listener(8081) {
    resource function get v1/[int id]/v2/[string name]/v3/[float value]/payment/[int data] (@http:Header {} string X\-Client) returns websocket:Service|websocket:UpgradeError {
        return new ChatServer1();
    }
}

service class ChatServer {
    *websocket:Service;

     remote function onSubscribe(websocket:Caller caller, Subscribe message) returns int {
        return 5;
    }
}

service class ChatServer1{
    *websocket:Service;

    remote function onSubscribe(websocket:Caller caller, Subscribe message) {
    }
}

public type Subscribe record {
    int id?;
    string event?;
    Heartbeat hello;
    string fdf;
    string bb;
    string type1;
};

public type Hello int;
public type Heartbeat record {
    int id;
    string event;
    Hello hello;
    string fdf;
    string type1;
};
