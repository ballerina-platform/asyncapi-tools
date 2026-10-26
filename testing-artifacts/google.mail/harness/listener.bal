// Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import ballerinax/googleapis.gmail;
import ballerina/log;

@display {label: "Google Mail", iconPath: "docs/icon.png"}
public class Listener {
    private http:Listener httpListener;
    private DispatcherService dispatcherService;

    private string startHistoryId = "";
    private string topicResource = "";
    private string subscriptionResource = "";
    private string userId = ME;
    private gmail:ConnectionConfig gmailConfig;
    private string project;
    private string pushEndpoint;

    private WatchRequestBody requestBody = {topicName: ""};
    http:Client pubSubClient;
    http:Client gmailHttpClient;

    public function init(ListenerConfig listenerConfig, int|http:Listener listenOn = 8090) returns error? {
        if listenOn is http:Listener {
            self.httpListener = listenOn;
        } else {
            self.httpListener = check new (listenOn);
        }
        http:ClientSecureSocket? socketConfig = listenerConfig.secureSocketConfig;
        // Create pubsub http client.
        self.pubSubClient = check new (PUBSUB_BASE_URL, {
            auth: {
                clientId: listenerConfig.clientId,
                clientSecret: listenerConfig.clientSecret,
                refreshUrl: listenerConfig.refreshUrl,
                refreshToken: listenerConfig.refreshToken
            },
            secureSocket: socketConfig
        });
        // Create gmail http client.
        gmail:ConnectionConfig gmailConfig = {
            auth: {
                clientId: listenerConfig.clientId,
                clientSecret: listenerConfig.clientSecret,
                refreshUrl: listenerConfig.refreshUrl,
                refreshToken: listenerConfig.refreshToken
            }
        };
        self.gmailHttpClient = check new (GMAIL_BASE_URL, {
            auth: gmailConfig.auth
        });
        self.gmailConfig = gmailConfig;
        self.project = listenerConfig.project;
        self.pushEndpoint = listenerConfig.callbackURL;

        TopicSubscriptionDetail topicSubscriptionDetail = check createTopic(self.pubSubClient, listenerConfig.project, listenerConfig.callbackURL);
        self.topicResource = topicSubscriptionDetail.topicResource;
        self.subscriptionResource = topicSubscriptionDetail.subscriptionResource;
        self.requestBody = {topicName: self.topicResource, labelIds: [INBOX], labelFilterAction: INCLUDE};
        self.dispatcherService = new DispatcherService(gmailConfig, self.subscriptionResource);
    }

    public isolated function attach(GenericServiceType serviceRef, () attachPoint) returns @tainted error? {
        string serviceTypeStr = self.getServiceTypeStr(serviceRef);
        check self.dispatcherService.addServiceRef(serviceTypeStr, serviceRef);
        check self.watchMailbox();
        Job job = new (self);
        check job.scheduleNextWatchRenewal();
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
        check stop(self.gmailHttpClient, self.userId);
        log:printInfo("Unsubscribed from the watch channel for user id: " + self.userId);
        return self.httpListener.gracefulStop();
    }

    public isolated function immediateStop() returns error? {
        check stop(self.gmailHttpClient, self.userId);
        log:printInfo("Unsubscribed from the watch channel for user id: " + self.userId);
        return self.httpListener.immediateStop();
    }

    public isolated function watchMailbox() returns @tainted error? {
        WatchResponse response = check watch(self.gmailHttpClient, self.userId, self.requestBody);
        self.startHistoryId = response.historyId;
        log:printInfo(NEW_HISTORY_ID + self.startHistoryId);
        self.dispatcherService.setStartHistoryId(self.startHistoryId);

    }

    private isolated function getServiceTypeStr(GenericServiceType serviceRef) returns string {
        return "GmailService";
    }
}
