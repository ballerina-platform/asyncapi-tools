import ballerina/websocket;

listener websocket:Listener ep0 = new (80, config = {host: "www.asyncapi.com"});

public type Subscribe record{
    int id;
    string event;
};

@websocket:ServiceConfig{dispatcherKey: "event"}
service /payloadV on helloEp {
    resource function get pathParam() returns websocket:Service|websocket:UpgradeError {
        return new ChatServer();
    }

}

service class ChatServer{
    *websocket:Service;

    remote function onSubscribe(websocket:Caller caller, Subscribe message) returns int {
        return 5;
    }

}

