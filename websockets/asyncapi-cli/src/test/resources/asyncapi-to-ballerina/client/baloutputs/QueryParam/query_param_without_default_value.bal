# Query parameters as a record
#
public type QueryParams record {|
    # Offset
    boolean offset;
    # Latitude
    string lat;
    # Longtitude
    string lon;
    # exclude
    string exclude;
    # units description
    int units;
|};

public type Message readonly & record {string event;};

public type MessageWithId readonly & record {string event; string id;};

public type Subscribe record {
    string id;
    string event;
};

public type UnSubscribe record {
    string 'type;
    record {} payload?;
    string event;
};
