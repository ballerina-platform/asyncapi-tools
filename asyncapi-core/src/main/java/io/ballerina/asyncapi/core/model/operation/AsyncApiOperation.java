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
package io.ballerina.asyncapi.core.model.operation;

import com.fasterxml.jackson.databind.JsonNode;
import io.ballerina.asyncapi.core.model.channel.AsyncApiChannel;
import io.ballerina.asyncapi.core.model.doc.AsyncApiExternalDocs;
import io.ballerina.asyncapi.core.model.message.AsyncApiMessage;
import io.ballerina.asyncapi.core.model.security.AsyncApiSecurityScheme;
import io.ballerina.asyncapi.core.model.tag.AsyncApiTag;

import java.util.List;
import java.util.Map;

/**
 * Represents an Operation Object in an AsyncAPI document.
 *
 * @param action      The action this operation performs (send or receive).
 * @param channel     The channel this operation is associated with.
 * @param title       A human-friendly title for the operation.
 * @param summary     A short summary of the operation.
 * @param description A verbose description of the operation.
 * @param messages    A list of messages associated with this operation.
 * @param security    A list of security mechanisms available for this operation.
 * @param reply       The definition of the reply for this operation.
 * @param tags        A list of tags for API documentation control.
 * @param externalDocs Additional external documentation for the operation.
 * @param bindings    Protocol-specific operation bindings.
 * @param traits      A list of traits to apply to the operation.
 * @param extensions  Specification extensions (fields prefixed with "x-").
 */
public record AsyncApiOperation(
        Action action,
        AsyncApiChannel channel,
        String title,
        String summary,
        String description,
        List<AsyncApiMessage> messages,
        List<AsyncApiSecurityScheme> security,
        AsyncApiOperationReply reply,
        List<AsyncApiTag> tags,
        AsyncApiExternalDocs externalDocs,
        AsyncApiOperationBindings bindings,
        List<AsyncApiOperationTrait> traits,
        Map<String, JsonNode> extensions
) {

    /** The action type indicating whether an operation sends or receives messages. */
    public enum Action {
        SEND,
        RECEIVE
    }
}
