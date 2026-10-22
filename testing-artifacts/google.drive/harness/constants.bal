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

//Base URL
// DEVIATION FROM PRODUCTION (documented in SUMMARY.md): points util.bal's own httpClient (used
// for watch/changes calls) and, via listener.bal/watch_scheduler.bal's driveConnection.baseUrl,
// the patched connector's getFile() calls at the local mock instead of the real
// https://www.googleapis.com.
const string BASE_URL = "http://localhost:8092";

// MimeTypes
const string FOLDER = "application/vnd.google-apps.folder";
const string FILE = "application/vnd.google-apps.file";
const string SPREADSHEET = "application/vnd.google-apps.spreadsheet";
const string DOCS = "application/vnd.google-apps.document";
const string GOOGLE_DRAWING = "application/vnd.google-apps.drawing";
const string FORM = "application/vnd.google-apps.form";
const string PHOTO = "application/vnd.google-apps.photo";
const string PRESENTATION = "application/vnd.google-apps.presentation";
const string VIDEO = "application/vnd.google-apps.video";
const string AUDIO = "application/vnd.google-apps.audio";
const string LOG_FILE = "text/x-log";
const string TEXT_FILE = "text/plain";

// Header constants
const string GOOGLE_CHANNEL_ID = "X-Goog-Channel-ID";
const string GOOGLE_RESOURCE_ID = "X-Goog-Resource-ID";
const string GOOGLE_RESOURCE_STATE = "X-Goog-Resource-State";
const string SYNC = "sync";
const string CONTENT_TYPE = "Content-Type";

// Watch 
const decimal MAX_EXPIRATION_TIME_FOR_FILE_RESOURCE = 86400;
const decimal MAX_EXPIRATION_TIME_FOR_CHANGES_ALL_DRIVE = 604800;

// Optional Query Parameters
const string ACKKNOWLEDGE_ABUSE = "acknowledgeAbuse";
const string FIELDS = "fields";
const string INCLUDE_PERMISSIONS_FOR_VIEW = "includePermissionsForView";
const string SUPPORTS_ALL_DRIVES = "supportsAllDrives";
const string IGNORE_DEFAULT_VISIBILITY = "ignoreDefaultVisibility";
const string KEEP_REVISION_FOREVER = "keepRevisionForever";
const string OCR_LANGUAGE = "ocrLanguage";
const string ADD_PARENTS = "addParents";
const string REMOVE_PARENTS = "removeParents";
const string USE_CONTENT_AS_INDEXABLE_TEXT = "useContentAsIndexableText";
const string CORPORA = "corpora";
const string DRIVE_ID = "driveId";
const string ORDER_BY = "orderBy";
const string PAGE_SIZE = "pageSize";
const string SPACES = "spaces";
const string INCLUDE_ITEMS_FROM_ALL_DRIVES = "includeItemsFromAllDrives";
const string INCLUDE_CORPUS_REMOVALS = "includeCorpusRemovals";
const string INCLUDE_REMOVED = "includeRemoved";
const string RESTRICT_TO_MY_DRIVE = "restrictToMyDrive";

// Drive
const string DRIVE_URL = "https://www.googleapis.com";
public const string REFRESH_URL = "https://www.googleapis.com/oauth2/v3/token";
const string DRIVE_PATH = "/drive/v3";
const string ABOUT = "/about";
const string UPLOAD = "/upload";
const string UPLOAD_TYPE = "uploadType";
const string TYPE_MEDIA = "media";
const string TYPE_MULTIPART = "multipart";
const string TYPE_RESUMABLE = "resumable";
const string FILES = "/files";
const string CHANGES = "/changes";
const string WATCH = "/watch";
const string COPY = "/copy";
const string CHANNELS = "/channels";
const string STOP = "/stop";
const string Q = "q";
const string AMPERSAND = "&";
const string PAGE_TOKEN = "pageToken";
const string WEB_CONTENT_LINK = "webContentLink";
const string WEB_HOOK = "web_hook";
const string START_PAGE_TOKEN = "startPageToken";

//Symbols
const string QUESTION_MARK = "?";
const string PATH_SEPARATOR = "/";
const string EMPTY_STRING = "";
const string WHITE_SPACE = " ";
const string FORWARD_SLASH = "/";
const string DASH_WITH_WHITE_SPACES_SYMBOL = " - ";
const string COLON = ":";
const string EXCLAMATION_MARK = "!";
const string EQUAL = "=";
const string _ALL = "*";
const string TRUE = "true";
const string FALSE = "false";

// URL encoding
const string ENCODING_CHARSET = "utf-8";
const string UNABLE_TO_ENCODE = "Unable to encode value: ";

// Error
const string ERR_CHANNEL_REGISTRATION = "Unable to register new channel.";
const string ERR_SCHEDULE = "Unable to schedule subscription renewal.";
const string ERR_UNAUTHORIZED_WEBHOOK_CHANNEL = "Unauthorized WebHook callback channel";
const string ERR_FILE_NOT_FOUND ="File not found";
const string ERR_INVALID_FILE_OR_FOLDER ="Cannot register watch to file or folder specified";

// Warn constants
const string WARN_CHANNEL_REGISTRATION = "Could not register watch channel";
const string WARN_SCHEDULE = "Could not schedule subscription renewal";

// Info constants
const string INFO_RETRY_CHANNEL_REGISTRATION = "Retrying to register new channel. Attempt - ";
const string INFO_RETRY_SCHEDULE = "Retrying to schedule subscription renewal. Attempt - ";
const string GOOGLE_SITE_VERIFICATION_PREFIX = "google-site-verification";
const string CHANNEL_REGISTRATION_IN_PROGRESS = "Google verification in progress...";
const string REQUEST_DOMAIN_VERIFICATION = "Please register the callback URL " +
"by visiting https://console.cloud.google.com/apis/credentials/domainverification";
