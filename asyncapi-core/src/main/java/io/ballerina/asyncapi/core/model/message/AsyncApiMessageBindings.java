package io.ballerina.asyncapi.core.model.message;

/**
 * Represents protocol-specific message binding definitions.
 * This structure is designed to be extensible to accommodate future
 * protocol implementations and additional binding properties.
 *
 * @param httpMessageBindings HTTP-specific message binding properties.
 * @param wsMessageBindings   WebSocket-specific message binding properties.
 */
public record AsyncApiMessageBindings(
        HttpMessageBindings httpMessageBindings,
        WsMessageBindings wsMessageBindings
) {
}
