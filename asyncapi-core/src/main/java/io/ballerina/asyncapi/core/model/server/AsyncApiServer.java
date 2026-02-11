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
package io.ballerina.asyncapi.core.model.server;

import com.fasterxml.jackson.databind.JsonNode;
import io.ballerina.asyncapi.core.model.doc.AsyncApiExternalDocs;
import io.ballerina.asyncapi.core.model.security.AsyncApiSecurityScheme;
import io.ballerina.asyncapi.core.model.tag.AsyncApiTag;

import java.util.List;
import java.util.Map;

/**
 * Represents a Server Object in an AsyncAPI document.
 * Defines a message broker or server.
 *
 * @param host            The server host name.
 * @param protocol        The protocol this server supports (e.g., "kafka", "ws", "http").
 * @param protocolVersion The version of the protocol used.
 * @param pathname        The path to a resource on the host.
 * @param description     A description of the host.
 * @param title           A human-friendly title for the server.
 * @param summary         A short summary of the server.
 * @param variables       A map of variable substitutions for the server URL template.
 * @param security        A list of security mechanisms supported by the server.
 * @param tags            A list of tags for API documentation control.
 * @param externalDocs    Additional external documentation for the server.
 * @param bindings        Protocol-specific server bindings.
 * @param extensions      Specification extensions (fields prefixed with "x-").
 */
public record AsyncApiServer(
        String host,
        String protocol,
        String protocolVersion,
        String pathname,
        String description,
        String title,
        String summary,
        Map<String, AsyncApiServerVariable> variables,
        List<AsyncApiSecurityScheme> security,
        List<AsyncApiTag> tags,
        AsyncApiExternalDocs externalDocs,
        AsyncApiServerBindings bindings,
        Map<String, JsonNode> extensions
) {
}
