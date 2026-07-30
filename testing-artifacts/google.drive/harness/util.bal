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

import testorg/googleapis_drive_mockable as drive;
import ballerina/http;
import ballerina/log;
import ballerina/time;
import ballerina/url;
import ballerina/uuid;
import ballerinax/'client.config;

# Subscribes to all the changes or specific fileId.
# + callbackURL - Registered callback URL of the 
# + config - Client configurations.
# + fileId - FileId that you want to initiate watch operations. Optional. 
# Dont specify if you want TO trigger the listener for all the changes.
# + return - 'WatchResponse' on success and error if unsuccessful. 
isolated function startWatch(drive:ConnectionConfig config, string callbackURL, string? fileId = ())
                        returns @tainted WatchResponse|error {
    if (fileId is string) {
        // Watch for specified file changes
        return watchFilesById(config, fileId, callbackURL);
    } else {
        // Watch for all file changes.
        return watchFiles(config, callbackURL);
    }
}

# Stop all subscriptions for listening.
# + config - Client configurations.
# + channelUuid - UUID or other unique string you provided to identify this notification channel
# + watchResourceId - An opaque value that identifies the watched resource
#
# + return - Returns error, if unsuccessful.
isolated function stopWatchChannel(drive:ConnectionConfig config, string channelUuid, string watchResourceId)
                                    returns @tainted error? {
    boolean|error response = watchStop(config, channelUuid, watchResourceId);
    if (response is boolean) {
        log:printInfo("Watch channel stopped");
        return;
    } else {
        log:printInfo("Watch channel was not stopped");
        return response;
    }
}

# List changes by page token
# + config - Client configurations.
# + pageToken - The token for continuing a previous list request on the next page. This should be set to the value of 
# 'nextPageToken' from the previous response or to the response from the getStartPageToken method.
# + return - 'ChangesListResponse[]' on success and error if unsuccessful. 
isolated function getAllChangeList(string pageToken, drive:ConnectionConfig config)
                        returns @tainted ChangesListResponse[]|error {
    ChangesListResponse[] changeList = [];
    string? token = pageToken;
    while (token is string) {
        ChangesListResponse response = check listChanges(config, pageToken);
        changeList.push(response);
        token = response?.nextPageToken;
    }
    return changeList;
}

# Send GET request.
#
# + httpClient - Drive client
# + path - GET URI path
# + return - JSON or error if not suceeded
isolated function sendRequest(http:Client httpClient, string path) returns @tainted json|error {
    http:Response httpResponse = <http:Response>check httpClient->get(<@untainted>path);
    int statusCode = httpResponse.statusCode;
    json|http:ClientError jsonResponse = httpResponse.getJsonPayload();
    if (jsonResponse is json) {
        error? validateStatusCodeRes = validateStatusCode(jsonResponse, statusCode);
        if (validateStatusCodeRes is error) {
            return validateStatusCodeRes;
        }
        return jsonResponse;
    } else {
        return getDriveError(jsonResponse);
    }
}

# Prepare URL with Watch changes list optional parameters.
#
# + pageToken - The token for continuing a previous list request on the next page. This should be set to the value of 
# 'nextPageToken' from the previous response or to the response from the getStartPageToken method.
# + optional - Record that contains optional parameters
# + return - The prepared URL with encoded query
isolated function prepareUrlwithChangesListOptional(string pageToken, ChangesListOptional? optional = ()) returns string {
    string[] value = [];
    map<string> optionalMap = {};
    string path = prepareUrl([DRIVE_PATH, CHANGES]);
    optionalMap[PAGE_TOKEN] = pageToken.toString();
    if (optional is ChangesListOptional) {
        if (optional?.driveId is string) {
            optionalMap[DRIVE_ID] = optional?.driveId.toString();
        }
        if (optional?.fields is string) {
            optionalMap[FIELDS] = optional?.fields.toString();
        }
        if (optional?.supportsAllDrives is boolean) {
            optionalMap[SUPPORTS_ALL_DRIVES] = optional?.supportsAllDrives.toString();
        }
        if (optional?.includeCorpusRemovals is boolean) {
            optionalMap[INCLUDE_CORPUS_REMOVALS] = optional?.includeCorpusRemovals.toString();
        }
        if (optional?.includeItemsFromAllDrives is boolean) {
            optionalMap[INCLUDE_ITEMS_FROM_ALL_DRIVES] = optional?.includeItemsFromAllDrives.toString();
        }
        if (optional?.includePermissionsForView is string) {
            optionalMap[INCLUDE_PERMISSIONS_FOR_VIEW] = optional?.includePermissionsForView.toString();
        }
        if (optional?.includeRemoved is boolean) {
            optionalMap[INCLUDE_REMOVED] = optional?.includeRemoved.toString();
        }
        if (optional?.pageSize is int) {
            optionalMap[PAGE_SIZE] = optional?.pageSize.toString();
        }
        if (optional?.restrictToMyDrive is boolean) {
            optionalMap[RESTRICT_TO_MY_DRIVE] = optional?.restrictToMyDrive.toString();
        }
        if (optional?.spaces is string) {
            optionalMap[SPACES] = optional?.spaces.toString();
        }
    }
    foreach var val in optionalMap {
        value.push(val);
    }
    path = prepareQueryUrl([path], optionalMap.keys(), value);
    return path;
}

# Gets the starting pageToken for listing future changes 
#
# + httpClient - The HTTP Client
# + return - If successful, returns `string`. Else returns `error` 
isolated function getStartPageToken(http:Client httpClient) returns @tainted string|error {
    string path = prepareUrl([DRIVE_PATH, CHANGES, START_PAGE_TOKEN]);
    json jsonResponse = check sendRequest(httpClient, path);
    StartPageTokenResponse response = check jsonResponse.cloneWithType(StartPageTokenResponse);
    return response.startPageToken;
}

isolated function isCreated(string createdTime, string changeTime) returns boolean|error {
    boolean isCreated = false;
    time:Utc createdTimeUNIX = check time:utcFromString(createdTime);
    time:Utc changeTimeUNIX = check time:utcFromString(changeTime);
    time:Seconds due = time:utcDiffSeconds(changeTimeUNIX, createdTimeUNIX);
    log:printDebug("Due : " + due.toString());
    if (due <= 12d) {
        isCreated = true;
    }
    return isCreated;
}

isolated function isUpdated(string createdTime, string changeTime) returns boolean|error {
    boolean isModified = false;
    time:Utc createdTimeUNIX = check time:utcFromString(createdTime);
    time:Utc changeTimeUNIX = check time:utcFromString(changeTime);
    time:Seconds due = time:utcDiffSeconds(changeTimeUNIX, createdTimeUNIX);
    log:printDebug("Due : " + due.toString());
    if (due > 12d) {
        isModified = true;
    }
    return isModified;
}

# Validate for the existence of resources
#
# + folderId - Id that uniquely represents a folder. 
# + driveClient - Drive connecter client.
# + return - If unsuccessful, return error.
isolated function validateSpecificFolderExsistence(string folderId, drive:Client driveClient) returns @tainted error? {
    drive:File folder = check driveClient->getFile(folderId,
    "createdTime,modifiedTime,trashed,viewedByMeTime,viewedByMe");
    if (folder?.trashed == true) {
        fail error("Specific folder/file with Id :" + folderId + "had been removed to trashed");
    }
}

# Checking the MimeType to find folder. 
#
# + driveClient - Drive client connecter. 
# + specificParentFolderId - The Folder Id for the parent folder.
# + return - If successful, returns boolean. Else error.
isolated function checkMimeType(drive:Client driveClient, string specificParentFolderId)
                                    returns @tainted boolean|error {
    drive:File item = check driveClient->getFile(specificParentFolderId, "mimeType,trashed");
    if (item?.mimeType.toString() == FOLDER) {
        return true;
    } else {
        if (item?.trashed == true) {
            fail error("Already trashed file :" + specificParentFolderId);
        } else {
            return false;
        }

    }
}

# Subscribes to in a specific file.
#
# + httpClient - The HTTP Client
# + fileId - Id of the file that needs to be subscribed for watching
# + fileWatchRequest - 'WatchResponse' record as request body of the request.
# + optional - 'WatchFileOptional' object with optional params.
# + return - If successful, returns `WatchResponse`. Else returns `error` 
isolated function watchFilesUsingId(http:Client httpClient, string fileId, WatchResponse? fileWatchRequest = (),
                        WatchFileOptional? optional = ()) returns @tainted WatchResponse|error {
    string path = prepareUrlwithWatchFileOptional(optional, fileId);
    json payload = check fileWatchRequest.cloneWithType(json);
    json resp = check sendRequestWithPayload(httpClient, path, payload);
    WatchResponse response = check mapJsonToWatchResponse(<map<json>>resp);
    if (optional?.pageToken is string) {
        response.startPageToken = optional?.pageToken.toString();
    }
    return response;
}

# Send POST request with  a Payload.
#
# + httpClient - Drive client
# + path - POST URI path
# + jsonPayload - Payload of the request.
# + return - json or error if not suceeded.
isolated function sendRequestWithPayload(http:Client httpClient, string path, json jsonPayload) returns @tainted json|error {
    http:Request httpRequest = new;
    if (jsonPayload != ()) {
        httpRequest.setJsonPayload(<@untainted>jsonPayload);
    }
    http:Response httpResponse = <http:Response>check httpClient->post(<@untainted>path, httpRequest);
    int statusCode = httpResponse.statusCode;
    json|http:ClientError jsonResponse = httpResponse.getJsonPayload();
    if (jsonResponse is json) {
        error? validateStatusCodeRes = validateStatusCode(jsonResponse, statusCode);
        if (validateStatusCodeRes is error) {
            return validateStatusCodeRes;
        }
        return jsonResponse;
    } else {
        return getDriveError(jsonResponse);
    }
}

# Prepare URL with File Watch optional parameters.
#
# + fileId - File id
# + optional - Record that contains optional parameters
# + return - The prepared URL with encoded query
isolated function prepareUrlwithWatchFileOptional(WatchFileOptional? optional = (), string? fileId = ()) returns string {
    string[] value = [];
    map<string> optionalMap = {};
    string path = EMPTY_STRING;
    if (fileId is string) {
        path = prepareUrl([DRIVE_PATH, FILES, fileId, WATCH]);
    } else {
        path = prepareUrl([DRIVE_PATH, CHANGES, WATCH]);
    }
    if (optional is WatchFileOptional) {
        if (optional?.acknowledgeAbuse is boolean) {
            optionalMap[ACKKNOWLEDGE_ABUSE] = optional?.acknowledgeAbuse.toString();
        }
        if (optional?.fields is string) {
            optionalMap[FIELDS] = optional?.fields.toString();
        }
        if (optional?.supportsAllDrives is boolean) {
            optionalMap[SUPPORTS_ALL_DRIVES] = optional?.supportsAllDrives.toString();
        }
        if (optional?.pageToken is string) {
            optionalMap[PAGE_TOKEN] = optional?.pageToken.toString();
        }
        foreach var val in optionalMap {
            value.push(val);
        }
        path = prepareQueryUrl([path], optionalMap.keys(), value);
    }
    return path;
}

#
#
# + httpClient - The HTTP Client  
# + fileWatchRequest - 'WatchResponse' object  
# + optional - Optional
# + return - If successful, returns `WatchResponse`. Else returns `error`
isolated function watchAllFiles(http:Client httpClient, WatchResponse fileWatchRequest, WatchFileOptional? optional = ())
                        returns @tainted WatchResponse|error {
    string path = prepareUrlwithWatchFileOptional(optional);
    json payload = check fileWatchRequest.cloneWithType(json);
    json resp = check sendRequestWithPayload(httpClient, path, payload);
    WatchResponse response = check mapJsonToWatchResponse(<map<json>>resp);
    if (optional?.pageToken is string) {
        response.startPageToken = optional?.pageToken.toString();
    }
    return response;
}

# Stop the subscribtions 
#
# + httpClient - The HTTP Client
# + fileWatchRequest - Id of the file that needs to be subscribed for watching
# + return - If successful, returns `json`. Else returns `error` 
isolated function stopWatch(http:Client httpClient, WatchResponse fileWatchRequest) returns @tainted boolean|error {
    string path = prepareUrl([DRIVE_PATH, CHANNELS, STOP]);
    json payload = check fileWatchRequest.cloneWithType(json);
    boolean resp = check stopChannelRequest(httpClient, path, payload);
    return resp;
}

# List changes by page token 
#
# + httpClient - The HTTP Client
# + pageToken - The token for continuing a previous list request on the next page. This should be set to the value of 
# 'nextPageToken' from the previous response or to the response from the getStartPageToken method.
# + return - If successful, returns `json`. Else returns `error` 
isolated function listChangesByPageToken(http:Client httpClient, string pageToken)
                                            returns @tainted ChangesListResponse|error {
    string path = prepareUrlwithChangesListOptional(pageToken);
    json jsonResponse = check sendRequest(httpClient, path);
    ChangesListResponse response = check jsonResponse.cloneWithType(ChangesListResponse);
    return response;
}

# Prepare Validate Status Code.
#
# + response - JSON response fromthe request
# + statusCode - The Status code
# + return - Error Message
isolated function validateStatusCode(json response, int statusCode) returns error? {
    if (statusCode != http:STATUS_OK) {
        return getDriveError(response);
    }
}

# Formation of error message
#
# + errorResponse - Can be json or error type
# + return - error if not exist.
isolated function getDriveError(json|error errorResponse) returns error {
    if (errorResponse is json) {
        return error(errorResponse.toString());
    } else {
        return errorResponse;
    }
}

# Optional parameters used in listing changes
# + paths - An array of paths prefixes
# + return - The prepared URL
isolated function prepareUrl(string[] paths) returns string {
    string url = EMPTY_STRING;
    if (paths.length() > 0) {
        foreach var path in paths {
            if (!path.startsWith(FORWARD_SLASH)) {
                url = url + FORWARD_SLASH;
            }
            url = url + path;
        }
    }
    return <@untainted>url;
}

# Prepare URL with encoded query.
#
# + paths - An array of paths prefixes
# + queryParamNames - An array of query param names
# + queryParamValues - An array of query param values
# + return - The prepared URL with encoded query
isolated function prepareQueryUrl(string[] paths, string[] queryParamNames, string[] queryParamValues) returns string {
    string url = prepareUrl(paths);
    url = url + QUESTION_MARK;
    boolean first = true;
    int i = 0;
    foreach var name in queryParamNames {
        string value = queryParamValues[i];
        string|url:Error encoded = url:encode(value, ENCODING_CHARSET);
        if (encoded is string) {
            if (first) {
                url = url + name + EQUAL + encoded;
                first = false;
            } else {
                url = url + AMPERSAND + name + EQUAL + encoded;
            }
        } else {
            log:printError(UNABLE_TO_ENCODE + value, 'error = encoded);
            break;
        }
        i = i + 1;
    }
    return url;
}

# Stop channel watching.
#
# + httpClient - Drive client  
# + path - Path  
# + jsonPayload - Request payload
# + return - boolean or error if not suceeded, True if Deleted successfully.
isolated function stopChannelRequest(http:Client httpClient, string path, json jsonPayload) returns @tainted boolean|error {
    http:Request httpRequest = new;
    if (jsonPayload != ()) {
        httpRequest.setJsonPayload(<@untainted>jsonPayload);
    }
    http:Response httpResponse = <http:Response>check httpClient->post(<@untainted>path, httpRequest);
    if (httpResponse.statusCode == http:STATUS_NO_CONTENT) {
        return true;
    }
    json|http:ClientError jsonResponse = httpResponse.getJsonPayload();
    return getDriveError(jsonResponse);
}

isolated function getClient(drive:ConnectionConfig driveConfig) returns http:Client|error {
    drive:ConnectionConfig connectionConfig = driveConfig;
    connectionConfig.http1Settings = {chunking: http:CHUNKING_NEVER};
    http:ClientConfiguration httpClientConfig = check config:constructHTTPClientConfig(connectionConfig);
    return check new (BASE_URL, httpClientConfig);
}

isolated function listChanges(drive:ConnectionConfig driveConfig, string pageToken) returns @tainted ChangesListResponse|error {
    http:Client httpClient = check getClient(driveConfig);
    return listChangesByPageToken(httpClient, pageToken);
}

isolated function watchFilesById(drive:ConnectionConfig driveConfig, string fileId, string address, string? pageToken = (),
                        int? expiration = ()) returns @tainted WatchResponse|error {
    http:Client httpClient = check getClient(driveConfig);
    WatchResponse payload = {};
    payload.id = uuid:createType1AsString();
    string token = EMPTY_STRING;
    payload.'type = WEB_HOOK;
    payload.address = address;
    if (expiration is int) {
        payload.expiration = expiration;
    }
    if (pageToken is ()) {
        token = check getStartPageToken(httpClient);
    } else {
        token = pageToken;
    }
    WatchFileOptional optional = {supportsAllDrives: true, pageToken: token};
    return watchFilesUsingId(httpClient, fileId, payload, optional);
}

isolated function watchFiles(drive:ConnectionConfig driveConfig, string address, string? pageToken = (), int? expiration = ())
                            returns @tainted WatchResponse|error {
    http:Client httpClient = check getClient(driveConfig);
    WatchResponse payload = {};
    WatchFileOptional optional = {};
    string token = EMPTY_STRING;
    payload.id = uuid:createType1AsString();
    payload.'type = WEB_HOOK;
    payload.address = address;
    if (expiration is int) {
        payload.expiration = expiration;
    }
    if (pageToken is ()) {
        token = check getStartPageToken(httpClient);
    } else {
        token = pageToken;
    }
    optional = {pageToken: token};
    return watchAllFiles(httpClient, payload, optional);
}

isolated function watchStop(drive:ConnectionConfig driveConfig, string channelId, string resourceId)
                    returns @tainted boolean|error {
    http:Client httpClient = check getClient(driveConfig);
    WatchResponse payload = {};
    payload.id = channelId;
    payload.resourceId = resourceId;
    return stopWatch(httpClient, payload);
}

isolated function mapJsonToWatchResponse(map<json> jsonPayload) returns WatchResponse|error {
    WatchResponse response = {
        kind: jsonPayload["kind"].toString(),
        id: jsonPayload["id"].toString(),
        resourceId: jsonPayload["resourceId"].toString(),
        resourceUri: jsonPayload["resourceUri"].toString(),
        expiration: check int:fromString(jsonPayload["expiration"].toString())
    };
    return response;
}
