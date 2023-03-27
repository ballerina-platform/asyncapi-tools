import ballerina/http;
import ballerina/openapi;

listener http:Listener ep0 = new (9090);

@openapi:serviceInfo {
    contract: "hello_openapi.yaml"
}
service /payloadV on ep0 {
    resource function get pets() returns http:Ok {
        http:Ok ok = {body: ()};
        return ok;
    }
}
//asyncapi_info- project 02 can’t  be done because @asyncapi:ServiceInfo annotation is not yet present