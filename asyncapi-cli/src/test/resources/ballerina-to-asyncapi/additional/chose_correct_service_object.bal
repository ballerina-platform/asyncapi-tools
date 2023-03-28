import ballerina/websocket;
import ballerina/http;

listener websocket:Listener ep0 = new(80,config = { secureSocket : {
        key: {
            certFile: "../resource/path/to/public.crt",
            keyFile: "../resource/path/to/private.key"
        }
    }
} );


@websocket:ServiceConfig {subProtocols: [],dispatcherKey: "bb"}
service /hello on ep0{
    resource function get payment/[string id]() returns websocket:Service|websocket:UpgradeError {
        return new FirstChatServer();
    }

}

@websocket:ServiceConfig{dispatcherKey: "event"}
service /hello2 on ep0,new websocket:Listener(8081){
    resource function get v1/[int id]/v2/[string name]/v3/[float value]/payment/[int data] (@http:Header {} string X\-Client) returns websocket:Service|websocket:UpgradeError {
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
    string event;
};
public type UnSubscribe record{
    string id;
    string event;

};
