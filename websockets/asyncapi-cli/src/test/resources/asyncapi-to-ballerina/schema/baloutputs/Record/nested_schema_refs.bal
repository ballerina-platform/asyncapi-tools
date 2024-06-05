public type Subscribe record {
    anydata pet_type;
    string event;
};

public type UnSubscribe record {
    int zipCode?;
    string event;
    # A generic Asana Resource, containing a globally unique identifier.
    AsanaResource request?;
};

# A generic Asana Resource, containing a globally unique identifier.
public type AsanaResource record {
    # Globally unique identifier of the resource, as a string.
    string gid?;
    # The base type of this resource.
    string resource_type?;
};

public type UserCompact record {
    *AsanaResource;
    # Read-only except when same user as requester.
    string name?;
};

public type ProjectStatusCompact record {
    *AsanaResource;
    # The title of the project status update.
    string title?;
};

public type ProjectStatusRequest ProjectStatusBase;

public type ProjectStatusBase record {
    *ProjectStatusCompact;
    UserCompact author?;
    # The time at which this project status was last modified.
    anydata modified_at?;
    # The text content of the status update.
    string text;
    string html_text?;
    # The color associated with the status update.
    string color;
};
