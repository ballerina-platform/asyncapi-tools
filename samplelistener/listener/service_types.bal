# Service type to handle the events coming to the channel app/mention  
public type AppHandlingService service object {
   remote function onAppMention(GenericEventWrapperEvent e) returns error?;
};

# Service type to handle the events coming to the channel app/create  
public type ChannelHandlingService service object {
   remote function onChannelCreated(GenericEventWrapperEvent e) returns error?;
   remote function onChannelDeleted(GenericEventWrapperEvent e) returns error?;
};

# Generic service type for all the above service types  
public type GenericService AppHandlingService|ChannelHandlingService;
