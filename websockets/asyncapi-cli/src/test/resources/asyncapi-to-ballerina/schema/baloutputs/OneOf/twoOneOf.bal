public type Subscribe record {
    int zipCode?;
    string event;
};

public type UnSubscribe Time|Activity;

public type Time record {
    int minute;
    string second?;
    string hour?;
    string day?;
    string event;
};

public type Activity record {
    # Unique identifier for the activity
    string uuid?;
    string event;
};
