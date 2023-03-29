import ballerina/websocket;
import ballerina/http;

listener websocket:Listener ep0 = new(9090);


@websocket:ServiceConfig {subProtocols: [],dispatcherKey: "type1"}
service /payloadV on ep0{
    resource function get hi(int param) returns websocket:Service|websocket:UpgradeError {
        return new ChatServer();
    }

}

service class ChatServer{
    *websocket:Service;

     remote function onSubscribe(websocket:Caller caller, Subscribe message) returns int {
        return 5;
    }

}
