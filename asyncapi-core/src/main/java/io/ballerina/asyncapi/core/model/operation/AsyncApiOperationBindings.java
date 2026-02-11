package io.ballerina.asyncapi.core.model.operation;

/**
 * Represents protocol-specific operation binding definitions.
 * This structure is designed to be extensible to accommodate future
 * protocol implementations and additional binding properties.
 *
 * @param httpOperationBindings HTTP-specific operation binding properties.
 * @param wsOperationBindings   WebSocket-specific operation binding properties.
 */
public record AsyncApiOperationBindings(
        HttpOperationBindings httpOperationBindings,
        WsOperationBindings wsOperationBindings
) {
}
