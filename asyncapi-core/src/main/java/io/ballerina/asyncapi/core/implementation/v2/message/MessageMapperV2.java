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
package io.ballerina.asyncapi.core.implementation.v2.message;

import com.fasterxml.jackson.databind.JsonNode;
import io.apicurio.datamodels.models.Tag;
import io.apicurio.datamodels.models.asyncapi.AsyncApiChannelItem;
import io.apicurio.datamodels.models.asyncapi.AsyncApiComponents;
import io.apicurio.datamodels.models.asyncapi.AsyncApiMessage;
import io.apicurio.datamodels.models.asyncapi.AsyncApiOperation;
import io.apicurio.datamodels.models.asyncapi.AsyncApiMessageTrait;
import io.apicurio.datamodels.models.asyncapi.AsyncApiReferenceable;
import io.apicurio.datamodels.models.asyncapi.v20.AsyncApi20Message;
import io.apicurio.datamodels.models.asyncapi.v20.AsyncApi20Operation;
import io.apicurio.datamodels.models.asyncapi.v21.AsyncApi21Message;
import io.apicurio.datamodels.models.asyncapi.v21.AsyncApi21Operation;
import io.apicurio.datamodels.models.asyncapi.v22.AsyncApi22Message;
import io.apicurio.datamodels.models.asyncapi.v22.AsyncApi22Operation;
import io.apicurio.datamodels.models.asyncapi.v23.AsyncApi23Message;
import io.apicurio.datamodels.models.asyncapi.v23.AsyncApi23Operation;
import io.apicurio.datamodels.models.asyncapi.v24.AsyncApi24Message;
import io.apicurio.datamodels.models.asyncapi.v24.AsyncApi24Operation;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25Message;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25Operation;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26Message;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26Operation;
import io.ballerina.asyncapi.core.Constants;
import io.ballerina.asyncapi.core.implementation.v2.doc.ExternalDocMapperV2;
import io.ballerina.asyncapi.core.implementation.v2.tag.TagMapperV2;
import io.ballerina.asyncapi.core.model.tag.AsyncApiTag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps Apicurio {@link AsyncApiMessage} to model
 * {@link io.ballerina.asyncapi.core.model.message.AsyncApiMessage} for AsyncAPI 2.x.
 */
public final class MessageMapperV2 {

    private static final Logger LOG = LogManager.getLogger(MessageMapperV2.class);

    private MessageMapperV2() {

    }

    /**
     * Maps an Apicurio {@link AsyncApiMessage} to a model message.
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
        Map<String, JsonNode> extensions = null;

        switch (message) {
            case AsyncApi26Message typed -> {
                headers = typed.getHeaders();
                payload = typed.getPayload();
                extensions = typed.getExtensions();
            }
            case AsyncApi25Message typed -> {
                headers = typed.getHeaders();
                payload = typed.getPayload();
                extensions = typed.getExtensions();
            }
            case AsyncApi24Message typed -> {
                headers = typed.getHeaders();
                payload = typed.getPayload();
                extensions = typed.getExtensions();
            }
            case AsyncApi23Message typed -> {
                headers = typed.getHeaders();
                payload = typed.getPayload();
                extensions = typed.getExtensions();
            }
            case AsyncApi22Message typed -> {
                headers = typed.getHeaders();
                payload = typed.getPayload();
                extensions = typed.getExtensions();
            }
            case AsyncApi21Message typed -> {
                headers = typed.getHeaders();
                payload = typed.getPayload();
                extensions = typed.getExtensions();
            }
            case AsyncApi20Message typed -> {
                headers = typed.getHeaders();
                payload = typed.getPayload();
                extensions = typed.getExtensions();
            }
            default -> { }
        }

        List<AsyncApiTag> tags = null;
        List<? extends Tag> apicurioTags = message.getTags();
        if (apicurioTags != null) {
            tags = apicurioTags.stream()
                    .map(TagMapperV2::map)
                    .toList();
        }

        List<io.ballerina.asyncapi.core.model.message.AsyncApiMessageTrait> traits = null;
        List<? extends AsyncApiMessageTrait> apicurioTraits = message.getTraits();
        if (apicurioTraits != null) {
            traits = apicurioTraits.stream()
                    .map(MessageTraitMapperV2::map)
                    .toList();
        }

        return new io.ballerina.asyncapi.core.model.message.AsyncApiMessage(
                headers,
                payload,
                CorrelationIdMapperV2.map(message.getCorrelationId()),
                message.getContentType(),
                message.getName(),
                message.getTitle(),
                message.getSummary(),
                message.getDescription(),
                tags,
                ExternalDocMapperV2.map(message.getExternalDocs()),
                MessageBindingsMapperV2.map(message.getBindings()),
                MessageExampleMapperV2.map(message),
                traits,
                extensions
        );
    }

    /**
     * Extracts messages from a channel item's publish and subscribe operations.
     *
     * @param channelItem the Apicurio channel item object
     * @param components  the AsyncAPI components (for $ref resolution)
     * @return a map of message names to AsyncApiMessage objects, or null if no messages found
     */
    public static Map<String, io.ballerina.asyncapi.core.model.message.AsyncApiMessage> extractMessages(
            AsyncApiChannelItem channelItem, AsyncApiComponents components) {
        Map<String, io.ballerina.asyncapi.core.model.message.AsyncApiMessage> messages =
                new HashMap<>();

        for (AsyncApiOperation operation : new AsyncApiOperation[]{
                channelItem.getPublish(), channelItem.getSubscribe()}) {
            if (operation == null) {
                continue;
            }

            io.apicurio.datamodels.models.asyncapi.AsyncApiMessage message = switch (operation) {
                case AsyncApi26Operation typed -> typed.getMessage();
                case AsyncApi25Operation typed -> typed.getMessage();
                case AsyncApi24Operation typed -> typed.getMessage();
                case AsyncApi23Operation typed -> typed.getMessage();
                case AsyncApi22Operation typed -> typed.getMessage();
                case AsyncApi21Operation typed -> typed.getMessage();
                case AsyncApi20Operation typed -> typed.getMessage();
                default -> null;
            };

            if (message == null) {
                continue;
            }

            if (message instanceof AsyncApiReferenceable referenceable) {
                String $ref = referenceable.get$ref();
                if ($ref != null) {
                    if (!$ref.startsWith(Constants.MESSAGES_REF_PREFIX) || components == null) {
                        LOG.warn("Could not resolve message $ref: {}. Skipping message.", $ref);
                        continue;
                    }
                    String refName = $ref.substring(Constants.MESSAGES_REF_PREFIX.length());
                    Map<String, ? extends AsyncApiMessage> refMessages = components.getMessages();
                    io.apicurio.datamodels.models.asyncapi.AsyncApiMessage resolved =
                            refMessages != null ? refMessages.get(refName) : null;
                    if (resolved == null) {
                        LOG.warn("Could not resolve message $ref: {}. Skipping message.", $ref);
                        continue;
                    }
                    if (resolved instanceof AsyncApiReferenceable resolvedRef
                            && resolvedRef.get$ref() != null) {
                        LOG.warn("Resolved message $ref points to another $ref: {}. Skipping message.",
                                resolvedRef.get$ref());
                        continue;
                    }
                    message = resolved;
                }
            }

            io.ballerina.asyncapi.core.model.message.AsyncApiMessage mappedMessage = map(message);
            if (mappedMessage != null) {
                String key = message.getName();
                if (key == null || key.isBlank()) {
                    key = message.getTitle();
                    if (key == null || key.isBlank()) {
                        key = "message_" + messages.size();
                    }
                }
                messages.put(key, mappedMessage);
            }
        }

        return messages.isEmpty() ? null : messages;
    }

}
