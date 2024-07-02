public type Subscribe record {
    anydata pet_type;
    string event;
};

public type UnSubscribe record {
    int zipCode?;
    string event;
};

# Pet details
public type Pet record {
    int id;
    string name;
    string tag?;
    string 'type?;
};

# Dog details
public type Dog record {
    # Pet details
    Pet pet_details?;
    boolean bark?;
};

# Pets details
public type Pets record {
    anydata pet_details?;
    int numer_of_pets?;
};
