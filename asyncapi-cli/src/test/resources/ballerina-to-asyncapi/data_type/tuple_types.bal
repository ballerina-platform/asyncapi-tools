import ballerina/websocket;

public type Tuple record {
    [int, string, decimal, float, User] address;
    int id;
    [string, decimal]? unionTuple;
    ReturnTypes? tuples;
    string event;
};

public type User readonly & record {|
    int id;
    int age;
    string event;
|};

public type ReturnTypes readonly & [int, decimal];

@websocket:ServiceConfig{dispatcherKey: "event"}
service /payloadV on new websocket:Listener(9090) {
    resource function get .(User payload)returns websocket:Service|websocket:UpgradeError {
          return new ChatServer();

    }
}

service class ChatServer{
    *websocket:Service;
    # Remote tuple description
    #
    # + message - Tuple message description
    # + return - this is User return description
     remote function onTuple(websocket:Caller caller, Tuple message) returns User {
        return {id:5,age:45,event:"Testing"};
    }


}
