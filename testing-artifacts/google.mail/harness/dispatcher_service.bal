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
import ballerinax/asyncapi.native.handler;
import ballerinax/googleapis.gmail;
import ballerina/log;

service class DispatcherService {
    *http:Service;
    private map<GenericServiceType> services = {};
    private handler:NativeHandler nativeHandler = new ();
    private string startHistoryId = "";
    private final string subscriptionResource;
    private final gmail:ConnectionConfig gmailConfig;

    public function init(gmail:ConnectionConfig gmailConfig, string subscriptionResource) {
        self.gmailConfig = gmailConfig;
        self.subscriptionResource = subscriptionResource;
    }

    public isolated function setStartHistoryId(string startHistoryId) {
        lock {
            self.startHistoryId = startHistoryId;
        }
    }

    public isolated function getStartHistoryId() returns string {
        lock {
            return self.startHistoryId;
        }
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

    // We are not using the (@http:payload GenericEventWrapperEvent g) notation because of a bug in Ballerina.
    // Issue: https://github.com/ballerina-platform/ballerina-lang/issues/32859
    resource function post .(http:Caller caller, http:Request request) returns error? {
        json ReqPayload = check request.getJsonPayload();
        string incomingSubscription = check ReqPayload.subscription;

        if (self.subscriptionResource === incomingSubscription) {
            string? pageToken = ();
            boolean historyFetchFailed = false;
            string startId = self.getStartHistoryId();
            string lastHistoryId = startId;
            while true {
                var historyResponse = listHistory(self.gmailConfig, startId, pageToken = pageToken);
                if historyResponse is gmail:ListHistoryResponse {
                    gmail:History[]? historyList = historyResponse.history;
                    if historyList is gmail:History[] {
                        foreach gmail:History historyItem in historyList {
                            check self.dispatch(historyItem);
                            string? itemId = historyItem.id;
                            if itemId is string {
                                lastHistoryId = itemId;
                            }
                        }
                    }
                    // Prefer the mailbox-level historyId from the response as the next cursor
                    string? responseHistoryId = historyResponse.historyId;
                    if responseHistoryId is string {
                        lastHistoryId = responseHistoryId;
                    }
                    string? nextToken = historyResponse.nextPageToken;
                    if nextToken is string {
                        pageToken = nextToken;
                    } else {
                        break;
                    }
                } else {
                    log:printError(ERR_HISTORY_LIST, 'error = historyResponse);
                    historyFetchFailed = true;
                    break;
                }
            }
            if !historyFetchFailed {
                self.setStartHistoryId(lastHistoryId);
                log:printDebug(NEXT_HISTORY_ID + lastHistoryId);
            }
            if historyFetchFailed {
                http:Response errorRes = new;
                errorRes.statusCode = http:STATUS_INTERNAL_SERVER_ERROR;
                check caller->respond(errorRes);
            } else {
                http:Response ackRes = new;
                ackRes.statusCode = http:STATUS_OK;
                check caller->respond(ackRes);
            }
        } else {
            log:printWarn(WARN_UNKNOWN_PUSH_NOTIFICATION + incomingSubscription);
            http:Response unknownSubscriptionRes = new;
            unknownSubscriptionRes.statusCode = http:STATUS_OK;
            check caller->respond(unknownSubscriptionRes);
        }
    }

    private isolated function executeRemoteFunc(GenericDataType genericEvent, string eventName, string serviceTypeStr, string eventFunction) returns error? {
        GenericServiceType? genericService = self.services[serviceTypeStr];
        if genericService is GenericServiceType {
            check self.nativeHandler.invokeRemoteFunction(genericEvent, eventName, eventFunction, genericService);
        }
    }

    isolated function dispatch(gmail:History history) returns @tainted error? {
        gmail:HistoryMessageAdded[]? messagesAdded = history.messagesAdded;
        if messagesAdded is gmail:HistoryMessageAdded[] {
            if messagesAdded.length() > 0 {
                foreach gmail:HistoryMessageAdded newMessage in messagesAdded {
                    gmail:Message? msg = newMessage.message;
                    if msg is gmail:Message && msg.labelIds is string[] {
                        foreach var labelId in <string[]>msg.labelIds {
                            match labelId {
                                INBOX => {
                                    check self.dispatchNewMessage(newMessage);
                                    check self.dispatchNewThread(newMessage);
                                }
                            }
                        }
                    }
                }
            }
        }
        gmail:HistoryLabelAdded[]? labelsAdded = history.labelsAdded;
        if labelsAdded is gmail:HistoryLabelAdded[] {
            if labelsAdded.length() > 0 {
                foreach gmail:HistoryLabelAdded addedlabel in labelsAdded {
                    check self.dispatchLabelAddedEmail(addedlabel);
                    check self.dispatchStarredEmail(addedlabel);
                }
            }
        }
        gmail:HistoryLabelRemoved[]? labelsRemoved = history.labelsRemoved;
        if labelsRemoved is gmail:HistoryLabelRemoved[] {
            if labelsRemoved.length() > 0 {
                foreach gmail:HistoryLabelRemoved removedLabel in labelsRemoved {
                    check self.dispatchLabelRemovedEmail(removedLabel);
                    check self.dispatchStarRemovedEmail(removedLabel);
                }
            }
        }
    }

    isolated function dispatchNewMessage(gmail:HistoryMessageAdded newMessage) returns @tainted error? {
        gmail:Message? msg = newMessage.message;
        if msg is () {
            return;
        }
        gmail:Message message = check readMessage(self.gmailConfig, <@untainted>msg.id);
        check self.executeRemoteFunc(message, "newEmail", "GmailService", "onNewEmail");
        MessageBodyPart[] msgAttachments = convertToMessageBodyParts(getAttachments(message));
        if (msgAttachments.length() > 0) {
            check self.dispatchNewAttachment(msgAttachments, message);
        }
    }

    isolated function dispatchNewAttachment(MessageBodyPart[] msgAttachments, gmail:Message message) returns error? {
        MailAttachment mailAttachment = {
            messageId: message.id,
            msgAttachments: msgAttachments
        };
        check self.executeRemoteFunc(mailAttachment, "newAttachment", "GmailService", "onNewAttachment");
    }

    isolated function dispatchNewThread(gmail:HistoryMessageAdded newMessage) returns @tainted error? {
        gmail:Message? msg = newMessage.message;
        if msg is () {
            return;
        }
        if (msg.id == msg.threadId) {
            gmail:MailThread thread = check readThread(self.gmailConfig, <@untainted>msg.threadId);
            check self.executeRemoteFunc(thread, "newThread", "GmailService", "onNewThread");
        }
    }

    isolated function dispatchLabelAddedEmail(gmail:HistoryLabelAdded addedlabel) returns @tainted error? {
        if addedlabel.labelIds !is string[] {
            return;
        }
        gmail:Message? msg = addedlabel.message;
        if msg is () {
            return;
        }
        gmail:Message message = check readMessage(self.gmailConfig, <@untainted>msg.id);
        ChangedLabel changedLabel = {
            messageDetail: message,
            changedLabelId: <string[]>addedlabel.labelIds
        };
        check self.executeRemoteFunc(changedLabel, "emailLabelAdded", "GmailService", "onEmailLabelAdded");
    }

    isolated function dispatchStarredEmail(gmail:HistoryLabelAdded addedlabel) returns @tainted error? {
        if (addedlabel.labelIds is string[]) {
            foreach var label in <string[]>addedlabel.labelIds {
                match label {
                    STARRED => {
                        gmail:Message? msg = addedlabel.message;
                        if msg is () {
                            return;
                        }
                        gmail:Message message = check readMessage(self.gmailConfig, <@untainted>msg.id);
                        check self.executeRemoteFunc(message, "emailStarred", "GmailService", "onEmailStarred");
                    }
                }
            }
        }
    }

    isolated function dispatchLabelRemovedEmail(gmail:HistoryLabelRemoved removedLabel) returns @tainted error? {
        if removedLabel.labelIds !is string[] {
            return;
        }
        gmail:Message? msg = removedLabel.message;
        if msg is () {
            return;
        }
        gmail:Message message = check readMessage(self.gmailConfig, <@untainted>msg.id);
        ChangedLabel changedLabel = {
            messageDetail: message,
            changedLabelId: <string[]>removedLabel.labelIds
        };
        check self.executeRemoteFunc(changedLabel, "emailLabelRemoved", "GmailService", "onEmailLabelRemoved");
    }

    isolated function dispatchStarRemovedEmail(gmail:HistoryLabelRemoved removedLabel) returns @tainted error? {
        if (removedLabel.labelIds is string[]) {
            foreach var label in <string[]>removedLabel.labelIds {
                match label {
                    STARRED => {
                        gmail:Message? msg = removedLabel.message;
                        if msg is () {
                            return;
                        }
                        gmail:Message message = check readMessage(self.gmailConfig, <@untainted>msg.id);
                        check self.executeRemoteFunc(message, "emailStarRemoved", "GmailService", "onEmailStarRemoved");
                    }
                }
            }
        }
    }
}
