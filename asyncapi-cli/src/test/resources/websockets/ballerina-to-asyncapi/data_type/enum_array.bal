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

enum Action {
    GET,
    POST,
    PUT,
    DELETE,
    PATCH
}

type Link record {|
    string rel;
    string href;
    string[] mediaTypes?;
    Action[] actions?;
    string event;
|};

@websocket:ServiceConfig{dispatcherKey: "event"}
service /payloadV on new websocket:Listener(9090) {

    #  reservation channel description
    #
    # + id - id description
    resource function get reservation/[string id]() returns websocket:Service|websocket:UpgradeError {
        return new ChatServer();
    }
}

service class ChatServer{
    *websocket:Service;

    # onlink remote function description
    #
    # + link - link description
    # + return - Return int description
     remote function onLink(websocket:Caller caller, Link link) returns int {
        return 5;
    }
}
