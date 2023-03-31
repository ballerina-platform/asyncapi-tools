import ballerina/websocket;

@websocket:ServiceConfig {dispatcherKey: "event"}
service /payloadV on new websocket:Listener(9090) {
    string y="hello";
    resource function get hello(string definedY=y,string prefixDefinedY="hi"+y,int offset=10/2,string limitV=getHeader()) returns websocket:Service|websocket:Error {
        return new WsService();
    }
}

service class WsService {
    *websocket:Service;

    remote isolated function onSubscribe(websocket:Caller caller, Subscribe data) returns string? {
        return "Hello World!";
    }
}


public type Subscribe record{
    int id;
    string event;
};

function getHeader() returns string {
    return "query";
}
