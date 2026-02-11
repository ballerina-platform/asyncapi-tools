package io.ballerina.asyncapi.core.model.channel;

/**
 * Represents protocol-specific channel binding definitions.
 * This structure is designed to be extensible to accommodate future
 * protocol implementations and additional binding properties.
 *
 * @param httpChannelBindings HTTP-specific channel binding properties.
 * @param wsChannelBindings   WebSocket-specific channel binding properties.
 */
public record AsyncApiChannelBindings(
        HttpChannelBindings httpChannelBindings,
        WsChannelBindings wsChannelBindings
) {
}
