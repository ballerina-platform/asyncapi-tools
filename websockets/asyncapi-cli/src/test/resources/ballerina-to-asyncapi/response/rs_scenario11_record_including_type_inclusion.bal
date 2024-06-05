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

@websocket:ServiceConfig {dispatcherKey: "event"}
service /payloadV on new websocket:Listener(9090) {
     # Represents Snowpeak location resource
    #
    resource function get locations() returns websocket:Service|websocket:Error {
        return new WsService();
    }
}

service class WsService {
    *websocket:Service;

    remote function onSubscribe(websocket:Caller caller, Subscribe message) returns ReservationCreated|ReservationConflict {
        Reservation reservation= {
                reserveRooms: [
                    {
                        id: "r1000",
                        count: 2
                    }
                ],
                startDate: "2021-08-01",
                endDate: "2021-08-03"
            };
        ReservationCreated created = createReservation(reservation);
        return created;

    }

}



function createReservation(Reservation reservation) returns ReservationCreated {
    return {

        body: {
            id: "re1000",
            expiryDate: "2021-07-01",
            lastUpdated: "2021-06-29T13:01:30Z",
            reservation: {
                reserveRooms: [
                    {
                        id: "r1000",
                        count: 2
                    }
                ],
                startDate: "2021-08-01",
                endDate: "2021-08-03"
            },
            links: [
                {
                    rel: "cancel",
                    href: "http://localhost:9090/reservation/re1000",
                    mediaTypes: ["applicaion/vnd.snowpeak.resort+json"],
                    actions: [DELETE]
                },
                {
                    rel: "edit",
                    href: "http://localhost:9090/reservation/re1000",
                    mediaTypes: ["applicaion/vnd.snowpeak.resort+json"],
                    actions: [PUT]
                },
                {
                    rel: "payment",
                    href: "http://localhost:9090/payment/re1000",
                    mediaTypes: ["applicaion/vnd.snowpeak.resort+json"],
                    actions: [POST]
                }
            ]
        }
    };
}





public type Subscribe record{
    int id;
    string event;
};
# Link details
type Link record {|
    # linnk rel
    string rel;
    # link href
    string href;
    # mediaTypes
    string[] mediaTypes?;
    # actions
    Action[] actions?;
|};

# Link details
type Links record {|
    # Array links
    Link[] links;
|};



enum Action {
    GET,
    POST,
    PUT,
    DELETE,
    PATCH
}

# Represents rooms be reserved
type ReserveRoom record {|
    # Unique identification of the room
    string id;
    # Number of rooms
    int count;
|};
# Represents a reservation of rooms
type Reservation record {|
    # Rooms to be reserved
    ReserveRoom[] reserveRooms;
    # Start date in yyyy-mm-dd
    string startDate;
    # End date in yyyy-mm-dd
    string endDate;
|};
# Represents a receipt for the reservation
type ReservationReceipt record {|
    *Links;
    # Unique identification
    string id;
    # Expiry date in yyyy-mm-dd
    string expiryDate;
    # Last updated time stamp
    string lastUpdated;
    # Reservation
    Reservation reservation;
|};

type ReservationCreated record {|

    ReservationReceipt body;
|};
type ReservationConflict record {|
    string body = "Error occurred while updating the reservation";
|};
