public type Subscribe record {
    int zipCode?;
    string event;
    # Code array
    Activity[] code?;
    boolean codePen?;
};

public type UnSubscribe record {
    int zipCode?;
    string event;
};

public type Activity Request[];

public type Request int;