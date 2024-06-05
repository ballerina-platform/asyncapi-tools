public type Subscribe record {
    int pet_type;
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
    int id;
    string name;
    int tagNumber = 10;
    string 'type?;
    boolean isTrue = true;
    decimal decimalValue = 0.05;
    float floatValue = 11.5;
    string stringDecimalValue = "00.05";
};
