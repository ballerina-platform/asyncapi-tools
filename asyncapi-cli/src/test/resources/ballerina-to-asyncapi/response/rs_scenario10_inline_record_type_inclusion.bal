import ballerina/websocket;

@websocket:ServiceConfig {dispatcherKey: "event"}
service /payloadV on new websocket:Listener(9090) {
    resource function get .() returns websocket:Service|websocket:Error {
        return new WsService();
    }
}

service class WsService {
    *websocket:Service;

    remote function onSubscribe(websocket:Caller caller, Subscribe message) returns record{| *Test; string body;|}{
        return {id:1, name: "",body: ""};
    }

}

public type Subscribe record{
    int id;
    string event;
};

type Test record {
    int id ;
    string name;
};
