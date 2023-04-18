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

public type Tuple record {
    [int, string, decimal, float, User] address;
    int id;
    [string, decimal]? unionTuple;
    ReturnTypes? tuples;
    string event;
};

public type User readonly & record {|
    int id;
    int age;
    string event;
|};

public type ReturnTypes readonly & [int, decimal];

@websocket:ServiceConfig{dispatcherKey: "event"}
service /payloadV on new websocket:Listener(9090) {
    resource function get .(User payload)returns websocket:Service|websocket:UpgradeError {
          return new ChatServer();

    }
}

service class ChatServer{
    *websocket:Service;
    # Remote tuple description
    #
    # + message - Tuple message description
    # + return - this is User return description
     remote function onTuple(websocket:Caller caller, Tuple message) returns User {
        return {id:5,age:45,event:"Testing"};
    }


}
