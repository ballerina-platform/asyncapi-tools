# Path parameters as a record
#
# + id - Unique identification of location
public type PathParams record {|
    Id id;
|};

public type Message readonly & record {string event;};

public type MessageWithId readonly & record {string event; string id;};

public type Subscribe record {
    string id;
    string event;
};

public type Id int;

public type UnSubscribe record {
    string 'type;
    record {} payload?;
    string event;
};
