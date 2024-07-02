public type Subscribe record {
    int pet_type;
    string event;
};

public type UnSubscribe record {
    int zipCode?;
    string event;
    Request request?;
};

public type Request record {
    int id;
    # name field
    string name;
    string tag?;
    record {
        # type id
        string typeId?;
        string tagType?;
    } 'type?;
};
