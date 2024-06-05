public type Subscribe record {
    int pet_type;
    string event;
};

public type UnSubscribe record {
    int zipCode?;
    string event;
    Request request?;
};

public type Request record {
    # The customer's address.
    Response[]|string address?;
    string name?;
};

public type Response string;

