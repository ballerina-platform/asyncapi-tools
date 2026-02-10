package io.ballerina.asyncapi.core.model;

import java.util.List;
import java.util.Map;

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
        Map<String, String> extensions
) {

    public enum Action {
        SEND,
        RECEIVE
    }

    public record AsyncApiOperationReply(
            AsyncApiChannel channel,
            AsyncApiOperationReplyAddress address,
            Map<String, AsyncApiMessage> messages,
            Map<String, String> extensions
    ) {
    }

    public record AsyncApiOperationReplyAddress(
            String location,
            String description,
            Map<String, String> extensions
    ) {
    }

    public record AsyncApiOperationTrait(
            String title,
            String summary,
            String description,
            List<AsyncApiSecurityRequirement> security,
            List<AsyncApiTag> tags,
            AsyncApiExternalDocs externalDocs,
            AsyncApiOperationBindings bindings,
            Map<String, String> extensions
    ) {
    }

    public record AsyncApiOperationBindings(
            HttpOperationBindings httpOperationBindings,
            WsOperationBindings wsOperationBindings
    ) {
    }

    public record HttpOperationBindings(
            HttpMethod method,
            Object query,
            String bindingVersion
    ) {
    }

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

    public record WsOperationBindings() {
    }

}
