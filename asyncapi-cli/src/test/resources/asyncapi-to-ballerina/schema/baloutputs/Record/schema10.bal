public type Subscribe record {
    anydata pet_type;
    string event;
};

public type UnSubscribe record {
    int zipCode?;
    string event;
};
