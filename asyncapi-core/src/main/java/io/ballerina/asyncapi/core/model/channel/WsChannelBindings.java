package io.ballerina.asyncapi.core.model.channel;

/**
 * WebSocket Channel Binding Object.
 *
 * @param method         The HTTP method to use when establishing the WebSocket connection.
 * @param query          A Schema Object containing the definitions for the query parameters.
 * @param headers        A Schema Object containing the definitions for the HTTP headers.
 * @param bindingVersion The version of this binding.
 */
public record WsChannelBindings(
        String method,
        Object query,
        Object headers,
        String bindingVersion
) {
}
