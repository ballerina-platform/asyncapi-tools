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

# A fake mountain resort
@websocket:ServiceConfig{dispatcherKey: "event"}
service /payloadV on new websocket:Listener(9090) {
    resource function get .()returns websocket:Service|websocket:UpgradeError {
        return new FirstChatServer();
    }

}

service class FirstChatServer{
    *websocket:Service;

      remote function onSubscribe(websocket:Caller caller,Subscribe payment)
                    returns int|string|boolean{

            return 1;
        }

    # Represents Snowpeak payment resource
    #
    # + id - Unique identification of payment
    # + payment - Payment representation
    # + return - `PaymentCreated` or `PaymentConflict` representation
    remote function onPayment(websocket:Caller caller,Payment payment)
                returns int|string|map<json>|map<string>{

        return "1";
    }



}

public type Subscribe record {|
    string body = "Error occurred while updating the payment";
    string event;
|};

# Reperesents payement for rooms
public type Payment record {|
    # Name of the card holder
    string cardholderName;
    # Card number
    int cardNumber;
    # Expiration month of the card in mm
    string expiryMonth;
    # Expiaration year of the card in yyyy
    string expiryYear;
    # Event description
    string event;
|};

