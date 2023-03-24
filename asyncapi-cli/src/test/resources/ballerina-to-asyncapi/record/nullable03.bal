import ballerina/websocket;

type Link record {|
    string? rel;
    Pet? pet?;
    string event;
|};

type Pet record {|
    int id;
    string? name?;
|};

listener websocket:Listener ep0 = new(443, config = {host: "petstore.swagger.io"});

@websocket:ServiceConfig{dispatcherKey:"event"}
service /payloadV on ep0 {
    resource function get pet(Link payload) returns websocket:Service|websocket:UpgradeError {
        return new ChatServer();
    }
}

service class ChatServer{
    *websocket:Service;

     remote function onLink(websocket:Caller caller, Link message) returns int {

        return 5;
    }


}
