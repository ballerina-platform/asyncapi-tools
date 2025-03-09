public type Message readonly & record {string 'type;};

public type ConnectionInit record {
    string 'type;
    record {} payload?;
};

public type ConnectionAckMessage record {
    string 'type;
    record {} payload?;
};

public type Subscribe record {
    string 'type;
    string id;
    record {string? operationName?; string query; anydata? variables?; anydata? extensions?;} payload;
};

public type NextMessage record {
    string 'type;
    string id;
    json payload;
};

public type Complete record {
    string 'type;
    string id;
};
