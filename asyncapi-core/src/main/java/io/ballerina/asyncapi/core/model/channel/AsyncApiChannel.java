package io.ballerina.asyncapi.core.model.channel;

import com.fasterxml.jackson.databind.JsonNode;
import io.ballerina.asyncapi.core.model.doc.AsyncApiExternalDocs;
import io.ballerina.asyncapi.core.model.message.AsyncApiMessage;
import io.ballerina.asyncapi.core.model.server.AsyncApiServer;
import io.ballerina.asyncapi.core.model.tag.AsyncApiTag;

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
}
