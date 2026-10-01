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

import ballerina/http;
import ballerina/log;
import ballerina/uuid;
import ballerinax/'client.config;
import ballerinax/googleapis.calendar;

# Create subscription to get notification.
# 
# + config - Calendar configuration
# + calendarId - Calendar id
# + address - The address where notifications are delivered for this channel
# + expiration - The time-to-live in seconds for the notification channel
# + return - WatchResponse object on success else an error
isolated function watchEvents(calendar:ConnectionConfig config, string calendarId, string address,
                                string? expiration = ()) returns @tainted WatchResponse|error {
    json payload;
    if (expiration is string) {
        payload = {
            id: uuid:createType1AsString(),
            token: uuid:createType1AsString(),
            'type: WEBHOOK,
            address: address,
            params: {
                ttl: expiration
            }
        };
    } else {
        payload = {
            id: uuid:createType1AsString(),
            token: uuid:createType1AsString(),
            'type: WEBHOOK,
            address: address
        };
    }
    http:Request req = new;
    string path = prepareUrl([CALENDAR_PATH, CALENDAR, calendarId, EVENTS, WATCH]);
    req.setJsonPayload(payload);
    http:Client httpClient = check getClient(config);
    http:Response httpResponse = check httpClient->post(path, req);
    json result = check checkAndSetErrors(httpResponse);
    return toWatchResponse(result);
}

# Stop channel from subscription
# 
# + config - Calendar configuration
# + id - Channel id
# + resourceId - Id of resource being watched
# + token - An arbitrary string delivered to the target address with each notification (optional)
# + return - Error on failure
isolated function stopChannel(calendar:ConnectionConfig config, string id, string resourceId, string? token = ()) 
                                returns @tainted error? {
    json payload = {
        id: id,
        resourceId: resourceId,
        token: token
    };
    string path = prepareUrl([CALENDAR_PATH, CHANNELS, STOP]);
    http:Request req = new;
    req.setJsonPayload(payload);
    http:Client httpClient = check getClient(config);
    http:Response httpResponse = check httpClient->post(path, req);
    _ = check checkAndSetErrors(httpResponse);
    return;
}

# Convert json to Event.
# 
# + payload - Json response
# + return - An Event record on success else an error
isolated function toEventResponse(json payload) returns calendar:EventResponse|error {
    calendar:EventResponse|error res = payload.cloneWithType(calendar:EventResponse);
    if (res is calendar:EventResponse) {
        return res;
    } else {
        log:printError("calendar:ERR_EVENT" + PAYLOAD + payload.toJsonString(), 'error = res);
        return error("calendar:ERR_EVENT", res);
    }
}

isolated function getClient(calendar:ConnectionConfig config) returns http:Client|error {
    http:ClientConfiguration httpClientConfig = check config:constructHTTPClientConfig(config);
    return check new (BASE_URL, httpClientConfig);
}

# Prepare URL.
# 
# + paths - An array of paths prefixes
# + return - The prepared URL
isolated function prepareUrl(string[] paths) returns string {
    string url = EMPTY_STRING;
    if (paths.length() > 0) {
        foreach var path in paths {
            url = (!path.startsWith(FORWARD_SLASH)) ? (url + FORWARD_SLASH) : url;
            url = url + path;
        }
    }
    return <@untainted>url;
}

# Check HTTP response and return JSON payload on success else an error.
# 
# + httpResponse - HTTP respone or HTTP payload or error
# + return - JSON result on success else an error
isolated function checkAndSetErrors(http:Response|error httpResponse) returns @tainted json|error {
    if (httpResponse is http:Response) {
        if (httpResponse.statusCode == http:STATUS_OK || httpResponse.statusCode == http:STATUS_CREATED) {
            json|error jsonResponse = httpResponse.getJsonPayload();
            if (jsonResponse is json) {
                return jsonResponse;
            } else {
                return error(JSON_ACCESSING_ERROR_MSG, jsonResponse);
            }
        } else if (httpResponse.statusCode == http:STATUS_NO_CONTENT) {
            return {};
        } else {
            json|error jsonResponse = httpResponse.getJsonPayload();
            if (jsonResponse is json) {
                return error(HTTP_ERROR_MSG + jsonResponse.toString());
            } else {
                return error(ERR_EXTRACTING_ERROR_MSG, jsonResponse);
            }
        }
    } else {
        return error(HTTP_ERROR_MSG + (<error>httpResponse).message());
    }
}

# Convert json to WatchResponse.
# 
# + payload - Json response
# + return - A WatchResponse object on success else an error
isolated function toWatchResponse(json payload) returns WatchResponse|error {
    WatchResponse|error res = payload.cloneWithType(WatchResponse);
    if (res is WatchResponse) {
        return res;
    } else {
        log:printError(ERR_WATCH_RESPONSE + PAYLOAD + payload.toJsonString(), 'error = res);
        return error(ERR_WATCH_RESPONSE, res);
    }
}
