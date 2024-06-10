public type Subscribe record {
    boolean timing?;
    string event;
};

public type UnSubscribe record {
    *Time;
};

public type Time record {
    int minute;
    string second?;
    string hour?;
    string day?;
    string event;
};
