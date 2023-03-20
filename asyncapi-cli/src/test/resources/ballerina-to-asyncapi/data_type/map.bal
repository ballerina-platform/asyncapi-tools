import ballerina/websocket;

public type Link record {
    string rel?;
    string href;
    Subscribe[] types?;
    string[] methods?;
};

public type Subscribe record{
    int id?;
    string event?;

    string fdf;

    string bb;

    string type1;
};

public type Location record {|
    map<Link> _links;
    map<string> name;
    map<int> id;
    map<float> addressCode;
    map<json> item?;
    map<string[]> mapArray?;
    map<map<json>> mapMap?;
    map<string>[] arrayItemMap?;
    string event;
|};

map<string> Test1 = {
    hello:"hello",
    event:"Test"
};

@websocket:ServiceConfig{dispatcherKey: "event"}
service /payloadV on new websocket:Listener(9090) {
    resource function get locations/[string id]() returns websocket:Service|websocket:UpgradeError {
        return new ChatServer();
    }
}

service class ChatServer{
    *websocket:Service;


    #Testing remote description
    # + location - remote above link description
    # + return - remote return description
    remote function onLocation(websocket:Caller caller, Location location) returns map<string>{
        return Test1;

    }

}