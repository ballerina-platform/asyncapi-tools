import ballerina/websocket;

@websocket:ServiceConfig {dispatcherKey: "event"}
service /payloadV on new websocket:Listener(9090) {
    resource function get hello() returns websocket:Service|websocket:Error {
        return new WsService();
    }
}

service class WsService {
    *websocket:Service;

    remote isolated function onSubscribe(websocket:Caller caller, Subscribe data) returns json {
        return "Hello World!";
    }
}

public type Subscribe record{
    int id;
    string event;
};
