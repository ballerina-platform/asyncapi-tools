package io.ballerina.asyncapi.core.model.message;

import com.fasterxml.jackson.databind.JsonNode;
import io.ballerina.asyncapi.core.model.doc.AsyncApiExternalDocs;
import io.ballerina.asyncapi.core.model.tag.AsyncApiTag;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.Map;

/**
 * Represents a Message Object in an AsyncAPI document.
 *
 * @param headers       Schema Object or Reference Object for the message headers.
 * @param payload       Schema Object or Reference Object for the message payload.
 * @param correlationId The correlation ID used for message tracing.
 * @param contentType   The content type of the message payload (e.g., "application/json").
 * @param name          A machine-friendly name for the message.
 * @param title         A human-friendly title for the message.
 * @param summary       A short summary of the message.
 * @param description   A verbose description of the message.
 * @param tags          A list of tags for API documentation control.
 * @param externalDocs  Additional external documentation for the message.
 * @param bindings      Protocol-specific message bindings.
 * @param examples      A list of example messages.
 * @param traits        A list of traits to apply to the message.
 * @param extensions    Specification extensions (fields prefixed with "x-").
 */
public record AsyncApiMessage(
        Object headers,
        Object payload,
        AsyncApiCorrelationId correlationId,
        MediaType contentType,
        String name,
        String title,
        String summary,
        String description,
        List<AsyncApiTag> tags,
        AsyncApiExternalDocs externalDocs,
        AsyncApiMessageBindings bindings,
        List<AsyncApiMessageExample> examples,
        List<AsyncApiMessageTrait> traits,
        Map<String, JsonNode> extensions
) {
}
