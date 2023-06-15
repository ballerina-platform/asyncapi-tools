public type Subscribe record {
    anydata pet_type;
    string event;
};

public type UnSubscribe record {
    int zipCode?;
    string event;
    Request request?;
};

# Response details
public type Response record {
    int id;
    string name;
    string tag?;
};

public type Request record {
    # Response details
    Response time;
    boolean detail?;
};
