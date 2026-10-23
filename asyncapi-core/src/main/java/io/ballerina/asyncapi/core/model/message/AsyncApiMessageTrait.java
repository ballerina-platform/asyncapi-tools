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

import java.util.List;
import java.util.Map;

/**
 * Represents a Message Trait Object that defines reusable message properties.
 *
 * @param name          A machine-friendly name for the message.
 * @param title         A human-friendly title for the message.
 * @param summary       A short summary of the message.
 * @param description   A verbose description of the message.
 * @param contentType   The content type of the message payload.
 * @param headers       Schema Object for the message headers.
 * @param correlationId The correlation ID for message tracing.
 * @param tags          A list of tags for API documentation control.
 * @param externalDocs  Additional external documentation.
 * @param bindings      Protocol-specific message bindings.
 * @param examples      A list of example messages.
 * @param extensions    Specification extensions (fields prefixed with "x-").
 */
public record AsyncApiMessageTrait(
        String name,
        String title,
        String summary,
        String description,
        String contentType,
        Object headers,
        AsyncApiCorrelationId correlationId,
        List<AsyncApiTag> tags,
        AsyncApiExternalDocs externalDocs,
        AsyncApiMessageBindings bindings,
        List<AsyncApiMessageExample> examples,
        Map<String, JsonNode> extensions
) {
}
