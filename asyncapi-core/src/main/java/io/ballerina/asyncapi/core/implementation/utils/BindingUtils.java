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
import io.apicurio.datamodels.models.asyncapi.AsyncApiBinding;

/**
 * Utility class for extracting typed values from {@link AsyncApiBinding} items.
 */
public final class BindingUtils {

    private BindingUtils() {
    }

    /**
     * Returns the binding item for {@code key} as a String.
     * Textual nodes return their text; non-textual nodes return their JSON representation.
     *
     * @param binding the Apicurio binding object
     * @param key     the item key
     * @return the string value, or null if the item is absent
     */
    public static String getItemAsText(AsyncApiBinding binding, String key) {
        JsonNode node = binding.getItem(key);
        if (node == null) {
            return null;
        }
        return node.isTextual() ? node.asText() : node.toString();
    }

    /**
     * Returns the binding item for {@code key} as an Integer.
     *
     * @param binding the Apicurio binding object
     * @param key     the item key
     * @return the integer value, or null if the item is absent or not an integer node
     */
    public static Integer getItemAsInteger(AsyncApiBinding binding, String key) {
        JsonNode node = binding.getItem(key);
        if (node == null || !node.isInt()) {
            return null;
        }
        return node.asInt();
    }
}
