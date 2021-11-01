// Listener related configurations should be included here
public type ListenerConfigs record {
};

public type CustomTestSchema record {
    string test_id?;
};

public type GenericEventWrapper record {
    # An array of string-based User IDs. Each member of the collection represents a user that has installed your application/bot and indicates the described event would be visible to those users.
    string[] authed_users;
    anydata[] array_with_anydata?;
    boolean boolean_value?;
    # The unique identifier your installed Slack application.
    string api_app_id;
    # The unique identifier of the workspace where the event occurred
    string team_id;
    # Indicates which kind of event dispatch this is, usually `event_callback`
    string 'type;
    # A verification token to validate the event originated from Slack
    string token;
    CustomTestSchema custom_test_schema?;
    record {}[] array_with_inner_object?;
    # A unique identifier for this specific event, globally unique across all workspaces.
    string event_id;
    CustomTestSchema[] array_with_ref?;
    float float_number?;
    CustomTestSchema ref_object?;
    decimal decimal_number?;
    record  { # When the event was dispatched
        string event_ts?; # The specific name of the event
        string 'type?;}  object_without_type?;
    # The actual event, an object, that happened
    record  { # When the event was dispatched
        string event_ts; # The specific name of the event
        string 'type;}  event;
    # The epoch timestamp in seconds indicating when this event was dispatched.
    int event_time;
    string[][] array_with_inner_array?;
    record {} plain_object?;
    # This should be considered as anydata
    anydata 'anydata?;
};

public type GenericDataType CustomTestSchema|GenericEventWrapper;
