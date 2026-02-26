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
import io.apicurio.datamodels.models.asyncapi.AsyncApiMessage;
import io.apicurio.datamodels.models.asyncapi.AsyncApiMessageTrait;
import io.apicurio.datamodels.models.asyncapi.v20.AsyncApi20Message;
import io.apicurio.datamodels.models.asyncapi.v20.AsyncApi20MessageTrait;
import io.apicurio.datamodels.models.asyncapi.v21.AsyncApi21Message;
import io.apicurio.datamodels.models.asyncapi.v21.AsyncApi21MessageTrait;
import io.apicurio.datamodels.models.asyncapi.v22.AsyncApi22Message;
import io.apicurio.datamodels.models.asyncapi.v22.AsyncApi22MessageExample;
import io.apicurio.datamodels.models.asyncapi.v22.AsyncApi22MessageTrait;
import io.apicurio.datamodels.models.asyncapi.v23.AsyncApi23Message;
import io.apicurio.datamodels.models.asyncapi.v23.AsyncApi23MessageExample;
import io.apicurio.datamodels.models.asyncapi.v23.AsyncApi23MessageTrait;
import io.apicurio.datamodels.models.asyncapi.v24.AsyncApi24Message;
import io.apicurio.datamodels.models.asyncapi.v24.AsyncApi24MessageExample;
import io.apicurio.datamodels.models.asyncapi.v24.AsyncApi24MessageTrait;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25Message;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25MessageExample;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25MessageTrait;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26Message;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26MessageExample;
import io.apicurio.datamodels.models.asyncapi.v26.AsyncApi26MessageTrait;
import io.ballerina.asyncapi.core.model.message.AsyncApiMessageExample;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps Apicurio message examples to model
 * {@link AsyncApiMessageExample} for AsyncAPI 2.x.
 */
final class MessageExampleMapperV2 {

    private MessageExampleMapperV2() {

    }

    /**
     * Maps message examples from an AsyncAPI 2.x message.
     * v2.2-v2.6: Maps structured example object with name, summary, headers, payload, extensions.
     * v2.0/v2.1: Maps Map format where MIME types become example names and values become payloads.
     *
     * @param message the Apicurio message object
     * @return a list of mapped examples, or null if not available
     */
    static List<AsyncApiMessageExample> map(AsyncApiMessage message) {
        if (message == null) {
            return null;
        }
        return switch (message) {
            case AsyncApi26Message typed -> {
                AsyncApi26MessageExample ex = typed.getExamples();
                yield ex != null ? List.of(mapMessageExampleItem(
                        ex.getName(), ex.getSummary(), ex.getHeaders(),
                        ex.getPayload(), ex.getExtensions())) : null;
            }
            case AsyncApi25Message typed -> {
                AsyncApi25MessageExample ex = typed.getExamples();
                yield ex != null ? List.of(mapMessageExampleItem(
                        ex.getName(), ex.getSummary(), ex.getHeaders(),
                        ex.getPayload(), ex.getExtensions())) : null;
            }
            case AsyncApi24Message typed -> {
                AsyncApi24MessageExample ex = typed.getExamples();
                yield ex != null ? List.of(mapMessageExampleItem(
                        ex.getName(), ex.getSummary(), ex.getHeaders(),
                        ex.getPayload(), ex.getExtensions())) : null;
            }
            case AsyncApi23Message typed -> {
                AsyncApi23MessageExample ex = typed.getExamples();
                yield ex != null ? List.of(mapMessageExampleItem(
                        ex.getName(), ex.getSummary(), ex.getHeaders(),
                        ex.getPayload(), ex.getExtensions())) : null;
            }
            case AsyncApi22Message typed -> {
                AsyncApi22MessageExample ex = typed.getExamples();
                yield ex != null ? List.of(mapMessageExampleItem(
                        ex.getName(), ex.getSummary(), ex.getHeaders(),
                        ex.getPayload(), ex.getExtensions())) : null;
            }
            case AsyncApi21Message typed -> {
                Map<String, JsonNode> examplesMap = typed.getExamples();
                yield (examplesMap == null || examplesMap.isEmpty()) ? null :
                        examplesMap.entrySet().stream()
                                .map(e -> new AsyncApiMessageExample(null, e.getValue(), e.getKey(), null, null))
                                .toList();
            }
            case AsyncApi20Message typed -> {
                Map<String, JsonNode> examplesMap = typed.getExamples();
                yield (examplesMap == null || examplesMap.isEmpty()) ? null :
                        examplesMap.entrySet().stream()
                                .map(e -> new AsyncApiMessageExample(null, e.getValue(), e.getKey(), null, null))
                                .toList();
            }
            default -> null;
        };
    }

    /**
     * Maps message examples from an AsyncAPI 2.x message trait.
     *
     * @param trait the Apicurio message trait object
     * @return a list of mapped examples, or null if not available
     */
    static List<AsyncApiMessageExample> map(AsyncApiMessageTrait trait) {
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
        if (examplesMap == null || examplesMap.isEmpty()) {
            return null;
        }
        return examplesMap.entrySet().stream()
                .map(e -> new AsyncApiMessageExample(null, e.getValue(), e.getKey(), null, null))
                .toList();
    }

    /**
     * Builds an {@link AsyncApiMessageExample} from structured example fields.
     *
     * @param name       the example name
     * @param summary    the example summary
     * @param headers    the example headers map
     * @param payload    the example payload
     * @param extensions the extensions map
     * @return the mapped AsyncApiMessageExample
     */
    private static AsyncApiMessageExample mapMessageExampleItem(
            String name,
            String summary,
            Map<String, JsonNode> headers,
            JsonNode payload,
            Map<String, JsonNode> extensions) {
        Map<String, Object> headersMap = null;
        if (headers != null && !headers.isEmpty()) {
            headersMap = new LinkedHashMap<>(headers);
        }
        return new AsyncApiMessageExample(headersMap, payload, name, summary, extensions);
    }
}
