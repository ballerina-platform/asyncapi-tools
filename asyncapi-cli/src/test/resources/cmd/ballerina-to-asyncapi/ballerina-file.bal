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
        return new ChatServer();
    }

}

@websocket:ServiceConfig{dispatcherKey: "fdf"}
service /hello2 on ep0,new websocket:Listener(8081){
    resource function get v1/[int id]/v2/[string name]/v3/[float value]/payment/[int data] (@http:Header {} string X\-Client) returns websocket:Service|websocket:UpgradeError {
        return new ChatServer1();
    }

}
service class ChatServer{
    *websocket:Service;

     remote function onSubscribe(websocket:Caller caller, Subscribe message) returns int {
        // io:println(data);
        return 5;
    }


}

service class ChatServer1{
    *websocket:Service;

    remote function onSubscribe(websocket:Caller caller, Subscribe message) {


    }

}

public type Subscribe record{
    int id?;
    string event?;

    Heartbeat hello;

    string fdf;

    string bb;

    string type1;
};
public type Hello int;
public type Heartbeat record{
    int id;
    string event;

    Hello hello;

    string fdf;
    string type1;
};

