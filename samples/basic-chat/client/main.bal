// Copyright (c) 2024 WSO2 LLC. (http://www.wso2.com).
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

import ballerina/io;

UserClient chatClient = check new ();

public function main() returns error? {
    worker subscribe returns error? {
        io:println("Subscribing to the chat service");
        stream<Response,error?> subscription = check chatClient->doSubscribe({"event":"subscribe","name":"Ballerina","gender":"Female"}, 10);
        // the server will send two responses to the subscription request immediately
        printSingleResponse(subscription);
        printSingleResponse(subscription);
        //to notify the subscription is done
        true ->> function;
        while true {
            printSingleResponse(subscription);
        }
    }
    boolean waitForSubscribe = check <- subscribe;
    if waitForSubscribe is true {
        io:print("Enter your message: ");
        string message = io:readln();
        io:print("Enter to whom you want to send the message: ");
        string toUser = io:readln();
        Response|error response = chatClient->doChat({"event":"chat","message":message, "toUserId": toUser}, 10);
        if response is error {
            io:println("Error occurred: " + response.message());
        }
        io:println("RESPONSE: ", response);
    }
    check wait subscribe;
}

function printSingleResponse(stream<Response,error?> subscription) {
    record {|Response value;|}|error? message = subscription.next();
    if message !is error? {
        io:println(message.value);
    } else if message is error {
        io:println("Error occurred at worker: " + message.message());
    } else {
        io:println("NILL");
    }
}