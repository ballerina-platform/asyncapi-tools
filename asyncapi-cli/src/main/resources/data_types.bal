import ballerina/http;

// Listener related configurations should be included here
public type ListenerConfiguration record {|
    *http:ListenerConfiguration;
|};
