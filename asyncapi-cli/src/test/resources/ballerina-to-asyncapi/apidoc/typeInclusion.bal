import ballerina/websocket;

# Link record
type Link record {|
   # link rel
   string rel;
   # link href
   string href;
   #link mediatype
   string[] mediaTypes?;
   # Remote trigger field
   string event;
|};

# Links array
type Links record {|
   # Array links
   Link[] links;
   # link id
   int linkid;
|};

# ReservationReceipt details
type ReservationReceipt record {|
   *Links;
   # Reservation receipt id
   string id;
   # Remote trigger field
   string event;
|};

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

    remote function onReservationReceipt(websocket:Caller caller, ReservationReceipt message) returns int {

        return 5;
    }
    remote function onLink(websocket:Caller caller, Link message) returns int {

        return 5;
    }


}

