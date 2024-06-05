public type Subscribe record {
    int zipCode?;
    string event;
    string[][][] tag;
};

public type UnSubscribe record {
    int zipCode?;
    string event;
};
