import ballerina/websocket;

@websocket:ServiceConfig {dispatcherKey: "event"}
service /payloadV on new websocket:Listener(9090) {
    resource function get .() returns websocket:Service|websocket:Error {
        return new WsService();
    }
}

service class WsService {
    *websocket:Service;

    remote function onSubscribe(websocket:Caller caller, Subscribe message) returns SubscriptionStatus{
        return {id:1,type1: "subscriptionStatus"};
    }

}

public type Subscribe record{
    int id;
    string event;
};
public type SubscriptionStatus record {
    int id;
    string type1;
};