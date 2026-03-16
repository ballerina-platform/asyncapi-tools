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

import java.util.List;
import java.util.Map;

/**
 * Represents a Channel Parameter Object for address template substitution.
 *
 * @param description  A description of the parameter.
 * @param defaultValue The default value to use for substitution.
 * @param enumValues   An enumeration of allowed string values.
 * @param examples     Example values for the parameter.
 * @param location     A runtime expression specifying the location of the parameter value.
 * @param extensions   Specification extensions (fields prefixed with "x-").
 */
public record AsyncApiChannelParameter(
        String description,
        String defaultValue,
        List<String> enumValues,
        List<String> examples,
        String location,
        Map<String, JsonNode> extensions
) {
}
