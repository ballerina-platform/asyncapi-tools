import ballerina/websocket;

listener websocket:Listener ep1 = new(80);


@websocket:ServiceConfig {dispatcherKey: "type1"}
service on ep1{
    resource function get hi() returns websocket:Service|websocket:UpgradeError {
        return new FirstChatServer();
    }

}

@websocket:ServiceConfig{dispatcherKey: "event"}
service on new websocket:Listener(9090){
    resource function get hi() returns websocket:Service|websocket:UpgradeError {
        return new SecondChatServer();
    }

}
service class FirstChatServer{
    *websocket:Service;

     remote function onSubscribe(websocket:Caller caller, Subscribe message) returns int {
        return 5;
    }

}

service class SecondChatServer{
    *websocket:Service;

    remote function onUnSubscribe(websocket:Caller caller, UnSubscribe message)returns string{
        return "testing";
    }

}

public type Subscribe record{
    int id;
    string type1;
};
public type UnSubscribe record{
    string id;
    string event;

};
