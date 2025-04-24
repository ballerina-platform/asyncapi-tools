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

listener websocket:Listener websocketListener2 = check new (9092);

@websocket:ServiceConfig {dispatcherKey: "event"}
service / on websocketListener2 {

    resource function get .() returns websocket:Service|websocket:UpgradeError {
        return new WsService2();
    }
}

service class WsService2 {
    *websocket:Service;

    remote function onHello(Hello clientData) returns websocket:NormalClosure {
        return websocket:NORMAL_CLOSURE;
    }
}
