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
    # A link to the Web API endpoint returning the full result of the request
    string href?;
    # The requested data.
    anydata items?;
    # The maximum number of items in the response (as set in the query or by default).
    anydata? 'limit?;
    # URL to the next page of items. ( `null` if none)
    string next?;
    # The offset of the items returned (as set in the query or by default)
    int offset?;
    # URL to the previous page of items. ( `null` if none) //anydata
    anydata? previous?;
    ListObject total?;
};

public type ListObject record {
};
