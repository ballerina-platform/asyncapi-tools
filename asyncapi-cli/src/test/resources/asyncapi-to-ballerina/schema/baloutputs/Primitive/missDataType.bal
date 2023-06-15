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
    # this is missing dataType
    anydata id;
    string name;
    decimal tag?;
    string 'type?;
};
