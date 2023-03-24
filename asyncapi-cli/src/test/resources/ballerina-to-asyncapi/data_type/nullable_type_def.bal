import ballerina/websocket;

public type Action string?;
public type Count decimal?;
public type Rels string[]?;
public type Books map<string>?;
public type Salary int|float|decimal?;

type Link record {|
    Books books;
    Rels rels;
    Action actions;
    Count count?;
    Salary salary;
    string action;
|};

@websocket:ServiceConfig{dispatcherKey: "action"}
service /payloadV on new websocket:Listener(9090) {

    # Resource function description
    #
    # + id - Query parameter id
    resource function get pathParam(int id) returns websocket:Service|websocket:UpgradeError {
        return new ChatServer();
    }
}

service class ChatServer{
    *websocket:Service;
    # Remote link description
    #
    # + message - message description
    # + return - this is return description
     remote function onLink(websocket:Caller caller, Link message) returns Action {
        return "Testing string return";
    }


}