import ballerina/websocket;
// import ballerina/io;

listener websocket:Listener ep0 = new(85,config={host:"0.0.0.0"});


@websocket:ServiceConfig {dispatcherKey: "event"}
service /hello on ep0,new websocket:Listener(8080){
    resource function get payment/[string id]() returns websocket:Service|websocket:UpgradeError {
        return new ChatServer();
    }


}

service /hello2 on ep0{
    resource function get payment/[string id]() returns websocket:Service|websocket:UpgradeError {
        return new ChatServer1();
    }


}

service class ChatServer {
    *websocket:Service;

     remote function onSubscribe(websocket:Caller caller, Subscribe message) returns websocket:Error? {
        // io:println(data);
        check caller->writeMessage({"type": "subscribe", "id":"1", "payload":{"query": "{ __schema { types { name } } }"}});
    }

    remote function onHeartbeat( Heartbeat data) returns stream<string> {
        string[] greets = ["Hi Sam", "Hey Sam", "GM Sam"];
        return greets.toStream();
        // return {"event": "heartbeat"};
    }
}

service class ChatServer1{
    *websocket:Service;

     remote function onSubscribe(websocket:Caller caller, Subscribe message) returns Heartbeat{
        // io:println(data);
        return {id:1};
    }

    remote function onHeartbeat(websocket:Caller caller, Heartbeat data) returns json {
        return {"event": "heartbeat"};
    }
}

public type Subscribe record{
    int id;
};

public type Heartbeat record{
    int id;
};

