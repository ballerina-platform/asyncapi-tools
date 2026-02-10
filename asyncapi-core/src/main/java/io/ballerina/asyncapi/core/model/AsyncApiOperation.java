package io.ballerina.asyncapi.core.model;

import com.fasterxml.jackson.databind.JsonNode;

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
        List<AsyncApiSecurityRequirement> security,
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

    /**
     * Represents the address for an operation reply.
     *
     * @param location    A runtime expression that specifies the location of the reply address.
     * @param description A description of the reply address.
     * @param extensions  Specification extensions (fields prefixed with "x-").
     */
    public record AsyncApiOperationReplyAddress(
            String location,
            String description,
            Map<String, JsonNode> extensions
    ) {
    }

    /**
     * Represents an Operation Trait Object that defines reusable operation properties.
     *
     * @param title        A human-friendly title for the operation.
     * @param summary      A short summary of the operation.
     * @param description  A verbose description of the operation.
     * @param security     A list of security mechanisms available for this operation.
     * @param tags         A list of tags for API documentation control.
     * @param externalDocs Additional external documentation.
     * @param bindings     Protocol-specific operation bindings.
     * @param extensions   Specification extensions (fields prefixed with "x-").
     */
    public record AsyncApiOperationTrait(
            String title,
            String summary,
            String description,
            List<AsyncApiSecurityRequirement> security,
            List<AsyncApiTag> tags,
            AsyncApiExternalDocs externalDocs,
            AsyncApiOperationBindings bindings,
            Map<String, JsonNode> extensions
    ) {
    }

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

    /**
     * HTTP Operation Binding Object.
     *
     * @param method         The HTTP method for the request.
     * @param query          A Schema Object for the query parameters.
     * @param bindingVersion The version of this binding.
     */
    public record HttpOperationBindings(
            HttpMethod method,
            Object query,
            String bindingVersion
    ) {
    }

    /** The HTTP method for the request in an HTTP operation binding. */
    public enum HttpMethod {
        GET,
        POST, 
        PUT, 
        PATCH, 
        DELETE, 
        HEAD, 
        OPTIONS, 
        CONNECT, 
        TRACE
    }

    /** WebSocket Operation Binding Object. Reserved for future use; has no defined properties. */
    public record WsOperationBindings() {
    }

}
