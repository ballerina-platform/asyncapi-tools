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
package io.ballerina.asyncapi.core.implementation.v2.component;

import com.fasterxml.jackson.databind.JsonNode;
import io.apicurio.datamodels.models.Tag;
import io.apicurio.datamodels.models.asyncapi.AsyncApiMessage;
import io.apicurio.datamodels.models.asyncapi.AsyncApiMessageBindings;
import io.apicurio.datamodels.models.asyncapi.AsyncApiMessageTrait;
import io.apicurio.datamodels.models.asyncapi.v20.AsyncApi20Message;
import io.apicurio.datamodels.models.asyncapi.v21.AsyncApi21Message;
import io.apicurio.datamodels.models.asyncapi.v22.AsyncApi22Message;
import io.apicurio.datamodels.models.asyncapi.v23.AsyncApi23Message;
import io.apicurio.datamodels.models.asyncapi.v24.AsyncApi24Message;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25Message;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26Message;
import io.apicurio.datamodels.models.asyncapi.v22.AsyncApi22MessageExample;
import io.apicurio.datamodels.models.asyncapi.v23.AsyncApi23MessageExample;
import io.apicurio.datamodels.models.asyncapi.v24.AsyncApi24MessageExample;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25MessageExample;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26MessageExample;
import io.apicurio.datamodels.models.asyncapi.v20.AsyncApi20MessageTrait;
import io.apicurio.datamodels.models.asyncapi.v21.AsyncApi21MessageTrait;
import io.apicurio.datamodels.models.asyncapi.v22.AsyncApi22MessageTrait;
import io.apicurio.datamodels.models.asyncapi.v23.AsyncApi23MessageTrait;
import io.apicurio.datamodels.models.asyncapi.v24.AsyncApi24MessageTrait;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25MessageTrait;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26MessageTrait;
import io.ballerina.asyncapi.core.implementation.v2.doc.ExternalDocMapperV2;
import io.ballerina.asyncapi.core.implementation.v2.tag.TagMapperV2;
import io.ballerina.asyncapi.core.model.message.HttpMessageBindings;
import io.ballerina.asyncapi.core.model.message.WsMessageBindings;
import io.ballerina.asyncapi.core.model.tag.AsyncApiTag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps Apicurio {@link AsyncApiMessage} to model
 * {@link io.ballerina.asyncapi.core.model.message.AsyncApiMessage} for AsyncAPI 2.x.
 */
public final class MessageMapperV2 {

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
     * Maps an Apicurio {@link AsyncApiMessageBindings} to a model message bindings.
     *
     * @param bindings the Apicurio message bindings object
     * @return the mapped AsyncApiMessageBindings, or null if empty
     */
    static io.ballerina.asyncapi.core.model.message.AsyncApiMessageBindings
            mapBindings(AsyncApiMessageBindings bindings) {
        if (bindings == null) {
            return null;
        }
        HttpMessageBindings http = mapHttpMessageBinding(bindings.getHttp());
        WsMessageBindings ws = bindings.getWs() != null
                ? new WsMessageBindings() : null;
        if (http == null && ws == null) {
            return null;
        }
        return new io.ballerina.asyncapi.core.model.message.AsyncApiMessageBindings(
                http, ws);
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
        Object headers = binding.getItem("headers");
        Integer statusCode = getBindingItemAsInteger(binding, "statusCode");
        String bindingVersion = getBindingItemAsText(binding, "bindingVersion");
        return new HttpMessageBindings(headers, statusCode, bindingVersion);
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

        // v2.2-v2.6: getExamples() returns a single structured example object
        // v2.0/v2.1: getExamples() returns Map<String, JsonNode> (map MIME types to example names)
        return switch (message) {
            case AsyncApi26Message typed -> {
                AsyncApi26MessageExample ex = typed.getExamples();
                yield ex != null ? List.of(buildMessageExample(
                        ex.getName(),
                        ex.getSummary(),
                        ex.getHeaders(),
                        ex.getPayload(),
                        ex.getExtensions()
                )) : null;
            }
            case AsyncApi25Message typed -> {
                AsyncApi25MessageExample ex = typed.getExamples();
                yield ex != null ? List.of(buildMessageExample(
                        ex.getName(),
                        ex.getSummary(),
                        ex.getHeaders(),
                        ex.getPayload(),
                        ex.getExtensions()
                )) : null;
            }
            case AsyncApi24Message typed -> {
                AsyncApi24MessageExample ex = typed.getExamples();
                yield ex != null ? List.of(buildMessageExample(
                        ex.getName(),
                        ex.getSummary(),
                        ex.getHeaders(),
                        ex.getPayload(),
                        ex.getExtensions()
                )) : null;
            }
            case AsyncApi23Message typed -> {
                AsyncApi23MessageExample ex = typed.getExamples();
                yield ex != null ? List.of(buildMessageExample(
                        ex.getName(),
                        ex.getSummary(),
                        ex.getHeaders(),
                        ex.getPayload(),
                        ex.getExtensions()
                )) : null;
            }
            case AsyncApi22Message typed -> {
                AsyncApi22MessageExample ex = typed.getExamples();
                yield ex != null ? List.of(buildMessageExample(
                        ex.getName(),
                        ex.getSummary(),
                        ex.getHeaders(),
                        ex.getPayload(),
                        ex.getExtensions()
                )) : null;
            }
            // v2.0 and v2.1 have Map<String, JsonNode> format - map using MIME types as names
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
        // Convert headers from Map<String, JsonNode> to Map<String, Object>
        Map<String, Object> headersMap = null;
        if (headers != null && !headers.isEmpty()) {
            headersMap = new LinkedHashMap<>();
            for (Map.Entry<String, JsonNode> entry : headers.entrySet()) {
                headersMap.put(entry.getKey(), entry.getValue());
            }
        }

        return new io.ballerina.asyncapi.core.model.message.AsyncApiMessageExample(
                name,
                summary,
                headersMap,
                payload,  // JsonNode is compatible with Object
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
                    entry.getKey(),  // Use MIME type as name (e.g., "application/json")
                    null,            // No summary available in Map format
                    null,            // No headers available in Map format
                    entry.getValue(), // The payload JsonNode
                    null             // No extensions available in Map format
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

        // All v2.x MessageTraits have Map<String, JsonNode> format
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
