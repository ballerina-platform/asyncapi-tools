import ballerina/http;
import ballerina/openapi;

@openapi:ServiceInfo {
}
service / on new http:Listener(9090) {
    resource function get greeting() returns string {
        return "Hello, World!";
    }
}



// This has to be changed after implement the @asyncapi: ServiceInfo annotation