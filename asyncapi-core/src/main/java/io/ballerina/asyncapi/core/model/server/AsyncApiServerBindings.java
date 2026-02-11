package io.ballerina.asyncapi.core.model.server;

/**
 * Represents protocol-specific server binding definitions.
 * This structure is designed to be extensible to accommodate future
 * protocol implementations and additional binding properties.
 *
 * @param httpServerBindings HTTP-specific server binding properties.
 * @param wsServerBindings   WebSocket-specific server binding properties.
 */
public record AsyncApiServerBindings(
        HttpServerBindings httpServerBindings,
        WsServerBindings wsServerBindings
) {
}
