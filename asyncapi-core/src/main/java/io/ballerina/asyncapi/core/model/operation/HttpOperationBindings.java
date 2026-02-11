package io.ballerina.asyncapi.core.model.operation;

/**
 * HTTP Operation Binding Object.
 *
 * @param method         The HTTP method for the request.
 * @param query          A Schema Object for the query parameters.
 * @param bindingVersion The version of this binding.
 */
public record HttpOperationBindings(
        HttpMethod method,
        Object query,
        String bindingVersion
) {

    /** The HTTP method for the request in an HTTP operation binding. */
    public enum HttpMethod {
        GET,
        POST,
        PUT,
        PATCH,
        DELETE,
        HEAD,
        OPTIONS,
        CONNECT,
        TRACE
    }
}
