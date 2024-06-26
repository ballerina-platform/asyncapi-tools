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
import 'service.representations as rep;
import 'service.mock;

# A fake mountain resort
@websocket:ServiceConfig{dispatcherKey: "event"}
service /payloadV on new websocket:Listener(80) {
    resource function get .()returns websocket:Service|websocket:UpgradeError {
        return new SnowPeakChatServer();
    }
}

service class SnowPeakChatServer{
    *websocket:Service;

    # Represents Snowpeak location resource
    #
    # + return - `Location` representation
    remote function onLocation(websocket:Caller caller,rep:Location location) returns rep:Locations {
        rep:Locations locations = mock:getLocations();
        return locations;
    }

    # Reperesents Snowpeak room collection resource
    #
    # + message - Rooms identification of message
    # + return - `Rooms` representation
    remote function onRooms(websocket:Caller caller,rep:Rooms message) returns rep:Location {
        return {id:"",event:""};
    }

    # Represents Snowpeak reservation resource
    #
    # + reservation - Reservation representation
    # + return - `ReservationCreated` or ReservationConflict representation
    remote function onReservation(websocket:Caller caller,rep:Reservation reservation)
                returns rep:ReservationCreated|rep:ReservationConflict {
        rep:ReservationCreated created = mock:createReservation(reservation);
        return created;
    }


    # Represents Snowpeak payment resource
    #
    # + id - Unique identification of payment
    # + payment - Payment representation
    # + return - `PaymentCreated` or `PaymentConflict` representation
    remote function onPayment(websocket:Caller caller,rep:Payment payment)
                returns rep:PaymentCreated|rep:PaymentFault|int|string|rep:PaymentConflict|map<json>|rep:PaymentSuccess{
        string id="5";
        rep:PaymentCreated paymentCreated = mock:createPayment(id, payment);
        return paymentCreated;
    }
}
