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
import ballerinax/googleapis.calendar;
import ballerina/log;

@display {label: "Google Calendar", iconPath: "docs/icon.png"}
public class Listener {
    private http:Listener httpListener;
    private DispatcherService dispatcherService;
    private string calendarId;
    private string address;
    private string? expiration;
    private string resourceId = "";
    private string channelId = "";
    private string? syncToken = ();
    public decimal expirationTime = 0;
    private calendar:ConnectionConfig config;

    public function init(ListenerConfig listenerConfig, int|http:Listener listenOn = 8090) returns error? {
        if listenOn is http:Listener {
            self.httpListener = listenOn;
        } else {
            self.httpListener = check new (listenOn);
        }
        calendar:ConnectionConfig config = {
            auth: {
                clientId: listenerConfig.clientId,
                clientSecret: listenerConfig.clientSecret,
                refreshUrl: listenerConfig.refreshUrl,
                refreshToken: listenerConfig.refreshToken
            }
        };
        self.config = config;
        self.calendarId = listenerConfig.calendarId;
        self.address = listenerConfig.callbackURL;
        self.expiration = listenerConfig.expiration;
        self.dispatcherService = new DispatcherService(listenerConfig, config);
    }

    public isolated function attach(GenericServiceType serviceRef, () attachPoint) returns @tainted error? {
        string serviceTypeStr = self.getServiceTypeStr(serviceRef);
        check self.dispatcherService.addServiceRef(serviceTypeStr, serviceRef);
        check self.registerWatchChannel();
        Job job = new (self);
        check job.scheduleNextChannelRenewal();
    }

    public isolated function detach(GenericServiceType serviceRef) returns error? {
        string serviceTypeStr = self.getServiceTypeStr(serviceRef);
        check self.dispatcherService.removeServiceRef(serviceTypeStr);
    }

    public isolated function 'start() returns error? {
        check self.httpListener.attach(self.dispatcherService, ());
        return self.httpListener.'start();
    }

    public isolated function gracefulStop() returns @tainted error? {
        check stopChannel(self.config, self.calendarId, self.resourceId);
        log:printInfo("Unsubscribed from channel id: " + self.channelId + ", resource id: " + self.resourceId);
        return self.httpListener.gracefulStop();
    }

    public isolated function immediateStop() returns error? {
        check stopChannel(self.config, self.calendarId, self.resourceId);
        log:printInfo("Unsubscribed from channel id: " + self.channelId + ", resource id: " + self.resourceId);
        return self.httpListener.immediateStop();
    }

    public isolated function registerWatchChannel() returns @tainted error? {
        WatchResponse res = check watchEvents(self.config, self.calendarId, self.address, self.expiration);
        self.channelId = res.id;
        self.resourceId = res.resourceId;
        DispatcherService? httpService = self.dispatcherService;
        if httpService is DispatcherService {
            httpService.setChannelId(self.channelId);
            httpService.setResourceId(self.resourceId);
        }
        log:printDebug("Subscribed to channel id : " + self.channelId + " resource id :  " + self.resourceId);
        self.expirationTime = check decimal:fromString(res.expiration);
        return;
    }

    private isolated function getServiceTypeStr(GenericServiceType serviceRef) returns string {
        return "CalendarService";
    }
}
