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
package io.ballerina.asyncapi.core.implementation.utils;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for JsonNode conversions.
 */
public final class JsonNodeUtils {

    private JsonNodeUtils() {

    }

    /**
     * Converts a JsonNode to a String representation.
     *
     * @param node the JsonNode to convert
     * @return the string value, or null if node is null
     */
    public static String jsonNodeToString(JsonNode node) {
        if (node == null) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        return node.toString();
    }

    /**
     * Converts a list of JsonNode to a list of Strings.
     *
     * @param nodes the list of JsonNodes
     * @return the list of strings, or null if input is null or empty
     */
    public static List<String> jsonNodeListToStringList(List<JsonNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }
        return nodes.stream().map(JsonNodeUtils::jsonNodeToString).collect(Collectors.toList());
    }
}
