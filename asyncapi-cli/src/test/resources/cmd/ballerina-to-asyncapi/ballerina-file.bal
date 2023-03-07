import ballerina/websocket;
listener websocket:Listener ep0 = new(85,config={host:"First"});

service /hello on new websocket:Listener(80,config={host:"Second"}), new websocket:Listener(84,config={host:"Third"}),ep0{
    resource function get hi() returns websocket:Service|websocket:UpgradeError {
        return new ChatServer();
    }
}

service class ChatServer {
    *websocket:Service;

    remote function onOpen(websocket:Caller caller) returns error? {

    }

    remote function onMessage(websocket:Caller caller, string text) returns error? {

    }


    remote function onClose(websocket:Caller caller, int statusCode, string reason) returns error? {

    }
}


