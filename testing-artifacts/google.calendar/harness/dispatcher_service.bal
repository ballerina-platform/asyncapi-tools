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
import ballerinax/asyncapi.native.handler;
import ballerinax/googleapis.calendar;

service class DispatcherService {
    *http:Service;
    private map<GenericServiceType> services = {};
    private handler:NativeHandler nativeHandler = new ();
    private ListenerConfig listenerConfig;
    private string channelId = "";
    private string resourceId = "";
    private string? currentSyncToken = ();
    private final calendar:ConnectionConfig calendarConfig;

    public function init(ListenerConfig listenerConfig, calendar:ConnectionConfig calendarConfig) {
        self.listenerConfig = listenerConfig;
        self.calendarConfig = calendarConfig;
    }

    isolated function addServiceRef(string serviceType, GenericServiceType genericService) returns error? {
        if (self.services.hasKey(serviceType)) {
            return error("Service of type " + serviceType + " has already been attached");
        }
        self.services[serviceType] = genericService;
    }

    isolated function removeServiceRef(string serviceType) returns error? {
        if (!self.services.hasKey(serviceType)) {
            return error("Cannot detach the service of type " + serviceType + ". Service has not been attached to the listener before");
        }
        _ = self.services.remove(serviceType);
    }

    public isolated function setChannelId(string channelId) {
        lock {
            self.channelId = channelId;
        }
    }

    public isolated function setResourceId(string resourceId) {
        lock {
            self.resourceId = resourceId;
        }
    }

    // We are not using the (@http:payload GenericEventWrapperEvent g) notation because of a bug in Ballerina.
    // Issue: https://github.com/ballerina-platform/ballerina-lang/issues/32859
    resource function post .(http:Caller caller, http:Request request) returns error? {
        // Intent verification Handling
        if (check self.isValidRequest(request)) {
            http:Response res = new;
            res.statusCode = http:STATUS_OK;
            if (check self.isValidSyncRequest(request)) {
                lock {
                    self.currentSyncToken = check self.getInitialSyncToken(self.calendarConfig, self.listenerConfig.calendarId);
                }
                check caller->respond(res);
            } else {
                string syncToken = check self.processEvent();
                lock {
                    self.currentSyncToken = syncToken;
                }
                check caller->respond(res);
            }
        } else {
            http:Response invalidRequestRes = new;
            invalidRequestRes.statusCode = http:STATUS_OK;
            check caller->respond(invalidRequestRes);
        }
    }

    private function matchRemoteFunc(GenericDataType genericDataType) returns error? {
        if (self.isCreateOrUpdateEvent(genericDataType)) {
            if (self.isNewEvent(genericDataType)) {
                check self.executeRemoteFunc(genericDataType, "newEvent", "CalendarService", "onNewEvent");
            } else {
                check self.executeRemoteFunc(genericDataType, "eventUpdate", "CalendarService", "onEventUpdate");
            }
        } else {
            check self.executeRemoteFunc(genericDataType, "eventDelete", "CalendarService", "onEventDelete");
        }
    }

    private function executeRemoteFunc(GenericDataType genericEvent, string eventName, string serviceTypeStr, string eventFunction) returns error? {
        GenericServiceType? genericService = self.services[serviceTypeStr];
        if genericService is GenericServiceType {
            check self.nativeHandler.invokeRemoteFunction(genericEvent, eventName, eventFunction, genericService);
        }
    }

    isolated function isValidRequest(http:Request request) returns boolean|error {
        lock {
            return ((check request.getHeader(GOOGLE_CHANNEL_ID)) == self.channelId && (check request.getHeader(
                GOOGLE_RESOURCE_ID)) == self.resourceId);
        }
    }

    isolated function getInitialSyncToken(calendar:ConnectionConfig config, string calendarId, 
                                            string? pageToken = ()) returns @tainted string?|error {
        calendar:EventResponse resp = check self.getEventsResponse(pageToken = pageToken);
        string? nextPageToken = resp?.nextPageToken;
        string? syncToken = ();
        if (nextPageToken is string) {
            syncToken = check self.getInitialSyncToken(config, calendarId, nextPageToken);
        }
        if (syncToken is string) {
            return syncToken;
        }
        return resp?.nextSyncToken;
    }

    function isValidSyncRequest(http:Request request) returns boolean|error {
        return ((check request.getHeader(GOOGLE_RESOURCE_STATE)) == SYNC);
    }

    function processEvent() returns string|error {
        calendar:EventResponse resp = check self.getEventsResponse();
        string syncToken = resp?.nextSyncToken ?: EMPTY_STRING;
        calendar:Event event = resp?.items[0];
        check self.matchRemoteFunc(event);
        return syncToken;
    }

    function isCreateOrUpdateEvent(GenericDataType event) returns boolean {
        string? createdTime = event?.created;
        string? updatedTime = event?.updated;
        calendar:Time? 'start = event?.'start;
        calendar:Time? end = event?.end;
        return (createdTime is string && updatedTime is string && 'start is calendar:Time && end is calendar:Time);
    }

    isolated function isNewEvent(GenericDataType event) returns boolean {
        string createdTime = event?.created.toString();
        string updatedTime = event?.updated.toString();
        return (createdTime.substring(0, 19) == updatedTime.substring(0, 19));
    }

    isolated function getEventsResponse(int? count = (), string? pageToken = ()) returns @tainted 
            calendar:EventResponse|error {
        string path = EMPTY_STRING;
        lock {
            path = calendar:prepareUrlWithEventsOptionalParams(self.listenerConfig.calendarId, count, pageToken, self.currentSyncToken);
        }

        http:Client httpClient = check getClient(self.calendarConfig);
        http:Response httpResponse = check httpClient->get(path);
        json resp = check checkAndSetErrors(httpResponse);
        return toEventResponse(resp);
    }
}
