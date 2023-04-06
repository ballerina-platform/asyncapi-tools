import ballerina/websocket;

listener websocket:Listener ep0 = new(80 );


@websocket:ServiceConfig {dispatcherKey: "event"}
service /payloadV on ep0{
    resource function get v1/[int id]/v2/[string name](int tag) returns websocket:Service|websocket:UpgradeError {
        return new ChatServer();
    }

}

service class ChatServer{
    *websocket:Service;
     remote function onSubscribe(websocket:Caller caller, Subscribe message) returns stream<string> {
        string[] array=["hello"];
        return array.toStream();
    }
}

public type Subscribe record{
    int id=5;
    string event;

};
