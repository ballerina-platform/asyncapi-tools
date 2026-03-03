/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com)
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package io.ballerina.asyncapi.core.implementation.v3.message;

import com.fasterxml.jackson.databind.JsonNode;
import io.apicurio.datamodels.models.Tag;
import io.apicurio.datamodels.models.asyncapi.AsyncApiComponents;
import io.apicurio.datamodels.models.asyncapi.AsyncApiMessage;
import io.apicurio.datamodels.models.asyncapi.AsyncApiMessageTrait;
import io.apicurio.datamodels.models.asyncapi.AsyncApiReferenceable;
import io.apicurio.datamodels.models.asyncapi.AsyncApiSchema;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Channel;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Message;
import io.ballerina.asyncapi.core.Constants;
import io.ballerina.asyncapi.core.implementation.common.SchemaMapper;
import io.ballerina.asyncapi.core.implementation.v3.doc.ExternalDocMapperV3;
import io.ballerina.asyncapi.core.implementation.v3.tag.TagMapperV3;
import io.ballerina.asyncapi.core.model.message.AsyncApiMessageExample;
import io.ballerina.asyncapi.core.model.tag.AsyncApiTag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Maps Apicurio Message models to {@link io.ballerina.asyncapi.core.model.message.AsyncApiMessage}
 * and related types for AsyncAPI 3.0.
 */
public final class MessageMapperV3 {

    private static final Logger LOG = LogManager.getLogger(MessageMapperV3.class);

    private MessageMapperV3() {

    }

    /**
     * Extracts all messages from an AsyncAPI 3.0 channel, resolving any {@code $ref} entries.
     *
     * @param channel    the AsyncAPI 3.0 channel object
     * @param components the AsyncAPI components (for $ref resolution)
     * @return a map of message names to model messages, or null if no messages found
     */
    public static Map<String, io.ballerina.asyncapi.core.model.message.AsyncApiMessage> map(
            AsyncApi30Channel channel, AsyncApiComponents components) {
        if (channel == null) {
            return null;
        }
        Map<String, AsyncApi30Message> channelMessages = channel.getMessages();
        if (channelMessages == null || channelMessages.isEmpty()) {
            return null;
        }
        Map<String, io.ballerina.asyncapi.core.model.message.AsyncApiMessage> result = new LinkedHashMap<>();
        for (Map.Entry<String, AsyncApi30Message> entry : channelMessages.entrySet()) {
            String messageName = entry.getKey();
            AsyncApi30Message message = entry.getValue();
            if (message == null) {
                continue;
            }
            io.ballerina.asyncapi.core.model.message.AsyncApiMessage mapped =
                    mapMessageItem(message, components);
            if (mapped != null) {
                result.put(messageName, mapped);
            }
        }
        return result.isEmpty() ? null : result;
    }

    /**
     * Maps a single Apicurio {@link AsyncApi30Message} to a model message,
     * resolving any {@code $ref} before building the model.
     *
     * @param message    the Apicurio message object (may be a reference)
     * @param components the AsyncAPI components (for $ref resolution)
     * @return the mapped model message, or null if message is null or unresolvable
     */
    public static io.ballerina.asyncapi.core.model.message.AsyncApiMessage mapMessageItem(
            AsyncApi30Message message, AsyncApiComponents components) {
        if (message == null) {
            return null;
        }

        if (message instanceof AsyncApiReferenceable referenceable && referenceable.get$ref() != null) {
            AsyncApiMessage resolved = resolveMessageRef(referenceable.get$ref(), components);
            if (resolved == null) {
                LOG.warn("Could not resolve $ref '{}' for message. Skipping.", referenceable.get$ref());
                return null;
            }
            if (!(resolved instanceof AsyncApi30Message typedResolved)) {
                LOG.warn("Resolved $ref '{}' is not an AsyncApi30Message. Skipping.", referenceable.get$ref());
                return null;
            }
            message = typedResolved;
        }

        Object headers = null;
        Object payload = null;
        List<AsyncApiMessageExample> examples = null;
        Map<String, JsonNode> extensions = null;

        if (message instanceof AsyncApi30Message typedMessage) {
            if (typedMessage.getHeaders() instanceof AsyncApiSchema typedSchema) {
                headers = SchemaMapper.map(typedSchema);
            }
            payload = MessagePayloadMapperV3.map(typedMessage.getPayload(), components);
            extensions = typedMessage.getExtensions();
            examples = MessageExampleMapperV3.map(typedMessage);
        }

        List<AsyncApiTag> tags = null;
        List<? extends Tag> apicurioTags = message.getTags();
        if (apicurioTags != null) {
            tags = apicurioTags.stream()
                    .map(tag -> TagMapperV3.map(tag, components))
                    .toList();
        }

        List<io.ballerina.asyncapi.core.model.message.AsyncApiMessageTrait> traits = null;
        List<? extends AsyncApiMessageTrait> apicurioTraits = message.getTraits();
        if (apicurioTraits != null) {
            traits = apicurioTraits.stream()
                    .map(MessageTraitMapperV3::map)
                    .toList();
        }

        return new io.ballerina.asyncapi.core.model.message.AsyncApiMessage(
                headers,
                payload,
                CorrelationIdMapperV3.map(message.getCorrelationId()),
                message.getContentType(),
                message.getName(),
                message.getTitle(),
                message.getSummary(),
                message.getDescription(),
                tags,
                ExternalDocMapperV3.map(message.getExternalDocs(), components),
                MessageBindingsMapperV3.map(message.getBindings()),
                examples,
                traits,
                extensions
        );
    }

    /**
     * Resolves a message {@code $ref} string, following any chain of refs, to the
     * final concrete {@link AsyncApiMessage}. Detects cyclic references.
     *
     * @param $ref       the initial $ref string (e.g., {@code #/components/messages/MyMessage})
     * @param components the AsyncAPI components used for lookup
     * @return the concrete message, or {@code null} if resolution fails
     */
    private static AsyncApiMessage resolveMessageRef(String $ref, AsyncApiComponents components) {
        if (components == null) {
            LOG.warn("Cannot resolve $ref: {}. Components is null.", $ref);
            return null;
        }
        Set<String> visited = new HashSet<>();
        String current = $ref;
        while (current != null) {
            if (!current.startsWith(Constants.MESSAGES_REF_PREFIX)) {
                LOG.warn("Unsupported $ref format: {}. Skipping message.", current);
                return null;
            }
            if (!visited.add(current)) {
                LOG.warn("Cyclic $ref detected: {}. Skipping message.", current);
                return null;
            }
            String msgName = current.substring(Constants.MESSAGES_REF_PREFIX.length());
            Map<String, ? extends AsyncApiMessage> messagesMap = components.getMessages();
            AsyncApiMessage resolved = messagesMap != null ? messagesMap.get(msgName) : null;
            if (resolved == null) {
                LOG.warn("Could not resolve $ref: '{}'. No matching message found.", current);
                return null;
            }
            if (resolved instanceof AsyncApiReferenceable resolvedRef && resolvedRef.get$ref() != null) {
                current = resolvedRef.get$ref();
            } else {
                return resolved;
            }
        }
        return null;
    }
}
