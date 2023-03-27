import ballerina/websocket;


public type Subscribe record{
    int id;
    string event;
};

@websocket:ServiceConfig{dispatcherKey: "event"}
service /payloadV on new websocket:Listener(9090) {
    # Reperesents Snowpeak room collection resource
    #
    # + id - Unique identification of location
    resource function get locations/[string id]/rooms() returns websocket:Service|websocket:UpgradeError  {
        return new ChatServer();
    }
}

service class ChatServer{
    *websocket:Service;

     remote function onSubscribe(websocket:Caller caller, Subscribe message) returns int {
        // io:println(data);
        return 5;
    }


}