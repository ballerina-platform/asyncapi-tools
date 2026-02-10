package io.ballerina.asyncapi.core.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * Represents a Channel Object in an AsyncAPI document.
 *
 * @param address      The address (topic, routing key, path, etc.) for the channel.
 * @param messages     A map of message names to their definitions available on this channel.
 * @param title        A human-friendly title for the channel.
 * @param summary      A short summary of the channel.
 * @param description  A description of the channel.
 * @param servers      A list of servers this channel is available on.
 * @param parameters   A map of parameter names to their definitions for this channel's address.
 * @param tags         A list of tags for API documentation control.
 * @param externalDocs Additional external documentation for the channel.
 * @param bindings     Protocol-specific channel bindings.
 * @param extensions   Specification extensions (fields prefixed with "x-").
 */
public record AsyncApiChannel(
        String address,
        Map<String, AsyncApiMessage> messages,
        String title,
        String summary,
        String description,
        List<AsyncApiServer> servers,
        Map<String, AsyncApiChannelParameter> parameters,
        List<AsyncApiTag> tags,
        AsyncApiExternalDocs externalDocs,
        AsyncApiChannelBindings bindings,
        Map<String, JsonNode> extensions
) {

    /**
     * Represents a Channel Parameter Object for dynamic channel address components.
     *
     * @param description  A description of the parameter.
     * @param location     A runtime expression that specifies the location of the parameter value.
     * @param defaultValue The default value to use for this parameter.
     * @param enumValues   An enumeration of allowed string values.
     * @param examples     Example values for the parameter.
     * @param extensions   Specification extensions (fields prefixed with "x-").
     */
    public record AsyncApiChannelParameter(
            String description,
            String location,
            String defaultValue,
            List<String> enumValues,
            List<String> examples,
            Map<String, JsonNode> extensions
    ) {
    }

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

    /** HTTP Channel Binding Object. Reserved for future use; has no defined properties. */
    public record HttpChannelBindings() {
    }

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
}
