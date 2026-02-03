package io.ballerina.asyncapi.core.model;

import java.util.List;
import java.util.Map;

public record AsyncApiChannel(
        String name,
        String address,
        Map<String, AsyncApiMessage> messages,
        String title,
        String summary,
        String description,
        List<String> servers,
        Map<String, AsyncApiChannelParameter> parameters,
        List<AsyncApiTag> tags,
        AsyncApiExternalDocs externalDocs,
        AsyncApiBindings bindings,
        Map<String, Object> extensions
) {

    public record AsyncApiMessage(
            String name,
            String messageId,
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
            List<AsyncApiMessageTrait> traits,
            String ref,
            Map<String, Object> extensions
    ) {
    }

    public record AsyncApiChannelParameter(
            String name,
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

    public record AsyncApiMessageExample(
            String name,
            String summary,
            Map<String, Object> headers,
            Object payload,
            Map<String, Object> extensions
    ) {
    }

    public record AsyncApiMessageTrait(
            String messageId,
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
}
