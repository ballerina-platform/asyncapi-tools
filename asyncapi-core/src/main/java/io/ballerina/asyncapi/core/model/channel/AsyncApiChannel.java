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
package io.ballerina.asyncapi.core.model.channel;

import com.fasterxml.jackson.databind.JsonNode;
import io.ballerina.asyncapi.core.model.doc.AsyncApiExternalDocs;
import io.ballerina.asyncapi.core.model.message.AsyncApiMessage;
import io.ballerina.asyncapi.core.model.server.AsyncApiServer;
import io.ballerina.asyncapi.core.model.tag.AsyncApiTag;

import java.util.List;
import java.util.Map;

/**
 * Represents a Channel Object in an AsyncAPI document.
 *
 * @param address      The address (topic, routing key, path, etc.) for the channel.
 * @param messages     A map of message names to their definitions available on this channel.
 * @param title        A human-friendly title for the channel.
 * @param summary      A short summary of the channel.
 * @param description  A description of the channel.
 * @param servers      A list of servers this channel is available on.
 * @param parameters   A map of parameter names to their definitions for this channel's address.
 * @param tags         A list of tags for API documentation control.
 * @param externalDocs Additional external documentation for the channel.
 * @param bindings     Protocol-specific channel bindings.
 * @param extensions   Specification extensions (fields prefixed with "x-").
 */
public record AsyncApiChannel(
        String address,
        Map<String, AsyncApiMessage> messages,
        String title,
        String summary,
        String description,
        List<AsyncApiServer> servers,
        Map<String, AsyncApiChannelParameter> parameters,
        List<AsyncApiTag> tags,
        AsyncApiExternalDocs externalDocs,
        AsyncApiChannelBindings bindings,
        Map<String, JsonNode> extensions
) {
}
