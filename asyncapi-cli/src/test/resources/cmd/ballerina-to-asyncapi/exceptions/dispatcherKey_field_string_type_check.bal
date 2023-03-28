import ballerina/websocket;


listener websocket:Listener helloEp = new (80);

public type Subscribe record{
    int id;
    int event;
};

public type Ticker record{
    int id;
};
@websocket:ServiceConfig{dispatcherKey: "event"}
service / on helloEp {
    resource function get .() returns websocket:Service|websocket:UpgradeError {
        return new ChatServer();
    }

}

service class ChatServer{
    *websocket:Service;

    remote function onSubscribe(websocket:Caller caller,Subscribe message) returns Ticker{
        return {id:1};
    }

}

