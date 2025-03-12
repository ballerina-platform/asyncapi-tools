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

type Hello record {|
    string message;
    string event;
|};

type HelloError record {|
    string message;
    string event;
|};

type Response record {|
    string event = "just a message";
    string message;
|};

public type Heartbeat record {
    int heartbeatId;
    string event;
};

public type HeartbeatResponse record {
    int id;
};

public type Subscribe record {
    int subscribeId;
    string event;
};

public type SubscribeResponse record {
    int id;
};
