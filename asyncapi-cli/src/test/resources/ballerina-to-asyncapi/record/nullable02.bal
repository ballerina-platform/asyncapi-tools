import ballerina/websocket;

type Link record {|
    string rel;
    string? href;
    string[]? mediaTypes?;
    string event;
|};

listener websocket:Listener ep0 = new(443, config = {host: "petstore.swagger.io"});

@websocket:ServiceConfig{dispatcherKey: "event"}
service /payloadV on ep0 {
    resource function get pet(int payload) returns websocket:Service|websocket:UpgradeError{
            return new ChatServer();
    }
}


service class ChatServer{
    *websocket:Service;

     remote function onLink(websocket:Caller caller, Link message) returns int {

        return 5;
    }


}
