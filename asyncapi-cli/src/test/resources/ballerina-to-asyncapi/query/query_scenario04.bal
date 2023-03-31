import ballerina/websocket;

@websocket:ServiceConfig {dispatcherKey: "event"}
service /payloadV on new websocket:Listener(9090) {
    resource function get .(int[]? pet) returns websocket:Service|websocket:Error {
        return new WsService();
    }
}

service class WsService {
    *websocket:Service;

    remote isolated function onSubscribe(websocket:Caller caller, Subscribe data) returns string? {
        return "Hello World!";
    }
}


public type Subscribe record{
    int id;
    string event;
};