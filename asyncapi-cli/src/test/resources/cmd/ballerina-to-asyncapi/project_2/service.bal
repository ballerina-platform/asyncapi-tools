import ballerina/websocket;

listener websocket:Listener ep0 = new(80 );


@websocket:ServiceConfig {dispatcherKey: "type1"}
service /payloadV on ep0{
    resource function get v1/[int id]/v2/[string name](int tag) returns websocket:Service|websocket:UpgradeError {
        return new ChatServer();
    }

}

service class ChatServer{
    *websocket:Service;
     remote function onSubscribe(websocket:Caller caller, Subscribe message) returns int {
        return 5;
    }
}

public type Subscribe record{
    int id;
    string type1;
};
