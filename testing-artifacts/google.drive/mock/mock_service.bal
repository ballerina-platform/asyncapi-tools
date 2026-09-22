import ballerina/http;

// Fixed watch-channel identifiers returned by the fake watch endpoint. The harness's dispatcher
// stores these after registerWatchChannel() and only accepts inbound requests whose
// X-Goog-Channel-ID / X-Goog-Resource-ID headers match them - the driver must echo these exact
// values.
const string CHANNEL_ID = "test-channel-id";
const string RESOURCE_ID = "test-resource-id";

// One fixed file/folder ID reused across every scenario - only one scenario is ever "active" at
// a time (set by the driver via POST /control/scenario before each dispatch POST), so there's no
// need to disambiguate by ID.
const string MOCK_FILE_ID = "mock-file-id";

const string MIME_FOLDER = "application/vnd.google-apps.folder";
const string MIME_FILE = "text/plain";

// Fixed change timestamp every scenario's Change entry uses. Paired against each scenario's
// createdTime (below) to land on the correct side of the connector's exact 12-second
// isCreated/isUpdated boundary (util.bal: due <= 12s -> created, > 12s -> updated).
const string CHANGE_TIME = "2026-01-01T00:00:10Z";

// Which scenario the next /changes and /files/{id}?fields=... calls should reflect. Set by the
// driver via POST /control/scenario before each dispatch test POST to the harness.
isolated string currentScenario = "file_create";

service / on new http:Listener(8092) {

    // Fakes the OAuth2 refresh-token exchange the http:Client's auth handler performs lazily on
    // the first authenticated call. Request body isn't validated - any refresh attempt succeeds.
    resource function post oauth/token(http:Caller caller, http:Request req) returns error? {
        json body = {"access_token": "mock-access-token", "token_type": "Bearer", "expires_in": 3600};
        check caller->respond(body);
    }

    // Fakes GET /drive/v3/changes/startPageToken - called once during watch registration.
    resource function get drive/v3/changes/startPageToken(http:Caller caller, http:Request req) returns error? {
        json body = {"kind": "drive#startPageToken", "startPageToken": "1"};
        check caller->respond(body);
    }

    // Fakes POST /drive/v3/changes/watch - whole-drive watch registration (watchFiles(), not
    // watchFilesById() - this harness deliberately doesn't exercise specific-file/folder watch
    // mode, see SUMMARY.md).
    resource function post drive/v3/changes/watch(http:Caller caller, http:Request req) returns error? {
        json watchResponse = {
            "kind": "api#channel",
            "id": CHANNEL_ID,
            "resourceId": RESOURCE_ID,
            "resourceUri": "http://localhost:8092/drive/v3/changes?pageToken=1",
            "expiration": "9999999999999"
        };
        // util.bal's validateStatusCode requires exactly 200, not any 2xx - a bare
        // caller->respond(json) on a POST defaults to 201, which would otherwise be
        // misinterpreted as a Drive API error (same 200-vs-201 bug class as #8920).
        http:Response res = new;
        res.statusCode = http:STATUS_OK;
        res.setJsonPayload(watchResponse);
        check caller->respond(res);
    }

    // Fakes GET /drive/v3/changes?pageToken=... - returns one Change entry shaped for whichever
    // scenario is currently selected.
    resource function get drive/v3/changes(http:Caller caller, http:Request req) returns error? {
        string scenario;
        lock {
            scenario = currentScenario;
        }
        string mimeType = scenario.startsWith("folder") ? MIME_FOLDER : MIME_FILE;
        boolean removed = scenario == "delete";
        json changeEntry = {
            "kind": "drive#change",
            "changeType": "file",
            "time": CHANGE_TIME,
            "removed": removed,
            "fileId": MOCK_FILE_ID,
            "file": {
                "mimeType": mimeType
            }
        };
        json response = {
            "kind": "drive#changeList",
            "newStartPageToken": "2",
            "changes": [changeEntry]
        };
        check caller->respond(response);
    }

    // Fakes GET /drive/v3/files/{fileId} - both the bare call (mapEvents: existence/mimeType
    // gate, and the delete-detection failure) and the fields-restricted call
    // (identifyFileEvent/identifyFolderEvent: createdTime/trashed comparison), disambiguated by
    // whether a "fields" query param is present.
    resource function get drive/v3/files/[string fileId](http:Caller caller, http:Request req) returns error? {
        string scenario;
        lock {
            scenario = currentScenario;
        }
        string? fields = req.getQueryParamValue("fields");

        if fields is () {
            // Bare call. Every scenario except "delete" must succeed (any valid File JSON is
            // fine - mapEvents takes mimeType from the Change entry, not from this response).
            // "delete" must fail, since onDelete only fires when changeLog.removed is true AND
            // this call itself errors.
            if scenario == "delete" {
                http:Response res = new;
                res.statusCode = http:STATUS_NOT_FOUND;
                res.setJsonPayload({"error": {"code": 404, "message": "File not found"}});
                check caller->respond(res);
                return;
            }
            check caller->respond({"kind": "drive#file", "id": fileId, "mimeType": MIME_FILE});
            return;
        }

        // Fields-restricted call. createdTime is set relative to CHANGE_TIME to land on the
        // correct side of the connector's 12-second isCreated/isUpdated boundary; trashed is set
        // directly for the trash scenarios (checked before the isUpdated comparison).
        string createdTime;
        boolean trashed = false;
        if scenario.endsWith("_create") {
            createdTime = "2026-01-01T00:00:05Z"; // 5s before CHANGE_TIME -> isCreated
        } else if scenario.endsWith("_trash") {
            createdTime = "2025-01-01T00:00:00Z"; // long before CHANGE_TIME, but trashed wins
            trashed = true;
        } else {
            createdTime = "2025-01-01T00:00:00Z"; // long before CHANGE_TIME -> isUpdated
        }
        json fileResponse = {
            "kind": "drive#file",
            "id": fileId,
            "mimeType": scenario.startsWith("folder") ? MIME_FOLDER : MIME_FILE,
            "createdTime": createdTime,
            "modifiedTime": CHANGE_TIME,
            "trashed": trashed,
            "parents": []
        };
        check caller->respond(fileResponse);
    }

    // Fakes POST /drive/v3/channels/stop
    resource function post drive/v3/channels/stop(http:Caller caller, http:Request req) returns error? {
        http:Response res = new;
        res.statusCode = http:STATUS_NO_CONTENT;
        check caller->respond(res);
    }

    // Test-only control endpoint - the driver calls this before each POST to the harness to
    // select which scenario the next /changes and /files/{id} calls should reflect. Not part of
    // Google's API. Valid values: file_create, folder_create, file_update, folder_update,
    // file_trash, folder_trash, delete.
    resource function post control/scenario(http:Caller caller, http:Request req) returns error? {
        json body = check req.getJsonPayload();
        string scenario = check body.scenario;
        lock {
            currentScenario = scenario;
        }
        check caller->respond({"status": "ok", "scenario": scenario});
    }
}
