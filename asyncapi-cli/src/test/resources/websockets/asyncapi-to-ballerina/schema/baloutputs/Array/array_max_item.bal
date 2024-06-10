public type Subscribe record {
    @constraint:Array {maxLength: 7}
    int[] tag;
    string event;
    Activity activity?;
};

public type UnSubscribe record {
    int zipCode?;
    string event;
};

@constraint:Array {maxLength: 7}
public type Activity int[];
