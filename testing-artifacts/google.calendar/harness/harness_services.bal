import ballerina/io;

// Points the harness's OAuth refresh flow and (via the BASE_URL deviation in constants.bal)
// its Calendar API calls at the local mock server in ../mock/. Values otherwise don't matter -
// the mock doesn't validate them.
listener Listener calendarHarnessListener = check new (
    {
        calendarId: "primary",
        callbackURL: "http://localhost:8090/",
        clientId: "mock-client-id",
        clientSecret: "mock-client-secret",
        refreshUrl: "http://localhost:8091/oauth/token",
        refreshToken: "mock-refresh-token"
    },
    8090
);

service CalendarService on calendarHarnessListener {
    remote function onNewEvent(Event payload) returns error? {
        io:println("FIRED::CalendarService::onNewEvent");
    }
    remote function onEventUpdate(Event payload) returns error? {
        io:println("FIRED::CalendarService::onEventUpdate");
    }
    remote function onEventDelete(Event payload) returns error? {
        io:println("FIRED::CalendarService::onEventDelete");
    }
}
