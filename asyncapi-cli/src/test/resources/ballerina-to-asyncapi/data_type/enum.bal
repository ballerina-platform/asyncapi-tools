import ballerina/websocket;

enum Action {
    GET,
    POST,
    PUT,
    DELETE,
    PATCH
}

# this is for testing
#
# + rel-rel description
# + actions-actions description
# + fdf- fdf description
type Link record {|
    string rel;
# Try override description
    Action actions?;
    # Try override description
    int fdf;
    # Try override description
    string s8jk;
|};

type Order record {|
    string rel;
    OrderType actions?;
    string s8jk;
|};
const SIZE = "size";

enum OrderType {
    FULL = "full",
    HALF = "Half \"Portion\"",
    CUSTOM = "custom " + SIZE
};
type Test record{|
string check2;
string hello;
string s8jk;

|};

@websocket:ServiceConfig{dispatcherKey: "s8jk"}
service /payloadV on new websocket:Listener(9090) {
    #resource function description
    #+ id - test id description
    resource function get payment/[string id]() returns websocket:Service|websocket:UpgradeError {
        return new ChatServer();
    }

}

service class ChatServer{
    *websocket:Service;



    #Testing remote description
    # + message - remote above link description
    # + return - remote return description
    remote function onLink(websocket:Caller caller, Link message) returns Test{
        return {s8jk:"checking",check2: "hello",hello:"hi"};


    }


    # Order remote description
    # + message- order above link description
    # + return - order return description
    remote function onOrder(websocket:Caller caller, Order message) returns int {
        return 5;

    }

}
