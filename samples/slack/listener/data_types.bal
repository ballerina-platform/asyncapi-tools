public type GenericEventWrapper record {
    # A unique identifier for this specific event, globally unique across all workspaces.
    string event_id;
    # The unique identifier your installed Slack application.
    string api_app_id;
    # The unique identifier of the workspace where the event occurred
    string team_id;
    # The actual event, an object, that happened
    record  { # When the event was dispatched
        string event_ts; # The message content
        string text?; # The specific name of the event
        string 'type;}  event;
    # Indicates which kind of event dispatch this is, usually `event_callback`
    string 'type;
    # The epoch timestamp in seconds indicating when this event was dispatched.
    int event_time;
    # A verification token to validate the event originated from Slack
    string token;
};

public type Message record {
    string[] authed_users;
    string event_id;
    # Your Slack app's unique ID
    string api_app_id;
    # The unique identifier of the team/workspace where the event happened
    string team_id;
    record  { string deleted_ts; boolean hidden; record  { record  { anydata preview; string filetype; string title; anydata[] ims; string mode; string url_private; string id; string state; boolean display_as_bot; int timestamp; string editor; int created; boolean editable; string last_editor; anydata[] groups; boolean is_external; string pretty_type; string external_type; string url_private_download; string permalink_public; string[] channels; int size; int comments_count; boolean is_public; string name; string mimetype; string permalink; boolean public_url_shared; int updated; string user; string username;}  file; string subtype; anydata comment; string text; string 'type; string ts;}  previous_message; string subtype; string event_ts; string 'channel; string 'type; string ts;}  event;
    string 'type;
    int event_time;
    # Verification token used to validate the origin of the event
    string token;
};

public type GenericDataType GenericEventWrapper|Message;
