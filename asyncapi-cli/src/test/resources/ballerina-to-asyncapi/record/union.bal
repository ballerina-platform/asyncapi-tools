import ballerina/websocket ;

type Link record {|
    string rel;
    Dog|Cat pet;
|};

type Dog record {|
    int? id;
    string name?;
    string event;
|};

type Cat record {|
    int id;
    string eat?;
    string event;
|};

listener websocket:Listener ep0 = new(443);

@websocket:ServiceConfig{dispatcherKey: "event"}
service /payloadV on ep0 {
    resource function get pet(Link payload) returns websocket:Service| websocket:UpgradeError  {
         return new ChatServer();
    }
}

service class ChatServer{
    *websocket:Service;

     remote function onDog(websocket:Caller caller, Dog message) returns Cat {

        return {id:5,eat:"ate",event:"Cat"};
    }


}