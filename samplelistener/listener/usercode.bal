import ballerina/io;

listener Listener slackListener = new ();

service on slackListener {
   remote function onAppMentionAdded(GenericEventWrapperEvent e) {
      io:println("onAppMentionAdded");
   }
   remote function onAppMentionRemoved(GenericEventWrapperEvent e) {
      io:println("onAppMentionRemoved");
   }
}

service on slackListener {
   remote function onAppCreated(GenericEventWrapperEvent e) {
      io:println("onAppCreated");
   }
}
