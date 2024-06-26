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

# this is for testing
#
# + rel-rel description
# + actions-actions description
# + fdf- fdf description
type Link record {|
    string rel;
    # Try override description
    Action actions?;
    # Try override description
    int fdf;
    # Try override description
    string s8jk;
|};

type Order record {|
    string rel;
    OrderType actions?;
    string s8jk;
|};

const SIZE = "size";

enum OrderType {
    FULL = "full",
    HALF = "Half \"Portion\"",
    CUSTOM = "custom " + SIZE
};

type Test record{|
    string check2;
    string hello;
    string s8jk;
|};

@websocket:ServiceConfig{dispatcherKey: "s8jk"}
service /payloadV on new websocket:Listener(9090) {
    #resource function description
    #+ id - test id description
    resource function get payment/[string id]() returns websocket:Service|websocket:UpgradeError {
        return new ChatServer();
    }
}

service class ChatServer{
    *websocket:Service;

    #Testing remote description
    # + message - remote above link description
    # + return - remote return description
    remote function onLink(websocket:Caller caller, Link message) returns Test{
        return {s8jk:"checking",check2: "hello",hello:"hi"};
    }

    # Order remote description
    # + message- order above link description
    # + return - order return description
    remote function onOrder(websocket:Caller caller, Order message) returns int {
        return 5;
    }
}
