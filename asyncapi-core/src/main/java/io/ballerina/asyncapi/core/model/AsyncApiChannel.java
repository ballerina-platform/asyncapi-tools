package io.ballerina.asyncapi.core.model;

import java.util.List;
import java.util.Map;

public record AsyncApiChannel(
        String address,
        Map<String, AsyncApiMessage> messages,
        String title,
        String summary,
        String description,
        List<AsyncApiServer> servers,
        Map<String, AsyncApiChannelParameter> parameters,
        List<AsyncApiTag> tags,
        AsyncApiExternalDocs externalDocs,
        AsyncApiChannelBindings bindings,
        Map<String, String> extensions
) {

    public record AsyncApiChannelParameter(
            String description,
            String location,
            String defaultValue,
            List<String> enumValues,
            List<String> examples,
            Map<String, String> extensions
    ) {
    }

    public record AsyncApiChannelBindings(
            HttpChannelBindings httpChannelBindings,
            WsChannelBindings wsChannelBindings
    ) {
    }

    public record HttpChannelBindings() {
    }

    public record WsChannelBindings(
            String method,
            Object query,
            Object headers,
            String bindingVersion
    ) {
    }
}
