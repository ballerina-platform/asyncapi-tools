package io.ballerina.asyncapi.core.model;

import java.util.List;
import java.util.Map;

public record AsyncApiComponent(
        Map<String, AsyncApiSchema> schemas,
        Map<String, AsyncApiServer> servers,
        Map<String, AsyncApiServer.AsyncApiServerVariable> serverVariables,
        Map<String, AsyncApiChannel> channels,
        Map<String, AsyncApiOperation> operations,
        Map<String, AsyncApiMessage> messages,
        Map<String, AsyncApiSecurityScheme> securitySchemes,
        Map<String, AsyncApiParameter> parameters,
        Map<String, AsyncApiCorrelationId> correlationIds,
        Map<String, AsyncApiOperationTrait> operationTraits,
        Map<String, AsyncApiMessageTrait> messageTraits,
        Map<String, AsyncApiOperationReply> replies,
        Map<String, AsyncApiOperationReplyAddress> replyAddresses,
        Map<String, AsyncApiExternalDocs> externalDocs,
        Map<String, AsyncApiTag> tags,
        Map<String, AsyncApiBindings> serverBindings,
        Map<String, AsyncApiBindings> channelBindings,
        Map<String, AsyncApiBindings> operationBindings,
        Map<String, AsyncApiBindings> messageBindings,
        Map<String, Object> extensions
) {

    public record AsyncApiSchema(
            String schemaFormat,
            Object schema,
            String ref,
            Map<String, Object> extensions
    ) {
    }

    public record AsyncApiMessage(
            String name,
            String title,
            String summary,
            String description,
            String contentType,
            Object payload,
            Object headers,
            String correlationId,
            List<AsyncApiTag> tags,
            AsyncApiExternalDocs externalDocs,
            AsyncApiBindings bindings,
            List<AsyncApiMessageExample> examples,
            List<String> traits,
            Map<String, Object> extensions
    ) {
    }

    public record AsyncApiMessageExample(
            String name,
            String summary,
            Map<String, Object> headers,
            Object payload
    ) {
    }

    public record AsyncApiSecurityScheme(
            String type,
            String description,
            String name,
            String in,
            String scheme,
            String bearerFormat,
            AsyncApiOAuthFlows flows,
            String openIdConnectUrl,
            List<String> scopes,
            String ref,
            Map<String, Object> extensions
    ) {
    }

    public record AsyncApiOAuthFlows(
            AsyncApiOAuthFlow implicit,
            AsyncApiOAuthFlow password,
            AsyncApiOAuthFlow clientCredentials,
            AsyncApiOAuthFlow authorizationCode,
            Map<String, Object> extensions
    ) {
    }

    public record AsyncApiOAuthFlow(
            String authorizationUrl,
            String tokenUrl,
            String refreshUrl,
            Map<String, String> availableScopes,
            Map<String, Object> extensions
    ) {
    }

    public record AsyncApiParameter(
            String description,
            Object schema,
            String location,
            String defaultValue,
            List<String> enumValues,
            List<String> examples,
            String ref,
            Map<String, Object> extensions
    ) {
    }

    public record AsyncApiCorrelationId(
            String description,
            String location,
            String ref,
            Map<String, Object> extensions
    ) {
    }

    public record AsyncApiOperationTrait(
            String title,
            String summary,
            String description,
            List<Map<String, List<String>>> security,
            List<AsyncApiTag> tags,
            AsyncApiExternalDocs externalDocs,
            AsyncApiBindings bindings,
            String ref,
            Map<String, Object> extensions
    ) {
    }

    public record AsyncApiMessageTrait(
            String title,
            String summary,
            String description,
            String contentType,
            Object headers,
            String correlationId,
            List<AsyncApiTag> tags,
            AsyncApiExternalDocs externalDocs,
            AsyncApiBindings bindings,
            List<AsyncApiMessageExample> examples,
            String ref,
            Map<String, Object> extensions
    ) {
    }

    public record AsyncApiOperationReply(
            String channel,
            AsyncApiOperationReplyAddress address,
            List<String> messages,
            String ref,
            Map<String, Object> extensions
    ) {
    }

    public record AsyncApiOperationReplyAddress(
            String location,
            String description,
            String ref,
            Map<String, Object> extensions
    ) {
    }

}
