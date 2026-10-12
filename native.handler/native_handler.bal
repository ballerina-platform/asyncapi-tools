import ballerina/data.jsondata as _;
import ballerina/jballerina.java;

public class NativeHandler {
    public isolated function invokeRemoteFunction(any event, string eventName, string eventFunction, service object {} serviceObj) returns error? = @java:Method {
        'class: "io.ballerinax.event.NativeHttpToEventAdaptor"
    } external;

    public isolated function bindEventPayload(service object {} serviceObj, string eventFunction, json payload) returns any|error = @java:Method {
        'class: "io.ballerinax.event.NativeHttpToEventAdaptor"
    } external;
}
