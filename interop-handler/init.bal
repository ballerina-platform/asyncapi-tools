import ballerina/jballerina.java;

isolated function init() {
    setModule();
}

isolated function setModule() = @java:Method {
    'class: "io.ballerinax.event.ModuleUtils"
} external;
