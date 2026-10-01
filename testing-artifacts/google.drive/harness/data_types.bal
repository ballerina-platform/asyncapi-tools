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

# Listener Configuration. 
# + clientId - Client Id
# + clientSecret -Client Secret
# + refreshUrl - Refrest URL
# + refreshToken - Refresh Token
# + specificFolderOrFileId - Folder or file Id.  
# + domainVerificationFileContent - File content of HTML file used in domain verification.
# + callbackURL - Callback URL registered.  
# + channelRenewalConfig - Channel renewal configuration.
@display {label: "Listener Config"}
public type ListenerConfig record {
    @display {label: "Client Id", "description": "OAuth Credentials - Client ID"}
    string clientId;
    @display {label: "Client Secret", "description": "OAuth Credentials - Client Secret"}
    string clientSecret;
    @display {label: "Refresh URL", "description": "OAuth Credentials - Refresh URL"}
    string refreshUrl;
    @display {label: "Refresh Token", "description": "OAuth Credentials - Refresh Token"}
    string refreshToken;
    @display {label: "Callback URL", "description": "Callback service URL for webhook"}
    string callbackURL;
    @display {label: "Domain Verification File Content", "description": "text content of the google domain verification file"}
    string domainVerificationFileContent?;
    @display {label: "Specific Folder ID", "description": "Specific folder or file ID if we need to subscribe to single file/folder"}
    string specificFolderOrFileId?;
    ChannelRenewalConfig channelRenewalConfig?;
};

# Client configuration for cookies.
#
# + enabled - User agents provide users with a mechanism for disabling or enabling cookies
# + maxCookiesPerDomain - Maximum number of cookies per domain, which is 50
# + maxTotalCookieCount - Maximum number of total cookies allowed to be stored in cookie store, which is 3000
# + blockThirdPartyCookies - User can block cookies from third party responses and refuse to send cookies for third 
# party requests, if needed
public type CookieConfig record {|
    boolean enabled = false;
    int maxCookiesPerDomain = 50;
    int maxTotalCookieCount = 3000;
    boolean blockThirdPartyCookies = true;
|};

# Channel Renewal Configuration
#
# + retryCount - Maximum number of reties allowed to renew listener channel in seconds. (default : 20s)
# + retryInterval - Time between retries to renew listener channel in seconds. (default: 100s)  
# + leadTime - Time prior to expiration that renewal should happen happen. (default: 180s) 
# + domainVerificationDelay - Initial wait time for domain verification check in seconds. (default: 300s)  
@display {label: "Channel Renewal Config"}
public type ChannelRenewalConfig record {
    @display {label: "Retry Count"}
    int retryCount?;
    @display {label: "Retry Interval"}
    int retryInterval?;
    @display {label: "Lead Time"}
    int leadTime?;
    @display {label: "Domain Verification Delay"}
    int domainVerificationDelay?;
};

# Optional parameters for the watch files.
#
# + acknowledgeAbuse - Whether the user is acknowledging the risk of downloading known malware or other abusive files. 
# This is only applicable when alt=media. (Default: false)  
# + pageToken - Page token   
# + fields - The paths of the fields you want included in the response. If not specified, the response includes a 
# default set of fields specific to this method  
# + supportsAllDrives - Whether the requesting application supports both My Drives and shared drives. (Default: false)  
public type WatchFileOptional record {
    boolean acknowledgeAbuse?;
    string fields?;
    boolean? supportsAllDrives = true;
    string pageToken?;
};

# Represents the response of watch request. 
#
# + resourceId - A UUID or similar unique string that identifies this channel
# + address - The address where notifications are delivered for this channel
# + payload - A Boolean value to indicate whether the payload is needed
# + kind - Identifies this as a notification channel used to watch for changes to a resource, which is `api#channel`
# + expiration - Date and time of notification channel expiration, expressed as a Unix timestamp, in milliseconds
# + startPageToken - The starting page token for listing changes
# + id - A UUID or similar unique string that identifies this channel
# + resourceUri - A version-specific identifier for the watched resource
# + params - Additional parameters controlling delivery channel behavior  
# + type - The type of delivery mechanism used for this channel. Valid values are "web_hook" (or "webhook"). 
# Both values refer to a channel where Http requests are used to deliver messages
# + token - An arbitrary string delivered to the target address with each notification delivered over this channel
@display {label: "Watch Response"}
public type WatchResponse record {
    string kind?;
    string id?;
    string resourceId?;
    string resourceUri?;
    string token?;
    int expiration?;
    string 'type?;
    string address?;
    boolean payload?;
    string startPageToken?;
    StringKeyValuePairs params?;
};

# Record Type to accpet string values  
public type StringKeyValuePairs record {|
    string...;
|};

# Represents response from list changes request.
#
# + kind - Identifies what kind of resource this is. Value: the fixed string "drive#changeList"
# + nextPageToken - The page token for the next page of changes. This will be absent if the end of the changes list has 
# been reached. If the token is rejected for any reason, it should be discarded, and pagination should 
# be restarted from the first page of results
# + changes - The list of changes. If nextPageToken is populated, then this list may be incomplete and an additional 
# page of results should be fetched
# + newStartPageToken - The starting page token for future changes. This will be present only if the end of the current 
# changes list has been reached
public type ChangesListResponse record {
    string kind?;
    string nextPageToken?;
    string newStartPageToken?;
    Change[] changes?;
};

type StartPageTokenResponse record {
    string kind?;
    string startPageToken;
};

# A change to a file or shared drive.
#
# + kind - Identifies what kind of resource this is. Value: the fixed string "change" 
# + driveId - The ID of the shared drive associated with this change
# + removed - Whether the file or shared drive has been removed from this list of changes, for example by deletion or 
# loss of access
# + file - The updated state of the file. Present if the type is file and the file has not been removed from this 
# list of changes.
# + changeType - The type of the change. Possible values are file and drive  
# + time - The time of this change (RFC 3339 date-time) 
# + mimeType - The MIME type of the file
# + fileId - The ID of the file which has changed
public type Change record {
    string kind?;
    string changeType?;
    string mimeType?;
    string time?;
    boolean removed?;
    string fileId?;
    drive:File file?;
    string driveId?;
};

@display {label: "Changes list"}
public type ChangesListOptional record {
    string driveId?;
    string fields?;
    boolean includeCorpusRemovals?;
    boolean includeItemsFromAllDrives?;
    string includePermissionsForView?;
    boolean includeRemoved?;
    int pageSize?;
    boolean restrictToMyDrive?;
    string spaces?;
    boolean? supportsAllDrives = true;
};


public type GenericDataType Change;

