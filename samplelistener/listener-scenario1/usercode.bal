import ballerina/io;

listener SlackListener slackListener1 = new ();

service / on slackListener1 {
   remote function onAppMention(GenericEventWrapperEvent e) {
      io:println("onAppMention");
   }
}

listener SlackListener slackListener2 = new ();

service / on slackListener2 {
   remote function onAppCreated(GenericEventWrapperEvent e) {
      io:println("onAppCreated");
   }
}
