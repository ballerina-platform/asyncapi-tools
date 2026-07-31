import ballerina/io;

// Points the harness's OAuth refresh flow, Pub/Sub provisioning (constants.bal's PUBSUB_BASE_URL
// deviation) and Gmail API calls (constants.bal's GMAIL_BASE_URL deviation + util.bal's
// MOCK_GMAIL_SERVICE_URL deviation) at the local mock server in ../mock/. Values otherwise don't
// matter - the mock doesn't validate them.
listener Listener mailHarnessListener = check new (
    {
        clientId: "mock-client-id",
        clientSecret: "mock-client-secret",
        refreshUrl: "http://localhost:8091/oauth/token",
        refreshToken: "mock-refresh-token",
        project: "mock-project",
        callbackURL: "http://localhost:8090/"
    },
    8090
);

service GmailService on mailHarnessListener {
    remote function onNewEmail(Message message) returns error? {
        io:println("FIRED::GmailService::onNewEmail");
    }
    remote function onNewThread(MailThread thread) returns error? {
        io:println("FIRED::GmailService::onNewThread");
    }
    remote function onEmailLabelAdded(ChangedLabel changedLabel) returns error? {
        io:println("FIRED::GmailService::onEmailLabelAdded");
    }
    remote function onEmailStarred(Message message) returns error? {
        io:println("FIRED::GmailService::onEmailStarred");
    }
    remote function onEmailLabelRemoved(ChangedLabel changedLabel) returns error? {
        io:println("FIRED::GmailService::onEmailLabelRemoved");
    }
    remote function onEmailStarRemoved(Message message) returns error? {
        io:println("FIRED::GmailService::onEmailStarRemoved");
    }
    remote function onNewAttachment(MailAttachment attachment) returns error? {
        io:println("FIRED::GmailService::onNewAttachment");
    }
}
