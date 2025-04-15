import ballerina/websocket;

listener websocket:Listener websocketListener = check new (9090);

type Subscribe record {|
    string 'type;
|};

type ConnectionAck record {|
    string 'type;
    map<json> payload?;
|};

@websocket:ServiceConfig {dispatcherKey: "type"}
service / on websocketListener {
    resource function get .() returns websocket:Service|websocket:UpgradeError {
        return new WsService();
    }
}

service class WsService {
    *websocket:Service;

    @websocket:DispatcherMapping {
        value: "subscribe"
    }
    remote function onSubscribeMessage(Subscribe message) returns string {
        return "onSubscribeMessage";
    }
}
