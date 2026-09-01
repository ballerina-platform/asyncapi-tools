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
import ballerina/log;
import testorg/googleapis_drive_mockable as drive;
import ballerina/task;

# Drive event listener   
@display {label: "Google Drive", iconPath: "docs/icon.png"}
public class Listener {
    # Watch Channel ID
    public string channelUuid = EMPTY_STRING;
    # Watch Resource ID
    public string watchResourceId = EMPTY_STRING;
    private string currentToken = EMPTY_STRING;
    private string specificFolderOrFileId = EMPTY_STRING;
    private drive:Client driveClient;
    private boolean isWatchOnSpecificResource = false;
    private boolean isFolder = true;
    private ListenerConfig listenerConfig;
    private http:Listener httpListener;
    private DispatcherService dispatcherService;
    private drive:ConnectionConfig driveConnection;
    # Initializes Google Drive connector listener.
    #
    # + listenerConfig - Listener configuration
    # + return - An error on failure of initialization or else `()`
    public isolated function init(ListenerConfig listenerConfig, int|http:Listener listenOn = 8090) returns @tainted error? {
        if listenOn is http:Listener {
            self.httpListener = listenOn;
        } else {
            self.httpListener = check new (listenOn);
        }
        self.driveConnection = {
            auth: {
                clientId: listenerConfig.clientId,
                clientSecret: listenerConfig.clientSecret,
                refreshUrl: listenerConfig.refreshUrl,
                refreshToken: listenerConfig.refreshToken
            },
            // DEVIATION FROM PRODUCTION (documented in SUMMARY.md): baseUrl only exists on the
            // patched testorg/googleapis_drive_mockable connector used by this harness, points
            // getFile() calls at the local mock instead of the real Google Drive API.
            baseUrl: BASE_URL
        };
        self.driveClient = check new (self.driveConnection);
        self.listenerConfig = listenerConfig;
        string domainVerificationFileContent = listenerConfig.domainVerificationFileContent ?: EMPTY_STRING;
        self.dispatcherService = new (listenerConfig, self.channelUuid, self.currentToken, self.watchResourceId,
                                            self.isWatchOnSpecificResource, self.isFolder,
                                            self.specificFolderOrFileId, domainVerificationFileContent, self.driveConnection);
    }

    public isolated function attach(GenericServiceType serviceRef, () attachPoint) returns error? {
        string serviceTypeStr = self.getServiceTypeStr(serviceRef);
        check self.dispatcherService.addServiceRef(serviceTypeStr, serviceRef);
        _ = check task:scheduleJobRecurByFrequency(new Job(self.listenerConfig, self.driveClient, self, self.dispatcherService), 0, 1);
    }

    public isolated function 'start() returns error? {
        check self.httpListener.attach(self.dispatcherService, ());
        check self.httpListener.'start();
    }

    public isolated function detach(GenericServiceType serviceRef) returns @tainted error? {
        string serviceTypeStr = self.getServiceTypeStr(serviceRef);
        check self.dispatcherService.removeServiceRef(serviceTypeStr);
    }

    public isolated function gracefulStop() returns @tainted error? {
        check stopWatchChannel(self.driveConnection, self.channelUuid, self.watchResourceId);
        log:printInfo("Unsubscribed from the watch channel ID: " + self.channelUuid);
        return self.httpListener.gracefulStop();
    }

    public isolated function immediateStop() returns @tainted error? {
        check stopWatchChannel(self.driveConnection, self.channelUuid, self.watchResourceId);
        log:printInfo("Unsubscribed from the watch channel ID: " + self.channelUuid);
        return self.httpListener.immediateStop();
    }
    private isolated function getServiceTypeStr(GenericServiceType serviceRef) returns string {
        return "DriveService";
    }
}
