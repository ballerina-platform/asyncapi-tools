public type Subscribe record {
    int pet_type;
    string event;
};

public type UnSubscribe record {
    int zipCode?;
    string event;
    Request request?;
};

# Response details
public type Response record {
    int id;
    string name;
    string tag?;
};

public type Request record {
    int id;
    string name;
    string tagName = "TagName";
    string? 'type = "<|endoftext|>";
    string|string[]? prompt = "<|endoftext|>";
    int|string anyOfField = "<|endoftext|>";
};
