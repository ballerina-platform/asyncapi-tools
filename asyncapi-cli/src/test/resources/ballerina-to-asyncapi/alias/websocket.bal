import ballerina/websocket as ws;
import ballerina/http;

listener ws:Listener helloEp = new (9090);

public type Subscribe record{
    int id;
    string event;
};

@ws:ServiceConfig{dispatcherKey: "event"}
service /payloadV on helloEp {
    resource function get ping(@http:Header string headerValue) returns ws:Service|ws:UpgradeError {
        return new ChatServer();
    }

}

service class ChatServer{
    *ws:Service;

    remote function onSubscribe(ws:Caller caller, Subscribe message) returns int {
        return 5;
    }

}

