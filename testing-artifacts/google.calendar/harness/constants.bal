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

// Header constants
const string GOOGLE_CHANNEL_ID = "X-Goog-Channel-ID";
const string GOOGLE_RESOURCE_ID = "X-Goog-Resource-ID";
const string GOOGLE_RESOURCE_STATE = "X-Goog-Resource-State";
const string SYNC = "sync";

// Path Constants
// DEVIATION FROM PRODUCTION (documented in SUMMARY.md): points at the local mock Google API
// server (testing-artifacts/google.calendar/mock/) instead of the real https://www.googleapis.com,
// since that base URL is otherwise hardcoded with no config-based way to redirect it.
const string BASE_URL = "http://localhost:8091";
const string CALENDAR_PATH = "/calendar/v3";
const string CALENDAR = "calendars";
const string CHANNELS = "channels";
const string STOP = "stop";
const string EVENTS = "events";
const string WATCH = "watch";
const string WEBHOOK = "webhook";

// Symbols
const string QUESTION_MARK = "?";
const string EQUAL_SIGN = "=";
const string EMPTY_STRING = "";
const string AMPERSAND = "&";
const string FORWARD_SLASH = "/";
const string PAYLOAD = " payload: ";

// Error constants
const string HTTP_ERROR_MSG = "Error occurred while getting the HTTP response : ";
const string ERR_EXTRACTING_ERROR_MSG = "Error occured while extracting errors from payload.";
const string JSON_ACCESSING_ERROR_MSG = "Error occurred while accessing the JSON payload of the response.";
const string ERR_WATCH_RESPONSE = "Error occurred while constructing WatchResponse record.";
const string ERR_CHANNEL_REGISTRATION = "Unable to register new channel.";
const string ERR_SCHEDULE = "Unable to schedule subscription renewal.";
const string INVALID_ID_ERROR = "Invalid channel id/ resource id";

// Warn constants
const string WARN_CHANNEL_REGISTRATION = "Could not register watch channel";
const string WARN_SCHEDULE = "Could not schedule subscription renewal";

// Info constants
const string INFO_RETRY_CHANNEL_REGISTRATION = "Retrying to register new channel. Attempt - ";
const string INFO_RETRY_SCHEDULE = "Retrying to schedule subscription renewal. Attempt - ";

// Method names
const string ON_NEW_EVENT = "onNewEvent";
const string ON_EVENT_UPDATE = "onEventUpdate";
const string ON_EVENT_DELETE = "onEventDelete";
