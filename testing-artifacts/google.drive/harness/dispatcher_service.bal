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

import ballerina/log;
import ballerina/http;
import testorg/googleapis_drive_mockable as drive;
import ballerinax/asyncapi.native.handler;

service class DispatcherService {
    *http:Service;
    private map<GenericServiceType> services = {};
    private handler:NativeHandler nativeHandler = new ();
    private string channelUuid;
    private string currentToken;
    private string watchResourceId;
    private json[] currentFileStatus = [];
    private final ListenerConfig config;
    private final string specificFolderOrFileId;
    private final drive:ConnectionConfig driveConfig;
    private final boolean isWatchOnSpecificResource;
    private final boolean isFolder;
    private final string domainVerificationFileContent;

    isolated function init(ListenerConfig config, string channelUuid, string currentToken, string watchResourceId,
                            boolean isWatchOnSpecificResource, boolean isFolder,
                            string specificFolderOrFileId, string domainVerificationFileContent, drive:ConnectionConfig driveConfig) {

        self.channelUuid = channelUuid;
        self.currentToken = currentToken;
        self.watchResourceId = watchResourceId;
        self.config = config.clone();
        self.driveConfig = driveConfig;
        self.isFolder = isFolder;
        self.isWatchOnSpecificResource = isWatchOnSpecificResource;
        self.specificFolderOrFileId = specificFolderOrFileId;
        self.domainVerificationFileContent = domainVerificationFileContent;
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

    public isolated function setChannelUuid(string channelUuid) {
        lock {
            self.channelUuid = channelUuid;
        }
    }

    public isolated function setCurrentToken(string currentToken) {
        lock {
            self.currentToken = currentToken;
        }
    }

    public isolated function setWatchResourceId(string watchResourceId) {
        lock {
            self.watchResourceId = watchResourceId;
        }
    }

    public isolated function getChannelUuid() returns string {
        lock {
            return self.channelUuid;
        }
    }

    public isolated function getCurrentToken() returns string {
        lock {
            return self.currentToken;
        }
    }

    resource isolated function post .(http:Caller caller, http:Request request) returns @tainted error? {
        if (check request.getHeader(GOOGLE_CHANNEL_ID) != self.getChannelUuid()) {
            fail error("Different channel IDs found, Resend the watch request");
        } else {
            ChangesListResponse[] response = check getAllChangeList(self.getCurrentToken(), self.driveConfig);
            foreach ChangesListResponse item in response {
                self.setCurrentToken(item?.newStartPageToken.toString());
                if (self.isWatchOnSpecificResource && self.isFolder) {
                    log:printDebug("Folder watch response processing");
                    check self.mapEventForSpecificResource(<@untainted>self.specificFolderOrFileId, <@untainted>item,
                            <@untainted>self.driveConfig);
                } else if (self.isWatchOnSpecificResource && self.isFolder == false) {
                    log:printDebug("File watch response processing");
                    check self.mapFileUpdateEvents(self.specificFolderOrFileId, item, self.driveConfig);
                } else {
                    log:printDebug("Whole drive watch response processing");
                    check self.mapEvents(<@untainted>item, <@untainted>self.driveConfig);
                }
            }
            http:Response ackRes = new;
            ackRes.statusCode = http:STATUS_OK;
            check caller->respond(ackRes);
        }
    }

    // Resource function required for domain verification by Google
    # Description
    # + name - Parameter Description  
    # + caller - Parameter Description
    # + return - Return Value Description  
    resource isolated function get [string name](http:Caller caller) returns @tainted error? {
        http:Response r = new ();
        if (self.domainVerificationFileContent.length() < 100 &&
                self.domainVerificationFileContent.startsWith(GOOGLE_SITE_VERIFICATION_PREFIX)) {
            r.setHeader(CONTENT_TYPE, "text/html; charset=UTF-8");
            r.setTextPayload(self.domainVerificationFileContent);
            log:printDebug("Domain verification on process");
        } else {
            fail error("Invalid input for domain verification");
        }
        check caller->respond(r);
    }

    private isolated function executeRemoteFunc(GenericDataType genericEvent, string eventName, string serviceTypeStr, string eventFunction) returns error? {
        GenericServiceType? genericService = self.services[serviceTypeStr];
        if genericService is GenericServiceType {
            check self.nativeHandler.invokeRemoteFunction(genericEvent, eventName, eventFunction, genericService);
        }
    }

    # Checks for a modified resource.
    #
    # + resourceId - An opaque ID that identifies the resource being watched on this channel.
    # Stable across different API versions.   
    # + changeList - Record which maps the response from list changes request.  
    # + driveConfig - Drive connector configuration  
    # + return - If unsuccessful, return error.
    isolated function mapEventForSpecificResource(string resourceId, ChangesListResponse changeList,
                                            drive:ConnectionConfig driveConfig) returns @tainted error? {
        Change[]? changes = changeList?.changes;
        if (changes is Change[] && changes.length() > 0) {
            foreach Change changeLog in changes {
                string fileOrFolderId = changeLog?.fileId.toString();
                string mimeType = changeLog?.file?.mimeType.toString();
                if (mimeType != FOLDER) {
                    check self.identifyFileEvent(fileOrFolderId, changeLog, driveConfig, true, resourceId);
                } else {
                    check self.identifyFolderEvent(fileOrFolderId, changeLog, driveConfig, true, resourceId);
                }
            }
        }
    }

    # Maps and identify file change events.
    #
    # + fileId - fileId that subjected to a change.   
    # + changeLog - Change log  
    # + driveConfig - Drive connector configuration
    # + isSepcificFolder - Is specific folder  
    # + specFolderId - Spec folder ID
    # + return - if unsucessful, returns error.
    isolated function identifyFileEvent(string fileId, Change changeLog,
        drive:ConnectionConfig driveConfig, boolean isSepcificFolder = false, string? specFolderId = ())
        returns @tainted error? {
        drive:Client driveClient = check new (driveConfig);
        drive:File file = check driveClient->getFile(fileId, "createdTime,modifiedTime,trashed,parents");
        string changeTime = changeLog?.time.toString();
        boolean? isTrashed = file?.trashed;
        string[]? parentList = file?.parents;
        string createdTime = file?.createdTime.toString();
        string parent = EMPTY_STRING;
        if (parentList is string[] && parentList.length() > 0) {
            parent = parentList[0].toString();
        }
        if (isSepcificFolder && parent == specFolderId.toString()) || !isSepcificFolder {
            if (check isCreated(createdTime, changeTime)) {
                check self.executeRemoteFunc(changeLog, "is_created", "DriveService", "onFileCreate");
            } else if (isTrashed is boolean && isTrashed) {
                check self.executeRemoteFunc(changeLog, "is_trashed", "DriveService", "onFileTrash");
            } else if (check isUpdated(createdTime, changeTime)) {
                check self.executeRemoteFunc(changeLog, "is_updated", "DriveService", "onFileUpdate");
            }
        }
    }
    # Maps and identify folder change events.
    #
    # + folderId - folderId that subjected to a change.   
    # + changeLog - Change log   
    # + driveConfig - Drive connector configuration
    # + isSepcificFolder - Is specific Folder  
    # + specFolderId - Spec folder ID
    # + return - if unsucessful, returns error.
    isolated function identifyFolderEvent(string folderId, Change changeLog,
        drive:ConnectionConfig driveConfig, boolean isSepcificFolder = false, string? specFolderId = ())
        returns @tainted error? {
        drive:Client driveClient = check new (driveConfig);
        drive:File folder = check driveClient->getFile(folderId, "createdTime,modifiedTime,trashed,parents");
        string changeTime = changeLog?.time.toString();
        boolean? isTrashed = folder?.trashed;
        string createdTime = folder?.createdTime.toString();
        string[]? parentList = folder?.parents;
        string parent = EMPTY_STRING;
        if (parentList is string[] && parentList.length() > 0) {
            parent = parentList[0].toString();
        }
        if (isSepcificFolder && parent == specFolderId.toString()) || !isSepcificFolder {
            if (check isCreated(createdTime, changeTime)) {
                check self.executeRemoteFunc(changeLog, "is_created", "DriveService", "onFolderCreate");
            } else if (isTrashed is boolean && isTrashed) {
                check self.executeRemoteFunc(changeLog, "is_trashed", "DriveService", "onFolderTrash");
            } else if (check isUpdated(createdTime, changeTime)) {
                check self.executeRemoteFunc(changeLog, "is_folder_updated", "DriveService", "onFolderUpdate");
            }
        }
    }
    # Checks for a modified resource.
    #
    # + resourceId - An opaque ID that identifies the resource being watched on this channel.
    # Stable across different API versions.   
    # + changeList - Record which maps the response from list changes request.  
    # + driveConfig - Drive connector configuration 
    # + return - If it is modified, returns boolean(true). Else error.
    isolated function mapFileUpdateEvents(string resourceId, ChangesListResponse changeList, drive:ConnectionConfig driveConfig) returns @tainted error? {
        Change[]? changes = changeList?.changes;
        if (changes is Change[] && changes.length() > 0) {
            foreach Change changeLog in changes {
                string fileOrFolderId = changeLog?.fileId.toString();
                string changeTime = changeLog?.time.toString();
                if (fileOrFolderId == resourceId) {
                    drive:Client driveClient = check new (driveConfig);
                    drive:File file = check driveClient->getFile(fileOrFolderId, "createdTime,modifiedTime,trashed");
                    string createdTime = file?.createdTime.toString();
                    boolean? istrashed = file?.trashed;
                    if (istrashed == true) {
                        check self.executeRemoteFunc(changeLog, "is_created", "DriveService", "onFileTrash");
                    } else if (check isUpdated(createdTime, changeTime)) {
                        check self.executeRemoteFunc(changeLog, "is_file_updated", "DriveService", "onFileUpdate");
                    }
                }
            }
        }
    }
    # Maps Events to Change records
    #
    # + changeList - 'ChangesListResponse' record that contains the whole changeList.  
    # + driveConfig - Drive connector configuration 
    # + return - if unsucessful, returns error.
    isolated function mapEvents(ChangesListResponse changeList, drive:ConnectionConfig driveConfig) returns @tainted error? {
        Change[]? changes = changeList?.changes;
        if (changes is Change[] && changes.length() > 0) {
            foreach Change changeLog in changes {
                string fileOrFolderId = changeLog?.fileId.toString();
                drive:Client driveClient = check new (driveConfig);
                drive:File|error fileOrFolder = driveClient->getFile(fileOrFolderId);
                string mimeType = changeLog?.file?.mimeType.toString();
                if (changeLog?.removed == true && fileOrFolder is error) {
                    check self.executeRemoteFunc(changeLog, "is_file_deleted", "DriveService", "onDelete");
                } else if (mimeType != FOLDER) {
                    log:printDebug("File change event found file id : " + fileOrFolderId + " | Mime type : " + mimeType);
                    check self.identifyFileEvent(fileOrFolderId, changeLog, driveConfig);
                } else {
                    log:printDebug("Folder change event found folder id : " + fileOrFolderId + " | Mime type : " + mimeType);
                    check self.identifyFolderEvent(fileOrFolderId, changeLog, driveConfig);
                }
            }
        }
    }

}

