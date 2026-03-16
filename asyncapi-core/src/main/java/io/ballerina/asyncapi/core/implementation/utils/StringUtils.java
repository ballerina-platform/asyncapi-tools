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

import java.util.Locale;

/**
 * General-purpose string utilities for the AsyncAPI core module.
 */
public final class StringUtils {

    private StringUtils() {
    }

    /**
     * Converts a string to PascalCase (UpperCamelCase).
     * @param input the string to convert; returned as-is if {@code null} or blank
     * @return the PascalCase representation
     */
    public static String toPascalCase(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }
        String[] tokens = input.split("[ \\-_/.]+");
        StringBuilder result = new StringBuilder();
        for (String token : tokens) {
            if (token.isEmpty()) {
                continue;
            }
            boolean isPathParam = token.startsWith("{") && token.endsWith("}");
            String cleaned = token.replaceAll("[^a-zA-Z0-9]", "");
            if (cleaned.isEmpty()) {
                continue;
            }
            if (isPathParam) {
                result.append("By");
            }
            String[] subWords = cleaned.split("(?<=[a-z])(?=[A-Z])");
            for (String word : subWords) {
                if (!word.isEmpty()) {
                    result.append(Character.toUpperCase(word.charAt(0)));
                    if (word.length() > 1) {
                        result.append(word.substring(1).toLowerCase(Locale.ENGLISH));
                    }
                }
            }
        }
        return result.toString();
    }
}
