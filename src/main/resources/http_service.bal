import ballerina/http;
import ballerina/jballerina.java;

service class DispatcherService {
   private map<GenericService> services = {};

   isolated function addServiceRef(string serviceType, GenericService genericService) returns error? {
        if (self.services.hasKey(serviceType)) {
             return error("Service of type " + serviceType + " has already been attached");
        }
        self.services[serviceType] = genericService;
   }

   isolated function removeServiceRef(string serviceType) returns error? {
        if (!self.services.hasKey(serviceType)) {
             return error("Cannot detach the service of type " + serviceType + ". Service has not been attached to the listener before");
        }
        _ = self.services.remove(serviceType);
   }

   // We are not using the (@http:payload GenericEventWrapperEvent g) notation because of a bug in Ballerina.
   // Issue: https://github.com/ballerina-platform/ballerina-lang/issues/32859
   resource function post events (http:Caller caller, http:Request request) returns error? {
        json payload = check request.getJsonPayload();
        GenericEventWrapperEvent genericEvent = check payload.cloneWithType(GenericEventWrapperEvent);
        match genericEvent.event.'type {
          "app_mention" => {
               check self.executeRemoteFunc(genericEvent, "AppHandlingService", "onAppMention");
          }
          "channel_created" => {
               check self.executeRemoteFunc(genericEvent, "ChannelHandlingService", "onChannelCreated");
          }
          "channel_deleted" => {
               check self.executeRemoteFunc(genericEvent, "ChannelHandlingService", "onChannelDeleted");
          }
        }
        check caller->respond(http:STATUS_OK);
   }

   private function executeRemoteFunc(GenericDataType genericEvent, string eventName, string serviceTypeStr, string eventFunction) returns error? {
     if self.services.hasKey(serviceTypeStr) {
          check self.callOnAppEvent(genericEvent, eventName, eventFunction, self.services[serviceTypeStr]);
     }
   }

    isolated function callOnAppEvent(GenericEventWrapperEvent event, string eventName, string eventFunction, any serviceObj) returns error?
    = @java:Method {
        'class: "io.ballerinax.event.NativeHttpToEventAdaptor"
    } external;

    isolated function verifyURL(http:Caller caller, json payload) returns @untainted error? {
        http:Response response = new;
        response.statusCode = http:STATUS_OK;
        response.setPayload({challenge: check <@untainted>payload.challenge});
        check caller->respond(response);
    }
}

