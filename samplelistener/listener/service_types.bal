type SlackAppMentionHandlingService service object {
   remote function onAppMention(GenericEventWrapperEvent e);
};

type SlackAppCreatedHandlingService service object {
   remote function onAppCreated(GenericEventWrapperEvent e);
};


type genericType SlackAppMentionHandlingService | SlackAppCreatedHandlingService;