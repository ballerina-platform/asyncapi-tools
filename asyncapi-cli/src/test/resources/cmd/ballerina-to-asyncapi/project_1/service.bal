import ballerina/http;
import ballerina/openapi;

@openapi:ServiceInfo {
    contract: "hello_openapi.yaml"
}
service / on new http:Listener(9090) {
    resource function get greeting() returns string {
        return "Hello, World!";
    }
}


// This has to be changed after implement the @asyncapi: ServiceInfo annotation