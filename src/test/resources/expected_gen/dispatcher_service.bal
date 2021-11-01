import ballerina/http;
import ballerinax/asyncapi.native.handler;

service class DispatcherService {
    private map<GenericServiceType> services = {};
    private handler:NativeHandler nativeHandler = new ();

    isolated function addServiceRef(string serviceType, GenericServiceType genericService) returns error? {
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
    resource function post .(http:Caller caller, http:Request request) returns error? {
        json payload = check request.getJsonPayload();
        GenericDataType genericDataType = check payload.cloneWithType(GenericDataType);
        check self.matchRemoteFunc(genericDataType);
        check caller->respond(http:STATUS_OK);
    }

    private function matchRemoteFunc(GenericDataType genericDataType) returns error? {
        match genericDataType.event.'type {
            "app_mention_added" => {
                check self.executeRemoteFunc(genericDataType, "app_mention_added", "AppMentionHandlingService", "onAppMentionAdded");
            }
            "app_mention_removed" => {
                check self.executeRemoteFunc(genericDataType, "app_mention_removed", "AppMentionHandlingService", "onAppMentionRemoved");
            }
            "app_rate_limited" => {
                check self.executeRemoteFunc(genericDataType, "app_rate_limited", "AppRateLimitedHandlingService", "onAppRateLimited");
            }
            "app_created" => {
                check self.executeRemoteFunc(genericDataType, "app_created", "AppCreatedHandlingService", "onAppCreated");
            }
        }
    }

    private function executeRemoteFunc(GenericDataType genericEvent, string eventName, string serviceTypeStr, string eventFunction) returns error? {
        GenericServiceType? genericService = self.services[serviceTypeStr];
        if genericService is GenericServiceType {
            check self.nativeHandler.invokeRemoteFunction(genericEvent, eventName, eventFunction, genericService);
        }
    }
}
