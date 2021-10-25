import ballerinax/triggers.slack;
import ballerina/io;

listener slack:Listener slackListener = new ("verification_token");

service slack:AppService on slackListener {
    remote function onAppMention(slack:GenericEventWrapper event) returns error? {
        io:println(event.event.'type);
    }

    remote function onAppRateLimited(slack:GenericEventWrapper event) returns error? {}

    remote function onAppUninstalled(slack:GenericEventWrapper event) returns error? {}
}