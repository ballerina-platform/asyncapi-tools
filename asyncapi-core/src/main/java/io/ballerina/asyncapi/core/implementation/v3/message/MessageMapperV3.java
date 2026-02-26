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
import io.apicurio.datamodels.models.asyncapi.*;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Channel;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Message;
import io.ballerina.asyncapi.core.Constants;
import io.ballerina.asyncapi.core.implementation.v3.doc.ExternalDocMapperV3;
import io.ballerina.asyncapi.core.implementation.v3.tag.TagMapperV3;
import io.ballerina.asyncapi.core.model.message.AsyncApiMessageExample;
import io.ballerina.asyncapi.core.model.tag.AsyncApiTag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps Apicurio Message models to {@link io.ballerina.asyncapi.core.model.message.AsyncApiMessage}
 * and related types for AsyncAPI 3.0.
 */
public final class MessageMapperV3 {

    private static final Logger LOG = LogManager.getLogger(MessageMapperV3.class);

    private MessageMapperV3() {

    }

    /**
     * Extracts messages from an AsyncAPI 3.0 channel, resolving any {@code $ref} entries.
     *
     * @param channel    the AsyncAPI 3.0 channel object
     * @param components the AsyncAPI components (for $ref resolution)
     * @return a map of message names to {@link io.ballerina.asyncapi.core.model.message.AsyncApiMessage},
     *         or null if no messages found
     */
    public static Map<String, io.ballerina.asyncapi.core.model.message.AsyncApiMessage> extractMessages(
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
            if (message instanceof AsyncApiReferenceable referenceable) {
                String $ref = referenceable.get$ref();
                if ($ref != null) {
                    if (!$ref.startsWith(Constants.MESSAGES_REF_PREFIX)) {
                        LOG.warn("Unsupported message $ref format: {}. Skipping message.", $ref);
                        continue;
                    }
                    String refName = $ref.substring(Constants.MESSAGES_REF_PREFIX.length());
                    Map<String, ? extends AsyncApiMessage> refMessages =
                            components != null ? components.getMessages() : null;
                    AsyncApiMessage refMessage = refMessages != null ? refMessages.get(refName) : null;
                    AsyncApi30Message resolved =
                            refMessage instanceof AsyncApi30Message typedMsg ? typedMsg : null;
                    if (resolved == null) {
                        LOG.warn("Could not resolve message $ref: {} in channel message '{}'. Skipping message.",
                                $ref, messageName);
                        continue;
                    }
                    if (resolved instanceof AsyncApiReferenceable resolvedRef && resolvedRef.get$ref() != null) {
                        LOG.warn("Resolved message $ref points to another $ref: {}. Skipping message '{}'.",
                                resolvedRef.get$ref(), messageName);
                        continue;
                    }
                    message = resolved;
                }
            }
            io.ballerina.asyncapi.core.model.message.AsyncApiMessage mappedMessage = map(message);
            if (mappedMessage != null) {
                result.put(messageName, mappedMessage);
            }
        }
        return result.isEmpty() ? null : result;
    }

    /**
     * Maps an Apicurio {@link AsyncApiMessage} to a model
     * {@link io.ballerina.asyncapi.core.model.message.AsyncApiMessage}.
     *
     * @param message the Apicurio message object
     * @return the mapped AsyncApiMessage, or null if message is null
     */
    public static io.ballerina.asyncapi.core.model.message.AsyncApiMessage map(AsyncApiMessage message) {
        if (message == null) {
            return null;
        }

        Object headers = null;
        Object payload = null;
        List<AsyncApiMessageExample> examples = null;
        Map<String, JsonNode> extensions = null;

        if (message instanceof AsyncApi30Message typedMessage) {
            headers = typedMessage.getHeaders();
            payload = typedMessage.getPayload();
            extensions = typedMessage.getExtensions();
            examples = MessageExampleMapperV3.map(typedMessage);
        }

        List<AsyncApiTag> tags = null;
        List<? extends Tag> apicurioTags = message.getTags();
        if (apicurioTags != null) {
            tags = apicurioTags.stream()
                    .map(tag -> TagMapperV3.map(tag, null))
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
                ExternalDocMapperV3.map(message.getExternalDocs(), null),
                MessageBindingsMapperV3.map(message.getBindings()),
                examples,
                traits,
                extensions
        );
    }

}
