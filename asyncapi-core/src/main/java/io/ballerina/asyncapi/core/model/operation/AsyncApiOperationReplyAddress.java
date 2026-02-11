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

import java.util.Map;

/**
 * Represents the address for an operation reply.
 *
 * @param location    A runtime expression that specifies the location of the reply address.
 * @param description A description of the reply address.
 * @param extensions  Specification extensions (fields prefixed with "x-").
 */
public record AsyncApiOperationReplyAddress(
        String location,
        String description,
        Map<String, JsonNode> extensions
) {
}
