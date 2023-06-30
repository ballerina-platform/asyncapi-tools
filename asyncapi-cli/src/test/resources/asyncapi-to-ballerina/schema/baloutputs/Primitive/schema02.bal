public type Subscribe record {
    anydata pet_type;
    string event;
};

public type UnSubscribe record {
    int zipCode?;
    string event;
    Request request?;
};

public type Request record {
    int id;
    string name;
    string tag?;
    string 'type?;
};
