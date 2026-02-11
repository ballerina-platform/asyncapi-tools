package io.ballerina.asyncapi.core.model.message;

/**
 * HTTP Message Binding Object.
 *
 * @param headers        A Schema Object containing the definitions for the HTTP headers.
 * @param statusCode     The HTTP response status code.
 * @param bindingVersion The version of this binding.
 */
public record HttpMessageBindings(
        Object headers,
        Integer statusCode,
        String bindingVersion
) {
}
