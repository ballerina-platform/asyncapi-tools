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

type Link record {|
    string rel?;
    string href?;
    string[] mediaTypes?;
|};

type Links record {|
    Link[] links?;
    int linkid?;
|};

type ReservationReceipt record {|
    *Links;
    string id?;
    string event;
|};

public type Subscribe record{
    Depth depth?;
    Interval interval?;
    MaxRateCount maxratecount?;
    Name name;
    Token token?;
    string event;
};

public type Depth int?;
public type Token string?;

public enum Name {
    book,
    ohlc,
    openOrders,
    ownTrades,
    spread,
    ticker,
    trade
}

public type MaxRateCount int?;
public type Interval int?;

listener websocket:Listener ep0 = new(443, config = {host: "petstore.swagger.io"});

@websocket:ServiceConfig{dispatcherKey: "event"}
service /payloadV on ep0 {
    resource function get pathParam(ReservationReceipt queryParam) returns websocket:Service|websocket:UpgradeError {
        return new ChatServer();
    }
}

service class ChatServer{
    *websocket:Service;

    remote function onSubscribe(websocket:Caller caller, Subscribe message) returns int {
        return 5;
    }
    remote function onReservationReceipt(websocket:Caller caller, ReservationReceipt message) returns int {
        return 5;
    }
}
