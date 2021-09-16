import ballerina/http;
import ballerina/jballerina.java;

service class DispatcherService {
     private map<GenericService> services = {};

   isolated function addServiceRef(string serviceType, GenericService genericService) returns error? {
        if (self.services.hasKey(serviceType)) {
             return error("Same service type has been defined more than one time");
        }
        self.services[serviceType] = genericService;
   }

   // We are not using the (@http:payload GenericEventWrapperEvent g) notation because of 
   // a bug in Ballerina. (Unable to handle complex objects as payloads)
   resource function post events (http:Caller caller, http:Request request) returns error? {
        json payload = check request.getJsonPayload();

        GenericEventWrapperEvent genericEvent = check payload.cloneWithType(GenericEventWrapperEvent);
        match genericEvent.event.'type {
             "app_mention_added" => {
                if self.services.hasKey("AppMentionHandlingService") {
                    check self.callOnAppEvent(genericEvent, genericEvent.event.'type, "onAppMentionAdded", self.services["AppMentionHandlingService"]);
               }
             }
             "app_mention_removed" => {
                if self.services.hasKey("AppMentionHandlingService") {
                    check self.callOnAppEvent(genericEvent, genericEvent.event.'type, "onAppMentionRemoved", self.services["AppMentionHandlingService"]);
               }
             }
             "app_created" => {
               if self.services.hasKey("AppCreatedHandlingService") {
                    check self.callOnAppEvent(genericEvent, genericEvent.event.'type, "onAppCreated", self.services["AppCreatedHandlingService"]);
               }
             }
        }
   }

    isolated function callOnAppEvent(GenericEventWrapperEvent event, string eventName, string eventFunction, any serviceObj) returns error?
    = @java:Method {
        'class: "io.ballerinax.event.NativeHttpToEventAdaptor"
    } external;
}

