import ballerina/websocket;

@websocket:ServiceConfig {dispatcherKey: "event"}
service /payloadV on new websocket:Listener(9090) {
    resource function get hello(string pet="hello",int offsetInteger=5,decimal offsetDecimal=100.08,boolean offsetBoolean=true, float offsetFloat=100.08,int[] offsetIntegerArray= [2, 1, 3, 4],int? offset=(),map<json>? offsetNullableJson = {"x": {"id": "sss"}},map<json> offsetJson = {"x": {"id": "sss"}}) returns websocket:Service|websocket:Error {
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