// Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
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

import ballerinax/googleapis.calendar;

# Listener Configuration.
#
# + calendarId - Identifier of the specific calendar
# + callbackURL - Public service url
# + expiration - Expiration period of the token
# + clientId - Client ID 
# + clientSecret - Client Secret
# + refreshUrl - Refresh URL
# + refreshToken - Refresh Token
@display {label: "Connection Config"}
public type ListenerConfig record {
    @display {label: "Calendar Id", "description": "Google Calendar ID ('primary' for main calendar)"}
    string calendarId;
    @display {label: "Call Back URL", "description": "Callback service URL for webhook" }
    string callbackURL;
    @display {label: "Client Id", "description": "OAuth Credentials - Client ID"}
    string clientId;
    @display {label: "Client Secret", "description": "OAuth Credentials - Client Secret"}
    string clientSecret;
    @display {label: "Refresh URL", "description": "OAuth Credentials - Refresh URL"}
    string refreshUrl;
    @display {label: "Refresh Token", "description": "OAuth Credentials - Refresh Token"}
    string refreshToken;
    @display {label: "Expiration", "description": "Date and time of notification channel expiration"}
    string expiration?;
};

# Defines watch response.
#
# + kind - Identifies this as a notification channel used to watch for changes to a resource
# + id - A UUID or similar unique string that identifies this channel
# + resourceId - An opaque ID that identifies the resource being watched on this channel
# + resourceUri - A version-specific identifier for the watched resource
# + token - An arbitrary string delivered to the target address
# + expiration - Date and time of notification channel expiration
public type WatchResponse record {
    string kind;
    string id;
    string resourceId;
    string resourceUri;
    string token?;
    string expiration;
};

public type Event calendar:Event;

public type GenericDataType Event;
