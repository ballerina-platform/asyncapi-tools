//  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
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

public type Message readonly & record {string 'type;};

public type ConnectionInit record {
    string 'type;
    record {} payload?;
};

public type ConnectionAck record {
    string 'type;
    record {} payload?;
};

public type PingMessage record {
    string 'type;
    record {} payload?;
};

public type PongMessage record {
    string 'type;
    record {} payload?;
};

public type Subscribe record {
    string 'type;
    string id;
    record {string? operationName?; string query; anydata? variables?; anydata? extensions?;} payload;
};

public type Next record {
    string 'type;
    string id;
    json payload;
};

public type Complete record {
    string 'type;
    string id;
};
