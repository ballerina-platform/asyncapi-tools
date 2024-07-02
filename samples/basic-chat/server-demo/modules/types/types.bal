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

import ballerina/websocket;

# Representation of a response
# 
# + event - dispatcher key
# + message - message to be sent
public type Response record {|
    string event = "just a message";
    string message;
|};

# Representation of an unsubscribe message.
#
# + event - dispatcher key
public type Unsubscribe record {|
    string event;
|};

# Representation of an info message.
#
# + event - dispatcher key
public type Info record {|
    string event;
|};

# Representation of a subscription.
#
# + event - type of event
# + name - name of the user
# + gender - gender of the user
public type Subscribe record {|
    string event;
    string name;
    string gender;
|};

# Representation of a user.
#
# + name - name of the user
# + gender - gender of the user
# + id - id of the user (connection id)
# + caller - websocket caller object
public type User record {|
    string name;
    string gender;
    string id;
    websocket:Caller caller;
|};

# Repersentation of a message.
#
# + message - message to be sent  
# + event - dispatcher key
# + toUserId - user id to send the message
public type Chat record {|
    string message;
    string event;
    string toUserId;
|};

public type Hello record {|
    string message;
    string event;
|};

# Representation of a basic event.
#
# + event - type of event
# + id - id of the event
public type ClientData record {|
    string event;
    string id;
|};
