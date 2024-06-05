public type Subscribe record {
    anydata pet_type;
    string event;
};

public type UnSubscribe record {
    int zipCode?;
    string event;
    # Test description
    Request request?;
};

# Test description
public type Request record {
    # tests
    int id;
    # tests
    string name;
    # tests
    decimal tag?;
    # tests
    string 'type?;
};
