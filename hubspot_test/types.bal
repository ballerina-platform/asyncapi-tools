import ballerina/http;

public type ListenerConfiguration record {|
    *http:ListenerConfiguration;
    string webhookSecret = "";
|};

public type WebhookEvent record {
    # The ID of the event that triggered this notification. This value is not guaranteed to be unique.
    int eventId;
    # The ID of the subscription that triggered a notification about the event.
    int subscriptionId?;
    # The customer's (HubSpot account ID)[https://knowledge.hubspot.com/account/manage-multiple-hubspot-accounts?_ga=2.56562472.2054080341.1656611011-2068059512.1656469161#check-your-current-account] where the event occurred.
    int portalId?;
    # The ID of the HubSpot application
    int appId?;
    # When this event occurred as a millisecond timestamp.
    int occurredAt?;
    # Type of the event
    string subscriptionType?;
    # Starting at 0, which number attempt this is to notify your service of this event. If your service times-out or throws an error as describe in the Retries section below, HubSpot will attempt to send the notification again.
    int attemptNumber?;
    # The ID of the object that was created, changed, or deleted. For contacts this is the contact ID; for companies, the company ID; for deals, the deal ID; and for conversations the thread ID
    int objectId?;
    # The source of the change. This can be any of the change sources that appear in contact property histories.
    string changeSource?;
    # The ID of the source of the change. The format depends on the change source (for example, "userId:12345" for changes made by a user in the CRM UI).
    string sourceId?;
    # Flag of the change.
    string changeFlag?;
    # The name of the property changed. Present for propertyChange events.
    string propertyName?;
    # The new value set for the property that triggered the notification. Present for propertyChange events.
    string propertyValue?;
    # The type of the association (e.g. CONTACT_TO_COMPANY, DEAL_TO_LINE_ITEM). Present for associationChange events.
    string associationType?;
    # The ID of the record that the association change was made from. Present for associationChange events.
    int fromObjectId?;
    # The ID of the secondary record in the association event. Present for associationChange events.
    int toObjectId?;
    # Whether the association was removed (true) or added (false). Present for associationChange events.
    boolean associationRemoved?;
    # Whether the secondary record is the primary association. Present for associationChange events.
    boolean isPrimaryAssociation?;
    # The ID of the merge winner, which is the record that remains after the merge. Present for merge events.
    int primaryObjectId?;
    # An array of IDs that represent the records merged into the winner. Present for merge events.
    int[] mergedObjectIds?;
    # The ID of the record created as a result of the merge. Present for merge events.
    int newObjectId?;
    # How many properties were transferred during the merge. Present for merge events.
    int numberOfPropertiesMoved?;
    # The ID of the new message. Present for conversation.newMessage events.
    int messageId?;
    # The type of the new message, either MESSAGE or COMMENT. Present for conversation.newMessage events.
    string messageType?;
};

public type GenericDataType WebhookEvent;
