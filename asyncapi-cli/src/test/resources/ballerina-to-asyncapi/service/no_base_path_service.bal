import ballerina/websocket;

listener websocket:Listener ep0 = new(80,config = { secureSocket : {
        key: {
            certFile: "../resource/path/to/public.crt",
            keyFile: "../resource/path/to/private.key"
        }
    }
} );


@websocket:ServiceConfig {subProtocols: [],dispatcherKey: "type1"}
service / on ep0{
    resource function get payment/[string id]() returns websocket:Service|websocket:UpgradeError {
        return new FirstChatServer();
    }

}

service class FirstChatServer{
    *websocket:Service;

     remote function onSubscribe(websocket:Caller caller, Subscribe message) returns int {
        return 5;
    }

}

public type Subscribe record{
    int id;
    string type1;
};
