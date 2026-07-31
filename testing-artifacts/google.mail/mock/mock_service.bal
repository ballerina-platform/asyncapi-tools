import ballerina/http;
import ballerina/io;
import ballerina/time;

// Which scenario's fixtures the next GET .../history, .../messages/{id}, and .../threads/{id}
// calls should return. Set by the driver via POST /control/scenario before each dispatch test.
isolated string currentScenario = "scenario1";

// Fixed subscription resource name always echoed back by the subscription-create call below,
// regardless of the (uuid-generated, not knowable in advance) subscription name the trigger
// actually requests. The driver uses this exact value as the "subscription" field of its
// simulated Pub/Sub push, matching what the trigger stores as self.subscriptionResource during
// init() and checks against on every inbound POST.
const string FIXED_SUBSCRIPTION_RESOURCE = "projects/mock-project/subscriptions/test-subscription-fixed";

service / on new http:Listener(8091) {

    // Fakes the OAuth2 refresh-token exchange both the pubSubClient/gmailHttpClient http:Clients
    // and the googleapis.gmail connector's underlying oas:Client perform lazily on first call.
    // Request body isn't validated - any refresh attempt succeeds.
    resource function post oauth/token(http:Caller caller, http:Request req) returns error? {
        http:Response res = new;
        res.statusCode = http:STATUS_OK;
        res.setJsonPayload({"access_token": "mock-access-token", "token_type": "Bearer", "expires_in": 3600});
        check caller->respond(res);
    }

    // Fakes PUT /projects/{project}/topics/{topic} (Pub/Sub topic creation, 1st of 4 startup
    // calls). The exact echoed name doesn't matter - it's only threaded through to the Gmail
    // watch request body, which this mock doesn't validate.
    resource function put pubsub/v1/projects/[string project]/topics/[string topic](http:Caller caller,
            http:Request req) returns error? {
        json topicResponse = {"name": string `projects/${project}/topics/${topic}`};
        check caller->respond(topicResponse);
    }

    // Fakes GET /{topicResource}:getIamPolicy (2nd startup call). Ballerina path params match a
    // whole `/`-delimited segment, so the "topic:getIamPolicy" suffix arrives as one string in
    // topicAction - not parsed further since the etag is all that's needed.
    resource function get pubsub/v1/projects/[string project]/topics/[string topicAction](http:Caller caller,
            http:Request req) returns error? {
        json policyResponse = {"etag": "mock-etag-123"};
        check caller->respond(policyResponse);
    }

    // Fakes POST /{topicResource}:setIamPolicy (3rd startup call). Response is mostly ignored by
    // the trigger, just needs to parse as a Policy.
    resource function post pubsub/v1/projects/[string project]/topics/[string topicAction](http:Caller caller,
            http:Request req) returns error? {
        http:Response res = new;
        res.statusCode = http:STATUS_OK;
        res.setJsonPayload({"etag": "mock-etag-123", "version": 1, "bindings": []});
        check caller->respond(res);
    }

    // Fakes PUT /projects/{project}/subscriptions/{subscription} (4th startup call) - always
    // returns the fixed resource name above regardless of the requested subscription name.
    resource function put pubsub/v1/projects/[string project]/subscriptions/[string subscription](
            http:Caller caller, http:Request req) returns error? {
        json subscriptionResponse = {
            "name": FIXED_SUBSCRIPTION_RESOURCE,
            "topic": string `projects/${project}/topics/mock-topic`,
            "pushConfig": {"pushEndpoint": "http://localhost:8090/"}
        };
        check caller->respond(subscriptionResponse);
    }

    // Fakes POST /v1/users/{userId}/watch (5th and final startup call, via gmailHttpClient on
    // GMAIL_BASE_URL).
    resource function post gmail/v1/users/[string userId]/watch(http:Caller caller,
            http:Request req) returns error? {
        time:Utc now = time:utcNow();
        int nowMillis = now[0] * 1000;
        int expirationMillis = nowMillis + 3600000; // +1h - well beyond a short test run
        http:Response res = new;
        res.statusCode = http:STATUS_OK;
        res.setJsonPayload({"historyId": "1000", "expiration": expirationMillis.toString()});
        check caller->respond(res);
    }

    // Fakes POST /v1/users/{userId}/stop (called on listener shutdown).
    resource function post gmail/v1/users/[string userId]/stop(http:Caller caller,
            http:Request req) returns error? {
        http:Response res = new;
        res.statusCode = http:STATUS_NO_CONTENT;
        check caller->respond(res);
    }

    // Fakes GET /v1/users/{userId}/history (dispatch-critical, via the googleapis.gmail
    // connector on MOCK_GMAIL_SERVICE_URL). Query params (startHistoryId/labelId/maxResults/
    // pageToken) aren't inspected - the scenario fixture already omits nextPageToken, so this
    // always returns a single page.
    resource function get gmail/v1/users/[string userId]/history(http:Caller caller,
            http:Request req) returns error? {
        string scenario;
        lock {
            scenario = currentScenario;
        }
        json fixture = check io:fileReadJson(string `../payloads/history_${scenario}.json`);
        check caller->respond(fixture);
    }

    // Fakes GET /v1/users/{userId}/messages/{messageId} (dispatch-critical, via the connector).
    // Returns the current scenario's full Message fixture regardless of the requested messageId.
    resource function get gmail/v1/users/[string userId]/messages/[string messageId](http:Caller caller,
            http:Request req) returns error? {
        string scenario;
        lock {
            scenario = currentScenario;
        }
        json fixture = check io:fileReadJson(string `../payloads/message_${scenario}.json`);
        check caller->respond(fixture);
    }

    // Fakes GET /v1/users/{userId}/threads/{threadId} (dispatch-critical, via the connector).
    // Only called for the "new thread" scenario.
    resource function get gmail/v1/users/[string userId]/threads/[string threadId](http:Caller caller,
            http:Request req) returns error? {
        string scenario;
        lock {
            scenario = currentScenario;
        }
        json fixture = check io:fileReadJson(string `../payloads/thread_${scenario}.json`);
        check caller->respond(fixture);
    }

    // Test-only control endpoint - the driver calls this before each dispatch test POST to
    // select which scenario's fixtures the next history/messages/threads calls should return.
    // Not part of Google's API.
    resource function post control/scenario(http:Caller caller, http:Request req) returns error? {
        json body = check req.getJsonPayload();
        string scenario = check body.scenario;
        lock {
            currentScenario = scenario;
        }
        check caller->respond({"status": "ok", "scenario": scenario});
    }
}
