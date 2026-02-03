package io.ballerina.asyncapi.core.model;

import java.util.List;
import java.util.Map;

public record AsyncApiOperation(
        String name,
        String action,
        String channelRef,
        String title,
        String summary,
        String description,
        List<String> messages,
        List<AsyncApiSecurityRequirement> security,
        AsyncApiOperationReply reply,
        List<AsyncApiTag> tags,
        AsyncApiExternalDocs externalDocs,
        AsyncApiBindings bindings,
        List<AsyncApiOperationTrait> traits,
        String ref,
        Map<String, Object> extensions
) {

    public record AsyncApiSecurityRequirement(
            String name,
            List<String> scopes
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

    public record AsyncApiOperationTrait(
            String title,
            String summary,
            String description,
            List<AsyncApiSecurityRequirement> security,
            List<AsyncApiTag> tags,
            AsyncApiExternalDocs externalDocs,
            AsyncApiBindings bindings,
            String ref,
            Map<String, Object> extensions
    ) {
    }
    
}
