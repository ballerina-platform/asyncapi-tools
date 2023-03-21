import ballerina/websocket;

type Link record {|
    string rel?;
    string href?;
    string[] mediaTypes?;
|};

type Links record {|
    Link[] links?;
    int linkid?;
|};

type ReservationReceipt record {|
    *Links;
    string id?;
    string event;
|};

public type Subscribe record{
    Depth depth?;
    Interval interval?;
    MaxRateCount maxratecount?;
    Name name;
    Token token?;
    string event;
};
public type Depth int?;
public type Token string?;


public enum Name {
    book,
    ohlc,
    openOrders,
    ownTrades,
    spread,
    ticker,
    trade
}
public type MaxRateCount int?;
public type Interval int?;



listener websocket:Listener ep0 = new(443, config = {host: "petstore.swagger.io"});

@websocket:ServiceConfig{dispatcherKey: "event"}
service /payloadV on ep0 {
    resource function get pathParam(ReservationReceipt queryParam) returns websocket:Service|websocket:UpgradeError {
        return new ChatServer();
    }
}

service class ChatServer{
    *websocket:Service;

     remote function onSubscribe(websocket:Caller caller, Subscribe message) returns int {
        // io:println(data);
        return 5;
    }
    remote function onReservationReceipt(websocket:Caller caller, ReservationReceipt message) returns int {
        // io:println(data);
        return 5;
    }


}