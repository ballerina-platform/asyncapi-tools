
import ballerina/websocket;

public type Subscribe record{
    int id;
    string event;
};

listener websocket:Listener ep0 = new(9090);
@websocket:ServiceConfig{dispatcherKey: "event"}
service  /payloadV on ep0 {
    resource function get pets () returns websocket:Service|websocket:Error {
        return new ChatServer();
    }
}

service class ChatServer{
    *websocket:Service;

     remote function onSubscribe(websocket:Caller caller, Subscribe message) returns int {
        return 5;
    }
}
