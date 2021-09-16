# Service type to handle the events coming to the channel app/mention  
public type AppMentionHandlingService service object {
   remote function onAppMentionAdded(GenericEventWrapperEvent e);
   remote function onAppMentionRemoved(GenericEventWrapperEvent e);
};

# Service type to handle the events coming to the channel app/create  
public type AppCreatedHandlingService service object {
   remote function onAppCreated(GenericEventWrapperEvent e);
};

# Generic service type for all the above service types  
public type GenericService AppMentionHandlingService | AppCreatedHandlingService;
