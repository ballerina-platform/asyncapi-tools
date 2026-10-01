import ballerina/io;

// google.sheets needs no mock server and no deviation from production at all: the trigger
// dispatches directly from the inbound POST body (checking spreadsheetId, then matching on
// eventType), with zero outbound calls of its own. This listener just needs a matching
// spreadsheetId to accept requests against.
listener Listener sheetsHarnessListener = check new (
    {
        spreadsheetId: "test-spreadsheet-id"
    },
    8090
);

service SheetRowService on sheetsHarnessListener {
    remote function onAppendRow(GSheetEvent payload) returns error? {
        io:println("FIRED::SheetRowService::onAppendRow");
    }
    remote function onUpdateRow(GSheetEvent payload) returns error? {
        io:println("FIRED::SheetRowService::onUpdateRow");
    }
}
