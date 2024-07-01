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

public type Message readonly & record {string event;};

# Representation of a subscription.
public type Subscribe record {
    # type of event
    string event;
    # name of the user
    string name;
    # gender of the user
    string gender;
};

# Representation of a response
public type Response record {
    # dispatcher key
    string event;
    # message to be sent
    string message;
};

# Representation of an unsubscribe message.
public type Unsubscribe record {
    # dispatcher key
    string event;
};

# Repersentation of a message.
public type Chat record {
    # message to be sent  
    string message;
    # dispatcher key
    string event;
    # user id to send the message
    string toUserId;
};
