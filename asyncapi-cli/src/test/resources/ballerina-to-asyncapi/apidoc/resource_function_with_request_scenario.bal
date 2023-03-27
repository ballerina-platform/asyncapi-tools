import ballerina/websocket;


# Represents rooms be reserved
type ReserveRoom record {|
    # Unique identification of the room
    string id;
    # Number of rooms
    int count;
    # Remote trigger field
    string event;
|};
# Represents a reservation of rooms
type Reservation record {|
    # Rooms to be reserved
    ReserveRoom[] reserveRooms;
    # Start date in yyyy-mm-dd
    string startDate;
    # End date in yyyy-mm-dd
    string endDate;
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

    remote function onReservation(websocket:Caller caller, Reservation message) returns int {

        return 5;
    }
    remote function onReserveRoom(websocket:Caller caller, ReserveRoom message) returns int {

        return 5;
    }


}