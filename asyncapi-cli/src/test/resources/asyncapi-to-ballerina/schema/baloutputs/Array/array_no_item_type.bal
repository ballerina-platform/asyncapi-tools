public type Subscribe record {
    Activity activity?;
    string event;
    anydata[] tag;
};

public type UnSubscribe record {
    int zipCode?;
    string event;
};

public type Activity anydata[];