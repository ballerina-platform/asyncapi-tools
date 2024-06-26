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

listener websocket:Listener helloEp = new (80);

public type Subscribe record {
    int id;
    string event;
};

public type Ticker record {
    int id;
};

public type Ping record {
    string event;
    Reqid reqid?;
};

public type Pong record {
    string event?;
    Reqid reqid?;
};

public type Reqid int?;
@websocket:ServiceConfig{dispatcherKey: "event"}
service / on helloEp {
    resource function get .() returns websocket:Service| websocket:UpgradeError {
        return new ChatServer();
    }
}

service class ChatServer{
    *websocket:Service;

    remote function onSubscribe(websocket:Caller caller,Subscribe message) returns Ticker {
        return {id:1};
    }

	remote function onPong(websocket:Caller caller, byte[] data) {

	}
}
