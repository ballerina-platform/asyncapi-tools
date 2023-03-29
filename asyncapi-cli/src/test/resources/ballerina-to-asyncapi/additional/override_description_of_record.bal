import ballerina/websocket;

listener websocket:Listener helloEp = new (80);

# Subscribe record description
# + id - Override id description
# + event - Override event description
public type Subscribe record{
    #Subscribe id description
    int id;
    #Subscribe event description
    string event;
};

public type Ticker record{
    int id;
};

public type Reqid int?;
@websocket:ServiceConfig{dispatcherKey: "event"}
service /payloadV on helloEp {
    resource function get .() returns websocket:Service| websocket:UpgradeError {
        return new ChatServer();
    }
}

service class ChatServer{
    *websocket:Service;

    remote function onSubscribe(websocket:Caller caller,Subscribe message) returns Ticker{
        return {id:1};
    }
}

