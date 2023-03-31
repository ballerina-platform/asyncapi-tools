import ballerina/websocket;

listener websocket:Listener helloEp = new (80);

public type Subscribe record{
    int id;
    string event;
};

public type Ticker record{
    int id;
};

public type Ping record {
    string event;
    Reqid reqid?;
};

public type Pong record {
    string event?;
    Reqid reqid?;
};

public type Reqid int?;
@websocket:ServiceConfig{dispatcherKey: "event"}
service / on helloEp {
    resource function get .() returns websocket:Service| websocket:UpgradeError {
        return new ChatServer();
    }
}

service class ChatServer{
    *websocket:Service;

    remote function onSubscribe(websocket:Caller caller,Subscribe message) returns Ticker{
        return {id:1};
    }

	remote function onPing(websocket:Caller caller, byte[] data) returns byte[]{
        return [4,5];

	}
}

