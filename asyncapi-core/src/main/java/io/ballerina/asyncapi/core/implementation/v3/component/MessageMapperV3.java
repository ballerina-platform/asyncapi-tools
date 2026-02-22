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
package io.ballerina.asyncapi.core.implementation.v3.component;

import com.fasterxml.jackson.databind.JsonNode;
import io.apicurio.datamodels.models.Tag;
import io.apicurio.datamodels.models.asyncapi.AsyncApiComponents;
import io.apicurio.datamodels.models.asyncapi.AsyncApiCorrelationID;
import io.apicurio.datamodels.models.asyncapi.AsyncApiExtensible;
import io.apicurio.datamodels.models.asyncapi.AsyncApiMessage;
import io.apicurio.datamodels.models.asyncapi.AsyncApiMessageBindings;
import io.apicurio.datamodels.models.asyncapi.AsyncApiMessageTrait;
import io.apicurio.datamodels.models.asyncapi.AsyncApiReferenceable;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Channel;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30CorrelationID;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Message;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30MessageExample;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30MessageTrait;
import io.ballerina.asyncapi.core.Constants;
import io.ballerina.asyncapi.core.implementation.v3.doc.ExternalDocMapperV3;
import io.ballerina.asyncapi.core.implementation.v3.tag.TagMapperV3;
import io.ballerina.asyncapi.core.model.message.AsyncApiCorrelationId;
import io.ballerina.asyncapi.core.model.message.AsyncApiMessageExample;
import io.ballerina.asyncapi.core.model.message.HttpMessageBindings;
import io.ballerina.asyncapi.core.model.message.WsMessageBindings;
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
    public static Map<String, io.ballerina.asyncapi.core.model.message.AsyncApiMessage>
            extractMessages(AsyncApi30Channel channel, AsyncApiComponents components) {
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
                    AsyncApi30Message resolved = resolveMessageRef($ref, components);
                    if (resolved == null) {
                        LOG.warn("Could not resolve message $ref: {} in channel message '{}'. Skipping message.",
                                $ref, messageName);
                        continue;
                    }
                    if (resolved instanceof AsyncApiReferenceable resolvedRef
                            && resolvedRef.get$ref() != null) {
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
     * Resolves a {@code $ref} to a message in components.
     *
     * @param $ref       the $ref string (e.g., {@code #/components/messages/MyMessage})
     * @param components the AsyncAPI components object
     * @return the resolved message, or null if not found or invalid
     */
    private static AsyncApi30Message resolveMessageRef(String $ref, AsyncApiComponents components) {
        if (!$ref.startsWith(Constants.MESSAGES_REF_PREFIX)) {
            LOG.warn("Unsupported message $ref format: {}. Skipping message.", $ref);
            return null;
        }
        String name = $ref.substring(Constants.MESSAGES_REF_PREFIX.length());
        if (components == null) {
            return null;
        }
        Map<String, ? extends AsyncApiMessage> messagesMap = components.getMessages();
        if (messagesMap == null) {
            return null;
        }
        AsyncApiMessage message = messagesMap.get(name);
        return message instanceof AsyncApi30Message typedMessage ? typedMessage : null;
    }

    /**
     * Maps an Apicurio {@link AsyncApiMessage} to a model
     * {@link io.ballerina.asyncapi.core.model.message.AsyncApiMessage}.
     *
     * @param message the Apicurio message object
     * @return the mapped AsyncApiMessage, or null if message is null
     */
    public static io.ballerina.asyncapi.core.model.message.AsyncApiMessage
            map(AsyncApiMessage message) {
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
            List<AsyncApi30MessageExample> apicurioExamples =
                    typedMessage.getExamples();
            if (apicurioExamples != null) {
                examples = apicurioExamples.stream()
                        .map(MessageMapperV3::mapMessageExample)
                        .toList();
            }
        }

        List<AsyncApiTag> tags = null;
        List<? extends Tag> apicurioTags = message.getTags();
        if (apicurioTags != null) {
            tags = apicurioTags.stream()
                    .map(tag -> TagMapperV3.map(tag, null))
                    .toList();
        }

        List<io.ballerina.asyncapi.core.model.message.AsyncApiMessageTrait>
                traits = null;
        List<? extends AsyncApiMessageTrait> apicurioTraits =
                message.getTraits();
        if (apicurioTraits != null) {
            traits = apicurioTraits.stream()
                    .map(MessageMapperV3::mapTrait)
                    .toList();
        }

        return new io.ballerina.asyncapi.core.model.message.AsyncApiMessage(
                headers,
                payload,
                mapCorrelationId(message.getCorrelationId()),
                message.getContentType(),
                message.getName(),
                message.getTitle(),
                message.getSummary(),
                message.getDescription(),
                tags,
                ExternalDocMapperV3.map(message.getExternalDocs(), null),
                mapBindings(message.getBindings()),
                examples,
                traits,
                extensions
        );
    }

    /**
     * Maps an Apicurio {@link AsyncApiMessageTrait} to a model
     * {@link io.ballerina.asyncapi.core.model.message.AsyncApiMessageTrait}.
     *
     * @param trait the Apicurio message trait object
     * @return the mapped AsyncApiMessageTrait, or null if trait is null
     */
    public static io.ballerina.asyncapi.core.model.message.AsyncApiMessageTrait
            mapTrait(AsyncApiMessageTrait trait) {
        if (trait == null) {
            return null;
        }

        Object headers = null;
        List<AsyncApiMessageExample> examples = null;
        Map<String, JsonNode> extensions = null;

        if (trait instanceof AsyncApi30MessageTrait typedTrait) {
            headers = typedTrait.getHeaders();
            extensions = typedTrait.getExtensions();
            List<AsyncApi30MessageExample> apicurioExamples =
                    typedTrait.getExamples();
            if (apicurioExamples != null) {
                examples = apicurioExamples.stream()
                        .map(MessageMapperV3::mapMessageExample)
                        .toList();
            }
        }

        List<AsyncApiTag> tags = null;
        List<? extends Tag> apicurioTags = trait.getTags();
        if (apicurioTags != null) {
            tags = apicurioTags.stream()
                    .map(tag -> TagMapperV3.map(tag, null))
                    .toList();
        }

        return new io.ballerina.asyncapi.core.model.message.AsyncApiMessageTrait(
                trait.getName(),
                trait.getTitle(),
                trait.getSummary(),
                trait.getDescription(),
                trait.getContentType(),
                headers,
                mapCorrelationId(trait.getCorrelationId()),
                tags,
                ExternalDocMapperV3.map(trait.getExternalDocs(), null),
                mapBindings(trait.getBindings()),
                examples,
                extensions
        );
    }

    /**
     * Maps an Apicurio {@link AsyncApiMessageBindings} to a model
     * {@link io.ballerina.asyncapi.core.model.message.AsyncApiMessageBindings}.
     *
     * @param bindings the Apicurio message bindings object
     * @return the mapped AsyncApiMessageBindings, or null if empty
     */
    static io.ballerina.asyncapi.core.model.message.AsyncApiMessageBindings
            mapBindings(AsyncApiMessageBindings bindings) {
        if (bindings == null) {
            return null;
        }
        HttpMessageBindings http = HttpMessageBindingMapperV3.map(bindings.getHttp());
        WsMessageBindings ws = bindings.getWs() != null
                ? new WsMessageBindings() : null;
        Map<String, JsonNode> extensions = null;
        if (bindings instanceof AsyncApiExtensible extensible) {
            extensions = extensible.getExtensions();
        }
        if (http == null && ws == null && extensions == null) {
            return null;
        }
        return new io.ballerina.asyncapi.core.model.message.AsyncApiMessageBindings(
                http, ws, extensions);
    }

    /**
     * Maps an Apicurio {@link AsyncApiCorrelationID} to a model
     * {@link AsyncApiCorrelationId}.
     *
     * @param correlationId the Apicurio correlation ID object
     * @return the mapped AsyncApiCorrelationId, or null if input is null
     */
    static AsyncApiCorrelationId mapCorrelationId(
            AsyncApiCorrelationID correlationId) {
        if (correlationId == null) {
            return null;
        }
        Map<String, JsonNode> extensions = null;
        if (correlationId instanceof AsyncApi30CorrelationID typed) {
            extensions = typed.getExtensions();
        }
        return new AsyncApiCorrelationId(
                correlationId.getDescription(),
                correlationId.getLocation(),
                extensions
        );
    }

    /**
     * Maps an Apicurio {@link AsyncApi30MessageExample} to a model
     * {@link AsyncApiMessageExample}.
     *
     * @param example the Apicurio message example object
     * @return the mapped AsyncApiMessageExample, or null if input is null
     */
    private static AsyncApiMessageExample mapMessageExample(
            AsyncApi30MessageExample example) {
        if (example == null) {
            return null;
        }
        Map<String, Object> headers = null;
        Map<String, JsonNode> apicurioHeaders = example.getHeaders();
        if (apicurioHeaders != null) {
            headers = new LinkedHashMap<>(apicurioHeaders);
        }
        return new AsyncApiMessageExample(
                headers,
                example.getPayload(),
                example.getName(),
                example.getSummary(),
                example.getExtensions()
        );
    }
}
