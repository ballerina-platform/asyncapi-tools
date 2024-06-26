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

public type Link record {
    string rel?;
    string href;
    Subscribe[] types?;
    string[] methods?;
};

public type Subscribe record{
    int id?;
    string event?;

    string fdf;

    string bb;

    string type1;
};

public type Location record {|
    map<Link> _links;
    map<string> name;
    map<int> id;
    map<float> addressCode;
    map<json> item?;
    map<string[]> mapArray?;
    map<map<json>> mapMap?;
    map<string>[] arrayItemMap?;
    string event;
|};

map<string> Test1 = {
    hello:"hello",
    event:"Test"
};

@websocket:ServiceConfig{dispatcherKey: "event"}
service /payloadV on new websocket:Listener(9090) {
    resource function get locations/[string id]() returns websocket:Service|websocket:UpgradeError {
        return new ChatServer();
    }
}

service class ChatServer{
    *websocket:Service;


    #Testing remote description
    # + location - remote above link description
    # + return - remote return description
    remote function onLocation(websocket:Caller caller, Location location) returns map<string>?{
        return Test1;

    }

}