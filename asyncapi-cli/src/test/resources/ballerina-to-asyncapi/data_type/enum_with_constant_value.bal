import ballerina/websocket;

enum Action {
    GET = "get" ,
    POST = "post",
    PUT = "put",
    DELETE = "delete",
    PATCH = "patch"
}

type Link record {|
    string rel;
    Action actions?;
    string event;
|};


@websocket:ServiceConfig{dispatcherKey: "event"}
service /payloadV on new websocket:Listener(9090) {

    #  reservation channel description
    #
    # + id - id description
    resource function get reservation/[string id]() returns websocket:Service|websocket:UpgradeError {
        return new ChatServer();
    }
}

service class ChatServer{
    *websocket:Service;

    # onlink remote function description
    #
    # + link - link description
    # + return - Return int description
     remote function onLink(websocket:Caller caller, Link link) returns int {
        // io:println(data);
        return 5;
    }


}