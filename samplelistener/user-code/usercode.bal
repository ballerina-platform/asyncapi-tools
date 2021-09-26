import ballerina/io;
import ballerinax/slack.'listener as slackL;

configurable string verificationCode = ?;

listener slackL:Listener slackListener = new (verificationCode); //verification code

// We are not using the `service AppMentionHandlingService on slackListener` notation because of a bug in Ballerina.
// Issue: https://github.com/ballerina-platform/ballerina-lang/issues/32898
service on slackListener {
   remote function onAppMention(slackL:GenericEventWrapperEvent e) returns error? {
      io:println("onAppMention");
   }
}

service on slackListener {
   remote function onChannelCreated(slackL:GenericEventWrapperEvent e) returns error? {
      io:println("onChannelCreated");
   }
   remote function onChannelDeleted(slackL:GenericEventWrapperEvent e) returns error? {
      io:println("onChannelDeleted");
   }
}
