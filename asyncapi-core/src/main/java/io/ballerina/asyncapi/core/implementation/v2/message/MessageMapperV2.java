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
import io.apicurio.datamodels.models.asyncapi.AsyncApiMessageTrait;
import io.apicurio.datamodels.models.asyncapi.AsyncApiOperation;
import io.apicurio.datamodels.models.asyncapi.AsyncApiReferenceable;
import io.apicurio.datamodels.models.asyncapi.AsyncApiSchema;
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
import io.ballerina.asyncapi.core.implementation.common.SchemaMapper;
import io.ballerina.asyncapi.core.implementation.v2.doc.ExternalDocMapperV2;
import io.ballerina.asyncapi.core.implementation.v2.tag.TagMapperV2;
import io.ballerina.asyncapi.core.model.tag.AsyncApiTag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Maps Apicurio {@link AsyncApiMessage} to model
 * {@link io.ballerina.asyncapi.core.model.message.AsyncApiMessage} for AsyncAPI 2.x.
 */
public final class MessageMapperV2 {

    private static final Logger LOG = LogManager.getLogger(MessageMapperV2.class);

    private MessageMapperV2() {
    }

    /**
     * Extracts all messages from a channel item's publish and subscribe operations.
     *
     * @param channelItem the Apicurio channel item object
     * @param components  the AsyncAPI components (for $ref resolution)
     * @return a map of message names to model messages, or null if no messages found
     */
    public static Map<String, io.ballerina.asyncapi.core.model.message.AsyncApiMessage> map(
            AsyncApiChannelItem channelItem, AsyncApiComponents components) {
        Map<String, io.ballerina.asyncapi.core.model.message.AsyncApiMessage> messages = new HashMap<>();

        for (AsyncApiOperation operation : new AsyncApiOperation[]{
                channelItem.getPublish(), channelItem.getSubscribe()}) {
            if (operation == null) {
                continue;
            }

            AsyncApiMessage message = switch (operation) {
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

            String messageRefKey = null;
            if (message instanceof AsyncApiReferenceable referenceable && referenceable.get$ref() != null) {
                String ref = referenceable.get$ref();
                messageRefKey = ref.substring(ref.lastIndexOf('/') + 1);
                message = resolveMessageRef(ref, components);
                if (message == null) {
                    continue;
                }
            }

            List<? extends AsyncApiMessage> oneOf = switch (message) {
                case AsyncApi26Message typed -> typed.getOneOf();
                case AsyncApi25Message typed -> typed.getOneOf();
                case AsyncApi24Message typed -> typed.getOneOf();
                case AsyncApi23Message typed -> typed.getOneOf();
                case AsyncApi22Message typed -> typed.getOneOf();
                case AsyncApi21Message typed -> typed.getOneOf();
                case AsyncApi20Message typed -> typed.getOneOf();
                default -> null;
            };

            if (oneOf != null && !oneOf.isEmpty()) {
                for (AsyncApiMessage oneOfMsg : oneOf) {
                    AsyncApiMessage resolvedMsg = oneOfMsg;
                    String oneOfRefKey = null;
                    if (oneOfMsg instanceof AsyncApiReferenceable ref && ref.get$ref() != null) {
                        String refStr = ref.get$ref();
                        oneOfRefKey = refStr.substring(refStr.lastIndexOf('/') + 1);
                        resolvedMsg = resolveMessageRef(refStr, components);
                        if (resolvedMsg == null) {
                            continue;
                        }
                    }
                    String key = oneOfRefKey != null ? oneOfRefKey : resolveMessageKey(resolvedMsg, messages.size());
                    io.ballerina.asyncapi.core.model.message.AsyncApiMessage mappedMessage =
                            mapMessageItem(resolvedMsg, components);
                    if (mappedMessage != null) {
                        messages.put(key, mappedMessage);
                    }
                }
            } else {
                String key = messageRefKey != null ? messageRefKey : resolveMessageKey(message, messages.size());
                io.ballerina.asyncapi.core.model.message.AsyncApiMessage mappedMessage =
                        mapMessageItem(message, components);
                if (mappedMessage != null) {
                    messages.put(key, mappedMessage);
                }
            }
        }

        return messages.isEmpty() ? null : messages;
    }

    /**
     * Maps a single Apicurio {@link AsyncApiMessage} to a model message.
     * If the message is a {@code $ref}, it is resolved before mapping.
     *
     * @param message    the Apicurio message object
     * @param components the AsyncAPI components (for $ref resolution)
     * @return the mapped model message, or null if message is null or unresolvable
     */
    public static io.ballerina.asyncapi.core.model.message.AsyncApiMessage mapMessageItem(
                    AsyncApiMessage message, AsyncApiComponents components) {
        if (message == null) {
            return null;
        }

        if (message instanceof AsyncApiReferenceable referenceable && referenceable.get$ref() != null) {
            AsyncApiMessage resolved = resolveMessageRef(referenceable.get$ref(), components);
            if (resolved == null) {
                return null;
            }
            return mapMessageItem(resolved, components);
        }

        Object headers = null;
        Object payload = null;
        Map<String, JsonNode> extensions = null;

        switch (message) {
            case AsyncApi26Message typed -> {
                if (typed.getHeaders() instanceof AsyncApiSchema typedSchema) {
                    headers = SchemaMapper.map(typedSchema);
                }
                payload = MessagePayloadMapperV2.map(typed.getPayload(), components);
                extensions = typed.getExtensions();
            }
            case AsyncApi25Message typed -> {
                if (typed.getHeaders() instanceof AsyncApiSchema typedSchema) {
                    headers = SchemaMapper.map(typedSchema);
                }
                payload = MessagePayloadMapperV2.map(typed.getPayload(), components);
                extensions = typed.getExtensions();
            }
            case AsyncApi24Message typed -> {
                if (typed.getHeaders() instanceof AsyncApiSchema typedSchema) {
                    headers = SchemaMapper.map(typedSchema);
                }
                payload = MessagePayloadMapperV2.map(typed.getPayload(), components);
                extensions = typed.getExtensions();
            }
            case AsyncApi23Message typed -> {
                if (typed.getHeaders() instanceof AsyncApiSchema typedSchema) {
                    headers = SchemaMapper.map(typedSchema);
                }
                payload = MessagePayloadMapperV2.map(typed.getPayload(), components);
                extensions = typed.getExtensions();
            }
            case AsyncApi22Message typed -> {
                if (typed.getHeaders() instanceof AsyncApiSchema typedSchema) {
                    headers = SchemaMapper.map(typedSchema);
                }
                payload = MessagePayloadMapperV2.map(typed.getPayload(), components);
                extensions = typed.getExtensions();
            }
            case AsyncApi21Message typed -> {
                if (typed.getHeaders() instanceof AsyncApiSchema typedSchema) {
                    headers = SchemaMapper.map(typedSchema);
                }
                payload = MessagePayloadMapperV2.map(typed.getPayload(), components);
                extensions = typed.getExtensions();
            }
            case AsyncApi20Message typed -> {
                if (typed.getHeaders() instanceof AsyncApiSchema typedSchema) {
                    headers = SchemaMapper.map(typedSchema);
                }
                payload = MessagePayloadMapperV2.map(typed.getPayload(), components);
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
                    .map(t -> MessageTraitMapperV2.map(t, components))
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
     * Resolves a message {@code $ref} string, following any chain of refs, to the
     * final concrete {@link AsyncApiMessage}. Detects cyclic references.
     *
     * @param ref        the initial $ref string (e.g., "#/components/messages/MyMessage")
     * @param components the AsyncAPI components used for lookup
     * @return the concrete message, or {@code null} if resolution fails
     */
    private static AsyncApiMessage resolveMessageRef(String ref, AsyncApiComponents components) {
        if (components == null) {
            LOG.warn("Cannot resolve $ref: {}. Components is null.", ref);
            return null;
        }
        Set<String> visited = new HashSet<>();
        String current = ref;
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

    /**
     * Determines the map key for a message, falling back to title or a generated index key.
     *
     * @param message     the message to derive a key from
     * @param currentSize the current size of the messages map (used for fallback key generation)
     * @return a non-null, non-blank key string
     */
    private static String resolveMessageKey(AsyncApiMessage message, int currentSize) {
        String key = message.getName();
        if (key == null || key.isBlank()) {
            key = message.getTitle();
            if (key == null || key.isBlank()) {
                key = String.valueOf(currentSize);
            }
        }
        return key;
    }
}
