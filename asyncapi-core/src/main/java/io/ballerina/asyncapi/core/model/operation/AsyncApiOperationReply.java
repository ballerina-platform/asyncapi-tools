package io.ballerina.asyncapi.core.model.operation;

import com.fasterxml.jackson.databind.JsonNode;
import io.ballerina.asyncapi.core.model.channel.AsyncApiChannel;
import io.ballerina.asyncapi.core.model.message.AsyncApiMessage;

import java.util.Map;

/**
 * Represents the reply definition for an operation.
 *
 * @param channel    The channel where the reply is sent.
 * @param address    The address for the reply.
 * @param messages   A map of message names to their definitions for the reply.
 * @param extensions Specification extensions (fields prefixed with "x-").
 */
public record AsyncApiOperationReply(
        AsyncApiChannel channel,
        AsyncApiOperationReplyAddress address,
        Map<String, AsyncApiMessage> messages,
        Map<String, JsonNode> extensions
) {
}
