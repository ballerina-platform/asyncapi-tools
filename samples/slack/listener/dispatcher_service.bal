import ballerina/http;
import athukorala/eventapi.interop.handler as handler;

service class DispatcherService {
   private map<GenericServiceType> services = {};
   private handler:InteropHandler interopHandler = new ();

   isolated function addServiceRef(string serviceType, GenericServiceType genericServiceType) returns error? {
        if (self.services.hasKey(serviceType)) {
             return error("Service of type " + serviceType + " has already been attached");
        }
        self.services[serviceType] = genericServiceType;
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
        GenericDataType genericDataType = check payload.cloneWithType(GenericDataType);
        match genericDataType.event.'type {
          "app_mention_added" => {
               check self.executeRemoteFunc(genericDataType, "app_mention_added", "AppMentionHandlingService", "onAppMentionAdded");
          }
          "app_mention_removed" => {
               check self.executeRemoteFunc(genericDataType, "app_mention_removed", "AppMentionHandlingService", "onAppMentionRemoved");
          }
          "app_created" => {
               check self.executeRemoteFunc(genericDataType, "app_created", "AppCreatedHandlingService", "onAppCreated");
          }
        }
   }

   private function executeRemoteFunc(GenericDataType genericDataType, string eventName, string serviceTypeStr, string eventFunction) returns error? {
         GenericServiceType? genericServiceType = self.services[serviceTypeStr];
         if genericServiceType is GenericServiceType {
              check self.interopHandler.invokeRemoteFunction(genericDataType, eventName, eventFunction, genericServiceType);
         }
   }
}
