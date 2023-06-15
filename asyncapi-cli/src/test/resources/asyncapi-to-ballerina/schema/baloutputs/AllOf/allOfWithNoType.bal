public type Subscribe record {
    boolean pure?;
    boolean timing?;
    string event;
};

public type UnSubscribe record {
    *Time;
    boolean timing?;
};

public type Time record {
    int minute;
    string second?;
    string hour?;
    string day?;
    string event;
};
