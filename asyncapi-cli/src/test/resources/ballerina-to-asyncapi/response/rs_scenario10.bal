import ballerina/websocket;

@websocket:ServiceConfig {dispatcherKey: "event"}
service /payloadV on new websocket:Listener(9090) {
    resource function get .() returns websocket:Service|websocket:Error {
        return new WsService();
    }
}

service class WsService {
    *websocket:Service;

    remote function onSubscribe(websocket:Caller caller, Subscribe message) returns SubscriptionStatus[]{
        SubscriptionStatus subscriptionStatus={id:1,type1: "subscriptionStatus"};
        SubscriptionStatus[] subscriptionArray=[subscriptionStatus];
        return subscriptionArray;
    }
    remote function onUnSubscribe(websocket:Caller caller, UnSubscribe message) returns string[]{
        string[] st=["test"];
        return st;
    }
}

public type Subscribe record{
    int id;
    string event;
};
public type UnSubscribe record{
    int id;
    string event;
};

public type SubscriptionStatus record {
    int id;
    string type1;
};