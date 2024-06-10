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

public type Activity record {
    int id;
    string name;
    string tag?;
};
