import ballerina/http;

// Listener related configurations should be included here
public type ListenerConfiguration record {|
    *http:ListenerConfiguration;
|};

public type CustomTestSchema record {
    string test_id?;
};

public type GenericEventWrapper record {
    CustomTestSchema custom_test_schema?;
    # The unique identifier your installed Slack application.
    string api_app_id;
    # An array of string-based User IDs. Each member of the collection represents a user that has installed your application/bot and indicates the described event would be visible to those users.
    string[] authed_users;
    # The actual event, an object, that happened
    record  { # When the event was dispatched
        string event_ts; # The specific name of the event
        string 'type;}  event;
    # A unique identifier for this specific event, globally unique across all workspaces.
    string event_id;
    # The epoch timestamp in seconds indicating when this event was dispatched.
    int event_time;
    # The unique identifier of the workspace where the event occurred
    string team_id;
    # A verification token to validate the event originated from Slack
    string token;
    # Indicates which kind of event dispatch this is, usually `event_callback`
    string 'type;
    decimal decimal_number?;
    float float_number?;
    boolean boolean_value?;
    record {} plain_object?;
    CustomTestSchema ref_object?;
    record  { # When the event was dispatched
        string event_ts?; # The specific name of the event
        string 'type?;}  object_without_type?;
    # This should be considered as anydata
    anydata 'anydata?;
    CustomTestSchema[] array_with_ref?;
    string[][] array_with_inner_array?;
    record {}[] array_with_inner_object?;
    anydata[] array_with_anydata?;
};

public type GenericDataType CustomTestSchema|GenericEventWrapper;
