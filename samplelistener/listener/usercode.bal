import ballerina/io;

listener SlackListener slackListener = new ();

service on slackListener {
   remote function onAppMention(GenericEventWrapperEvent e) {
      io:println("onAppMention");
   }
}
