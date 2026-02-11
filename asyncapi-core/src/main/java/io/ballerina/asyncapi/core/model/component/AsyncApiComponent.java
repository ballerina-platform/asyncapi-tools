package io.ballerina.asyncapi.core.model.component;

import com.fasterxml.jackson.databind.JsonNode;
import io.ballerina.asyncapi.core.model.channel.AsyncApiChannel;
import io.ballerina.asyncapi.core.model.channel.AsyncApiChannelBindings;
import io.ballerina.asyncapi.core.model.channel.AsyncApiChannelParameter;
import io.ballerina.asyncapi.core.model.doc.AsyncApiExternalDocs;
import io.ballerina.asyncapi.core.model.message.AsyncApiCorrelationId;
import io.ballerina.asyncapi.core.model.message.AsyncApiMessage;
import io.ballerina.asyncapi.core.model.message.AsyncApiMessageBindings;
import io.ballerina.asyncapi.core.model.message.AsyncApiMessageTrait;
import io.ballerina.asyncapi.core.model.operation.AsyncApiOperation;
import io.ballerina.asyncapi.core.model.operation.AsyncApiOperationBindings;
import io.ballerina.asyncapi.core.model.operation.AsyncApiOperationReply;
import io.ballerina.asyncapi.core.model.operation.AsyncApiOperationReplyAddress;
import io.ballerina.asyncapi.core.model.operation.AsyncApiOperationTrait;
import io.ballerina.asyncapi.core.model.security.AsyncApiSecurityScheme;
import io.ballerina.asyncapi.core.model.server.AsyncApiServer;
import io.ballerina.asyncapi.core.model.server.AsyncApiServerBindings;
import io.ballerina.asyncapi.core.model.server.AsyncApiServerVariable;
import io.ballerina.asyncapi.core.model.tag.AsyncApiTag;

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
        Map<String, AsyncApiServerVariable> serverVariables,
        Map<String, AsyncApiChannel> channels,
        Map<String, AsyncApiOperation> operations,
        Map<String, AsyncApiMessage> messages,
        Map<String, AsyncApiSecurityScheme> securitySchemes,
        Map<String, AsyncApiChannelParameter> parameters,
        Map<String, AsyncApiCorrelationId> correlationIds,
        Map<String, AsyncApiOperationTrait> operationTraits,
        Map<String, AsyncApiMessageTrait> messageTraits,
        Map<String, AsyncApiOperationReply> replies,
        Map<String, AsyncApiOperationReplyAddress> replyAddresses,
        Map<String, AsyncApiExternalDocs> externalDocs,
        Map<String, AsyncApiTag> tags,
        Map<String, AsyncApiServerBindings> serverBindings,
        Map<String, AsyncApiChannelBindings> channelBindings,
        Map<String, AsyncApiOperationBindings> operationBindings,
        Map<String, AsyncApiMessageBindings> messageBindings,
        Map<String, JsonNode> extensions
) {


}
