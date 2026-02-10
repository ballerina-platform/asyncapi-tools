package io.ballerina.asyncapi.core.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * Represents the Components Object in an AsyncAPI document.
 * Holds a set of reusable objects for different aspects of the AsyncAPI specification.
 *
 * @param schemas           Reusable Schema Objects.
 * @param servers           Reusable Server Objects.
 * @param serverVariables   Reusable Server Variable Objects.
 * @param channels          Reusable Channel Objects.
 * @param operations        Reusable Operation Objects.
 * @param messages          Reusable Message Objects.
 * @param securitySchemes   Reusable Security Scheme Objects.
 * @param parameters        Reusable Channel Parameter Objects.
 * @param correlationIds    Reusable Correlation ID Objects.
 * @param operationTraits   Reusable Operation Trait Objects.
 * @param messageTraits     Reusable Message Trait Objects.
 * @param replies           Reusable Operation Reply Objects.
 * @param replyAddresses    Reusable Operation Reply Address Objects.
 * @param externalDocs      Reusable External Documentation Objects.
 * @param tags              Reusable Tag Objects.
 * @param serverBindings    Reusable Server Binding Objects.
 * @param channelBindings   Reusable Channel Binding Objects.
 * @param operationBindings Reusable Operation Binding Objects.
 * @param messageBindings   Reusable Message Binding Objects.
 * @param extensions        Specification extensions (fields prefixed with "x-").
 */
public record AsyncApiComponent(
        Map<String, Object> schemas,
        Map<String, AsyncApiServer> servers,
        Map<String, AsyncApiServer.AsyncApiServerVariable> serverVariables,
        Map<String, AsyncApiChannel> channels,
        Map<String, AsyncApiOperation> operations,
        Map<String, AsyncApiMessage> messages,
        Map<String, AsyncApiSecurityRequirement> securitySchemes,
        Map<String, AsyncApiChannel.AsyncApiChannelParameter> parameters,
        Map<String, AsyncApiMessage.AsyncApiCorrelationId> correlationIds,
        Map<String, AsyncApiOperation.AsyncApiOperationTrait> operationTraits,
        Map<String, AsyncApiMessage.AsyncApiMessageTrait> messageTraits,
        Map<String, AsyncApiOperation.AsyncApiOperationReply> replies,
        Map<String, AsyncApiOperation.AsyncApiOperationReplyAddress> replyAddresses,
        Map<String, AsyncApiExternalDocs> externalDocs,
        Map<String, AsyncApiTag> tags,
        Map<String, AsyncApiServer.AsyncApiServerBindings> serverBindings,
        Map<String, AsyncApiChannel.AsyncApiChannelBindings> channelBindings,
        Map<String, AsyncApiOperation.AsyncApiOperationBindings> operationBindings,
        Map<String, AsyncApiMessage.AsyncApiMessageBindings> messageBindings,
        Map<String, JsonNode> extensions
) {


}
