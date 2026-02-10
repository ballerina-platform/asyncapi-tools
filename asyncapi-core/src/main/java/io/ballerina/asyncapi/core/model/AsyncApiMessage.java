package io.ballerina.asyncapi.core.model;

import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.Map;

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
        Map<String, String> extensions
) {

    public record AsyncApiMessageExample(
            String name,
            String summary,
            Map<String, Object> headers,
            Object payload,
            Map<String, String> extensions
    ) {
    }

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
            Map<String, String> extensions
    ) {
    }

    public record AsyncApiMessageBindings(
            HttpMessageBindings httpMessageBindings,
            WsMessageBindings wsMessageBindings
    ) {
    }

    public record HttpMessageBindings(
            Object headers,
            Integer statusCode,
            String bindingVersion
    ) {
    }

    public record WsMessageBindings() {
    }
}
