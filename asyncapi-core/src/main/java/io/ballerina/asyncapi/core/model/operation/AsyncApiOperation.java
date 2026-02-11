package io.ballerina.asyncapi.core.model.operation;

import com.fasterxml.jackson.databind.JsonNode;
import io.ballerina.asyncapi.core.model.channel.AsyncApiChannel;
import io.ballerina.asyncapi.core.model.doc.AsyncApiExternalDocs;
import io.ballerina.asyncapi.core.model.message.AsyncApiMessage;
import io.ballerina.asyncapi.core.model.security.AsyncApiSecurityScheme;
import io.ballerina.asyncapi.core.model.tag.AsyncApiTag;

import java.util.List;
import java.util.Map;

/**
 * Represents an Operation Object in an AsyncAPI document.
 *
 * @param action      The action this operation performs (send or receive).
 * @param channel     The channel this operation is associated with.
 * @param title       A human-friendly title for the operation.
 * @param summary     A short summary of the operation.
 * @param description A verbose description of the operation.
 * @param messages    A list of messages associated with this operation.
 * @param security    A list of security mechanisms available for this operation.
 * @param reply       The definition of the reply for this operation.
 * @param tags        A list of tags for API documentation control.
 * @param externalDocs Additional external documentation for the operation.
 * @param bindings    Protocol-specific operation bindings.
 * @param traits      A list of traits to apply to the operation.
 * @param extensions  Specification extensions (fields prefixed with "x-").
 */
public record AsyncApiOperation(
        Action action,
        AsyncApiChannel channel,
        String title,
        String summary,
        String description,
        List<AsyncApiMessage> messages,
        List<AsyncApiSecurityScheme> security,
        AsyncApiOperationReply reply,
        List<AsyncApiTag> tags,
        AsyncApiExternalDocs externalDocs,
        AsyncApiOperationBindings bindings,
        List<AsyncApiOperationTrait> traits,
        Map<String, JsonNode> extensions
) {

    /** The action type indicating whether an operation sends or receives messages. */
    public enum Action {
        SEND,
        RECEIVE
    }
}
