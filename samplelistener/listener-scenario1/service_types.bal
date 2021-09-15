type SlackAppMentionHandlingService service object {
   remote function onAppMention(GenericEventWrapperEvent e);
};

type SlackAppCreatedHandlingService service object {
   remote function onAppCreated(GenericEventWrapperEvent e);
};

public type GenericServiceType SlackAppMentionHandlingService | SlackAppCreatedHandlingService;