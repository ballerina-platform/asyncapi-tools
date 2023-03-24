import ballerina/websocket;

type Link record {|
    string rel;
    string|Cat pet;
    string testing;
|};

type Dog record {|
    int? id;
    string name?;
    string testing;
|};

type Cat record {|
    int id;
    string eat?;
    string testing;
|};

listener websocket:Listener ep0 = new(443, config = {host: "petstore.swagger.io"});

@websocket:ServiceConfig{dispatcherKey: "testing"}
service /payloadV on ep0 {
    resource function get pet(Link payload) returns websocket:Service|websocket:UpgradeError {
        return new ChatServer();
    }
}

service class ChatServer{
    *websocket:Service;

     remote function onDog(websocket:Caller caller, Dog message) returns Cat {

        return {id:5,eat:"ate",testing:"testing"};
    }


}
