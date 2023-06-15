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
import ballerina/io;

@websocket:ServiceConfig {dispatcherKey: "type", subProtocols: ["graphql-transport-ws"]}
service /payloadV on new websocket:Listener(9090) {
    resource function get .() returns websocket:Service|websocket:UpgradeError {
        return new ChatServer();

    }
}

service class ChatServer {
    *websocket:Service;

    remote function onSubscribeMessage(SubscribeMessage message) returns stream<NextMessage|CompleteMessage> {
        NextMessage returnMessage = {id: message.id, payload: (), 'type: "next"};
        NextMessage[] array = genStream(10, returnMessage, message.id);
        return array.toStream();
    }

    remote function onPingMessage(PingMessage message) returns PongMessage {
        return {'type: "error"};
    }

    remote function onPongMessage(PongMessage message) {
        io:println(message);
    }

    remote function onConnectionInitMessage(ConnectionInitMessage message) returns ConnectionAckMessage {
        return {'type: "connectionack"};
    }

    remote function onCompleteMessage(CompleteMessage message) {
        io:println(message);

    }

    remote function onError(websocket:Caller caller, error err) returns error? {
        check caller->writeMessage({"type": "error"});
    }

    remote function onIdleTimeout() {
        io:println("timeout");

    }

}

isolated function genStream(int times, NextMessage payload, string id) returns NextMessage[] {
    NextMessage[] array = [];
    int time = 0;
    while (time < times) {
        array.push(payload);
        time += 1;
    }
    array.push({id, 'type: "complete", payload: ()});
    return array;

}

type ConnectionInitMessage record {|
    string 'type="connection_init";
    map<json> payload?;
|};

type ConnectionAckMessage record {|
    string 'type="connection_ack";
    map<json> payload?;
|};

type PongMessage record {|
    string 'type="pong";
    map<json> payload?;
|};

type PingMessage record {|
    string 'type="ping";
    map<json> payload?;
|};

public type NextMessage record {
    string id;
    string 'type = "next";
    json payload;

};

public type CompleteMessage record {
    string id;
    string 'type = "complete";

};

public type TestMessage record {
    string id;
    string 'type = "error";
    json payload;

};

public type SubscribeMessage record {|
    string id;
    string 'type="subscribe";
    record {|
        string? operationName?;
        string query;
        map<json>? variables?;
        map<json>? extensions?;
    |} payload;
|};

