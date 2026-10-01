import ballerina/http;
import ballerina/io;
import ballerina/time;

// Fixed watch-channel identifiers returned by the fake /events/watch endpoint. The harness's
// dispatcher stores these after registerWatchChannel() and only accepts inbound requests whose
// X-Goog-Channel-ID / X-Goog-Resource-ID headers match them - the driver must echo these exact
// values.
const string CHANNEL_ID = "test-channel-id";
const string RESOURCE_ID = "test-resource-id";

// Which event fixture the next GET .../events call should return. Set by the driver via
// POST /control/scenario before each dispatch test POST to the harness.
isolated string currentScenario = "new";

service / on new http:Listener(8091) {

    // Fakes the OAuth2 refresh-token exchange the http:Client's auth handler performs lazily on
    // the first authenticated call. Request body isn't validated - any refresh attempt succeeds.
    resource function post oauth/token(http:Caller caller, http:Request req) returns error? {
        json body = {"access_token": "mock-access-token", "token_type": "Bearer", "expires_in": 3600};
        check caller->respond(body);
    }

    // Fakes POST /calendar/v3/calendars/{calendarId}/events/watch
    resource function post calendar/v3/calendars/[string calendarId]/events/watch(http:Caller caller,
            http:Request req) returns error? {
        time:Utc now = time:utcNow();
        int nowMillis = now[0] * 1000;
        int expirationMillis = nowMillis + 3600000; // +1h - well beyond a short test run
        json watchResponse = {
            "kind": "api#channel",
            "id": CHANNEL_ID,
            "resourceId": RESOURCE_ID,
            "resourceUri": string `http://localhost:8091/calendar/v3/calendars/${calendarId}/events`,
            "expiration": expirationMillis.toString()
        };
        check caller->respond(watchResponse);
    }

    // Fakes GET /calendar/v3/calendars/{calendarId}/events - returns whichever scenario's
    // fixture from ../payloads/ is currently selected.
    resource function get calendar/v3/calendars/[string calendarId]/events(http:Caller caller,
            http:Request req) returns error? {
        string scenario;
        lock {
            scenario = currentScenario;
        }
        string fixturePath = string `../payloads/event_response_${scenario}.json`;
        json fixture = check io:fileReadJson(fixturePath);
        check caller->respond(fixture);
    }

    // Fakes POST /calendar/v3/channels/stop
    resource function post calendar/v3/channels/stop(http:Caller caller, http:Request req) returns error? {
        http:Response res = new;
        res.statusCode = http:STATUS_NO_CONTENT;
        check caller->respond(res);
    }

    // Test-only control endpoint - the driver calls this before each POST to the harness to
    // select which event fixture the next /events call should return. Not part of Google's API.
    resource function post control/scenario(http:Caller caller, http:Request req) returns error? {
        json body = check req.getJsonPayload();
        string scenario = check body.scenario;
        lock {
            currentScenario = scenario;
        }
        check caller->respond({"status": "ok", "scenario": scenario});
    }
}
