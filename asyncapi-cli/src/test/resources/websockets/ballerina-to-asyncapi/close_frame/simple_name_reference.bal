//  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org).
//
//  WSO2 LLC. licenses this file to you under the Apache License,
//  Version 2.0 (the "License"); you may not use this file except
//  in compliance with the License.
//  You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing,
//  software distributed under the License is distributed on an
//  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//  KIND, either express or implied. See the License for the
//  specific language governing permissions and limitations
//  under the License.

import ballerina/websocket;

listener websocket:Listener l3 = new (9093);

@websocket:ServiceConfig {dispatcherKey: "event"}
service /simple/name/reference on l3 {
    resource function get simple_name_reference() returns websocket:Service|websocket:UpgradeError {
        return new SimpleNameReferenceServer();
    }
}

type SimpleNameReference websocket:CloseFrame;

service class SimpleNameReferenceServer {
    *websocket:Service;

    remote function onHeartbeat(websocket:Caller caller, Heartbeat Heartbeat) returns HeartbeatResponse|SimpleNameReference {
        return websocket:NORMAL_CLOSURE;
    }
}
