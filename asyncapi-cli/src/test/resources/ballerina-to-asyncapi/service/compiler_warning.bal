import ballerina/websocket;

@websocket:ServiceConfig {dispatcherKey: "event"}
service /foo on new websocket:Listener(9090) {
    resource function get .() returns websocket:Service|websocket:Error {
        return new WsService();
    }
}

service class WsService {
    *websocket:Service;
    int yy = 0;

    remote function onSubscribe(websocket:Caller caller, Subscribe message) returns int { 
        self.yy = 10;
        return 5;
    }

    remote function onUnSubscribe(websocket:Caller caller, UnSubscribe message) returns int { 
        self.yy = 20;
        return 5;
    }

}

public type Subscribe record{
    int id;
    string event;
};
public type UnSubscribe record{
    string id;
    string event;

};
