# Header parameters as a record
#
public type HeaderParams record {|
    # offset
    int offset = 5;
    # Latitude
    string lat = "78'08";
    # Longtitude
    string lon = "90'78";
    # exclude
    string exclude = "56'89";
    # units description
    int units = 12;
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
