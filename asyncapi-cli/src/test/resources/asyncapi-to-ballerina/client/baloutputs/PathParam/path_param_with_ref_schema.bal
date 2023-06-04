# Path parameters as a record
#
# + id - Unique identification of location
public type PathParams record {|
    Id id;
|};

public type ResponseMessage record {
    string event;
};

public type ResponseMessageWithId record {
    string event;
    string id;
};

public type Subscribe record {
    string id;
    string event;
};

public type Id int;

public type UnSubscribe record {
    string 'type;
    map<json> payload?;
};
