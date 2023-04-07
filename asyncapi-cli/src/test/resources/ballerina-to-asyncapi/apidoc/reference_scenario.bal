import ballerina/websocket;

enum Action {
    GET,
    POST,
    PUT,
    DELETE,
    PATCH
}

type Link record {|
    string rel;
    string href;
    string[] mediaTypes?;
    Action[] actions?;
    # Remote trigger field
    string event;
|};
type Links record {|
    Link[] links;
|};

# Represents locations
type Location record {|
    *Links;
    # Name of the location
    string name;
    # Unique identification
    string id;
    # Address of the location
    string address;
    # Remote trigger field
    string event;
|};

enum RoomCategory {
    DELUXE,
    KING,
    FAMILY
}

enum RoomStatus {
    AVAILABLE,
    RESERVED,
    BOOKED
}

# Represents resort room
type Room record {|
    # Unique identification
    string id;
    #Types of rooms available
    RoomCategory category;
    # Number of people that can be accommodate
    int capacity;
    # Availability of wifi
    boolean wifi;
    # Availability of room
    RoomStatus status;
    # Currency used in price
    string currency;
    # Cost for the room
    decimal price;
    # Number of rooms as per the status
    int count;
|};
# Represents a collection of resort rooms
type Rooms record {|
    *Links;
    # Array of rooms
    Room[] rooms;
|};

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
# Represents a receipt for the reservation
type ReservationReceipt record {|
    *Links;
    # Unique identification
    string id;
    # Expiry date in yyyy-mm-dd
    string expiryDate;
    # Last updated time stamp
    string lastUpdated;
    # Reservation
    Reservation reservation;
    # Remote trigger field
    string event;
|};
type ReservationUpdated record {|
    ReservationReceipt body;
|};
type ReservationCreated record {|
    ReservationReceipt body;
|};
type ReservationConflict record {|
    string body = "Error occurred while updating the reservation";
|};

# Reperesents payement for rooms
type Payment record {|
    # Name of the card holder
    string cardholderName;
    # Card number
    int cardNumber;
    # Expiration month of the card in mm
    string expiryMonth;
    # Expiaration year of the card in yyyy
    string expiryYear;
     # Remote trigger field
    string event;
|};

# Reperesents receipt for the payment
type PaymentReceipt record {|
    # Unique identification
    string id;
    # Total amount paid
    decimal total;
    # Last updated time stamp
    string lastUpdated;
    # Booked rooms
    Room[] rooms;
    # Remote trigger field
    string event;
|};
type PaymentCreated record {|
    PaymentReceipt body;
|};
type PaymentConflict record {|
    string body = "Error occurred while updating the payment";
|};


@websocket:ServiceConfig{dispatcherKey: "event"}
service /payloadV on new websocket:Listener(9090) {
     resource function get locations() returns websocket:Service|websocket:UpgradeError  {
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

    # Represents Snowpeak location resource
    #
    # + message - ReserveRoom with a request
    # + return - `Location` representation
    remote function onReserveRoom(websocket:Caller caller,ReserveRoom message) returns Location[] {
        Location[] locations = getLocation();
        return locations;
    }

    # Reperesents Snowpeak room collection resource
    #
    # + location - location message containing whole details
    # + return - `Rooms` representation
    remote function onLocation(websocket:Caller caller,Location location)returns Rooms {
        string startDate="26/03/2023";
        string endDate="30/03/2023";
        Rooms rooms = getRooms(startDate, endDate);
        return rooms;
    }

    # Represents Snowpeak reservation resource
    #
    # + reservation - Reservation representation
    # + return - `ReservationCreated` or ReservationConflict representation
    remote function onReservation(websocket:Caller caller,Reservation reservation)returns ReservationCreated|ReservationConflict {
        ReservationCreated created = createReservation(reservation);
        return created;
    }

    # Represents Snowpeak reservation resource
    #
    # + reservationMessage - Reservation representation
    # + return - `ReservationCreated` or ReservationConflict representation
    remote function onPaymentReceipt(websocket:Caller caller,PaymentReceipt reservationMessage)returns ReservationUpdated|ReservationConflict {
        Reservation reservation= {startDate:"29/03/2023",endDate:"30/03/2023",reserveRooms:[{id:"5",count:5,event:""}],event:""};
        ReservationUpdated updated = updateReservation(reservation);
        return updated;
    }

    # Represents Snowpeak payment resource
    #
    # + payment - Payment representation
    # + return - `PaymentCreated` or `PaymentConflict` representation
    remote function onPayment(websocket:Caller caller,Payment payment) returns PaymentCreated|PaymentConflict {
        string id="5";
        PaymentCreated paymentCreated = createPayment(id, payment);
        return paymentCreated;
    }
}



function getLocation() returns Location[] {
    return [
        {
            name: "Alps",
            id: "l1000",
            address: "NC 29384, some place, switzerland",
            links: [
                {
                    rel: "room",
                    href: "http://localhost:9090/snowpeak/locations/l1000/rooms",
                    mediaTypes: ["applicaion/vnd.snowpeak.resort+json"],
                    actions: [GET],
                    event: ""
                }
            ],
            event: ""
        },
        {
            name: "Pilatus",
            id: "l2000",
            address: "NC 29444, some place, switzerland",
            links: [
                {
                    rel: "room",
                    href: "http://localhost:9090/snowpeak/locations/l2000/rooms",
                    mediaTypes: ["applicaion/vnd.snowpeak.resort+json"],
                    actions: [GET],
                    event: ""
                }
            ],
            event: ""
        }
    ];
}

function getRooms(string startDate, string endDate) returns Rooms {
    return {
        rooms: [
            {
                id: "r1000",
                category: DELUXE,
                capacity: 5,
                wifi: true,
                status: AVAILABLE,
                currency: "USD",
                price: 200.00,
                count: 3
            }
        ],
        links: [
            {
                rel: "reservation",
                href: "http://localhost:9090/rooms/reservation",
                mediaTypes: ["applicaion/vnd.snowpeak.resort+json"],
                actions: [POST],
                event: ""
            }
        ]
    };
}

function createReservation(Reservation reservation) returns ReservationCreated {
    return {

        body: {
            id: "re1000",
            expiryDate: "2021-07-01",
            lastUpdated: "2021-06-29T13:01:30Z",
            reservation: {
                reserveRooms: [
                    {
                        id: "r1000",
                        count: 2,
                        event: ""
                    }
                ],
                startDate: "2021-08-01",
                endDate: "2021-08-03",
                event:""

            },
            links: [
                {
                    rel: "cancel",
                    href: "http://localhost:9090/reservation/re1000",
                    mediaTypes: ["applicaion/vnd.snowpeak.resort+json"],
                    actions: [DELETE],
                    event: ""
                },
                {
                    rel: "edit",
                    href: "http://localhost:9090/reservation/re1000",
                    mediaTypes: ["applicaion/vnd.snowpeak.resort+json"],
                    actions: [PUT],
                    event: ""
                },
                {
                    rel: "payment",
                    href: "http://localhost:9090/payment/re1000",
                    mediaTypes: ["applicaion/vnd.snowpeak.resort+json"],
                    actions: [POST],
                    event: ""
                }
            ],
            event: ""
        }
    };
}

function updateReservation(Reservation reservation) returns ReservationUpdated {
    return {
        body: {
            id: "re1000",
            expiryDate: "2021-07-01",
            lastUpdated: "2021-07-05T13:01:30Z",
            reservation: {
                reserveRooms: [
                    {
                        id: "r1000",
                        count: 1,
                        event: ""
                    }
                ],
                startDate: "2021-08-01",
                endDate: "2021-08-03",
                event: ""
            },
            links: [
                {
                    rel: "cancel",
                    href: "http://localhost:9090/reservation/re1000",
                    mediaTypes: ["applicaion/vnd.snowpeak.resort+json"],
                    actions: [DELETE],
                    event: ""
                },
                {
                    rel: "edit",
                    href: "http://localhost:9090/reservation/re1000",
                    mediaTypes: ["applicaion/vnd.snowpeak.resort+json"],
                    actions: [PUT],
                    event: ""
                },
                {
                    rel: "payment",
                    href: "http://localhost:9090/payment/re1000",
                    mediaTypes: ["applicaion/vnd.snowpeak.resort+json"],
                    actions: [POST],
                    event: ""
                }
            ],
            event: ""
        }
    };
}

function createPayment(string id, Payment payment) returns PaymentCreated {
    return {
        body: {
            id: "p1000",
            total: 400.00,
            lastUpdated: "2021-06-29T13:01:30Z",
            rooms: [
                    {
                    id: "r1000",
                    category: DELUXE,
                    capacity: 5,
                    wifi: true,
                    status: RESERVED,
                    currency: "USD",
                    price: 200.00,
                    count: 1
                }
            ],
            event: ""
        }
    };
}
