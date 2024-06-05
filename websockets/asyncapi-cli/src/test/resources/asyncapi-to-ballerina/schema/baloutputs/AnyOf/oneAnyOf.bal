public type ResponseMessage record {
    string event;
};

public type ResponseMessageWithId record {
    string event;
    string id;
};

public type Subscribe record {
    boolean timing?;
    string event;
};

public type UnSubscribe Time;

public type Time record {
    int minute;
    string second?;
    string hour?;
    string day?;
    string event;
};
