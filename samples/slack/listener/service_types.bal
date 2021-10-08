# Service type to handle the events coming to the channel app/mention  
public type AppMentionHandlingService service object {
   remote function onAppMentionAdded(GenericEventWrapperEvent e) returns error?;
   remote function onAppMentionRemoved(GenericEventWrapperEvent e) returns error?;
};

# Service type to handle the events coming to the channel app/create  
public type AppCreatedHandlingService service object {
   remote function onAppCreated(GenericEventWrapperEvent e) returns error?;
};

# Generic service type for all the above service types  
public type GenericServiceType AppMentionHandlingService|AppCreatedHandlingService;
