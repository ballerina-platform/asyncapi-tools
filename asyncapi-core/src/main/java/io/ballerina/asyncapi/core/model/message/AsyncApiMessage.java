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
package io.ballerina.asyncapi.core.model.message;

import com.fasterxml.jackson.databind.JsonNode;
import io.ballerina.asyncapi.core.model.doc.AsyncApiExternalDocs;
import io.ballerina.asyncapi.core.model.tag.AsyncApiTag;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.Map;

/**
 * Represents a Message Object in an AsyncAPI document.
 *
 * @param headers       Schema Object or Reference Object for the message headers.
 * @param payload       Schema Object or Reference Object for the message payload.
 * @param correlationId The correlation ID used for message tracing.
 * @param contentType   The content type of the message payload (e.g., "application/json").
 * @param name          A machine-friendly name for the message.
 * @param title         A human-friendly title for the message.
 * @param summary       A short summary of the message.
 * @param description   A verbose description of the message.
 * @param tags          A list of tags for API documentation control.
 * @param externalDocs  Additional external documentation for the message.
 * @param bindings      Protocol-specific message bindings.
 * @param examples      A list of example messages.
 * @param traits        A list of traits to apply to the message.
 * @param extensions    Specification extensions (fields prefixed with "x-").
 */
public record AsyncApiMessage(
        Object headers,
        Object payload,
        AsyncApiCorrelationId correlationId,
        MediaType contentType,
        String name,
        String title,
        String summary,
        String description,
        List<AsyncApiTag> tags,
        AsyncApiExternalDocs externalDocs,
        AsyncApiMessageBindings bindings,
        List<AsyncApiMessageExample> examples,
        List<AsyncApiMessageTrait> traits,
        Map<String, JsonNode> extensions
) {
}
