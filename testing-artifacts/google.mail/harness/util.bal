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
import ballerina/uuid;
import ballerina/http;
import ballerinax/googleapis.gmail as gmail;

// DEVIATION FROM PRODUCTION (documented in SUMMARY.md): the real ballerinax/googleapis.gmail
// Client.init() already exposes a `serviceUrl` parameter defaulting to the real Gmail API
// (https://gmail.googleapis.com/gmail/v1) - unlike googleapis.drive's connector, no cloning or
// patching is needed. readMessage/readThread/listHistory below pass this constant instead of
// relying on the default, so dispatch-critical connector calls land on the local mock too.
const string MOCK_GMAIL_SERVICE_URL = "http://localhost:8091/gmail/v1";

type mapJson map<json>;
isolated function createTopic(http:Client pubSubClient, string project, string pushEndpoint)
                                returns @tainted TopicSubscriptionDetail | error {
    string uuid = uuid:createType4AsString();
    string topicName = TOPIC_NAME_PREFIX + uuid;
    string subscriptionName = SUBSCRIPTION_NAME_PREFIX + uuid;
    Topic topic = check createPubsubTopic(pubSubClient, project,topicName);
    string topicResource = topic.name;
    log:printInfo(topicResource + " is created");
    if (topicResource !== "") {
        Policy existingPolicy = check getPubsubTopicIamPolicy(pubSubClient, <@untainted>topicResource);
        string etag = existingPolicy.etag;
        if (etag !== "") {
            Policy newPolicy = {
                                        'version: 1,
                                        etag: etag,
                                        bindings: [
                                            {
                                                role: ROLE,
                                                members: [
                                                         IAM_POLICY_BINDING_MEMBER
                                                        ]
                                            }
                                        ]
                                    };
            json newPolicyRequestbody = {
                                            "policy": newPolicy.toJson()
                                        };
            _ = check setPubsubTopicIamPolicy(pubSubClient, <@untainted>topicResource,
                                                                                    newPolicyRequestbody);
            string subscriptionResource = check createSubscription(pubSubClient, subscriptionName, project, pushEndpoint,
                                                                   topicResource);
            TopicSubscriptionDetail topicSubscriptionDetail = {
                                                                topicResource: topicResource,
                                                                subscriptionResource: subscriptionResource
                                                              };
            return topicSubscriptionDetail;
        }
    }
    return error(GMAIL_LISTENER_ERROR_CODE, message ="Could not setup a topic and subscription.");
}

isolated function createSubscription(http:Client pubSubClient, string subscriptionName, string project,
                                     string pushEndpoint, string topicResource) returns @tainted string | error {
    SubscriptionRequest subscriptionRequestbody  = {
                                    topic: topicResource,
                                    pushConfig: {
                                                    pushEndpoint: pushEndpoint
                                                }
                                };
    Subscription subscription = check subscribePubsubTopic(pubSubClient, project, subscriptionName,
                                                                                    subscriptionRequestbody);
    log:printInfo(subscription.name + " is created");
    return  subscription.name;
}


isolated function createPubsubTopic(http:Client pubSubClient, string project, string topic,
                                    TopicRequestBody? requestBody ={}) returns @tainted Topic | error {
    string path = PROJECTS + project + TOPICS + topic;
    http:Response httpResponse = <http:Response> check pubSubClient->put(path, requestBody.toJson());
    json jsonResponse = check handleResponse(httpResponse);
    return jsonResponse.cloneWithType(Topic);
}

isolated function getPubsubTopicIamPolicy(http:Client pubSubClient, string resourceName)
                                          returns @tainted Policy | error {
    string path = FORWARD_SLASH_SYMBOL + resourceName + GETIAMPOLICY;
    http:Response httpResponse = <http:Response> check pubSubClient->get(path);
    json jsonResponse = check handleResponse(httpResponse);
    return jsonResponse.cloneWithType(Policy);
}

isolated function setPubsubTopicIamPolicy(http:Client pubSubClient, string resourceName, json requestBody)
                                          returns @tainted Policy | error {
    string path = FORWARD_SLASH_SYMBOL + resourceName + SETIAMPOLICY;
    http:Response httpResponse = <http:Response> check pubSubClient->post(path, requestBody);
    json jsonResponse = check handleResponse(httpResponse);
    return jsonResponse.cloneWithType(Policy);
}

isolated function subscribePubsubTopic(http:Client pubSubClient, string project, string subscription,
                                       SubscriptionRequest requestBody) returns @tainted Subscription | error {
    string path = PROJECTS + project + SUBSCRIPTIONS + subscription;
    http:Response httpResponse = <http:Response> check pubSubClient->put(path, requestBody.toJson());
    json jsonResponse = check handleResponse(httpResponse);
    return jsonResponse.cloneWithType(Subscription);
}

isolated function deletePubsubTopic(http:Client pubSubClient, string topic) returns @tainted json | error {
    string path = FORWARD_SLASH_SYMBOL + topic;
    http:Response httpResponse = <http:Response> check pubSubClient->delete(path);
    json jsonResponse = check handleResponse(httpResponse);
    return jsonResponse;
}

isolated function deletePubsubSubscription(http:Client pubSubClient, string subscription) returns @tainted json | error {
    string path = FORWARD_SLASH_SYMBOL + subscription;
    http:Response httpResponse = <http:Response> check pubSubClient->delete(path);
    json jsonResponse = check handleResponse(httpResponse);
    return jsonResponse;
}

isolated function handleResponse(http:Response httpResponse) returns @tainted json|error {
    if (httpResponse.statusCode == http:STATUS_NO_CONTENT) {
        //If status 204, then no response body. So returns json boolean true.
        return true;
    }
    var jsonResponse = httpResponse.getJsonPayload();
    if (jsonResponse is json) {
        if (httpResponse.statusCode == http:STATUS_OK) {
            //If status is 200, request is successful. Returns resulting payload.
            return jsonResponse;
        } else if (httpResponse.statusCode == http:STATUS_CONFLICT) {
            //If status is 409, request has conflict. Returns error message.
            json conflictResponseJson = check httpResponse.getJsonPayload();
            map<json> conflictResponse = <map<json>>conflictResponseJson;
            if (conflictResponse.hasKey("error")) {
                PubSubError pubSubError = check conflictResponse["error"].cloneWithType(PubSubError);
                error err = error(GMAIL_LISTENER_ERROR_CODE, message = pubSubError?.message);
                return err;
            }
            return error(GMAIL_LISTENER_ERROR_CODE, message = conflictResponseJson);
        } else {
            //If status is not 200 or 204, request is unsuccessful. Returns error.
            error err = error(GMAIL_LISTENER_ERROR_CODE, message = jsonResponse);
            return err;
        }
    } else {
        error err = error(GMAIL_LISTENER_ERROR_CODE, message =
            "Error occurred while accessing the JSON payload of the response", 'error= jsonResponse);
        return err;
    }
}

// Gmail watch and stop Functions

isolated function watch(http:Client gmailHttpClient, string userId, WatchRequestBody requestBody)
                        returns @tainted WatchResponse | error {
    http:Request request = new;
    string watchPath = USER_RESOURCE + userId + WATCH;
    request.setJsonPayload(requestBody.toJson());
    http:Response httpResponse = <http:Response> check gmailHttpClient->post(watchPath, request);
    json jsonWatchResponse = check handleResponse(httpResponse);
    WatchResponse watchResponse = check jsonWatchResponse.cloneWithType(WatchResponse);
    return watchResponse;
}

isolated function stop(http:Client gmailHttpClient, string userId) returns @tainted error? {
    http:Request request = new;
    string stopPath = USER_RESOURCE + userId + STOP;
    return check gmailHttpClient->post(stopPath, request);
}

# Retrieves whether the particular remote method is available.
#
# + methodName - Name of the required method
# + methods - All available methods
# + return - `true` if method available or else `false`
isolated function isMethodAvailable(string methodName, string[] methods) returns boolean {
    boolean isAvailable = methods.indexOf(methodName) is int;
    if (isAvailable) {
        var index = methods.indexOf(methodName);
        if (index is int) {
            _ = methods.remove(index);
        }
    }
    return isAvailable;
}

isolated function readMessage(gmail:ConnectionConfig gmailConfig, string messageId, string? userId = ())
                              returns @tainted gmail:Message|error {
    string userEmailId = ME;
    if (userId is string) {
        userEmailId = userId;
    }
    gmail:Client gmailClient = check new (gmailConfig, MOCK_GMAIL_SERVICE_URL);
    return gmailClient->/users/[userEmailId]/messages/[messageId]();
}

isolated function readThread(gmail:ConnectionConfig gmailConfig, string threadId, string? userId = ())
                             returns @tainted gmail:MailThread|error {
    string userEmailId = ME;
    if (userId is string) {
        userEmailId = userId;
    }
    gmail:Client gmailClient = check new (gmailConfig, MOCK_GMAIL_SERVICE_URL);
    return gmailClient->/users/[userEmailId]/threads/[threadId]();
}

isolated function listHistory(gmail:ConnectionConfig gmailConfig, string startHistoryId, string? labelId = (),
                              int? maxResults = (), string? pageToken = (), string? userId = ())
                              returns @tainted gmail:ListHistoryResponse|error {
    string userEmailId = ME;
    if (userId is string) {
        userEmailId = userId;
    }
    gmail:Client gmailClient = check new (gmailConfig, MOCK_GMAIL_SERVICE_URL);
    return gmailClient->/users/[userEmailId]/history(startHistoryId = startHistoryId, labelId = labelId,
            maxResults = maxResults, pageToken = pageToken);
}

# Extracts attachment parts from a message.
#
# + message - The Gmail message
# + return - Array of message parts that are attachments
isolated function getAttachments(gmail:Message message) returns gmail:MessagePart[] {
    gmail:MessagePart? payload = message.payload;
    if payload is () {
        return [];
    }
    gmail:MessagePart[]? parts = payload.parts;
    if parts is () {
        return [];
    }
    gmail:MessagePart[] attachments = [];
    foreach gmail:MessagePart part in parts {
        string? filename = part.filename;
        if filename is string && filename != "" {
            attachments.push(part);
        }
    }
    return attachments;
}

# Converts gmail:MessagePart array to MessageBodyPart array to maintain public API compatibility.
#
# + parts - Array of gmail:MessagePart
# + return - Array of MessageBodyPart
isolated function convertToMessageBodyParts(gmail:MessagePart[] parts) returns MessageBodyPart[] {
    MessageBodyPart[] bodyParts = [];
    foreach gmail:MessagePart part in parts {
        MessageBodyPart bodyPart = {
            partId: part.partId,
            mimeType: part.mimeType,
            bodyHeaders: part.headers,
            fileId: part.attachmentId,
            fileName: part.filename,
            data: part.data,
            size: part.size
        };
        bodyParts.push(bodyPart);
    }
    return bodyParts;
}
