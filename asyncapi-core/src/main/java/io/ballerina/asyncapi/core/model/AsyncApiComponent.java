package io.ballerina.asyncapi.core.model;

import java.util.Map;

public record AsyncApiComponent(
        Map<String, Object> schemas,
        Map<String, AsyncApiServer> servers,
        Map<String, AsyncApiServer.AsyncApiServerVariable> serverVariables,
        Map<String, AsyncApiChannel> channels,
        Map<String, AsyncApiOperation> operations,
        Map<String, AsyncApiMessage> messages,
        Map<String, AsyncApiSecurityRequirement> securitySchemes,
        Map<String, AsyncApiChannel.AsyncApiChannelParameter> parameters,
        Map<String, AsyncApiCorrelationId> correlationIds,
        Map<String, AsyncApiOperation.AsyncApiOperationTrait> operationTraits,
        Map<String, AsyncApiMessage.AsyncApiMessageTrait> messageTraits,
        Map<String, AsyncApiOperation.AsyncApiOperationReply> replies,
        Map<String, AsyncApiOperation.AsyncApiOperationReplyAddress> replyAddresses,
        Map<String, AsyncApiExternalDocs> externalDocs,
        Map<String, AsyncApiTag> tags,
        Map<String, AsyncApiBindings> serverBindings,
        Map<String, AsyncApiBindings> channelBindings,
        Map<String, AsyncApiBindings> operationBindings,
        Map<String, AsyncApiBindings> messageBindings,
        Map<String, String> extensions
) {


}
