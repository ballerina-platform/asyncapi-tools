import ballerina/websocket;

@websocket:ServiceConfig {dispatcherKey: "event"}
service /payloadV on new websocket:Listener(9090) {
    resource function get .() returns websocket:Service|websocket:Error {
        return new WsService();
    }
}

service class WsService {
    *websocket:Service;

    remote function onSubscribe(websocket:Caller caller, Subscribe message) returns xml{
        xml x1 = xml `<book>The Lost World</book>`;
        return x1;
    }

}

public type Subscribe record{
    int id;
    string event;
};
