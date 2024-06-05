public type Subscribe record {
    boolean timing?;
    string event;
};

public type UnSubscribe record {
    *Time;
    *Activity;
};

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
};
