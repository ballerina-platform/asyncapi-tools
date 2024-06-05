public type Subscribe record {
    boolean timing?;
    string event;
};

public type UnSubscribe record {
    *Time;
    Time time?;
};

public type Time record {
    int minute;
    string second?;
    string hour?;
    string day?;
    string event;
};
