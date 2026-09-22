import ballerina/io;

// Points the harness's OAuth refresh flow and (via the BASE_URL deviation in constants.bal, plus
// the patched connector's baseUrl field) its Drive API calls at the local mock server in
// ../mock/. Values otherwise don't matter - the mock doesn't validate them.
listener Listener driveHarnessListener = check new (
    {
        clientId: "mock-client-id",
        clientSecret: "mock-client-secret",
        refreshUrl: "http://localhost:8092/oauth/token",
        refreshToken: "mock-refresh-token",
        callbackURL: "http://localhost:8090/"
    },
    8090
);

service DriveService on driveHarnessListener {
    remote function onFileCreate(Change changeInfo) returns error? {
        io:println("FIRED::DriveService::onFileCreate");
    }
    remote function onFolderCreate(Change changeInfo) returns error? {
        io:println("FIRED::DriveService::onFolderCreate");
    }
    remote function onFileUpdate(Change changeInfo) returns error? {
        io:println("FIRED::DriveService::onFileUpdate");
    }
    remote function onFolderUpdate(Change changeInfo) returns error? {
        io:println("FIRED::DriveService::onFolderUpdate");
    }
    remote function onDelete(Change changeInfo) returns error? {
        io:println("FIRED::DriveService::onDelete");
    }
    remote function onFileTrash(Change changeInfo) returns error? {
        io:println("FIRED::DriveService::onFileTrash");
    }
    remote function onFolderTrash(Change changeInfo) returns error? {
        io:println("FIRED::DriveService::onFolderTrash");
    }
}
