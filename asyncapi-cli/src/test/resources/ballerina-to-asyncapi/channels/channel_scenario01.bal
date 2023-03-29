import ballerina/websocket;

listener websocket:Listener ep0 = new(80,config = { secureSocket : {
        key: {
            certFile: "../resource/path/to/public.crt",
            keyFile: "../resource/path/to/private.key"
        }
    }
} );


@websocket:ServiceConfig {dispatcherKey: "type1"}
service /payloadV on ep0{
    resource function get .() returns websocket:Service|websocket:UpgradeError {
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
public type UnSubscribe record{
    string id;
    string event;

};
