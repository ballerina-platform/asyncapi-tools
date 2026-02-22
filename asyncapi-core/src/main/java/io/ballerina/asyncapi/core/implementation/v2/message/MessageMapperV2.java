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
import io.apicurio.datamodels.models.asyncapi.AsyncApiExtensible;
import io.apicurio.datamodels.models.asyncapi.AsyncApiMessage;
import io.apicurio.datamodels.models.asyncapi.AsyncApiMessageBindings;
import io.apicurio.datamodels.models.asyncapi.AsyncApiMessageTrait;
import io.apicurio.datamodels.models.asyncapi.AsyncApiReferenceable;
import io.apicurio.datamodels.models.asyncapi.v20.AsyncApi20Message;
import io.apicurio.datamodels.models.asyncapi.v20.AsyncApi20MessageTrait;
import io.apicurio.datamodels.models.asyncapi.v20.AsyncApi20Operation;
import io.apicurio.datamodels.models.asyncapi.v21.AsyncApi21Message;
import io.apicurio.datamodels.models.asyncapi.v21.AsyncApi21MessageTrait;
import io.apicurio.datamodels.models.asyncapi.v21.AsyncApi21Operation;
import io.apicurio.datamodels.models.asyncapi.v22.AsyncApi22Message;
import io.apicurio.datamodels.models.asyncapi.v22.AsyncApi22MessageExample;
import io.apicurio.datamodels.models.asyncapi.v22.AsyncApi22MessageTrait;
import io.apicurio.datamodels.models.asyncapi.v22.AsyncApi22Operation;
import io.apicurio.datamodels.models.asyncapi.v23.AsyncApi23Message;
import io.apicurio.datamodels.models.asyncapi.v23.AsyncApi23MessageExample;
import io.apicurio.datamodels.models.asyncapi.v23.AsyncApi23MessageTrait;
import io.apicurio.datamodels.models.asyncapi.v23.AsyncApi23Operation;
import io.apicurio.datamodels.models.asyncapi.v24.AsyncApi24Message;
import io.apicurio.datamodels.models.asyncapi.v24.AsyncApi24MessageExample;
import io.apicurio.datamodels.models.asyncapi.v24.AsyncApi24MessageTrait;
import io.apicurio.datamodels.models.asyncapi.v24.AsyncApi24Operation;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25Message;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25MessageExample;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25MessageTrait;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25Operation;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26Message;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26MessageExample;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26MessageTrait;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26Operation;
import io.ballerina.asyncapi.core.implementation.v2.message.CorrelationIdMapperV2;
import io.ballerina.asyncapi.core.implementation.v2.doc.ExternalDocMapperV2;
import io.ballerina.asyncapi.core.implementation.v2.tag.TagMapperV2;
import io.ballerina.asyncapi.core.model.message.HttpMessageBindings;
import io.ballerina.asyncapi.core.model.message.WsMessageBindings;
import io.ballerina.asyncapi.core.model.tag.AsyncApiTag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
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
    public static io.ballerina.asyncapi.core.model.message.AsyncApiMessage
            map(AsyncApiMessage message) {
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
                mapBindings(message.getBindings()),
                mapMessageExample(message),
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
    public static Map<String, io.ballerina.asyncapi.core.model.message.AsyncApiMessage>
            extractMessages(AsyncApiChannelItem channelItem, AsyncApiComponents components) {
        Map<String, io.ballerina.asyncapi.core.model.message.AsyncApiMessage> messages =
                new LinkedHashMap<>();

        if (channelItem.getPublish() != null) {
            extractMessagesFromOperation(channelItem.getPublish(), messages, components);
        }

        if (channelItem.getSubscribe() != null) {
            extractMessagesFromOperation(channelItem.getSubscribe(), messages, components);
        }

        return messages.isEmpty() ? null : messages;
    }

    /**
     * Extracts messages from a single operation and adds them to the messages map.
     *
     * @param operation  the Apicurio operation object (publish or subscribe)
     * @param messages   the map to populate with extracted messages
     * @param components the AsyncAPI components (for $ref resolution)
     */
    private static void extractMessagesFromOperation(
            io.apicurio.datamodels.models.asyncapi.AsyncApiOperation operation,
            Map<String, io.ballerina.asyncapi.core.model.message.AsyncApiMessage> messages,
            AsyncApiComponents components) {
        if (operation == null) {
            return;
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

        if (message != null) {
            if (message instanceof AsyncApiReferenceable referenceable) {
                String $ref = referenceable.get$ref();
                if ($ref != null) {
                    io.apicurio.datamodels.models.asyncapi.AsyncApiMessage resolved =
                            MessageRefResolverV2.resolveMessageRef($ref, components);
                    if (resolved == null) {
                        LOG.warn("Could not resolve message $ref: {}. Skipping message.", $ref);
                        return;
                    }

                    if (resolved instanceof AsyncApiReferenceable resolvedRef
                            && resolvedRef.get$ref() != null) {
                        LOG.warn("Resolved message $ref points to another $ref: {}. Skipping message.",
                                resolvedRef.get$ref());
                        return;
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
    }

    /**
     * Maps an Apicurio {@link AsyncApiMessageBindings} to a model message bindings.
     *
     * @param bindings the Apicurio message bindings object
     * @return the mapped AsyncApiMessageBindings, or null if empty
     */
    public static io.ballerina.asyncapi.core.model.message.AsyncApiMessageBindings
            mapBindings(AsyncApiMessageBindings bindings) {
        if (bindings == null) {
            return null;
        }
        HttpMessageBindings http = mapHttpMessageBinding(bindings.getHttp());
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
     * Maps an Apicurio HTTP message binding to {@link HttpMessageBindings}.
     *
     * @param binding the Apicurio HTTP binding object
     * @return the mapped HttpMessageBindings, or null if binding is null
     */
    private static HttpMessageBindings mapHttpMessageBinding(
            io.apicurio.datamodels.models.asyncapi.AsyncApiBinding binding) {
        if (binding == null) {
            return null;
        }
        Integer statusCode = getBindingItemAsInteger(binding, "statusCode");
        String bindingVersion = getBindingItemAsText(binding, "bindingVersion");
        return new HttpMessageBindings(null, statusCode, bindingVersion);
    }

    /**
     * Gets a binding item as text.
     *
     * @param binding the binding object
     * @param key     the item key
     * @return the text value, or null if not found or not textual
     */
    private static String getBindingItemAsText(
            io.apicurio.datamodels.models.asyncapi.AsyncApiBinding binding, String key) {
        JsonNode node = binding.getItem(key);
        if (node == null) {
            return null;
        }
        return node.isTextual() ? node.asText() : node.toString();
    }

    /**
     * Gets a binding item as an integer.
     *
     * @param binding the binding object
     * @param key     the item key
     * @return the integer value, or null if not found or not numeric
     */
    private static Integer getBindingItemAsInteger(
            io.apicurio.datamodels.models.asyncapi.AsyncApiBinding binding, String key) {
        JsonNode node = binding.getItem(key);
        if (node == null || !node.isInt()) {
            return null;
        }
        return node.asInt();
    }

    /**
     * Maps message examples from AsyncAPI 2.x message.
     * v2.2-v2.6: Maps structured example object with name, summary, headers, payload, extensions.
     * v2.0/v2.1: Maps Map format where MIME types become example names and values become payloads.
     *
     * @param message the Apicurio message object
     * @return a list of mapped examples, or null if not available
     */
    private static List<io.ballerina.asyncapi.core.model.message.AsyncApiMessageExample>
            mapMessageExample(AsyncApiMessage message) {
        if (message == null) {
            return null;
        }

        return switch (message) {
            case AsyncApi26Message typed -> {
                AsyncApi26MessageExample ex = typed.getExamples();
                yield ex != null ? List.of(buildMessageExample(
                        ex.getName(), ex.getSummary(), ex.getHeaders(),
                        ex.getPayload(), ex.getExtensions()
                )) : null;
            }
            case AsyncApi25Message typed -> {
                AsyncApi25MessageExample ex = typed.getExamples();
                yield ex != null ? List.of(buildMessageExample(
                        ex.getName(), ex.getSummary(), ex.getHeaders(),
                        ex.getPayload(), ex.getExtensions()
                )) : null;
            }
            case AsyncApi24Message typed -> {
                AsyncApi24MessageExample ex = typed.getExamples();
                yield ex != null ? List.of(buildMessageExample(
                        ex.getName(), ex.getSummary(), ex.getHeaders(),
                        ex.getPayload(), ex.getExtensions()
                )) : null;
            }
            case AsyncApi23Message typed -> {
                AsyncApi23MessageExample ex = typed.getExamples();
                yield ex != null ? List.of(buildMessageExample(
                        ex.getName(), ex.getSummary(), ex.getHeaders(),
                        ex.getPayload(), ex.getExtensions()
                )) : null;
            }
            case AsyncApi22Message typed -> {
                AsyncApi22MessageExample ex = typed.getExamples();
                yield ex != null ? List.of(buildMessageExample(
                        ex.getName(), ex.getSummary(), ex.getHeaders(),
                        ex.getPayload(), ex.getExtensions()
                )) : null;
            }
            case AsyncApi21Message typed -> mapExamplesFromMap(typed.getExamples());
            case AsyncApi20Message typed -> mapExamplesFromMap(typed.getExamples());
            default -> null;
        };
    }

    /**
     * Builds an AsyncApiMessageExample from the structured example fields.
     *
     * @param name       the example name
     * @param summary    the example summary
     * @param headers    the example headers map
     * @param payload    the example payload
     * @param extensions the extensions map
     * @return the mapped AsyncApiMessageExample
     */
    private static io.ballerina.asyncapi.core.model.message.AsyncApiMessageExample buildMessageExample(
            String name,
            String summary,
            Map<String, JsonNode> headers,
            JsonNode payload,
            Map<String, JsonNode> extensions) {
        Map<String, Object> headersMap = null;
        if (headers != null && !headers.isEmpty()) {
            headersMap = new LinkedHashMap<>();
            for (Map.Entry<String, JsonNode> entry : headers.entrySet()) {
                headersMap.put(entry.getKey(), entry.getValue());
            }
        }

        return new io.ballerina.asyncapi.core.model.message.AsyncApiMessageExample(
                headersMap,
                payload,
                name,
                summary,
                extensions
        );
    }

    /**
     * Maps examples from Map&lt;String, JsonNode&gt; format (used in v2.0/v2.1 Messages and all MessageTraits).
     * Each map entry becomes an example where the MIME type is used as the name and the JsonNode is the payload.
     *
     * @param examplesMap the map of MIME type to example payload
     * @return a list of mapped examples, or null if map is null or empty
     */
    static List<io.ballerina.asyncapi.core.model.message.AsyncApiMessageExample>
            mapExamplesFromMap(Map<String, JsonNode> examplesMap) {
        if (examplesMap == null || examplesMap.isEmpty()) {
            return null;
        }

        List<io.ballerina.asyncapi.core.model.message.AsyncApiMessageExample> examples =
                new ArrayList<>();

        for (Map.Entry<String, JsonNode> entry : examplesMap.entrySet()) {
            examples.add(new io.ballerina.asyncapi.core.model.message.AsyncApiMessageExample(
                    null,
                    entry.getValue(),
                    entry.getKey(),
                    null,
                    null
            ));
        }

        return examples.isEmpty() ? null : examples;
    }

    /**
     * Maps message trait examples from AsyncAPI 2.x message trait.
     * All v2.x MessageTraits use Map&lt;String, JsonNode&gt; format where MIME types are keys.
     *
     * @param trait the Apicurio message trait object
     * @return a list of mapped examples, or null if not available
     */
    static List<io.ballerina.asyncapi.core.model.message.AsyncApiMessageExample>
            mapMessageTraitExample(AsyncApiMessageTrait trait) {
        if (trait == null) {
            return null;
        }

        Map<String, JsonNode> examplesMap = switch (trait) {
            case AsyncApi26MessageTrait typed -> typed.getExamples();
            case AsyncApi25MessageTrait typed -> typed.getExamples();
            case AsyncApi24MessageTrait typed -> typed.getExamples();
            case AsyncApi23MessageTrait typed -> typed.getExamples();
            case AsyncApi22MessageTrait typed -> typed.getExamples();
            case AsyncApi21MessageTrait typed -> typed.getExamples();
            case AsyncApi20MessageTrait typed -> typed.getExamples();
            default -> null;
        };

        return mapExamplesFromMap(examplesMap);
    }
}
