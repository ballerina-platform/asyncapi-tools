package io.ballerina.asyncapi.core.model.message;

import com.fasterxml.jackson.databind.JsonNode;
import io.ballerina.asyncapi.core.model.doc.AsyncApiExternalDocs;
import io.ballerina.asyncapi.core.model.tag.AsyncApiTag;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.Map;

/**
 * Represents a Message Trait Object that defines reusable message properties.
 *
 * @param name          A machine-friendly name for the message.
 * @param title         A human-friendly title for the message.
 * @param summary       A short summary of the message.
 * @param description   A verbose description of the message.
 * @param contentType   The content type of the message payload.
 * @param headers       Schema Object for the message headers.
 * @param correlationId The correlation ID for message tracing.
 * @param tags          A list of tags for API documentation control.
 * @param externalDocs  Additional external documentation.
 * @param bindings      Protocol-specific message bindings.
 * @param examples      A list of example messages.
 * @param extensions    Specification extensions (fields prefixed with "x-").
 */
public record AsyncApiMessageTrait(
        String name,
        String title,
        String summary,
        String description,
        MediaType contentType,
        Object headers,
        AsyncApiCorrelationId correlationId,
        List<AsyncApiTag> tags,
        AsyncApiExternalDocs externalDocs,
        AsyncApiMessageBindings bindings,
        List<AsyncApiMessageExample> examples,
        Map<String, JsonNode> extensions
) {
}
