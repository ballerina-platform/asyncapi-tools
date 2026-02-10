package io.ballerina.asyncapi.core.model;

import com.fasterxml.jackson.databind.JsonNode;
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

    /**
     * Represents an example of a message.
     *
     * @param name       A machine-friendly name for the example.
     * @param summary    A short summary of the example.
     * @param headers    Example of the message headers as a map.
     * @param payload    Example of the message payload.
     * @param extensions Specification extensions (fields prefixed with "x-").
     */
    public record AsyncApiMessageExample(
            String name,
            String summary,
            Map<String, Object> headers,
            Object payload,
            Map<String, JsonNode> extensions
    ) {
    }

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

    /**
     * HTTP Message Binding Object.
     *
     * @param headers        A Schema Object containing the definitions for the HTTP headers.
     * @param statusCode     The HTTP response status code.
     * @param bindingVersion The version of this binding.
     */
    public record HttpMessageBindings(
            Object headers,
            Integer statusCode,
            String bindingVersion
    ) {
    }

    /** WebSocket Message Binding Object. Reserved for future use; has no defined properties. */
    public record WsMessageBindings() {
    }

    /**
     * Represents a Correlation ID Object in an AsyncAPI document.
     *
     * @param description A description of the identifier.
     * @param location    A runtime expression that specifies the location of the correlation ID.
     * @param extensions  Specification extensions (fields prefixed with "x-").
     */
    public record AsyncApiCorrelationId(
            String description,
            String location,
            Map<String, JsonNode> extensions
    ) {
    }
}
