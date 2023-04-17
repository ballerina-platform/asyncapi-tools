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

public type Action string?;
public type Count decimal?;
public type Rels string[]?;
public type Books map<string>?;
public type Salary int|float|decimal?;

type Link record {|
    Books books;
    Rels rels;
    Action actions;
    Count count?;
    Salary salary;
    string action;
|};

@websocket:ServiceConfig{dispatcherKey: "action"}
service /payloadV on new websocket:Listener(9090) {

    # Resource function description
    #
    # + id - Query parameter id
    resource function get pathParam(int id) returns websocket:Service|websocket:UpgradeError {
        return new ChatServer();
    }
}

service class ChatServer{
    *websocket:Service;
    # Remote link description
    #
    # + message - message description
    # + return - this is return description
     remote function onLink(websocket:Caller caller, Link message) returns Action {
        return "Testing string return";
    }


}