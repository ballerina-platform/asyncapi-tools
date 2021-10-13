public type AppMentionHandlingService service object {
    remote function onAppMentionAdded(GenericEventWrapper event) returns error?;
    remote function onAppMentionRemoved(GenericEventWrapper event) returns error?;
};

public type AppRateLimitedHandlingService service object {
    remote function onAppRateLimited(GenericEventWrapper event) returns error?;
};

public type AppCreatedHandlingService service object {
    remote function onAppCreated(CustomTestSchema event) returns error?;
};

public type GenericServiceType AppMentionHandlingService|AppRateLimitedHandlingService|AppCreatedHandlingService;
