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
import io.apicurio.datamodels.models.asyncapi.AsyncApiCorrelationID;
import io.apicurio.datamodels.models.asyncapi.AsyncApiMessage;
import io.apicurio.datamodels.models.asyncapi.AsyncApiMessageBindings;
import io.apicurio.datamodels.models.asyncapi.AsyncApiMessageTrait;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30CorrelationID;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30Message;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30MessageExample;
import io.apicurio.datamodels.models.asyncapi.v30.AsyncApi30MessageTrait;
import io.ballerina.asyncapi.core.implementation.v3.doc.ExternalDocMapperV3;
import io.ballerina.asyncapi.core.implementation.v3.tag.TagMapperV3;
import io.ballerina.asyncapi.core.model.message.AsyncApiCorrelationId;
import io.ballerina.asyncapi.core.model.message.AsyncApiMessageExample;
import io.ballerina.asyncapi.core.model.message.HttpMessageBindings;
import io.ballerina.asyncapi.core.model.message.WsMessageBindings;
import io.ballerina.asyncapi.core.model.tag.AsyncApiTag;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps Apicurio Message models to {@link io.ballerina.asyncapi.core.model.message.AsyncApiMessage}
 * and related types for AsyncAPI 3.0.
 */
public final class MessageMapperV3 {

    private MessageMapperV3() {

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
        if (http == null && ws == null) {
            return null;
        }
        return new io.ballerina.asyncapi.core.model.message.AsyncApiMessageBindings(http, ws);
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
                example.getName(),
                example.getSummary(),
                headers,
                example.getPayload(),
                example.getExtensions()
        );
    }
}
