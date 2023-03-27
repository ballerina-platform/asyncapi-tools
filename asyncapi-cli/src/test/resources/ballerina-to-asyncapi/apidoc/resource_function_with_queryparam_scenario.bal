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
    # + startDate - Start date in format yyyy-mm-dd
    # + endDate - End date in format yyyy-mm-dd
    resource function get locations/[string id]/rooms(string startDate, string endDate) returns websocket:Service|websocket:UpgradeError  {
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