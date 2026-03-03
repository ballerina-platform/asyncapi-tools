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

import java.util.Map;

/**
 * Represents an example of a message.
 *
 * @param headers    Example of the message headers as a map.
 * @param payload    Example of the message payload.
 * @param name       A machine-friendly name for the example.
 * @param summary    A short summary of the example.
 * @param extensions Specification extensions (fields prefixed with "x-").
 */
public record AsyncApiMessageExample(
        Map<String, Object> headers,
        Object payload,
        String name,
        String summary,
        Map<String, JsonNode> extensions
) {
}
