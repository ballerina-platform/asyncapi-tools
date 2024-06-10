public type Subscribe record {
    int pet_type;
    string event;
    Time time?;
};

public type UnSubscribe record {
    int zipCode?;
    string event;
    Request request?;
};

public type Request record {
    int id;
    string name;
    string tag?;
    string 'type?;
};

public type Response record {
    *Request;
    boolean isTime?;
};

public type Time Request[]?;
