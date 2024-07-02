# Query parameters as a record
#
public type QueryParams record {|
    # Offset
    boolean offset = true;
    # Latitude
    Latitude lat = "78'";
    # Longtitude
    string lon = "90'05";
    # exclude
    string exclude = "current";
    # units description
    int units = 12;
|};

public type Message readonly & record {string event;};

public type MessageWithId readonly & record {string event; string id;};

public type Latitude string;

public type Subscribe record {
    string id;
    string event;
};

public type UnSubscribe record {
    string 'type;
    record {} payload?;
    string event;
};
