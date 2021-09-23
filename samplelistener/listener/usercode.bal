import ballerina/io;

listener Listener slackListener = new ();

// We are not using the `service AppMentionHandlingService on slackListener` notation because of a bug in Ballerina.
// Issue: https://github.com/ballerina-platform/ballerina-lang/issues/32898
service on slackListener {
   remote function onAppMentionAdded(GenericEventWrapperEvent e) returns error? {
      io:println("onAppMentionAdded");
   }
   remote function onAppMentionRemoved(GenericEventWrapperEvent e) returns error? {
      io:println("onAppMentionRemoved");
   }
}

service on slackListener {
   remote function onAppCreated(GenericEventWrapperEvent e) returns error? {
      io:println("onAppCreated");
   }
}
