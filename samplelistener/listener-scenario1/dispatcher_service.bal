import ballerina/http;
import ballerina/jballerina.java;

service class DispatcherService {
   private GenericServiceType serviceRef;
   //private string verificationToken;

   isolated function init(GenericServiceType serviceRef) returns error? { //Receive verification token
        self.serviceRef = serviceRef;
        //self.verificationToken = "9asdbas9009123nas1e2";
   }

   isolated resource function post events (http:Caller caller, http:Request request) returns error? {
        json payload = check request.getJsonPayload();
        // string eventOrVerification = check payload.'type;

        // if (payload.token !== self.verificationToken) {
        //     return error("Verification token mismatch");
        // }

        // if (eventOrVerification == URL_VERIFICATION) {
        //     check self.verifyURL(caller, payload);
        // } else if (eventOrVerification == EVENT_CALLBACK) {
        // }

        GenericEventWrapperEvent genericEvent = check payload.cloneWithType(GenericEventWrapperEvent);
        if genericEvent.event.'type == "app_mention" {
                SlackAppMentionHandlingService serviceReference = <SlackAppMentionHandlingService> self.serviceRef;
                var s = check self.callOnAppEvent(genericEvent, "app_mention", "onAppMention", serviceReference);
        }
   }

    isolated function callOnAppEvent(GenericEventWrapperEvent event, string eventName, string eventFunction, any serviceObj) returns error?
    = @java:Method {
        'class: "io.ballerinax.event.NativeHttpToEventAdaptor"
    } external;

    //Respomnd to verification token
    //isolated function verifyURL(http:Caller caller, json payload) returns @untainted error? {
    //     http:Response response = new;
    //     response.statusCode = http:STATUS_OK;
    //     response.setPayload({challenge: check <@untainted>payload.challenge});
    //     check caller->respond(response);
    //     log:printInfo("Request URL Verified");
    // }
}

